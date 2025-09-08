package radiography.compose.slottable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * Represents a single row in a [TreeBrowser] that may have children.
 *
 * @param id An ID for the item that is unique across the entire tree.
 * @param computeChildren Function that returns the children for this item. Defaults to returning
 * an empty list. The function does not need to perform any caching itself, as long as any mutable
 * data it uses to derive the list of children is stored in snapshot state.
 * @param content The content of the row.
 */
internal class TreeItem(
  val id: String,
  private val computeChildren: () -> List<TreeItem> = ::emptyList,
  val content: @Composable RowScope.() -> Unit,
) {
  var isExpanded: Boolean by mutableStateOf(false)
  val hasChildren: Boolean by derivedStateOf { computeChildren().isNotEmpty() }
  val children: List<TreeItem> by derivedStateOf {
    if (isExpanded) computeChildren() else emptyList()
  }
}

/**
 * A vertical list of rows, where each row maybe have children and be expanded and collapsed.
 */
@Composable internal fun TreeBrowser(
  items: List<TreeItem>,
  modifier: Modifier = Modifier
) {
  val updatedItems by rememberUpdatedState(items)
  val flattenedTree by derivedStateOf {
    updatedItems.flatMap { it.flatten() }
  }

  BoxWithConstraints {
    val screenHeight = maxHeight

    Column(
      modifier
        .verticalScroll(rememberScrollState())
        .horizontalScroll(rememberScrollState())
        .width(IntrinsicSize.Max)
    ) {
      flattenedTree.forEach { item ->
        key(item.item.id + "-" + item.nestingLevel) {
          TreeRow(item)
        }
      }

      // Add some space at the end to allow scrolling the last item up a little bit.
      Spacer(Modifier.height(screenHeight / 2))
    }
  }
}

@Preview
@Composable
private fun TreeBrowserPreview() {
  fun TreeItem(text: String, vararg children: TreeItem) = TreeItem(
    id = text,
    computeChildren = { children.asList() },
    content = { Text(text) }
  )

  val tree = remember {
    listOf(
      TreeItem(
        "root1",
        TreeItem("child 1"),
        TreeItem(
          "child 2",
          TreeItem(
            "foo really long name that hopefully should wrap at least in portrait mode on a phone",
            TreeItem("bar")
          ),
          TreeItem(" baz"),
        ),
      ),
      TreeItem("root2")
    )
  }

  TreeBrowser(tree)
}

@Composable private fun TreeRow(item: FlattenedTreeItem) {
  val toggleSize = 36.dp
  val toggleableModifier = if (item.item.hasChildren) {
    Modifier.toggleable(
      value = item.item.isExpanded,
      onValueChange = { item.item.isExpanded = it }
    )
  } else Modifier

  Row(
    Modifier
      .animateHeightFromZero()
      .fillMaxWidth()
      .then(toggleableModifier)
      .padding(
        start = toggleSize * item.nestingLevel,
        top = 4.dp,
        bottom = 4.dp
      ),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (item.item.hasChildren) {
      val isExpanded = item.item.isExpanded
      val iconAngle by animateFloatAsState(
        targetValue = if (isExpanded) 0f else -90f
      )
      Icon(
        Icons.Default.ArrowDropDown,
        contentDescription = if (isExpanded) "Expand" else "Collapse",
        modifier = Modifier
          .size(toggleSize)
          .wrapContentSize()
          .rotate(iconAngle)
      )
    } else {
      Spacer(Modifier.width(toggleSize))
    }
    item.item.content(this)
  }
}

/**
 * Returns a list of [FlattenedTreeItem] that is the depth-first traversal of the nodes starting
 * at this [TreeItem].
 */
private fun TreeItem.flatten(nestingLevel: Int = 0): Sequence<FlattenedTreeItem> {
  val root = FlattenedTreeItem(this, nestingLevel)
  val children = children.asSequence().flatMap {
    it.flatten(nestingLevel = nestingLevel + 1)
  }
  return sequenceOf(root) + children
}

private fun Modifier.animateHeightFromZero(): Modifier = composed {
  val yScale = remember { Animatable(0f) }
  LaunchedEffect(Unit) {
    yScale.animateTo(1f)
  }
  Modifier
    .graphicsLayer {
      scaleY = yScale.value
    }
    .layout { measurable, constraints ->
      val placeable = measurable.measure(constraints)
      layout(placeable.width, (placeable.height * yScale.value).toInt()) {
        placeable.placeRelative(IntOffset.Zero)
      }
    }
}

private data class FlattenedTreeItem(
  val item: TreeItem,
  val nestingLevel: Int,
)
