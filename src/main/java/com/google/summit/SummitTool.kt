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
import com.google.summit.symbols.SummitResolver
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * This is a simple command line tool to parse and translate Apex source files.
 *
 * Pass arguments that are file paths or directories to search. All files with the `.cls` and
 * `.trigger` extensions will be read.
 */
object SummitTool {
  private val logger = FluentLogger.forEnclosingClass()

  // TODO: it'll be useful to have this support flags.
  @JvmStatic
  fun main(args: Array<String>) {
    // TODO: using a `FluentLogger` here (in this class specifically) does not seem like
    //   the right thing to do.
    logger.atInfo().log("Summit AST Tool")
    logger.atInfo().log("Usage: SummitTool <Apex files or search directories>")

    var numFiles = 0
    var numFailures = 0
    for (arg in args) {
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
          SummitAST.parseAndTranslate(path)
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
