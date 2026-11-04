package radiography.internal

import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.tooling.data.Group
import androidx.compose.ui.tooling.data.UiToolingDataApi
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.LazyThreadSafetyMode.PUBLICATION

private class ComposerVariantConstants(
  val contextHolderClass: Class<*>,
  val contextImplClass: Class<*>,
  val refField: Field,
  val composersField: Field,
  val composersToIterable: (Any) -> Iterable<*>,
)

private fun tryLoadVariant(composerClassName: String): ComposerVariantConstants? {
  return try {
    val contextHolderClass =
      Class.forName("$composerClassName\$CompositionContextHolder")
    val contextImplClass =
      Class.forName("$composerClassName\$CompositionContextImpl")
    val refField = contextHolderClass.getDeclaredField("ref")
      .apply { isAccessible = true }
    val composersField = contextImplClass.getDeclaredField("composers")
      .apply { isAccessible = true }

    val composersToIterable: (Any) -> Iterable<*> = run {
      val fieldType = composersField.type
      when {
        Iterable::class.java.isAssignableFrom(fieldType) -> { obj -> obj as Iterable<*> }
        else -> {
          val asSetMethod: Method? = try {
            fieldType.getMethod("asSet")
          } catch (_: NoSuchMethodException) {
            try {
              fieldType.getMethod("asMutableSet")
            } catch (_: NoSuchMethodException) {
              null
            }
          }
          if (asSetMethod != null) {
            { obj -> asSetMethod(obj) as Iterable<*> }
          } else {
            return null
          }
        }
      }
    }

    ComposerVariantConstants(
      contextHolderClass, contextImplClass, refField, composersField, composersToIterable
    )
  } catch (_: Throwable) {
    null
  }
}

private val COMPOSER_VARIANTS: List<ComposerVariantConstants> by lazy(PUBLICATION) {
  listOf(
    "androidx.compose.runtime.GapComposer",
    "androidx.compose.runtime.LinkComposer",
    "androidx.compose.runtime.ComposerImpl",
  ).mapNotNull { tryLoadVariant(it) }
}

private val REMEMBER_OBSERVER_HOLDER_CLASS: Class<*> by lazy(PUBLICATION) {
  Class.forName("androidx.compose.runtime.RememberObserverHolder")
}

private val GET_WRAPPED_METHOD: Method by lazy(PUBLICATION) {
  REMEMBER_OBSERVER_HOLDER_CLASS.getMethod("getWrapped")
}

@OptIn(UiToolingDataApi::class)
internal fun Group.getCompositionContexts(): Sequence<CompositionContext> {
  return if (COMPOSER_VARIANTS.isEmpty()) {
    emptySequence()
  } else {
    data.asSequence()
      .mapNotNull { datum ->
        if (datum == null) return@mapNotNull null
        // Try direct match (Compose <= 1.10: data contains CompositionContextHolder directly)
        for (variant in COMPOSER_VARIANTS) {
          if (variant.contextHolderClass.isInstance(datum)) {
            return@mapNotNull variant.refField.get(datum) as? CompositionContext
          }
        }
        // Compose >= 1.11: data contains RememberObserverHolder wrapping CompositionContextHolder
        if (!REMEMBER_OBSERVER_HOLDER_CLASS.isInstance(datum)) return@mapNotNull null
        val wrapped = GET_WRAPPED_METHOD(datum) ?: return@mapNotNull null
        for (variant in COMPOSER_VARIANTS) {
          if (variant.contextHolderClass.isInstance(wrapped)) {
            return@mapNotNull variant.refField.get(wrapped) as? CompositionContext
          }
        }
        null
      }
  }
}

@Suppress("UNCHECKED_CAST")
internal fun CompositionContext.tryGetComposers(): Iterable<Composer> {
  for (variant in COMPOSER_VARIANTS) {
    if (variant.contextImplClass.isInstance(this)) {
      val composersObj = variant.composersField.get(this) ?: return emptyList()
      return variant.composersToIterable(composersObj) as Iterable<Composer>
    }
  }
  return emptyList()
}
