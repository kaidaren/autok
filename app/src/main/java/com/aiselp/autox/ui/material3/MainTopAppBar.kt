package com.aiselp.autox.ui.material3

import android.content.Intent
import android.os.Build
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import com.aiselp.autojs.codeeditor.EditActivity
import com.aiselp.autox.ui.material3.components.MenuTopAppBar
import org.autojs.autojs.ui.log.LogActivityKt
import org.autojs.autojs.ui.main.BottomNavigationItem
import org.autojs.autojs.ui.main.scripts.ScriptListFragment
import org.autojs.autoxjs.R


@Composable
fun MainTopAppBar(
    openMenuRequest: () -> Unit,
    showCreateEntry: Boolean = false,
    actions: @Composable () -> Unit = {}
) {
    MenuTopAppBar(
        title = stringResource(id = R.string.main_top_bar_title),
        openMenuRequest = openMenuRequest,
        actions = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                EditorButton()
            }
            if (showCreateEntry) {
                CreateButton()
            }
            LogButton()
            actions()
        }
    )
}

@Composable
private fun EditorButton() {
    val context = LocalContext.current
    IconButton(onClick = {
        context.startActivity(Intent(context, EditActivity::class.java))
    }) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "editor",
            tint = Color(0xFF8A919C)
        )
    }
}

@Composable
private fun CreateButton() {
    val context = LocalContext.current
    IconButton(
        onClick = {
            LocalBroadcastManager.getInstance(context).sendBroadcast(
                Intent(ScriptListFragment.ACTION_SHOW_CREATE_DIALOG)
            )
        }
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "create",
            tint = Color(0xFF8A919C)
        )
    }
}

//主界面日志按钮
@Composable
private fun LogButton() {
    val context = LocalContext.current
    IconButton(onClick = { LogActivityKt.start(context) }) {
        Icon(
            painter = painterResource(id = R.drawable.ic_logcat),
            contentDescription = stringResource(id = R.string.text_logcat),
            tint = Color(0xFF8A919C)
        )
    }
}

@Composable
fun BottomBar(
    items: List<BottomNavigationItem>,
    currentSelected: Int,
    onSelectedChange: (Int) -> Unit
) {
    val bg = Color(0xFF020408)
    val active = Color(0xFF9CFF93)
    val inactive = Color(0x4DFFFFFF) // white/30

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == currentSelected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectedChange(index) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = item.icon),
                    contentDescription = item.label,
                    tint = if (selected) active else inactive,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.label,
                    fontSize = 10.sp,
                    color = if (selected) active else inactive,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}