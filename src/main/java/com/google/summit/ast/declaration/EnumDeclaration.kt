package com.google.summit.ast.declaration

import com.google.summit.ast.Identifier
import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation

/**
 * A declaration for a enum symbol.
 *
 * @param id the name of the enum
 * @param loc the location in the source file
 */
class EnumDeclaration(id: Identifier, loc: SourceLocation) : TypeDeclaration(id, loc) {
  // TODO(b/215202709): the body is not yet translated

  override fun getChildren(): List<Node> = modifiers + listOf(id)
}
