package radiography.test.compose

import android.view.ViewGroup.LayoutParams
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role.Companion.RadioButton
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsProperties.ContentDescription
import androidx.compose.ui.semantics.SemanticsProperties.Disabled
import androidx.compose.ui.semantics.SemanticsProperties.Focused
import androidx.compose.ui.semantics.SemanticsProperties.Heading
import androidx.compose.ui.semantics.SemanticsProperties.HideFromAccessibility
import androidx.compose.ui.semantics.SemanticsProperties.HorizontalScrollAxisRange
import androidx.compose.ui.semantics.SemanticsProperties.ImeAction
import androidx.compose.ui.semantics.SemanticsProperties.InvisibleToUser
import androidx.compose.ui.semantics.SemanticsProperties.IsDialog
import androidx.compose.ui.semantics.SemanticsProperties.IsPopup
import androidx.compose.ui.semantics.SemanticsProperties.PaneTitle
import androidx.compose.ui.semantics.SemanticsProperties.Password
import androidx.compose.ui.semantics.SemanticsProperties.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties.Role
import androidx.compose.ui.semantics.SemanticsProperties.SelectableGroup
import androidx.compose.ui.semantics.SemanticsProperties.Selected
import androidx.compose.ui.semantics.SemanticsProperties.StateDescription
import androidx.compose.ui.semantics.SemanticsProperties.TestTag
import androidx.compose.ui.semantics.SemanticsProperties.TextSelectionRange
import androidx.compose.ui.semantics.SemanticsProperties.ToggleableState
import androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState.On
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction.Companion.Send
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import radiography.ExperimentalRadiographyComposeApi
import radiography.Radiography
import radiography.ScanScopes.composeTestTagScope
import radiography.ViewFilters.skipComposeTestTagsFilter
import radiography.ViewStateRenderers.DefaultsIncludingPii
import radiography.ViewStateRenderers.DefaultsNoPii
import radiography.ViewStateRenderers.ViewRenderer
import radiography.ViewStateRenderers.textViewRenderer

@Suppress("TestFunctionName")
@OptIn(ExperimentalRadiographyComposeApi::class)
class ComposeUiTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun when_includingPii_then_hierarchyContainsText() {
    composeRule.setContentWithExplicitRoot { BasicText("FooBar") }

    composeRule.runOnIdle {
      val hierarchy = Radiography.scan(viewStateRenderers = DefaultsIncludingPii)
      assertThat(hierarchy).contains("FooBar")
    }
  }

  @Test
  fun when_noPii_then_hierarchyExcludesText() {
    composeRule.setContentWithExplicitRoot { BasicText("FooBar") }

    composeRule.runOnIdle {
      val hierarchy = Radiography.scan(viewStateRenderers = DefaultsNoPii)
      assertThat(hierarchy).doesNotContain("FooBar")
      assertThat(hierarchy).contains("text-length:6")
    }
  }

  @Test
  fun viewSizeReported() {
    composeRule.setContentWithExplicitRoot {
      val (width, height) = with(LocalDensity.current) { Pair(30.toDp(), 40.toDp()) }
      Box(modifier = Modifier.size(width, height))
    }

    val hierarchy = composeRule.runOnIdle {
      Radiography.scan(viewStateRenderers = listOf(ViewRenderer))
    }

    assertThat(hierarchy).contains("Box { 30×40px }")
  }

  @Test
  fun zeroSizeViewReported() {
    composeRule.setContentWithExplicitRoot { Box(Modifier) }

    val hierarchy = composeRule.runOnIdle { Radiography.scan(viewStateRenderers = emptyList()) }

    assertThat(hierarchy).contains("Box")
  }

  @OptIn(ExperimentalComposeUiApi::class)
  @Test
  fun semanticsAreReported() {
    composeRule.setContentWithExplicitRoot {
      Box(Modifier.semantics { set(TestTag, "test tag") })
      Box(Modifier.semantics { set(ContentDescription, listOf("content description")) })
      Box(Modifier.semantics { set(StateDescription, "state description") })
      Box(Modifier.semantics { set(Disabled, Unit) })
      Box(Modifier.semantics { set(Focused, true) })
      Box(Modifier.semantics { set(Focused, false) })
      Box(Modifier.semantics { set(HideFromAccessibility, Unit) })
      Box(Modifier.semantics { set(IsDialog, Unit) })
      Box(Modifier.semantics { set(IsPopup, Unit) })
      Box(Modifier.semantics { set(ProgressBarRangeInfo, ProgressBarRangeInfo(.2f, 0f..0.5f)) })
      Box(Modifier.semantics { set(PaneTitle, "pane title") })
      Box(Modifier.semantics { set(SelectableGroup, Unit) })
      Box(Modifier.semantics { set(Heading, Unit) })
      Box(
        Modifier.semantics {
          set(HorizontalScrollAxisRange, ScrollAxisRange(value = { .3f }, maxValue = { .5f }))
        }
      )
      Box(
        Modifier.semantics {
          set(VerticalScrollAxisRange, ScrollAxisRange(value = { .4f }, maxValue = { .5f }))
        }
      )
      Box(Modifier.semantics { set(Role, RadioButton) })
      Box(Modifier.semantics { set(TextSelectionRange, TextRange(3, 4)) })
      Box(Modifier.semantics { set(ImeAction, Send) })
      Box(Modifier.semantics { set(Selected, true) })
      Box(Modifier.semantics { set(ToggleableState, On) })
      Box(Modifier.semantics { set(Password, Unit) })
    }

    val hierarchy = composeRule.runOnIdle { Radiography.scan() }

    assertThat(hierarchy).contains("Box { test-tag:\"test tag\" }")
    assertThat(hierarchy).contains("Box { content-description:[\"content description\"] }")
    assertThat(hierarchy).contains("Box { state-description:\"state description\" }")
    assertThat(hierarchy).contains("Box { DISABLED }")
    assertThat(hierarchy).contains("Box { FOCUSED }")
    assertThat(hierarchy).contains("Box")
    assertThat(hierarchy).contains("Box { DIALOG }")
    assertThat(hierarchy).contains("Box { POPUP }")
    assertThat(hierarchy)
      .contains(
        "Box { progress-bar-range:ProgressBarRangeInfo(current=0.2, range=0.0..0.5, steps=0) }"
      )
    assertThat(hierarchy).contains("Box { pane-title:\"pane title\" }")
    assertThat(hierarchy).contains("Box { SELECTABLE-GROUP }")
    assertThat(hierarchy).contains("Box { HEADING }")
    assertThat(hierarchy).contains("Box { INVISIBLE-TO-USER }")
    assertThat(hierarchy)
      .contains("Box { horizontal-scroll-axis-range:\"ScrollAxisRange(value=0.3, maxValue=0.5)\" }")
    assertThat(hierarchy)
      .contains("Box { vertical-scroll-axis-range:\"ScrollAxisRange(value=0.4, maxValue=0.5)\" }")
    assertThat(hierarchy).contains("Box { role:RadioButton }")
    assertThat(hierarchy).contains("Box { SELECTED-TEXT }")
    assertThat(hierarchy).contains("Box { ime-action:Send }")
    assertThat(hierarchy).contains("Box { SELECTED }")
    assertThat(hierarchy).contains("Box { toggle-state:On }")
    assertThat(hierarchy).contains("Box { PASSWORD }")
  }

  /**
   * Copy of [semanticsAreReported] but for deprecated properties that are reported with the same
   * tags as their replacements.
   */
  @Suppress("DEPRECATION")
  @OptIn(ExperimentalComposeUiApi::class)
  @Test
  fun deprecatedSemanticsAreReported() {
    composeRule.setContentWithExplicitRoot {
      Box(Modifier.semantics { set(InvisibleToUser, Unit) })
    }

    val hierarchy = composeRule.runOnIdle { Radiography.scan() }

    assertThat(hierarchy).contains("Box { INVISIBLE-TO-USER }")
  }

  @Test
  fun checkableChecked() {
    composeRule.setContentWithExplicitRoot { Checkbox(checked = true, onCheckedChange = {}) }

    val hierarchy = composeRule.runOnIdle {
      Radiography.scan(viewStateRenderers = listOf(ViewRenderer))
    }

    assertThat(hierarchy).contains("Checkbox")
    assertThat(hierarchy).contains("toggle-state:On")
  }

  @Test
  fun textEmptyContents() {
    composeRule.setContentWithExplicitRoot { BasicText("") }

    val hierarchy = composeRule.runOnIdle {
      Radiography.scan(viewStateRenderers = listOf(textViewRenderer(renderTextValue = true)))
    }

    assertThat(hierarchy).doesNotContain("text-length")
    assertThat(hierarchy).contains("text:\"\"")
  }

  @Test
  fun textContents() {
    composeRule.setContentWithExplicitRoot { BasicText("Baguette Avec Fromage") }

    val hierarchy = composeRule.runOnIdle {
      Radiography.scan(viewStateRenderers = listOf(textViewRenderer(renderTextValue = true)))
    }

    assertThat(hierarchy).doesNotContain("text-length")
    assertThat(hierarchy).contains("text:\"Baguette Avec Fromage\"")
  }

  @Test
  fun textContentsEllipsized() {
    composeRule.setContentWithExplicitRoot { BasicText("Baguette Avec Fromage") }

    val hierarchy = composeRule.runOnIdle {
      Radiography.scan(
        viewStateRenderers =
          listOf(textViewRenderer(renderTextValue = true, textValueMaxLength = 11))
      )
    }

    assertThat(hierarchy).contains("text-length:21")
    assertThat(hierarchy).contains("text:\"Baguette A…\"")
  }

  @Test
  fun textExcludedByDefault() {
    composeRule.setContentWithExplicitRoot { BasicText("Baguette Avec Fromage") }

    val hierarchy = composeRule.runOnIdle { Radiography.scan() }

    assertThat(hierarchy).contains("text-length:21")
    assertThat(hierarchy).doesNotContain("text:")
  }

  @Test
  fun textFieldEmptyContents() {
    composeRule.setContentWithExplicitRoot { TextField("", onValueChange = {}, label = {}) }

    val hierarchy = composeRule.runOnIdle {
      Radiography.scan(viewStateRenderers = listOf(textViewRenderer(renderTextValue = true)))
    }

    assertThat(hierarchy).contains("text:\"\"")
  }

  @Test
  fun textFieldContents() {
    composeRule.setContentWithExplicitRoot {
      TextField("Baguette Avec Fromage", onValueChange = {}, label = {})
    }

    val hierarchy = composeRule.runOnIdle {
      Radiography.scan(viewStateRenderers = listOf(textViewRenderer(renderTextValue = true)))
    }

    assertThat(hierarchy).contains("text:\"Baguette Avec Fromage\"")
  }

  @Test
  fun textFieldContentsEllipsized() {
    composeRule.setContentWithExplicitRoot {
      TextField("Baguette Avec Fromage", onValueChange = {}, label = {})
    }

    val hierarchy = composeRule.runOnIdle {
      Radiography.scan(
        viewStateRenderers =
          listOf(textViewRenderer(renderTextValue = true, textValueMaxLength = 11))
      )
    }

    assertThat(hierarchy).contains("text-length:21")
    assertThat(hierarchy).contains("text:\"Baguette A…\"")
  }

  @Test
  fun skipTestTags() {
    composeRule.setContentWithExplicitRoot {
      Box { Button(modifier = Modifier.testTag("42"), onClick = {}, content = {}) }
    }

    val hierarchy = composeRule.runOnIdle {
      Radiography.scan(viewFilter = skipComposeTestTagsFilter("42"))
    }

    assertThat(hierarchy).contains("Box")
    assertThat(hierarchy).doesNotContain("Button")
  }

  @Test
  fun nestedLayouts() {
    composeRule.setContentWithExplicitRoot {
      Box(Modifier.testTag("root")) {
        Box(Modifier)
        Column {
          Box(Modifier)
          Box(Modifier)
        }
        Row {
          Box(Modifier)
          Box(Modifier)
        }
      }
    }

    val hierarchy = composeRule.runOnIdle { Radiography.scan(composeTestTagScope("root")) }

    @Suppress("RemoveCurlyBracesFromTemplate")
    assertThat(hierarchy)
      .contains(
        """
        Box:
      $BLANK ${BLANK}Box { test-tag:"root" }
      $BLANK ${BLANK}├─Box
      $BLANK ${BLANK}├─Column
      $BLANK ${BLANK}│ ├─Box
      $BLANK ${BLANK}│ ╰─Box
      $BLANK ${BLANK}╰─Row
      $BLANK ${BLANK}  ├─Box
      $BLANK ${BLANK}  ╰─Box
      """
          .trimIndent()
      )
  }

  @Test
  fun nestedViewsInsideLayouts() {
    composeRule.setContentWithExplicitRoot {
      Box(Modifier.testTag("root")) {
        AndroidView(::TextView) { it.layoutParams = LayoutParams(0, 42) }
      }
    }

    val hierarchy = composeRule.runOnIdle { Radiography.scan(composeTestTagScope("root")) }

    // The stuff immediately below the AndroidView is implementation details, testing it is brittle
    // and pointless.
    @Suppress("RemoveCurlyBracesFromTemplate")
    assertThat(hierarchy)
      .contains(
        """
      ${BLANK} Box:
        ${BLANK}Box { 0×42px, test-tag:"root" }
      ${BLANK} ${BLANK}╰─AndroidView { 0×42px }
      """
          .trimIndent()
      )
    // But this view description should show up at some point.
    assertThat(hierarchy).contains("╰─TextView { 0×42px, text-length:0 }")
  }

  @Test
  fun scanningHandlesDialog() {
    composeRule.setContent {
      currentComposer.collectParameterInformation()

      Box(Modifier.testTag("parent")) {
        Dialog(onDismissRequest = {}) { Box(Modifier.testTag("child")) }
      }
    }

    val hierarchy = composeRule.runOnIdle { Radiography.scan(composeTestTagScope("parent")) }

    assertThat(hierarchy)
      .isEqualTo(
        """
      |Box:
      |${BLANK}Box { test-tag:"parent" }
      |${BLANK}╰─Dialog
      |${BLANK}  ╰─ProvideCompositionLocals { DIALOG }
      |${BLANK}    ╰─Box { test-tag:"child" }
      |
      """
          .trimMargin()
      )
  }

  @Test
  fun scanningHandlesWrappedDialog() {
    @Composable
    fun CustomTestDialog(children: @Composable () -> Unit) {
      Dialog(onDismissRequest = {}, content = children)
    }

    composeRule.setContent {
      currentComposer.collectParameterInformation()

      Box(Modifier.testTag("parent")) { CustomTestDialog { Box(Modifier.testTag("child")) } }
    }

    val hierarchy = composeRule.runOnIdle { Radiography.scan(composeTestTagScope("parent")) }

    assertThat(hierarchy)
      .isEqualTo(
        """
      |Box:
      |${BLANK}Box { test-tag:"parent" }
      |${BLANK}╰─CustomTestDialog
      |${BLANK}  ╰─ProvideCompositionLocals { DIALOG }
      |${BLANK}    ╰─Box { test-tag:"child" }
      |
      """
          .trimMargin()
      )
  }

  @Test
  fun scanningHandlesSingleSubcomposeLayout_withSingleChild() {
    composeRule.setContent {
      currentComposer.collectParameterInformation()

      Box(Modifier.testTag("parent")) {
        SingleSubcompositionLayout(Modifier.testTag("subcompose-layout")) {
          Box(Modifier.testTag("child"))
        }
      }
    }

    val hierarchy = composeRule.runOnIdle { Radiography.scan(composeTestTagScope("parent")) }

    assertThat(hierarchy)
      .isEqualTo(
        """
      |Box:
      |${BLANK}Box { test-tag:"parent" }
      |${BLANK}╰─SingleSubcompositionLayout { test-tag:"subcompose-layout" }
      |${BLANK}  ╰─<subcomposition of SingleSubcompositionLayout>
      |${BLANK}    ╰─Box { test-tag:"child" }
      |
      """
          .trimMargin()
      )
  }

  @Test
  fun scanningHandlesSingleSubcomposeLayout_withMultipleChildren() {
    composeRule.setContent {
      currentComposer.collectParameterInformation()

      Box(Modifier.testTag("parent")) {
        SingleSubcompositionLayout(Modifier.testTag("subcompose-layout")) {
          Box(Modifier.testTag("child1"))
          Box(Modifier.testTag("child2"))
        }
      }
    }

    val hierarchy = composeRule.runOnIdle { Radiography.scan(composeTestTagScope("parent")) }

    assertThat(hierarchy)
      .isEqualTo(
        """
      |Box:
      |${BLANK}Box { test-tag:"parent" }
      |${BLANK}╰─SingleSubcompositionLayout { test-tag:"subcompose-layout" }
      |${BLANK}  ╰─<subcomposition of SingleSubcompositionLayout>
      |${BLANK}    ├─Box { test-tag:"child1" }
      |${BLANK}    ╰─Box { test-tag:"child2" }
      |
      """
          .trimMargin()
      )
  }

  @Test
  fun scanningHandlesSingleSubcomposeLayout_withMultipleSubcompositionsAndChildren() {
    composeRule.setContent {
      currentComposer.collectParameterInformation()

      Box(Modifier.testTag("parent")) {
        MultipleSubcompositionLayout(
          Modifier.testTag("subcompose-layout"),
          firstChildren = {
            Box(Modifier.testTag("child1.1"))
            Box(Modifier.testTag("child1.2"))
          },
          secondChildren = {
            Box(Modifier.testTag("child2.1"))
            Box(Modifier.testTag("child2.2"))
          },
        )
      }
    }

    val hierarchy = composeRule.runOnIdle { Radiography.scan(composeTestTagScope("parent")) }

    assertThat(hierarchy)
      .isEqualTo(
        """
      |Box:
      |${BLANK}Box { test-tag:"parent" }
      |${BLANK}╰─MultipleSubcompositionLayout { test-tag:"subcompose-layout" }
      |${BLANK}  ├─<subcomposition of MultipleSubcompositionLayout>
      |${BLANK}  │ ├─Box { test-tag:"child1.1" }
      |${BLANK}  │ ╰─Box { test-tag:"child1.2" }
      |${BLANK}  ╰─<subcomposition of MultipleSubcompositionLayout>
      |${BLANK}    ├─Box { test-tag:"child2.1" }
      |${BLANK}    ╰─Box { test-tag:"child2.2" }
      |
      """
          .trimMargin()
      )
  }

  @Test
  fun scanningHandlesSiblingSubcomposeLayouts() {
    composeRule.setContent {
      currentComposer.collectParameterInformation()

      Box(Modifier.testTag("parent")) {
        SingleSubcompositionLayout(Modifier.testTag("subcompose-layout1")) {
          Box(Modifier.testTag("child1"))
        }
        SingleSubcompositionLayout(Modifier.testTag("subcompose-layout2")) {
          Box(Modifier.testTag("child2"))
        }
      }
    }

    val hierarchy = composeRule.runOnIdle { Radiography.scan(composeTestTagScope("parent")) }

    assertThat(hierarchy)
      .isEqualTo(
        """
      |Box:
      |${BLANK}Box { test-tag:"parent" }
      |${BLANK}├─SingleSubcompositionLayout { test-tag:"subcompose-layout1" }
      |${BLANK}│ ╰─<subcomposition of SingleSubcompositionLayout>
      |${BLANK}│   ╰─Box { test-tag:"child1" }
      |${BLANK}╰─SingleSubcompositionLayout { test-tag:"subcompose-layout2" }
      |${BLANK}  ╰─<subcomposition of SingleSubcompositionLayout>
      |${BLANK}    ╰─Box { test-tag:"child2" }
      |
      """
          .trimMargin()
      )
  }

  @Test
  fun scanningHandlesWithConstraints() {
    composeRule.setContent {
      currentComposer.collectParameterInformation()

      Box(Modifier.testTag("parent")) {
        BoxWithConstraints(Modifier.testTag("with-constraints")) { Box(Modifier.testTag("child")) }
      }
    }

    val hierarchy = composeRule.runOnIdle { Radiography.scan(composeTestTagScope("parent")) }

    assertThat(hierarchy)
      .isEqualTo(
        """
      |Box:
      |${BLANK}Box { test-tag:"parent" }
      |${BLANK}╰─BoxWithConstraints { test-tag:"with-constraints" }
      |${BLANK}  ╰─<subcomposition of BoxWithConstraints>
      |${BLANK}    ╰─Box { test-tag:"child" }
      |
      """
          .trimMargin()
      )
  }

  @Test
  fun scanningHandlesLazyLists() {
    composeRule.setContent {
      currentComposer.collectParameterInformation()

      Box(Modifier.testTag("parent")) {
        LazyColumn(Modifier.testTag("list")) {
          items(listOf(1, 2, 3)) {
            Box(Modifier.testTag("child:$it"))
            if (it % 2 == 0) {
              Box(Modifier.testTag("child:$it (even)"))
            }
          }
        }
      }
    }

    val hierarchy = composeRule.runOnIdle { Radiography.scan(composeTestTagScope("parent")) }

    assertThat(hierarchy)
      .isEqualTo(
        """
      |Box:
      |${BLANK}Box { test-tag:"parent" }
      |${BLANK}╰─LazyColumn { test-tag:"list", vertical-scroll-axis-range:"ScrollAxisRange(value=0.0, maxValue=0.0)" }
      |${BLANK}  ├─<subcomposition of LazyColumn>
      |${BLANK}  │ ╰─SkippableItem { test-tag:"child:1" }
      |${BLANK}  ├─<subcomposition of LazyColumn>
      |${BLANK}  │ ├─SkippableItem { test-tag:"child:2" }
      |${BLANK}  │ ╰─SkippableItem { test-tag:"child:2 (even)" }
      |${BLANK}  ╰─<subcomposition of LazyColumn>
      |${BLANK}    ╰─SkippableItem { test-tag:"child:3" }
      |
      """
          .trimMargin()
      )
  }

  @Test
  fun scanningSubcomposition_includesSize() {
    composeRule.setContent {
      currentComposer.collectParameterInformation()

      // Convert 10 px to DP, since output is always in px.
      val sizeDp = with(LocalDensity.current) { 10.toDp() }

      Box(Modifier.testTag("parent")) {
        MultipleSubcompositionLayout(
          Modifier.testTag("subcompose-layout"),
          firstChildren = {
            Box(Modifier.testTag("child1").size(sizeDp))
            Box(Modifier.testTag("child2").size(sizeDp))
          },
          secondChildren = { Box(Modifier.testTag("child3").size(sizeDp)) },
        )
      }
    }

    val hierarchy = composeRule.runOnIdle { Radiography.scan(composeTestTagScope("parent")) }

    assertThat(hierarchy)
      .isEqualTo(
        """
      |Box:
      |${BLANK}Box { 10×30px, test-tag:"parent" }
      |${BLANK}╰─MultipleSubcompositionLayout { 10×30px, test-tag:"subcompose-layout" }
      |${BLANK}  ├─<subcomposition of MultipleSubcompositionLayout>
      |${BLANK}  │ ├─Box { 10×10px, test-tag:"child1" }
      |${BLANK}  │ ╰─Box { 10×10px, test-tag:"child2" }
      |${BLANK}  ╰─<subcomposition of MultipleSubcompositionLayout>
      |${BLANK}    ╰─Box { 10×10px, test-tag:"child3" }
      |
      """
          .trimMargin()
      )
  }

  /**
   * Wrap the call to SubcomposeLayout, since real code almost never calls SubcomposeLayout directly
   * in line, so this more accurately represents a real use case.
   */
  @Composable
  private fun SingleSubcompositionLayout(modifier: Modifier, children: @Composable () -> Unit) {
    SubcomposeLayout(modifier) { constraints ->
      val placeables = subcompose(Unit, children).map { it.measure(constraints) }

      layout(0, 0) { placeables.forEach { it.placeRelative(0, 0) } }
    }
  }

  /**
   * Like [SingleSubcompositionLayout] but creates two subcompositions, and lays out all children
   * from both compositions in a column.
   */
  @Composable
  private fun MultipleSubcompositionLayout(
    modifier: Modifier,
    firstChildren: @Composable () -> Unit,
    secondChildren: @Composable () -> Unit,
  ) {
    SubcomposeLayout(modifier) { constraints ->
      val placeables =
        listOf(subcompose(0, firstChildren), subcompose(1, secondChildren)).flatten().map {
          it.measure(constraints)
        }

      layout(
        width = placeables.maxOfOrNull { it.width } ?: 0,
        height = placeables.sumOf { it.height },
      ) {
        placeables.fold(0) { y, placeable ->
          placeable.placeRelative(0, y)
          y + placeable.height
        }
      }
    }
  }

  /**
   * Like [SingleSubcompositionLayout] but creates one subcomposition per slot, in the order slots
   * are provided. Used to test that radiography reports subcompositions in subcompose-call order
   * even when the internal Set of composers does not preserve insertion order.
   */
  @Composable
  private fun MultiSlotSubcompositionLayout(
    modifier: Modifier,
    slots: List<@Composable () -> Unit>,
  ) {
    SubcomposeLayout(modifier) { constraints ->
      val allPlaceables =
        slots
          .mapIndexed { index, content ->
            subcompose(index, content).map { it.measure(constraints) }
          }
          .flatten()

      layout(0, 0) { allPlaceables.forEach { it.placeRelative(0, 0) } }
    }
  }

  @Test
  fun scanningHandlesMultipleSubcompositionsInDeterministicOrder() {
    composeRule.setContent {
      currentComposer.collectParameterInformation()

      Box(Modifier.testTag("parent")) {
        MultiSlotSubcompositionLayout(
          modifier = Modifier.testTag("subcompose-layout"),
          slots =
            listOf(
              { Box(Modifier.testTag("slot1")) },
              { Box(Modifier.testTag("slot2")) },
              { Box(Modifier.testTag("slot3")) },
              { Box(Modifier.testTag("slot4")) },
            ),
        )
      }
    }

    val hierarchy = composeRule.runOnIdle { Radiography.scan(composeTestTagScope("parent")) }

    @Suppress("RemoveCurlyBracesFromTemplate")
    assertThat(hierarchy)
      .isEqualTo(
        """
      |Box:
      |${BLANK}Box { test-tag:"parent" }
      |${BLANK}╰─MultiSlotSubcompositionLayout { test-tag:"subcompose-layout" }
      |${BLANK}  ├─<subcomposition of MultiSlotSubcompositionLayout>
      |${BLANK}  │ ╰─Box { test-tag:"slot1" }
      |${BLANK}  ├─<subcomposition of MultiSlotSubcompositionLayout>
      |${BLANK}  │ ╰─Box { test-tag:"slot2" }
      |${BLANK}  ├─<subcomposition of MultiSlotSubcompositionLayout>
      |${BLANK}  │ ╰─Box { test-tag:"slot3" }
      |${BLANK}  ╰─<subcomposition of MultiSlotSubcompositionLayout>
      |${BLANK}    ╰─Box { test-tag:"slot4" }
      |
      """
          .trimMargin()
      )
  }

  companion object {
    private const val BLANK = '\u00a0'
  }
}
