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

package com.google.summit

import com.google.common.base.Ascii
import com.google.common.flogger.FluentLogger
import com.google.summit.translation.Translate
import com.nawforce.apexparser.ApexLexer
import com.nawforce.apexparser.ApexParser
import com.nawforce.apexparser.CaseInsensitiveInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

/** This is a simple command line tool to parse and translate Apex source files.
  *
  * Pass arguments that are file paths or directories to search. All files with
  * the *.cls extension will be read.
  */
object SummitTool {
  val logger = FluentLogger.forEnclosingClass()

  /** Listener for syntax errors that keeps a total count. */
  class SyntaxErrorListener : BaseErrorListener() {
    public var numErrors = 0

    override fun syntaxError(
      recognizer: Recognizer<*, *>,
      offendingSymbol: Any?,
      line: Int,
      charPositionInLine: Int,
      msg: String,
      e: RecognitionException?
    ) {
      this.numErrors += 1
      logger.atInfo().log("Syntax error at %d:%d: %s", line, charPositionInLine, msg)
    }
  }

  /** Parses and translates a single Apex source file and returns success. */
  private fun parseAndTranslate(path: Path): Boolean {
    // Apex is a case-insensitive language and the grammar is
    // defined to operate on fully lower-cased inputs.
    val lowerCasedStream = CaseInsensitiveInputStream(CharStreams.fromPath(path))
    val lexer = ApexLexer(lowerCasedStream)
    val tokens = CommonTokenStream(lexer)
    val parser = ApexParser(tokens)

    val errorCounter = SyntaxErrorListener()
    lexer.addErrorListener(errorCounter)
    parser.addErrorListener(errorCounter)

    // Do parse as complete compilation unit
    val tree = when {
      isApexClassFile(path) -> parser.compilationUnit()
      isApexTriggerFile(path) -> parser.triggerUnit()
      else -> throw IllegalArgumentException("Unexpected file type")
    }

    if (errorCounter.numErrors > 0) {
      logger.atWarning().log("Failed to parse %s", path)
      return false // failure
    }

    try {
      val translator = Translate(path.toString(), tokens)
      translator.translate(tree)
    } catch (e: Translate.TranslationException) {
      logger.atWarning().withCause(e).log("Failed to translate %s", path)
      return false // failure
    }

    return true
  }

  /**
   * Returns true if the path is an Apex class file. These are regular files with the
   * (case-insensitive) suffix ".cls".
   */
  private fun isApexClassFile(path: Path): Boolean =
    Files.isRegularFile(path) && Ascii.toLowerCase(path.toString()).endsWith(".cls")

  /**
   * Returns true if the path is an Apex trigger file. These are regular files with the
   * (case-insensitive) suffix ".trigger".
   */
  private fun isApexTriggerFile(path: Path): Boolean =
    Files.isRegularFile(path) && Ascii.toLowerCase(path.toString()).endsWith(".trigger")

  /** Returns true if the path is an Apex source file: either a class or a trigger. */
  private fun isApexSourceFile(path: Path): Boolean =
    isApexClassFile(path) || isApexTriggerFile(path)

  @JvmStatic
  fun main(args: Array<String>) {
    logger.atInfo().log("Summit AST Tool")
    logger.atInfo().log("Usage: SummitTool <Apex files or search directories>")

    var numFiles = 0
    var numFailures = 0
    for (arg in args) {
      logger.atInfo().log("Searching for Apex source at: %s", arg)

      try {
        val stream: Stream<Path> =
          Files.find(Paths.get(arg), Integer.MAX_VALUE, { path: Path, _ -> isApexSourceFile(path) })
        stream.forEach({ path: Path ->
          numFiles++
          val success = parseAndTranslate(path)
          numFailures += if (success) 0 else 1
        })
      } catch (e: IOException) {
        logger.atWarning().withCause(e).log("Invalid path %s", arg)
      }
    }

    logger.atInfo().log("Found %d Apex source files", numFiles)
    logger.atInfo().log("Failed to build AST for %d files", numFailures)
  }
}
