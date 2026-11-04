package radiography.internal

import android.annotation.SuppressLint
import android.util.SparseArray
import android.view.View
import androidx.compose.runtime.Composer
import androidx.compose.runtime.Composition
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.data.asTree
import java.lang.reflect.Field
import kotlin.LazyThreadSafetyMode.PUBLICATION
import radiography.ScannableView
import radiography.ScannableView.ChildRenderingError
import radiography.ScannableView.ComposeView
import radiography.toScannableView

private const val WRAPPED_COMPOSITION_CLASS_NAME = "androidx.compose.ui.platform.WrappedComposition"
private const val COMPOSITION_IMPL_CLASS_NAME = "androidx.compose.runtime.CompositionImpl"
private const val ANDROID_COMPOSE_VIEW_CLASS_NAME =
  "androidx.compose.ui.platform.AndroidComposeView"

internal const val COMPOSE_UNSUPPORTED_MESSAGE =
  "Composition was found, but either Compose Tooling artifact is missing or the Compose version " +
    "is not supported. Please ensure you have a dependency on androidx.ui:ui-tooling or check " +
    "https://github.com/square/radiography for a new release."

/** Reflectively tries to determine if Compose is on the classpath. */
internal val isComposeAvailable by
  lazy(PUBLICATION) {
    try {
      Class.forName(ANDROID_COMPOSE_VIEW_CLASS_NAME)
      true
    } catch (e: Throwable) {
      false
    }
  }

/**
 * True if this view looks like the private view type that Compose uses to host compositions. It
 * does a fuzzy match to try to detect unsupported Compose versions, which will not be rendered but
 * will at least warn that the version is unsupported.
 */
internal val View.mightBeComposeView: Boolean
  get() = "AndroidComposeView" in this::class.java.name

private val viewKeyedTagsField: Field? by
  lazy(PUBLICATION) {
    try {
      View::class.java.getDeclaredField("mKeyedTags").apply { isAccessible = true }
    } catch (e: NoSuchFieldException) {
      // Some devices don't have this field apparently.
      // See https://github.com/square/radiography/issues/119.
      null
    }
  }

/**
 * Tries to extract a list of [ComposeView]s from [composeView], which must be a view for which
 * [mightBeComposeView] is true. Returns the list of views and a boolean indicating whether the
 * extraction was successful.
 *
 * There is no public API for this, so this function uses reflection. If the reflection fails, or
 * the right Compose artifacts aren't on the classpath, or the runtime Compose version is
 * unsupported, this function will return a [ChildRenderingError] and false.
 */
@SuppressLint("VisibleForTests")
internal fun getComposeScannableViews(composeView: View): Pair<List<ScannableView>, Boolean> {
  var linkageError: LinkageError? = null

  val scannableViews =
    try {
      tryGetLayoutInfos(composeView)?.map(ComposeLayoutInfo::toScannableView)?.toList()
    } catch (e: LinkageError) {
      // The view looks like an AndroidComposeView, but the Compose code on the classpath is
      // not what we expected – the app is probably using a newer (or older) version of Compose than
      // we support.
      linkageError = e
      null
    }
  // If we were able to successfully construct the LayoutInfos, then we assume the Compose version
  // is supported.

  return scannableViews?.let { Pair(it, true) }
    // Display a warning but then continue rendering Android views, since the composition may emit
    // view children and so it's better than nothing.
    ?: Pair(listOf(composeRenderingError(linkageError)), false)
}

/**
 * Returns a [ChildRenderingError] that includes a message with instructions for updating
 * radiography and Compose versions.
 */
internal fun composeRenderingError(exception: LinkageError?): ScannableView {
  val message = buildString {
    append(COMPOSE_UNSUPPORTED_MESSAGE)
    exception?.let { appendLine().append("Error: $exception") }
  }
  return ChildRenderingError(message)
}

/**
 * Uses reflection to try to pull a `SlotTable` out of [composeView] and render it. If any of the
 * reflection fails, returns false.
 */
private fun tryGetLayoutInfos(composeView: View): Sequence<ComposeLayoutInfo>? {
  // Any of this reflection code can fail if running with an unsupported version of Compose.
  // Compose doesn't provide a public API for this (yet) because they don't want it to be used in
  // production.
  // See this slack thread:
  // https://kotlinlang.slack.com/archives/CJLTWPH7S/p1596749016388100?thread_ts=1596749016.388100&cid=CJLTWPH7S
  val keyedTags = composeView.getKeyedTags()
  val composition = keyedTags.first { it is Composition } as Composition? ?: return null
  val composer = composition.unwrap().getComposerOrNull() ?: return null

  // Composer and its slot table are finally public API again.
  // asTree is provided by the Compose Tooling library. It "reads" the slot table and parses it
  // into a tree of Group objects. This means we're technically traversing the composable tree
  // twice, so why not just read the slot table directly? As opaque as the Group API is, the actual
  // slot table API is quite complicated, and the actual format of the slot table (effectively an
  // array that stores a flattened version of a composition tree) is super low level. It's likely to
  // change a lot between compose versions, and keeping up with that with every two-week dev release
  // would be a lot of work. Additionally, a lot of the objects stored in the slot table are not
  // public (eg LayoutNode), so we'd need to use even more (brittle) reflection to do that parsing.
  // That said, once Compose is more stable, it might be worth it to read the slot table directly,
  // since then we could drop the requirement for the Tooling library to be on the classpath.
  @OptIn(UiToolingDataApi::class) val rootGroup = composer.compositionData.asTree()

  val semanticsOwner = (composeView as? RootForTest)?.semanticsOwner
  @OptIn(UiToolingDataApi::class)
  return rootGroup.computeLayoutInfos(semanticsOwner = semanticsOwner)
}

private fun View.getKeyedTags(): SparseArray<*> {
  return viewKeyedTagsField?.get(this) as SparseArray<*>? ?: SparseArray<Nothing>(0)
}

private inline fun SparseArray<*>.first(predicate: (Any?) -> Boolean): Any? {
  for (i in 0 until size()) {
    val item = valueAt(i)
    if (predicate(item)) return item
  }
  return null
}

/** If this is a `WrappedComposition`, returns the composition being wrapped, else returns this. */
private fun Composition.unwrap(): Composition {
  if (this::class.java.name != WRAPPED_COMPOSITION_CLASS_NAME) return this
  val wrappedClass = Class.forName(WRAPPED_COMPOSITION_CLASS_NAME)
  val originalField = wrappedClass.getDeclaredField("original").apply { isAccessible = true }
  return originalField.get(this) as Composition
}

/** Tries to pull a [Composer] out of this [Composition], or returns null if it can't find one. */
private fun Composition.getComposerOrNull(): Composer? {
  if (this::class.java.name != COMPOSITION_IMPL_CLASS_NAME) return null
  val compositionImplClass = Class.forName(COMPOSITION_IMPL_CLASS_NAME)
  val composerField =
    compositionImplClass.getDeclaredField("composer").apply { isAccessible = true }
  return composerField.get(this) as? Composer
}
