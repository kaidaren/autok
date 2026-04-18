package org.autojs.autojs.ui.main.scripts

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import android.text.format.Formatter
import com.aiselp.autox.ui.material3.components.BaseDialog
import com.aiselp.autox.ui.material3.components.CheckboxOption
import com.aiselp.autox.ui.material3.components.DialogController
import com.aiselp.autox.ui.material3.components.DialogTitle
import com.google.android.material.snackbar.Snackbar
import com.stardust.app.GlobalAppContext.get
import com.stardust.autojs.servicecomponents.EngineController
import com.stardust.pio.PFiles
import com.stardust.util.IntentUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.autojs.autojs.Pref
import org.autojs.autojs.external.fileprovider.AppFileProvider
import org.autojs.autojs.model.explorer.ExplorerDirPage
import org.autojs.autojs.model.explorer.ExplorerFileItem
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.model.script.Scripts.edit
import org.autojs.autojs.ui.build.ProjectConfigActivity
import org.autojs.autojs.ui.common.ScriptOperations
import org.autojs.autojs.ui.explorer.ExplorerViewKt
import org.autojs.autojs.ui.viewmodel.ExplorerItemList.SortConfig
import org.autojs.autojs.ui.widget.fillMaxSize
import org.autojs.autoxjs.R
import com.aiselp.autox.ui.material3.components.ComposeDialog
import com.stardust.app.DialogUtils
import com.aiselp.autox.ui.material3.components.DialogText
import com.aiselp.autox.ui.material3.components.AlertDialog
import java.io.File
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Created by wilinz on 2022/7/15.
 */
class ScriptListFragment : Fragment() {

    val explorerView by lazy { ExplorerViewKt(this.requireContext()) }
    private var createReceiver: BroadcastReceiver? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        explorerView.setUpViews()
        return ComposeView(requireContext()).apply {
            setContent {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF0A0E14),
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(it)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        item {
                            HomeHeader()
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        item {
                    AndroidView(
                                // Keep enough height for folder/file area, then allow page to continue below.
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(420.dp),
                        factory = { explorerView }
                    )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (createReceiver == null) {
            createReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: android.content.Context, intent: Intent) {
                    if (intent.action == ACTION_SHOW_CREATE_DIALOG) {
                        showCreateOptionsDialog()
                    }
                }
            }
        }
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            createReceiver!!,
            IntentFilter(ACTION_SHOW_CREATE_DIALOG)
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun showCreateOptionsDialog() {
        val context = requireContext()
        val dialog = ComposeDialog(context) {
            BasicAlertDialog(onDismissRequest = { dismiss() }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121820)),
                    border = BorderStroke(1.dp, Color(0x3344484F))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "新建",
                            color = Color(0xFFF1F3FC),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CreateOptionItem("文件夹") {
                            dismiss()
                            showCreateFolderDialog()
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        CreateOptionItem("文件") {
                            dismiss()
                            showCreateFileDialog()
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        CreateOptionItem("导入") {
                            dismiss()
                            getScriptOperations(context, this@ScriptListFragment).importFile()
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        CreateOptionItem("项目") {
                            dismiss()
                            val dir = explorerView.currentPage?.toScriptFile()
                            if (dir != null) {
                                ProjectConfigActivity.newProject(context, dir)
                            } else {
                                Snackbar.make(explorerView, R.string.text_create_fail, Snackbar.LENGTH_SHORT).show()
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "取消",
                            color = Color(0xFF00F4FE),
                            fontSize = 18.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dismiss() }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
        DialogUtils.showDialog(dialog)
    }

    @Composable
    private fun CreateOptionItem(title: String, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F141A)),
            border = BorderStroke(1.dp, Color(0x2644484F))
        ) {
            Text(
                text = title,
                color = Color(0xFFF1F3FC),
                fontSize = 18.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
    }

    private fun showCreateFolderDialog() {
        val context = requireContext()
        val dialog = ComposeDialog(context) {
            val nameState = remember { mutableStateOf("") }
            BaseDialog(
                title = { DialogTitle("新建文件夹") },
                positiveText = context.getString(R.string.ok),
                onPositiveClick = {
                    dismiss()
                    val name = nameState.value.trim()
                    val dir = explorerView.currentPage?.toScriptFile()
                    if (name.isEmpty() || dir == null) {
                        showSnackbar(explorerView, R.string.text_create_fail)
                        return@BaseDialog
                    }
                    val newDir = File(dir, name)
                    if (newDir.exists()) {
                        showSnackbar(explorerView, R.string.text_file_exists)
                        return@BaseDialog
                    }
                    if (newDir.mkdirs()) {
                        Explorers.workspace().notifyItemCreated(ExplorerDirPage(newDir, explorerView.currentPage))
                        showSnackbar(explorerView, R.string.text_already_create)
                    } else {
                        showSnackbar(explorerView, R.string.text_create_fail)
                    }
                },
                negativeText = context.getString(R.string.cancel),
                onNegativeClick = { dismiss() }
            ) {
                TextField(
                    value = nameState.value,
                    onValueChange = { nameState.value = it },
                    label = { Text(context.getString(R.string.text_please_input_name)) }
                )
            }
        }
        DialogUtils.showDialog(dialog)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun showCreateFileDialog() {
        val context = requireContext()
        val dialog = ComposeDialog(context) {
            BasicAlertDialog(onDismissRequest = { dismiss() }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121820)),
                    border = BorderStroke(1.dp, Color(0x3344484F))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "新建文件",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CreateOptionItem("JavaScript (.js)") {
                            dismiss()
                            getScriptOperations(context, this@ScriptListFragment).newScriptFileForScript(null)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        CreateOptionItem("ES模块 (.mjs)") {
                            dismiss()
                            createMjsFileViaDialog()
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = context.getString(R.string.cancel),
                            color = Color.White,
                            fontSize = 18.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dismiss() }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
        DialogUtils.showDialog(dialog)
    }

    private fun createMjsFileViaDialog() {
        val context = requireContext()
        val dialog = ComposeDialog(context) {
            val nameState = remember { mutableStateOf("") }
            BaseDialog(
                title = { DialogTitle("新建 .mjs 文件") },
                positiveText = context.getString(R.string.ok),
                onPositiveClick = {
                    dismiss()
                    val name = nameState.value.trim()
                    val dir = explorerView.currentPage?.toScriptFile()
                    if (name.isEmpty() || dir == null) {
                        showSnackbar(explorerView, R.string.text_create_fail)
                        return@BaseDialog
                    }
                    val file = File(dir, "$name.mjs")
                    PFiles.createIfNotExists(file.path)
                    Explorers.workspace().notifyItemCreated(ExplorerFileItem(file, explorerView.currentPage))
                    showSnackbar(explorerView, R.string.text_already_create)
                },
                negativeText = context.getString(R.string.cancel),
                onNegativeClick = { dismiss() }
            ) {
                TextField(
                    value = nameState.value,
                    onValueChange = { nameState.value = it },
                    label = { Text(context.getString(R.string.text_please_input_name)) }
                )
            }
        }
        DialogUtils.showDialog(dialog)
    }

    @Composable
    private fun HomeHeader() {
        val context = LocalContext.current
        val deviceInfo = remember(context) { collectDeviceInfo(context) }
        val runtimeStats by produceState(initialValue = RuntimeStats(0)) {
            while (true) {
                val runtime = Runtime.getRuntime()
                val used = runtime.totalMemory() - runtime.freeMemory()
                val load = if (runtime.maxMemory() > 0) {
                    ((used * 100f) / runtime.maxMemory()).toInt().coerceIn(0, 100)
                } else 0
                value = RuntimeStats(load)
                delay(2000)
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier
                    .width(220.dp)
                    .height(178.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121820))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "基本信息",
                        color = Color(0xFFF1F3FC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DeviceInfoRow("屏幕宽度", deviceInfo.screenWidth)
                    DeviceInfoRow("屏幕高度", deviceInfo.screenHeight)
                    DeviceInfoRow("DPI", deviceInfo.dpi)
                    DeviceInfoRow("IP", deviceInfo.ip)
                    DeviceInfoRow("品牌", deviceInfo.brand)
                    DeviceInfoRow("型号", deviceInfo.model)
                    DeviceInfoRow("设备UUID", deviceInfo.deviceUuid)
                    DeviceInfoRow("当前版本号", deviceInfo.sdkInt)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Card(
                modifier = Modifier
                    .width(110.dp)
                    .height(178.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121820))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_performance),
                        contentDescription = null,
                        tint = Color(0xFF00F4FE),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "负载", color = Color(0xFFA8ABB3), fontSize = 9.sp)
                    Text(
                        text = "${runtimeStats.loadPercent}%",
                        color = Color(0xFF00F4FE),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "脚本库",
                color = Color(0xFFF1F3FC),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "根目录 / 脚本 / 第一卷",
                color = Color(0xFFA8ABB3),
                fontSize = 9.sp
            )
        }
    }

    @Composable
    private fun DeviceInfoRow(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
        ) {
            Text(
                text = "$label:",
                color = Color(0xFFA8ABB3),
                fontSize = 9.sp,
                modifier = Modifier.width(62.dp)
            )
            Text(
                text = value,
                color = Color(0xFFF1F3FC),
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }

    private fun collectDeviceInfo(context: Context): DeviceInfo {
        val dm = context.resources.displayMetrics
        val ip = runCatching {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            val wifiIp = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (wifiIp != 0) Formatter.formatIpAddress(wifiIp) else ""
        }.getOrNull().orEmpty().ifBlank {
            runCatching {
                NetworkInterface.getNetworkInterfaces().toList()
                    .flatMap { it.inetAddresses.toList() }
                    .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains(":") == false }
                    ?.hostAddress
            }.getOrNull().orEmpty().ifBlank { "-" }
        }
        val androidId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull().orEmpty().ifBlank { "-" }
        return DeviceInfo(
            screenWidth = dm.widthPixels.toString(),
            screenHeight = dm.heightPixels.toString(),
            dpi = dm.densityDpi.toString(),
            ip = ip,
            brand = Build.BRAND ?: "-",
            model = Build.MODEL ?: "-",
            deviceUuid = androidId,
            sdkInt = Build.VERSION.SDK_INT.toString()
        )
    }

    data class RuntimeStats(
        val loadPercent: Int
    )

    data class DeviceInfo(
        val screenWidth: String,
        val screenHeight: String,
        val dpi: String,
        val ip: String,
        val brand: String,
        val model: String,
        val deviceUuid: String,
        val sdkInt: String
    )

    @Composable
    private fun FloatingButton() {
        Column(
            modifier = Modifier.padding(end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.End
        ) {
            var expand by remember { mutableStateOf(false) }
            val rotate by animateFloatAsState(
                if (!expand) 0f else 360f,
                label = "FloatingActionButton"
            )
            AnimatedVisibility(expand) {
                Actions { expand = false }
                Spacer(modifier = Modifier.height(16.dp))
            }

            FloatingActionButton(
                onClick = { expand = !expand },
                modifier = Modifier.rotate(rotate),
                containerColor = Color(0xFF8FD7EA),
                contentColor = Color(0xFF0A2330)
            ) {
                if (expand) {
                    Icon(Icons.Default.Close, null)
                } else Icon(
                    Icons.Default.Add,
                    null,
                )
            }
        }
    }

    @Composable
    fun Actions(closeRequest: () -> Unit) {
        val context = LocalContext.current
        val spacerModifier = Modifier.height(12.dp)
        Column(horizontalAlignment = Alignment.End) {
            NewDirectory(closeRequest)
            Spacer(modifier = spacerModifier)
            NewFile(closeRequest)
            Spacer(modifier = spacerModifier)
            ImportFile(context, closeRequest)
            Spacer(modifier = spacerModifier)
            NewProject(context, closeRequest)
            Spacer(modifier = spacerModifier)
        }
    }

    @Composable
    private fun NewProject(context: Context, closeRequest: () -> Unit) {
        ExtendedFloatingActionButton(text = { Text(text = stringResource(id = R.string.text_project)) },
            icon = {
                Icon(
                    painterResource(id = R.drawable.ic_project2),
                    contentDescription = null,
                    tint = Color(0xFF09ECBF)
                )
            },
            onClick = {
                closeRequest()
                val explorerView = this@ScriptListFragment.explorerView
                ProjectConfigActivity.newProject(context, explorerView.currentPage!!.toScriptFile())
            })
    }


    @Composable
    private fun ImportFile(context: Context, closeRequest: () -> Unit) {
        ExtendedFloatingActionButton(text = { Text(text = stringResource(id = R.string.text_import)) },
            icon = {
                Icon(
                    painterResource(id = R.drawable.ic_floating_action_menu_open),
                    contentDescription = null,
                    tint = Color(0xFF831DDD)
                )
            },
            onClick = {
                closeRequest()
                getScriptOperations(
                    context, this@ScriptListFragment
                ).importFile()
            })
    }

    @Composable
    private fun NewFile(closeRequest: () -> Unit) {
        val dialog = remember { DialogController() }
        var name by remember { mutableStateOf("") }
        var jsFile by remember { mutableStateOf(false) }
        var mjsFile by remember { mutableStateOf(false) }

        dialog.BaseDialog(onDismissRequest = { dialog.dismiss() }, title = {
            DialogTitle(title = stringResource(R.string.text_name))
        }, positiveText = stringResource(R.string.ok), onPositiveClick = {
            closeRequest();dialog.dismiss()
            val dir = explorerView.currentPage?.toScriptFile()
            if (name.isEmpty()) {
                showSnackbar(explorerView, R.string.text_file_name_cannot_be_empty)
                return@BaseDialog
            }
            if (dir != null) {
                var fileName = name
                if (jsFile) fileName += ".js"
                if (mjsFile) fileName += ".mjs"
                val file = File(dir, fileName)
                PFiles.createIfNotExists(file.path)
                Explorers.workspace()
                    .notifyItemCreated(ExplorerFileItem(file, explorerView.currentPage))
                showSnackbar(explorerView, R.string.text_already_create)
            } else {
                showSnackbar(explorerView, R.string.text_create_fail)
            }
        }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.text_please_input_name)) },
                    suffix = {
                        if (jsFile || mjsFile) {
                            Text(text = ".${if (jsFile) "js" else "mjs"}")
                        }
                    }
                )
                Row {
                    CheckboxOption(
                        modifier = Modifier,
                        checked = jsFile,
                        onCheckedChange = {
                            jsFile = it
                            if (it && mjsFile) mjsFile = false
                        },
                        name = stringResource(R.string.text_js_file)
                    )
                    CheckboxOption(
                        modifier = Modifier,
                        checked = mjsFile,
                        onCheckedChange = {
                            mjsFile = it
                            if (it && jsFile) jsFile = false
                        },
                        name = stringResource(R.string.text_mjs_file)
                    )
                }
            }
        }
        ExtendedFloatingActionButton(text = { Text(text = stringResource(id = R.string.text_file)) },
            icon = {
                Icon(
                    painterResource(id = R.drawable.ic_floating_action_menu_file),
                    contentDescription = null,
                    tint = Color(0xFF2196F3)
                )
            },
            onClick = { dialog.show() })
    }

    @Composable
    private fun NewDirectory(closeRequest: () -> Unit) {
        val dialog = remember { DialogController() }
        var name by remember { mutableStateOf("") }

        dialog.BaseDialog(onDismissRequest = { dialog.dismiss() }, title = {
            DialogTitle(title = stringResource(R.string.text_name))
        }, positiveText = stringResource(R.string.ok), onPositiveClick = {
            closeRequest();dialog.dismiss()
            val dir = explorerView.currentPage?.toScriptFile()
            if (name.isEmpty()) {
                showSnackbar(explorerView, R.string.text_file_name_cannot_be_empty)
                return@BaseDialog
            }
            if (dir != null && name.isNotEmpty()) {
                val newDir = File(dir, name)
                if (newDir.exists()){
                    showSnackbar(explorerView, R.string.text_file_exists)
                    return@BaseDialog
                }
                newDir.mkdirs()
                Explorers.workspace()
                    .notifyItemCreated(ExplorerDirPage(newDir, explorerView.currentPage))
                showSnackbar(explorerView, R.string.text_already_create)
            } else {
                showSnackbar(explorerView, R.string.text_create_fail)
            }
        }) {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.text_please_input_name)) }
            )
        }
        ExtendedFloatingActionButton(text = { Text(text = stringResource(id = R.string.text_directory)) },
            icon = {
                Icon(
                    painterResource(id = R.drawable.ic_floating_action_menu_dir),
                    tint = Color(0xFFFFC107),
                    contentDescription = null
                )
            },
            onClick = { dialog.show() })
    }

    fun ExplorerViewKt.setUpViews() {
        fillMaxSize()
        sortConfig = SortConfig.from(
            PreferenceManager.getDefaultSharedPreferences(
                requireContext()
            )
        )
        setExplorer(
            Explorers.workspace(),
            ExplorerDirPage.createRoot(Pref.getScriptDirPath())
        )
        setOnItemClickListener { _, item ->
            item?.let {
                if (item.isEditable) {
                    edit(requireContext(), item.toScriptFile());
                } else {
                    IntentUtil.viewFile(get(), item.path, AppFileProvider.AUTHORITY)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        createReceiver?.let {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(it)
        }
        explorerView.sortConfig?.saveInto(
            PreferenceManager.getDefaultSharedPreferences(
                requireContext()
            )
        )
    }

    private fun getScriptOperations(
        context: Context,
        scriptListFragment: ScriptListFragment
    ): ScriptOperations {
        val explorerView = scriptListFragment.explorerView
        return ScriptOperations(
            context,
            explorerView,
            explorerView.currentPage
        )
    }

    fun onBackPressed(): Boolean {
        if (explorerView.canGoBack()) {
            explorerView.goBack()
            return true
        }
        return false
    }

    companion object {
        private const val TAG = "MyScriptListFragment"
        const val ACTION_SHOW_CREATE_DIALOG = "org.autojs.autojs.action.SHOW_CREATE_DIALOG"
        private fun showSnackbar(view: View, res: Int) {
            Snackbar.make(
                view, res, Snackbar.LENGTH_SHORT
            ).show()
        }
    }


}