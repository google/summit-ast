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
import com.google.summit.ast.SourceLocation

object FakeAst {

  enum class FakeId {
    NODE_0,
    NODE_1,
    NODE_2,
    NODE_3,
    NODE_4,
  }

  // Fake several AST node types for testing traversal
  open class FakeNode(val id: FakeId, val childList: List<Node>) : Node() {
    override fun getSourceLocation() = SourceLocation.UNKNOWN
    override fun getChildren(): List<Node> = childList
  }
  class FakeNodeTypeA(id: FakeId, children: List<Node>) : FakeNode(id, children)
  class FakeNodeTypeB(id: FakeId, children: List<Node>) : FakeNode(id, children)

  /**
   * Returns a fake AST with the structure:
   * ```
   *          FakeNode,NODE_0 (root)
   *             /    \
   * FakeNodeA,NODE_1 FakeNodeB,NODE_2
   *          |           |
   * FakeNodeB,NODE_3 FakeNodeB,NODE_4
   * ```
   */
  fun createTestAST(): Node {
    return FakeNode(
      FakeId.NODE_0,
      listOf(
        FakeNodeTypeA(FakeId.NODE_1, listOf(FakeNodeTypeB(FakeId.NODE_3, listOf()))),
        FakeNodeTypeB(FakeId.NODE_2, listOf(FakeNodeTypeB(FakeId.NODE_4, listOf())))
      )
    )
  }

  val NODE_PREORDER =
    listOf(FakeId.NODE_0, FakeId.NODE_1, FakeId.NODE_3, FakeId.NODE_2, FakeId.NODE_4)
  val NODE_POSTORDER =
    listOf(FakeId.NODE_3, FakeId.NODE_1, FakeId.NODE_4, FakeId.NODE_2, FakeId.NODE_0)

  fun nodeToId(node: Node): FakeId? = (node as? FakeNode)?.id
  fun nodeIdIs1(node: Node): Boolean = (node as? FakeNode)?.id == FakeId.NODE_1
  fun nodeIdIs2(node: Node): Boolean = (node as? FakeNode)?.id == FakeId.NODE_2
  fun nodeIdIsEven(node: Node): Boolean =
    (node as? FakeNode)?.id in listOf(FakeId.NODE_0, FakeId.NODE_2, FakeId.NODE_4)
}
