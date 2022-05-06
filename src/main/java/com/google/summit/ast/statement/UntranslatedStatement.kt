package com.google.summit.ast.statement

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.Untranslated

/**
 * An untranslated statement.
 *
 * In the short-term, statements may be untranslated pending development. Having a placeholder is
 * useful in the meantime to unblock the translation of code that utilizes these constructs.
 *
 * In the long-term, statements may be left untranslated if they are not needed for any application
 * or if the complexity exceeds the benefit.
 *
 * @param loc the location in the source file
 */
class UntranslatedStatement(loc: SourceLocation) : Untranslated, Statement(loc) {
  override fun getChildren(): List<Node> = emptyList()
}
