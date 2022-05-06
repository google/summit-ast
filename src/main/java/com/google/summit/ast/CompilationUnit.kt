package com.google.summit.ast

import com.google.summit.ast.declaration.TypeDeclaration

/**
 * A compilation unit.
 *
 * @property typeDeclaration the top-level type declaration
 * @property file path (or other descriptor) that is being translated
 */
class CompilationUnit(val typeDeclaration: TypeDeclaration, val file: String, loc: SourceLocation) :
  NodeWithSourceLocation(loc) {

  override fun getChildren(): List<Node> = listOf(typeDeclaration)
}
