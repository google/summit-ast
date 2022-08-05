package com.google.summit.ast.modifier

/** Indicates that a [Node][com.google.summit.ast.Node] can be modified by [Modifier]s. */
interface HasModifiers {
  /** The modifiers for this node. */
  var modifiers: List<Modifier>

  /** The subset of [modifiers] that are [annotations][AnnotationModifier]. */
  val annotationModifiers
    get() = modifiers.filterIsInstance<AnnotationModifier>()

  /** The subset of [modifiers] that are [keywords][KeywordModifier]. */
  val keywordModifiers
    get() = modifiers.filterIsInstance<KeywordModifier>()

  /** Returns whether this declaration has a [KeywordModifier] that matches the given [keyword]. */
  fun hasKeyword(keyword: KeywordModifier.Keyword) = keywordModifiers.any { it.keyword == keyword }
}
