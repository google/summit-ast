package com.google.summit.ast.traversal

import com.google.summit.ast.Node
import java.util.function.Supplier
import java.util.stream.Stream

typealias NodePredicate = (Node) -> Boolean

/**
 * This depth-first search walker enables AST traversal as a stream of nodes.
 *
 * Given a [start] node, its subtree is walked in depth-first order. The visited nodes can be
 * orderded in either pre- or post- visitation ordering.
 *
 * The [stream] method returns a [Stream<Node>] that allows the nodes to be filtered or processed as
 * they are traversed. A typical usage might be: `DfsWalker(node).stream().filter(...).forEach(...)`
 *
 * The class is a [Supplier] of pairs of a [Node] and a "done" flags. The latter provides an
 * indication to terminate a stream after all nodes have been visited, as is done internally in
 * [stream].
 *
 * @property start the root of the subtree to walk
 * @property ordering whether the nodes are in pre- or post-order visitation
 * @property skipBelow should return `true` to skip the children/subtree of a node
 */
class DfsWalker(
  val start: Node,
  val ordering: Ordering = Ordering.POST_ORDER,
  val skipBelow: NodePredicate = { _ -> false }
) : Supplier<DfsWalker.NodeAndDoneFlag> {

  enum class Ordering {
    PRE_ORDER, // a parent is visited before all its children
    POST_ORDER, // a parent is visited after all its children
  }

  /** Returns a finite stream of [Node] objects from walking the AST. */
  fun stream(): Stream<Node> = Stream.generate(this).takeWhile { !it.done }.map { it.node }

  /** Pairing of a [Node] and a `Boolean` to indicate stream termination. */
  data class NodeAndDoneFlag(val node: Node, val done: Boolean)

  /** A node and its DFS traversal status. */
  private data class NodeAndStatus(val node: Node, val status: Status)
  private enum class Status {
    /** When a node is UNVISITED, we have yet to push its children onto the stack. */
    UNVISITED,
    /**
     * When a node is IN_PROGRESS, we have pushed its children onto the stack and are in the process
     * of traversing its subtree.
     */
    IN_PROGRESS,
  }

  /** Stack of current DFS traversal path. Initialized with [start] node. */
  private val stack = mutableListOf(NodeAndStatus(start, Status.UNVISITED))

  /** Pushes the children of a node onto the stack, unless [skipBelow] is `true`. */
  private fun pushChildrenUnlessSkipped(parent: Node) {
    if (!skipBelow(parent)) {
      stack.addAll(parent.getChildren().asReversed().map { NodeAndStatus(it, Status.UNVISITED) })
    }
  }

  /**
   * Gets the next [Node] and whether the traversal is done.
   *
   * When the return value has the "done" flag set, the [Node] can be ignored.
   */
  override fun get(): NodeAndDoneFlag {
    while (!stack.isEmpty()) {
      val element = stack.removeLast()
      when (ordering) {
        Ordering.PRE_ORDER -> {
          pushChildrenUnlessSkipped(element.node)
          return NodeAndDoneFlag(element.node, false)
        }
        Ordering.POST_ORDER -> {
          if (element.status == Status.IN_PROGRESS) {
            return NodeAndDoneFlag(element.node, false)
          } else {
            stack.add(NodeAndStatus(element.node, Status.IN_PROGRESS))
            pushChildrenUnlessSkipped(element.node)
          }
        }
      }
    }
    // When the stack is empty, the traversal is finished. Indicate via the "done" flag.
    return NodeAndDoneFlag(start /*unused*/, true)
  }
}
