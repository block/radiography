package com.squareup.radiography.sample.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Preview
@Composable
private fun RadiographyLogoPreview() {
  RadiographyLogo()
}

/** Draws a Radiography logo, nesting the xray screen until it's too small to see. */
@Composable
fun RadiographyLogo(modifier: Modifier = Modifier) {
  val outerImage = painterResource(R.drawable.logo_outer)
  val innerImage = painterResource(R.drawable.logo_inner)
  val aspectRatio = outerImage.intrinsicSize.width / outerImage.intrinsicSize.height

  Box(modifier.aspectRatio(aspectRatio), propagateMinConstraints = true) {
    Image(outerImage, contentDescription = null)
    InfiniteMirror(centerOffsetFraction = Offset(.5f, .59f), scaleFactor = .46f) {
      Image(innerImage, contentDescription = null)
    }
  }
}

/**
 * Draws the inner, recursive part of the Radiography logo, recursively.
 *
 * The image and density are passed as parameters, instead of being loaded or read from the ambient
 * inside the function, to avoid having to load the same resource or observe to the same ambient
 * multiple times.
 */
@Composable
private fun InfiniteMirror(
  centerOffsetFraction: Offset,
  scaleFactor: Float,
  minimumSizeThreshold: Dp = 8.dp,
  content: @Composable () -> Unit,
) {
  // Use BoxWithConstraints as a big box that centers the actual content within itself.
  BoxWithConstraints(
    Modifier.fillMaxSize().wrapContentSize(FractionalAlignment(centerOffsetFraction))
  ) {
    // Don't draw the content if it's going to be too small to see, and stop recursing.
    val minSize = with(LocalDensity.current) { minimumSizeThreshold.roundToPx() }
    if (constraints.maxHeight < minSize && constraints.maxWidth < minSize) {
      return@BoxWithConstraints
    }

    // Draw the content and recurse at the next scale. Note that the centering is being performed by
    // wrapContentSize above, this modifier just needs to scale.
    Box(Modifier.fillMaxSize(scaleFactor)) {
      content()
      InfiniteMirror(centerOffsetFraction, scaleFactor, minimumSizeThreshold, content)
    }
  }
}

/** Aligns the composable as horizontal and vertical fractions of its bounds. */
private class FractionalAlignment(private val offsetFraction: Offset) : Alignment {
  override fun align(size: IntSize, space: IntSize, layoutDirection: LayoutDirection): IntOffset =
    IntOffset(
      x = (space.width * offsetFraction.x).roundToInt(),
      y = (space.height * offsetFraction.y).roundToInt(),
    )
}
