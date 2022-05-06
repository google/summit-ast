package com.google.summit.ast

/**
 * An interface to indicate that an AST node is untranslated.
 *
 * In the short-term, a node may be untranslated pending development. Having a placeholder is useful
 * in the meantime to unblock the translation of code that utilizes these constructs.
 *
 * In the long-term, nodes may be left untranslated if they are not needed for any application or if
 * the complexity exceeds the benefit.
 */
interface Untranslated
