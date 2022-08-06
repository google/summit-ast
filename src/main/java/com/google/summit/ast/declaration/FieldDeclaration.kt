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
import com.google.summit.ast.NodeWithSourceLocation
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.TypeRef
import com.google.summit.ast.expression.Expression
import com.google.summit.ast.modifier.HasModifiers
import com.google.summit.ast.modifier.Modifier

/**
 * A group of comma-separated [FieldDeclaration]s that share the same [type] and [modifiers].
 *
 * @property type a reference to the type of the field
 * @property declarations the fields declared in this group
 * @param loc the location in the source file
 */
class FieldDeclarationGroup(
  val type: TypeRef,
  var declarations: List<FieldDeclaration>,
  loc: SourceLocation
) : NodeWithSourceLocation(loc), HasModifiers {

  override var modifiers: List<Modifier> = emptyList()

  override fun getChildren(): List<Node> = modifiers + type + declarations
}

/**
 * A declaration for a field (a.k.a. class member).
 *
 * @param id the name of the field
 * @property initializer an optional initializer expression
 * @param loc the location in the source file
 */
class FieldDeclaration(id: Identifier, val initializer: Expression?, loc: SourceLocation) :
  Declaration(id, loc), HasModifiers {

  /** The [FieldDeclarationGroup] this declaration is contained in. */
  private val group
    get() = parent as FieldDeclarationGroup

  override var modifiers: List<Modifier>
    get() = group.modifiers
    set(value) {
      group.modifiers = value
    }

  override fun getChildren(): List<Node> = listOfNotNull(id, initializer)

  override fun getEnclosingType(): TypeDeclaration? = group.parent as? TypeDeclaration
}
