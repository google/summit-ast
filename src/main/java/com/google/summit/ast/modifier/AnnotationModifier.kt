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

import com.google.summit.ast.Identifier
import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation

/**
 * An annotation modifier.
 *
 * The name suffix avoids conflict with the [java.lang.Annotation] class.
 */
class AnnotationModifier(val name: Identifier, loc: SourceLocation) : Modifier(loc) {
  // TODO(b/215202709): Translate annotation parameters and values

  /**
   * Returns the list of children of this node.
   *
   * The children include the name identifier and (eventually) any parameters and values.
   */
  override fun getChildren(): List<Node> = listOf(name)
}
