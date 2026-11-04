package radiography.test

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.view.Window.Callback
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicReference
import org.junit.Test
import radiography.Radiography
import radiography.ScanScopes.FocusedWindowScope
import radiography.ViewStateRenderers.DefaultsIncludingPii
import radiography.test.utilities.TestActivity
import radiography.test.utilities.TestActivity.Companion.withTextViewText

class RadiographyUiTest {

  @Test
  fun when_noActivity_then_emptyHierarchy() {
    val hierarchy = getOnMain { Radiography.scan() }

    assertThat(hierarchy).isEmpty()
  }

  @Test
  fun when_launchedActivity_then_hierarchyContainsActivity() {
    val scenario = ActivityScenario.launch<TestActivity>(TestActivity.intent)

    val hierarchy = scenario.runOnActivity { Radiography.scan() }

    assertThat(hierarchy).contains(TestActivity::class.java.name)
  }

  @Test
  fun when_includingPii_then_hierarchyContainsText() {
    val scenario = ActivityScenario.launch<TestActivity>(TestActivity.intent.withTextViewText("Yo"))

    val hierarchy = scenario.runOnActivity {
      Radiography.scan(viewStateRenderers = DefaultsIncludingPii)
    }
    assertThat(hierarchy).contains("Yo")
  }

  @Test
  fun when_showDialog_then_hierarchyHasTwoWindows() {
    val scenario = ActivityScenario.launch<TestActivity>(TestActivity.intent)
    val activity = scenario.activity

    activity.showDialog { AlertDialog.Builder(activity).create() }

    val hierarchy = getOnMain { Radiography.scan() }

    assertThat(hierarchy.countSubstring("window-focus")).isEqualTo(2)
  }

  @Test
  fun when_onlyFocusedWindow_then_hierarchyHasOnlyDialog() {
    val scenario = ActivityScenario.launch<TestActivity>(TestActivity.intent)
    val activity = scenario.activity

    activity.showDialog { AlertDialog.Builder(activity).setTitle("Dialog title").create() }

    val hierarchy = scenario.runOnActivity {
      Radiography.scan(scanScope = FocusedWindowScope, viewStateRenderers = DefaultsIncludingPii)
    }

    assertThat(hierarchy).contains("window-focus:true")
    assertThat(hierarchy).contains("Dialog title")

    assertThat(hierarchy.countSubstring("window-focus")).isEqualTo(1)
  }

  private val ActivityScenario<*>.activity: Activity
    get() = runOnActivity { it }

  private fun <A : Activity, R> ActivityScenario<A>.runOnActivity(block: (A) -> R): R {
    var result: R? = null
    onActivity { result = block(it) }
    @Suppress("UNCHECKED_CAST")
    return result as R
  }

  private fun String.countSubstring(substring: String) =
    windowed(substring.length).filter { it == substring }.count()

  private fun Activity.showDialog(block: () -> Dialog) {
    lateinit var dialog: Dialog
    getInstrumentation().runOnMainSync {
      dialog = block()
      dialog.show()
    }
    waitForFocus(dialog, this)
  }

  private fun <T> getOnMain(runOnMainSync: () -> T): T {
    val result = AtomicReference<T>()
    getInstrumentation().runOnMainSync { result.set(runOnMainSync()) }
    return result.get()
  }

  /** Waits for the activity to lose focus and the dialog to gain focus. */
  private fun waitForFocus(dialog: Dialog, activity: Activity) {
    val dialogFocused = CountDownLatch(2)
    getInstrumentation().runOnMainSync {
      val activityHasWindowFocus = activity.hasWindowFocus()
      val dialogHasWindowFocus = dialog.window!!.peekDecorView()?.hasWindowFocus() ?: false

      if (!activityHasWindowFocus) {
        dialogFocused.countDown()
      } else {
        val activityWindow = activity.window
        val delegateCallback = activityWindow.callback
        activityWindow.callback =
          object : Callback by delegateCallback {
            override fun onWindowFocusChanged(hasFocus: Boolean) {
              delegateCallback.onWindowFocusChanged(hasFocus)
              if (!hasFocus) {
                dialogFocused.countDown()
              }
            }
          }
      }

      if (dialogHasWindowFocus) {
        dialogFocused.countDown()
      } else {
        val delegateCallback = dialog.window!!.callback
        dialog.window!!.callback =
          object : Callback by delegateCallback {
            override fun onWindowFocusChanged(hasFocus: Boolean) {
              delegateCallback.onWindowFocusChanged(hasFocus)
              if (hasFocus) {
                dialogFocused.countDown()
              }
            }
          }
      }
    }
    assertThat(dialogFocused.await(10, SECONDS)).isTrue()
  }
}
