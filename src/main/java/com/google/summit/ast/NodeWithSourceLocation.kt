package com.google.summit.ast

/**
 * Abstract base class for all AST nodes that (may) have a source location.
 *
 * @param sourceLocation the location in the source file
 */
abstract class NodeWithSourceLocation(private val sourceLocation: SourceLocation) : Node() {
  override fun getSourceLocation(): SourceLocation = sourceLocation
}
