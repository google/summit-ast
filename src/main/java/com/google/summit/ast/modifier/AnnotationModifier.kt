/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.summit.ast.modifier

import com.google.summit.ast.Identifier
import com.google.summit.ast.Node
import com.google.summit.ast.NodeWithSourceLocation
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.expression.Expression

/**
 * An annotation modifier.
 *
 * The name suffix avoids conflict with the [java.lang.annotation.Annotation] class.
 */
class AnnotationModifier(
  val name: Identifier,
  val args: List<ElementArgument>,
  loc: SourceLocation
) : Modifier(loc) {
  override fun getChildren(): List<Node> = listOf(name) + args
}

/**
 * An argument to an annotation element, consisting of the [name] of the element and its [value]. In
 * unnamed arguments, the [name] is implicitly set to `value`.
 */
class ElementArgument
private constructor(
  val name: Identifier,
  val value: ElementValue,
  val isNameImplicit: Boolean,
  loc: SourceLocation,
) : NodeWithSourceLocation(loc) {
  companion object {
    private fun implicitName(): Identifier = Identifier("value", SourceLocation.UNKNOWN)

    fun named(name: Identifier, value: ElementValue, loc: SourceLocation): ElementArgument =
      ElementArgument(name, value, isNameImplicit = false, loc)

    fun unnamed(value: ElementValue, loc: SourceLocation): ElementArgument =
      ElementArgument(implicitName(), value, isNameImplicit = true, loc)
  }

  override fun getChildren(): List<Node> = listOf(name, value)
}

/** A value that can be assigned to an annotation element. */
sealed class ElementValue(loc: SourceLocation) : NodeWithSourceLocation(loc) {
  /** An element value that is an [Expression]. */
  class ExpressionValue(val value: Expression, loc: SourceLocation) : ElementValue(loc) {
    override fun getChildren(): List<Node> = listOf(value)
  }

  /** An element value that is an [AnnotationModifier]. */
  class AnnotationValue(val value: AnnotationModifier, loc: SourceLocation) : ElementValue(loc) {
    override fun getChildren(): List<Node> = listOf(value)
  }

  /** An element value that is an array of [ElementValue]s. */
  class ArrayValue(val values: List<ElementValue>, loc: SourceLocation) : ElementValue(loc) {
    override fun getChildren(): List<Node> = values
  }
}
