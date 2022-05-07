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

package com.google.summit.ast.initializer

import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.TypeRef
import com.google.summit.ast.expression.Expression

/**
 * An object initializer for lists, sets, or arrays via a list of values.
 *
 * An empty set of values may also be used to initialize maps.
 *
 * @property values in the initialized collection
 * @property type of the initialized object
 * @param loc the location in the source file
 */
class ValuesInitializer(val values: List<Expression>, type: TypeRef, loc: SourceLocation) :
  Initializer(type, loc) {
  override fun getChildren(): List<Node> = values + listOf(type)
}
