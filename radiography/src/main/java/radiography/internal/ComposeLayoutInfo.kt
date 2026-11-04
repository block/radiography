@file:OptIn(UiToolingDataApi::class)

package radiography.internal

import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.node.InteroperableComposeUiNode
import androidx.compose.ui.node.Ref
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.tooling.data.CallGroup
import androidx.compose.ui.tooling.data.Group
import androidx.compose.ui.tooling.data.NodeGroup
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.data.asTree
import androidx.compose.ui.unit.IntRect
import java.lang.reflect.Method
import java.util.IdentityHashMap
import radiography.ScannableView.CallGroupInfo
import radiography.internal.ComposeLayoutInfo.AndroidViewInfo
import radiography.internal.ComposeLayoutInfo.LayoutNodeInfo
import radiography.internal.ComposeLayoutInfo.SubcompositionInfo

/**
 * Information about a Compose `LayoutNode`, extracted from a [Group] tree via
 * [Group.computeLayoutInfos].
 *
 * This is a useful layer of indirection from directly handling Groups because it allows us to
 * define our own notion of what an atomic unit of "composable" is independently from how Compose
 * actually represents things under the hood. When this changes in some future dev version, we only
 * need to update the "parsing" logic in this file. It's also helpful since we actually gather data
 * from multiple Groups for a single LayoutInfo, so parsing them ahead of time into these objects
 * means the visitor can be stateless.
 */
internal sealed class ComposeLayoutInfo {
  data class LayoutNodeInfo(
    val name: String,
    val callChain: List<CallGroupInfo>,
    val bounds: IntRect,
    val modifiers: List<Modifier>,
    val children: Sequence<ComposeLayoutInfo>,
    val semanticsNodes: List<SemanticsNode>,
    // Node reference used to sort Subcompose Layouts deterministically across runs
    val layoutNode: Any? = null,
  ) : ComposeLayoutInfo()

  data class SubcompositionInfo(
    val name: String,
    val callChain: List<CallGroupInfo>,
    val bounds: IntRect,
    val children: Sequence<ComposeLayoutInfo>,
  ) : ComposeLayoutInfo()

  data class AndroidViewInfo(val view: View) : ComposeLayoutInfo()
}

/**
 * Recursively parses [ComposeLayoutInfo]s from a [Group]. Groups form a tree and can contain
 * different type of nodes which represent function calls, arbitrary data stored directly in the
 * slot table, or just subtrees.
 *
 * This function walks the tree and collects only Groups which represent emitted values
 * ([NodeGroup]s). These either represent `LayoutNode`s (Compose's internal primitive for layout
 * algorithms) or classic Android views that the composition emitted. This function collapses all
 * the groups in between each of these nodes, but uses the top-most Group under the previous node to
 * derive the "name" of the [ComposeLayoutInfo]. The other [ComposeLayoutInfo] properties come
 * directly off [NodeGroup] values.
 *
 * To preserve details about the Call Groups between Layout Nodes, the call chain is preserved in
 * order to provide granular detail about the hierarchy if desired.
 */
internal fun Group.computeLayoutInfos(
  parentCallChain: List<CallGroupInfo> = emptyList(),
  /**
   * The semantics owner for this Group. This is used to look up the semantics nodes for each layout
   * node.
   */
  semanticsOwner: SemanticsOwner? = null,
): Sequence<ComposeLayoutInfo> {
  val callChain =
    this.name?.let { parentCallChain + CallGroupInfo(it, this.location) } ?: parentCallChain

  // Things that we want to consider children of the current node, but aren't actually child nodes
  // as reported by Group.children.
  val irregularChildren = subComposedChildren(callChain, semanticsOwner) + androidViewChildren()

  // Certain composables produce an internal structure that is hard to read if we report it exactly.
  // Instead, we use heuristics to recognize subtrees that match certain expected structures and
  // aggregate them somewhat before reporting.
  tryParseSubcomposition(callChain, irregularChildren, semanticsOwner)?.let {
    return it
  }
  tryParseAndroidView(callChain, irregularChildren, semanticsOwner)?.let {
    return it
  }

  // This is an intermediate group that doesn't represent a LayoutNode, so we flatten by just
  // reporting its children without reporting a new subtree.
  if (this !is NodeGroup) {
    return children.asSequence().flatMap { it.computeLayoutInfos(callChain, semanticsOwner) } +
      irregularChildren
  }

  val children =
    children
      .asSequence()
      // This node will "consume" the name, so reset it name to empty for children.
      .flatMap { it.computeLayoutInfos(semanticsOwner = semanticsOwner) }

  val semanticsId = (this.node as? LayoutInfo)?.semanticsId
  val semanticsNodes =
    semanticsOwner?.getAllSemanticsNodes(mergingEnabled = false)?.filter { it.id == semanticsId }
      ?: emptyList()

  val layoutInfo =
    LayoutNodeInfo(
      name = callChain.firstOrNull()?.name.orEmpty(),
      callChain = callChain,
      bounds = box,
      modifiers = modifierInfo.map { it.modifier },
      semanticsNodes = semanticsNodes,
      children = children + irregularChildren,
      layoutNode = this.node,
    )
  return sequenceOf(layoutInfo)
}

/**
 * Look for any `CompositionContext`s stored in this group. These will be rolled up into the
 * `SubcomposeLayout` if present, otherwise they will just be shown as regular children. The
 * compositionData val is marked as internal, and not intended for public consumption. The returned
 * [SubcompositionInfo]s should be collated by [tryParseSubcomposition].
 */
private fun Group.subComposedChildren(
  callChain: List<CallGroupInfo>,
  semanticsOwner: SemanticsOwner?,
): Sequence<SubcompositionInfo> =
  getCompositionContexts()
    .flatMap { it.tryGetComposers().asSequence() }
    .map { subcomposer ->
      SubcompositionInfo(
        name = callChain.firstOrNull()?.name.orEmpty(),
        callChain = callChain,
        bounds = box,
        children =
          subcomposer.compositionData.asTree().computeLayoutInfos(semanticsOwner = semanticsOwner),
      )
    }

/**
 * The `AndroidView` composable remembers a [Ref] to a special internal subclass of [ViewGroup] that
 * manages the wiring between the hosting android view and the child view. This function looks for
 * refs to views and returns them as [AndroidViewInfo]s to be collated with [tryParseAndroidView].
 *
 * Note that [Ref] is a public type – any third-party composable could also remember a ref to a
 * view, and it would be reported by this function. That would almost certainly be a code smell for
 * a number of reasons though, so we don't try to ignore those cases.
 */
@OptIn(InternalComposeUiApi::class)
private fun Group.androidViewChildren(): List<AndroidViewInfo> {
  return data.mapNotNull { datum ->
    (datum as? InteroperableComposeUiNode)?.getInteropView()?.let(::AndroidViewInfo)
  }
}

/**
 * SubcomposeLayouts need to be handled specially, because all their subcompositions are always
 * logical children of their single LayoutNode. In order to render them so that the rendering
 * actually matches that logical structure, we need to reorganize the subtree a bit so
 * subcompositions are children of the layout node and not siblings of it.
 *
 * Note that there's no sure-fire way to actually detect a SubcomposeLayout. The best we can do is
 * use a heuristic. If any part of the heuristics don't match, then we fall back to treating the
 * group like any other.
 *
 * The heuristic we use is:
 * - Name of the group is "SubcomposeLayout".
 * - Has one or more subcompositions under it.
 * - Has exactly one LayoutNode child.
 * - That LayoutNode has no children of its own.
 */
private fun Group.tryParseSubcomposition(
  callChain: List<CallGroupInfo>,
  irregularChildren: Sequence<ComposeLayoutInfo>,
  semanticsOwner: SemanticsOwner?,
): Sequence<ComposeLayoutInfo>? {
  if (this.name != "SubcomposeLayout") return null

  val (subcompositions, regularChildren) =
    (children.asSequence().flatMap { it.computeLayoutInfos(callChain, semanticsOwner) } +
        irregularChildren)
      .partition { it is SubcompositionInfo }
      .let {
        // There's no type-safe partition operator so we just cast.
        @Suppress("UNCHECKED_CAST")
        it as Pair<List<SubcompositionInfo>, List<ComposeLayoutInfo>>
      }

  if (subcompositions.isEmpty()) return null
  if (regularChildren.size != 1) return null

  val mainNode = regularChildren.single()
  if (mainNode !is LayoutNodeInfo) return null
  if (!mainNode.children.isEmpty()) return null

  // We can be pretty confident at this point that this is an actual SubcomposeLayout, so
  // expose its layout node as the parent of all its subcompositions.
  // Sort subcompositions by the placement order of their children within the parent LayoutNode,
  // since the set backing composers may not preserve insertion order.
  val subcompositionName = "<subcomposition of ${mainNode.name}>"
  return sequenceOf(
    mainNode.copy(
      children =
        sortSubcompositionsByChildOrder(this, subcompositions).asSequence().map {
          it.copy(name = subcompositionName)
        }
    )
  )
}

/**
 * The AndroidView composable also needs to be special-cased. The actual android view is stored in a
 * Ref deep inside the hierarchy somewhere, but we want to expose it as the immediate child of
 * nearest common parent node that contains both the android view and the LayoutNode that is used as
 * a proxy to measure and lay it out in the composable.
 *
 * We can't rely on just the composable name, since any composable could be called "AndroidView", so
 * if any of the subtree parsing fails to match our expectations, we fallback to treating it like
 * any other group. Note that this heuristic isn't as strict as the subcomposition one, since
 * there's only one way to get an android view into a composition, so we can rely more heavily on
 * the presence of an actual android view. We still require there to be only one LayoutNode child,
 * otherwise it would be ambiguous which node we should report as the parent of the view. We also
 * require the common parent to be a CallGroup, since that is a valid assumption as of the time of
 * this writing and it saves us the additional logic of having to decide whether to return this or
 * the mainNode as the root of the subtree if this is a NodeGroup for some reason.
 *
 * Note that while this looks very similar to the [tryParseSubcomposition], that is probably mostly
 * coincidental, so it's probably not a good idea to factor out any abstractions. Since they both
 * rely on internal-only implementation details of how the Compose runtime happens to work, either
 * of them could change independently in the future, and it will be easier to update the logic of
 * both if that happens if they're completely independent.
 */
private fun Group.tryParseAndroidView(
  callChain: List<CallGroupInfo>,
  irregularChildren: Sequence<ComposeLayoutInfo>,
  semanticsOwner: SemanticsOwner?,
): Sequence<ComposeLayoutInfo>? {
  if (this.name != "AndroidView") return null
  if (this !is CallGroup) return null

  val (androidViews, regularChildren) =
    (children.asSequence().flatMap { it.computeLayoutInfos(callChain, semanticsOwner) } +
        irregularChildren)
      .partition { it is AndroidViewInfo }
      .let {
        // There's no type-safe partition operator so we just cast.
        @Suppress("UNCHECKED_CAST")
        it as Pair<List<AndroidViewInfo>, List<ComposeLayoutInfo>>
      }

  if (androidViews.isEmpty()) return null
  if (regularChildren.size != 1) return null

  val mainNode = regularChildren.single()
  if (mainNode !is LayoutNodeInfo) return null

  // We can be pretty confident at this point that this is an actual AndroidView composable,
  // so expose its layout node as the parent of its actual view.
  return sequenceOf(mainNode.copy(children = mainNode.children + androidViews))
}

/**
 * Sorts subcompositions by the placement order of their first child LayoutNode within the parent
 * LayoutNode. Uses object identity to match layout nodes, which is reliable regardless of whether
 * the nodes have semantics. Falls back to the original order if reflection fails.
 */
private fun sortSubcompositionsByChildOrder(
  parentGroup: Group,
  subcompositions: List<SubcompositionInfo>,
): List<SubcompositionInfo> {
  if (subcompositions.size <= 1) return subcompositions
  val childOrder = getChildLayoutNodeOrder(parentGroup) ?: return subcompositions
  val materialized = subcompositions.map { it.copy(children = it.children.toList().asSequence()) }

  return materialized.sortedBy { sub ->
    sub.children
      .filterIsInstance<LayoutNodeInfo>()
      .firstOrNull { it.layoutNode != null && it.layoutNode in childOrder }
      ?.let { childOrder[it.layoutNode] } ?: Int.MAX_VALUE
  }
}

private val GET_CHILDREN_METHOD: Method by lazy {
  Class.forName("androidx.compose.ui.node.LayoutNode").getMethod("getChildren\$ui")
}

@Suppress("UNCHECKED_CAST")
private fun getChildLayoutNodeOrder(parentGroup: Group): IdentityHashMap<Any, Int>? {
  val children = GET_CHILDREN_METHOD(checkNotNull(findNodeGroup(parentGroup)).node) as List<*>
  val order = IdentityHashMap<Any, Int>(children.size)
  children.forEachIndexed { index, child -> if (child != null) order[child] = index }
  return order
}

private fun findNodeGroup(group: Group): NodeGroup? {
  if (group is NodeGroup) return group
  for (child in group.children) {
    findNodeGroup(child)?.let {
      return it
    }
  }
  return null
}

private fun Sequence<*>.isEmpty(): Boolean = !iterator().hasNext()
