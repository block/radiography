package radiography.test.compose

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import radiography.Radiography
import radiography.ViewStateRenderers.DefaultsIncludingPii

class ComposeUnsupportedTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun when_composeVersionNotSupported_then_failsGracefully() {
    composeRule.setContent { BasicText("FooBar") }

    composeRule.runOnIdle {
      val hierarchy = Radiography.scan(viewStateRenderers = DefaultsIncludingPii)
      assertThat(hierarchy).doesNotContain("FooBar")
      assertThat(hierarchy)
        .contains(
          "Composition was found, but either Compose Tooling artifact is missing or the Compose " +
            "version is not supported. Please ensure you have a dependency on " +
            "androidx.ui:ui-tooling or check https://github.com/square/radiography for a new " +
            "release."
        )
    }
  }
}
