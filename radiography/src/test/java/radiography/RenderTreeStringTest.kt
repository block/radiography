package radiography

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import radiography.internal.renderTreeString

internal class RenderTreeStringTest {

  @Test
  fun `renderTreeString handles single node`() {
    val tree = Node("root")

    val rendering = buildString { renderTreeString(this, tree) { renderNode(it) } }

    assertThat(rendering).isEqualTo("${BLANK}root\n")
  }

  @Test
  fun `renderTreeString handles deep skinny tree`() {
    val tree = Node("root", Node("1", Node("11")))

    val rendering = buildString { renderTreeString(this, tree) { renderNode(it) } }

    assertThat(rendering)
      .isEqualTo(
        """
        |${BLANK}root
        |$BLANK╰─1
        |$BLANK  ╰─11
        |
      """
          .trimMargin()
      )
  }

  @Test
  fun `renderTreeString handles shallow bushy tree`() {
    val tree = Node("root", Node("1"), Node("2"), Node("3"))

    val rendering = buildString { renderTreeString(this, tree) { renderNode(it) } }

    assertThat(rendering)
      .isEqualTo(
        """
        |${BLANK}root
        |$BLANK├─1
        |$BLANK├─2
        |$BLANK╰─3
        |
      """
          .trimMargin()
      )
  }

  @Test
  fun `renderTreeString handles bushy tree with initial deep subtree`() {
    val tree = Node("root", Node("1", Node("11", Node("111"))), Node("2"), Node("3"))

    val rendering = buildString { renderTreeString(this, tree) { renderNode(it) } }

    assertThat(rendering)
      .isEqualTo(
        """
        |${BLANK}root
        |$BLANK├─1
        |$BLANK│ ╰─11
        |$BLANK│   ╰─111
        |$BLANK├─2
        |$BLANK╰─3
        |
      """
          .trimMargin()
      )
  }

  @Test
  fun `renderTreeString handles bushy tree with last deep subtree`() {
    val tree = Node("root", Node("1"), Node("2"), Node("3", Node("33", Node("333"))))

    val rendering = buildString { renderTreeString(this, tree) { renderNode(it) } }

    assertThat(rendering)
      .isEqualTo(
        """
        |${BLANK}root
        |$BLANK├─1
        |$BLANK├─2
        |$BLANK╰─3
        |$BLANK  ╰─33
        |$BLANK    ╰─333
        |
      """
          .trimMargin()
      )
  }

  @Test
  fun `renderTreeString handles 64-deep tree`() {
    val tree =
      generateSequence(Node("leaf")) { child -> Node("node", Node("leaf"), child) }.take(33).last()

    val rendering = buildString { renderTreeString(this, tree) { renderNode(it) } }
    val lastTwoLines = rendering.trim().lines().takeLast(2).map { it.trim() }

    assertThat(lastTwoLines).containsExactly("├─leaf", "╰─leaf")
  }

  @Test
  fun `renderTreeString handles over 64-deep tree`() {
    val tree =
      generateSequence(Node("leaf")) { child -> Node("node", Node("leaf"), child) }.take(300).last()

    val rendering = buildString { renderTreeString(this, tree) { renderNode(it) } }
    val lastTwoLines = rendering.trim().lines().takeLast(2).map { it.trim() }

    assertThat(lastTwoLines).containsExactly("├─leaf", "╰─leaf")
  }

  @Test
  fun `renderTreeString handles multiline nodes`() {
    val tree = Node("root1\nroot2", Node("1\n1", Node("11\n11")), Node("2\n2"))

    val rendering = buildString { renderTreeString(this, tree) { renderNode(it) } }

    assertThat(rendering)
      .isEqualTo(
        """
        |${BLANK}root1
        |${BLANK}root2
        |$BLANK├─1
        |$BLANK│ 1
        |$BLANK│ ╰─11
        |$BLANK│   11
        |$BLANK╰─2
        |$BLANK  2
        |
      """
          .trimMargin()
      )
  }

  private fun StringBuilder.renderNode(node: Node): List<Node> {
    append(node.description)
    return node.children.asList()
  }

  private class Node(val description: String, vararg val children: Node = emptyArray())

  companion object {
    private const val BLANK = '\u00a0'
  }
}
