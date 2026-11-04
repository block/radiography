package radiography

/** Used to filter out views from the output of [Radiography.scan]. */
public fun interface ViewFilter {
  /** @return true to keep the view in the output of [Radiography.scan], false to filter it out. */
  public fun matches(view: ScannableView): Boolean
}
