package org.autojs.autojs.ui.floating.layoutinspector

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.stardust.util.ViewUtil

/**
 * Draw a transient bounding-box highlight for VSCode devtools.
 */
class BoundsHighlightView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val statusBarHeight: Int = ViewUtil.getStatusBarHeight(context)

    private val fillPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        // VSCode panel uses rgba(255,59,48,0.2) => alpha ~= 0x33
        color = Color.parseColor("#33FF3B30")
    }

    private val strokePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#FFFF3B30")
    }

    private var boundsInScreen: Rect? = null

    fun setBounds(bounds: Rect) {
        boundsInScreen = Rect(bounds)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rect = boundsInScreen ?: return
        if (rect.width() <= 0 || rect.height() <= 0) return

        val offsetRect = Rect(rect)
        offsetRect.offset(0, -statusBarHeight)
        canvas.drawRect(offsetRect, fillPaint)
        canvas.drawRect(offsetRect, strokePaint)
    }
}

