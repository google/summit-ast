package com.google.summit.symbols

import com.google.summit.ast.CompilationUnit
import com.google.summit.ast.declaration.ClassDeclaration
import com.google.summit.ast.declaration.MethodDeclaration

/**
 * Use this class to resolve the symbols in a Summit AST.
 */
class SummitResolver {

  fun resolve(allAsts: List<CompilationUnit>) {
    val classMap = SummitClassResolver.resolveClassesAndMethods(allAsts)

    if (PRINT_DEBUG) {
      SummitResolverDebug.printClassesAndMethods(classMap)
    }
  }

  /**
   * A representation of a [ClassDeclaration] with its symbols resolved.
   */
  // TODO: maybe this should be a data class... maybe not.
  internal class ClassSymbol(
    val classDeclaration: ClassDeclaration,
    val methodDeclarationList: List<MethodDeclaration>,
  )

  companion object {
    /**
     * If `true`, [resolve] will print debugging information.
     */
    private const val PRINT_DEBUG = false
  }
}