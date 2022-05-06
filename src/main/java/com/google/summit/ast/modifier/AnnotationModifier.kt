package com.google.summit.ast.modifier

import com.google.summit.ast.Identifier
import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation

/**
 * An annotation modifier.
 *
 * The name suffix avoids conflict with the [java.lang.Annotation] class.
 */
class AnnotationModifier(val name: Identifier, loc: SourceLocation) : Modifier(loc) {
  // TODO(b/215202709): Translate annotation parameters and values

  /**
   * Returns the list of children of this node.
   *
   * The children include the name identifier and (eventually) any parameters and values.
   */
  override fun getChildren(): List<Node> = listOf(name)
}
