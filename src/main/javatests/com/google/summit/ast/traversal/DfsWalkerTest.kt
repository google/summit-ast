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

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.summit.ast.Node
import java.util.function.Predicate.not
import java.util.stream.Collectors
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DfsWalkerTest {

  @Test
  fun stream_is_PreOrderedCorrectly() {
    val root = FakeAst.createTestAST()

    val visited =
      DfsWalker(root, DfsWalker.Ordering.PRE_ORDER)
        .stream()
        .map(FakeAst::nodeToId)
        .collect(Collectors.toList())

    assertThat(visited).containsExactlyElementsIn(FakeAst.NODE_PREORDER).inOrder()
  }

  @Test
  fun stream_is_PostrderedCorrectly() {
    val root = FakeAst.createTestAST()

    val visited =
      DfsWalker(root, DfsWalker.Ordering.POST_ORDER)
        .stream()
        .map(FakeAst::nodeToId)
        .collect(Collectors.toList())

    assertThat(visited).containsExactlyElementsIn(FakeAst.NODE_POSTORDER).inOrder()
  }

  @Test
  fun takeWhile_halts_preorderImmediately() {
    val root = FakeAst.createTestAST()

    val visited =
      DfsWalker(root, DfsWalker.Ordering.PRE_ORDER)
        .stream()
        .takeWhile(not(FakeAst::nodeIdIs2))
        .map(FakeAst::nodeToId)
        .collect(Collectors.toList())

    assertWithMessage("The traversal should halt at NODE_2, before descending into its children")
      .that(visited)
      .containsExactly(FakeAst.FakeId.NODE_0, FakeAst.FakeId.NODE_1, FakeAst.FakeId.NODE_3)
      .inOrder()
  }

  @Test
  fun skipBelow_excludes_onlySubtree() {
    val root = FakeAst.createTestAST()

    val visited =
      DfsWalker(root, skipBelow = FakeAst::nodeIdIs1)
        .stream()
        .map(FakeAst::nodeToId)
        .collect(Collectors.toList())

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

  @Test
  fun dfsWalker_equivalentTo_walkSubtreeWithVisitor() {
    val root = FakeAst.createTestAST()

    val visited1 = mutableListOf<Node>()
    root.walkSubtree(
      object : Visitor() {
        override fun visit(node: Node) {
          visited1.add(node)
        }
        override fun skipBelow(node: Node): Boolean = FakeAst.nodeIdIs1(node)
      }
    )

    val visited2 =
      DfsWalker(root, skipBelow = FakeAst::nodeIdIs1).stream().collect(Collectors.toList())

    assertThat(visited1).containsExactlyElementsIn(visited2).inOrder()
  }

  @Test
  fun findFirst_matches_firstElementOfCollection() {
    val root = FakeAst.createTestAST()

    val allVisited = DfsWalker(root).stream().collect(Collectors.toList())
    val firstVisited = DfsWalker(root).stream().findFirst().get()

    assertThat(firstVisited).isEqualTo(allVisited.first())
  }

  @Test
  fun filter_includes_onlyMatchingNodes() {
    val root = FakeAst.createTestAST()

    val visited =
      DfsWalker(root)
        .stream()
        .filter(FakeAst::nodeIdIsEven)
        .map(FakeAst::nodeToId)
        .collect(Collectors.toList())

    assertWithMessage("Only even node IDs should be included")
      .that(visited)
      .containsExactly(FakeAst.FakeId.NODE_4, FakeAst.FakeId.NODE_2, FakeAst.FakeId.NODE_0)
      .inOrder()
  }
}
