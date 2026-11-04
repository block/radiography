package radiography.internal

internal fun CharSequence.ellipsize(maxLength: Int): CharSequence =
  if (length > maxLength) "${subSequence(0, maxLength - 1)}…" else this

internal fun formatPixelDimensions(width: Int, height: Int): String = "$width×${height}px"
