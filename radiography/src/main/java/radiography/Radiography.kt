package radiography

import android.view.View
import android.view.WindowManager
import androidx.annotation.VisibleForTesting
import radiography.Radiography.scan
import radiography.ScanExecutors.HandlerPostingExecutor
import radiography.ScanExecutors.NeverThrowingExecutor
import radiography.ScanExecutors.mainHandler
import radiography.ScanScopes.AllWindowsScope
import radiography.ScannableView.AndroidView
import radiography.ViewStateRenderers.DefaultsNoPii
import radiography.internal.renderTreeString
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Utility class to scan through a view hierarchy and pretty print it to a [String].
 * Call [scan] or [View.scan].
 */
public object Radiography {

  /**
   * Scans the view hierarchies and pretty print them to a [String].
   *
   * You should generally call this method from the main thread, as views are meant to be accessed
   * from a single thread. If you call this from a background thread, this will schedule a message
   * to the main thread to retrieve the view hierarchy from there and will wait up to 5 seconds
   * or return an error message. This method will never throw, any thrown exception will have
   * its message included in the returned string.
   *
   * @param scanScope the [ScanScope] that determines what to scan. [AllWindowsScope] by default.
   *
   * @param viewStateRenderers render extra attributes for specifics types, in order.
   *
   * @param viewFilter a filter to exclude specific views from the rendering. If a view is excluded
   * then all of its children are excluded as well. Use [ViewFilters.skipIdsViewFilter] to ignore
   * views that match specific ids (e.g. a debug drawer). Use [ViewFilters.FocusedWindowViewFilter]
   * to keep only the views of the currently focused window, if any.
   */
  @JvmStatic
  @JvmOverloads
  public fun scan(
    scanScope: ScanScope = AllWindowsScope,
    viewStateRenderers: List<ViewStateRenderer> = DefaultsNoPii,
    viewFilter: ViewFilter = ViewFilters.NoFilter,
    scanExecutor: ScanExecutor = NeverThrowingExecutor(
      HandlerPostingExecutor(
        mainHandler,
        5,
        SECONDS
      )
    )
  ): String = scanExecutor.execute {
    buildString {
      val roots = scanScope.findRoots()
      roots.forEach { scanRoot ->
        scanRoot(scanRoot, viewStateRenderers, viewFilter)
      }
    }
  }

  private fun StringBuilder.scanRoot(
    rootView: ScannableView,
    viewStateRenderers: List<ViewStateRenderer>,
    viewFilter: ViewFilter
  ) {
    if (!viewFilter.matches(rootView)) return

    if (length > 0) {
      appendLine()
    }

    val androidView = (rootView as? AndroidView)?.view
    val layoutParams = androidView?.layoutParams
    val title = (layoutParams as? WindowManager.LayoutParams)?.title?.toString()
      ?: rootView.displayName
    appendLine("$title:")

    val startPosition = length
    try {
      androidView?.let {
        appendLine("window-focus:${it.hasWindowFocus()}")
      }
      renderScannableViewTree(this, rootView, viewStateRenderers, viewFilter)
    } catch (e: Throwable) {
      insert(
        startPosition,
        "Exception when going through view hierarchy: ${e.message}\n"
      )
    }
  }

  @VisibleForTesting
  @JvmSynthetic
  internal fun renderScannableViewTree(
    builder: StringBuilder,
    rootView: ScannableView,
    viewStateRenderers: List<ViewStateRenderer>,
    viewFilter: ViewFilter
  ) {
    renderTreeString(builder, rootView) {
      append(it.displayName)

      // Surround attributes in curly braces.
      // If no attributes get written (the length doesn't change), we'll remove the curly
      // brace. We append the opening brace before attempting to write, and then delete it later if
      // nothing was written, because most views have at least one attribute, so in most cases we
      // need the prefix anyway.
      append(" { ")
      val attributesStartIndex = length

      val appendable = AttributeAppendable(this)
      for (renderer in viewStateRenderers) {
        with(renderer) {
          appendable.render(it)
        }
      }

      if (length == attributesStartIndex) {
        // No attributes were written, remove the opening curly brace.
        delete(attributesStartIndex - 3, length)
      } else {
        // At least one attribute was written, so close the braces.
        append(" }")
      }

      return@renderTreeString it.children.filter(viewFilter::matches).toList()
    }
  }
}
