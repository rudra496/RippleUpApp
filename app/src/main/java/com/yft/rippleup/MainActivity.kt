package com.yft.rippleup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yft.rippleup.ui.nav.RippleUpAppRoot
import com.yft.rippleup.ui.theme.RippleUpTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            RippleUpTheme {
                val vm: com.yft.rippleup.ui.AppViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                RippleUpAppRoot(vm)
            }
        }
    }
}
