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

package com.google.summit.ast

import com.google.summit.ast.declaration.TypeDeclaration

/**
 * A compilation unit.
 *
 * @property typeDeclaration the top-level type declaration
 * @property file path (or other descriptor) that is being translated
 */
class CompilationUnit(val typeDeclaration: TypeDeclaration, val file: String, loc: SourceLocation) :
  NodeWithSourceLocation(loc) {

  override fun getChildren(): List<Node> = listOf(typeDeclaration)
}
