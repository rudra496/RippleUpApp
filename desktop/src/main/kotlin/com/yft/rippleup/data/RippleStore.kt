package com.yft.rippleup.data

import com.yft.rippleup.data.db.RippleEntity
import com.yft.rippleup.data.db.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Desktop persistence: a tiny JSON file store with the same surface the Android
 * build gets from Room. Single JSON document at ~/.rippleup/state.json, written
 * atomically on every mutation.
 */
class RippleStore {

    @Serializable
    data class State(
        val users: List<UserDto> = emptyList(),
        val ripples: List<RippleDto> = emptyList(),
    )

    @Serializable
    data class UserDto(
        val email: String,
        val firstName: String,
        val lastName: String,
        val passwordHash: String,
        val createdAt: Long,
    )

    @Serializable
    data class RippleDto(
        val id: Long,
        val userEmail: String,
        val title: String,
        val subtitle: String,
        val points: Int,
        val co2eKg: Float,
        val actionKey: String,
        val status: Int,
        val art: String,
        val createdAt: Long,
        val demo: Boolean = false,
        val tamperTag: String = "",
    )

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val dir: Path = Path.of(System.getProperty("user.home"), ".rippleup")
    private val file: Path = dir.resolve("state.json")

    private val stateInternal = MutableStateFlow(State())
    val state: StateFlow<State> = stateInternal

    private var nextId: Long = 1

    init {
        runCatching { load() }
        // seed the demo account exactly like the Android build
        if (stateInternal.value.users.isEmpty()) {
            seed()
        } else {
            nextId = (stateInternal.value.ripples.maxOfOrNull { it.id } ?: 0) + 1
        }
    }

    private fun load() {
        if (!Files.exists(file)) return
        val text = Files.readString(file)
        stateInternal.value = json.decodeFromString<State>(text)
    }

    private fun seed() {
        val now = System.currentTimeMillis()
        val users = listOf(
            UserDto(
                email = Repo.TEST_USER,
                firstName = Repo.TEST_FIRST,
                lastName = Repo.TEST_LAST,
                passwordHash = Repo.hash(Repo.TEST_USER, Repo.TEST_PASS),
                createdAt = now,
            )
        )
        fun ripple(title: String, subtitle: String, pts: Int, kg: Float, key: String, status: Int, art: String) =
            RippleDto(
                id = 0, userEmail = Repo.TEST_USER, title = title, subtitle = subtitle,
                points = pts, co2eKg = kg, actionKey = key, status = status, art = art,
                createdAt = now, demo = true,
            )
        val seeded = listOf(
            ripple("Bring your reusables", "Carried your jute grocery bag today!", 20, 0.05f, "refill", 1, "veg"),
            ripple("Donated clothes @ Thrifty", "Contributing to fabric circularity!", 340, 1.8f, "donate", 2, "balloon"),
            ripple("Weekly meal prep", "Reduce food waste!", 30, 0.4f, "food", 0, "none"),
        ).map { it.copy(id = nextId++, tamperTag = tagOf(it)) }
        stateInternal.value = State(users, seeded)
        save()
    }

    // ---- mutations ------------------------------------------------------------

    fun addUser(user: UserDto) {
        update { it.copy(users = it.users + user) }
    }

    fun findUser(email: String): UserDto? = stateInternal.value.users.firstOrNull { it.email == email }

    fun updateUserPassword(email: String, hash: String) {
        update { s ->
            s.copy(users = s.users.map { if (it.email == email) it.copy(passwordHash = hash) else it })
        }
    }

    fun insertRipple(ripple: RippleEntity): Long {
        val dto = ripple.copy(id = nextId++).toDto()
        update { it.copy(ripples = it.ripples + dto) }
        return dto.id
    }

    fun updateRipple(ripple: RippleEntity) {
        update { s -> s.copy(ripples = s.ripples.map { if (it.id == ripple.id) ripple.toDto() else it }) }
    }

    fun deleteRipple(id: Long) {
        update { s -> s.copy(ripples = s.ripples.filterNot { it.id == id }) }
    }

    // ---- helpers --------------------------------------------------------------

    private fun tagOf(dto: RippleDto): String =
        com.yft.rippleup.util.Guard.tag(dto.userEmail, dto.title, dto.points, dto.status, dto.createdAt)

    private fun UserDto.toEntity() = UserEntity(email, firstName, lastName, passwordHash, createdAt)
    fun entityOf(dto: UserDto): UserEntity = dto.toEntity()

    private fun RippleEntity.toDto() = RippleDto(
        id, userEmail, title, subtitle, points, co2eKg, actionKey, status, art, createdAt, demo, tamperTag,
    )
    fun entityOf(dto: RippleDto): RippleEntity = RippleEntity(
        dto.id, dto.userEmail, dto.title, dto.subtitle, dto.points, dto.co2eKg,
        dto.actionKey, dto.status, dto.art, dto.createdAt, dto.demo, dto.tamperTag,
    )

    private fun update(transform: (State) -> State) {
        stateInternal.value = transform(stateInternal.value)
        save()
    }

    private fun save() {
        val snapshot = stateInternal.value
        scope.launch {
            runCatching {
                Files.createDirectories(dir)
                val tmp = dir.resolve("state.json.tmp")
                Files.writeString(tmp, json.encodeToString(snapshot))
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            }
        }
    }
}
