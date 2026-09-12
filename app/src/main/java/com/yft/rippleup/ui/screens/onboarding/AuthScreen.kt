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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.yft.rippleup.data.remote.Config
import com.yft.rippleup.ui.components.RippleLogo
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.theme.*
import kotlinx.coroutines.launch

/**
 * GOOGLE-ONLY sign-in (Rudra Sir's call): the entire auth surface is one button.
 * Accounts are created automatically on first Google sign-in. The email/password
 * and OTP APIs remain available in CloudSync/AppViewModel if email sign-in is
 * ever restored.
 */
@Composable
fun AuthScreen(vm: com.yft.rippleup.ui.AppViewModel) {
    var googleBusy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgOnboarding)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.9f))
        RippleLogo(size = 96.dp, useEmblem = true)
        Spacer(Modifier.height(18.dp))
        Text(
            "RippleUp",
            style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold),
            color = Teal,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Sustainability, made fun",
            style = TextStyle(fontSize = 14.sp),
            color = Secondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))

        if (err.isNotEmpty()) {
            Text(err, color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .noRippleClickable(!googleBusy) {
                    googleBusy = true
                    err = ""
                    scope.launch {
                        try {
                            val manager = CredentialManager.create(context)
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(
                                    GetGoogleIdOption.Builder()
                                        .setServerClientId(Config.GOOGLE_WEB_CLIENT_ID)
                                        .setFilterByAuthorizedAccounts(false)
                                        .build(),
                                )
                                .build()
                            val result = manager.getCredential(context, request)
                            val idToken = when (val cred = result.credential) {
                                is CustomCredential ->
                                    if (cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
                                        GoogleIdTokenCredential.createFrom(cred.data).idToken
                                    else null
                                else -> null
                            }
                            if (idToken == null) {
                                err = "Google sign-in is unavailable on this device."
                                googleBusy = false
                            } else {
                                vm.loginWithGoogle(idToken) { ok, msg ->
                                    googleBusy = false
                                    if (!ok) err = msg
                                }
                            }
                        } catch (e: GetCredentialCancellationException) {
                            googleBusy = false
                        } catch (e: Exception) {
                            err = e.message ?: "Google sign-in failed."
                            googleBusy = false
                        }
                    }
                }
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFD6D6D6), RoundedCornerShape(28.dp))
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("G", color = Color(0xFF4285F4), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(10.dp))
                Text(
                    if (googleBusy) "Opening Google…" else "Continue with Google",
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    color = if (googleBusy) Secondary else Ink,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "By continuing you agree to our Terms of Service and Privacy Policy.",
            color = Secondary,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 26.dp),
        )
    }
}
