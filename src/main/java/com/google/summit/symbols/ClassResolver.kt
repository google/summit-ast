package com.google.summit.symbols

import com.google.summit.ast.CompilationUnit
import com.google.summit.ast.Node
import com.google.summit.ast.declaration.ClassDeclaration
import com.google.summit.ast.declaration.MethodDeclaration
import com.google.summit.ast.traversal.Visitor

/**
 * This class is used to resolve symbols in Apex classes.
 */
object ClassResolver {

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
            // TODO(zorzella): we need to properly handle enclosed classes -- right now
            //   the enclosing class would report all methods of the enclosed class as
            //   (also) their own.
            // TODO(zorzella): right now this is causing enclosed classes to be visited
            //   twice (once by the anonymous visitor we're inside of and the other by
            //   the classVisitor). Fix this.
            node.walkSubtree(classVisitor)
            if (classMap.containsKey(node.qualifiedName)) {
              throw RuntimeException("Found (at least) two class definitions for ${node.qualifiedName} -- '${
                classMap[node.qualifiedName]
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
  private fun Node.containingFile(): CompilationUnit {
    if (this is CompilationUnit) {
      return this
    }
    parent?.let {
      return it.containingFile()
    }
    throw RuntimeException("Can't find the CompilationUnit root for $this")
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