package org.autojs.autojs.ui.explorer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import org.autojs.autojs.ui.filechooser.FileChooseListView
import org.autojs.autoxjs.R

@Composable
fun FileIcon(text: String, background: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            color = Color(0xFFFFFFFF),
            fontSize = 24.sp
        )
    }
}

@Composable
fun FileInfo(modifier: Modifier = Modifier, name: String, desc: String) {
    Column(modifier = modifier) {
        Text(
            text = name,
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFF1F3FC),
            maxLines = 1,
            fontSize = 14.sp
        )
        Text(
            text = desc,
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFA8ABB3),
            maxLines = 1,
            fontSize = 11.sp
        )
    }
}

@Composable
fun ExplorerListRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showCheckbox: Boolean,
    leadingIcon: @Composable () -> Unit,
    name: String,
    typeLabel: String,
    dateLabel: String,
    onRowClick: () -> Unit,
    showMore: Boolean,
    onMoreClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF2A3038),
            contentColor = Color(0xFFF1F3FC)
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showCheckbox) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = onCheckedChange
                )
                Spacer(Modifier.width(4.dp))
            }
            leadingIcon()
            Spacer(Modifier.width(10.dp))
            Column(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onRowClick)
            ) {
                Text(
                    text = name,
                    color = Color(0xFFF1F3FC),
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Text(
                    text = typeLabel,
                    color = Color(0xFFA8ABB3),
                    fontSize = 11.sp,
                    maxLines = 1
                )
                if (dateLabel.isNotEmpty()) {
                    Text(
                        text = dateLabel,
                        color = Color(0xFFA8ABB3),
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
            if (showMore) {
                IconButton(onClick = onMoreClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert_black_24dp),
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ExplorerDirectoryRowLeading(iconRes: Int) {
    Image(
        painter = painterResource(iconRes),
        contentDescription = null,
        modifier = Modifier.size(40.dp)
    )
}

@Composable
fun FileChooseExplorerItem(item: FileChooseListView.ExplorerItemViewHolder) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.checked,
            onCheckedChange = { item.onCheckedChanged() }
        )
        Spacer(Modifier.width(8.dp))
        FileIcon(item.firstChar, item.firstCharBackground)
        Spacer(Modifier.width(12.dp))
        FileInfo(
            modifier = Modifier
                .weight(1f)
                .clickable { item.onCheckedChanged() },
            name = item.name,
            desc = item.desc
        )
    }
}

@Composable
private fun IconBox(modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(24.dp)
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun CategoryItem(holder: ExplorerViewKt.CategoryViewHolder, options: @Composable () -> Unit) {
    val tintColor = Color(0xFF828384)

    Row(
        modifier = Modifier.clickable { holder.collapseOrExpand() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        val state by animateFloatAsState(if (holder.arrowCollapsed) -90f else 0f)
        IconBox(
            modifier = Modifier.rotate(state),
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.ic_expanded),
                contentDescription = null,
                tint = tintColor
            )
        }

        Text(
            text = holder.title,
            modifier = Modifier.weight(1f),
            color = tintColor,
            fontSize = 12.sp
        )

        IconBox(Modifier.clickable { holder.goBack2() }) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(R.drawable.ic_dir_up),
                contentDescription = null,
                tint = tintColor
            )
        }
        IconBox(Modifier.clickable { holder.changeSortOrder() }) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(
                    if (holder.sortOrder) R.drawable.ic_ascending_order
                    else R.drawable.ic_descending_order
                ),
                contentDescription = null,
                tint = tintColor
            )
        }

        IconBox(Modifier.clickable { holder.showMenu = true }) {
            options()
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.ic_sort),
                contentDescription = null,
                tint = tintColor
            )
        }
    }
}


data class ExplorerItemConfig(
    val showRun: Boolean = false,
    val showEdit: Boolean = false,
    val showMore: Boolean = true
)

val LocalExplorerItemConfig = staticCompositionLocalOf { ExplorerItemConfig() }