package radiography.internal

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey

/**
 * Helpers for accessing [SemanticsProperties] that are defined in newer versions of Compose from
 * builds linked to older versions of Compose that don't have those properties yet.
 */
internal object SemanticsPropertiesHelpers {
  private var _HideFromAccessibility: Any? = UNINITIALIZED
  val HideFromAccessibility: SemanticsPropertyKey<Unit>?
    get() {
      if (_HideFromAccessibility === UNINITIALIZED) {
        _HideFromAccessibility =
          try {
            SemanticsProperties.HideFromAccessibility
          } catch (_: NoSuchMethodError) {
            // Actual compose version is too old to have this property.
            null
          }
      }

      @Suppress("UNCHECKED_CAST")
      return _HideFromAccessibility as SemanticsPropertyKey<Unit>?
    }

  private object UNINITIALIZED
}
