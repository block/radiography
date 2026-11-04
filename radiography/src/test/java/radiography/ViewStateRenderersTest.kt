package radiography

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import radiography.ScannableView.AndroidView
import radiography.ViewStateRenderers.androidViewStateRendererFor
import radiography.ViewStateRenderers.appendTextValue

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
internal class ViewStateRenderersTest {
  @Suppress("DEPRECATION") private val context: Context = RuntimeEnvironment.application

  @Test
  fun `androidViewStateRendererFor ignores different view types`() {
    var rendered = false
    val renderer = androidViewStateRendererFor<TextView> { rendered = true }

    with(renderer) { AttributeAppendable(StringBuilder()).render(AndroidView(View(context))) }

    assertThat(rendered).isFalse()
  }

  @Test
  fun `androidViewStateRendererFor accepts view subtypes`() {
    var rendered = false
    val renderer = androidViewStateRendererFor<TextView> { rendered = true }

    with(renderer) { AttributeAppendable(StringBuilder()).render(AndroidView(EditText(context))) }

    assertThat(rendered).isTrue()
  }

  @Test
  fun `appendTextValue with text over max length`() {
    val text = "hello world"

    val result = buildString {
      AttributeAppendable(this)
        .appendTextValue(label = "text", text, renderTextValue = true, textValueMaxLength = 5)
    }

    assertThat(result).isEqualTo("""text:"hell…", text-length:11""")
  }

  @Test
  fun `appendTextValue with text exactly max length`() {
    val text = "hello world"

    val result = buildString {
      AttributeAppendable(this)
        .appendTextValue(label = "text", text, renderTextValue = true, textValueMaxLength = 11)
    }

    assertThat(result).isEqualTo("""text:"hello world"""")
  }

  @Test
  fun `appendTextValue with text under max length`() {
    val text = "hello world"

    val result = buildString {
      AttributeAppendable(this)
        .appendTextValue(label = "text", text, renderTextValue = true, textValueMaxLength = 100)
    }

    assertThat(result).isEqualTo("""text:"hello world"""")
  }

  @Test
  fun `appendTextValue with no text`() {
    val text = "hello world"

    val result = buildString {
      AttributeAppendable(this)
        .appendTextValue(label = "text", text, renderTextValue = false, textValueMaxLength = 0)
    }

    assertThat(result).isEqualTo("""text-length:11""")
  }
}
