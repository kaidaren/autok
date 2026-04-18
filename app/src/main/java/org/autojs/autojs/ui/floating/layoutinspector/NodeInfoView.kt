package org.autojs.autojs.ui.floating.layoutinspector

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.stardust.util.ClipboardUtil
import com.stardust.util.sortedArrayOf
import com.stardust.view.accessibility.NodeInfo
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration
import org.autojs.autoxjs.R
import java.lang.reflect.Field

/**
 * Created by Stardust on 2017/3/10.
 */

class NodeInfoView : RecyclerView {

    private sealed interface RowItem {
        data object Header : RowItem
        data class Section(val title: String) : RowItem
        data class Item(val name: String, val value: String) : RowItem
    }

    private val items = mutableListOf<RowItem>()

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }

    fun setNodeInfo(nodeInfo: NodeInfo) {
        val map = HashMap<String, String>(FIELD_NAMES.size)
        for (i in FIELDS.indices) {
            try {
                val value = FIELDS[i].get(nodeInfo)
                map[FIELD_NAMES[i]] = value?.toString() ?: ""
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }

        items.clear()
        items.add(RowItem.Header)
        items.add(RowItem.Section("基础属性"))
        addItem(map, "packageName")
        addItem(map, "id")
        addItem(map, "fullId")
        addItem(map, "idHex")
        addItem(map, "text")
        addItem(map, "desc")
        addItem(map, "className")
        addItem(map, "bounds")
        items.add(RowItem.Section("行为属性"))
        addItem(map, "enabled")
        addItem(map, "clickable")
        addItem(map, "longClickable")
        addItem(map, "scrollable")
        addItem(map, "focusable")
        addItem(map, "checked")
        addItem(map, "selected")
        addItem(map, "accessibilityFocused")
        addItem(map, "editable")
        addItem(map, "dismissable")
        addItem(map, "contextClickable")
        addItem(map, "drawingOrder")
        addItem(map, "indexInParent")
        addItem(map, "row")
        addItem(map, "rowCount")
        addItem(map, "rowSpan")
        addItem(map, "column")
        addItem(map, "columnCount")
        addItem(map, "columnSpan")

        adapter?.notifyDataSetChanged()
    }

    private fun init() {
        setBackgroundColor(0xFF0F141A.toInt())
        adapter = Adapter()
        layoutManager = LinearLayoutManager(context)
        addItemDecoration(HorizontalDividerItemDecoration.Builder(context)
                .color(0x33242C39)
                .size(1)
                .build())
    }

    private fun initData() {
        // no-op (kept for binary compatibility if referenced elsewhere)
    }

    private inner class Adapter : RecyclerView.Adapter<ViewHolder>() {

        internal val VIEW_TYPE_HEADER = 0
        internal val VIEW_TYPE_SECTION = 1
        internal val VIEW_TYPE_ITEM = 2


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val layoutRes = when (viewType) {
                VIEW_TYPE_HEADER -> R.layout.node_info_view_header
                VIEW_TYPE_SECTION -> R.layout.node_info_section_header
                else -> R.layout.node_info_view_item
            }
            return ViewHolder(LayoutInflater.from(parent.context).inflate(layoutRes, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            when (val row = items[position]) {
                is RowItem.Header -> {
                    holder.attrName?.text = resources.getString(R.string.text_attribute)
                    holder.attrValue?.text = resources.getString(R.string.text_value)
                }

                is RowItem.Section -> {
                    holder.sectionTitle?.text = row.title
                }

                is RowItem.Item -> {
                    holder.attrName?.text = row.name
                    holder.attrValue?.text = row.value
                    val isHighlight = row.name == "text"
                    holder.itemView.setBackgroundResource(
                        if (isHighlight) R.drawable.node_info_highlight else R.drawable.node_info_item_bg
                    )
                    holder.attrName?.setTextColor(
                        when {
                            isHighlight -> 0xB310B981.toInt()
                            row.name == "bounds" -> 0xFF86E8C8.toInt()
                            row.name == "id" || row.name == "fullId" -> 0xFFA8ABB3.toInt()
                            else -> 0xFFA8ABB3.toInt()
                        }
                    )
                    holder.attrValue?.setTextColor(
                        when {
                            isHighlight -> 0xFF9CFF93.toInt()
                            row.name == "id" || row.name == "fullId" -> 0xFF00F4FE.toInt()
                            row.name == "bounds" -> 0xFF10B981.toInt()
                            row.name == "clickable" || row.name == "enabled" || row.name == "focusable" -> 0xFFB9C0CD.toInt()
                            else -> 0xFFF1F3FC.toInt()
                        }
                    )
                }
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is RowItem.Header -> VIEW_TYPE_HEADER
                is RowItem.Section -> VIEW_TYPE_SECTION
                is RowItem.Item -> VIEW_TYPE_ITEM
            }
        }
    }

    internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val attrName: TextView? = itemView.findViewById(R.id.name)
        val attrValue: TextView? = itemView.findViewById(R.id.value)
        val sectionTitle: TextView? = itemView.findViewById(R.id.title)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                val row = items.getOrNull(pos)
                if (row !is RowItem.Item)
                    return@setOnClickListener
                ClipboardUtil.setClip(context, row.name + "(\"" + row.value + "\")")
                Snackbar.make(this@NodeInfoView, R.string.text_already_copy_to_clip, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(0xFF121820.toInt())
                    .setTextColor(0xFF9CFF93.toInt())
                    .show()
            }
        }

    }

    companion object {

        private val FIELD_NAMES = sortedArrayOf(
                "id",
                "idHex",
                "fullId",
                "bounds",
                "depth",
                "desc",
                "className",
                "packageName",
                "text",
                "drawingOrder",
                "accessibilityFocused",
                "checked",
                "clickable",
                "contextClickable",
                "dismissable",
                "editable",
                "enabled",
                "focusable",
                "indexInParent",
                "longClickable",
                "row",
                "rowCount",
                "rowSpan",
                "column",
                "columnCount",
                "columnSpan",
                "selected",
                "scrollable")
        private val FIELDS = Array<Field>(FIELD_NAMES.size) {
            val field = NodeInfo::class.java.getDeclaredField(FIELD_NAMES[it])
            field.isAccessible = true
            field
        }
    }

    private fun addItem(map: Map<String, String>, name: String) {
        if (!map.containsKey(name)) return
        items.add(RowItem.Item(name, map[name].orEmpty()))
    }

}