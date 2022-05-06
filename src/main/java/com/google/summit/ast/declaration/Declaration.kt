package com.google.summit.ast.declaration

import com.google.summit.ast.Identifier
import com.google.summit.ast.NodeWithSourceLocation
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.modifier.Modifier

/**
 * A symbol declaration.
 *
 * @property id the unqualified name of the class
 * @param loc the location in the source file
 */
sealed class Declaration(val id: Identifier, loc: SourceLocation) : NodeWithSourceLocation(loc) {

  /**
   * Constructs a declaration and sets modifiers.
   *
   * @param id the unqualified name of the class
   * @param modifier the list of modifiers
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
  var modifiers: List<Modifier> = emptyList()

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
