package radiography

import android.view.View
import curtains.Curtains
import radiography.ScannableView.AndroidView
import radiography.ScannableView.ComposeView
import radiography.internal.composeRenderingError
import radiography.internal.findTestTags
import radiography.internal.isComposeAvailable

public object ScanScopes {

  @JvmField internal val EmptyScope: ScanScope = ScanScope { emptyList() }

  /** Scans all the windows owned by the app. */
  @JvmField
  public val AllWindowsScope: ScanScope = ScanScope { Curtains.rootViews.map(::AndroidView) }

  /** Scans the window that currently has focus. */
  @JvmField
  public val FocusedWindowScope: ScanScope = ScanScope {
    AllWindowsScope.findRoots().filterIsInstance<AndroidView>().filter {
      it.view.parent?.parent != null || it.view.hasWindowFocus()
    }
  }

  /** Scans the given [rootView]. */
  @JvmStatic
  public fun singleViewScope(rootView: View): ScanScope = ScanScope {
    listOf(AndroidView(rootView))
  }

  /**
   * Limits the scope of the scan to start from composables that have a
   * [`Modifier.testTag`][androidx.compose.ui.platform.testTag] modifier with the given [testTag].
   * [All windows][AllWindowsScope] are searched by default, but you can limit where composables are
   * found by passing another [ScanScope] to [inScope].
   *
   * Example:
   * ```
   * @Composable fun App() {
   *   Column {
   *     ActionBar()
   *     Body(Modifier.testTag("app-body"))
   *     BottomBar()
   *   }
   * }
   *
   * Radiography.scan(scanScope = composeTestTagScope("app-body"))
   * ```
   *
   * To use test tags to filter out certain parts of your UI, use
   * [radiography.compose.ComposableFilters.skipTestTagsFilter].
   */
  @ExperimentalRadiographyComposeApi
  @JvmStatic
  @JvmOverloads
  public fun composeTestTagScope(testTag: String, inScope: ScanScope = AllWindowsScope): ScanScope {
    if (!isComposeAvailable) return EmptyScope

    fun ScannableView.subtreesMatching(
      predicate: (ScannableView) -> Boolean
    ): Sequence<ScannableView> {
      if (predicate(this)) return sequenceOf(this)
      return children.flatMap { it.subtreesMatching(predicate) }
    }

    return ScanScope {
      try {
        return@ScanScope inScope
          .findRoots()
          .asSequence()
          .flatMap { rootView ->
            rootView.subtreesMatching { (it is ComposeView) && testTag in it.findTestTags() }
          }
          .toList()
      } catch (e: LinkageError) {
        // Some version of Compose is available, but the Compose code on the classpath is
        // not what we expected – the app is probably using a newer (or older) version of Compose
        // than we support.
        return@ScanScope listOf(composeRenderingError(e))
      }
    }
  }
}
