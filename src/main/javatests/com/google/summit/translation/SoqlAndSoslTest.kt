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
import com.google.summit.ast.CompilationUnit
import com.google.summit.ast.expression.SoqlExpression
import com.google.summit.ast.expression.SoslExpression
import com.google.summit.ast.expression.VariableExpression
import com.google.summit.testing.TranslateHelpers
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SoqlAndSoslTest {

  /** Concatenates the string in a field initializer context and returns the AST. */
  private fun parseSoqlOrSoslInCode(soql: String): CompilationUnit {
    return TranslateHelpers.parseAndTranslate(
      """
        class Test {
          Object x = [$soql];
        }
        """
    )
  }

  @Test
  fun soqlPrimary_contains_query() {
    val query = "SELECT Id FROM Contact"

    val root = parseSoqlOrSoslInCode(query)

    val node = TranslateHelpers.findFirstNodeOfType<SoqlExpression>(root)
    assertThat(node).isNotNull()
    assertThat(node!!.query).isEqualTo(query)
    assertThat(node.bindings).isEmpty()
  }

  @Test
  fun soslPrimary_contains_query() {
    val query = "FIND :search IN ALL FIELDS RETURNING Account(Name)"

    val root = parseSoqlOrSoslInCode(query)

    val node = TranslateHelpers.findFirstNodeOfType<SoslExpression>(root)
    assertThat(node).isNotNull()
    assertThat(node!!.query).isEqualTo(query)
    assertThat(node.bindings).hasSize(1)
  }

  @Test
  fun soslPrimary_contains_query_with_all_bindings() {
    val query = """
      FIND :myString1 IN ALL FIELDS
      RETURNING
         Account (Id, Name WHERE Name LIKE :myString2
                  LIMIT :myInt3),
         Contact,
         Opportunity,
         Lead
      //WITH DIVISION =:myString4 // that's not supported by apex-parser yet
      WITH DIVISION = 'ccc'
      LIMIT :myInt5
    """.trimIndent()

    val root = parseSoqlOrSoslInCode(query)

    val node = TranslateHelpers.findFirstNodeOfType<SoslExpression>(root)
    assertThat(node).isNotNull()
    assertThat(node!!.query).isEqualTo(query)
    assertThat(node.bindings).hasSize(4)
    val varExpressions = node.bindings
        .flatMap { it.getChildren() }
        .filterIsInstance<VariableExpression>()
        .map { it.id.string }
        .toList()
    assertThat(varExpressions).hasSize(4)
    assertThat(varExpressions).containsExactly("myString1", "myString2", "myInt3", "myInt5")
  }

  @Test
  fun soslWithUserMode() {
    val query = "FIND :SecondarySearchList IN NAME FIELDS RETURNING " +
            "Account(Id, Account.Name WHERE ID = '' LIMIT 100) " +
            "WITH USER_MODE"

    val root = parseSoqlOrSoslInCode(query)

    val node = TranslateHelpers.findFirstNodeOfType<SoslExpression>(root)
    assertThat(node).isNotNull()
    assertThat(node!!.query).isEqualTo(query)
    assertThat(node.bindings).hasSize(1)
  }
}
