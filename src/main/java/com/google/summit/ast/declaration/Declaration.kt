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
import com.google.summit.ast.NodeWithSourceLocation
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.modifier.HasModifiers
import com.google.summit.ast.modifier.Modifier

/**
 * A symbol declaration.
 *
 * @property id the unqualified name of the declaration
 * @param loc the location in the source file
 */
sealed class Declaration(val id: Identifier, loc: SourceLocation) : NodeWithSourceLocation(loc) {
  /**
   * The qualified Name of the symbol includes any enclosing class(es) as prefixes, delimited by a
   * dot. For example: `OuterClass.InnerClass.innerMethod`
   */
  val qualifiedName: String
    get() =
      if (getEnclosingType() != null) {
        "${getEnclosingType()?.qualifiedName}.${id.string}"
      } else {
        id.string
      }

  /** Returns the enclosing type declaration. */
  fun getEnclosingType(): TypeDeclaration? = parent as? TypeDeclaration
}

/**
 * A [Declaration] that has associated [Modifier]s.
 *
 * @property id the unqualified name of the declaration
 * @param loc the location in the source file
 */
sealed class DeclarationWithModifiers(id: Identifier, loc: SourceLocation) :
  Declaration(id, loc), HasModifiers {

  /**
   * Constructs a declaration and sets modifiers.
   *
   * @param id the unqualified name of the declaration
   * @param modifiers the list of modifiers
   * @param loc the location in the source file
   */
  constructor(id: Identifier, modifiers: List<Modifier>, loc: SourceLocation) : this(id, loc) {
    this.modifiers = modifiers
  }

  /**
   * The modifiers for this declaration.
   *
   * Because the modifiers are frequently parsed outside of the specific declaration rule in the
   * Apex grammar, they are mutable and appended post-construction.
   */
  final override var modifiers: List<Modifier> = emptyList()
}
