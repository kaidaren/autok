package org.autojs.autojs.devplugin

import android.os.Build
import android.util.Log
import android.app.Application
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.core.console.LogEntry
import com.stardust.autojs.servicecomponents.ScriptServiceConnection
import com.stardust.autojs.servicecomponents.BinderConsoleListener
import com.stardust.autojs.servicecomponents.EngineController
import com.stardust.autojs.IndependentScriptService
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.FrameType
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.ByteString.Companion.toByteString
import org.autojs.autojs.devplugin.message.Hello
import org.autojs.autojs.devplugin.message.HelloResponse
import org.autojs.autojs.devplugin.message.LogData
import org.autojs.autojs.devplugin.message.Message
import org.autojs.autoxjs.BuildConfig
import org.autojs.autoxjs.R
import java.io.File
import java.net.SocketTimeoutException
import java.nio.charset.StandardCharsets

object DevPlugin {

    data class State(val state: Int, val e: Throwable? = null) {
        companion object {
            const val DISCONNECTED = 0
            const val CONNECTING = 1
            const val CONNECTED = 2
            const val CONNECTION_FAILED = 3
            const val RECONNECTING = 4
            const val HANDSHAKE_TIMEOUT = 5
        }
    }

    private val gson = Gson()
    const val SERVER_PORT = 9317
    private const val CLIENT_VERSION = 2
    private const val TAG = "DevPlugin"
    private const val TYPE_HELLO = "hello"
    private const val TYPE_PING = "ping"
    private const val TYPE_PONG = "pong"
    private const val TYPE_CLOSE = "close"
    private const val TYPE_BYTES_COMMAND = "bytes_command"
    private const val maxRetry = 3
    /**
     * VSCode `websocket` default max frame is 128KiB; chunk before we exceed a safe whole-message size.
     */
    private const val SCREEN_CAPTURE_SINGLE_FRAME_UTF8_MAX = 120 * 1024
    /** Also chunk large Base64 even if UTF-8 estimate is borderline. */
    private const val SCREEN_CAPTURE_SINGLE_FRAME_BASE64_MAX = 80_000
    /**
     * Base64 chars per chunk (multiple of 4). With JSON wrapper must stay under ~128KiB peer frame limit.
     */
    private const val SCREEN_CAPTURE_CHUNK_CHARS = 96 * 1024

    private val _connectState = MutableSharedFlow<State>()
    private val client by lazy { WebSocketClient() }
    private val server by lazy { WebSocketServer() }

    private var connection: Connection? = null
    val isActive get() = connection?.isActive ?: false
    private val bytesMap = HashMap<String, Bytes>()
    private val requiredBytesCommands = HashMap<String, JsonObject>()
    private val devToolMutex = Mutex()
    private val responseHandler: DevPluginResponseHandler by lazy {
        val cache = File(GlobalAppContext.get().cacheDir, "remote_project")
        DevPluginResponseHandler(cache)
    }
    private val devToolDebounceMutex = Mutex()
    private val devToolLastCommandAt = HashMap<String, Long>()
    private const val DEV_TOOL_DEBOUNCE_MS = 1000L
    @Volatile
    private var devToolMessageHandler: DevToolMessageHandler? = null

    private fun isMlKitAlreadyInitializedError(t: Throwable): Boolean {
        val msg = t.message?.lowercase().orEmpty()
        if (msg.contains("mlkitcontext is already initialized")) return true
        return t.cause?.message?.lowercase()?.contains("mlkitcontext is already initialized") == true
    }

    private fun ensureDevToolMessageHandler(statusReporter: ((String) -> Unit)? = null): DevToolMessageHandler? {
        devToolMessageHandler?.let { return it }
        return try {
            val autoJs = org.autojs.autojs.autojs.AutoJs.getInstance() as org.autojs.autojs.autojs.AutoJs
            DevToolMessageHandler(autoJs, statusReporter).also { devToolMessageHandler = it }
        } catch (e: UninitializedPropertyAccessException) {
            try {
                statusReporter?.invoke("初始化AutoJs中…")
                org.autojs.autojs.autojs.AutoJs.initInstance(GlobalAppContext.get() as Application)
                val autoJs = org.autojs.autojs.autojs.AutoJs.getInstance() as org.autojs.autojs.autojs.AutoJs
                DevToolMessageHandler(autoJs, statusReporter).also { devToolMessageHandler = it }
            } catch (t: Throwable) {
                Log.w(TAG, "AutoJs instance is not initialized yet for devtools", t)
                null
            }
        } catch (e: Throwable) {
            Log.e(TAG, "ensureDevToolMessageHandler failed", e)
            null
        }
    }

    private suspend fun ensureDevToolMessageHandlerReady(
        waitMs: Long = 4000L,
        statusReporter: ((String) -> Unit)? = null
    ): DevToolMessageHandler? {
        // Fast path: reuse existing handler to avoid repeated initialization churn.
        ensureDevToolMessageHandler(statusReporter)?.let { return it }

        runCatching {
            // Ensure script process service is started (more reliable on some ROMs).
            IndependentScriptService.startForeground(GlobalAppContext.get())
            ScriptServiceConnection.GlobalConnection.bind(GlobalAppContext.get())
        }
        runCatching {
            statusReporter?.invoke("等待脚本服务连接中…")
            ScriptServiceConnection.GlobalConnection.awaitConnected()
            statusReporter?.invoke("脚本服务已连接")
        }.onFailure {
            statusReporter?.invoke("脚本服务连接失败：${it.message ?: it.javaClass.simpleName}")
        }

        ensureDevToolMessageHandler(statusReporter)?.let { return it }
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < waitMs) {
            delay(200)
            ensureDevToolMessageHandler(statusReporter)?.let { return it }
        }
        return null
    }


    val connectState = _connectState.asSharedFlow()
    val isUSBDebugServiceActive get() = server.isActive

    init {
        CoroutineScope(Dispatchers.IO).launch {
            EngineController.registerGlobalConsoleListener(
                object : BinderConsoleListener {
                    override fun onPrintln(log: LogEntry) {
                        log(log.content.toString())
                    }
                }
            )
            collect()
        }

    }

    suspend fun connect(url: String) {
        withContext(Dispatchers.IO) {
            emitState(State(State.CONNECTING))
            try {
                client.connect(
                    url
                ) {
                    newConnection(this, url)
                }
            } catch (e: Exception) {
                emitState(State(State.CONNECTION_FAILED, e))
                client.close()
                e.printStackTrace()
            }
        }
    }

    fun log(log: String) {
        connection?.log(log)
    }

    suspend fun collect() {
        connectState.collect {
            when (it.state) {
                State.CONNECTING -> {
                    Log.d(TAG, "ConnectComputerSwitch: CONNECTING")
                    GlobalAppContext.toast(R.string.text_connecting)
                }

                State.RECONNECTING -> {
                    Log.d(TAG, "ConnectComputerSwitch: RECONNECTING")
                    GlobalAppContext.toast(R.string.text_reconnecting)
                }

                State.CONNECTION_FAILED -> {
                    Log.d(TAG, "ConnectComputerSwitch: CONNECTION_FAILED")
                    GlobalAppContext.toast(
                        true,
                        R.string.text_connect_failed,
                        it.e?.localizedMessage ?: ""
                    )
                    it.e?.printStackTrace()
                }

                State.HANDSHAKE_TIMEOUT -> {
                    GlobalAppContext.toast(R.string.text_handshake_failed)
                }
            }
        }
    }

    suspend fun startUSBDebug() {
        withContext(Dispatchers.IO) {
            server.listen(SERVER_PORT, "/", host = "0.0.0.0") {
                newConnection(this)
            }
        }
    }


    suspend fun stopUSBDebug() {
        withContext(Dispatchers.IO) {
            server.stop()
        }
    }

    suspend fun close() = connection?.close()

    class Connection(
        private val session: WebSocketSession,
        private var serverUrl: String? = null
    ) {
        private var lastPongId = -1L
        private val sendMutex = Mutex()
        val isActive get() = session.isActive

        suspend fun init() {
            session.shakeHandsAndHandle()
        }

        private suspend fun WebSocketSession.shakeHandsAndHandle() {
            shakeHands {
                handle()
            }
        }

        suspend fun WebSocketSession.handle() {
            emitState(State(State.CONNECTED))
            withContext(Dispatchers.IO) {
                launch {
                    mapToData(
                        onJson = {
                            onJsonData(it)
                        },
                        onBytes = {
                            onBytesData(it)
                        }
                    )
                }
            }
        }

        private suspend fun WebSocketSession.shakeHands(onSuccess: suspend () -> Unit) {
            val message = Message(
                type = TYPE_HELLO,
                data = Hello(
                    deviceName = Build.BRAND + " " + Build.MODEL,
                    clientVersion = CLIENT_VERSION,
                    appVersion = BuildConfig.VERSION_NAME,
                    appVersionCode = BuildConfig.VERSION_CODE
                )
            )
            send(gson.toJson(message))
            val frame = incoming.receive()
            serveHello(
                frame = frame,
                ok = { resp ->
                    coroutineScope {
                        launch { onSuccess() }
                        if (resp.versionCode() >= 11090) {
                            launch { ping() }
                        }
                    }
                },
                fail = {
                    onHandshakeTimeout()
                }
            )
        }

        private suspend fun onJsonData(element: JsonElement) {
            if (!element.isJsonObject) {
                Log.w(TAG, "onSocketData: not json object: $element")
                return
            }
            try {
                val obj = element.asJsonObject
                val type = obj["type"] ?: return
                if (!type.isJsonPrimitive) return
                when (type.asString) {
                    TYPE_PONG -> {
                        lastPongId = obj["data"].asLong
                    }

                    TYPE_CLOSE -> {
                        this.close()
                    }

                    TYPE_BYTES_COMMAND -> {
                        val md5 = obj["md5"].asString
                        bytesMap.remove(md5)?.let {
                            handleBytes(obj, it)
                        } ?: kotlin.run {
                            requiredBytesCommands[md5] = obj
                        }
                    }

                    "screen_capture",
                    "get_color",
                    "dump_node_tree",
                    "find_nodes_by_text",
                    "find_nodes_by_id",
                    "highlight_node",
                    "color_test" -> {
                        // Run devtool command off the reader loop; keep pong processing responsive.
                        CoroutineScope(Dispatchers.IO).launch {
                            handleDevToolMessage(obj)
                        }
                    }

                    else -> responseHandler.handle(obj)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private suspend fun handleDevToolMessage(obj: JsonObject) {
            withContext(Dispatchers.IO) {
                devToolMutex.withLock {
                    try {
                        val commandType = obj["type"]?.asString.orEmpty()
                        if (commandType.isNotEmpty() && shouldDebounceDevToolCommand(commandType)) {
                            runCatching { log("[devtool] 忽略${DEV_TOOL_DEBOUNCE_MS}ms内重复指令: $commandType") }
                            return@withLock
                        }
                        val reporter: (String) -> Unit = { step ->
                            runCatching { log("[devtool] $step") }
                        }
                        val handler = ensureDevToolMessageHandlerReady(waitMs = 25000L, statusReporter = reporter)
                        if (handler == null) {
                            if (session.isActive) {
                                sendSafely("""{"type":"error","data":{"message":"手机端服务拉起超时，请保持应用前台并重试截图"}}""")
                            }
                            return@withLock
                        }
                        handler.handleMessage(obj.toString())?.let { responseJson ->
                            if (session.isActive) {
                                if (commandType == "screen_capture") {
                                    sendScreenCaptureResponsePossiblyChunked(responseJson)
                                } else {
                                    sendSafely(responseJson)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "handleDevToolMessage failed", e)
                        runCatching { GlobalAppContext.toast("调试指令处理失败：${e.message ?: "unknown"}") }
                        val errorMessage = e.message ?: "unknown error"
                        if (session.isActive) {
                            sendSafely("""{"type":"error","data":{"message":"$errorMessage"}}""")
                        }
                    }
                }
                Unit
            }
        }

        private suspend fun shouldDebounceDevToolCommand(type: String): Boolean {
            val now = System.currentTimeMillis()
            return devToolDebounceMutex.withLock {
                val last = devToolLastCommandAt[type]
                devToolLastCommandAt[type] = now
                last != null && now - last < DEV_TOOL_DEBOUNCE_MS
            }
        }

        private suspend fun onBytesData(bytes: Bytes) {
            requiredBytesCommands.remove(bytes.md5)?.let { command ->
                handleBytes(command, bytes)
            } ?: kotlin.run {
                bytesMap[bytes.md5] = bytes
            }
        }


        private suspend fun ping() {
            while (true) {
                Log.d(TAG, "ping")
                val ping = Message(
                    type = TYPE_PING,
                    data = System.currentTimeMillis()
                )
                sendSafely(gson.toJson(ping))
                delay(10000)
                if (lastPongId != ping.data) {
                    Log.d(TAG, "ping: $lastPongId != ${ping.data}")
                    if (session.isActive) {
                        coroutineScope {
                            launch {
                                reconnect()
                            }
                        }
                    }
                    break
                }
            }
        }

        suspend fun newConnection(session: WebSocketSession, serverUrl: String? = null) =
            DevPlugin.newConnection(session, serverUrl)

        private suspend fun reconnect() {
            serverUrl?.let { url ->
                Log.i(TAG, "reconnect")
                emitDisconnect(session)
                var ok = false
                for (i in 0 until maxRetry) {
                    emitState(State(State.RECONNECTING))
                    try {
                        client.connect(url) {
                            ok = true
                            newConnection(this, url)
                        }
                    } catch (e: Exception) {
                        if (i == maxRetry - 1) {
                            emitState(State(State.CONNECTION_FAILED, e))
                        }
                        client.close()
                        e.printStackTrace()
                    }
                    if (ok) break
                }
            }
        }


        private suspend fun onHandshakeTimeout() {
            Log.i(TAG, "onHandshakeTimeout")
            emitState(State(State.HANDSHAKE_TIMEOUT))
            close(e = SocketTimeoutException("handshake timeout"))
        }


        private suspend fun handleBytes(obj: JsonObject, bytes: Bytes) {
            val projectDir = responseHandler.handleBytes1(obj, bytes)
            obj["data"].asJsonObject.add("dir", JsonPrimitive(projectDir.path))
            responseHandler.handle(obj)
        }

        private suspend fun serveHello(
            frame: Frame,
            ok: suspend (data: HelloResponse) -> Unit,
            fail: suspend () -> Unit
        ) {
            if (frame is Frame.Text) {
                val msg = frame.readText()
                val data =
                    kotlin.runCatching { gson.fromJson(msg, HelloResponse::class.java) }.getOrNull()
                Log.d(TAG, "serveHello: " + data?.toString())
                data?.let {
                    val okData = if (it.versionCode() >= 11090) "ok" else "连接成功"
                    if (it.type == TYPE_HELLO && it.data == okData) {
                        ok(data)
                        return
                    }
                }
                fail()
            }
        }

        fun log(log: String) {
            if (!session.isActive) return
            val data = Message(
                type = "log",
                data = LogData(log = log)
            )
            runBlocking {
                sendSafely(gson.toJson(data))
            }
        }

        private suspend fun sendSafely(text: String) {
            sendMutex.withLock {
                if (session.isActive) {
                    session.send(text)
                }
            }
        }

        /**
         * Large PNG Base64 in one JSON frame can drop the VSCode side WebSocket; split into UTF-8 chunks.
         */
        private suspend fun sendScreenCaptureResponsePossiblyChunked(responseJson: String) {
            val root = runCatching {
                JsonParser.parseString(responseJson).takeIf { it.isJsonObject }?.asJsonObject
            }.getOrNull()
            if (root == null || root.get("type")?.asString != "screen_capture_result") {
                sendSafely(responseJson)
                return
            }
            val data = root.getAsJsonObject("data") ?: run {
                sendSafely(responseJson)
                return
            }
            val base64Prim = data.get("base64") ?: run {
                sendSafely(responseJson)
                return
            }
            if (!base64Prim.isJsonPrimitive || !base64Prim.asJsonPrimitive.isString) {
                sendSafely(responseJson)
                return
            }
            val b64 = base64Prim.asString
            val utf8Len = responseJson.toByteArray(StandardCharsets.UTF_8).size
            if (utf8Len <= SCREEN_CAPTURE_SINGLE_FRAME_UTF8_MAX &&
                b64.length <= SCREEN_CAPTURE_SINGLE_FRAME_BASE64_MAX
            ) {
                sendSafely(responseJson)
                return
            }
            val captureId = "${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}"
            val chunkChars = SCREEN_CAPTURE_CHUNK_CHARS
            val ranges = ArrayList<Pair<Int, Int>>()
            var offset = 0
            while (offset < b64.length) {
                val remaining = b64.length - offset
                val take = if (remaining <= chunkChars) {
                    remaining
                } else {
                    (chunkChars / 4) * 4
                }
                ranges.add(offset to offset + take)
                offset += take
            }
            val beginData = JsonObject().apply {
                addProperty("captureId", captureId)
                addProperty("chunkTotal", ranges.size)
                for ((k, v) in data.entrySet()) {
                    if (k != "base64") add(k, v)
                }
            }
            sendSafely(
                JsonObject().apply {
                    addProperty("type", "screen_capture_begin")
                    add("data", beginData)
                }.toString()
            )
            ranges.forEachIndexed { index, range ->
                val chunkData = JsonObject().apply {
                    addProperty("captureId", captureId)
                    addProperty("index", index)
                    addProperty("base64", b64.substring(range.first, range.second))
                }
                sendSafely(
                    JsonObject().apply {
                        addProperty("type", "screen_capture_chunk")
                        add("data", chunkData)
                    }.toString()
                )
            }
            sendSafely(
                JsonObject().apply {
                    addProperty("type", "screen_capture_end")
                    add(
                        "data",
                        JsonObject().apply {
                            addProperty("captureId", captureId)
                        }
                    )
                }.toString()
            )
        }

        suspend fun close(
            reason: CloseReason = CloseReason(CloseReason.Codes.NORMAL, ""),
            e: Throwable? = null
        ) {
            Log.i(TAG, "close: ${reason.message}")
            e?.printStackTrace()
            emitDisconnect(session, e)
        }

        suspend fun WebSocketSession.mapToData(
            onJson: suspend WebSocketSession.(JsonElement) -> Unit,
            onBytes: suspend WebSocketSession.(Bytes) -> Unit,
        ) {
            this.handleSession(
                onMessage = { frame ->
                    when (frame.frameType) {
                        FrameType.TEXT -> {
                            val text = (frame as Frame.Text).readText()
                            JsonUtil.dispatchJson(text)?.let { onJson(it) }
                        }

                        FrameType.BINARY -> {
                            val bytes = frame.readBytes()
                            val md5 = bytes.toByteString(0, bytes.size).md5().hex()
                            onBytes(Bytes(md5, bytes))
                        }

                        else -> {}
                    }
                },
                onError = { e ->
                    Log.i(TAG, "onError: ${e.message}")
                    e.printStackTrace()
                    emitDisconnect(this, e)
                },
                onClose = { e ->
                    Log.i(TAG, "onClose: ${e.message}")
                    e.printStackTrace()
                    emitDisconnect(this, e)
                },
                onFinally = {
                    Log.i(TAG, "onFinally")
                    emitDisconnect(this)
                }
            )

        }

        private suspend fun emitDisconnect(session: WebSocketSession?, e: Throwable? = null) {
            emitState(State(State.DISCONNECTED, e))
            session?.close()
            session?.cancel()
        }

        private suspend fun WebSocketSession.handleSession(
            onMessage: suspend WebSocketSession.(frame: Frame) -> Unit = { },
            onError: suspend WebSocketSession.(e: Throwable) -> Unit = { },
            onClose: suspend WebSocketSession.(e: ClosedReceiveChannelException) -> Unit = { },
            onFinally: suspend WebSocketSession.() -> Unit = { },
        ) {
            try {
                for (frame in incoming) {
                    onMessage(frame)
                }
            } catch (e: ClosedReceiveChannelException) {
                onClose(e)
            } catch (e: Throwable) {
                onError(e)
            } finally {
                onFinally()
            }
        }

        suspend fun emitState(state: State) {
            if (connection === this) _connectState.emit(state)
        }
    }

    suspend fun newConnection(session: WebSocketSession, serverUrl: String? = null) {
        val connection = Connection(session, serverUrl)
        this@DevPlugin.connection = connection
        connection.init()
    }

    suspend fun emitState(state: State) = _connectState.emit(state)
}