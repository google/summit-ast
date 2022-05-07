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

package com.google.summit.ast.modifier

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import org.apache.commons.lang3.StringUtils.deleteWhitespace

/**
 * This class represents the use of a [Keyword] modifier.
 *
 * @property keyword which modifier this is
 * @param loc the location in the source file
 */
class KeywordModifier(val keyword: Keyword, loc: SourceLocation) : Modifier(loc) {
  /**
   * This is the union of the set of modifiers that can be applied to any of: type declarations,
   * members, methods, parameters, local variable declarations, caught exceptions, and property
   * getter/setter blocks.
   */
  enum class Keyword {
    ABSTRACT,
    FINAL,
    GLOBAL,
    INHERITEDSHARING,
    OVERRIDE,
    PRIVATE,
    PROTECTED,
    PUBLIC,
    STATIC,
    TESTMETHOD,
    TRANSIENT,
    VIRTUAL,
    WEBSERVICE,
    WITHSHARING,
    WITHOUTSHARING,
  }

  /** Returns empty list of children because this is a leaf node. */
  override fun getChildren(): List<Node> = emptyList()

  companion object {
    /**
     * Converts a keyword string to the keyword enum.
     *
     * This conversion depends on naming the enum values after the keyword strings. Any whitespace
     * in the input (including between words) is removed first.
     *
     * Whitespace-insensitivity is particularly convenient for use with `getText` methods in the
     * parser, which exclude whitespace between tokens.
     *
     * @param str case-insensitive keyword string
     * @return the enum value or null if it is unknown
     */
    fun keywordFromString(str: String): Keyword? {
      return try {
        Keyword.valueOf(deleteWhitespace(str.uppercase()))
      } catch (e: IllegalArgumentException) {
        null
      }
    }
  }
}
