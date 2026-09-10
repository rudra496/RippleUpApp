# -*- coding: utf-8 -*-
"""Final deterministic desktop port from Android sources."""
import os, re, shutil

A = "../app/src/main/java/com/yft/rippleup"
D = "src/main/kotlin/com/yft/rippleup"

def resource_transform(s):
    s = s.replace("androidx.compose.ui.res.painterResource", "org.jetbrains.compose.resources.painterResource")
    s = s.replace("import com.yft.rippleup.R\n", "import com.yft.rippleup.resources.Res\n")
    s = re.sub(r"\bcom\.yft\.rippleup\.R\.drawable\.", "com.yft.rippleup.resources.Res.drawable.", s)
    s = re.sub(r"(?<![\w.])R\.drawable\.", "Res.drawable.", s)
    names = set(re.findall(r"Res\.drawable\.(\w+)", s)) | set(re.findall(r"Res\.font\.(\w+)", s))
    for n in sorted(names):
        imp = f"import com.yft.rippleup.resources.{n}\n"
        if imp not in s:
            if "import com.yft.rippleup.resources.Res\n" in s:
                s = s.replace("import com.yft.rippleup.resources.Res\n", "import com.yft.rippleup.resources.Res\n" + imp)
            else:
                idx = s.rfind("\nimport ")
                s = s[:idx+1] + imp + "import com.yft.rippleup.resources.Res\n" + s[idx+1:]
    return s

# ---- 1. pure-copy files (android -> desktop + resource transform) ----
copies = [
    "ui/screens/home/HomeSheets.kt",
    "ui/screens/home/HomeScreen.kt",
    "ui/screens/discover/DiscoverScreen.kt",
    "ui/screens/rewards/RewardsScreen.kt",
    "ui/screens/profile/ProfileScreen.kt",
    "ui/screens/profile/MyVerificationsScreen.kt",
    "ui/screens/admin/AdminScreen.kt",
    "ui/screens/verify/VerificationReceipt.kt",
    "ui/screens/onboarding/SplashAndGate.kt",
    "ui/components/FormBits.kt",
    "ui/components/Bits.kt",
    "data/remote/CloudSync.kt",
    "data/remote/CloudModels.kt",
    "data/remote/Config.kt",
    "util/QrPayload.kt",
]
for t in copies:
    s = open(os.path.join(A, t), encoding="utf-8").read()
    s = resource_transform(s)
    out = os.path.join(D, t)
    os.makedirs(os.path.dirname(out), exist_ok=True)
    open(out, "w", encoding="utf-8").write(s)
    print("copied", t)

# ---- 2. AuthScreen: copy + strip Google credential launcher (desktop hint only) ----
s = open(os.path.join(A, "ui/screens/onboarding/AuthScreen.kt"), encoding="utf-8").read()
s = resource_transform(s)
s = s.replace(
    ".noRippleClickable(enabled = Config.googleConfigured) {",
    ".noRippleClickable(enabled = false) {",
)
s = s.replace(
    """                    scope.launch {
                        val idToken = googleIdToken(context)
                        if (idToken != null) {
                            vm.loginWithGoogle(idToken) { ok, msg -> if (!ok) err = msg }
                        } else err = "Google sign-in cancelled."
                    }
""",
    """                    err = "Google sign-in is available on the RippleUp mobile app."
""",
)
# remove the googleIdToken helper function entirely
idx = s.find("/** Google Credential Manager -> ID token for the Supabase grant. */")
if idx != -1:
    s = s[:idx].rstrip() + "\n"
open(os.path.join(D, "ui/screens/onboarding/AuthScreen.kt"), "w", encoding="utf-8").write(s)
print("AuthScreen ported (google hint-only)")

# ---- 3. AppViewModel: android copy minus Android framework bits ----
s = open(os.path.join(A, "ui/AppViewModel.kt"), encoding="utf-8").read()
s = s.replace("""import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
""", "")
s = s.replace("""/** PRODUCTION model: cloud-only. No demo accounts, no local fallback. */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    val sessions = SessionManager(app)
    val cloud = CloudSync(sessions)""",
"""/** PRODUCTION desktop model: cloud-only. No demo accounts, no local fallback. */
class AppViewModel {

    val sessions = SessionManager()
    val cloud = CloudSync(sessions)""")
s = s.replace(
    """    private val onboardedPrefs =
        app.getSharedPreferences("rippleup_state", android.content.Context.MODE_PRIVATE)""",
    """    private val onboardedPrefs: java.util.prefs.Preferences =
        java.util.prefs.Preferences.userRoot().node("rippleup-state")""",
)
s = s.replace(
    'onboardedPrefs.edit().putBoolean("onboarded", true).apply()',
    'onboardedPrefs.putBoolean("onboarded", true)',
)
open(os.path.join(D, "ui/AppViewModel.kt"), "w", encoding="utf-8").write(s)
print("AppViewModel ported")

# ---- 4. QrScanScreen: desktop location-picker version (hand-built) ----
qr = open("qr_desktop_template.txt", encoding="utf-8").read()
open(os.path.join(D, "ui/screens/verify/QrScanScreen.kt"), "w", encoding="utf-8").write(qr)
print("QrScanScreen written")

# ---- 5. Nav: desktop Router + boot gate (hand-built) ----
nav = open("nav_desktop_template.txt", encoding="utf-8").read()
open(os.path.join(D, "ui/nav/RippleUpNav.kt"), "w", encoding="utf-8").write(nav)
print("Nav written")

# ---- 6. Main ----
main_kt = """package com.yft.rippleup

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.yft.rippleup.resources.Res
import com.yft.rippleup.resources.drop
import com.yft.rippleup.ui.AppViewModel
import com.yft.rippleup.ui.nav.RippleUpAppRoot
import com.yft.rippleup.ui.theme.BgMain
import com.yft.rippleup.ui.theme.RippleUpTheme
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    val vm = AppViewModel()

    Window(
        onCloseRequest = ::exitApplication,
        title = "RippleUp",
        icon = painterResource(Res.drawable.drop),
        state = rememberWindowState(width = 430.dp, height = 940.dp),
    ) {
        RippleUpTheme {
            Surface(color = BgMain, modifier = Modifier.fillMaxSize()) {
                RippleUpAppRoot(vm)
            }
        }
    }
}
"""
open(os.path.join(D, "Main.kt"), "w", encoding="utf-8").write(main_kt)
print("Main written")
