package radiography

public class AttributeAppendable(private val stringBuilder: StringBuilder) {

  private var first = true

  public fun append(attribute: CharSequence?) {
    if (attribute == null) {
      return
    }
    if (first) {
      first = false
    } else {
      stringBuilder.append(", ")
    }
    stringBuilder.append(attribute)
  }
}
