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

/**
 * Source range within a file.
 *
 * A special "unknown" value can also be constructed.
 *
 * @property startLine the line where the source location starts
 * @property startColumn the column where the source location starts
 * @property endLine the line where the source location ends
 * @property endColumn the column where the source location ends
 */
data class SourceLocation(
  val startLine: Int?,
  val startColumn: Int?,
  val endLine: Int?,
  val endColumn: Int?,
) {
  /** Returns whether this is an unknown source location. */
  fun isUnknown(): Boolean = (startLine == null)

  /** Prints known source locations as a range and unknown ones as such. */
  override fun toString(): String {
    return if (isUnknown()) {
      "<unknown>"
    } else {
      "[$startLine:$startColumn,$endLine:$endColumn]"
    }
  }

  /**
   * Extracts this source location from a [source] string. If this location is [unknown][isUnknown],
   * returns `null`.
   */
  fun extractFrom(source: String): String? {
    if (isUnknown()) {
      return null
    }

    val lines = source.lines().subList(startLine!! - 1, endLine!!)
    val joinedLines = lines.joinToString(separator = "\n")

    val distanceFromStart = startColumn!!
    val distanceFromEnd = lines.last().length - endColumn!!
    return joinedLines.drop(distanceFromStart).dropLast(distanceFromEnd)
  }

  companion object {
    /** An unknown source location. */
    val UNKNOWN = SourceLocation(null, null, null, null)
  }
}
