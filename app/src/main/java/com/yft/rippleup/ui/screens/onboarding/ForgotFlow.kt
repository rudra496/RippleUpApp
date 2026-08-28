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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.ui.components.CircleIconButton
import com.yft.rippleup.ui.components.GradientButton
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.theme.*
import kotlinx.coroutines.delay

/** p25/p26 — Forgot Password (disabled/enabled button states). */
@Composable
fun ForgotScreen(onBack: () -> Unit, onSent: () -> Unit) {
    var email by remember { mutableStateOf("") }
    val valid = email.contains("@") && email.contains(".")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        CircleIconButton(onClick = onBack) {
            Text("‹", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(40.dp))
        Text("Forgot Password?", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold), color = Ink, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "No worries! Enter your email and we'll send you a secure reset link.",
            style = TextStyle(fontSize = 14.sp, lineHeight = 24.sp), color = Secondary,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(34.dp))
        FieldLabel("Email address")
        MintField(email, { email = it }, "you@university.edu", KeyboardType.Email)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(30.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("🔒 The reset link expires in ", color = Secondary, fontSize = 11.sp)
            Text("10 minutes", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(" for your security.", color = Secondary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(14.dp))
        GradientButton("Send Reset Link", enabled = valid, modifier = Modifier.fillMaxWidth()) { onSent() }
        Spacer(Modifier.height(28.dp))
    }
}

/** p27 — Check your email, checklist card, Resend in 30s countdown. */
@Composable
fun CheckEmailScreen(onBack: () -> Unit, onReset: () -> Unit) {
    var seconds by remember { mutableIntStateOf(30) }
    LaunchedEffect(Unit) {
        while (seconds > 0) {
            delay(1000)
            seconds -= 1
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        CircleIconButton(onClick = onBack) {
            Text("‹", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(40.dp))
        Text("Check your email", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold), color = Ink, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text("We sent a reset link to", style = TextStyle(fontSize = 14.sp), color = Secondary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("you@university.edu", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = Teal, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(30.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFAFDFC))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CheckRow("🔗", "Click the link in the email")
            CheckRow("⏱️", "Link expires in 10 minutes")
            CheckRow("📂", "Check your spam folder too")
        }
        Spacer(Modifier.weight(1f))
        Text(
            if (seconds > 0) "Resend in ${seconds}s" else "Resend link",
            color = Secondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .noRippleClickable(enabled = seconds <= 0) { seconds = 30 },
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        GradientButton("Reset Password", modifier = Modifier.fillMaxWidth()) { onReset() }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun CheckRow(emoji: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.width(12.dp))
        Text(text, color = Body, fontSize = 14.sp)
    }
}

/** p28 — Reset Password (two mint fields) → Back to Log In. */
@Composable
fun ResetPasswordScreen(onBackToLogin: () -> Unit) {
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    var show1 by remember { mutableStateOf(false) }
    var show2 by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        CircleIconButton(onClick = onBackToLogin) {
            Text("‹", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(40.dp))
        Text("Reset Password", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold), color = Ink, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text("Set your new password", style = TextStyle(fontSize = 14.sp), color = Secondary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(34.dp))
        FieldLabel("Reset Password")
        MintField(
            p1, { p1 = it }, "New password", KeyboardType.Password,
            trailing = { ShowToggle(show1) { show1 = it } },
            visual = if (show1) VisualTransformation.None else PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(16.dp))
        FieldLabel("Confirm Password")
        MintField(
            p2, { p2 = it }, "Repeat password", KeyboardType.Password,
            trailing = { ShowToggle(show2) { show2 = it } },
            visual = if (show2) VisualTransformation.None else PasswordVisualTransformation(),
        )
        if (p2.isNotEmpty() && p1 != p2) {
            Spacer(Modifier.height(8.dp))
            Text("Passwords don't match yet.", color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(30.dp))
        GradientButton("Back to Log In", enabled = p1.length >= 6 && p1 == p2, modifier = Modifier.fillMaxWidth()) { onBackToLogin() }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun ShowToggle(shown: Boolean, set: (Boolean) -> Unit) {
    Text(
        if (shown) "Hide" else "Show",
        color = Secondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.noRippleClickable { set(!shown) },
    )
}
