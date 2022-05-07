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

package com.google.summit.ast.expression

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.TypeRef

/**
 * A cast expression.
 *
 * @property value the instance being cast
 * @property type the resulting type
 * @param loc the location in the source file
 */
class CastExpression(val value: Expression, val type: TypeRef, loc: SourceLocation) :
  Expression(loc) {

  override fun getChildren(): List<Node> = listOf(value, type)
}
