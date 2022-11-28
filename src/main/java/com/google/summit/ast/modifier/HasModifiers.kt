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

/** Indicates that a [Node][com.google.summit.ast.Node] can be modified by [Modifier]s. */
interface HasModifiers {
  /** The modifiers for this node. */
  var modifiers: List<Modifier>

  /** The subset of [modifiers] that are [annotations][AnnotationModifier]. */
  val annotationModifiers
    get() = modifiers.filterIsInstance<AnnotationModifier>()

  /** The subset of [modifiers] that are [keywords][KeywordModifier]. */
  val keywordModifiers
    get() = modifiers.filterIsInstance<KeywordModifier>()

  /** Returns whether this declaration has a [KeywordModifier] that matches the given [keyword]. */
  fun hasKeyword(keyword: KeywordModifier.Keyword) = keywordModifiers.any { it.keyword == keyword }
}
