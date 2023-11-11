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

package com.google.summit

import com.google.common.truth.Truth.assertThat
import com.google.summit.SummitAST.CompilationType
import org.junit.Assert
import kotlin.io.path.Path
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SummitASTTest {
  private val classString = "global with sharing interface Test { }"
  private val triggerString = "trigger MyTrigger on MyObject(before update, after delete) { }"

  @Test
  fun parsePath_valid() {
    val path = Path("src/main/javatests/com/google/summit/testdata/mixednodes.cls")
    val cu = SummitAST.parseAndTranslate(path)
    assertThat(cu).isNotNull()
  }

  @Test
  fun parseString_valid_explicitClass() {
    val string = classString
    val cu = SummitAST.parseAndTranslate(string, type = CompilationType.CLASS)
    assertThat(cu).isNotNull()
  }

  @Test
  fun parseString_valid_implicitClass() {
    val string = classString
    val cu = SummitAST.parseAndTranslate(string, type = null)
    assertThat(cu).isNotNull()
  }

  @Test
  fun parseString_valid_explicitTrigger() {
    val string = triggerString
    val cu = SummitAST.parseAndTranslate(string, type = CompilationType.TRIGGER)
    assertThat(cu).isNotNull()
  }

  @Test
  fun parseString_valid_implicitTrigger() {
    val string = triggerString
    val cu = SummitAST.parseAndTranslate(string, type = null)
    assertThat(cu).isNotNull()
  }

  @Test
  fun parseString_invalid_classAsTrigger() {
    val string = classString
    val exception = Assert.assertThrows(SummitAST.ParseException::class.java) {
      SummitAST.parseAndTranslate(
        string,
        type = CompilationType.TRIGGER
      )
    };
    assertThat(exception).isNotNull()
  }
}
