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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.data.remote.Config
import com.yft.rippleup.ui.components.CircleIconButton
import com.yft.rippleup.ui.components.FieldLabel
import com.yft.rippleup.ui.components.GradientButton
import com.yft.rippleup.ui.components.MintField
import com.yft.rippleup.ui.components.SegmentedTabs
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.theme.*
import kotlinx.coroutines.launch

/**
 * PRODUCTION auth: email + password (PDF design). Every new account is created as
 * "pending" and must be approved in the Admin Review queue before it can be used —
 * that is the verification layer. Google sign-in activates once the OAuth client ID
 * is configured (Config.GOOGLE_WEB_CLIENT_ID).
 */
@Composable
fun AuthScreen(vm: com.yft.rippleup.ui.AppViewModel) {
    var mode by remember { mutableStateOf(0) }   // 0 Join Us, 1 Log In
    var codeLogin by remember { mutableStateOf(false) }  // Log In via emailed 6-digit code
    var codeSent by remember { mutableStateOf(false) }
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

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
        if (mode == 1 && codeLogin) {
            Spacer(Modifier.height(14.dp))
            FieldLabel("6-digit code from your email")
            MintField(code, { code = it.filter { c -> c.isDigit() }.take(8) }, "12345678", KeyboardType.Number)
            Spacer(Modifier.height(8.dp))
            Text(
                "Resend code",
                color = Teal,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().noRippleClickable {
                    scope.launch { err = vm.requestOtp(email) ?: "" }
                },
                textAlign = TextAlign.Center,
            )
        }
        if (!(mode == 1 && codeLogin)) {
        Spacer(Modifier.height(14.dp))
        FieldLabel("Password")
        MintField(
            pass, { pass = it }, "Min. 6 characters", KeyboardType.Password,
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

        if (mode == 1) {
            Spacer(Modifier.height(8.dp))
            Text(
                if (codeLogin) "Use password instead" else "Use email code instead",
                color = Teal,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().noRippleClickable {
                    codeLogin = !codeLogin
                    code = ""
                    err = ""
                },
                textAlign = TextAlign.Center,
            )
        }

        if (mode == 0) {
            Spacer(Modifier.height(10.dp))
            Text(
                "By signing up you agree to our Terms of Service and Privacy Policy. " +
                    "Your account is verified by the RippleUp team before first use.",
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
            label = if (busy) "Please wait…" else "Continue",
            enabled = !busy && email.contains("@") && email.contains(".") &&
                (if (mode == 1 && codeLogin) code.length in 6..8 else pass.length >= 6) &&
                (mode == 1 || (first.isNotBlank() && last.isNotBlank())),
            modifier = Modifier.fillMaxWidth(),
        ) {
            busy = true
            scope.launch {
                if (mode == 0) {
                    val (e, detail) = vm.signUpWithPassword(first, last, email, pass)
                    when {
                        e == null -> {
                            android.widget.Toast.makeText(
                                context, "Account created — welcome to RippleUp!", android.widget.Toast.LENGTH_LONG,
                            ).show()
                            vm.activatePasswordSession(email.trim().lowercase(), null, first.trim() + " " + last.trim()) { ok, msg ->
                                if (!ok) err = msg
                            }
                        }
                        e == "CONFIRM_EMAIL_ON" ->
                            err = "Account created! Please turn Confirm email OFF (Auth → Providers → Email), then log in."
                        e.contains("already registered", true) ->
                            err = "This email already has an account — switch to Log In and use the email-code option."
                        else -> err = e + (detail?.let { " — $it" } ?: "")
                    }
                } else if (codeLogin) {
                    vm.verifyCode(email, code) { ok, msg ->
                        err = if (ok) "" else (msg ?: "Invalid or expired code.")
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
