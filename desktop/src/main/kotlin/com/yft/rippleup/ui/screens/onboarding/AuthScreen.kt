package com.yft.rippleup.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.data.remote.Config
import com.yft.rippleup.data.remote.GoogleDesktop
import com.yft.rippleup.ui.components.CircleIconButton
import com.yft.rippleup.ui.components.FieldLabel
import com.yft.rippleup.ui.components.GradientButton
import com.yft.rippleup.ui.components.MintField
import com.yft.rippleup.ui.components.SegmentedTabs
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.theme.*
import kotlinx.coroutines.launch

/**
 * PRODUCTION auth: email + password, 6-digit email code, and Google sign-in.
 * New accounts are approved automatically; the Admin Review queue moderates
 * actions. Includes a working forgot-password flow (emailed 6-digit code).
 */
@Composable
fun AuthScreen(vm: com.yft.rippleup.ui.AppViewModel) {
    var mode by remember { mutableStateOf(0) }   // 0 Join Us, 1 Log In
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var googleBusy by remember { mutableStateOf(false) }
    var forgot by remember { mutableStateOf(false) }     // Forgot password flow
    var forgotSent by remember { mutableStateOf(false) } // reset code emailed
    var code by remember { mutableStateOf("") }
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
            if (mode == 0) "Create Account" else "Welcome Back",
            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            color = Ink,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (mode == 0) "It's easy to start making an impact" else "Good to see you again",
            style = TextStyle(fontSize = 14.sp),
            color = Secondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(26.dp))

        if (mode == 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    FieldLabel("First Name")
                    MintField(first, { first = it }, "John", KeyboardType.Text)
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel("Last Name")
                    MintField(last, { last = it }, "Doe", KeyboardType.Text)
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        FieldLabel("Email")
        MintField(email, { email = it }, "you@university.edu", KeyboardType.Email)
        if (mode == 1 && forgot && forgotSent) {
            Spacer(Modifier.height(14.dp))
            FieldLabel("6-digit code from your email")
            MintField(code, { code = it.filter { c -> c.isDigit() }.take(8) }, "12345678", KeyboardType.Number)
        }
        if (!(mode == 1 && forgot && !forgotSent)) {
        Spacer(Modifier.height(14.dp))
        FieldLabel(if (forgot) "New password" else "Password")
        MintField(
            pass, { pass = it }, if (forgot) "New password (min. 6)" else "Min. 6 characters", KeyboardType.Password,
            trailing = {
                Text(
                    if (show) "Hide" else "Show",
                    color = Secondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.noRippleClickable { show = !show },
                )
            },
            visual = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        )
        }

        if (mode == 1 && !forgot) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Forgot password?",
                color = Teal,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().noRippleClickable {
                    forgot = true
                    forgotSent = false
                    code = ""
                    pass = ""
                    err = ""
                },
                textAlign = TextAlign.End,
            )
        }
        if (mode == 1 && forgot && forgotSent) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Back to log in",
                color = Teal,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().noRippleClickable {
                    forgot = false
                    forgotSent = false
                    code = ""
                    pass = ""
                    err = ""
                },
                textAlign = TextAlign.Center,
            )
        }

        if (mode == 0) {
            Spacer(Modifier.height(10.dp))
            Text(
                "By signing up you agree to our Terms of Service and Privacy Policy.",
                color = Secondary, fontSize = 10.sp, lineHeight = 16.sp,
            )
        }

        if (err.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(err, color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(24.dp))
        GradientButton(
            label = when {
                busy -> "Please wait…"
                mode == 1 && forgot && !forgotSent -> "Send reset code"
                mode == 1 && forgot -> "Reset password & sign in"
                else -> "Continue"
            },
            enabled = !busy && email.contains("@") && email.contains(".") && when {
                mode == 0 -> pass.length >= 6 && first.isNotBlank() && last.isNotBlank()
                forgot -> if (!forgotSent) true else code.length in 6..8 && pass.length >= 6
                else -> pass.length >= 6
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            busy = true
            scope.launch {
                if (mode == 0) {
                    val (e, detail) = vm.signUpWithPassword(first, last, email, pass)
                    when {
                        e == null -> {
                            vm.activatePasswordSession(email.trim().lowercase(), null, first.trim() + " " + last.trim()) { ok, msg ->
                                if (!ok) err = msg
                            }
                        }
                        e == "CONFIRM_EMAIL_ON" ->
                            err = "Account created! Please turn Confirm email OFF (Auth → Providers → Email), then log in."
                        else -> err = e + (detail?.let { " — $it" } ?: "")
                    }
                } else if (mode == 1 && forgot) {
                    if (!forgotSent) {
                        val e2 = vm.requestOtp(email.trim())
                        if (e2 == null) {
                            forgotSent = true
                            err = ""
                        } else err = e2
                    } else {
                        vm.verifyCode(email.trim(), code) { ok, msg ->
                            if (!ok) {
                                busy = false
                                err = msg ?: "Invalid or expired code."
                            } else scope.launch {
                                val e3 = vm.cloud.updatePassword(pass)
                                busy = false
                                if (e3 != null) err = e3
                            }
                        }
                        return@launch
                    }
                } else {
                    val (e, detail) = vm.loginWithPassword(email, pass)
                    if (e == null) {
                        vm.activatePasswordSession(email.trim().lowercase(), null, null) { ok, msg ->
                            if (!ok) err = msg
                        }
                    } else err = e + (detail?.let { " — $it" } ?: "")
                }
                busy = false
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .noRippleClickable(!googleBusy) {
                    googleBusy = true
                    err = ""
                    scope.launch {
                        val result = GoogleDesktop.signIn()
                        if (result.access == null) {
                            err = result.error ?: "Google sign-in failed."
                            googleBusy = false
                        } else {
                            vm.loginWithGoogleTokens(result.access, result.refresh) { ok, msg ->
                                googleBusy = false
                                if (!ok) err = msg
                            }
                        }
                    }
                }
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFD6D6D6), RoundedCornerShape(28.dp))
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (googleBusy) "Opening Google…" else "Continue with Google",
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = if (googleBusy) Secondary else Ink,
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}
