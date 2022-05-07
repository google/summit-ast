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

/**
 * An identifier, in standard AST parlance, is simply the name of some AST construct that has a
 * name, e.g. the name of a method or class. This information is captured in the [string] property,
 * which is, most often, the name as seen in the source code, but, in some circumstances, this may
 * be a synthetic string -- e.g. for "anonymous initialization block" elements, which, otherwise,
 * would have a blank string as an identifier.
 *
 * An advantage of representing Identifiers as separate objects is that it provides a place to
 * attach a source location. This can be useful for error reporting or for automatic fixes, when we
 * want to manipulate (for example) a variable name but not the entire declaration.
 *
 * A disadvantage of separate Identifier objects is the extra layer of indirection to query the name
 * of a symbol.
 *
 * @property string the identifier name
 * @param loc the location in the source file
 */
class Identifier(val string: String, loc: SourceLocation) :
  NodeWithSourceLocation(loc), PrintableAsCodeString {
  /** Returns empty list of children because this is a leaf node. */
  override fun getChildren(): List<Node> = listOf()

  /** Returns the identifier string. */
  override fun asCodeString(): String = string

  /** Creates a copy of the object with the same string and source location. */
  fun copy(): Identifier = Identifier(string, getSourceLocation())
}
