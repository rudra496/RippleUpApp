package com.yft.rippleup.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.ui.components.CircleIconButton
import com.yft.rippleup.ui.components.GradientButton
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.theme.*

/**
 * p23-24 — Sign Up / Log In with the mint segmented control.
 * p25-28 — Forgot password → Check email (resend 30s) → Reset password.
 * Local auth only. Test account: admin / rudra (one-tap fill).
 */
@Composable
fun AuthScreen(
    startTab: Int,
    vm: com.yft.rippleup.ui.AppViewModel,
    onAuthed: () -> Unit,
    onNeedsPersonalisation: () -> Unit,
) {
    var tab by remember { mutableStateOf(if (startTab == 0) 0 else 1) }
    var subRoute by remember { mutableStateOf(SubRoute.FORM) }

    when (subRoute) {
        SubRoute.FORM -> AuthForm(tab, { tab = it }, vm, onAuthed, onNeedsPersonalisation) { subRoute = it }
        SubRoute.FORGOT -> ForgotScreen { subRoute = SubRoute.CHECK }
        SubRoute.CHECK -> CheckEmailScreen(onBack = { subRoute = SubRoute.FORM }, onReset = { subRoute = SubRoute.RESET })
        SubRoute.RESET -> ResetPasswordScreen(onBackToLogin = { subRoute = SubRoute.FORM })
    }
}

private enum class SubRoute { FORM, FORGOT, CHECK, RESET }

@Composable
private fun AuthForm(
    tab: Int,
    setTab: (Int) -> Unit,
    vm: com.yft.rippleup.ui.AppViewModel,
    onAuthed: () -> Unit,
    onNeedsPersonalisation: () -> Unit,
    openSub: (SubRoute) -> Unit,
) {
    var first by remember { mutableStateOf("") }
    var last by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    var needsPersonalisation by remember { mutableStateOf(true) }

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
            Text("‹", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        SegmentedTabs(tab) { setTab(it); err = "" }
        Spacer(Modifier.height(26.dp))
        Text(
            if (tab == 0) "Create Account" else "Welcome Back",
            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            color = Ink,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (tab == 0) "It's easy to start making an impact" else "Good to see you again",
            style = TextStyle(fontSize = 14.sp),
            color = Secondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(26.dp))

        if (tab == 0) {
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

        if (tab == 1) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Forgot Password?",
                color = Teal,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.End)
                    .noRippleClickable { openSub(SubRoute.FORGOT) },
            )
            Spacer(Modifier.height(6.dp))
            // Prompt-required: one-tap fill of the local test account.
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Mint)
                    .noRippleClickable {
                        email = com.yft.rippleup.data.Repo.TEST_USER
                        pass = com.yft.rippleup.data.Repo.TEST_PASS
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("⚡ Fill test account (admin / rudra)", color = Teal, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Spacer(Modifier.height(10.dp))
            Text(
                "By signing up you agree to our Terms of Service and Privacy Policy. Your data is never sold.",
                color = Secondary,
                fontSize = 10.sp,
                lineHeight = 16.sp,
            )
        }

        if (err.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(err, color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(24.dp))
        GradientButton(
            label = "Continue",
            enabled = email.isNotBlank() && pass.isNotBlank() && (tab == 1 || (first.isNotBlank() && last.isNotBlank())),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (tab == 0) {
                vm.signUp(first, last, email, pass) { ok, msg ->
                    err = msg
                    if (ok) onNeedsPersonalisation()
                }
            } else {
                vm.login(email, pass) { ok, msg ->
                    err = msg
                    if (ok) onAuthed()
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            if (tab == 0) "Already have an account? Log In" else "Don't have an account? Sign Up",
            color = Teal,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .noRippleClickable { setTab(1 - tab); err = "" }
                .padding(bottom = 20.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

/** p23/24 segmented Join Us / Log In control. */
@Composable
fun SegmentedTabs(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("Join Us", "Log In")
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MintTrack)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            labels.forEachIndexed { i, label ->
                val active = i == selected
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (active) Color.White else Color.Transparent)
                        .noRippleClickable { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = if (active) Teal else Secondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/** Two-segment control used by Rewards (Rewards / Badges). */
@Composable
fun RewardTabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(46.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        listOf("Rewards", "Badges").forEachIndexed { i, label ->
            val active = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(23.dp))
                    .background(if (active) Teal else Color(0xFFE9E9E9))
                    .noRippleClickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (active) Color.White else Secondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(
        text,
        color = Ink,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 7.dp),
    )
}

@Composable
fun MintField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    trailing: (@Composable () -> Unit)? = null,
    visual: VisualTransformation = VisualTransformation.None,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MintInput)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF0C2620)),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visual,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(placeholder, color = Color(0xFF8F8F8F), fontSize = 14.sp)
                        }
                        inner()
                    }
                    trailing?.invoke()
                }
            },
        )
    }
}
