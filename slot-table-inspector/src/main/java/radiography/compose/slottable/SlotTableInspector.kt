@file:OptIn(
  UiToolingDataApi::class,
  InternalComposeApi::class
)

package radiography.compose.slottable

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.data.Group
import androidx.compose.ui.tooling.data.ParameterInformation
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.data.asTree
import androidx.compose.ui.tooling.data.position
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Objects

/**
 * State shared between [SlotTableInspectable]s, which define the content whose slot table to
 * inspect, and [SlotTableInspector]s, which actually display the slot table contents.
 *
 * To update a [SlotTableInspector] with new data, for example after changing some state, call
 * [captureSlotTables]. If you do not call this method, [SlotTableInspector] will call it the first
 * time it's composed.
 *
 * E.g.:
 * ```
 * @Composable fun App() {
 *   val inspectorState = remember { SlotTableInspectorState() }
 *   var showInspector by remember { mutableStateOf(false) }
 *
 *   SlotTableInspectable(inspectorState) {
 *     Column {
 *       Button(onClick = {
 *         inspectorState.captureSlotTables()
 *         showInspector = true
 *       }) {
 *         Text("Inspect")
 *       }
 *     }
 *   }
 *
 *   if (showInspector) {
 *     Dialog {
 *       SlotTableInspector(inspectorState)
 *     }
 *   }
 * }
 * ```
 */
public class SlotTableInspectorState {

  internal val composers: MutableList<State<Composer?>> = mutableStateListOf()
  private var rootGroups: List<Group> by mutableStateOf(emptyList())

  internal val rootTreeItems: List<TreeItem> by derivedStateOf {
    rootGroups.map { it.toTreeItem() }
  }

  /**
   * Reads fresh slot table data from all [SlotTableInspector]s registered with this state.
   */
  public fun captureSlotTables() {
    rootGroups = composers.mapNotNull {
      it.value?.compositionData?.asTree()
    }
  }
}

/**
 * Defines some content to be inspected by a [SlotTableInspector]. The same
 * [SlotTableInspectorState] can be passed to multiple occurances of this function, and they will
 * each show as separate root groups in the [SlotTableInspector].
 */
@Composable public fun SlotTableInspectable(
  state: SlotTableInspectorState,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  val composer: MutableState<Composer?> = remember { mutableStateOf(null) }
  DisposableEffect(state) {

    state.composers += composer
    onDispose {
      state.composers -= composer
    }
  }

  // We don't need to actually do any layout. We introduce a subcomposition so that the content gets
  // its own Composer, and thus its own slot table, to scope the nodes shown in the inspector.
  SubcomposeLayout(modifier) { constraints ->
    val placeables = subcompose(Unit) {
      // Take the composer from the subcomposition.
      composer.value = currentComposer
      content()
    }.map { it.measure(constraints) }

    layout(
      width = placeables.maxOf { it.width },
      height = placeables.maxOf { it.height }
    ) {
      placeables.forEach { it.placeRelative(IntOffset.Zero) }
    }
  }
}

/**
 * Displays an interactive tree view of the slot table inside all the [SlotTableInspectable]s
 * to which [state] has been passed.
 */
@Composable public fun SlotTableInspector(
  state: SlotTableInspectorState,
  modifier: Modifier = Modifier
) {
  DisposableEffect(Unit) {
    // If we're called without any slot tables, we probably just need to perform the initial
    // capture.
    if (state.rootTreeItems.isEmpty()) {
      state.captureSlotTables()
    }
    onDispose {}
  }

  if (state.rootTreeItems.isEmpty()) {
    Text("No slot tables captured.", modifier.wrapContentSize())
  } else {
    TreeBrowser(
      items = state.rootTreeItems,
      modifier = modifier.fillMaxSize()
    )
  }
}

@OptIn(ExperimentalStdlibApi::class)
private fun Group.toTreeItem(): TreeItem {
  val id = Objects.hash(this.key, this.data, this.location, this.name).toString()
  return TreeItem(
    id = id,
    computeChildren = {
      val items = mutableListOf<TreeItem>()

      key?.let { key ->
        val locationString = buildAnnotatedString {
          withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append("Key: ")
          }
          withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
            append(key.toString())
          }
        }
        items += TreeItem("$id-key") {
          Text(
            locationString,
            fontSize = 12.sp,
            modifier = Modifier.alpha(.7f)
          )
        }
      }

      location?.let { location ->
        val locationString = buildAnnotatedString {
          withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append("Location: ")
          }
          withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
            if (location.sourceFile.isNullOrEmpty()) {
              append(location.toString())
            } else {
              append("${location.sourceFile}:${location.lineNumber}")
            }
          }
        }
        items += TreeItem("$id-location") {
          Text(
            locationString,
            fontSize = 12.sp,
            modifier = Modifier.alpha(.7f)
          )
        }
      }

      this.position?.let { position ->
        val locationString = buildAnnotatedString {
          withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append("Position: ")
          }
          withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
            append(position)
          }
        }
        items += TreeItem("$id-position") {
          Text(
            locationString,
            fontSize = 12.sp,
            modifier = Modifier.alpha(.7f)
          )
        }
      }

      this.parameters.takeUnless { it.isEmpty() }?.let { params ->
        items += TreeItem(
          id = "$id-parameters",
          computeChildren = {
            params.mapIndexed { index, param ->
              TreeItem("$id-parameters[$index]") {
                ParameterRow(param)
              }
            }
          }
        ) {
          Text("${parameters.size} Parameters")
        }
      }

      this.modifierInfo.takeUnless { it.isEmpty() }?.let { modifiers ->
        items += TreeItem(
          id = "$id-modifiers",
          computeChildren = {
            modifiers.mapIndexed { index, modifier ->
              TreeItem("$id-modifiers[$index]") {
                Text(modifier.toString(), fontFamily = FontFamily.Monospace)
              }
            }
          }
        ) {
          Text("${modifierInfo.size} Modifiers")
        }
      }

      this.data.takeUnless { it.isEmpty() }?.let { data ->
        items += TreeItem(
          id = "$id-data",
          computeChildren = {
            data.mapIndexed { index, datum ->
              TreeItem("$id-data[$index]") {
                Text(
                  datum.toString(),
                  fontFamily = FontFamily.Monospace,
                  fontSize = 12.sp
                )
              }
            }
          }
        ) {
          Text("${data.size} Data")
        }
      }

      children.takeUnless { it.isEmpty() }?.let { children ->
        items += TreeItem(
          id = "$id-groups",
          computeChildren = {
            buildList {
              children.mapTo(this) { it.toTreeItem() }
            }
          }
        ) {
          Text("${children.size} Groups")
        }
      }

      return@TreeItem items
    }
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = spacedBy(8.dp),
    ) {
      Text(
        this@toTreeItem.javaClass.simpleName,
        fontStyle = FontStyle.Italic,
        modifier = Modifier.alignByBaseline()
      )
      name?.let {
        Text(
          it,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.alignByBaseline()
        )
      }
      Text(
        "[${box.width}x${box.height}]",
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        modifier = Modifier
          .alpha(.7f)
          .alignByBaseline()
      )
    }
  }
}

@Composable private fun ParameterRow(param: ParameterInformation) {
  Row(horizontalArrangement = spacedBy(4.dp)) {
    Text(
      "${param.name}=${param.value}",
      fontFamily = FontFamily.Monospace,
      fontSize = 12.sp,
      modifier = Modifier.alignByBaseline()
    )

    @Composable fun Flag(text: String) {
      Text(
        text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Light,
        modifier = Modifier.alignByBaseline()
      )
    }

    if (param.fromDefault) {
      Flag("fromDefault")
    }
    if (param.compared) {
      Flag("compared")
    }
    if (param.stable) {
      Flag("stable")
    }
    if (param.static) {
      Flag("static")
    }
    param.inlineClass?.let {
      Flag("inlineClass=$it")
    }
  }
}
