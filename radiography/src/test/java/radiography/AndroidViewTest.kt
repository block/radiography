package radiography

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import radiography.ScannableView.AndroidView

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
internal class AndroidViewTest {
  @Suppress("DEPRECATION") private val context: Context = RuntimeEnvironment.application

  @Test
  fun `children are reported`() {
    val child1 = View(context)
    val child2 = View(context)
    val view =
      FrameLayout(context).apply {
        addView(child1)
        addView(child2)
      }

    val children = AndroidView(view).children.toList()
    assertThat(children).hasSize(2)
    assertThat((children[0] as AndroidView).view).isSameInstanceAs(child1)
    assertThat((children[1] as AndroidView).view).isSameInstanceAs(child2)
  }

  @Test
  fun `view removed before hasNext is not reported1`() {
    val child1 = View(context)
    val child2 = View(context)
    val view =
      FrameLayout(context).apply {
        addView(child1)
        addView(child2)
      }

    val children = AndroidView(view).children
    val childIterator = children.iterator()
    assertThat(childIterator.hasNext()).isTrue()
    assertThat((childIterator.next() as AndroidView).view).isSameInstanceAs(child1)

    view.removeView(child2)

    assertThat(childIterator.hasNext()).isFalse()
  }

  @Test
  fun `view removed after hasNext is reported`() {
    val child1 = View(context)
    val child2 = View(context)
    val view =
      FrameLayout(context).apply {
        addView(child1)
        addView(child2)
      }

    val children = AndroidView(view).children
    val childIterator = children.iterator()
    assertThat(childIterator.hasNext()).isTrue()
    assertThat((childIterator.next() as AndroidView).view).isSameInstanceAs(child1)

    // Calling hasNext captures the view, so it should still be reported after we remove it.
    assertThat(childIterator.hasNext()).isTrue()

    view.removeView(child2)

    assertThat(childIterator.hasNext()).isTrue()
    assertThat((childIterator.next() as AndroidView).view).isSameInstanceAs(child2)
  }

  @Test
  fun `view added after hasNext is not reported`() {
    val child1 = View(context)
    val child2 = View(context)
    val view = FrameLayout(context).apply { addView(child1) }

    val children = AndroidView(view).children
    val childIterator = children.iterator()
    assertThat(childIterator.hasNext()).isTrue()
    assertThat((childIterator.next() as AndroidView).view).isSameInstanceAs(child1)

    view.addView(child2)

    assertThat(childIterator.hasNext()).isFalse()
  }
}
