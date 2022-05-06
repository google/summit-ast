package com.google.summit.ast.modifier

import com.google.summit.ast.NodeWithSourceLocation
import com.google.summit.ast.SourceLocation

/**
 * Abstract base class for modifiers.
 *
 * @param loc the location in the source file
 */
sealed class Modifier(loc: SourceLocation) : NodeWithSourceLocation(loc)
