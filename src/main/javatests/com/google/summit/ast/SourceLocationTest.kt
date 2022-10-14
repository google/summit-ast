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

package com.google.summit.ast

import com.google.common.truth.Truth.assertThat
import com.google.summit.ast.Node
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SourceLocationTest {

  @Test
  fun unionOf_chooses_nonNullValues() {
    val unknown = SourceLocation.UNKNOWN
    val withLinesOnly = SourceLocation(1, null, 3, null)
    val withLinesAndColumns = SourceLocation(withLinesOnly.startLine, 10, withLinesOnly.endLine, 10)

    assertThat(unionOf(unknown, unknown)).isEqualTo(unknown)
    assertThat(unionOf(withLinesOnly, unknown)).isEqualTo(withLinesOnly)
    assertThat(unionOf(unknown, withLinesOnly)).isEqualTo(withLinesOnly)
    assertThat(unionOf(withLinesOnly, withLinesAndColumns)).isEqualTo(withLinesAndColumns)
    assertThat(unionOf(withLinesAndColumns, withLinesOnly)).isEqualTo(withLinesAndColumns)
  }

  @Test
  fun unionOf_returns_newRange() {
    val lower = SourceLocation(1, 1, 2, 2)
    val upper = SourceLocation(2, 2, 3, 3)

    val expected = SourceLocation(lower.startLine, lower.startColumn,
                                  upper.endLine, upper.endColumn)
    assertThat(unionOf(lower, upper)).isEqualTo(expected)
    assertThat(unionOf(upper, lower)).isEqualTo(expected)
  }

  @Test
  fun unionOf_is_idempotent() {
    val loc = SourceLocation(1, 3, 4, 2)

    assertThat(unionOf(loc)).isEqualTo(loc)
    assertThat(unionOf(loc, loc, loc)).isEqualTo(loc)
    assertThat(unionOf(loc, unionOf(loc, loc))).isEqualTo(loc)
  }

  @Test
  fun unionOf_ranks_lineOverColumn() {
    val widerLines = SourceLocation(1, 6, 10, 5)
    val widerColumns = SourceLocation(5, 1, 6, 10)

    assertThat(unionOf(widerLines, widerColumns)).isEqualTo(widerLines)
    assertThat(unionOf(widerColumns, widerLines)).isEqualTo(widerLines)
  }
}
