package radiography

import android.annotation.SuppressLint
import android.content.res.Resources.NotFoundException
import android.view.View
import android.widget.Checkable
import android.widget.TextView
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.layout.LayoutIdParentData
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsProperties.EditableText
import androidx.compose.ui.semantics.SemanticsProperties.Text
import androidx.compose.ui.semantics.getOrNull
import radiography.ScannableView.AndroidView
import radiography.ScannableView.ComposeView
import radiography.internal.SemanticsPropertiesHelpers
import radiography.internal.ellipsize
import radiography.internal.formatPixelDimensions
import radiography.internal.isComposeAvailable

@OptIn(ExperimentalRadiographyComposeApi::class)
public object ViewStateRenderers {

  private val NoRenderer: ViewStateRenderer = ViewStateRenderer {
    // Noop.
  }

  private val AndroidViewRenderer: ViewStateRenderer =
    androidViewStateRendererFor<View> { view ->
      if (view.id != View.NO_ID && view.resources != null) {
        try {
          val resourceName = view.resources.getResourceEntryName(view.id)
          append("id:$resourceName")
        } catch (ignore: NotFoundException) {
          // Do nothing.
        }
      }

      @SuppressLint("SwitchIntDef")
      when (view.visibility) {
        View.GONE -> append("GONE")
        View.INVISIBLE -> append("INVISIBLE")
      }

      append(formatPixelDimensions(view.width, view.height))

      if (view.isFocused) {
        append("focused")
      }

      if (!view.isEnabled) {
        append("disabled")
      }

      if (view.isSelected) {
        append("selected")
      }
    }

  @OptIn(ExperimentalComposeUiApi::class)
  @ExperimentalRadiographyComposeApi
  private val ComposeViewRenderer: ViewStateRenderer =
    if (!isComposeAvailable) NoRenderer
    else {
      ViewStateRenderer { scannableView ->
        val composeView = scannableView as? ComposeView ?: return@ViewStateRenderer

        // Dimensions
        composeView.apply {
          if (width != 0 || height != 0) {
            append(formatPixelDimensions(width, height))
          }
        }

        // Semantics
        composeView.semanticsConfigurations
          // Technically there can be multiple semantic modifiers on a single node, so read them
          // all.
          .flatten()
          .sortedBy { it.key.name }
          .forEach { (key, value) ->
            @Suppress("DEPRECATION")
            when (key) {
              SemanticsProperties.TestTag -> appendLabeledValue("test-tag", value)
              SemanticsProperties.ContentDescription ->
                appendLabeledValue("content-description", (value as List<*>).map { "\"$it\"" })

              SemanticsProperties.StateDescription -> appendLabeledValue("state-description", value)
              SemanticsProperties.Disabled -> append("DISABLED")
              SemanticsProperties.Focused -> if (value == true) append("FOCUSED")
              SemanticsProperties.IsDialog -> append("DIALOG")
              SemanticsProperties.IsPopup -> append("POPUP")
              SemanticsProperties.ProgressBarRangeInfo ->
                appendLabeledValue("progress-bar-range", value)

              SemanticsProperties.PaneTitle -> appendLabeledValue("pane-title", value)
              SemanticsProperties.SelectableGroup -> append("SELECTABLE-GROUP")
              SemanticsProperties.Heading -> append("HEADING")
              SemanticsProperties.InvisibleToUser, // InvisibleToUser is deprecated.
              SemanticsPropertiesHelpers.HideFromAccessibility -> append("INVISIBLE-TO-USER")
              SemanticsProperties.HorizontalScrollAxisRange ->
                appendLabeledValue(
                  "horizontal-scroll-axis-range",
                  scrollAxisRangeToString(value as? ScrollAxisRange),
                )

              SemanticsProperties.VerticalScrollAxisRange ->
                appendLabeledValue(
                  "vertical-scroll-axis-range",
                  scrollAxisRangeToString(value as? ScrollAxisRange),
                )

              SemanticsProperties.Role -> appendLabeledValue("role", value)
              SemanticsProperties.TextSelectionRange -> append("SELECTED-TEXT")
              SemanticsProperties.ImeAction -> appendLabeledValue("ime-action", value)
              SemanticsProperties.Selected -> append("SELECTED")
              SemanticsProperties.ToggleableState -> appendLabeledValue("toggle-state", value)
              SemanticsProperties.Password -> append("PASSWORD")
            }
          }

        // Layout ID
        composeView.modifiers.filterIsInstance<LayoutIdParentData>().singleOrNull()?.let { layoutId
          ->
          val idValue =
            if (layoutId.layoutId is CharSequence) {
              "\"${layoutId.layoutId}\""
            } else {
              layoutId.layoutId.toString()
            }
          append("layout-id:$idValue")
        }
      }
    }

  @JvmField
  public val ViewRenderer: ViewStateRenderer = ViewStateRenderer { view ->
    with(AndroidViewRenderer) { render(view) }
    with(ComposeViewRenderer) { render(view) }
  }

  @JvmField
  public val CheckableRenderer: ViewStateRenderer =
    androidViewStateRendererFor<Checkable> { checkable ->
      if (checkable.isChecked) {
        append("checked")
      }
    }

  @JvmField
  public val DefaultsNoPii: List<ViewStateRenderer> =
    listOf(
      ViewRenderer,
      textViewRenderer(renderTextValue = false, textValueMaxLength = 0),
      CheckableRenderer,
    )

  @JvmField
  public val DefaultsIncludingPii: List<ViewStateRenderer> =
    listOf(ViewRenderer, textViewRenderer(renderTextValue = true), CheckableRenderer)

  /**
   * Renders information about [TextView]s and text composables.
   *
   * @param renderTextValue whether to include the string content of TextView instances in the
   *   rendered view hierarchy. Defaults to false to avoid including any PII.
   * @param textValueMaxLength the max size of the string content of TextView instances when
   *   [renderTextValue] is true. When the max size is reached, the text is trimmed to a
   *   [textValueMaxLength] - 1 length and ellipsized with a '…' character.
   */
  @JvmStatic
  @JvmOverloads
  public fun textViewRenderer(
    renderTextValue: Boolean = false,
    textValueMaxLength: Int = Int.MAX_VALUE,
  ): ViewStateRenderer {
    if (renderTextValue) {
      check(textValueMaxLength >= 0) {
        "textFieldMaxLength should be greater than 0, not $textValueMaxLength"
      }
    }

    val androidTextViewRenderer =
      androidViewStateRendererFor<TextView> { textView ->
        appendTextValue(label = "text", textView.text, renderTextValue, textValueMaxLength)
        if (textView.isInputMethodTarget) {
          append("ime-target")
        }
      }
    val composeTextRenderer = composeTextRenderer(renderTextValue, textValueMaxLength)

    return ViewStateRenderer { view ->
      with(androidTextViewRenderer) { render(view) }
      with(composeTextRenderer) { render(view) }
    }
  }

  /**
   * Renders composables that expose a text value through the [Text] semantics property.
   *
   * TODO update kdoc to mention editable vs text
   *
   * @param renderTextValue whether to include the string value of the property in the rendered view
   *   hierarchy. Defaults to false to avoid including any PII.
   * @param textValueMaxLength the max size of the string value of the property when
   *   [renderTextValue] is true. When the max size is reached, the text is trimmed to a
   *   [textValueMaxLength] - 1 length and ellipsized with a '…' character.
   */
  @ExperimentalRadiographyComposeApi
  @JvmStatic
  @JvmSynthetic
  internal fun composeTextRenderer(
    renderTextValue: Boolean = false,
    textValueMaxLength: Int = Int.MAX_VALUE,
  ): ViewStateRenderer =
    if (!isComposeAvailable) NoRenderer
    else
      ViewStateRenderer { view ->
        val semantics = (view as? ComposeView)?.semanticsConfigurations ?: emptyList()

        semantics
          .mapNotNull { it.getOrNull(Text)?.joinToString() }
          .takeUnless { it.isEmpty() }
          ?.joinToString(separator = " ")
          ?.also { appendTextValue(label = "text", it, renderTextValue, textValueMaxLength) }

        semantics
          .mapNotNull { it.getOrNull(EditableText)?.text }
          .takeUnless { it.isEmpty() }
          ?.joinToString(separator = " ")
          ?.also {
            appendTextValue(label = "editable-text", it, renderTextValue, textValueMaxLength)
          }
      }

  /** Creates a [ViewStateRenderer] that renders [AndroidView]s with views of type [T]. */
  // This function is only visible to Kotlin consumers of this library.
  public inline fun <reified T : Any> androidViewStateRendererFor(
    noinline renderer: AttributeAppendable.(T) -> Unit
  ): ViewStateRenderer {
    // Don't create an anonymous instance of ViewStateRenderer here, since that would generate a new
    // anonymous class at every call site.
    return androidViewStateRendererFor(T::class.java, renderer)
  }

  /** Creates a [ViewStateRenderer] that renders [AndroidView]s with views of type [T]. */
  // This function is only visible to Java consumers of this library.
  @JvmStatic
  @PublishedApi
  internal fun <T : Any> androidViewStateRendererFor(
    renderedClass: Class<T>,
    renderer: AttributeAppendable.(T) -> Unit,
  ): ViewStateRenderer = ViewStateRenderer { scannableView ->
    val view = (scannableView as? AndroidView)?.view ?: return@ViewStateRenderer
    if (!renderedClass.isInstance(view)) return@ViewStateRenderer
    @Suppress("UNCHECKED_CAST") renderer(view as T)
  }

  internal fun AttributeAppendable.appendTextValue(
    label: String,
    text: CharSequence?,
    renderTextValue: Boolean,
    textValueMaxLength: Int,
  ) {
    if (text == null) return

    val appendTextLength =
      if (renderTextValue) {
        val ellipsizedText = text.ellipsize(textValueMaxLength)
        appendLabeledValue(label, ellipsizedText)
        ellipsizedText.length != text.length
      } else {
        true
      }
    if (appendTextLength) {
      appendLabeledValue("$label-length", text.length)
    }
  }

  private fun AttributeAppendable.appendLabeledValue(label: String, value: Any?) {
    if (value is CharSequence) {
      append("""$label:"$value"""")
    } else {
      append("""$label:$value""")
    }
  }

  private fun scrollAxisRangeToString(range: ScrollAxisRange?) = range?.let {
    """ScrollAxisRange(value=${range.value()}, maxValue=${range.maxValue()})"""
  }
}
