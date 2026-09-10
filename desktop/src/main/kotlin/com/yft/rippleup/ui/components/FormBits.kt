package com.yft.rippleup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yft.rippleup.ui.theme.*

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
        Row(Modifier.fillMaxSize().padding(6.dp)) {
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
                    Text(label, color = if (active) Teal else Secondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Two-segment control used by Rewards (Rewards / Badges). */
@Composable
fun RewardTabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(46.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
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
                Text(label, color = if (active) Color.White else Secondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(text, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 7.dp))
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
                        if (value.isEmpty()) Text(placeholder, color = Color(0xFF8F8F8F), fontSize = 14.sp)
                        inner()
                    }
                    trailing?.invoke()
                }
            },
        )
    }
}
