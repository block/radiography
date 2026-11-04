package radiography.internal

import androidx.compose.ui.semantics.SemanticsProperties
import radiography.ExperimentalRadiographyComposeApi
import radiography.ScannableView.ComposeView

/** Returns all tag strings set on the composable via `Modifier.testTag`. */
@OptIn(ExperimentalRadiographyComposeApi::class)
internal fun ComposeView.findTestTags(): Sequence<String> {
  return semanticsConfigurations
    .asSequence()
    .flatMap { semantics -> semantics.filter { it.key == SemanticsProperties.TestTag } }
    .mapNotNull { it.value as? String }
}
