package org.autojs.autojs.ui.main.web

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.stardust.toast
import com.stardust.util.IntentUtil
import kotlinx.coroutines.launch
import org.autojs.autojs.ui.widget.CommonMarkdownView
import java.util.Locale

class EditorAppManager : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                DocsPage()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onPause() {
        super.onPause()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DocsPage() {
        val context = LocalContext.current
        var query by remember { mutableStateOf("") }
        var docs by remember { mutableStateOf(emptyList<DocItem>()) }
        var docSearchIndex by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
        var current by remember { mutableStateOf<DocItem?>(null) }
        var showCatalog by remember { mutableStateOf(false) }
        var fontScale by remember { mutableIntStateOf(100) }
        val recentDocs = remember { mutableStateListOf<DocItem>() }

        LaunchedEffect(Unit) {
            docs = listDocsFromAssets(context, AUTOX_DOCS_ASSET_DIR).sortedBy { it.displayTitle.lowercase(Locale.getDefault()) }
            current = docs.firstOrNull { it.path.endsWith("index.md") } ?: docs.firstOrNull()
        }

        LaunchedEffect(docs) {
            docSearchIndex = docs.associate { item ->
                item.path to readAssetText(context, item.path).orEmpty()
            }
        }

        val filteredResults = remember(docs, query, docSearchIndex) {
            val q = query.trim().lowercase(Locale.getDefault())
            if (q.isEmpty()) {
                docs.map { SearchResult(it, null, 0) }
            } else {
                val variants = expandQueryVariants(q)
                docs.mapNotNull { item ->
                    val title = item.displayTitle.lowercase(Locale.getDefault())
                    val rawTitle = item.title.lowercase(Locale.getDefault())
                    val path = item.path.lowercase(Locale.getDefault())
                    val contentRaw = docSearchIndex[item.path].orEmpty()
                    val content = contentRaw.lowercase(Locale.getDefault())
                    val score = variants.maxOfOrNull { token ->
                        maxOf(
                            fuzzyScore(title, token),
                            fuzzyScore(rawTitle, token),
                            fuzzyScore(path, token),
                            fuzzyScore(content, token)
                        )
                    } ?: -1
                    if (score >= 0) {
                        SearchResult(
                            item = item,
                            snippet = extractMatchSnippet(contentRaw, variants),
                            score = score
                        )
                    } else null
                }.sortedByDescending { it.score }
            }
        }

        fun pushRecent(item: DocItem) {
            recentDocs.removeAll { it.path == item.path }
            recentDocs.add(0, item)
            while (recentDocs.size > 6) {
                recentDocs.removeLast()
            }
        }

        LaunchedEffect(current?.path) {
            current?.let { pushRecent(it) }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0E14))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("文档中心", color = Color(0xFFF1F3FC), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = current?.displayTitle ?: "未选择文档",
                        color = Color(0xFFA8ABB3),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { fontScale = 90 }) {
                        Text("小", color = if (fontScale == 90) Color(0xFF00F4FE) else Color(0xFFA8ABB3), fontSize = 12.sp)
                    }
                    TextButton(onClick = { fontScale = 100 }) {
                        Text("中", color = if (fontScale == 100) Color(0xFF00F4FE) else Color(0xFFA8ABB3), fontSize = 12.sp)
                    }
                    TextButton(onClick = { fontScale = 112 }) {
                        Text("大", color = if (fontScale == 112) Color(0xFF00F4FE) else Color(0xFFA8ABB3), fontSize = 12.sp)
                    }
                    TextButton(onClick = { showCatalog = true }) {
                        Text("目录", color = Color(0xFF00F4FE), fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            ReaderPanel(
                modifier = Modifier.fillMaxSize(),
                current = current,
                context = context,
                fontScale = fontScale
            )
        }

        if (showCatalog) {
            ModalBottomSheet(
                onDismissRequest = { showCatalog = false },
                containerColor = Color(0xFF121820),
                contentColor = Color(0xFFF1F3FC)
            ) {
                CatalogPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.88f)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    query = query,
                    onQueryChange = { query = it },
                    filtered = filteredResults,
                    current = current,
                    onSelect = {
                        current = it
                        showCatalog = false
                    },
                    recentDocs = recentDocs
                )
            }
        }
    }

    @Composable
    private fun CatalogPanel(
        modifier: Modifier,
        query: String,
        onQueryChange: (String) -> Unit,
        filtered: List<SearchResult>,
        current: DocItem?,
        onSelect: (DocItem) -> Unit,
        recentDocs: List<DocItem>
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F141A))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Text("文档目录", color = Color(0xFFF1F3FC), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索全文", color = Color(0x99F1F3FC)) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFF1F3FC)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFF1F3FC),
                        unfocusedTextColor = Color(0xFFF1F3FC),
                        cursorColor = Color(0xFF00F4FE),
                        focusedBorderColor = Color(0xFF00F4FE),
                        unfocusedBorderColor = Color(0xFF2D3642),
                        focusedContainerColor = Color(0xFF0B1016),
                        unfocusedContainerColor = Color(0xFF0B1016)
                    )
                )
                Spacer(Modifier.height(10.dp))
                if (recentDocs.isNotEmpty()) {
                    Text("最近浏览", color = Color(0xFFA8ABB3), fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(
                        modifier = Modifier.height(110.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(recentDocs) { item ->
                            val selected = current?.path == item.path
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(item) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) Color(0xFF151A21) else Color(0xFF0B1016)
                                )
                            ) {
                                Text(
                                    text = item.displayTitle,
                                    color = Color(0xFFE3E6EF),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("全部文档", color = Color(0xFFA8ABB3), fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filtered) { result ->
                        val item = result.item
                        val selected = current?.path == item.path
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(item) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) Color(0xFF151A21) else Color(0xFF0B1016)
                            )
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Text(
                                    text = if (query.isNotBlank()) {
                                        highlightSnippet(item.displayTitle, query)
                                    } else {
                                        androidx.compose.ui.text.AnnotatedString(item.displayTitle)
                                    },
                                    color = Color(0xFFF1F3FC),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (query.isNotBlank() && !result.snippet.isNullOrBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = highlightSnippet(result.snippet, query),
                                        color = Color(0xFFA8ABB3),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                if (query.isNotBlank() && filtered.isEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "未找到相关文档",
                        color = Color(0xFFA8ABB3),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "请尝试更短关键词或其他描述",
                        color = Color(0x667D8391),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }

    @Composable
    private fun ReaderPanel(
        modifier: Modifier,
        current: DocItem?,
        context: Context,
        fontScale: Int
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121820))
        ) {
            val doc = current
            if (doc == null) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text("暂无文档", color = Color(0xFFA8ABB3), fontSize = 14.sp)
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 6.dp),
                        factory = { ctx ->
                            CommonMarkdownView(ctx).apply {
                                setPadding("12px")
                                setBackgroundColor(0x00000000)
                                settings.textZoom = fontScale
                            }
                        },
                        update = { view ->
                            val md = readAssetText(context, doc.path) ?: ""
                            view.settings.textZoom = fontScale
                            // Base URL points to AutoX docs root so relative images under ./public work.
                            view.loadMarkdown(md, AUTOX_DOCS_BASE_URL)
                        }
                    )
                }
            }
        }
    }

    private data class DocItem(
        val title: String,
        val displayTitle: String,
        val path: String
    )

    private data class SearchResult(
        val item: DocItem,
        val snippet: String?,
        val score: Int
    )

    /**
     * Fuzzy score:
     * - exact contains: high score
     * - ordered subsequence: medium score
     * - no match: -1
     */
    private fun fuzzyScore(text: String, query: String): Int {
        if (query.isBlank()) return 0
        val containsIndex = text.indexOf(query)
        if (containsIndex >= 0) {
            return 1000 - containsIndex
        }
        var qi = 0
        var consecutive = 0
        var bestConsecutive = 0
        var firstMatchIndex = -1
        for (ti in text.indices) {
            if (qi < query.length && text[ti] == query[qi]) {
                if (firstMatchIndex == -1) firstMatchIndex = ti
                qi++
                consecutive++
                if (consecutive > bestConsecutive) bestConsecutive = consecutive
                if (qi == query.length) {
                    return 500 + bestConsecutive * 10 - firstMatchIndex
                }
            } else {
                consecutive = 0
            }
        }
        return -1
    }

    private fun listDocsFromAssets(context: Context, root: String): List<DocItem> {
        val out = mutableListOf<DocItem>()
        fun walk(dir: String) {
            val children = context.assets.list(dir) ?: return
            for (c in children) {
                val childPath = if (dir.isEmpty()) c else "$dir/$c"
                val sub = context.assets.list(childPath)
                if (sub != null && sub.isNotEmpty()) {
                    walk(childPath)
                } else if (c.endsWith(".md", true)) {
                    val rawTitle = c.removeSuffix(".md")
                    out.add(
                        DocItem(
                            title = rawTitle,
                            displayTitle = toBilingualDocTitle(rawTitle, childPath),
                            path = childPath
                        )
                    )
                }
            }
        }
        walk(root)
        return out
    }

    private fun toBilingualDocTitle(rawTitle: String, path: String): String {
        val source = "$path/$rawTitle".lowercase(Locale.getDefault())
        val fixed = linkedMapOf(
            "index" to "文档首页",
            "readme" to "使用说明",
            "quickstart" to "快速开始",
            "getting started" to "快速开始",
            "app" to "应用模块",
            "console" to "控制台模块",
            "device" to "设备模块",
            "files" to "文件模块",
            "floaty" to "悬浮窗模块",
            "http" to "网络请求模块",
            "images" to "图像模块",
            "engines" to "脚本引擎模块",
            "events" to "事件模块",
            "dialogs" to "对话框模块",
            "timers" to "定时器模块",
            "threads" to "线程模块",
            "ui" to "界面模块",
            "selector" to "控件选择器",
            "widgets" to "控件组件",
            "automator" to "无障碍自动化",
            "accessibility" to "无障碍服务",
            "ocr" to "文字识别",
            "barcode" to "条码识别",
            "intent" to "意图与跳转",
            "storages" to "本地存储",
            "crypto" to "加密模块",
            "canvas" to "画布模块",
            "websocket" to "WebSocket 模块",
            "shell" to "Shell 模块",
            "sensors" to "传感器模块",
            "tasks" to "任务模块"
        )
        fixed.forEach { (k, v) ->
            if (source.contains(k)) return "$v / ${toEnglishLabel(rawTitle)}"
        }
        val english = toEnglishLabel(rawTitle)
        return if (english.isEmpty()) "未命名文档 / Untitled" else "文档 / $english"
    }

    private fun toEnglishLabel(rawTitle: String): String {
        return rawTitle
            .replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractMatchSnippet(content: String, variants: List<String>): String? {
        if (content.isBlank()) return null
        val lower = content.lowercase(Locale.getDefault())
        val idx = variants
            .map { token -> lower.indexOf(token) }
            .filter { it >= 0 }
            .minOrNull() ?: return null
        val start = (idx - 24).coerceAtLeast(0)
        val end = (idx + 56).coerceAtMost(content.length)
        return content.substring(start, end)
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun highlightSnippet(snippet: String, query: String) = buildAnnotatedString {
        append(snippet)
        val lowerSnippet = snippet.lowercase(Locale.getDefault())
        val variants = expandQueryVariants(query)
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinct()
            .sortedByDescending { it.length }
        for (token in variants) {
            val lowerToken = token.lowercase(Locale.getDefault())
            var start = 0
            while (start < lowerSnippet.length) {
                val idx = lowerSnippet.indexOf(lowerToken, startIndex = start)
                if (idx < 0) break
                addStyle(
                    SpanStyle(
                        color = Color(0xFF00F4FE),
                        fontWeight = FontWeight.SemiBold
                    ),
                    idx,
                    idx + token.length
                )
                start = idx + token.length
            }
        }
    }

    private fun expandQueryVariants(query: String): List<String> {
        val variants = linkedSetOf(query)
        val aliasMap = mapOf(
            "click" to listOf("click()", "onclick", "tap", "press", "点击"),
            "press" to listOf("press()", "tap", "click", "点击"),
            "tap" to listOf("click", "press", "点击"),
            "swipe" to listOf("gesture", "滑动"),
            "selector" to listOf("findone", "text()", "id()", "控件"),
            "ocr" to listOf("文字识别", "识别"),
            "http" to listOf("网络请求", "axios", "request"),
            "image" to listOf("images", "图像"),
            "timer" to listOf("timers", "settimeout", "setinterval", "定时器")
        )
        aliasMap[query]?.let { variants.addAll(it.map { token -> token.lowercase(Locale.getDefault()) }) }
        return variants.toList()
    }

    private fun readAssetText(context: Context, path: String): String? {
        return runCatching { context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() } }.getOrNull()
    }

    companion object {
        const val TAG = "EditorAppManager"
        const val DocumentSourceKEY = "DocumentSource"
        private const val DOCUMENT_SAVED_URI_KEY = "DocumentSavedUri"
        private var saveStatus: SharedPreferences? = null
        private const val AUTOX_DOCS_ASSET_DIR = "docs/autox"
        private const val AUTOX_DOCS_BASE_URL = "https://appassets.androidplatform.net/docs/autox/"

        @Synchronized
        fun getSaveStatus(context: Context): SharedPreferences {
            if (saveStatus == null) {
                saveStatus = context.getSharedPreferences(TAG, Context.MODE_PRIVATE)
            }
            return saveStatus!!
        }

        fun openDocument(context: Context) {
            val name = getSaveStatus(context).getString(
                DocumentSourceKEY,
                DocumentSource.DOC_V2_LOCAL.name
            )
            val uri = DocumentSource.valueOf(name!!).let {
                if (it.isLocal) it.openUri else it.uri
            }
            if (uri != null) {
                IntentUtil.browse(context, uri)
            } else {
                toast(context, "此文档未提供在线uri")
            }
        }
    }
}