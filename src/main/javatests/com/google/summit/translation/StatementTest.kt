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
import com.google.common.truth.Truth.assertWithMessage
import com.google.summit.ast.CompilationUnit
import com.google.summit.ast.Node
import com.google.summit.ast.declaration.ClassDeclaration
import com.google.summit.ast.expression.LiteralExpression
import com.google.summit.ast.expression.VariableExpression
import com.google.summit.ast.statement.BreakStatement
import com.google.summit.ast.statement.ContinueStatement
import com.google.summit.ast.statement.DmlStatement
import com.google.summit.ast.statement.DoWhileLoopStatement
import com.google.summit.ast.statement.EnhancedForLoopStatement
import com.google.summit.ast.statement.ExpressionStatement
import com.google.summit.ast.statement.ForLoopStatement
import com.google.summit.ast.statement.IfStatement
import com.google.summit.ast.statement.ReturnStatement
import com.google.summit.ast.statement.RunAsStatement
import com.google.summit.ast.statement.SwitchStatement
import com.google.summit.ast.statement.ThrowStatement
import com.google.summit.ast.statement.TryStatement
import com.google.summit.ast.statement.UntranslatedStatement
import com.google.summit.ast.statement.VariableDeclarationStatement
import com.google.summit.ast.statement.WhileLoopStatement
import com.google.summit.ast.traversal.DfsWalker
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StatementTest {

  /** Counts the number of untranslated statement nodes in the AST. */
  private fun countUntranslatedStatements(node: Node): Long {
    return DfsWalker(node, DfsWalker.Ordering.PRE_ORDER)
      .stream()
      .filter { it is UntranslatedStatement }
      .count()
  }

  /** Concatenates the string into a method body and returns the AST. */
  private fun parseApexStatementInCode(statement: String): CompilationUnit {
    return TranslateHelpers.parseAndTranslate(
      """
        class Test {
          void f() {
            $statement
          }
        }
        """
    )
  }

  @Test
  fun methodBody_is_compoundStatement() {
    val compilationUnit = parseApexStatementInCode("1; return 2;")

    val classDecl = compilationUnit.typeDeclaration as ClassDeclaration
    val methodDecl = classDecl.methodDeclarations.first()

    assertNotNull(methodDecl.body)
    assertThat(methodDecl.body?.statements).hasSize(2)
    TranslateHelpers.assertFullyTranslated(compilationUnit)
  }

  @Test
  fun ifStatement_condition_is_variableExpression() {
    val root = parseApexStatementInCode("if (x) { }")
    val node = TranslateHelpers.findFirstNodeOfType<IfStatement>(root)

    assertNotNull(node)
    assertThat(node.condition).isInstanceOf(VariableExpression::class.java)
    val conditionVariable = node.condition as VariableExpression
    assertThat(conditionVariable.id.asCodeString()).isEqualTo("x")
    assertWithMessage("Without `else`, the statement should be null")
      .that(node.elseStatement)
      .isNull()
  }

  @Test
  fun ifStatement_has_elseStatement() {
    val root = parseApexStatementInCode("if (x) { } else { }")
    val node = TranslateHelpers.findFirstNodeOfType<IfStatement>(root)

    assertNotNull(node)
    assertWithMessage("Since `else` is present (even if empty), the statement should not be null")
      .that(node.elseStatement)
      .isNotNull()
  }

  @Test
  fun switchStatement_condition_is_variableExpression() {
    val root = parseApexStatementInCode("switch on x { when else { } }")
    val node = TranslateHelpers.findFirstNodeOfType<SwitchStatement>(root)

    assertNotNull(node)
    assertThat(node.condition).isInstanceOf(VariableExpression::class.java)
    val conditionVariable = node.condition as VariableExpression
    assertThat(conditionVariable.id.asCodeString()).isEqualTo("x")
    val whenClause = node.whenClauses.first()
    assertThat(whenClause).isInstanceOf(SwitchStatement.WhenElse::class.java)
  }

  @Test
  fun switchStatement_whenClause_hasTwoValues() {
    val root = parseApexStatementInCode("switch on x { when value1, value2 { } }")
    val node = TranslateHelpers.findFirstNodeOfType<SwitchStatement.WhenValue>(root)

    assertNotNull(node)
    assertThat(node.values).hasSize(2)
    assertWithMessage(
        "Identifiers in `when` clauses are enum values, which should be a `VariableExpression`"
      )
      .that(node.values.first())
      .isInstanceOf(VariableExpression::class.java)
  }

  @Test
  fun switchStatement_whenClause_hasLiteralValues() {
    val root =
      parseApexStatementInCode(
        """
      switch on x {
        when 0 { }
        when 1234L { }
        when 'string' { }
        when null { }
      }"""
      )

    assertThat(TranslateHelpers.findFirstNodeOfType<LiteralExpression.IntegerVal>(root)).isNotNull()
    assertThat(TranslateHelpers.findFirstNodeOfType<LiteralExpression.LongVal>(root)).isNotNull()
    assertThat(TranslateHelpers.findFirstNodeOfType<LiteralExpression.StringVal>(root)).isNotNull()
    assertThat(TranslateHelpers.findFirstNodeOfType<LiteralExpression.NullVal>(root)).isNotNull()
  }

  @Test
  fun switchStatement_whenClause_declaresVariable() {
    val root = parseApexStatementInCode("switch on x { when Type variable { } }")
    val node = TranslateHelpers.findFirstNodeOfType<SwitchStatement.WhenType>(root)

    assertNotNull(node)
    assertThat(node.type.asCodeString()).isEqualTo("Type")
    assertThat(node.variableDeclaration.type.asCodeString()).isEqualTo("Type")
    assertThat(node.variableDeclaration.id.asCodeString()).isEqualTo("variable")
    assertWithMessage("Variables declared in `when` clauses should not have an initializer")
      .that(node.variableDeclaration.initializer)
      .isNull()
  }

  @Test
  fun traditionalForStatement_declares_twoVariables() {
    val root = parseApexStatementInCode("for (int i=0, j=0; i+j<10; i++, j++) {}")
    val node = TranslateHelpers.findFirstNodeOfType<ForLoopStatement>(root)

    assertNotNull(node)
    assertThat(node.declarations).hasSize(2)
    assertThat(node.initializations).isEmpty()
    assertThat(node.condition).isNotNull()
    assertThat(node.updates).hasSize(2)
  }

  @Test
  fun traditionalForStatement_initializes_twoExpressions() {
    val root = parseApexStatementInCode("for (i=0, j=0; ; ) {}")
    val node = TranslateHelpers.findFirstNodeOfType<ForLoopStatement>(root)

    assertNotNull(node)
    assertThat(node.declarations).isEmpty()
    assertThat(node.initializations).hasSize(2)
    assertThat(node.condition).isNull()
    assertThat(node.updates).isEmpty()
  }

  @Test
  fun enhancedForStatement_has_variableDeclaration() {
    val root = parseApexStatementInCode("for (String s : collection) {}")
    val node = TranslateHelpers.findFirstNodeOfType<EnhancedForLoopStatement>(root)

    assertNotNull(node)
    assertThat(node.elementDeclaration.type.asCodeString()).isEqualTo("String")
    assertThat(node.elementDeclaration.id.asCodeString()).isEqualTo("s")
    assertThat(node.elementDeclaration.initializer).isNull()
  }

  @Test
  fun whileStatement_condition_is_variableExpression() {
    val root = parseApexStatementInCode("while (x) {}")
    val node = TranslateHelpers.findFirstNodeOfType<WhileLoopStatement>(root)

    assertNotNull(node)
    assertThat(node.condition).isInstanceOf(VariableExpression::class.java)
    val conditionVariable = node.condition as VariableExpression
    assertThat(conditionVariable.id.asCodeString()).isEqualTo("x")
  }

  @Test
  fun doWhileStatement_condition_is_variableExpression() {
    val root = parseApexStatementInCode("do {} while(x);")
    val node = TranslateHelpers.findFirstNodeOfType<DoWhileLoopStatement>(root)

    assertNotNull(node)
    assertThat(node.condition).isInstanceOf(VariableExpression::class.java)
    val conditionVariable = node.condition as VariableExpression
    assertThat(conditionVariable.id.asCodeString()).isEqualTo("x")
  }

  @Test
  fun tryStatement_has_finallyBlock() {
    val root = parseApexStatementInCode("try {} finally {}")
    val node = TranslateHelpers.findFirstNodeOfType<TryStatement>(root)

    assertNotNull(node)
    assertThat(node.catchBlocks).hasSize(0)
    assertThat(node.finallyBlock).isNotNull()
  }

  @Test
  fun tryStatement_has_twoCatchBlocks() {
    val root = parseApexStatementInCode("try {} catch (X x) {} catch (Y y) {}")
    val node = TranslateHelpers.findFirstNodeOfType<TryStatement>(root)

    assertNotNull(node)
    assertThat(node.catchBlocks).hasSize(2)
    assertThat(node.finallyBlock).isNull()
  }

  @Test
  fun catchBlock_declares_variable() {
    val root = parseApexStatementInCode("try {} catch (Exception e) {}")
    val node = TranslateHelpers.findFirstNodeOfType<TryStatement>(root)

    assertNotNull(node)
    assertThat(node.catchBlocks).hasSize(1)
    val catchBlock = node.catchBlocks.first()
    assertThat(catchBlock.exceptionVariable.type.asCodeString()).isEqualTo("Exception")
    assertThat(catchBlock.exceptionVariable.id.asCodeString()).isEqualTo("e")
    assertThat(node.finallyBlock).isNull()
  }

  @Test
  fun returnStatement_translation_hasOneChild() {
    val root = parseApexStatementInCode("return 7;")
    val node = TranslateHelpers.findFirstNodeOfType<ReturnStatement>(root)

    assertNotNull(node)
    assertWithMessage("Node should have one child").that(node.getChildren()).hasSize(1)
  }

  @Test
  fun throwStatement_translation_hasOneChild() {
    val root = parseApexStatementInCode("throw e;")
    val node = TranslateHelpers.findFirstNodeOfType<ThrowStatement>(root)

    assertNotNull(node)
    assertWithMessage("Node should have one child").that(node.getChildren()).hasSize(1)
  }

  @Test
  fun breakStatement_translation_isLeafNode() {
    val root = parseApexStatementInCode("break;")
    val node = TranslateHelpers.findFirstNodeOfType<BreakStatement>(root)

    assertNotNull(node)
    assertWithMessage("Node should have no children").that(node.getChildren()).isEmpty()
  }

  @Test
  fun continueStatement_translation_isLeafNode() {
    val root = parseApexStatementInCode("continue;")
    val node = TranslateHelpers.findFirstNodeOfType<ContinueStatement>(root)

    assertNotNull(node)
    assertWithMessage("Node should have no children").that(node.getChildren()).isEmpty()
  }

  @Test
  fun insertDmlStatement_translation_hasOneChild() {
    val root = parseApexStatementInCode("insert obj;")
    val node = TranslateHelpers.findFirstNodeOfType<DmlStatement.Insert>(root)

    assertNotNull(node)
    assertWithMessage("Node should have one child").that(node.getChildren()).hasSize(1)
  }

  @Test
  fun updateDmlStatement_translation_hasOneChild() {
    val root = parseApexStatementInCode("update obj;")
    val node = TranslateHelpers.findFirstNodeOfType<DmlStatement.Update>(root)

    assertNotNull(node)
    assertWithMessage("Node should have one child").that(node.getChildren()).hasSize(1)
  }

  @Test
  fun deleteDmlStatement_translation_hasOneChild() {
    val root = parseApexStatementInCode("delete obj;")
    val node = TranslateHelpers.findFirstNodeOfType<DmlStatement.Delete>(root)

    assertNotNull(node)
    assertWithMessage("Node should have one child").that(node.getChildren()).hasSize(1)
  }

  @Test
  fun undeleteDmlStatement_translation_hasOneChild() {
    val root = parseApexStatementInCode("undelete obj;")
    val node = TranslateHelpers.findFirstNodeOfType<DmlStatement.Undelete>(root)

    assertNotNull(node)
    assertWithMessage("Node should have one child").that(node.getChildren()).hasSize(1)
  }

  @Test
  fun upsertDmlStatement_translation_hasTwoChildren() {
    val root = parseApexStatementInCode("upsert obj field;")
    val node = TranslateHelpers.findFirstNodeOfType<DmlStatement.Upsert>(root)

    assertNotNull(node)
    assertWithMessage("Node should have two children").that(node.getChildren()).hasSize(2)
  }

  @Test
  fun mergeDmlStatement_translation_hasTwoChildren() {
    val root = parseApexStatementInCode("merge objto obj;")
    val node = TranslateHelpers.findFirstNodeOfType<DmlStatement.Merge>(root)

    assertNotNull(node)
    assertWithMessage("Node should have two children").that(node.getChildren()).hasSize(2)
  }

  @Test
  fun runAsStatement_translation_hasContextExpresssions() {
    val root = parseApexStatementInCode("system.runAs(user) { }")
    val node = TranslateHelpers.findFirstNodeOfType<RunAsStatement>(root)

    assertNotNull(node)
    assertWithMessage("Node should have one user context").that(node.contexts).hasSize(1)
  }

  @Test
  fun localVariableDeclarationStatement_translation_wrapsVariableDeclaration() {
    val root = parseApexStatementInCode("String s = null, t = 'hello';")
    val node = TranslateHelpers.findFirstNodeOfType<VariableDeclarationStatement>(root)

    assertNotNull(node)
    assertWithMessage("The statement should declare two variables")
      .that(node.variableDeclarations)
      .hasSize(2)
    val firstDecl = node.variableDeclarations.first()
    assertThat(firstDecl.id.asCodeString()).isEqualTo("s")
    assertThat(firstDecl.type.asCodeString()).isEqualTo("String")
    assertWithMessage("Variable 's' should be initialized to null")
      .that(firstDecl.initializer)
      .isInstanceOf(LiteralExpression.NullVal::class.java)
  }

  @Test
  fun expressionStatement_translation_hasOneChild() {
    val root = parseApexStatementInCode("x + y;")
    val node = TranslateHelpers.findFirstNodeOfType<ExpressionStatement>(root)

    assertNotNull(node)
    assertWithMessage("Node should have one child").that(node.getChildren()).hasSize(1)
  }
}
