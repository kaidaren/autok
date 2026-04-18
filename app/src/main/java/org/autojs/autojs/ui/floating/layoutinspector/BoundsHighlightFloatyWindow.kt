package org.autojs.autojs.ui.floating.layoutinspector

import android.view.View
import com.stardust.enhancedfloaty.FloatyService
import com.stardust.app.GlobalAppContext
import org.autojs.autoxjs.R
import org.autojs.autojs.ui.floating.FullScreenFloatyWindow

/**
 * Transient overlay used by VSCode devtools panel when a node is selected.
 */
class BoundsHighlightFloatyWindow(
    private val boundsInScreen: android.graphics.Rect,
    private val durationMs: Long
) : FullScreenFloatyWindow() {

    override fun onCreateView(floatyService: FloatyService): View {
        // Use the same theme as other floaty windows.
        val ctx = android.view.ContextThemeWrapper(floatyService, R.style.AppTheme)
        return BoundsHighlightView(ctx).apply {
            setBounds(boundsInScreen)
        }
    }

    override fun onViewCreated(v: View) {
        val ms = if (durationMs < 0) 0L else durationMs
        GlobalAppContext.postDelayed(Runnable { close() }, ms)
    }
}

