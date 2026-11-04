package radiography

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import radiography.ScannableView.AndroidView
import radiography.ViewFilters.and
import radiography.ViewFilters.skipIdsViewFilter
import radiography.ViewStateRenderers.textViewRenderer

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
internal class RadiographyTest {
  @Suppress("DEPRECATION") private val context: Context = RuntimeEnvironment.application

  @Test
  fun viewDetailsReported() {
    val view =
      View(context).apply {
        visibility = View.INVISIBLE
        right = 30
        bottom = 30
        isEnabled = false
        isSelected = true
      }
    view.scan().also {
      assertThat(it).contains("INVISIBLE")
      assertThat(it).contains("30×30px")
      assertThat(it).contains("disabled")
      assertThat(it).contains("selected")
    }
  }

  @Test
  fun nullView() {
    val view: View? = null
    assertThat(view.scan()).contains("null")
  }

  @Test
  fun checkableChecked() {
    val view = CheckBox(context)
    view.isChecked = true
    assertThat(view.scan()).contains("checked")
  }

  @Test
  fun textView() {
    val view = TextView(context)
    view.text = "Baguette"
    view.scan().also {
      assertThat(it).contains("text-length:8")
      assertThat(it).doesNotContain("text:")
    }
  }

  @Test
  fun textViewContents() {
    val view = TextView(context)
    view.text = "Baguette Avec Fromage"
    view.scan(viewStateRenderers = listOf(textViewRenderer(renderTextValue = true))).also {
      assertThat(it).doesNotContain("text-length")
      assertThat(it).contains("text:\"Baguette Avec Fromage\"")
    }
  }

  @Test
  fun textViewContentsEllipsized() {
    val view = TextView(context)
    view.text = "Baguette Avec Fromage"
    view
      .scan(
        viewStateRenderers =
          listOf(textViewRenderer(renderTextValue = true, textValueMaxLength = 11))
      )
      .also {
        assertThat(it).contains("text-length:21")
        assertThat(it).contains("text:\"Baguette A…\"")
      }
  }

  @Test
  fun recoversFromException() {
    val layout = FrameLayout(context)
    layout.addView(
      object : View(context) {
        override fun isEnabled(): Boolean {
          throw UnsupportedOperationException("Leave me alone")
        }
      }
    )
    layout.scan().also {
      assertThat(it).contains("FrameLayout")
      assertThat(it).contains("Leave me alone")
    }
  }

  @Test
  fun `skipIds with single input`() {
    val layout = FrameLayout(context)
    val view = Button(context).apply { id = 42 }
    layout.addView(view)
    layout.scan(viewFilter = skipIdsViewFilter(42)).also {
      assertThat(it).contains("FrameLayout")
      assertThat(it).doesNotContain("Button")
    }
  }

  @Test
  fun `skipIds with unsorted inputs`() {
    val layout = FrameLayout(context)
    val view = Button(context).apply { id = 42 }
    layout.addView(view)
    layout.scan(viewFilter = skipIdsViewFilter(42, 2, 6)).also {
      assertThat(it).contains("FrameLayout")
      assertThat(it).doesNotContain("Button")
    }
  }

  @Test
  fun combineFilters() {
    val layout = FrameLayout(context)
    layout.addView(CheckBox(context))
    layout.addView(Button(context).apply { id = 42 })
    layout.addView(EditText(context))

    val filter = skipIdsViewFilter(42) and ViewFilter { (it as? AndroidView)?.view !is EditText }
    layout.scan(viewFilter = filter).also {
      assertThat(it).contains("CheckBox")
      assertThat(it).doesNotContain("Button")
      assertThat(it).doesNotContain("TextView")
    }
  }

  @Test
  fun nestedViews() {
    val root = FrameLayout(context).apply { addView(View(context)) }
    val childLayout1 =
      LinearLayout(context).apply {
        addView(View(context))
        addView(View(context))
      }
    root.addView(childLayout1)
    val childLayout2 =
      LinearLayout(context).apply {
        addView(View(context))
        addView(View(context))
      }
    root.addView(childLayout2)
    assertThat(root.scan())
      .contains(
        """
          FrameLayout { 0×0px }
          $BLANK├─View { 0×0px }
          $BLANK├─LinearLayout { 0×0px }
          $BLANK│ ├─View { 0×0px }
          $BLANK│ ╰─View { 0×0px }
          $BLANK╰─LinearLayout { 0×0px }
          $BLANK  ├─View { 0×0px }
          $BLANK  ╰─View { 0×0px }
        """
          .trimIndent()
      )
  }

  @Test
  fun `renderScannableViewTree includes output from renderers`() {
    val builder = StringBuilder()
    val rootView = View(context)
    val scannableRoot = AndroidView(rootView)
    val renderers =
      listOf(ViewStateRenderer { append("render1") }, ViewStateRenderer { append("render2") })

    Radiography.renderScannableViewTree(builder, scannableRoot, renderers, ViewFilters.NoFilter)

    assertThat(builder.toString()).isEqualTo("${BLANK}View { render1, render2 }\n")
  }

  @Test
  fun `renderScannableViewTree doesn't render empty curly braces`() {
    val builder = StringBuilder()
    val rootView = View(context)
    val scannableRoot = AndroidView(rootView)

    Radiography.renderScannableViewTree(
      builder,
      scannableRoot,
      viewStateRenderers = emptyList(),
      ViewFilters.NoFilter,
    )

    assertThat(builder.toString()).isEqualTo("${BLANK}View\n")
  }

  companion object {
    private const val BLANK = '\u00a0'
  }
}
