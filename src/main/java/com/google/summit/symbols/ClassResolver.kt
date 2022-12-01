package com.google.summit.symbols

import com.google.summit.ast.CompilationUnit
import com.google.summit.ast.Node
import com.google.summit.ast.declaration.ClassDeclaration
import com.google.summit.ast.declaration.MethodDeclaration
import com.google.summit.ast.traversal.Visitor

/**
 * This class is used to resolve symbols in Apex classes.
 */
object SummitClassResolver {

  /**
   * Given a [listOfAsts], return a Map of all the class definitions (declarations)
   * found in this AST. The key in the map is the qualified name of the class.
   */
  internal fun resolveClassesAndMethods(listOfAsts: List<CompilationUnit>): Map<String, SummitResolver.ClassSymbol> {
    val classMap = mutableMapOf<String, SummitResolver.ClassSymbol>()

    for (ast in listOfAsts) {
      val visitor = object : Visitor() {
        override fun visit(node: Node) {
          if (node is ClassDeclaration) {
            val classVisitor = MethodDeclarationCollectorVisitor()
            node.walkSubtree(classVisitor)
            if (classMap.containsKey(node.qualifiedName)) {
              throw RuntimeException("Found (at least) two class definitions for ${node.qualifiedName} -- '${
                classMap.get(node.qualifiedName)
              }' and '${node.containingFile()}'.")
            } else {
              classMap[node.qualifiedName] = SummitResolver.ClassSymbol(node, classVisitor.methods)
            }
          }
        }
      }
      ast.walkSubtree(visitor)
    }
    return classMap
  }

  // TODO: this seems like a useful function -- find a good home for it.
  /**
   * Walks up the AST tree until it finds the [CompilationUnit], and returns it, or
   * `null` if it can't be found.
   */
  private fun Node.containingFile(): CompilationUnit? {
    if (this is CompilationUnit) {
      return this
    }
    return (parent ?: return null).containingFile()
  }

  class MethodDeclarationCollectorVisitor : Visitor() {
    internal val methods = mutableListOf<MethodDeclaration>()

    override fun visit(node: Node) {
      if (node is MethodDeclaration) {
        methods.add(node)
      }
    }
  }
}