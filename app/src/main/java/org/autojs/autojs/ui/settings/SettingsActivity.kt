package org.autojs.autojs.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.util.Pair
import com.google.android.material.appbar.MaterialToolbar
import com.stardust.autojs.IndependentScriptService
import com.stardust.autojs.servicecomponents.EngineController
import com.stardust.theme.app.ColorSelectActivity
import com.stardust.theme.app.ColorSelectActivity.ColorItem
import com.stardust.theme.util.ListBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.autojs.autojs.Pref
import org.autojs.autojs.tool.AccessibilityServiceTool
import org.autojs.autojs.ui.floating.FloatyWindowManger
import org.autojs.autojs.ui.settings.LicenseInfo.install
import org.autojs.autoxjs.R
import com.stardust.view.accessibility.AccessibilityService


/**
 * Created by Stardust on 2017/2/2.
 * update by aaron 2022年1月16日
 */
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SettingsScreen() }
    }

    @Composable
    private fun SettingsScreen() {
        val scope = rememberCoroutineScope()
        var accessibilityEnabled by remember { mutableStateOf(AccessibilityServiceTool.isAccessibilityServiceEnabled(this)) }
        var foregroundServiceEnabled by remember { mutableStateOf(Pref.isForegroundServiceEnabled()) }
        var floatingButtonEnabled by remember { mutableStateOf(Pref.isFloatingMenuShown()) }
        var pointerLocationEnabled by remember { mutableStateOf(false) }
        var clientModeEnabled by remember { mutableStateOf(false) }
        var serverModeEnabled by remember { mutableStateOf(false) }
        var publishNotificationEnabled by remember { mutableStateOf(NotificationManagerCompat.from(this).areNotificationsEnabled()) }
        var notificationReadEnabled by remember { mutableStateOf(false) }
        val activeTasks by produceState(initialValue = 0) {
            while (true) {
                value = runCatching {
                    EngineController.getAllScriptTasks().await().count { it.isRunning }
                }.getOrDefault(0)
                delay(2000)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF020408))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_hierarchy),
                    contentDescription = null,
                    tint = Color(0xFF8A919C),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "KINETIC_CONSOLE",
                    color = Color(0xFF8A919C),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0x0DFFFFFF)))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                SectionTitle("服务 (SERVICES)")
                SettingItem("无障碍服务", R.drawable.ic_accessibility, accessibilityEnabled) {
                    if (!accessibilityEnabled) {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    } else {
                        AccessibilityService.disable()
                    }
                    accessibilityEnabled = !accessibilityEnabled
                }
                SettingItem("前台服务", R.drawable.ic_performance, foregroundServiceEnabled) {
                    foregroundServiceEnabled = !foregroundServiceEnabled
                    Pref.def().edit().putBoolean("key_foreground_service", foregroundServiceEnabled).apply()
                    if (foregroundServiceEnabled) IndependentScriptService.startForeground(this@SettingsActivity)
                    else IndependentScriptService.stopForeground(this@SettingsActivity)
                }

                SectionTitle("工具 (TOOLS)")
                SettingItem("浮动按钮", R.drawable.ic_automation, floatingButtonEnabled) {
                    floatingButtonEnabled = !floatingButtonEnabled
                    if (floatingButtonEnabled) {
                        if (!FloatyWindowManger.showCircularMenu()) {
                            floatingButtonEnabled = false
                        }
                    } else {
                        FloatyWindowManger.hideCircularMenu()
                    }
                    Pref.setFloatingMenuShown(floatingButtonEnabled)
                }
                SettingItem("指针位置", R.drawable.ic_filter, pointerLocationEnabled) {
                    pointerLocationEnabled = !pointerLocationEnabled
                }

                SectionTitle("连接到计算机 (CONNECTIVITY)")
                SettingItem("客户端模式", R.drawable.ic_search, clientModeEnabled) {
                    clientModeEnabled = !clientModeEnabled
                }
                SettingItem("服务端模式", R.drawable.ic_hierarchy, serverModeEnabled) {
                    serverModeEnabled = !serverModeEnabled
                }

                SectionTitle("权限 (PERMISSIONS)")
                SettingItem("发布通知权限", R.drawable.ic_more_vert, publishNotificationEnabled) {
                    startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        }
                    )
                    publishNotificationEnabled = NotificationManagerCompat.from(this@SettingsActivity).areNotificationsEnabled()
                }
                SettingItem("通知读取权限", R.drawable.ic_accessibility, notificationReadEnabled) {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    notificationReadEnabled = !notificationReadEnabled
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF020408))
            ) {
                Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0x0DFFFFFF)))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ACTIVE STATUS", color = Color(0x4DFFFFFF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("任务", color = Color(0xFF9CFF93), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (activeTasks > 0) Color(0xFF9CFF93) else Color(0xFF4A4F58))
                        )
                    }
                }
                Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0x0DFFFFFF)))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    BottomAction("插件", iconRes = R.drawable.ic_automation) {}
                    BottomAction("重启", iconRes = R.drawable.ic_performance) { recreate() }
                    BottomAction("退出", iconRes = R.drawable.ic_more_vert) {
                        scope.launch { EngineController.appExit() }
                        finish()
                    }
                }
            }
        }
    }

    @Composable
    private fun SectionTitle(text: String) {
        Text(
            text = text,
            color = Color(0x33FFFFFF),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.12.sp,
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 6.dp)
        )
    }

    @Composable
    private fun SettingItem(title: String, iconRes: Int, checked: Boolean, onToggle: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onToggle() }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = iconRes),
                contentDescription = null,
                tint = Color(0x66FFFFFF),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = Color(0xB3FFFFFF),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            KineticSwitch(checked = checked)
        }
    }

    @Composable
    private fun KineticSwitch(checked: Boolean) {
        val trackShape = RoundedCornerShape(10.dp)
        val trackBg = if (checked) Color(0x1A9CFF93) else Color(0x0DFFFFFF) // kinetic/10 vs white/5
        val trackBorder = if (checked) Color(0x4D9CFF93) else Color(0x1AFFFFFF) // kinetic/30 vs white/10
        val thumbBg = if (checked) Color(0xFF9CFF93) else Color(0x33FFFFFF) // kinetic vs white/20
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(20.dp)
                .clip(trackShape)
                .background(trackBg)
                .border(1.dp, trackBorder, trackShape)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(thumbBg)
            )
        }
    }

    @Composable
    private fun BottomAction(label: String, iconRes: Int, onClick: () -> Unit) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onClick() }
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = iconRes),
                contentDescription = null,
                tint = Color(0x4DFFFFFF),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = Color(0x4DFFFFFF), fontSize = 10.sp)
        }
    }


    companion object {
        init {
            install()
        }

        private val COLOR_ITEMS = ListBuilder<Pair<Int, Int>>()
            .add(Pair(R.color.theme_color_red, R.string.theme_color_red))
            .add(Pair(R.color.theme_color_pink, R.string.theme_color_pink))
            .add(Pair(R.color.theme_color_purple, R.string.theme_color_purple))
            .add(Pair(R.color.theme_color_dark_purple, R.string.theme_color_dark_purple))
            .add(Pair(R.color.theme_color_indigo, R.string.theme_color_indigo))
            .add(Pair(R.color.theme_color_blue, R.string.theme_color_blue))
            .add(Pair(R.color.theme_color_light_blue, R.string.theme_color_light_blue))
            .add(Pair(R.color.theme_color_blue_green, R.string.theme_color_blue_green))
            .add(Pair(R.color.theme_color_cyan, R.string.theme_color_cyan))
            .add(Pair(R.color.theme_color_green, R.string.theme_color_green))
            .add(Pair(R.color.theme_color_light_green, R.string.theme_color_light_green))
            .add(Pair(R.color.theme_color_yellow_green, R.string.theme_color_yellow_green))
            .add(Pair(R.color.theme_color_yellow, R.string.theme_color_yellow))
            .add(Pair(R.color.theme_color_amber, R.string.theme_color_amber))
            .add(Pair(R.color.theme_color_orange, R.string.theme_color_orange))
            .add(Pair(R.color.theme_color_dark_orange, R.string.theme_color_dark_orange))
            .add(Pair(R.color.theme_color_brown, R.string.theme_color_brown))
            .add(Pair(R.color.theme_color_gray, R.string.theme_color_gray))
            .add(Pair(R.color.theme_color_blue_gray, R.string.theme_color_blue_gray))
            .list()

        fun selectThemeColor(context: Context) {
            val colorItems: MutableList<ColorItem> = ArrayList(COLOR_ITEMS.size)
            for (item in COLOR_ITEMS) {
                colorItems.add(
                    ColorItem(
                        context.getString(item.second),
                        context.resources.getColor(item.first)
                    )
                )
            }
            ColorSelectActivity.startColorSelect(
                context,
                context.getString(R.string.mt_color_picker_title),
                colorItems
            )
        }
    }
}
