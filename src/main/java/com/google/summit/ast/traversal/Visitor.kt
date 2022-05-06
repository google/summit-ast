package com.google.summit.ast.traversal

import com.google.summit.ast.Node

/**
 * Abstract interface for an AST visitor.
 *
 * @see Node.walkSubtree
 */
abstract class Visitor {
  /** Visits every (non-skipped) node in the tree. */
  abstract fun visit(node: Node)

  /* Stops the traversal when `true` is returned. The method is called immediately
   * after descending into a node. After stopping, there will be no
   * further calls to [Visitor.visit], including for this or any of the nodes on
   * the current path as it is
   * unwound.
   *
   * The default implementation does not stop until the walk is complete.
   */
  open fun stopAt(node: Node): Boolean = false

  /**
   * Skips traversing a node's children and subtree when `true`.
   *
   * The default implementation does not skip any nodes.
   */
  open fun skipBelow(node: Node): Boolean = false
}
