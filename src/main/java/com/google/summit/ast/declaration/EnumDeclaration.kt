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

package com.google.summit.ast.declaration

import com.google.summit.ast.Identifier
import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation

/**
 * A declaration for a enum symbol.
 *
 * @param id the name of the enum
 * @property values the list of enum values
 * @param loc the location in the source file
 */
class EnumDeclaration(id: Identifier, val values: List<EnumValue>, loc: SourceLocation) :
  TypeDeclaration(id, loc) {

  override fun getChildren(): List<Node> = modifiers + listOf(id) + values
}

/**
 * A declaration for a enum value.
 *
 * @param id the value name
 */
class EnumValue(val id: Identifier) : Node() {
  /** Returns list of children. */
  override fun getChildren(): List<Node> = listOf(id)

  /** Returns the [SourceLocation] of the identifier. */
  override fun getSourceLocation(): SourceLocation = id.getSourceLocation()
}
