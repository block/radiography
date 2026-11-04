package radiography.internal

import java.util.BitSet

/**
 * Renders [rootNode] as a [String] by recursively rendering it and all its children as an ASCII-
 * art tree.
 *
 * @param renderNode A function which write the description of a node to a [StringBuilder], and
 *   returns the node's children.
 */
internal fun <N> renderTreeString(
  builder: StringBuilder,
  rootNode: N,
  renderNode: StringBuilder.(N) -> List<N>,
) {
  renderRecursively(builder, rootNode, renderNode, depth = 0, lastChildMask = BitSet())
}

private fun <N> renderRecursively(
  builder: StringBuilder,
  node: N,
  renderNode: StringBuilder.(N) -> List<N>,
  depth: Int,
  lastChildMask: BitSet,
) {
  // Render node into a separate buffer so we can append a prefix to every line.
  val nodeDescription = StringBuilder()
  val children = nodeDescription.renderNode(node)

  nodeDescription.lineSequence().forEachIndexed { index, line ->
    builder.appendLinePrefix(depth, continuePreviousLine = index > 0, lastChildMask = lastChildMask)
    builder.appendLine(line)
  }

  val lastChildIndex = children.size - 1
  children.forEachIndexed { index, childNode ->
    val isLastChild = (index == lastChildIndex)
    // Set bit before recursing, will be unset again before returning.
    if (isLastChild) {
      lastChildMask.set(depth)
    }

    childNode?.let { renderRecursively(builder, childNode, renderNode, depth + 1, lastChildMask) }
  }

  // Unset the bit we set above before returning.
  lastChildMask.clear(depth)
}

private fun StringBuilder.appendLinePrefix(
  depth: Int,
  continuePreviousLine: Boolean,
  lastChildMask: BitSet,
) {
  val lastDepth = depth - 1
  // Add a non-breaking space at the beginning of the line because Logcat eats normal spaces.
  append('\u00a0')
  for (parentDepth in 0..lastDepth) {
    if (parentDepth > 0) {
      append(' ')
    }
    val lastChild = lastChildMask[parentDepth]
    if (lastChild) {
      if (parentDepth == lastDepth && !continuePreviousLine) {
        append('╰')
      } else {
        append(' ')
      }
    } else {
      if (parentDepth == lastDepth && !continuePreviousLine) {
        append('├')
      } else {
        append('│')
      }
    }
  }
  if (depth > 0) {
    if (continuePreviousLine) {
      append(" ")
    } else {
      append("─")
    }
  }
}
