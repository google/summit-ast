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

package com.google.summit.serialization

import com.google.common.truth.Truth.assertThat
import com.google.summit.ast.CompilationUnit
import com.google.summit.ast.statement.VariableDeclarationStatement
import com.google.summit.testing.TranslateHelpers
import java.io.File
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SerializationTest {
  private fun readTestFile(file: String): String =
    File("src/main/javatests/com/google/summit/testdata/$file").readText()

  @Test
  fun testSerialization_compilationUnit() {
    val ser = Serializer(format = true)
    val expectedJson = readTestFile("mixednodes.json").trimEnd()
    val testSrc = readTestFile("mixednodes.cls")
    val testTree = TranslateHelpers.parseAndTranslate(testSrc)

    val actualJson = ser.serialize(testTree)

    assertThat(actualJson).isEqualTo(expectedJson)
  }

  @Test
  fun testDeserialization_compilationUnit() {
    val ser = Serializer(format = true)
    val expectedSrc = readTestFile("mixednodes.cls")
    val expectedTree = TranslateHelpers.parseAndTranslate(expectedSrc)
    val testJson = readTestFile("mixednodes.json")

    val testTree = ser.deserialize(CompilationUnit::class.java, testJson)
    assertNotNull(testTree)

    val actualJson = ser.serialize(testTree)
    val expectedJson = ser.serialize(expectedTree)
    assertThat(actualJson).isEqualTo(expectedJson)
  }

  @Test
  fun testSerialization_variableDeclaration() {
    val ser = Serializer(format = true)
    val expectedJson = readTestFile("vardecl.json").trimEnd()
    val testSrc = readTestFile("vardecl.cls")
    val testTree = TranslateHelpers.parseAndTranslateStatement(testSrc)

    val actualJson = ser.serialize(testTree)

    assertThat(actualJson).isEqualTo(expectedJson)
  }

  @Test
  fun testRoundTrip_variableDeclaration() {
    val ser = Serializer(format = true)
    val expectedJson = readTestFile("vardecl.json").trimEnd()

    val testTree = ser.deserialize(VariableDeclarationStatement::class.java, expectedJson)
    assertNotNull(testTree)

    val actualJson = ser.serialize(testTree)
    assertThat(actualJson).isEqualTo(expectedJson)
  }
}
