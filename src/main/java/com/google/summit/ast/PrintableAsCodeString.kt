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
 * This interface is for nodes that can print their subtree as an Apex code string.
 *
 * Not every node corresponds to actual syntax in a well-formed Apex compilation unit. For example,
 * anonymous code blocks and special callsites may utilize special name identifiers for internal
 * use. These identifiers would never be printed in the code of their parent node, but the child
 * nodes may still return a code-like string for testing or debugging.
 */
interface PrintableAsCodeString {
  /** Returns the node's subtree printed as an Apex code string. */
  fun asCodeString(): String
}
