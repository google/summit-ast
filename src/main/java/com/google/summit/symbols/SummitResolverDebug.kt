package com.google.summit.symbols

import com.google.summit.ast.declaration.MethodDeclaration

/**
 * This class contains methods that are useful for developing/debugging the Summit AST resolver.
 */
object SummitResolverDebug {

  /**
   * Prints all classes in [classMap] and their methods.
   */
  internal fun printClassesAndMethods(classMap: Map<String, SummitResolver.ClassSymbol>) {
    for (node in classMap.toSortedMap().values) {
      println("\nClass: ${node.classDeclaration.id.asCodeString()}")
      // TODO: explicitly sort constructors before other methods (this happenstantially is
      //   most often true because of capitalization).
      val foo = node.methodDeclarationList.associateBy { it.qualifiedName }.toSortedMap()
      foo.forEach { it ->
        val methodDeclaration = it.value
        println("* ${methodDeclaration.id.asCodeString()}(${readableParameters(methodDeclaration)})${
          readableColonReturnType(methodDeclaration)
        }")
      }
    }
  }

  private fun readableColonReturnType(methodDeclaration: MethodDeclaration) =
    if (methodDeclaration.isConstructor) "" else ": ${methodDeclaration.returnType.asCodeString()}"

  private fun readableParameters(methodDeclaration: MethodDeclaration): String =
    methodDeclaration.parameterDeclarations.joinToString {
      "${it.type.asCodeString()} ${it.qualifiedName}"
    }
}