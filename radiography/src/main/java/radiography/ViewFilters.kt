package radiography

import android.view.View
import radiography.ScannableView.AndroidView
import radiography.ScannableView.ComposeView
import radiography.internal.findTestTags

public object ViewFilters {

  /** A [ViewFilter] that matches everything (does not do any filtering). */
  @JvmField public val NoFilter: ViewFilter = ViewFilter { true }

  /** Filters out views with ids matching [skippedIds] from the output of [Radiography.scan]. */
  @JvmStatic
  public fun skipIdsViewFilter(vararg skippedIds: Int): ViewFilter {
    val sortedIds = skippedIds.sortedArray()
    return androidViewFilterFor<View> { view ->
      val viewId = view.id
      (viewId == View.NO_ID || sortedIds.isEmpty() || sortedIds.binarySearch(viewId) < 0)
    }
  }

  /**
   * Filters out composables with [`Modifier.testTag`][androidx.compose.ui.platform.testTag]
   * modifiers matching [skippedTestTags].
   *
   * Example:
   * ```
   * @Composable fun App() {
   *   ModalDrawerLayout(drawerContent = {
   *     DebugDrawer(Modifier.testTag("debug-drawer"))
   *   }) {
   *     Scaffold(…)
   *   }
   * }
   *
   * Radiography.scan(viewFilter = skipComposeTestTagsFilter("debug-drawer"))
   * ```
   *
   * To use test tags to limit the part of your UI that Radiography scans, use
   * [radiography.ScanScopes.composeTestTagScope].
   */
  @ExperimentalRadiographyComposeApi
  @JvmStatic
  public fun skipComposeTestTagsFilter(vararg skippedTestTags: String): ViewFilter =
    ViewFilter { view ->
      (view as? ComposeView)?.findTestTags()?.none { it in skippedTestTags } ?: true
    }

  /** Creates a new filter that combines this filter with [otherFilter] */
  @JvmStatic
  public infix fun ViewFilter.and(otherFilter: ViewFilter): ViewFilter {
    return ViewFilter { this.matches(it) && otherFilter.matches(it) }
  }

  /**
   * Returns a [ViewFilter] that matches any [AndroidView]s whose views are instances of [T] for
   * which [predicate] returns true.
   */
  // This function is only visible to Kotlin consumers of this library.
  public inline fun <reified T : Any> androidViewFilterFor(
    noinline predicate: (T) -> Boolean
  ): ViewFilter {
    // Don't create an anonymous instance here, since that would generate a new anonymous class at
    // every call site.
    return androidViewFilterFor(T::class.java, predicate)
  }

  /**
   * Returns a [ViewFilter] that matches any [AndroidView]s whose views are instances of [T] for
   * which [predicate] returns true.
   */
  // This function is only visible to Java consumers of this library.
  @JvmStatic
  @PublishedApi
  internal fun <T : Any> androidViewFilterFor(
    filterClass: Class<T>,
    predicate: (T) -> Boolean,
  ): ViewFilter = ViewFilter {
    val view = (it as? AndroidView)?.view ?: return@ViewFilter true
    if (!filterClass.isInstance(view)) return@ViewFilter true
    @Suppress("UNCHECKED_CAST") predicate(view as T)
  }
}
