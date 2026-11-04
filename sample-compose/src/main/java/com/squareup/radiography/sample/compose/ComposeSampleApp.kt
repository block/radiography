package com.squareup.radiography.sample.compose

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog.Builder
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.Recomposer.State.Idle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import radiography.ExperimentalRadiographyComposeApi
import radiography.Radiography
import radiography.ScanScopes.FocusedWindowScope
import radiography.ScannableView
import radiography.ViewFilters.skipComposeTestTagsFilter
import radiography.ViewStateRenderers.CheckableRenderer
import radiography.ViewStateRenderers.DefaultsIncludingPii
import radiography.ViewStateRenderers.DefaultsNoPii
import radiography.ViewStateRenderers.ViewRenderer
import radiography.ViewStateRenderers.androidViewStateRendererFor
import radiography.ViewStateRenderers.textViewRenderer

internal const val TEXT_FIELD_TEST_TAG = "text-field"
internal const val LIVE_HIERARCHY_TEST_TAG = "live-hierarchy"

@Preview(showBackground = true, showSystemUi = true)
@Composable fun ComposeSampleAppPreview() {
  ComposeSampleApp()
}

@Composable fun ComposeSampleApp() {
  val context = LocalContext.current
  val liveHierarchy = remember { mutableStateOf<String?>(null) }

  var username by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var rememberMe by remember { mutableStateOf(false) }

  MaterialTheme {
    Column(
      modifier = Modifier.padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      RadiographyLogo(Modifier.height(128.dp))

      TextField(
        value = username,
        onValueChange = { username = it },
        label = { Text("Username") },
        colors = TextFieldDefaults.outlinedTextFieldColors(),
        modifier = Modifier.testTag(TEXT_FIELD_TEST_TAG)
      )
      TextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        colors = TextFieldDefaults.outlinedTextFieldColors(),
        visualTransformation = PasswordVisualTransformation()
      )
      Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
        Spacer(Modifier.width(8.dp))
        Text("Remember me")
      }

      Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = {}) {
          Text("SIGN IN")
        }
        TextButton(onClick = {}) {
          Text("FORGOT PASSWORD")
        }
      }

      // Include a classic Android view in the composition.
      AndroidView(::TextView) {
        @SuppressLint("SetTextI18n")
        it.text = "By signing in, you agree to our Terms and Conditions."
      }

      liveHierarchy.value?.let {
        Row(
          modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .weight(1f)
        ) {
          Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(
              liveHierarchy.value.orEmpty(),
              fontFamily = FontFamily.Monospace,
              fontSize = 6.sp,
              lineHeight = 1.2.em,
              modifier = Modifier.testTag(LIVE_HIERARCHY_TEST_TAG)
            )
          }
        }
      }
      TextButton(onClick = { showSelectionDialog(context) }) {
        Text("SHOW STRING RENDERING DIALOG")
      }

      LaunchedEffect(Unit) {
        runLiveScanning { liveHierarchy.value = it }
      }
    }
  }
}

@OptIn(ExperimentalRadiographyComposeApi::class)
private fun showSelectionDialog(context: Context) {
  val renderings = listOf(
    "Default" to {
      Radiography.scan()
    },
    "Focused window" to {
      Radiography.scan(scanScope = FocusedWindowScope)
    },
    "Skip testTag(\"$TEXT_FIELD_TEST_TAG\")" to {
      Radiography.scan(viewFilter = skipComposeTestTagsFilter(TEXT_FIELD_TEST_TAG))
    },
    "Focused window and custom filter" to {
      Radiography.scan(
        scanScope = FocusedWindowScope,
        viewFilter = { view ->
          (view as? ScannableView.AndroidView)?.view !is LinearLayout }
      )
    },
    "Include PII" to {
      Radiography.scan(viewStateRenderers = DefaultsIncludingPii)
    },
    "Include PII ellipsized" to {
      Radiography.scan(
        viewStateRenderers = listOf(
          ViewRenderer,
          textViewRenderer(renderTextValue = true, textValueMaxLength = 4),
          CheckableRenderer,
        )
      )
    },
    "Custom LinearLayout renderer" to {
      Radiography.scan(
        viewStateRenderers = DefaultsNoPii + androidViewStateRendererFor<LinearLayout> {
          append(if (it.orientation == LinearLayout.HORIZONTAL) "horizontal" else "vertical")
        })
    }
  )

  val items = renderings.map { it.first }
    .toTypedArray()
  Builder(context)
    .setTitle("Choose rendering")
    .setItems(items) { _, index ->
      val rendering = renderings[index].second()
      // Print each line as a separate logcat entry so the total output doesn't get truncated.
      rendering.lineSequence().forEach {
        Log.d("MainActivity", it)
      }
      showResult(context, rendering)
    }
    .show()
}

private fun showResult(
  context: Context,
  rendering: String
) {
  val renderingDialog = Builder(context)
    .setTitle("Rendering (also printed to Logcat)")
    .setMessage(rendering)
    .setPositiveButton("Ok") { _, _ ->
      showSelectionDialog(context)
    }
    .show()
  val messageView = renderingDialog.findViewById<TextView>(android.R.id.message)!!
  messageView.textSize = 9f
  messageView.typeface = Typeface.MONOSPACE
}

@OptIn(
  ExperimentalCoroutinesApi::class,
  FlowPreview::class,
  ExperimentalRadiographyComposeApi::class
)
private suspend fun runLiveScanning(onScanResult: (String) -> Unit) {
  // A Flow that emits false when all Recomposers are idle, and true when at least one Recomposer
  // is non-idle.
  val isComposing: Flow<Boolean> = Recomposer.runningRecomposers
    .flatMapLatest { recomposers ->
      combine(recomposers.map { it.state }) { states ->
        states.any { it != Idle }
      }
    }
    .distinctUntilChanged()

  fun emitEvery(periodMs: Long): Flow<Unit> = flow {
    while (true) {
      emit(Unit)
      delay(periodMs)
    }
  }

  @Suppress("NAME_SHADOWING")
  val scanTrigger = isComposing.flatMapLatest { isComposing ->
    if (isComposing) {
      // While any Recomposer's state is not idle, do a scan every 500 millis. This effectively
      // throttles scans during continuously-recomposing periods like animations.
      emitEvery(500)
    } else {
      // Emit once to scan the now-settled composition, then stop.
      flowOf(Unit)
    }
  }

  // If a scan takes too long and other scan requests were fired in the meantime, coalesce them.
  scanTrigger.buffer(1, DROP_OLDEST)
    .collect {
      withContext(Dispatchers.Default) {
        Radiography.scan(
          viewStateRenderers = DefaultsIncludingPii,
          // Don't trigger infinite recursion.
          viewFilter = skipComposeTestTagsFilter(LIVE_HIERARCHY_TEST_TAG)
        ).let(onScanResult)
      }
    }
}
