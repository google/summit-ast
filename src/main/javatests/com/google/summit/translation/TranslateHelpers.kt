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

import com.google.common.truth.Truth.assertWithMessage
import com.google.summit.ast.CompilationUnit
import com.google.summit.ast.Node
import com.google.summit.ast.Untranslated
import com.google.summit.ast.traversal.DfsWalker
import com.nawforce.apexparser.ApexLexer
import com.nawforce.apexparser.ApexParser
import com.nawforce.apexparser.CaseInsensitiveInputStream
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

object TranslateHelpers {

  /** Listener to fail on parse errors. */
  object FailOnErrorListener : BaseErrorListener() {
    override fun syntaxError(
      recognizer: Recognizer<*, *>?,
      offendingSymbol: Any?,
      line: Int,
      charPositionInLine: Int,
      msg: String?,
      e: RecognitionException?
    ) {
      assertWithMessage("Parsing failed at $line:$charPositionInLine. Reason: '$msg'").fail()
    }
  }

  /**
   * Parses and translates a source code input string.
   *
   * This helper is missing the extra exception logging (in the methods below) which also records
   * the problematic input text.
   *
   * @param input the source code to parse and translate
   * @return the translated AST for the compilation unit
   * @throw Translate.TranslationException when translation fails
   */
  fun parseAndTranslateWithExceptions(input: String): CompilationUnit {
    val lexer = ApexLexer(CaseInsensitiveInputStream(CharStreams.fromString(input)))
    lexer.addErrorListener(FailOnErrorListener)
    val tokens = CommonTokenStream(lexer)
    val parser = ApexParser(tokens)
    parser.addErrorListener(FailOnErrorListener)
    val tree = parser.compilationUnit()
    return Translate("<input>", tokens).translate(tree)
  }

  /**
   * Parses and translates a source code input string, failing the test if translation fails.
   *
   * The string should be parseable as a complete compilation unit. If there are any parsing errors,
   * asserts a test failure and throws.
   *
   * @param input the source code to parse and translate
   * @return the translated AST for the compilation unit
   * @throw Translate.TranslationException when translation fails
   */
  fun parseAndTranslate(input: String): CompilationUnit {
    try {
      return parseAndTranslateWithExceptions(input)
    } catch (e: Translate.TranslationException) {
      assertWithMessage("Translation failed on %s because %s", e.ctx.text, e.message).fail()
      // Deviation from http://go/java-practices/exceptions#log_rethrow for
      // syntactic purposes, to avoid returning optional type by exiting with exception.
      throw e
    }
  }

  /**
   * Parses a source code input string and then returns the first AST node of a type.
   *
   * The string should be parseable as a complete compilation unit. If there are any parsing errors,
   * asserts a test failure and throws.
   *
   * The AST is searched in depth-first pre-order.
   *
   * @param T the type of [Node] to find
   * @param input the source code to parse and translate
   * @return the first instance of [T] or null if none
   * @throw Translate.TranslationException when translation fails
   */
  inline fun <reified T : Node> parseAndFindFirstNodeOfType(input: String): T? {
    try {
      val root = parseAndTranslateWithExceptions(input)
      return findFirstNodeOfType<T>(root)
    } catch (e: Translate.TranslationException) {
      assertWithMessage("Translation failed on %s because %s", e.ctx.text, e.message).fail()
      // Deviation from http://go/java-practices/exceptions#log_rethrow for
      // syntactic purposes, to avoid returning optional type by exiting with exception.
      throw e
    }
  }

  /**
   * Finds and returns the first AST node of a type in the given AST.
   *
   * The AST is searched in depth-first pre-order.
   *
   * @param T the type of [Node] to find
   * @param root the AST to search
   * @return the first instance of [T] or null if none
   */
  inline fun <reified T : Node> findFirstNodeOfType(root: Node): T? =
    DfsWalker(root, DfsWalker.Ordering.PRE_ORDER)
      .stream()
      .filter { it is T }
      .findFirst()
      .orElse(null) as
      T?

  /**
   * Asserts that the AST has no node of the given type.
   *
   * @param T the type of [Node] to assert non-presence
   * @param root the AST to search
   */
  inline fun <reified T : Node> assertNoNodeOfType(root: Node) {
    val counterexample: Node? = findFirstNodeOfType<T>(root)

    val typeName = T::class.toString()
    assertWithMessage(
        """
        |AST should have no node of type '$typeName'. 
        |Found one from source location ${counterexample?.getSourceLocation()}
        """.trimMargin()
      )
      .that(counterexample)
      .isNull()
  }

  /**
   * Asserts that the AST has no untranslated nodes.
   *
   * @param root the AST to search
   */
  fun assertFullyTranslated(root: Node) {
    val untranslatedCount =
      DfsWalker(root, DfsWalker.Ordering.PRE_ORDER).stream().filter { it is Untranslated }.count()

    assertWithMessage("AST should have no untranslated nodes").that(untranslatedCount).isEqualTo(0)
  }
}
