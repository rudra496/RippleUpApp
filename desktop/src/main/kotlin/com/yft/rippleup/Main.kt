package com.yft.rippleup

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.yft.rippleup.data.RippleStore
import com.yft.rippleup.resources.Res
import com.yft.rippleup.resources.drop
import com.yft.rippleup.ui.AppViewModel
import com.yft.rippleup.ui.nav.RippleUpAppRoot
import com.yft.rippleup.ui.theme.BgMain
import com.yft.rippleup.ui.theme.RippleUpTheme

fun main() = application {
    val vm = AppViewModel(RippleStore())

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