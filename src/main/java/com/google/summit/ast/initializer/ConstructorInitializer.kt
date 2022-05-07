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
 * A object initializer via a constructor call.
 *
 * @property args the constructor call arguments
 * @property type of the initialized object
 * @param loc the location in the source file
 */
class ConstructorInitializer(val args: List<Expression>, type: TypeRef, loc: SourceLocation) :
  Initializer(type, loc) {
  override fun getChildren(): List<Node> = args + listOf(type)
}
