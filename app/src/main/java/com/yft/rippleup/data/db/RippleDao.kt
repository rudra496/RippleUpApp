package com.yft.rippleup.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun byEmail(email: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Query("UPDATE users SET passwordHash = :hash WHERE email = :email")
    suspend fun updatePassword(email: String, hash: String)
}

@Dao
interface RippleDao {
    @Insert
    suspend fun insert(ripple: RippleEntity): Long

    @Update
    suspend fun update(ripple: RippleEntity)

    @Query("DELETE FROM ripples WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM ripples WHERE userEmail = :email ORDER BY createdAt ASC")
    fun forUser(email: String): Flow<List<RippleEntity>>

    @Query("SELECT * FROM ripples WHERE userEmail = :email AND demo = 0 AND createdAt >= :sinceMs")
    suspend fun loggedSince(email: String, sinceMs: Long): List<RippleEntity>

    @Query("SELECT * FROM ripples WHERE userEmail = :email AND createdAt >= :sinceMs ORDER BY createdAt DESC LIMIT 1")
    suspend fun lastLogged(email: String, sinceMs: Long): RippleEntity?

    @Query("SELECT SUM(points) FROM ripples WHERE userEmail = :email AND status > 0")
    fun earnedPoints(email: String): Flow<Int?>

    @Query("SELECT SUM(co2eKg) FROM ripples WHERE userEmail = :email AND status > 0")
    fun earnedCo2(email: String): Flow<Float?>

    @Query("SELECT COUNT(*) FROM ripples WHERE userEmail = :email AND status > 0 AND title = :title")
    suspend fun countDone(email: String, title: String): Int
}

@Dao
interface PrefDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(pref: PrefEntity)

    @Query("SELECT value FROM prefs WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Query("SELECT * FROM prefs")
    fun all(): Flow<List<PrefEntity>>
}
