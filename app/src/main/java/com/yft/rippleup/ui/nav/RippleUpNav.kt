package com.yft.rippleup.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yft.rippleup.data.db.RippleEntity
import com.yft.rippleup.ui.AppViewModel
import com.yft.rippleup.ui.PendingVerify
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.screens.discover.DiscoverScreen
import com.yft.rippleup.ui.screens.home.EventDetailSheet
import com.yft.rippleup.ui.screens.home.HomeScreen
import com.yft.rippleup.ui.screens.home.NotificationsSheet
import com.yft.rippleup.ui.screens.home.VerifyChoiceDialog
import com.yft.rippleup.ui.screens.onboarding.AuthScreen
import com.yft.rippleup.ui.screens.onboarding.OnboardingScreen
import com.yft.rippleup.ui.screens.onboarding.PersonaliseScreen
import com.yft.rippleup.ui.screens.onboarding.SplashScreen
import com.yft.rippleup.ui.screens.profile.ProfileScreen
import com.yft.rippleup.ui.screens.rewards.RewardsScreen
import com.yft.rippleup.ui.screens.verify.QrScanScreen
import com.yft.rippleup.ui.screens.verify.VerifiedScreen
import com.yft.rippleup.ui.screens.verify.VerifyingScreen
import com.yft.rippleup.ui.screens.verify.VerifyActionScreen
import com.yft.rippleup.ui.theme.*

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val AUTH = "auth"
    const val PERSONALISE = "personalise"
    const val HOME = "home"
    const val DISCOVER = "discover"
    const val REWARDS = "rewards"
    const val PROFILE = "profile"
    const val VERIFY_ACTION = "verify_action"
    const val VERIFYING = "verifying"
    const val VERIFIED = "verified"
    const val QR_SCAN = "qr_scan"
}

@Composable
fun RippleUpAppRoot(vm: AppViewModel) {
    val nav = rememberNavController()
    val start = remember { vm.computeStart() }

    // flow-shared state
    var showChoice by remember { mutableStateOf(false) }
    var showNotifs by remember { mutableStateOf(false) }
    var showEvent by remember { mutableStateOf(false) }
    var pendingVerify by remember { mutableStateOf<PendingVerify?>(null) }

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.SPLASH) {
            SplashScreen {
                nav.navigate(Routes.ONBOARDING) { popUpTo(Routes.SPLASH) { inclusive = true } }
            }
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen {
                vm.markOnboarded()
                nav.navigate(Routes.AUTH) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            }
        }
        composable(Routes.AUTH) {
            AuthScreen(
                startTab = 1,
                vm = vm,
                onAuthed = { nav.navigate(Routes.HOME) { popUpTo(Routes.AUTH) { inclusive = true } } },
                onNeedsPersonalisation = { nav.navigate(Routes.PERSONALISE) { popUpTo(Routes.AUTH) { inclusive = true } } },
            )
        }
        composable(Routes.PERSONALISE) {
            PersonaliseScreen {
                nav.navigate(Routes.HOME) { popUpTo(Routes.PERSONALISE) { inclusive = true } }
            }
        }

        composable(Routes.HOME) {
            TabScaffold(
                vm = vm, nav = nav, route = Routes.HOME,
                showChoice = showChoice, setShowChoice = { showChoice = it },
                onChoiceSelf = {
                    showChoice = false
                    pendingVerify = defaultCustomPending()
                    nav.navigate(Routes.VERIFY_ACTION)
                },
                onChoiceQr = {
                    showChoice = false
                    nav.navigate(Routes.QR_SCAN)
                },
            ) {
                HomeScreen(
                    vm = vm,
                    onOpenNotifications = { showNotifs = true },
                    onOpenEvent = { showEvent = true },
                    onStartVerify = { entity ->
                        pendingVerify = entity?.toPending()
                        nav.navigate(Routes.VERIFY_ACTION)
                    },
                )
            }
        }
        composable(Routes.DISCOVER) {
            TabScaffold(
                vm = vm, nav = nav, route = Routes.DISCOVER,
                showChoice = showChoice, setShowChoice = { showChoice = it },
                onChoiceSelf = {
                    showChoice = false
                    pendingVerify = defaultCustomPending()
                    nav.navigate(Routes.VERIFY_ACTION)
                },
                onChoiceQr = {
                    showChoice = false
                    nav.navigate(Routes.QR_SCAN)
                },
            ) {
                DiscoverScreen(
                    vm = vm,
                    onOpenNotifications = { showNotifs = true },
                    onStartVerifyFor = { action ->
                        vm.addPending(action.title, action.note, action.points, action.actionKey, kgFor(action.points))
                        pendingVerify = PendingVerify(action.title, action.note, action.points, action.actionKey, kgFor(action.points), viaQr = false)
                        nav.navigate(Routes.VERIFY_ACTION)
                    },
                )
            }
        }
        composable(Routes.REWARDS) {
            TabScaffold(
                vm = vm, nav = nav, route = Routes.REWARDS,
                showChoice = showChoice, setShowChoice = { showChoice = it },
                onChoiceSelf = {
                    showChoice = false
                    pendingVerify = defaultCustomPending()
                    nav.navigate(Routes.VERIFY_ACTION)
                },
                onChoiceQr = {
                    showChoice = false
                    nav.navigate(Routes.QR_SCAN)
                },
            ) {
                RewardsScreen(onOpenNotifications = { showNotifs = true }, snackbar = { })
            }
        }
        composable(Routes.PROFILE) {
            TabScaffold(
                vm = vm, nav = nav, route = Routes.PROFILE,
                showChoice = showChoice, setShowChoice = { showChoice = it },
                onChoiceSelf = {
                    showChoice = false
                    pendingVerify = defaultCustomPending()
                    nav.navigate(Routes.VERIFY_ACTION)
                },
                onChoiceQr = {
                    showChoice = false
                    nav.navigate(Routes.QR_SCAN)
                },
            ) {
                ProfileScreen(
                    vm = vm,
                    onOpenAbout = { },
                    onOpenNotifSettings = { },
                    onOpenHelp = { },
                    onOpenPrivacy = { },
                )
            }
        }

        composable(Routes.VERIFY_ACTION) {
            val pending = pendingVerify
            if (pending == null) {
                nav.popBackStack()
            } else {
                VerifyActionScreen(
                    vm = vm,
                    pending = pending,
                    onVerified = { nav.navigate(Routes.VERIFYING) { popUpTo(Routes.VERIFY_ACTION) { inclusive = true } } },
                    onCancel = { nav.popBackStack() },
                )
            }
        }
        composable(Routes.VERIFYING) {
            VerifyingScreen(onDone = { nav.navigate(Routes.VERIFIED) { popUpTo(Routes.VERIFYING) { inclusive = true } } })
        }
        composable(Routes.VERIFIED) {
            val pending = pendingVerify
            VerifiedScreen(pending = pending ?: defaultCustomPending()) {
                pendingVerify = null
                nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
            }
        }
        composable(Routes.QR_SCAN) {
            QrScanScreen(
                onDetected = {
                    pendingVerify = PendingVerify(
                        "Donated clothes @ ThriftUp",
                        "Partner-verified at ThriftUp Store",
                        500, "donate", 1.2f, viaQr = true,
                    )
                    nav.navigate(Routes.VERIFYING)
                },
                onClose = { nav.popBackStack() },
            )
        }
    }

    // global sheets (PDF shows them over home)
    if (showNotifs) {
        NotificationsSheet(
            onClose = { showNotifs = false },
            onLogAction = { showChoice = true },
        )
    }
    if (showEvent) {
        EventDetailSheet(onClose = { showEvent = false })
    }
}

private fun RippleEntity.toPending() = PendingVerify(
    title = title, subtitle = subtitle, points = points,
    actionKey = actionKey, co2eKg = co2eKg, viaQr = status == 2 || actionKey == "qr",
)

private fun defaultCustomPending() = PendingVerify(
    title = "Used old t-shirt to make a DIY tote bag",
    subtitle = "Self-reported custom action",
    points = 20, actionKey = "custom", co2eKg = 0.3f, viaQr = false,
)

private fun kgFor(points: Int): Float = when {
    points >= 500 -> 1.2f
    points >= 100 -> 0.4f
    else -> 0.05f
}

/** Scaffold with the floating pill nav + center QR FAB (p03/p18). */
@Composable
fun TabScaffold(
    vm: AppViewModel,
    nav: NavHostController,
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
                .navigationBarsPadding()
                .padding(bottom = 6.dp)
        ) {
            Box(contentAlignment = Alignment.BottomCenter) {
                // FAB
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-28).dp)
                        .size(62.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(if (showChoice) Orange else Teal)
                        .noRippleClickable { setShowChoice(true) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.QrCode2, contentDescription = "Verify a ripple", tint = Color.White, modifier = Modifier.size(26.dp))
                }
                // pill bar
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .height(66.dp)
                        .shadow(8.dp, RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xF2FFFFFF)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NavItem(Icons.Outlined.Home, "Home", route == Routes.HOME, Modifier.weight(1f)) { nav.navigateTop(Routes.HOME) }
                    NavItem(Icons.Outlined.Eco, "Discover", route == Routes.DISCOVER, Modifier.weight(1f)) { nav.navigateTop(Routes.DISCOVER) }
                    Spacer(Modifier.width(64.dp)) // center gap for the FAB
                    NavItem(Icons.Outlined.CardGiftcard, "Rewards", route == Routes.REWARDS, Modifier.weight(1f)) { nav.navigateTop(Routes.REWARDS) }
                    NavItem(Icons.Outlined.Person, "Profile", route == Routes.PROFILE, Modifier.weight(1f)) { nav.navigateTop(Routes.PROFILE) }
                    Spacer(Modifier.width(6.dp))
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

private fun NavHostController.navigateTop(route: String) {
    navigate(route) {
        popUpTo(route) { inclusive = true }
        launchSingleTop = true
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
