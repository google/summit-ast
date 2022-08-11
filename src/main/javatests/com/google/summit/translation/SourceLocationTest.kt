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
import com.google.summit.ast.declaration.ClassDeclaration
import com.google.summit.ast.declaration.FieldDeclarationGroup
import com.google.summit.testing.TranslateHelpers
import kotlin.test.assertNotNull
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

  @Test
  fun extractFromSource_oneLineNode() {
    val input =
      """
        class Test {
          public String field
            = 'Hello';
        }
        """
    val classDecl = TranslateHelpers.parseAndFindFirstNodeOfType<ClassDeclaration>(input)
    assertNotNull(classDecl)
    val loc = classDecl.id.getSourceLocation()

    val extracted = loc.extractFrom(input)

    assertThat(extracted).isEqualTo("Test")
  }

  @Test
  fun extractFromSource_multiLineNode() {
    val input =
      """
        class Test {
          public String field
            = 'Hello';
        }
        """
    val fieldDeclGroup = TranslateHelpers.parseAndFindFirstNodeOfType<FieldDeclarationGroup>(input)
    assertNotNull(fieldDeclGroup)
    val loc = fieldDeclGroup.getSourceLocation()

    val extracted = loc.extractFrom(input)

    assertThat(extracted).isEqualTo("""String field
            = 'Hello';""")
  }

  @Test
  fun extractFromSource_wholeFile() {
    val input =
      """
        class Test {
          public String field
            = 'Hello';
        }
        """.trim()
    val cu = TranslateHelpers.parseAndTranslate(input)
    val loc = cu.getSourceLocation()

    val extracted = loc.extractFrom(input)

    assertThat(extracted).isEqualTo(input)
  }

  @Test
  fun extractFromSource_arbitraryLocation() {
    val input =
      """
        class Test {
          public String field
            = 'Hello';
        }
        """.trim()
    val loc = SourceLocation(startLine = 1, startColumn = 4, endLine = 3, endColumn = 16)

    val extracted = loc.extractFrom(input)

    assertThat(extracted).isEqualTo("""s Test {
          public String field
            = 'H""")
  }
}
