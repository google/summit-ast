package com.google.summit.ast.initializer

import com.google.summit.ast.NodeWithSourceLocation
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.TypeRef

/**
 * Abstract base class for initializers.
 *
 * An initializer is an action that occurs after object allocation to setup its initial state. A
 * constructor is a general initializer for classes, but there is also specific initializer syntax
 * to succinctly populate collection types like arrays, sets, lists, and maps.
 *
 * @property type of the initialized object
 * @param loc the location in the source file
 */
sealed class Initializer(val type: TypeRef, loc: SourceLocation) : NodeWithSourceLocation(loc)
