package org.autojs.autojs.ui.explorer

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.stardust.pio.PFiles
import org.autojs.autojs.model.explorer.ExplorerItem
import org.autojs.autojs.model.explorer.ExplorerPage
import org.autojs.autojs.model.explorer.ExplorerProjectPage
import org.autojs.autoxjs.R
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

enum class ExplorerContextAction(val labelRes: Int) {
    COPY(R.string.explorer_action_copy),
    RENAME(R.string.text_rename),
    MOVE(R.string.explorer_action_move),
    COMPRESS(R.string.explorer_action_compress),
    EXTRACT(R.string.explorer_action_extract),
    SHARE(R.string.explorer_action_share),
    DELETE(R.string.text_delete),
    BUILD_APK(R.string.text_build_apk),
}

fun buildExplorerContextActions(item: ExplorerItem): List<ExplorerContextAction> {
    val out = ArrayList<ExplorerContextAction>()
    val file = File(item.path)
    val isDir = file.isDirectory
    val ext = file.extension.lowercase(Locale.getDefault())

    out.add(ExplorerContextAction.COPY)
    if (item.canRename()) {
        out.add(ExplorerContextAction.RENAME)
        out.add(ExplorerContextAction.MOVE)
    }
    out.add(ExplorerContextAction.COMPRESS)
    if (!isDir && (ext == "zip" || ext == "apk" || ext == "jar")) {
        out.add(ExplorerContextAction.EXTRACT)
    }
    out.add(ExplorerContextAction.SHARE)
    if (item.canDelete()) {
        out.add(ExplorerContextAction.DELETE)
    }
    when {
        item is ExplorerProjectPage -> out.add(ExplorerContextAction.BUILD_APK)
        item.isExecutable -> out.add(ExplorerContextAction.BUILD_APK)
    }
    return out
}

fun explorerItemTypeLabel(context: Context, item: ExplorerItem): String {
    if (item is ExplorerPage || item.getType() == "/") {
        return context.getString(R.string.text_directory)
    }
    val t = item.getType()
    return if (t.isEmpty() || t == ExplorerItem.TYPE_UNKNOWN) {
        context.getString(R.string.explorer_type_file)
    } else {
        t.uppercase(Locale.getDefault())
    }
}

fun formatExplorerItemDate(timeMs: Long): String {
    if (timeMs <= 0L) return ""
    return DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.MEDIUM,
        Locale.getDefault()
    ).format(Date(timeMs))
}

fun computeCopyDestination(context: Context, src: File): File {
    val parent = src.parentFile ?: return src
    val suffix = context.getString(R.string.explorer_copy_suffix)
    if (src.isDirectory) {
        var i = 0
        while (true) {
            val name = if (i == 0) "${src.name} - $suffix" else "${src.name} - $suffix ($i)"
            val f = File(parent, name)
            if (!f.exists()) return f
            i++
        }
    } else {
        val base = PFiles.getNameWithoutExtension(src.name)
        val ext = PFiles.getExtension(src.name)
        var i = 0
        while (true) {
            val name = if (ext.isEmpty()) {
                if (i == 0) "$base - $suffix" else "$base - $suffix ($i)"
            } else {
                if (i == 0) "$base - $suffix.$ext" else "$base - $suffix ($i).$ext"
            }
            val f = File(parent, name)
            if (!f.exists()) return f
            i++
        }
    }
}

fun contextMenuIconForItem(item: ExplorerItem): Int {
    return when {
        item is ExplorerProjectPage -> R.drawable.ic_project
        item is ExplorerPage -> R.drawable.ic_folder_yellow_100px
        else -> R.drawable.ic_floating_action_menu_file
    }
}

@Composable
fun ExplorerContextMenuDialog(
    title: String,
    iconRes: Int,
    actions: List<ExplorerContextAction>,
    onDismiss: () -> Unit,
    onAction: (ExplorerContextAction) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8DEF8)),
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1B1F),
                        modifier = Modifier.weight(1f),
                        maxLines = 2
                    )
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0x33FFFFFF))
                Spacer(Modifier.height(4.dp))
                actions.forEach { action ->
                    Text(
                        text = stringResource(action.labelRes),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAction(action)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        fontSize = 16.sp,
                        color = Color(0xFF1C1B1F)
                    )
                }
            }
        }
    }
}
