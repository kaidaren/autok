package org.autojs.autojs.devplugin

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.Base64
import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.stardust.autojs.core.image.ImageWrapper
import com.stardust.autojs.core.image.capture.ScreenCapturer
import com.stardust.autojs.engine.JavaScriptEngine
import com.stardust.autojs.execution.ExecutionConfig
import com.stardust.autojs.runtime.api.Images
import com.stardust.autojs.script.StringScriptSource
import com.stardust.app.GlobalAppContext
import com.stardust.view.accessibility.AccessibilityService
import com.stardust.view.accessibility.NodeInfo
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.tool.AccessibilityServiceTool
import org.autojs.autojs.ui.main.MainActivity
import java.io.ByteArrayOutputStream
import java.lang.reflect.InvocationTargetException
import java.util.ArrayDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.max
import kotlin.math.min

class DevToolMessageHandler(
    private val autoJs: AutoJs,
    private val statusReporter: ((String) -> Unit)? = null
) {

    companion object {
        private const val TAG = "DevToolMessageHandler"
        private const val ENGINE_WAIT_MS = 3000L
        private const val INSPECT_WAIT_MS = 5000L
        private const val PERMISSION_WAIT_MS = 15000L
        private const val DEVTOOL_PERMISSION_WAIT_SEC = 40L
    }

    private val devToolRuntime by lazy {
        createDevToolEngine().runtime
    }

    @Volatile
    private var screenCapturePrepared = false

    fun handleMessage(jsonStr: String): String? {
        return try {
            val json = JsonParser.parseString(jsonStr).asJsonObject
            when (json.get("type")?.asString) {
                "screen_capture" -> handleScreenCapture(json)
                "get_color" -> handleGetColor(json)
                "dump_node_tree" -> handleDumpNodeTree()
                "find_nodes_by_text" -> handleFindNodesByText(json)
                "find_nodes_by_id" -> handleFindNodesById(json)
                "color_test" -> handleColorTest(json)
                "highlight_node" -> {
                    reportStatus("手机端已禁用节点高亮功能")
                    null
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleMessage failed", e)
            createErrorResponse("处理消息失败：${e.message}")
        }
    }

    private fun createDevToolEngine(): JavaScriptEngine {
        val source = StringScriptSource("DevToolRuntime", "// devtools runtime")
        val config = ExecutionConfig(workingDirectory = GlobalAppContext.get().cacheDir.absolutePath).apply {
            // Keep runtime alive for async permission callbacks.
            loopTimes = 0
            interval = 60_000L
        }
        val execution = autoJs.scriptEngineService.execute(source, config)
        val deadline = System.currentTimeMillis() + ENGINE_WAIT_MS
        while (System.currentTimeMillis() < deadline) {
            val engine = execution.engine
            if (engine is JavaScriptEngine) {
                return engine
            }
            Thread.sleep(50)
        }
        throw IllegalStateException("创建脚本引擎失败")
    }

    private fun ensureScreenCapturePermission(): String? {
        if (screenCapturePrepared) return null
        return try {
            reportStatus("开始准备截图权限")
            val imagesApi = devToolRuntime.images as Images
            reportStatus("拉起主界面到前台")
            bringAppToForegroundForCapture()
            Thread.sleep(300)
            reportStatus("请求系统录屏授权（第1次）")
            val granted = requestScreenCaptureInWorker(imagesApi)
            if (!granted) {
                reportStatus("第1次未授权，准备重试")
                bringAppToForegroundForCapture()
                Thread.sleep(500)
                reportStatus("请求系统录屏授权（第2次）")
                val retried = requestScreenCaptureInWorker(imagesApi)
                if (!retried) {
                    reportStatus("录屏授权失败：两次请求都未通过")
                    "未检测到录屏授权。请在手机端点“继续授权”，并在系统弹窗选择“立即开始/允许”后重试"
                } else {
                    reportStatus("录屏授权成功（第2次）")
                    screenCapturePrepared = true
                    null
                }
            } else {
                reportStatus("录屏授权成功（第1次）")
                screenCapturePrepared = true
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "ensureScreenCapturePermission failed", e)
            reportStatus("录屏授权异常：${e.message ?: e.javaClass.simpleName}")
            "截图权限申请失败：${e.message ?: "请确认手机端允许录屏权限弹窗"}"
        }
    }

    private fun requestScreenCaptureInWorker(imagesApi: Images): Boolean {
        reportStatus("在后台线程发起录屏权限请求")
        val executor = Executors.newSingleThreadExecutor()
        return try {
            val future = executor.submit<Boolean> {
                imagesApi.requestScreenCapture(ScreenCapturer.ORIENTATION_PORTRAIT)
            }
            val granted = future.get(DEVTOOL_PERMISSION_WAIT_SEC, TimeUnit.SECONDS)
            reportStatus("录屏权限请求返回：${if (granted) "已授权" else "未授权"}")
            granted
        } catch (e: TimeoutException) {
            // Do not interrupt the request coroutine aggressively; some ROMs need longer to
            // finish MediaProjection permission handoff.
            reportStatus("录屏权限请求超时：${DEVTOOL_PERMISSION_WAIT_SEC}秒内无返回")
            false
        } catch (e: Exception) {
            Log.e(TAG, "requestScreenCaptureInWorker failed", e)
            reportStatus("录屏权限请求异常：${e.message ?: e.javaClass.simpleName}")
            false
        } finally {
            executor.shutdown()
        }
    }

    private fun bringAppToForegroundForCapture() {
        runCatching {
            val ctx = GlobalAppContext.get()
            val intent = Intent(ctx, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            ctx.startActivity(intent)
        }
    }

    private fun isScreenCapturePermissionError(e: Throwable): Boolean {
        val msg = e.message?.lowercase().orEmpty()
        return e is SecurityException ||
            msg.contains("no screen capture permission") ||
            msg.contains("media projection") ||
            msg.contains("screen capture permission")
    }

    private inline fun <T> runWithScreenCapturePermission(block: () -> T): T {
        ensureScreenCapturePermission()?.let { throw IllegalStateException(it) }
        return try {
            block()
        } catch (e: Exception) {
            if (!isScreenCapturePermissionError(e)) throw e
            // Permission might have been revoked/invalidated; force re-request once.
            reportStatus("检测到权限失效，触发一次重新授权")
            screenCapturePrepared = false
            ensureScreenCapturePermission()?.let { throw IllegalStateException(it) }
            block()
        }
    }

    private fun ensureAccessibilityPermission(): String? {
        val context = GlobalAppContext.get()
        val enabledInSystem = runCatching {
            AccessibilityServiceTool.isAccessibilityServiceEnabled(context)
        }.getOrDefault(false)

        if (!enabledInSystem) {
            return "无障碍服务未开启，请手动到系统设置开启 AutoX 无障碍后重试"
        }

        // Wait for service bind completion to reduce transient null root/service issues.
        val enabled = runCatching { AccessibilityService.waitForEnabled(5000L) }.getOrDefault(false)
        return if (enabled) null else "无障碍服务正在启动中，请稍后重试"
    }

    private fun handleScreenCapture(json: JsonObject): String {
        return try {
            runWithScreenCapturePermission {
                reportStatus("开始执行截图")
                val data = json.getAsJsonObject("data")
                val quality = data?.get("quality")?.asInt ?: 72
                val mode = data?.get("mode")?.asString?.lowercase().orEmpty()
                // Default to precise capture for DevTool to keep coordinates aligned with source image.
                val explicitFast = mode == "fast" || mode == "compressed" || mode == "jpeg" ||
                    (data?.get("lossless")?.asBoolean == false)
                val precise = !explicitFast
                val image = captureScreenImage()
                val originalBitmap = image.bitmap
                val bitmap = if (precise) originalBitmap else scaleBitmapForTransport(originalBitmap, maxSide = 720)
                val outWidth = bitmap.width
                val outHeight = bitmap.height
                val originWidth = originalBitmap.width
                val originHeight = originalBitmap.height
                val payloadBytes = if (precise) {
                    ByteArrayOutputStream().use { output ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                        output.toByteArray()
                    }
                } else {
                    compressForTransport(bitmap, quality)
                }
                val base64 = Base64.encodeToString(payloadBytes, Base64.NO_WRAP)
                image.recycle()
                if (bitmap !== originalBitmap && !originalBitmap.isRecycled) {
                    originalBitmap.recycle()
                }
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
                JsonObject().apply {
                    addProperty("type", "screen_capture_result")
                    add("data", JsonObject().apply {
                        addProperty("base64", base64)
                        addProperty("timestamp", System.currentTimeMillis())
                        addProperty("width", outWidth)
                        addProperty("height", outHeight)
                        addProperty("originWidth", originWidth)
                        addProperty("originHeight", originHeight)
                        addProperty("format", if (precise) "png" else "jpeg")
                        addProperty("mode", if (precise) "precise" else "fast")
                    })
                }.toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleScreenCapture failed", e)
            reportStatus("截图执行失败：${e.message ?: e.javaClass.simpleName}")
            createErrorResponse("截图失败：${e.message}")
        }
    }

    private fun scaleBitmapForTransport(source: Bitmap, maxSide: Int): Bitmap {
        val width = source.width
        val height = source.height
        val largest = maxOf(width, height)
        if (largest <= maxSide) return source
        val ratio = maxSide.toFloat() / largest.toFloat()
        val targetW = (width * ratio).toInt().coerceAtLeast(1)
        val targetH = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    private fun compressForTransport(bitmap: Bitmap, startQuality: Int): ByteArray {
        val output = ByteArrayOutputStream()
        var q = startQuality.coerceIn(35, 88)
        bitmap.compress(Bitmap.CompressFormat.JPEG, q, output)
        // Keep payload smaller to avoid websocket peer closing on large text frames.
        while (output.size() > 240 * 1024 && q > 35) {
            q -= 6
            output.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, q, output)
        }
        if (output.size() > 280 * 1024) {
            output.reset()
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * 0.82f).toInt().coerceAtLeast(1), (bitmap.height * 0.82f).toInt().coerceAtLeast(1), true)
                .also { smaller ->
                    smaller.compress(Bitmap.CompressFormat.JPEG, 36, output)
                    if (!smaller.isRecycled) smaller.recycle()
                }
        }
        return output.toByteArray()
    }

    private fun handleGetColor(json: JsonObject): String {
        return try {
            runWithScreenCapturePermission {
                val data = json.getAsJsonObject("data") ?: return createErrorResponse("缺少数据参数")
                val x = data.get("x")?.asInt ?: 0
                val y = data.get("y")?.asInt ?: 0
                val image = captureScreenImage()
                val color = image.pixel(x, y)
                image.recycle()
                JsonObject().apply {
                    addProperty("type", "color_result")
                    add("data", JsonObject().apply {
                        addProperty("rgb", rgbToHex(color))
                        addProperty("x", x)
                        addProperty("y", y)
                    })
                }.toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleGetColor failed", e)
            createErrorResponse("获取颜色失败：${e.message}")
        }
    }

    private fun handleDumpNodeTree(): String {
        return try {
            ensureAccessibilityPermission()?.let { return createErrorResponse(it) }
            reportStatus("开始抓取节点树")
            val root = captureNodeTreeForDevTool()
                ?: return createErrorResponse("无法获取节点树，请保持目标页面在前台并确认无障碍服务已开启")
            JsonObject().apply {
                addProperty("type", "node_tree_result")
                add("data", JsonObject().apply {
                    add("tree", nodeToJson(root, JsonArray()))
                    addProperty("timestamp", System.currentTimeMillis())
                    addProperty("nodeCount", countNodes(root))
                })
            }.toString()
        } catch (e: Exception) {
            Log.e(TAG, "handleDumpNodeTree failed", e)
            createErrorResponse("导出节点树失败：${e.message}")
        }
    }

    private fun handleFindNodesByText(json: JsonObject): String {
        return try {
            ensureAccessibilityPermission()?.let { return createErrorResponse(it) }
            val keyword = json.getAsJsonObject("data")?.get("text")?.asString.orEmpty()
            val root = captureNodeTreeForDevTool()
                ?: return createErrorResponse("无法获取节点树，请保持目标页面在前台并确认无障碍服务已开启")
            val result = JsonArray()
            findNodes(root) { node ->
                node.text.orEmpty().contains(keyword, ignoreCase = true)
            }.forEach { result.add(nodeToJson(it, computeIndexPath(it))) }
            JsonObject().apply {
                addProperty("type", "find_nodes_result")
                add("data", JsonObject().apply {
                    add("nodes", result)
                    addProperty("count", result.size())
                    addProperty("searchText", keyword)
                })
            }.toString()
        } catch (e: Exception) {
            Log.e(TAG, "handleFindNodesByText failed", e)
            createErrorResponse("搜索节点失败：${e.message}")
        }
    }

    private fun handleFindNodesById(json: JsonObject): String {
        return try {
            ensureAccessibilityPermission()?.let { return createErrorResponse(it) }
            val keyword = json.getAsJsonObject("data")?.get("id")?.asString.orEmpty()
            val root = captureNodeTreeForDevTool()
                ?: return createErrorResponse("无法获取节点树，请保持目标页面在前台并确认无障碍服务已开启")
            val result = JsonArray()
            findNodes(root) { node ->
                node.fullId.orEmpty() == keyword || node.id.orEmpty() == keyword
            }.forEach { result.add(nodeToJson(it, computeIndexPath(it))) }
            JsonObject().apply {
                addProperty("type", "find_nodes_result")
                add("data", JsonObject().apply {
                    add("nodes", result)
                    addProperty("count", result.size())
                    addProperty("searchId", keyword)
                })
            }.toString()
        } catch (e: Exception) {
            Log.e(TAG, "handleFindNodesById failed", e)
            createErrorResponse("搜索节点失败：${e.message}")
        }
    }

    private fun handleColorTest(json: JsonObject): String {
        return try {
            runWithScreenCapturePermission {
                val data = json.getAsJsonObject("data") ?: return createErrorResponse("缺少数据参数")
                val anchorX = data.get("anchorX")?.asInt ?: return createErrorResponse("缺少锚点坐标X")
                val anchorY = data.get("anchorY")?.asInt ?: return createErrorResponse("缺少锚点坐标Y")
                val anchorColor = Color.parseColor(data.get("anchorColor")?.asString ?: "#000000")
                val threshold = data.get("threshold")?.asInt ?: 4
                val nearbyRadius = (data.get("nearbyRadius")?.asInt ?: 120).coerceIn(20, 500)
                val region = parseRegionArray(data.getAsJsonArray("region"))
                val points = parseColorTestPoints(data.getAsJsonArray("points"))
                val imagesApi = devToolRuntime.images as Images
                val finder = imagesApi.colorFinder
                val method = finder.javaClass.getMethod(
                    "findMultiColors",
                    ImageWrapper::class.java,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    Class.forName("org.opencv.core.Rect"),
                    IntArray::class.java
                )
                // Align with script-side images compatibility: preload OpenCV before color find.
                imagesApi.initOpenCvIfNeeded()
                val foundCoords = captureScreenImage().useImage { image ->
                    if (isColorTestMatchAt(image, anchorX, anchorY, anchorColor, threshold, points)) {
                        Pair(anchorX, anchorY)
                    } else {
                        val nearbyRegion = buildNearbyRegion(anchorX, anchorY, nearbyRadius, image)
                        val effectiveRegion = intersectRegion(region, nearbyRegion) ?: nearbyRegion
                        val rect = toOpenCvRectReflective(effectiveRegion)
                        val found = invokeColorTestWithRetry(
                            imagesApi = imagesApi,
                            method = method,
                            finder = finder,
                            image = image,
                            anchorColor = anchorColor,
                            threshold = threshold,
                            rect = rect,
                            points = points
                        )
                        parseFoundPoint(found)
                    }
                }
                JsonObject().apply {
                    addProperty("type", "color_test_result")
                    add("data", JsonObject().apply {
                        addProperty("found", foundCoords != null)
                        if (foundCoords != null) {
                            addProperty("x", foundCoords.first)
                            addProperty("y", foundCoords.second)
                        }
                    })
                }.toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleColorTest failed", e)
            createErrorResponse("图色测试失败：${readableError(e)}")
        }
    }

    private fun parseFoundPoint(found: Any?): Pair<Int, Int>? {
        if (found == null) return null
        val pointClass = Class.forName("org.opencv.core.Point")
        return Pair(
            pointClass.getField("x").getDouble(found).toInt(),
            pointClass.getField("y").getDouble(found).toInt()
        )
    }

    private fun invokeColorTestWithRetry(
        imagesApi: Images,
        method: java.lang.reflect.Method,
        finder: Any,
        image: ImageWrapper,
        anchorColor: Int,
        threshold: Int,
        rect: Any?,
        points: IntArray
    ): Any? {
        return try {
            method.invoke(finder, image, anchorColor, threshold, rect, points)
        } catch (e: InvocationTargetException) {
            val target = e.targetException
            if (target is UnsatisfiedLinkError) {
                // Some devices need one extra native init attempt on first call.
                imagesApi.initOpenCvIfNeeded()
                method.invoke(finder, image, anchorColor, threshold, rect, points)
            } else {
                throw e
            }
        }
    }

    private fun captureScreenImage(): ImageWrapper {
        val imagesApi = devToolRuntime.images as Images
        return imagesApi.captureScreen()
    }

    private fun captureNodeTree(): NodeInfo? {
        val inspector = autoJs.layoutInspector
        val latch = CountDownLatch(1)
        var capturedNode: NodeInfo? = null
        val listener = object : com.stardust.view.accessibility.LayoutInspector.CaptureAvailableListener {
            override fun onCaptureAvailable(capture: NodeInfo?) {
                capturedNode = capture
                inspector.removeCaptureAvailableListener(this)
                latch.countDown()
            }
        }
        inspector.addCaptureAvailableListener(listener)
        if (!inspector.captureCurrentWindow()) {
            inspector.removeCaptureAvailableListener(listener)
            return null
        }
        latch.await(INSPECT_WAIT_MS, TimeUnit.MILLISECONDS)
        return capturedNode
    }

    private fun captureNodeTreeWithRetry(maxAttempts: Int = 3): NodeInfo? {
        repeat(maxAttempts) { index ->
            val attempt = index + 1
            reportStatus("抓取节点树（第${attempt}次）")
            bringAppToForegroundForCapture()
            Thread.sleep(if (attempt == 1) 350 else 500)
            captureNodeTree()?.let { node ->
                reportStatus("节点树抓取成功（第${attempt}次）")
                return node
            }
            Thread.sleep(300)
        }
        reportStatus("节点树抓取失败：连续重试仍为空")
        return null
    }

    private fun captureNodeTreeForDevTool(): NodeInfo? {
        // Keep this path stable across process-separated accessibility services.
        return captureNodeTreeWithRetry(maxAttempts = 4)
    }

    private fun nodeToJson(node: NodeInfo, indexPath: JsonArray): JsonObject {
        return JsonObject().apply {
            addProperty("className", node.className.orEmpty())
            addProperty("text", node.text)
            addProperty("desc", node.desc.orEmpty())
            addProperty("viewId", node.fullId.orEmpty())
            addProperty("fullId", node.fullId.orEmpty())
            addProperty("packageName", node.packageName.orEmpty())
            addProperty("id", node.id.orEmpty())
            addProperty("idHex", node.idHex.orEmpty())
            add("bounds", JsonArray().apply {
                add(node.boundsInScreen.left)
                add(node.boundsInScreen.top)
                add(node.boundsInScreen.right)
                add(node.boundsInScreen.bottom)
            })
            addProperty("visible", node.visibleToUser)
            addProperty("clickable", node.clickable)
            addProperty("longClickable", node.longClickable)
            addProperty("focusable", node.focusable)
            addProperty("focused", node.focused)
            addProperty("checkable", node.checkable)
            addProperty("checked", node.checked)
            addProperty("enabled", node.enabled)
            addProperty("scrollable", node.scrollable)
            addProperty("editable", node.editable)
            addProperty("selected", node.selected)
            addProperty("depth", node.depth)
            addProperty("childCount", node.getChildren().size)
            add("indexPath", indexPath)
            addProperty("indexInParent", node.indexInParent)
            if (node.getChildren().isNotEmpty()) {
                add("children", JsonArray().apply {
                    node.getChildren().forEach { child ->
                        add(nodeToJson(child, computeIndexPath(child)))
                    }
                })
            }
        }
    }

    private fun computeIndexPath(node: NodeInfo): JsonArray {
        val indexes = ArrayDeque<Int>()
        var current: NodeInfo? = node
        while (current?.parent != null) {
            indexes.addFirst(current.indexInParent)
            current = current.parent
        }
        return JsonArray().apply { indexes.forEach { add(it) } }
    }

    private fun countNodes(root: NodeInfo): Int {
        var count = 1
        root.getChildren().forEach { child -> count += countNodes(child) }
        return count
    }

    private fun findNodes(root: NodeInfo, matcher: (NodeInfo) -> Boolean): List<NodeInfo> {
        val result = mutableListOf<NodeInfo>()
        fun walk(node: NodeInfo) {
            if (matcher(node)) result += node
            node.getChildren().forEach(::walk)
        }
        walk(root)
        return result
    }

    private fun parseRegionArray(region: JsonArray?): IntArray? {
        if (region == null || region.size() < 4) return null
        return intArrayOf(
            region[0].asInt,
            region[1].asInt,
            region[2].asInt,
            region[3].asInt
        )
    }

    private fun toOpenCvRectReflective(region: IntArray?): Any? {
        if (region == null || region.size < 4) return null
        val rectClass = Class.forName("org.opencv.core.Rect")
        val ctor = rectClass.getConstructor(
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        return ctor.newInstance(region[0], region[1], region[2], region[3])
    }

    private fun buildNearbyRegion(anchorX: Int, anchorY: Int, radius: Int, image: ImageWrapper): IntArray {
        val left = max(0, anchorX - radius)
        val top = max(0, anchorY - radius)
        val right = min(image.width - 1, anchorX + radius)
        val bottom = min(image.height - 1, anchorY + radius)
        return intArrayOf(left, top, max(1, right - left + 1), max(1, bottom - top + 1))
    }

    private fun intersectRegion(base: IntArray?, nearby: IntArray): IntArray? {
        if (base == null || base.size < 4) return nearby
        val bLeft = base[0]
        val bTop = base[1]
        val bRight = bLeft + base[2] - 1
        val bBottom = bTop + base[3] - 1

        val nLeft = nearby[0]
        val nTop = nearby[1]
        val nRight = nLeft + nearby[2] - 1
        val nBottom = nTop + nearby[3] - 1

        val left = max(bLeft, nLeft)
        val top = max(bTop, nTop)
        val right = min(bRight, nRight)
        val bottom = min(bBottom, nBottom)
        if (right < left || bottom < top) return null
        return intArrayOf(left, top, right - left + 1, bottom - top + 1)
    }

    private fun isColorTestMatchAt(
        image: ImageWrapper,
        anchorX: Int,
        anchorY: Int,
        anchorColor: Int,
        threshold: Int,
        points: IntArray
    ): Boolean {
        if (!isPixelMatch(image, anchorX, anchorY, anchorColor, threshold)) return false
        for (i in points.indices step 3) {
            val x = anchorX + points[i]
            val y = anchorY + points[i + 1]
            val expected = points[i + 2]
            if (!isPixelMatch(image, x, y, expected, threshold)) return false
        }
        return true
    }

    private fun isPixelMatch(image: ImageWrapper, x: Int, y: Int, expectedColor: Int, threshold: Int): Boolean {
        if (x < 0 || y < 0 || x >= image.width || y >= image.height) return false
        val actual = image.pixel(x, y)
        val dr = kotlin.math.abs(Color.red(actual) - Color.red(expectedColor))
        val dg = kotlin.math.abs(Color.green(actual) - Color.green(expectedColor))
        val db = kotlin.math.abs(Color.blue(actual) - Color.blue(expectedColor))
        return dr <= threshold && dg <= threshold && db <= threshold
    }

    private fun parseColorTestPoints(points: JsonArray?): IntArray {
        if (points == null) return IntArray(0)
        val list = ArrayList<Int>(points.size() * 3)
        points.forEach { element ->
            val point = element.asJsonObject
            list += point.get("x")?.asInt ?: 0
            list += point.get("y")?.asInt ?: 0
            list += Color.parseColor(point.get("color")?.asString ?: "#000000")
        }
        return list.toIntArray()
    }

    private fun rgbToHex(color: Int): String {
        return String.format("#%02X%02X%02X", Color.red(color), Color.green(color), Color.blue(color))
    }

    private inline fun <T> ImageWrapper.useImage(block: (ImageWrapper) -> T): T {
        return try {
            block(this)
        } finally {
            runCatching { recycle() }
        }
    }

    private fun readableError(error: Throwable): String {
        var root = if (error is InvocationTargetException && error.targetException != null) {
            error.targetException
        } else {
            error
        }
        val visited = HashSet<Throwable>()
        while (root.cause != null && visited.add(root)) {
            root = root.cause!!
        }
        val message = root.message?.takeIf { it.isNotBlank() }
            ?: error.message?.takeIf { it.isNotBlank() }
            ?: "未知异常"
        return "${root.javaClass.simpleName}: $message"
    }

    private fun createErrorResponse(message: String): String {
        return JsonObject().apply {
            addProperty("type", "error")
            add("data", JsonObject().apply {
                addProperty("message", message)
            })
        }.toString()
    }

    private fun reportStatus(message: String) {
        runCatching { statusReporter?.invoke(message) }
    }
}
