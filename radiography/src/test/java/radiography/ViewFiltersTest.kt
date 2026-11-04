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
import radiography.ViewFilters.androidViewFilterFor

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
internal class ViewFiltersTest {
  @Suppress("DEPRECATION") private val context: Context = RuntimeEnvironment.application

  @Test
  fun `androidViewFilterFor ignores different view types`() {
    var filterRan = false
    val filter =
      androidViewFilterFor<TextView> {
        filterRan = true
        false
      }

    filter.matches(AndroidView(View(context)))

    assertThat(filterRan).isFalse()
  }

  @Test
  fun `androidViewFilterFor accepts view subtypes`() {
    var filterRan = false
    val filter =
      androidViewFilterFor<TextView> {
        filterRan = true
        false
      }

    filter.matches(AndroidView(EditText(context)))

    assertThat(filterRan).isTrue()
  }
}
