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

package com.google.summit.translation

import com.google.common.truth.Truth.assertThat
import com.google.summit.ast.SourceLocation
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SourceLocationTest {

  @Test
  fun declaration_hasCorrectSourceLocation() {
    val input = "public class Test { }"

    val cu = TranslateHelpers.parseAndTranslate(input)

    assertThat(cu.typeDeclaration.getSourceLocation())
      .isEqualTo(
        // span from start of "class" keyword to end of input
        SourceLocation(1, input.indexOf("class"), 1, input.length)
      )
    assertThat(cu.typeDeclaration.id.getSourceLocation())
      .isEqualTo(
        // span from start of "Test" identifier to before open braces
        SourceLocation(1, input.indexOf("Test"), 1, input.indexOf("{") - 1)
      )
  }

  @Test
  fun unknownSourceLocation_printsSpecialString() {
    assertThat(SourceLocation.UNKNOWN.isUnknown()).isTrue()
    assertThat(SourceLocation.UNKNOWN.toString()).isEqualTo("<unknown>")
  }
}
