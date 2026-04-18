package org.autojs.autojs.ui.main.web

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.stardust.toast

/**
 * Document page has switched to an offline docs center.
 * Keep this dialog for compatibility, but only updates stored source selection.
 */
class DocumentSourceSelectDialog(private val context: Context) {
    private val documentSources = DocumentSource.values()
    private var select: DocumentSource? = null
    private val dialogBuilder = MaterialDialog.Builder(context)
        .title("选择文档源")
        .items(documentSources.map { it.sourceName })
        .itemsCallback { _, _, position, _ ->
            select = documentSources[position]
        }
        .dismissListener { _ -> persistSelection() }

    init {
        val name = EditorAppManager.getSaveStatus(context)
            .getString(EditorAppManager.DocumentSourceKEY, DocumentSource.DOC_V2_LOCAL.name)!!
        val documentSource = DocumentSource.valueOf(name)
        val i = documentSources.lastIndexOf(documentSource)
        dialogBuilder.itemsCallbackSingleChoice(if (i == -1) 0 else i) { _, _, position, _ ->
            select = documentSources[position]
            true
        }
    }

    private fun persistSelection() {
        val documentSource = select ?: return
        EditorAppManager.getSaveStatus(context)
            .edit()
            .putString(EditorAppManager.DocumentSourceKEY, documentSource.name)
            .apply()
        toast(context, "已切换: ${documentSource.sourceName}")
    }

    fun show(): MaterialDialog = dialogBuilder.show()
}