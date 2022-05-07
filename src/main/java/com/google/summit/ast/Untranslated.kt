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
 * An interface to indicate that an AST node is untranslated.
 *
 * In the short-term, a node may be untranslated pending development. Having a placeholder is useful
 * in the meantime to unblock the translation of code that utilizes these constructs.
 *
 * In the long-term, nodes may be left untranslated if they are not needed for any application or if
 * the complexity exceeds the benefit.
 */
interface Untranslated
