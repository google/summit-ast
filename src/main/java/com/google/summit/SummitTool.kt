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

import com.google.common.flogger.FluentLogger
import com.google.summit.ast.CompilationUnit
import com.google.summit.serialization.Serializer
import com.google.summit.symbols.SummitResolver
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * This is a simple command line tool to parse and translate Apex source files.
 *
 * Pass arguments that are file paths or directories to search. All files with the `.cls` and
 * `.trigger` extensions will be read.
 *
 * If the first argument is `-json`, then the [Serializer] will be used to write the AST
 * as JSON to a file.
 */
object SummitTool {
  private val logger = FluentLogger.forEnclosingClass()

  // TODO: it'll be useful to have this support flags.
  @JvmStatic
  fun main(args: Array<String>) {
    // TODO: using a `FluentLogger` here (in this class specifically) does not seem like
    //   the right thing to do.
    logger.atInfo().log("Summit AST Tool")
    logger.atInfo().log("Usage: SummitTool [-json] <Apex files or search directories>")

    var numFiles = 0
    var numFailures = 0
    var serializer : Serializer? = null;
    var filesOrDirectories : List<String> = args.toList()

    if (args.firstOrNull() == "-json") {
      logger.atInfo().log("Serializing parsed Apex sources to JSON")
      serializer = Serializer(true)
      filesOrDirectories = args.drop(1)
    }

    for (arg in filesOrDirectories) {
      logger.atInfo().log("Searching for Apex source at: %s", arg)

      // bazel changes the current working directory...
      var absolutePath = Paths.get(arg);
      val workingDirectory = System.getenv("BUILD_WORKING_DIRECTORY")
      if (!absolutePath.isAbsolute && workingDirectory != null) {
        absolutePath = Paths.get(workingDirectory).resolve(absolutePath);
      }

      try {
        val stream: Stream<Path> =
          Files.find(
            absolutePath,
            Integer.MAX_VALUE,
            { path, _ -> SummitAST.isApexSourceFile(path) }
          )

        val paths = stream.toList()
        val allAsts = paths.mapNotNull { path ->
          numFiles++
          var compilationUnit : CompilationUnit? = null

          try {
            compilationUnit = SummitAST.parseAndTranslate(path)

            if (serializer != null) {
              val json = serializer.serialize(compilationUnit!!)
              val jsonFile = path.resolveSibling(path.fileName.toString() + ".json")
              Files.write(jsonFile, Collections.singleton(json), StandardCharsets.UTF_8)
              logger.atInfo().log("Serialized into %s", jsonFile)
            }
          } catch (e: SummitAST.ParseException) {
            logger.atWarning().withCause(e).log("Couldn't parse %s", path)
          }

          compilationUnit
        }
        SummitResolver().resolve(allAsts)
        numFailures = numFiles - allAsts.size
      } catch (e: IOException) {
        logger.atWarning().withCause(e).log("Invalid path %s", arg)
      }
    }

    logger.atInfo().log("Found %d Apex source files", numFiles)
    logger.atInfo().log("Failed to build AST for %d files", numFailures)
  }
}
