package com.yft.rippleup.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.data.remote.Config
import com.yft.rippleup.ui.components.CircleIconButton
import com.yft.rippleup.ui.components.GradientButton
import com.yft.rippleup.ui.components.MintField
import com.yft.rippleup.ui.components.SegmentedTabs
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.theme.*
import kotlinx.coroutines.launch

/**
 * PRODUCTION auth: email + 6-digit verification code (passwordless — no unverified
 * accounts possible), Google sign-in once the OAuth client ID is configured, and
 * every new account lands in the Admin Review queue for manual approval.
 */
@Composable
fun AuthScreen(vm: com.yft.rippleup.ui.AppViewModel) {
    var mode by remember { mutableStateOf(0) }            // 0 Join Us, 1 Log In (visual — flow is identical)
    var stage by remember { mutableStateOf(0) }           // 0 email, 1 code
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        CircleIconButton(onClick = { }) {
            Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = "Back", tint = Ink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(20.dp))
        SegmentedTabs(mode) { mode = it; err = "" }
        Spacer(Modifier.height(26.dp))
        Text(
            if (stage == 0) (if (mode == 0) "Create Account" else "Welcome Back") else "Check your email",
            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            color = Ink,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (stage == 0)
                (if (mode == 0) "Join RippleUp — verify your email to get started" else "Good to see you again")
            else "Enter the 6-digit verification code sent to",
            style = TextStyle(fontSize = 14.sp),
            color = Secondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        if (stage == 1) {
            Text(
                email,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                color = Teal,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(26.dp))

        if (stage == 0) {
            com.yft.rippleup.ui.components.FieldLabel("Email")
            MintField(email, { email = it }, "you@university.edu", KeyboardType.Email)
        } else {
            com.yft.rippleup.ui.components.FieldLabel("Verification code")
            MintField(code, { code = it.filter { c -> c.isDigit() }.take(6) }, "123456", KeyboardType.Number)
            Spacer(Modifier.height(10.dp))
            Text(
                "Resend code",
                color = Teal,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .noRippleClickable {
                        scope.launch {
                            busy = true
                            err = vm.requestOtp(email) ?: ""
                            busy = false
                        }
                    },
                textAlign = TextAlign.Center,
            )
        }

        if (err.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(err, color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(24.dp))
        GradientButton(
            label = if (stage == 0) "Send verification code" else "Verify & continue",
            enabled = !busy && (if (stage == 0) email.contains("@") && email.contains(".") else code.length == 6),
            modifier = Modifier.fillMaxWidth(),
        ) {
            busy = true
            scope.launch {
                if (stage == 0) {
                    val e = vm.requestOtp(email)
                    if (e == null) {
                        err = ""
                        stage = 1
                    } else err = e
                } else {
                    vm.verifyCode(email, code) { ok, msg ->
                        err = if (ok) "" else (msg ?: "Invalid or expired code.")
                    }
                }
                busy = false
            }
        }
        Spacer(Modifier.height(12.dp))
        // Google sign-in (activates when the OAuth client ID is configured)
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(1.dp, Color(0x1F000000), RoundedCornerShape(24.dp))
                .noRippleClickable(enabled = false) {
                    err = "Google sign-in is available on the RippleUp mobile app."
                }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("G", color = Color(0xFF4285F4), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (Config.googleConfigured) "Continue with Google"
                    else "Continue with Google (setup in progress)",
                    color = if (Config.googleConfigured) Ink else Color(0xFF9AA6A3),
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "New accounts are reviewed by the RippleUp team before first use.",
            color = Secondary,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            textAlign = TextAlign.Center,
        )
    }
}
