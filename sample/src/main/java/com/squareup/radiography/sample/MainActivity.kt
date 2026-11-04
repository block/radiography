package com.squareup.radiography.sample

import android.R.id
import android.app.Activity
import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import radiography.Radiography
import radiography.ScanScopes.FocusedWindowScope
import radiography.ScannableView
import radiography.ViewFilters.skipIdsViewFilter
import radiography.ViewStateRenderers
import radiography.ViewStateRenderers.DefaultsIncludingPii
import radiography.ViewStateRenderers.DefaultsNoPii
import radiography.ViewStateRenderers.androidViewStateRendererFor
import radiography.scan

class MainActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.main)

    findViewById<View>(R.id.show_dialog).setOnClickListener { showSelectionDialog() }
  }

  private fun showSelectionDialog() {
    val renderings =
      listOf(
        "Default" to { Radiography.scan() },
        "Focused window" to { Radiography.scan(scanScope = FocusedWindowScope) },
        "Start from R.id.main" to { findViewById<View>(R.id.main).scan() },
        "Skip R.id.show_dialog" to
          {
            Radiography.scan(viewFilter = skipIdsViewFilter(R.id.show_dialog))
          },
        "Focused window and custom filter" to
          {
            Radiography.scan(
              scanScope = FocusedWindowScope,
              viewFilter = { (it as? ScannableView.AndroidView)?.view !is LinearLayout },
            )
          },
        "Include PII" to { Radiography.scan(viewStateRenderers = DefaultsIncludingPii) },
        "Include PII ellipsized" to
          {
            Radiography.scan(
              viewStateRenderers =
                listOf(
                  ViewStateRenderers.ViewRenderer,
                  ViewStateRenderers.textViewRenderer(
                    renderTextValue = true,
                    textValueMaxLength = 4,
                  ),
                  ViewStateRenderers.CheckableRenderer,
                )
            )
          },
        "Custom LinearLayout renderer" to
          {
            Radiography.scan(
              viewStateRenderers =
                DefaultsNoPii +
                  androidViewStateRendererFor<LinearLayout> {
                    append(
                      if (it.orientation == LinearLayout.HORIZONTAL) "horizontal" else "vertical"
                    )
                  }
            )
          },
        "View.toString() renderer" to
          {
            Radiography.scan(
              viewStateRenderers =
                listOf(
                  androidViewStateRendererFor<View> {
                    append(it.toString().substringAfter(' ').substringBeforeLast('}'))
                  }
                )
            )
          },
      )

    val items = renderings.map { it.first }.toTypedArray()
    AlertDialog.Builder(this)
      .setTitle("Choose rendering")
      .setItems(items) { _, index ->
        val rendering = renderings[index].second()
        Log.d("MainActivity", rendering)
        showResult(rendering)
      }
      .show()
  }

  private fun showResult(rendering: String) {
    val renderingDialog =
      AlertDialog.Builder(this)
        .setTitle("Rendering (also printed to Logcat)")
        .setMessage(rendering)
        .setPositiveButton("Ok") { _, _ -> showSelectionDialog() }
        .show()
    val messageView = renderingDialog.findViewById<TextView>(id.message)
    messageView.textSize = 9f
    messageView.typeface = Typeface.MONOSPACE
  }
}
