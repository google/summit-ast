package com.google.summit.ast

import com.google.summit.ast.traversal.Visitor

/** Abstract base class for all AST types. */
abstract class Node {
  abstract fun getSourceLocation(): SourceLocation

  /**
   * Walks the subtree rooted at this node in depth-first post-order.
   *
   * Every node is passed to [Visitor.visit], unless it is skipped or the traversal is stopped.
   *
   * The traversal is stopped when [Visitor.stopAt] returns `true`. At this point, there will be no
   * further calls to [Visitor.visit], including for any of the nodes on the current path as it is
   * unwound.
   *
   * The children and subtree below a node will be skipped when [Visitor.skipBelow] returns `true`.
   * The node that triggers this condition will still be passed to [Visitor.visit].
   *
   * @param visitor the visitor to pass every node
   */
  fun walkSubtree(visitor: Visitor) {
    /** Recurses on subtree and returns whether the traversal should stop. */
    fun recurseOnNode(node: Node, visitor: Visitor): Boolean {
      if (visitor.stopAt(node)) {
        return true
      }
      if (!visitor.skipBelow(node)) {
        for (child in node.getChildren()) {
          if (recurseOnNode(child, visitor)) {
            return true
          }
        }
      }
      visitor.visit(node)
      return false
    }
    recurseOnNode(this, visitor)
  }

  /**
   * Returns the list of children of this node.
   *
   * The structure of the AST is primarily defined by this method. Because it is a tree, each node
   * should only be the child of exactly one parent, and there must be no cycles in the
   * relationship.
   *
   * The bottom-up tree creation implies that the children already exist when the parent node is
   * constructed, and so the list of children should be fixed from initialization onward.
   */
  abstract fun getChildren(): List<Node>

  /**
   * The parent of this node will return it in [getChildren].
   *
   * These references are first set in one pass in
   * [com.google.summit.translation.Translate.setNodeParents] at
   * the end of translation.
   */
  var parent: Node? = null

  init {
    ++Node.totalCount
  }

  companion object {
    /**
     * Total number of nodes created.
     *
     * This is used as a part of an efficient check during translation that the number of newly
     * created [Node] objects is equal to the number of nodes reachable from the root
     * [CompilationUnit] via transitive calls to [getChildren]. (It's easy to add a node property
     * and forget to include it in that list.)
     */
    var totalCount: Int = 0
      private set
  }
}
