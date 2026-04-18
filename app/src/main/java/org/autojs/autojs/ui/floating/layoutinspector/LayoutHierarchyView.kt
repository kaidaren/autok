package org.autojs.autojs.ui.floating.layoutinspector

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.util.TypedValue
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import com.stardust.util.ViewUtil
import com.stardust.view.accessibility.NodeInfo
import org.autojs.autojs.ui.widget.LevelBeamView
import org.autojs.autoxjs.R
import pl.openrnd.multilevellistview.*
import java.util.*

/**
 * Created by Stardust on 2017/3/10.
 */
open class LayoutHierarchyView : MultiLevelListView {
    interface OnItemLongClickListener {
        fun onItemLongClick(view: View, nodeInfo: NodeInfo)
    }

    private var mAdapter: Adapter? = null
    private var mOnItemLongClickListener: ((view: View, nodeInfo: NodeInfo) -> Unit)? = null
    private var onItemTouchListener: ((view: View, event: MotionEvent) -> Boolean)? = null
    private val mOnItemLongClickListenerProxy =
        AdapterView.OnItemLongClickListener { _, view, _, _ ->
            (view.tag as ViewHolder).nodeInfo?.let {
                mOnItemLongClickListener?.invoke(view, it)
                return@OnItemLongClickListener true
            }
            false
        }


    var boundsPaint: Paint? = null
        private set
    private var mBoundsInScreen: IntArray? = null
    var mStatusBarHeight = 0
    var mClickedNodeInfo: NodeInfo? = null
    private var mClickedView: View? = null
    private var mOriginalBackground: Drawable? = null
    var mShowClickedNodeBounds = false
    private var mRootNode: NodeInfo? = null
    private val mInitiallyExpandedNodes: MutableSet<NodeInfo?> = HashSet()
    private var mFilterQuery: String = ""
    private var mFilteredChildren: IdentityHashMap<NodeInfo, List<NodeInfo>>? = null

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    fun setShowClickedNodeBounds(showClickedNodeBounds: Boolean) {
        mShowClickedNodeBounds = showClickedNodeBounds
    }

    fun setClickedColor(@Suppress("UNUSED_PARAMETER") clickedColor: Int) = Unit

    @SuppressLint("ClickableViewAccessibility")
    private fun init() {
        mAdapter = Adapter()
        setAdapter(mAdapter)
        nestType = NestType.MULTIPLE
        (getChildAt(0) as ListView).apply {
            setOnTouchListener { view, motionEvent ->
                return@setOnTouchListener onItemTouchListener?.invoke(view, motionEvent) ?: false
            }
            onItemLongClickListener = mOnItemLongClickListenerProxy
        }
        setWillNotDraw(false)
        initPaint()
        setOnItemClickListener(object : OnItemClickListener {
            override fun onItemClicked(
                parent: MultiLevelListView,
                view: View,
                item: Any,
                itemInfo: ItemInfo
            ) {
                setClickedItem(view, item as NodeInfo)
            }

            override fun onGroupItemClicked(
                parent: MultiLevelListView,
                view: View,
                item: Any,
                itemInfo: ItemInfo
            ) {
                setClickedItem(view, item as NodeInfo)
            }
        })
    }

    private fun setClickedItem(view: View, item: NodeInfo) {
        mClickedNodeInfo = item
        if (mClickedView == null) {
            mOriginalBackground = view.background
        } else {
            mClickedView!!.background = mOriginalBackground
        }
        view.setBackgroundResource(R.drawable.node_tree_item_selected_bg)
        mClickedView = view
        invalidate()
    }

    private fun initPaint() {
        boundsPaint = Paint()
        boundsPaint!!.color = Color.parseColor("#00F4FE")
        boundsPaint!!.style = Paint.Style.STROKE
        boundsPaint!!.isAntiAlias = true
        boundsPaint!!.strokeWidth = 4f
        mStatusBarHeight = ViewUtil.getStatusBarHeight(context)
    }

    fun setRootNode(rootNodeInfo: NodeInfo) {
        mRootNode = rootNodeInfo
        mAdapter!!.setDataItems(listOf(rootNodeInfo))
        mClickedNodeInfo = null
        mInitiallyExpandedNodes.clear()
        mFilterQuery = ""
        mFilteredChildren = null
    }

    fun setFilterQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed == mFilterQuery) return
        mFilterQuery = trimmed

        val root = mRootNode
        if (root == null) return

        if (mFilterQuery.isEmpty()) {
            mFilteredChildren = null
            mInitiallyExpandedNodes.clear()
            mAdapter?.reloadData()
            return
        }

        val map = IdentityHashMap<NodeInfo, List<NodeInfo>>()
        val expanded = HashSet<NodeInfo?>()

        fun matches(node: NodeInfo): Boolean {
            val q = mFilterQuery.lowercase(Locale.getDefault())
            val className = (node.className?.toString() ?: "").lowercase(Locale.getDefault())
            val idText = node.id.toString().lowercase(Locale.getDefault())
            val textValue = node.text.toString().lowercase(Locale.getDefault())
            val descValue = node.desc.toString().lowercase(Locale.getDefault())
            return className.contains(q) || idText.contains(q) || textValue.contains(q) || descValue.contains(q)
        }

        fun build(node: NodeInfo): Boolean {
            val children = node.getChildren()
            val keepChildren = ArrayList<NodeInfo>(children.size)
            var anyChildKept = false
            for (c in children) {
                if (build(c)) {
                    keepChildren.add(c)
                    anyChildKept = true
                }
            }
            val keepSelf = matches(node) || anyChildKept
            if (keepSelf) {
                map[node] = keepChildren
                if (keepChildren.isNotEmpty()) expanded.add(node)
            }
            return keepSelf
        }

        build(root)
        mFilteredChildren = map
        mInitiallyExpandedNodes.clear()
        mInitiallyExpandedNodes.addAll(expanded)
        mAdapter?.reloadData()
    }

    fun setOnItemTouchListener(listener: ((view: View, event: MotionEvent) -> Boolean)) {
        onItemTouchListener = listener
    }

    fun setOnItemLongClickListener(onNodeInfoSelectListener: (view: View, nodeInfo: NodeInfo) -> Unit) {
        mOnItemLongClickListener = onNodeInfoSelectListener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mBoundsInScreen == null) {
            mBoundsInScreen = IntArray(4)
            getLocationOnScreen(mBoundsInScreen)
            mStatusBarHeight = mBoundsInScreen!![1]
        }
        if (mShowClickedNodeBounds && mClickedNodeInfo != null) {
            LayoutBoundsView.drawRect(
                canvas,
                mClickedNodeInfo!!.boundsInScreen,
                mStatusBarHeight,
                boundsPaint
            )
        }
    }

    fun setSelectedNode(selectedNode: NodeInfo) {
        mInitiallyExpandedNodes.clear()
        val parents = Stack<NodeInfo?>()
        searchNodeParents(selectedNode, mRootNode, parents)
        mClickedNodeInfo = parents.peek()
        mInitiallyExpandedNodes.addAll(parents)
        mAdapter!!.reloadData()
    }

    private fun searchNodeParents(
        nodeInfo: NodeInfo,
        rootNode: NodeInfo?,
        stack: Stack<NodeInfo?>
    ): Boolean {
        stack.push(rootNode)
        if (nodeInfo == rootNode) {
            return true
        }
        var found = false
        for (child in rootNode!!.getChildren()) {
            if (searchNodeParents(nodeInfo, child, stack)) {
                found = true
                break
            }
        }
        if (!found) {
            stack.pop()
        }
        return found
    }

    private inner class ViewHolder internal constructor(view: View) {
        var nameView: TextView
        var infoView: TextView
        var levelIndexView: TextView
        var badgeView: TextView
        var arrowView: ImageView
        var levelBeamView: LevelBeamView
        var nodeInfo: NodeInfo? = null

        init {
            infoView = view.findViewById<View>(R.id.dataItemInfo) as TextView
            nameView = view.findViewById<View>(R.id.dataItemName) as TextView
            levelIndexView = view.findViewById<View>(R.id.dataItemLevelIndex) as TextView
            badgeView = view.findViewById<View>(R.id.dataItemBadge) as TextView
            arrowView = view.findViewById<View>(R.id.dataItemArrow) as ImageView
            levelBeamView = view.findViewById<View>(R.id.dataItemLevelBeam) as LevelBeamView
        }
    }

    private inner class Adapter : MultiLevelListAdapter() {
        override fun getSubObjects(`object`: Any): List<*> {
            val node = `object` as NodeInfo
            val map = mFilteredChildren
            return if (map == null) {
                node.getChildren()
            } else {
                map[node] ?: emptyList<NodeInfo>()
            }
        }

        override fun isExpandable(`object`: Any): Boolean {
            val node = `object` as NodeInfo
            val map = mFilteredChildren
            return if (map == null) {
                node.getChildren().isNotEmpty()
            } else {
                (map[node]?.isNotEmpty() == true)
            }
        }

        override fun isInitiallyExpanded(`object`: Any): Boolean {
            return mInitiallyExpandedNodes.contains(`object` as NodeInfo)
        }

        public override fun getViewForObject(
            `object`: Any,
            convertView: View?,
            itemInfo: ItemInfo
        ): View {
            val nodeInfo = `object` as NodeInfo
            val viewHolder: ViewHolder
            val convertView1 = if (convertView != null) {
                viewHolder = convertView.tag as ViewHolder
                convertView
            } else {
                val convertView2 =
                    LayoutInflater.from(context).inflate(R.layout.layout_hierarchy_view_item, null)
                viewHolder = ViewHolder(convertView2)
                convertView2.tag = viewHolder
                convertView2
            }

            viewHolder.nameView.text = simplifyClassName(nodeInfo.className)
            viewHolder.nodeInfo = nodeInfo
            viewHolder.levelIndexView.text = itemInfo.level.toString()
            viewHolder.infoView.visibility = VISIBLE
            viewHolder.infoView.text = buildInfoText(nodeInfo)
            applyStrictNodeTypography(viewHolder)
            val isSelected = nodeInfo == mClickedNodeInfo
            val isExpandable = itemInfo.isExpandable
            val isExpanded = itemInfo.isExpanded
            viewHolder.nameView.paintFlags =
                if (isSelected) viewHolder.nameView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                else viewHolder.nameView.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
            viewHolder.nameView.setTypeface(
                null,
                if (isSelected) Typeface.BOLD else Typeface.NORMAL
            )
            viewHolder.nameView.setTextColor(
                when {
                    isSelected -> Color.parseColor("#34D399")
                    isExpandable -> Color.parseColor("#2FD19A")
                    else -> Color.parseColor("#E5E7EB")
                }
            )
            viewHolder.infoView.setTextColor(
                when {
                    isSelected -> Color.parseColor("#669DE28A")
                    isExpandable -> Color.parseColor("#667085")
                    else -> Color.parseColor("#6B7280")
                }
            )
            viewHolder.levelIndexView.setTextColor(
                if (isSelected) Color.parseColor("#8AB4A3") else Color.parseColor("#5C6370")
            )
            viewHolder.arrowView.visibility = VISIBLE
            viewHolder.arrowView.setImageResource(
                if (isExpandable) R.drawable.ic_expand_more else R.drawable.ic_chevron_right
            )
            viewHolder.arrowView.rotation = if (isExpandable && !isExpanded) -90f else 0f
            viewHolder.arrowView.imageTintList = android.content.res.ColorStateList.valueOf(
                when {
                    isSelected -> Color.parseColor("#34D399")
                    isExpandable -> Color.parseColor("#10B981")
                    else -> Color.parseColor("#5C6370")
                }
            )
            viewHolder.badgeView.visibility =
                if (itemInfo.isExpandable && itemInfo.isExpanded) VISIBLE else GONE
            viewHolder.levelBeamView.setLevel(itemInfo.level)
            if (nodeInfo == mClickedNodeInfo) {
                convertView1?.let { setClickedItem(it, nodeInfo) }
            }
            return convertView1!!
        }

        private fun simplifyClassName(className: CharSequence?): String? {
            if (className == null) return null
            var s = className.toString()
            if (s.startsWith("android.widget.")) {
                s = s.substring(15)
            }
            return s
        }

        private fun getItemInfoDsc(itemInfo: ItemInfo): String {
            val builder = StringBuilder()
            builder.append(
                String.format(
                    Locale.getDefault(), "level[%d], idx in level[%d/%d]",
                    itemInfo.level + 1,  /*Indexing starts from 0*/
                    itemInfo.idxInLevel + 1 /*Indexing starts from 0*/,
                    itemInfo.levelSize
                )
            )
            if (itemInfo.isExpandable) {
                builder.append(String.format(", expanded[%b]", itemInfo.isExpanded))
            }
            return builder.toString()
        }
    }

    private fun buildInfoText(nodeInfo: NodeInfo): String {
        val parts = ArrayList<String>(2)
        val idText = nodeInfo.id.toString().trim()
        if (idText.isNotEmpty()) parts.add("id/$idText")
        val textValue = nodeInfo.text.toString().trim()
        if (textValue.isNotEmpty()) parts.add("\"$textValue\"")
        if (parts.isEmpty()) {
            val descText = nodeInfo.desc.toString().trim()
            if (descText.isNotEmpty()) parts.add(descText)
        }
        return parts.joinToString(" · ")
    }

    private fun applyStrictNodeTypography(viewHolder: ViewHolder) {
        // Use density-based px so it won't inflate with system font scale.
        fun setFixedTextSize(view: TextView, dpSize: Float) {
            val px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpSize,
                resources.displayMetrics
            )
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, px)
        }
        setFixedTextSize(viewHolder.nameView, 10.5f)
        setFixedTextSize(viewHolder.infoView, 8.5f)
        setFixedTextSize(viewHolder.levelIndexView, 8f)
        setFixedTextSize(viewHolder.badgeView, 7f)
    }
}