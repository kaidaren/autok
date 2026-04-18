package org.autojs.autojs.ui.explorer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.webkit.MimeTypeMap
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.stardust.autojs.servicecomponents.EngineController
import com.stardust.io.Zip
import com.stardust.pio.PFile
import com.stardust.pio.PFiles
import com.stardust.util.IntentUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.autojs.autojs.model.explorer.Explorer
import org.autojs.autojs.model.explorer.ExplorerChangeEvent
import org.autojs.autojs.model.explorer.ExplorerDirPage
import org.autojs.autojs.model.explorer.ExplorerFileItem
import org.autojs.autojs.model.explorer.ExplorerItem
import org.autojs.autojs.model.explorer.ExplorerPage
import org.autojs.autojs.model.explorer.ExplorerProjectPage
import org.autojs.autojs.model.explorer.ExplorerSamplePage
import org.autojs.autojs.model.explorer.Explorers
import org.autojs.autojs.model.script.ScriptFile
import org.autojs.autojs.external.fileprovider.AppFileProvider
import org.autojs.autojs.model.script.Scripts.edit
import org.autojs.autojs.model.script.Scripts.openByOtherApps
import org.autojs.autojs.model.script.Scripts.send
import org.autojs.autojs.tool.Observers
import org.autojs.autojs.ui.build.BuildActivity
import org.autojs.autojs.ui.common.ScriptLoopDialog
import org.autojs.autojs.ui.common.ScriptOperations
import org.autojs.autojs.ui.filechooser.FileChooserDialogBuilder
import org.autojs.autojs.ui.filechooser.FileChooseListView
import org.autojs.autojs.ui.viewmodel.ExplorerItemList
import org.autojs.autojs.ui.viewmodel.ExplorerItemList.SortConfig
import org.autojs.autojs.ui.widget.BindableViewHolder
import org.autojs.autojs.ui.widget.fillMaxSize
import org.autojs.autojs.workground.WrapContentGridLayoutManger
import org.autojs.autoxjs.R
import java.io.File
import java.io.FileOutputStream
import java.util.Stack
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalMaterial3Api::class)
open class ExplorerViewKt : FrameLayout, ViewTreeObserver.OnGlobalFocusChangeListener {

    private var onItemClickListener: ((view: View, item: ExplorerItem?) -> Unit)? = null
    private var onItemOperatedListener: ((item: ExplorerItem?) -> Unit)? = null
    private var onPageChangedListener: ((page: ExplorerPage?) -> Unit)? = null
    private var explorerItemList = ExplorerItemList()
    protected val explorerItemListView: RecyclerView = RecyclerView(context)
    private val projectToolbar: ExplorerProjectToolbar = ExplorerProjectToolbar(context)

    private val explorerAdapter: ExplorerAdapter = ExplorerAdapter()
    private var filter: ((ExplorerItem) -> Boolean)? = null
    private var explorer: Explorer? = null
    private val pageStateHistory = Stack<ExplorerPageState>()
    private var currentPageState = ExplorerPageState()
    private var rootPage: ExplorerPage? = null
    private var directorySpanSize1 = 2
    val currentPage get() = currentPageState.currentPage
    private var disposable: Disposable? = null
    private var isRefreshing by mutableStateOf(false)
    private var scope: CoroutineScope? = null

    private val explorerSelectionPaths = mutableSetOf<String>()

    fun isExplorerPathSelected(path: String): Boolean = path in explorerSelectionPaths

    fun toggleExplorerPathSelected(path: String) {
        if (!explorerSelectionPaths.add(path)) {
            explorerSelectionPaths.remove(path)
        }
        explorerAdapter.notifyDataSetChanged()
    }

    init {
        val composeView = ComposeView(context)
        composeView.setContent {
            scope = rememberCoroutineScope()
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { onRefresh() }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    projectToolbar.Content()
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(2.dp))
                    ) {
                        AndroidView(factory = { explorerItemListView.fillMaxSize() })
                    }
                }
            }
        }
        addView(composeView)
        Log.d(
            LOG_TAG, "item bg = " + Integer.toHexString(
                ContextCompat.getColor(context, R.color.item_background)
            )
        )
        initExplorerItemListView()
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    fun setRootPage(page: ExplorerPage?) {
        pageStateHistory.clear()
        setCurrentPageState(ExplorerPageState(page))
        loadItemList()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && canGoBack()) {
            goBack()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun setCurrentPageState(currentPageState: ExplorerPageState) {
        this.currentPageState = currentPageState
        if (this.currentPageState.currentPage is ExplorerProjectPage) {
            projectToolbar.visibility = false
            projectToolbar.setProject(currentPageState.currentPage!!.toScriptFile())
        } else {
            projectToolbar.visibility = true
        }
        onPageChangedListener?.invoke(this.currentPageState.currentPage)
    }

    protected fun enterDirectChildPage(childItemGroup: ExplorerPage?) {
        currentPageState.scrollY =
            (explorerItemListView.layoutManager as LinearLayoutManager?)!!.findLastCompletelyVisibleItemPosition()
        pageStateHistory.push(currentPageState)
        setCurrentPageState(ExplorerPageState(childItemGroup))
        loadItemList()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    fun setOnItemClickListener(listener: (view: View, item: ExplorerItem?) -> Unit) {
        this.onItemClickListener = listener
    }

    var sortConfig: SortConfig?
        get() = explorerItemList.sortConfig
        set(sortConfig) {
            explorerItemList.sortConfig = sortConfig
        }

    fun setExplorer(explorer: Explorer, rootPage: ExplorerPage?) {
        disposable?.dispose()
        this.explorer = explorer
        this.rootPage = rootPage
        setRootPage(rootPage)
        disposable = explorer.registerChangeListener { event -> onExplorerChange(event) }
    }

    fun setExplorer(explorer: Explorer?, rootPage: ExplorerPage?, currentPage: ExplorerPage) {
        disposable?.dispose()
        this.explorer = explorer
        this.rootPage = rootPage
        pageStateHistory.clear()
        setCurrentPageState(ExplorerPageState(rootPage))
        disposable = this.explorer!!.registerChangeListener { event -> onExplorerChange(event) }
        enterChildPage(currentPage)
    }

    fun enterChildPage(childPage: ExplorerPage) {
        val root = currentPageState.currentPage!!.toScriptFile()
        var dir = childPage.toScriptFile()
        val dirs = Stack<ScriptFile>()
        while (dir != root) {
            dir = dir!!.parentFile
            if (dir == null) {
                break
            }
            dirs.push(dir)
        }
        var parent: ExplorerDirPage? = null
        while (!dirs.empty()) {
            dir = dirs.pop()
            val dirPage = ExplorerDirPage(dir, parent)
            pageStateHistory.push(ExplorerPageState(dirPage))
            parent = dirPage
        }
        val leafPage = if (childPage is ExplorerDirPage) {
            ExplorerDirPage(childPage.toScriptFile(), parent)
        } else {
            childPage
        }
        setCurrentPageState(ExplorerPageState(leafPage))
        loadItemList()
    }

    fun setOnItemOperatedListener(listener: (item: ExplorerItem?) -> Unit) {
        this.onItemOperatedListener = listener
    }

    fun canGoBack(): Boolean {
        return !pageStateHistory.empty()
    }

    fun goBack() {
        setCurrentPageState(pageStateHistory.pop())
        loadItemList()
    }

    fun goUp(): Boolean {
        val parent = currentPageState.currentPage?.parent ?: return false
        pageStateHistory.push(currentPageState)
        setCurrentPageState(ExplorerPageState(parent))
        loadItemList()
        return true
    }

    fun setOnPageChangedListener(listener: (page: ExplorerPage?) -> Unit) {
        onPageChangedListener = listener
        onPageChangedListener?.invoke(currentPageState.currentPage)
    }

    /** 脚本工作区根路径（与 [setExplorer] 传入的 root 一致），用于面包屑等 UI。 */
    val workspaceRootPath: String?
        get() = rootPage?.path

    fun jumpToDirectory(path: String): Boolean {
        val rootScriptFile = rootPage?.toScriptFile() ?: return false
        val rootFile = File(rootScriptFile.path)
        val targetFile = File(path.trim())
        val canonicalRoot = runCatching { rootFile.canonicalFile }.getOrDefault(rootFile.absoluteFile)
        val canonicalTarget = runCatching { targetFile.canonicalFile }.getOrDefault(targetFile.absoluteFile)
        if (!canonicalTarget.exists() || !canonicalTarget.isDirectory) {
            return false
        }
        val rootPath = canonicalRoot.path
        val targetPath = canonicalTarget.path
        if (targetPath != rootPath && !targetPath.startsWith(rootPath + File.separator)) {
            return false
        }

        pageStateHistory.clear()
        var current = ExplorerDirPage(canonicalRoot, null)
        if (targetPath != rootPath) {
            val relativePath = targetPath.removePrefix(rootPath).trimStart(File.separatorChar)
            if (relativePath.isNotEmpty()) {
                relativePath.split(File.separatorChar).filter { it.isNotBlank() }.forEach { segment ->
                    val next = ExplorerDirPage(File(current.path, segment), current)
                    pageStateHistory.push(ExplorerPageState(current))
                    current = next
                }
            }
        }
        setCurrentPageState(ExplorerPageState(current))
        loadItemList()
        return true
    }

    fun setDirectorySpanSize(directorySpanSize: Int) {
        directorySpanSize1 = directorySpanSize
    }

    fun setFilter(filter: (ExplorerItem) -> Boolean) {
        this.filter = filter
        reload()
    }

    fun reload() {
        loadItemList()
    }

    override fun isFocused(): Boolean {
        return true
    }

    private fun initExplorerItemListView() {
        explorerItemListView.adapter = explorerAdapter
        val manager = WrapContentGridLayoutManger(context, 2)
        manager.setDebugInfo("ExplorerView")
        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val dirCount = explorerItemList.groupCount()
                return if (position < dirCount) directorySpanSize1 else 2
            }
        }
        explorerItemListView.layoutManager = manager
    }

    @SuppressLint("CheckResult", "NotifyDataSetChanged")
    private fun loadItemList() {
        isRefreshing = true
        explorer!!.fetchChildren(currentPageState.currentPage)
            .subscribeOn(Schedulers.io())
            .flatMapObservable { page: ExplorerPage? ->
                currentPageState.currentPage = page
                onPageChangedListener?.invoke(currentPageState.currentPage)
                Observable.fromIterable(page)
            }
            .filter { f: ExplorerItem -> if (filter == null) true else filter!!.invoke(f) }
            .collectInto(explorerItemList.cloneConfig()) { obj: ExplorerItemList, item: ExplorerItem? ->
                obj.add(
                    item
                )
            }
            .observeOn(Schedulers.computation())
            .doOnSuccess { obj: ExplorerItemList -> obj.sort() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { list: ExplorerItemList ->
                explorerItemList = list
                explorerAdapter.notifyDataSetChanged()
                isRefreshing = false
                post { explorerItemListView.scrollToPosition(currentPageState.scrollY) }
            }
    }

    fun onExplorerChange(event: ExplorerChangeEvent) {
        Log.d(LOG_TAG, "on explorer change: $event")
        if (event.action == ExplorerChangeEvent.ALL) {
            loadItemList()
            return
        }
        val currentDirPath = currentPageState.currentPage!!.path
        val changedDirPath = event.page.path
        val item = event.item
        val changedItemPath = item?.path
        if (currentDirPath == changedItemPath || currentDirPath == changedDirPath &&
            event.action == ExplorerChangeEvent.CHILDREN_CHANGE
        ) {
            loadItemList()
            return
        }
        if (currentDirPath == changedDirPath) {
            val i: Int
            when (event.action) {
                ExplorerChangeEvent.CHANGE -> {
                    i = explorerItemList.update(item, event.newItem)
                    if (i >= 0) {
                        explorerAdapter.notifyItemChanged(item, i)
                    }
                }

                ExplorerChangeEvent.CREATE -> {
                    explorerItemList.insertAtFront(event.newItem)
                    explorerAdapter.notifyItemInserted(event.newItem, 0)
                }

                ExplorerChangeEvent.REMOVE -> {
                    i = explorerItemList.remove(item)
                    if (i >= 0) {
                        explorerAdapter.notifyItemRemoved(item, i)
                    }
                }
            }
        }
    }

    fun onRefresh() {
        explorer!!.notifyChildrenChanged(currentPageState.currentPage)
        projectToolbar.refresh()
    }

    val currentDirectory: ScriptFile?
        get() = currentPage?.toScriptFile()


    fun onMenuSelect(optionMenu: OptionMenu, item: ExplorerItem) {
        val operations = ScriptOperations(context, this, currentPage)
        when (optionMenu) {
            OptionMenu.RUN_REPEATEDLY -> {
                ScriptLoopDialog(context, item.toScriptFile()).show()
            }

            OptionMenu.RENAME -> operations.rename(item as ExplorerFileItem)

            OptionMenu.DELETE -> operations.delete(item.toScriptFile()) {
                loadItemList()
            }

            OptionMenu.SEND -> send(item.toScriptFile())

            OptionMenu.RESET_TO_INITIAL_CONTENT -> {
                val e = Explorers.Providers.workspace().resetSample(
                    item.toScriptFile()
                ).observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        Snackbar.make(
                            this,
                            R.string.text_reset_succeed,
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }, Observers.toastMessage())
            }

            OptionMenu.TIMED_TASK -> operations.timedTask(item.toScriptFile())
            OptionMenu.BUILD_APK -> BuildActivity.start(context, item.path)
            OptionMenu.OPEN_BY_OTHER_APPS -> openByOtherApps(item.toScriptFile())
            OptionMenu.CREATE_SHORTCUT -> operations.createShortcut(item.toScriptFile())
            else -> {}
        }
    }

    fun onExplorerContextAction(action: ExplorerContextAction, item: ExplorerItem) {
        val operations = ScriptOperations(context, this, currentPage)
        when (action) {
            ExplorerContextAction.COPY -> copyExplorerItem(item)
            ExplorerContextAction.RENAME -> operations.rename(item as ExplorerFileItem)
            ExplorerContextAction.MOVE -> moveExplorerItem(item)
            ExplorerContextAction.COMPRESS -> compressExplorerItem(item)
            ExplorerContextAction.EXTRACT -> extractExplorerItem(item)
            ExplorerContextAction.SHARE -> shareExplorerItem(item)
            ExplorerContextAction.DELETE -> operations.delete(item.toScriptFile()) {
                loadItemList()
            }

            ExplorerContextAction.BUILD_APK -> BuildActivity.start(context, item.path)
        }
    }

    private fun copyExplorerItem(item: ExplorerItem) {
        val src = File(item.path)
        val dest = computeCopyDestination(context, src)
        EngineController.scope.launch(Dispatchers.IO) {
            try {
                if (src.isDirectory) {
                    PFiles.copyDirectory(src.toPath(), dest.toPath())
                } else {
                    PFiles.copy(src.path, dest.path)
                }
                withContext(Dispatchers.Main) {
                    loadItemList()
                    Snackbar.make(
                        this@ExplorerViewKt,
                        R.string.explorer_op_succeeded,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        this@ExplorerViewKt,
                        R.string.explorer_op_failed,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun moveExplorerItem(item: ExplorerItem) {
        val src = File(item.path)
        FileChooserDialogBuilder(context, context.getString(R.string.explorer_pick_target_folder))
            .dir(rootPage?.path)
            .chooseDir()
            .singleChoice { dest: PFile ->
                val target = File(dest.path, src.name)
                if (target.exists()) {
                    Snackbar.make(this, R.string.explorer_op_failed, Snackbar.LENGTH_SHORT).show()
                    return@singleChoice
                }
                EngineController.scope.launch(Dispatchers.IO) {
                    val ok = src.renameTo(target)
                    withContext(Dispatchers.Main) {
                        if (ok) {
                            loadItemList()
                            Snackbar.make(
                                this@ExplorerViewKt,
                                R.string.explorer_op_succeeded,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        } else {
                            Snackbar.make(
                                this@ExplorerViewKt,
                                R.string.explorer_op_failed,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .show()
    }

    private fun compressExplorerItem(item: ExplorerItem) {
        val src = File(item.path)
        var zip = File(src.parentFile ?: return, "${src.name}.zip")
        var i = 0
        while (zip.exists()) {
            i++
            zip = File(src.parentFile, "${src.name} ($i).zip")
        }
        val zipFile = zip
        EngineController.scope.launch(Dispatchers.IO) {
            try {
                FileOutputStream(zipFile).use { fos ->
                    ZipOutputStream(fos).use { zos ->
                        Zip.zipFile(src, src.name, zos)
                    }
                }
                withContext(Dispatchers.Main) {
                    loadItemList()
                    Snackbar.make(
                        this@ExplorerViewKt,
                        R.string.explorer_op_succeeded,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        this@ExplorerViewKt,
                        R.string.explorer_op_failed,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun extractExplorerItem(item: ExplorerItem) {
        val zip = File(item.path)
        val parent = zip.parentFile ?: return
        EngineController.scope.launch(Dispatchers.IO) {
            try {
                var dir = File(parent, PFiles.getNameWithoutExtension(zip.name))
                var n = 0
                while (dir.exists()) {
                    n++
                    dir = File(parent, "${PFiles.getNameWithoutExtension(zip.name)} ($n)")
                }
                dir.mkdirs()
                Zip.unzip(zip, dir)
                withContext(Dispatchers.Main) {
                    loadItemList()
                    Snackbar.make(
                        this@ExplorerViewKt,
                        R.string.explorer_op_succeeded,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        this@ExplorerViewKt,
                        R.string.explorer_op_failed,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun shareExplorerItem(item: ExplorerItem) {
        val file = File(item.path)
        val appContext = context.applicationContext
        EngineController.scope.launch(Dispatchers.IO) {
            try {
                val toShare: File = if (file.isDirectory) {
                    val zip = File(appContext.cacheDir, "${file.name}_share.zip")
                    if (zip.exists()) zip.delete()
                    FileOutputStream(zip).use { fos ->
                        ZipOutputStream(fos).use { zos ->
                            Zip.zipFile(file, file.name, zos)
                        }
                    }
                    zip
                } else {
                    file
                }
                withContext(Dispatchers.Main) {
                    val uri = IntentUtil.getUriOfFile(
                        appContext,
                        toShare.path,
                        AppFileProvider.AUTHORITY
                    )
                    val mime = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(toShare.extension.lowercase()) ?: "*/*"
                    appContext.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND)
                                .setType(mime)
                                .putExtra(Intent.EXTRA_STREAM, uri)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                            appContext.getString(R.string.explorer_action_share)
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        this@ExplorerViewKt,
                        R.string.explorer_op_failed,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun onMenuSelect(optionMenu: OptionMenu2) {
        val sortType = when (optionMenu) {
            OptionMenu2.NAME -> ExplorerItemList.SORT_TYPE_NAME
            OptionMenu2.TIME -> ExplorerItemList.SORT_TYPE_DATE
            OptionMenu2.SIZE -> ExplorerItemList.SORT_TYPE_SIZE
            OptionMenu2.TYPE -> ExplorerItemList.SORT_TYPE_TYPE
        }
        isRefreshing = true
        scope?.launch(Dispatchers.Default) {
            explorerItemList.sortItemGroup(sortType)
            explorerItemList.sortFile(sortType)
            withContext(Dispatchers.Main) {
                explorerAdapter.notifyDataSetChanged()
                isRefreshing = false
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun sort(sortType: Int, isDir: Boolean) {
        isRefreshing = true
        scope?.launch(Dispatchers.Default) {
            if (isDir) {
                explorerItemList.sortItemGroup(sortType)
            } else {
                explorerItemList.sortFile(sortType)
            }
            withContext(Dispatchers.Main) {
                explorerAdapter.notifyDataSetChanged()
                isRefreshing = false
            }
        }
    }


    protected open fun onCreateViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        viewType: Int
    ): BindableViewHolder<Any> {
        return when (viewType) {
            VIEW_TYPE_ITEM -> ExplorerItemViewHolder(ComposeView(context))
            VIEW_TYPE_PAGE -> ExplorerPageViewHolder(ComposeView(context))

            else -> {
                CategoryViewHolder(ComposeView(context))
            }
        }
    }

    @Composable
    fun ExplorerList() {
        LazyColumn {
            items(explorerItemList.groupCount()) {

            }
        }
    }

    private inner class ExplorerAdapter : RecyclerView.Adapter<BindableViewHolder<Any>>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindableViewHolder<Any> {
            val inflater = LayoutInflater.from(context)
            return this@ExplorerViewKt.onCreateViewHolder(inflater, parent, viewType)
        }

        override fun onBindViewHolder(holder: BindableViewHolder<Any>, position: Int) {
            val dirCount = explorerItemList.groupCount()
            if (position < dirCount) {
                holder.bind(explorerItemList.getItemGroup(position), position)
            } else {
                holder.bind(
                    explorerItemList.getItem(position - dirCount),
                    position
                )
            }
        }

        override fun getItemViewType(position: Int): Int {
            val dirCount = explorerItemList.groupCount()
            return if (position < dirCount) VIEW_TYPE_PAGE else VIEW_TYPE_ITEM
        }

        fun getItemPosition(item: ExplorerItem?, i: Int): Int {
            return if (item is ExplorerPage) {
                i
            } else {
                explorerItemList.groupCount() + i
            }
        }

        fun notifyItemChanged(item: ExplorerItem?, i: Int) {
            notifyItemChanged(getItemPosition(item, i))
        }

        fun notifyItemRemoved(item: ExplorerItem?, i: Int) {
            notifyItemRemoved(getItemPosition(item, i))
        }

        fun notifyItemInserted(item: ExplorerItem?, i: Int) {
            notifyItemInserted(getItemPosition(item, i))
        }

        override fun getItemCount(): Int {
            return explorerItemList.groupCount() + explorerItemList.itemCount()
        }
    }

    open inner class ExplorerItemViewHolder(view: ComposeView) :
        BindableViewHolder<Any>(view) {
        var name by mutableStateOf("")
        var firstChar by mutableStateOf("J")
        var desc by mutableStateOf("")
        var firstCharBackground by mutableStateOf(Color(0xFF5cab7d))

        var editVisibility by mutableStateOf(true)
        var runVisibility by mutableStateOf(true)

        private var explorerItem: ExplorerItem? by mutableStateOf(null)

        init {
            view.setContent {
                val ctx = LocalContext.current
                val host = this@ExplorerViewKt
                val cfg = LocalExplorerItemConfig.current
                explorerItem?.let { item ->
                    key(item.path) {
                        var showDialog by remember { mutableStateOf(false) }
                        ExplorerListRow(
                            checked = host.isExplorerPathSelected(item.path),
                            onCheckedChange = { want ->
                                val cur = host.isExplorerPathSelected(item.path)
                                if (want != cur) host.toggleExplorerPathSelected(item.path)
                            },
                            showCheckbox = true,
                            leadingIcon = {
                                FileIcon(firstChar, firstCharBackground)
                            },
                            name = name,
                            typeLabel = explorerItemTypeLabel(ctx, item),
                            dateLabel = formatExplorerItemDate(item.lastModified()),
                            onRowClick = { onItemClick() },
                            showMore = cfg.showMore,
                            onMoreClick = { showDialog = true }
                        )
                        if (showDialog) {
                            ExplorerContextMenuDialog(
                                title = name,
                                iconRes = contextMenuIconForItem(item),
                                actions = buildExplorerContextActions(item),
                                onDismiss = { showDialog = false },
                                onAction = { act ->
                                    host.onExplorerContextAction(act, item)
                                }
                            )
                        }
                    }
                }
            }
        }

        override fun bind(item: Any, position: Int) {
            if (item !is ExplorerItem) return
            explorerItem = item
            name = ExplorerViewHelper.getDisplayName(item)
            desc = PFiles.getHumanReadableSize(item.size)
            firstChar = ExplorerViewHelper.getIconText(item)
            firstCharBackground = Color(ExplorerViewHelper.getIconColor(item))
            editVisibility = item.isEditable
            runVisibility = item.isExecutable
        }

        fun onItemClick() {
            onItemClickListener?.invoke(itemView, explorerItem)
            onItemOperatedListener?.invoke(explorerItem)
        }

        fun run() {
            EngineController.runScript(File(explorerItem!!.path))
        }

        fun edit() {
            edit(context, ScriptFile(explorerItem!!.path))
        }
    }


    open inner class ExplorerPageViewHolder(
        view: ComposeView,
    ) : BindableViewHolder<Any>(view) {

        var name by mutableStateOf("")
        var optionsVisibility by mutableStateOf(false)
        var iconRes by mutableIntStateOf(R.drawable.circle_blue)
        private var explorerPage: ExplorerPage? = null

        init {
            view.setContent {
                val ctx = LocalContext.current
                val host = this@ExplorerViewKt
                val cfg = LocalExplorerItemConfig.current
                val fcHolder = this@ExplorerPageViewHolder as? FileChooseListView.ExplorerPageViewHolder
                val showFolderCheckbox = fcHolder == null || fcHolder.showCheckBox
                explorerPage?.let { page ->
                    key(page.path) {
                        var showDialog by remember { mutableStateOf(false) }
                        val checkedState = if (fcHolder != null && fcHolder.showCheckBox) {
                            fcHolder.checked
                        } else {
                            host.isExplorerPathSelected(page.path)
                        }
                        ExplorerListRow(
                            checked = checkedState,
                            onCheckedChange = { want ->
                                if (fcHolder != null && fcHolder.showCheckBox) {
                                    if (want != fcHolder.checked) fcHolder.onCheckedChanged()
                                } else {
                                    val cur = host.isExplorerPathSelected(page.path)
                                    if (want != cur) host.toggleExplorerPathSelected(page.path)
                                }
                            },
                            showCheckbox = showFolderCheckbox,
                            leadingIcon = { ExplorerDirectoryRowLeading(iconRes) },
                            name = name,
                            typeLabel = explorerItemTypeLabel(ctx, page),
                            dateLabel = formatExplorerItemDate(page.lastModified()),
                            onRowClick = { onItemClick() },
                            showMore = !optionsVisibility && cfg.showMore,
                            onMoreClick = { showDialog = true }
                        )
                        if (showDialog) {
                            ExplorerContextMenuDialog(
                                title = name,
                                iconRes = contextMenuIconForItem(page),
                                actions = buildExplorerContextActions(page),
                                onDismiss = { showDialog = false },
                                onAction = { act ->
                                    host.onExplorerContextAction(act, page)
                                }
                            )
                        }
                    }
                }
            }
        }

        override fun bind(data: Any, position: Int) {
            if (data !is ExplorerPage) return
            name = ExplorerViewHelper.getDisplayName(data)
            iconRes = ExplorerViewHelper.getIcon(data)
            optionsVisibility = data is ExplorerSamplePage
            explorerPage = data
        }

        private fun onItemClick() {
            enterDirectChildPage(explorerPage)
        }
    }

    inner class CategoryViewHolder(val view: ComposeView) :
        BindableViewHolder<Any>(view) {
        var title by mutableStateOf("")
        var showGoBack by mutableStateOf(false)
        var sortOrder by mutableStateOf(true)
        var arrowCollapsed by mutableStateOf(true)
        var showMenu by mutableStateOf(false)
        private var isDir = false

        init {
            view.setContent {
                CategoryItem(this) {
                    OptionMenu2(
                        expanded = showMenu, onDismissRequest = { showMenu = false },
                        listOf(
                            OptionMenu2.NAME, OptionMenu2.TIME, OptionMenu2.SIZE, OptionMenu2.TIME
                        )
                    ) {
                        showMenu = false
                        onMenuSelect(it)
                    }
                }
            }
        }

        fun goBack2() {
            if (canGoBack()) goBack()
        }

        override fun bind(isDirCategory: Any, position: Int) {
            if (isDirCategory !is Boolean) return
            title =
                view.context.getString(if (isDirCategory) R.string.text_directory else R.string.text_file)
            isDir = isDirCategory
            showGoBack = isDirCategory && canGoBack()
            if (isDirCategory) {
                arrowCollapsed = currentPageState.dirsCollapsed
                sortOrder = explorerItemList.isDirSortedAscending
            } else {
                arrowCollapsed = currentPageState.filesCollapsed
                sortOrder = explorerItemList.isFileSortedAscending
            }
        }


        fun changeSortOrder() {
            if (isDir) {
                sortOrder = explorerItemList.isDirSortedAscending
                explorerItemList.isDirSortedAscending = !explorerItemList.isDirSortedAscending
                sort(explorerItemList.dirSortType, isDir)
            } else {
                sortOrder = explorerItemList.isFileSortedAscending
                explorerItemList.isFileSortedAscending = !explorerItemList.isFileSortedAscending
                sort(explorerItemList.fileSortType, isDir)
            }
        }

        fun collapseOrExpand() {
            if (isDir) {
                currentPageState.dirsCollapsed = !currentPageState.dirsCollapsed
            } else {
                currentPageState.filesCollapsed = !currentPageState.filesCollapsed
            }
            explorerAdapter.notifyDataSetChanged()
        }
    }

    private class ExplorerPageState {
        var currentPage: ExplorerPage? = null
        var dirsCollapsed = false
        var filesCollapsed = false
        var scrollY = 0

        constructor()
        constructor(page: ExplorerPage?) {
            currentPage = page
        }
    }

    companion object {
        private const val LOG_TAG = "ExplorerView"
        const val VIEW_TYPE_ITEM = 0
        const val VIEW_TYPE_PAGE = 1

        protected const val VIEW_TYPE_CATEGORY = 2
    }

    override fun onGlobalFocusChanged(oldView: View, newView: View) {
        newView.setOnKeyListener { _, _, event ->
            Log.d("TAG", "dispatchKeyEvent: ")
            if (event.keyCode == KeyEvent.KEYCODE_BACK && canGoBack()) {
                goBack()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
    }
}