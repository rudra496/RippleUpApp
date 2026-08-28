package com.yft.rippleup.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.data.Content
import com.yft.rippleup.ui.components.GradientButton
import com.yft.rippleup.ui.components.RippleLogo
import com.yft.rippleup.ui.components.noRippleClickable
import com.yft.rippleup.ui.theme.*

/** p04/p29 — interest chips (3 horizontally scrollable rows), Continue. */
@Composable
fun PersonaliseScreen(onContinue: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf(Content.defaultInterests) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDFEFE))
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Mint),
                contentAlignment = Alignment.Center,
            ) {
                Text("‹", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RippleLogo(size = 30.dp)
            Spacer(Modifier.width(8.dp))
            Text("Personalisation", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold), color = Ink)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Add your interests so we can personalise your Ripple actions for you.",
            style = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
            color = Secondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(30.dp))

        val rows = listOf(
            Content.interests.take(5),
            Content.interests.drop(5) + Content.interests.take(2),
            Content.interests.drop(2).take(5),
        )
        rows.forEachIndexed { r, chips ->
            val offset = if (r == 0) (-30).dp else if (r == 2) (-14).dp else 6.dp
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Spacer(Modifier.width(offset))
                chips.forEach { chip ->
                    val idx = Content.interests.indexOf(chip)
                    val isSel = idx in selected
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(21.dp))
                            .background(if (isSel) MintChip else Color.White)
                            .border(
                                width = if (isSel) 1.5.dp else 1.dp,
                                color = if (isSel) Teal else Color(0x19000000),
                                shape = RoundedCornerShape(21.dp),
                            )
                            .noRippleClickable {
                                selected = if (isSel) selected - idx else selected + idx
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val parts = chip.split(" ")
                            Text(parts.first(), fontSize = 14.sp)
                            Spacer(Modifier.width(7.dp))
                            Text(
                                parts.drop(1).joinToString(" "),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSel) Teal else Ink,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.weight(1f))
        Text(
            "${selected.size} interests selected ✓",
            color = Teal,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(14.dp))
        GradientButton("Continue", modifier = Modifier.fillMaxWidth()) { onContinue() }
        Spacer(Modifier.height(26.dp))
    }
}

