package com.google.summit.ast.declaration

import com.google.summit.ast.Identifier
import com.google.summit.ast.SourceLocation

/**
 * A declaration for a type, which could be a class, interface, or enum.
 *
 * @param id the unqualified name of the type
 * @param loc the location in the source file
 */
sealed class TypeDeclaration(id: Identifier, loc: SourceLocation) : Declaration(id, loc)
