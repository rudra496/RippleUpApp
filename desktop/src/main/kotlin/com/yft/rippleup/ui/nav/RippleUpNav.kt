package com.yft.rippleup.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.ui.AppViewModel
import com.yft.rippleup.ui.Boot
import com.yft.rippleup.ui.PendingVerify
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.screens.admin.AdminScreen
import com.yft.rippleup.ui.screens.discover.DiscoverScreen
import com.yft.rippleup.ui.screens.home.EventDetailSheet
import com.yft.rippleup.ui.screens.home.HomeScreen
import com.yft.rippleup.ui.screens.home.NotificationsSheet
import com.yft.rippleup.ui.screens.home.VerifyChoiceDialog
import com.yft.rippleup.ui.screens.onboarding.AuthScreen
import com.yft.rippleup.ui.screens.onboarding.OnboardingScreen
import com.yft.rippleup.ui.screens.onboarding.PendingApprovalScreen
import com.yft.rippleup.ui.screens.onboarding.SplashScreen
import com.yft.rippleup.ui.screens.profile.MyVerificationsScreen
import com.yft.rippleup.ui.screens.profile.ProfileScreen
import com.yft.rippleup.ui.screens.rewards.RewardsScreen
import com.yft.rippleup.ui.screens.verify.QrScanScreen
import com.yft.rippleup.ui.screens.verify.VerifiedScreen
import com.yft.rippleup.ui.screens.verify.VerifyingScreen
import com.yft.rippleup.ui.screens.verify.VerifyActionScreen
import com.yft.rippleup.ui.theme.*

object Routes {
    const val HOME = "home"
    const val DISCOVER = "discover"
    const val REWARDS = "rewards"
    const val PROFILE = "profile"
    const val VERIFY_ACTION = "verify_action"
    const val VERIFYING = "verifying"
    const val VERIFIED = "verified"
    const val QR_SCAN = "qr_scan"
    const val ADMIN = "admin"
    const val MY_VERIFICATIONS = "my_verifications"
    const val RECEIPT = "receipt"
}

/** Minimal back-stack router (desktop fork of the Android NavHost). */
class Router(default: String) {
    val stack = mutableStateListOf(default)
    val current: String get() = stack.last()
    fun push(route: String) { stack.add(route) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun resetTo(route: String) { stack.clear(); stack.add(route) }
    fun navigateTop(route: String) { resetTo(route) }
}

@Composable
fun RippleUpAppRoot(vm: AppViewModel) {
    val boot by vm.boot.collectAsState()
    when (boot) {
        Boot.LOADING -> SplashScreen()
        Boot.ONBOARDING -> OnboardingScreen {
            vm.markOnboarded()
            vm.goToAuth()
        }
        Boot.AUTH -> AuthScreen(vm = vm)
        Boot.PENDING_APPROVAL -> PendingApprovalScreen(vm)
        Boot.HOME -> HomeRoot(vm)
        else -> SplashScreen()
    }
}

@Composable
private fun HomeRoot(vm: AppViewModel) {
    val router = remember { Router(Routes.HOME) }

    var showChoice by remember { mutableStateOf(false) }
    var showNotifs by remember { mutableStateOf(false) }
    var showEvent by remember { mutableStateOf(false) }
    var pendingVerify by remember { mutableStateOf<PendingVerify?>(null) }
    var lastReceiptId by remember { mutableStateOf<Long?>(null) }

    Surface(color = com.yft.rippleup.ui.theme.BgMain, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            when (router.current) {
                Routes.HOME -> TabScaffold(
                    vm, router, Routes.HOME, showChoice, { showChoice = it },
                    onChoiceSelf = {
                        showChoice = false
                        pendingVerify = defaultCustomPending()
                        router.push(Routes.VERIFY_ACTION)
                    },
                    onChoiceQr = {
                        showChoice = false
                        router.push(Routes.QR_SCAN)
                    },
                ) {
                    HomeScreen(
                        vm = vm,
                        onOpenNotifications = { showNotifs = true },
                        onOpenEvent = { showEvent = true },
                        onLogAction = { showChoice = true },
                    )
                }
                Routes.DISCOVER -> TabScaffold(
                    vm, router, Routes.DISCOVER, showChoice, { showChoice = it },
                    onChoiceSelf = {
                        showChoice = false
                        pendingVerify = defaultCustomPending()
                        router.push(Routes.VERIFY_ACTION)
                    },
                    onChoiceQr = {
                        showChoice = false
                        router.push(Routes.QR_SCAN)
                    },
                ) {
                    DiscoverScreen(vm = vm, onOpenNotifications = { showNotifs = true })
                }
                Routes.REWARDS -> TabScaffold(
                    vm, router, Routes.REWARDS, showChoice, { showChoice = it },
                    onChoiceSelf = {
                        showChoice = false
                        pendingVerify = defaultCustomPending()
                        router.push(Routes.VERIFY_ACTION)
                    },
                    onChoiceQr = {
                        showChoice = false
                        router.push(Routes.QR_SCAN)
                    },
                ) {
                    RewardsScreen(onOpenNotifications = { showNotifs = true }, snackbar = { })
                }
                Routes.PROFILE -> TabScaffold(
                    vm, router, Routes.PROFILE, showChoice, { showChoice = it },
                    onChoiceSelf = {
                        showChoice = false
                        pendingVerify = defaultCustomPending()
                        router.push(Routes.VERIFY_ACTION)
                    },
                    onChoiceQr = {
                        showChoice = false
                        router.push(Routes.QR_SCAN)
                    },
                ) {
                    ProfileScreen(
                        vm = vm,
                        onOpenAbout = { },
                        onOpenNotifSettings = { },
                        onOpenHelp = { },
                        onOpenPrivacy = { },
                        onOpenAdmin = { router.push(Routes.ADMIN) },
                        onOpenMyVerifications = { router.push(Routes.MY_VERIFICATIONS) },
                    )
                }

                Routes.VERIFY_ACTION -> {
                    val pending = pendingVerify
                    if (pending == null) router.pop() else VerifyActionScreen(
                        vm = vm,
                        pending = pending,
                        onVerified = { router.push(Routes.VERIFYING) },
                        onCancel = { router.pop() },
                    )
                }
                Routes.VERIFYING -> VerifyingScreen(onDone = { router.push(Routes.VERIFIED) })
                Routes.VERIFIED -> VerifiedScreen(
                    pending = pendingVerify ?: defaultCustomPending(),
                    showReceiptButton = lastReceiptId != null,
                    onViewReceipt = { router.push(Routes.RECEIPT) },
                ) {
                    pendingVerify = null
                    router.resetTo(Routes.HOME)
                }
                Routes.QR_SCAN -> QrScanScreen(
                    vm = vm,
                    onVerified = { verId ->
                        lastReceiptId = verId
                        pendingVerify = PendingVerify(
                            "Partner action verified",
                            "QR-verified at partner location",
                            500, "refill", 1.2f, viaQr = true,
                        )
                        router.push(Routes.VERIFYING)
                    },
                    onError = { },
                    onClose = { router.pop() },
                )
            }
        }
    }

    if (showNotifs) {
        NotificationsSheet(vm = vm, onClose = { showNotifs = false }, onLogAction = { showChoice = true })
    }
    if (showEvent) {
        EventDetailSheet(vm = vm, onClose = { showEvent = false })
    }
}

private fun defaultCustomPending() = PendingVerify(
    title = "Used old t-shirt to make a DIY tote bag",
    subtitle = "Self-reported custom action",
    points = 20, actionKey = "custom", co2eKg = 0.3f, viaQr = false,
)

/** Floating pill nav + center QR FAB (p03/p18). */
@Composable
fun TabScaffold(
    vm: AppViewModel,
    router: Router,
    route: String,
    showChoice: Boolean,
    setShowChoice: (Boolean) -> Unit,
    onChoiceSelf: () -> Unit,
    onChoiceQr: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        content()
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
        ) {
            Box(contentAlignment = Alignment.BottomCenter) {
                // pill bar (drawn first, underneath)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .height(68.dp)
                        .shadow(8.dp, RoundedCornerShape(30.dp))
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color(0xF7FFFFFF)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NavItem(Icons.Outlined.Home, "Home", route == Routes.HOME, Modifier.weight(1f)) { router.navigateTop(Routes.HOME) }
                    NavItem(Icons.Outlined.Eco, "Discover", route == Routes.DISCOVER, Modifier.weight(1f)) { router.navigateTop(Routes.DISCOVER) }
                    Spacer(Modifier.width(96.dp)) // center gap for the FAB
                    NavItem(Icons.Outlined.CardGiftcard, "Rewards", route == Routes.REWARDS, Modifier.weight(1f)) { router.navigateTop(Routes.REWARDS) }
                    NavItem(Icons.Outlined.Person, "Profile", route == Routes.PROFILE, Modifier.weight(1f)) { router.navigateTop(Routes.PROFILE) }
                    Spacer(Modifier.width(6.dp))
                }
                // FAB - drawn LAST so nothing covers it
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-24).dp)
                        .size(60.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(if (showChoice) Orange else Teal)
                        .noRippleClickable { setShowChoice(true) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.QrCode2, contentDescription = "Verify a ripple", tint = Color.White, modifier = Modifier.size(25.dp))
                }
            }
        }
    }

    if (showChoice) {
        VerifyChoiceDialog(
            onSelfReport = onChoiceSelf,
            onQr = onChoiceQr,
            onDismiss = { setShowChoice(false) },
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .padding(vertical = 5.dp, horizontal = 2.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) MintNavPill else Color.Transparent)
            .noRippleClickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) Ink else TealSoftText, modifier = Modifier.size(21.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Ink else TealSoftText)
    }
}
