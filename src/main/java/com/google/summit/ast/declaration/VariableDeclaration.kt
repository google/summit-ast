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
 * A group of [VariableDeclaration]s that share the same [type] and [modifiers].
 *
 * @property type a reference to the type of the variable
 * @property declarations the variables declared in this group
 * @param loc the location in the source file
 */
class VariableDeclarationGroup(
  val type: TypeRef,
  var declarations: List<VariableDeclaration>,
  loc: SourceLocation,
) : NodeWithSourceLocation(loc), HasModifiers {

  override var modifiers: List<Modifier> = emptyList()

  override fun getChildren(): List<Node> = modifiers + type + declarations

  companion object {
    /**
     * Creates a [VariableDeclarationGroup] with a single [VariableDeclaration].
     *
     * @param id the name of the variable
     * @param type a reference to the type of the variable
     * @param modifiers a list of all declaration modifiers
     * @param initializer an optional initializer expression
     * @param loc the location in the source file
     * */
    fun of(
      id: Identifier,
      type: TypeRef,
      modifiers: List<Modifier>,
      initializer: Expression?,
      loc: SourceLocation,
    ) =
      VariableDeclarationGroup(type, listOf(VariableDeclaration(id, initializer, loc)), loc).apply {
        this.modifiers = modifiers
      }
  }
}

/**
 * A declaration for a local variable.
 *
 * This includes explicit declarations but also (for example) `for` loops.
 *
 * @param id the name of the variable
 * @property type a reference to the type of the variable
 * @property modifiers a list of any modifiers
 * @property initializer an optional initializer expression
 * @param loc the location in the source file
 */
class VariableDeclaration(
  id: Identifier,
  val initializer: Expression?,
  loc: SourceLocation,
) : Declaration(id, loc), HasModifiers {

  /** The [VariableDeclarationGroup] this declaration is contained in. */
  private val group
    get() = parent as VariableDeclarationGroup

  /** The type of this variable. */
  val type: TypeRef
    get() = group.type

  override var modifiers: List<Modifier>
    get() = (parent as HasModifiers).modifiers
    set(value) {
      group.modifiers = value
    }

  override fun getChildren(): List<Node> = listOfNotNull(id, initializer)

  /**
    * Walks up the AST to find the first enclosing [TypeDeclaration].
    *
    * For local variable declarations, there are nodes between it and the
    * enclosing type (which is generally a [MethodDeclaration]).
    */
  override fun getEnclosingType(): TypeDeclaration? {
    var ancestor = parent
    while (ancestor != null) {
      if (ancestor is TypeDeclaration) {
        return ancestor
      }
      ancestor = ancestor.parent
    }
    return null
  }
}
