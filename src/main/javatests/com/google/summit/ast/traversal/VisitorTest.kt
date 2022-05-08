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

import com.google.common.truth.Truth.assertWithMessage
import com.google.summit.ast.Node
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

typealias FakeNode = FakeAst.FakeNode

@RunWith(JUnit4::class)
class VisitorTest {

  @Test
  fun visit_called_inDepthFirstPostOrder() {
    val root = FakeAst.createTestAST()

    val visited = mutableListOf<FakeAst.FakeId>()
    root.walkSubtree(
      object : Visitor() {

        override fun visit(node: Node) {
          if (node is FakeAst.FakeNode) {
            visited.add(node.id)
          }
        }
      }
    )

    assertWithMessage("All nodes should be visited in depth-first post-order")
      .that(visited)
      .containsExactlyElementsIn(FakeAst.NODE_POSTORDER)
      .inOrder()
  }

  @Test
  fun stopAt_halts_traversalImmediately() {
    val root = FakeAst.createTestAST()

    val visited = mutableListOf<FakeAst.FakeId>()
    root.walkSubtree(
      object : Visitor() {

        override fun visit(node: Node) {
          if (node is FakeAst.FakeNode) {
            visited.add(node.id)
          }
        }

        override fun stopAt(node: Node) = FakeAst.nodeIdIs2(node)
      }
    )

    assertWithMessage(
        "The traversal should halt at NODE_2, before descending into its children " +
          "and without visiting any of the in-progress path"
      )
      .that(visited)
      .containsExactly(FakeAst.FakeId.NODE_3, FakeAst.FakeId.NODE_1)
      .inOrder()
  }

  @Test
  fun skipBelow_excludes_onlySubtree() {
    val root = FakeAst.createTestAST()

    val visited = mutableListOf<FakeAst.FakeId>()
    root.walkSubtree(
      object : Visitor() {

        override fun visit(node: Node) {
          if (node is FakeAst.FakeNode) {
            visited.add(node.id)
          }
        }

        override fun skipBelow(node: Node) = FakeAst.nodeIdIs1(node)
      }
    )

    assertWithMessage("Skipping below at NODE_1 should exclude exactly NODE_3")
      .that(visited)
      .doesNotContain(FakeAst.FakeId.NODE_3)
    assertWithMessage("NODE_1, where the condition is triggered, should still be visited")
      .that(visited)
      .contains(FakeAst.FakeId.NODE_1)
    assertWithMessage("Nodes in other subtrees should be unaffected, for example NODE_4")
      .that(visited)
      .contains(FakeAst.FakeId.NODE_4)
  }
}
