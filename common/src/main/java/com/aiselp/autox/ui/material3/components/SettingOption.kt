package com.aiselp.autox.ui.material3.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape


@Composable
fun SettingOptionSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: @Composable (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        color = Color(0x0DFFFFFF), // white/5
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                it.invoke()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = Color(0xB3FFFFFF), // white/70
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            KineticBorderSwitch(checked = checked)
        }
    }
}

@Composable
private fun KineticBorderSwitch(checked: Boolean) {
    // Match stitch_script_automation_app/_5/code.html kinetic toggle.
    val trackShape = RoundedCornerShape(10.dp)
    val trackBg = if (checked) Color(0x1A9CFF93) else Color(0x0DFFFFFF) // kinetic/10 vs white/5
    val trackBorder = if (checked) Color(0x4D9CFF93) else Color(0x1AFFFFFF) // kinetic/30 vs white/10
    val thumbBg = if (checked) Color(0xFF9CFF93) else Color(0x33FFFFFF) // kinetic vs white/20

    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 20.dp)
            .clip(trackShape)
            .background(trackBg, trackShape)
            .border(1.dp, trackBorder, trackShape)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    )
    {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(thumbBg)
        )
    }
}

@Composable
fun SettingOptionSwitch(
    title: String,
    value: MutableState<Boolean>,
    icon: ImageVector? = null,
    tint: Color = LocalContentColor.current
) {
    SettingOptionSwitch(title, value.value, { value.value = it }, icon?.let {
        { Icon(imageVector = it, contentDescription = null,tint = tint) }
    })
}

@Composable
fun SettingOptionSwitch(
    title: String,
    icon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tint: Color = LocalContentColor.current
) {
    SettingOptionSwitch(title, checked, onCheckedChange, icon?.let {
        { Icon(imageVector = it, contentDescription = null,tint = tint) }
    })
}