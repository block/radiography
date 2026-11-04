package radiography.test.compose

import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Rule
import org.junit.Test
import radiography.ExperimentalRadiographyComposeApi
import radiography.ScannableView
import radiography.ScannableView.AndroidView
import radiography.ScannableView.ComposeView

@OptIn(ExperimentalRadiographyComposeApi::class)
class ComposeViewTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun androidView_children_includes_ComposeViews() {
    composeRule.setContentWithExplicitRoot { BasicText("hello") }

    val textComposable =
      findRootComposeView().allDescendentsDepthFirst.single { it.displayName == "BasicText" }
    assertThat(textComposable.children.asIterable()).isEmpty()
  }

  @Test
  fun composeView_children_includes_AndroidView() {
    composeRule.setContentWithExplicitRoot {
      Column { androidx.compose.ui.viewinterop.AndroidView(::TextView) }
    }

    val columnView =
      findRootComposeView().allDescendentsDepthFirst.single { it.displayName == "Column" }
    val androidViews = columnView.allDescendentsDepthFirst.filterIsInstance<AndroidView>().toList()
    assertThat(androidViews.count { it.view is TextView }).isEqualTo(1)
  }

  @Test
  fun composeView_reports_children() {
    composeRule.setContentWithExplicitRoot {
      Column {
        BasicText("hello")
        Button(onClick = {}) { Box(Modifier) }
      }
    }

    val columnView =
      findRootComposeView().allDescendentsDepthFirst.first { it.displayName == "Column" }
    val columnChildren = columnView.children.toList()
    assertThat(columnChildren).hasSize(2)
    assertThat(columnChildren[0].displayName).isEqualTo("BasicText")
    assertThat(columnChildren[1].displayName).isEqualTo("Button")
    assertThat(columnChildren[1].allDescendentsDepthFirst.count { it.displayName == "Box" })
      .isEqualTo(1)
  }

  @Test
  fun composeView_reports_LayoutModifiers() {
    composeRule.setContentWithExplicitRoot { Box(TestModifier) }

    val boxView =
      findRootComposeView().allDescendentsDepthFirst.single { it.displayName == "Box" }
        as ComposeView
    assertThat(boxView.modifiers).contains(TestModifier)
  }

  @Test
  fun composeView_reports_size() {
    var density: Density? = null
    composeRule.setContentWithExplicitRoot {
      density = LocalDensity.current
      Box(Modifier.size(30.dp, 40.dp))
    }

    val (widthPx, heightPx) =
      composeRule.runOnIdle { with(density!!) { Pair(30.dp.roundToPx(), 40.dp.roundToPx()) } }

    val boxView =
      findRootComposeView().allDescendentsDepthFirst.single { it.displayName == "Box" }
        as ComposeView
    assertThat(boxView.width).isEqualTo(widthPx)
    assertThat(boxView.height).isEqualTo(heightPx)
  }

  @Test
  fun composeView_inherits_name_of_top_most_call() {
    renderCallChainUI()

    val columnView =
      findRootComposeView().allDescendentsDepthFirst.first { it.displayName == "Column" }

    val columnChildren = columnView.children.toList()
    assertThat(columnChildren).hasSize(1)

    // Expect that the chain of custom Call functions is collapsed, so that the BasicText
    // is represented with the name "Call3" but contains the semantic information of the text
    // "hello".
    val callGroup = columnChildren[0] as ComposeView
    assertThat(callGroup.children.count()).isEqualTo(0)

    assertThat(callGroup.displayName).isEqualTo("Call1")

    assertThat(
        callGroup.semanticsConfigurations
          .firstNotNullOfOrNull { it[SemanticsProperties.Text] }
          ?.map { it.toString() }
      )
      .containsExactly("hello")
  }

  @OptIn(UiToolingDataApi::class)
  @Test
  fun composeView_reports_call_chain_info() {
    renderCallChainUI()

    val columnView =
      findRootComposeView().allDescendentsDepthFirst.first { it.displayName == "Column" }

    val callGroup = columnView.children.first() as ComposeView

    assertThat(callGroup.callChain).hasSize(6)

    assertThat(callGroup.callChain.map { it.name })
      .containsExactly("Call1", "Call2", "Call3", "BasicText", "Layout", "ReusableComposeNode")

    assertThat(callGroup.callChain.map { it.location?.sourceFile })
      .containsExactly(
        "ComposeViewTest.kt",
        "ComposeViewTest.kt",
        "ComposeViewTest.kt",
        "ComposeViewTest.kt",
        "BasicText.kt",
        "Layout.kt",
      )
  }

  private fun renderCallChainUI() {
    composeRule.setContentWithExplicitRoot {
      @Composable
      fun Call3() {
        BasicText(text = "hello")
      }

      @Composable
      fun Call2() {
        Call3()
      }

      @Composable
      fun Call1() {
        Call2()
      }

      Column { Call1() }
    }
  }

  private object TestModifier : LayoutModifier {
    override fun MeasureScope.measure(
      measurable: Measurable,
      constraints: Constraints,
    ): MeasureResult = layout(1, 2) {}
  }

  private fun findRootComposeView(): ComposeView =
    findRootAndroidView().allDescendentsDepthFirst.filterIsInstance<ComposeView>().first()

  private fun findRootAndroidView(): AndroidView {
    val latch = CountDownLatch(1)
    var androidView: AndroidView? = null
    composeRule.activityRule.scenario.onActivity {
      val contentView = it.findViewById<View>(android.R.id.content)
      androidView = AndroidView(contentView)
    }
    latch.await(1, TimeUnit.SECONDS)
    return androidView!!
  }

  private val ScannableView.allDescendentsDepthFirst: Sequence<ScannableView>
    get() = children.flatMap { sequenceOf(it) + it.allDescendentsDepthFirst }
}
