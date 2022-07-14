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

package com.google.summit.ast.declaration

import com.google.summit.ast.Identifier
import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.statement.CompoundStatement

/**
 * A declaration for a trigger.
 *
 * @param id the unqualified name of the trigger
 * @param target the SObject type to watch
 * @param cases a list of operations to watch
 * @param body the code to execute
 * @param loc the location in the source file
 */
class TriggerDeclaration(
  id: Identifier,
  val target: Identifier,
  val cases: List<TriggerCase>,
  val body: CompoundStatement,
  loc: SourceLocation
) : TypeDeclaration(id, loc) {

  /** This determines the cases when the trigger is executed. */
  enum class TriggerCase {
    TRIGGER_BEFORE_INSERT,
    TRIGGER_BEFORE_UPDATE,
    TRIGGER_BEFORE_DELETE,
    TRIGGER_BEFORE_UNDELETE,
    TRIGGER_AFTER_INSERT,
    TRIGGER_AFTER_UPDATE,
    TRIGGER_AFTER_DELETE,
    TRIGGER_AFTER_UNDELETE,
  }

  override fun getChildren(): List<Node> = modifiers + listOfNotNull(id, target, body)
}
