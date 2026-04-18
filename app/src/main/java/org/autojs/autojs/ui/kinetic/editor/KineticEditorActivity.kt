package org.autojs.autojs.ui.kinetic.editor

import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Bundle
import android.os.Build
import android.util.Log
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.stardust.autojs.servicecomponents.EngineController
import com.stardust.autojs.util.PermissionUtil
import com.stardust.autojs.util.StoragePermissionResultContract
import com.stardust.toast
import org.autojs.autojs.model.script.Scripts
import org.autojs.autojs.ui.edit.editor.CodeEditor
import org.autojs.autojs.ui.edit.theme.Theme
import org.autojs.autoxjs.R
import java.io.File

class KineticEditorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Page() }
    }

    @Composable
    private fun Page() {
        val context = LocalContext.current
        val path = intent.getStringExtra(EXTRA_PATH)
        val fileName = intent.getStringExtra(EXTRA_NAME) ?: (path?.let { File(it).name } ?: "index.js")
        val file = remember(path) { path?.let { File(it) } }
        val editorRef = remember { mutableStateOf<CodeEditor?>(null) }
        val ready = remember { mutableStateOf(false) }
        var isRunning by remember { mutableStateOf(false) }
        var runningTaskId by remember { mutableStateOf<Int?>(null) }
        var pendingAfterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }

        val requestRStorageLauncher =
            androidx.activity.compose.rememberLauncherForActivityResult(contract = StoragePermissionResultContract()) { allAllow ->
                if (allAllow) {
                    pendingAfterPermission?.invoke()
                } else {
                    toast(context, "没有存储权限，无法运行")
                }
                pendingAfterPermission = null
            }

        val requestLegacyStorageLauncher =
            androidx.activity.compose.rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { result ->
                val allAllow = result.values.all { it }
                if (allAllow) {
                    pendingAfterPermission?.invoke()
                } else {
                    toast(context, "没有存储权限，无法运行")
                }
                pendingAfterPermission = null
            }

        LaunchedEffect(file) {
            ready.value = file?.exists() == true
            if (!ready.value) {
                toast(context, "无法打开文件: $fileName")
                finish()
            }
        }
        DisposableEffect(file?.path) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context0: Context?, intent: Intent?) {
                    if (intent?.action != Scripts.ACTION_ON_EXECUTION_FINISHED) return
                    runOnUiThread {
                        isRunning = false
                        runningTaskId = null
                    }
                }
            }
            val filter = IntentFilter(Scripts.ACTION_ON_EXECUTION_FINISHED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(receiver, filter)
            }
            onDispose {
                runCatching { unregisterReceiver(receiver) }
            }
        }

        fun ensureStoragePermissionThen(action: () -> Unit) {
            if (PermissionUtil.checkStoragePermission()) {
                action()
                return
            }
            pendingAfterPermission = action
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                PermissionUtil.showPermissionDialog(context) { requestRStorageLauncher.launch(Unit) }
            } else {
                PermissionUtil.showPermissionDialog(context) {
                    requestLegacyStorageLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                }
            }
        }

        fun save(): Boolean {
            val f = file ?: return false
            return try {
                val editor = editorRef.value ?: return false
                f.writeText(editor.text)
                true
            } catch (_: Exception) {
                false
            }
        }

        fun run() {
            val f = file ?: return
            ensureStoragePermissionThen {
                if (!save()) {
                    toast(context, "保存失败，无法运行")
                    return@ensureStoragePermissionThen
                }
                try {
                    if (isRunning) {
                        toast(context, "脚本正在运行中")
                        return@ensureStoragePermissionThen
                    }
                    isRunning = true
                    toast(context, "开始运行: ${f.name}")
                    // Use built-in editor execution route so built-in modules are available.
                    val execution = Scripts.runWithBroadcastSender(f)
                    runningTaskId = execution.id
                } catch (e: Throwable) {
                    isRunning = false
                    runningTaskId = null
                    Log.e("KineticEditorActivity", e.stackTraceToString())
                    val rhino = Scripts.getRhinoException(e)
                    val msg = when {
                        rhino != null && !rhino.message.isNullOrBlank() -> rhino.message
                        !e.message.isNullOrBlank() -> e.message
                        else -> e.javaClass.simpleName
                    }
                    toast(context, "运行失败: $msg")
                }
            }
        }

        fun stopRun() {
            val id = runningTaskId
            if (!isRunning || id == null || id <= 0) {
                toast(context, "当前没有可停止的运行任务")
                return
            }
            EngineController.stopScript(id)
            isRunning = false
            runningTaskId = null
            toast(context, "已停止运行")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0E14))
        ) {
            TopAppBar(
                onSave = {
                    ensureStoragePermissionThen {
                        toast(
                            context,
                            if (save()) "已保存" else "保存失败"
                        )
                    }
                },
                isRunning = isRunning,
                onRun = { run() },
                onStopRun = { stopRun() }
            )

            // Editor area (uses existing CodeEditor for real editing).
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0A0E14))
            ) {
                if (ready.value) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            CodeEditor(ctx).apply {
                                // Force dark theme to match Kinetic design colors.
                                setTheme(Theme.fromAssetsJson(ctx, "editor/theme/dark_plus.json"))
                                setInitialText(file!!.readText())
                                editorRef.value = this
                            }
                        }
                    )
                }
            }

            // Bottom quick insert bar (Auto.js common snippets)
            QuickInsertBar(
                onInsert = { editorRef.value?.insert(it) }
            )
        }
    }

    @Composable
    private fun TopAppBar(
        onSave: () -> Unit,
        isRunning: Boolean,
        onRun: () -> Unit,
        onStopRun: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color(0xFF0A0E14))
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { finish() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_right),
                        contentDescription = null,
                        tint = Color(0xFFA8ABB3),
                        modifier = Modifier
                            .size(26.dp)
                            .graphicsLayer(rotationZ = 180f)
                    )
                }
                Icon(
                    painter = painterResource(id = R.drawable.ic_automation),
                    contentDescription = null,
                    tint = Color(0xFF9CFF93),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "编辑器",
                    color = Color(0xFF9CFF93),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onSave,
                    modifier = Modifier
                        .height(20.dp)
                        .background(Color(0xFF20262F), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0x1A44484F), RoundedCornerShape(10.dp)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more_vert),
                        contentDescription = null,
                        tint = Color(0xFFA8ABB3),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "保存",
                        color = Color(0xFFA8ABB3),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                TextButton(
                    onClick = { if (isRunning) onStopRun() else onRun() },
                    modifier = Modifier
                        .height(20.dp)
                        .background(
                            Brush.linearGradient(
                                if (isRunning) {
                                    listOf(Color(0xFFFF9F9F), Color(0xFFFF4D4D))
                                } else {
                                    listOf(Color(0xFF9CFF93), Color(0xFF00FC40))
                                }
                            ),
                            RoundedCornerShape(10.dp)
                        ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_performance),
                        contentDescription = null,
                        tint = if (isRunning) Color(0xFF4A0000) else Color(0xFF00440A),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRunning) "停止" else "运行",
                        color = if (isRunning) Color(0xFF4A0000) else Color(0xFF00440A),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    @Composable
    private fun FileInfoBar(fileName: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(0xFF0F141A))
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .background(Color(0xFF151A21))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "JS", color = Color(0xFF00F4FE), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = fileName, color = Color(0xFFF1F3FC), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "UTF-8", color = Color(0xFF72757D), fontSize = 10.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "JAVASCRIPT", color = Color(0xFF00EC3B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }

    @Composable
    private fun QuickInsertBar(onInsert: (String) -> Unit) {
        val scroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xCC1B2028))
                .border(1.dp, Color(0x1A44484F))
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .horizontalScroll(scroll),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                "{" to "{",
                "}" to "}",
                "(" to "(",
                ")" to ")",
                "[" to "[",
                "]" to "]",
                ";" to ";",
                "=" to "=",
                "text" to "text(\"\")",
                "id" to "id(\"\")",
                "desc" to "desc(\"\")",
                "click" to "click()",
                "longClick" to "longClick()",
                "sleep" to "sleep(1000)",
                "toast" to "toast(\"\")",
                "back" to "back()",
                "home" to "home()",
                "swipe" to "swipe(0, 0, 0, 0, 500)",
                "setText" to "setText(\"\")",
                "findOne" to "findOne(1000)"
            )
            for ((label, snippet) in items) {
                KeyButton(
                    label = label,
                    color = if (label.length <= 2) Color(0xFF00EC3B) else Color(0xFF9CFF93),
                    wide = label.length > 2
                ) { onInsert(snippet) }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }

    @Composable
    private fun KeyButton(label: String, color: Color, wide: Boolean = false, onClick: () -> Unit) {
        TextButton(
            onClick = onClick,
            modifier = Modifier
                .height(40.dp)
                .then(if (wide) Modifier.width(78.dp) else Modifier.width(40.dp))
                .background(Color(0xFF0F141A), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0x3344484F), RoundedCornerShape(8.dp)),
        ) {
            Text(
                text = label,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    @Composable
    private fun BottomNav(onGoLibrary: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color(0xE60F141A))
                .border(1.dp, Color(0x2644484F)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NavItem(iconRes = R.drawable.ic_home, label = "脚本库", selected = false, onClick = onGoLibrary)
            NavItem(iconRes = R.drawable.ic_manage, label = "编辑器", selected = true, onClick = {})
            NavItem(iconRes = R.drawable.ic_web, label = "计划", selected = false, onClick = {})
            NavItem(iconRes = R.drawable.ic_more_vert, label = "日志", selected = false, onClick = {})
        }
    }

    @Composable
    private fun NavItem(iconRes: Int, label: String, selected: Boolean, onClick: () -> Unit) {
        val color = if (selected) Color(0xFF9CFF93) else Color(0x66F1F3FC)
        TextButton(onClick = onClick) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    companion object {
        const val EXTRA_PATH = "path"
        const val EXTRA_NAME = "name"

        fun open(context: Context, path: String, name: String? = null) {
            val i = Intent(context, KineticEditorActivity::class.java)
                .putExtra(EXTRA_PATH, path)
                .putExtra(EXTRA_NAME, name)
            if (context !is android.app.Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }
}

