package com.google.summit.ast

/**
 * A type reference.
 *
 * These nodes occur in any context where a static type appears in code.
 *
 * @property components the sequence of one or more identifiers (with optional type arguments)
 * @property arrayNesting the number of levels of array nesting for this type
 */
class TypeRef(val components: List<Component>, val arrayNesting: Int) :
  Node(), PrintableAsCodeString {

  /** Returns whether this is the special `void` type. */
  fun isVoid(): Boolean = components.isEmpty()

  /**
   * A sequence of [Component]s delimited by dot constitutes a type reference.
   *
   * An example [TypeRef] with multiple [Component]s is an inner class.
   *
   * @property id the type identifier
   * @property args zero or more type arguments
   */
  data class Component(val id: Identifier, val args: List<TypeRef>) : PrintableAsCodeString {
    /**
     * Returns the [Component] as a code string.
     *
     * The angles braces are only included to enclose one or more type arguments.
     */
    override fun asCodeString(): String = id.asCodeString() + optionalTypeArgumentsAsCodeString()

    private fun optionalTypeArgumentsAsCodeString(): String =
      if (args.isNotEmpty()) {
        "<${args.joinToString { it.asCodeString() }}>"
      } else {
        ""
      }
  }

  /**
   * Returns the type reference as a code string.
   *
   * If this type [isVoid], then return "void", otherwise the format is:
   * ```
   * identifier ("." identifier)* "[]"*
   * ```
   */
  override fun asCodeString(): String =
    if (isVoid()) {
      "void"
    } else {
      components.joinToString(".") { it.asCodeString() } + "[]".repeat(arrayNesting)
    }

  /** Returns an unknown location, but the constituent identifiers have locations. */
  override fun getSourceLocation(): SourceLocation = SourceLocation.UNKNOWN

  /** Returns children from the identifiers and type arguments of all components. */
  override fun getChildren(): List<Node> = components.flatMap { listOf(it.id) + it.args }

  companion object {
    /** Creates a special `void` type to indicate the lack of a value. */
    fun createVoid(): TypeRef = TypeRef(emptyList(), 0)

    /**
     * Creates a type from an unqualified name.
     *
     * @param unqualifiedName the type name identifier (without dots)
     */
    fun createFromUnqualifiedName(unqualifiedName: Identifier) =
      TypeRef(listOf(Component(unqualifiedName, emptyList())), arrayNesting = 0)

    /**
     * Creates a type from a qualified name.
     *
     * @param qualifiedName the type name
     */
    fun createFromQualifiedName(qualifiedName: String) =
      TypeRef(qualifiedNameToComponentList(qualifiedName), arrayNesting = 0)

    /** Splits a string on the dots and creates a component from each piece. */
    private fun qualifiedNameToComponentList(qualifiedName: String): List<Component> =
      qualifiedName.split('.').map {
        Component(Identifier(it, SourceLocation.UNKNOWN), args = emptyList())
      }
  }
}
