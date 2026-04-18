package com.stardust.autojs.core.image.capture

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Script-process variant of screen capture permission activity.
 *
 * IMPORTANT: This class intentionally mirrors [ScreenCapturePermissionActivity].
 * The request/response callback is stored in static memory, so the requester and activity
 * must run in the same process.
 */
class ScreenCapturePermissionActivityScript : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requestId = intent?.getLongExtra(EXTRA_REQUEST_ID, -1L) ?: -1L
        if (requestId < 0) {
            finish()
            return
        }

        val launcher = registerForActivityResult(ScreenCaptureManager.ScreenCaptureRequester()) { data ->
            callbacks.remove(requestId)?.invoke(data)
            finish()
        }
        launcher.launch(this)
    }

    companion object {
        private const val EXTRA_REQUEST_ID = "extra_request_id"
        private val requestIdGen = AtomicLong(2000L)
        private val callbacks = ConcurrentHashMap<Long, (Intent?) -> Unit>()

        fun request(context: Context, callback: (Intent?) -> Unit) {
            val requestId = requestIdGen.getAndIncrement()
            callbacks[requestId] = callback
            val intent = Intent(context, ScreenCapturePermissionActivityScript::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_REQUEST_ID, requestId)
            }
            try {
                context.startActivity(intent)
            } catch (t: Throwable) {
                callbacks.remove(requestId)
                callback(null)
            }
        }
    }
}

