/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
