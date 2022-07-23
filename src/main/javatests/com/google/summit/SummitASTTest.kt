package com.google.summit

import com.google.common.truth.Truth.assertThat
import com.google.summit.SummitAST.CompilationType
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
    val cu = SummitAST.parseAndTranslate(string, type = CompilationType.TRIGGER)
    assertThat(cu).isNull()
  }
}
