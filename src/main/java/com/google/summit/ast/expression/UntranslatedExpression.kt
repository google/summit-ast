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
import com.google.summit.ast.Untranslated

/**
 * An untranslated expression.
 *
 * In the short-term, expressions may be untranslated pending development. Having a placeholder is
 * useful in the meantime to unblock the translation of code that utilizes these constructs.
 *
 * In the long-term, expressions may be left untranslated if they are not needed for any application
 * or if the complexity exceeds the benefit.
 *
 * @param loc the location in the source file
 */
class UntranslatedExpression(loc: SourceLocation) : Untranslated, Expression(loc) {
  override fun getChildren(): List<Node> = emptyList()
}
