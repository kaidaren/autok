package org.autojs.autojs.devplugin

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ScreenCaptureGuideActivity : AppCompatActivity() {

    private val finishHandler = Handler(Looper.getMainLooper())
    private val autoFinish = Runnable { finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(false)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.parseColor("#131A23"))
            setPadding(56, 72, 56, 56)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val title = TextView(this).apply {
            text = "需要录屏权限"
            setTextColor(Color.WHITE)
            textSize = 21f
            gravity = Gravity.CENTER
        }
        val desc = TextView(this).apply {
            text = "下一步将弹出系统录屏授权框。\n请点击“继续授权”，并在系统弹窗选择“立即开始/允许”。"
            setTextColor(Color.parseColor("#DDE3EE"))
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 36)
        }
        val action = Button(this).apply {
            text = "继续授权"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2F80ED"))
            minWidth = 360
            setOnClickListener { finish() }
        }

        root.addView(title)
        root.addView(desc)
        root.addView(action)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        finishHandler.removeCallbacks(autoFinish)
        finishHandler.postDelayed(autoFinish, 3200)
    }

    override fun onPause() {
        super.onPause()
        finishHandler.removeCallbacks(autoFinish)
    }
}

