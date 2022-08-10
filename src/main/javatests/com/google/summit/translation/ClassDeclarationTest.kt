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

package com.google.summit.translation

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.summit.ast.declaration.ClassDeclaration
import com.google.summit.ast.declaration.EnumDeclaration
import com.google.summit.ast.declaration.FieldDeclarationGroup
import com.google.summit.ast.declaration.InterfaceDeclaration
import com.google.summit.ast.declaration.MethodDeclaration
import com.google.summit.ast.declaration.PropertyDeclaration
import com.google.summit.ast.modifier.KeywordModifier
import com.google.summit.testing.TranslateHelpers
import kotlin.test.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ClassDeclarationTest {

  @Test
  fun class_translation_hasClassDeclaration() {
    val cu = TranslateHelpers.parseAndTranslate("class Test { }")

    assertThat(cu.typeDeclaration).isInstanceOf(ClassDeclaration::class.java)
    val classDecl = cu.typeDeclaration as ClassDeclaration
    assertWithMessage("Class should have no super class").that(classDecl.extendsType).isNull()
    assertWithMessage("Class should have no implemented interfaces")
      .that(classDecl.implementsTypes)
      .isEmpty()
  }

  @Test
  fun class_translation_includesInheritance() {
    val cu = TranslateHelpers.parseAndTranslate("class Test extends Base implements I1, I2 { }")
    val classDecl = cu.typeDeclaration as ClassDeclaration

    assertThat(classDecl.extendsType?.asCodeString()).isEqualTo("Base")
    assertThat(classDecl.implementsTypes.map { it.asCodeString() }).containsExactly("I1", "I2")
  }

  @Test
  fun innerTypes_have_enclosingType() {
    val enclosingClassDecl =
      TranslateHelpers.parseAndTranslate(
          """
    class EnclosingClass {
      class InnerClass { }
      interface InnerInterface { }
      enum InnerEnum { }
    }
    """
        )
        .typeDeclaration as ClassDeclaration

    assertThat(enclosingClassDecl.innerTypeDeclarations).hasSize(3)
    assertThat(enclosingClassDecl.getEnclosingType()).isNull()
    assertThat(enclosingClassDecl.qualifiedName).isEqualTo("EnclosingClass")

    val innerClassDecl =
      enclosingClassDecl.innerTypeDeclarations.filterIsInstance<ClassDeclaration>().first()
    assertThat(innerClassDecl.getEnclosingType()).isEqualTo(enclosingClassDecl)
    assertThat(innerClassDecl.qualifiedName).isEqualTo("EnclosingClass.InnerClass")

    val innerInterfaceDecl =
      enclosingClassDecl.innerTypeDeclarations.filterIsInstance<InterfaceDeclaration>().first()
    assertThat(innerInterfaceDecl.getEnclosingType()).isEqualTo(enclosingClassDecl)
    assertThat(innerInterfaceDecl.qualifiedName).isEqualTo("EnclosingClass.InnerInterface")

    val innerEnumDecl =
      enclosingClassDecl.innerTypeDeclarations.filterIsInstance<EnumDeclaration>().first()
    assertThat(innerEnumDecl.getEnclosingType()).isEqualTo(enclosingClassDecl)
    assertThat(innerEnumDecl.qualifiedName).isEqualTo("EnclosingClass.InnerEnum")
  }

  @Test
  fun fields_translate_asFieldDeclarations() {
    val input =
      """
        class Test {
          public String field = 'Hello';
        }
        """

    val fieldDeclGroup = TranslateHelpers.parseAndFindFirstNodeOfType<FieldDeclarationGroup>(input)

    assertNotNull(fieldDeclGroup)
    assertThat(fieldDeclGroup.declarations).hasSize(1)
    val fieldDecl = fieldDeclGroup.declarations.first()
    assertThat(fieldDecl.qualifiedName).isEqualTo("Test.field")
    assertThat(fieldDecl.modifiers).hasSize(1)
    assertThat(fieldDecl.hasKeyword(KeywordModifier.Keyword.PUBLIC)).isTrue()
    assertThat(fieldDecl.initializer).isNotNull()
    assertThat(fieldDecl.type.asCodeString()).isEqualTo("String")
  }

  @Test
  fun multipleFieldDeclarators_translate_toFieldDeclarationGroups() {
    val input =
      """
        class Test {
          public Int field1 = 1, field2 = 2;
          public Int field3 = 3;
        }
        """

    val classDecl = TranslateHelpers.parseAndFindFirstNodeOfType<ClassDeclaration>(input)

    assertNotNull(classDecl)
    assertThat(classDecl.fieldDeclarations).hasSize(2)

    val group1 = classDecl.fieldDeclarations.first()
    assertThat(group1.declarations).hasSize(2)

    val group2 = classDecl.fieldDeclarations.last()
    assertThat(group2.declarations).hasSize(1)
  }

  @Test
  fun anonymousInitialization_translates_asMethodNamedInit() {
    val input =
      """
        class Test {
          {
            print('init');
          }
          // second block
          {
            print('more init');
          }
          // static block
          static {
            print('more init');
          }
        }
        """
    val classDecl = TranslateHelpers.parseAndFindFirstNodeOfType<ClassDeclaration>(input)

    assertNotNull(classDecl)
    assertThat(classDecl.methodDeclarations).hasSize(3)
    for (methodDecl in classDecl.methodDeclarations) {
      assertThat(methodDecl.parameterDeclarations).hasSize(0)
      assertThat(methodDecl.returnType.isVoid()).isTrue()
      assertThat(methodDecl.isAnonymousInitializationCode()).isTrue()
      assertThat(methodDecl.id.asCodeString()).isEqualTo("_init")
    }
  }

  @Test
  fun automaticProperty_has_getterAndSetterWithoutBody() {
    val input =
      """
        class Test {
          public String property { get; set; }
        }
        """
    val classDecl = TranslateHelpers.parseAndFindFirstNodeOfType<ClassDeclaration>(input)

    assertNotNull(classDecl)
    val propDecl = classDecl.propertyDeclarations.singleOrNull()
    assertNotNull(propDecl)
    assertThat(propDecl.id.asCodeString()).isEqualTo("property")
    assertThat(propDecl.type.asCodeString()).isEqualTo("String")
    assertNotNull(propDecl.getter)
    assertWithMessage("Automattic getter should have no body").that(propDecl.getter?.body).isNull()
    assertNotNull(propDecl.setter)
    assertWithMessage("Automattic setter should have no body").that(propDecl.setter?.body).isNull()
  }

  @Test
  fun readOnlyProperty_has_nullSetter() {
    val input =
      """
        class Test {
          public String property { get; }
        }
        """
    val propDecl = TranslateHelpers.parseAndFindFirstNodeOfType<PropertyDeclaration>(input)

    assertNotNull(propDecl)
    assertWithMessage("Read-only property should have a null setter").that(propDecl.setter).isNull()
  }

  @Test
  fun definedGetter_has_body_and_correctTypes() {
    val input =
      """
        class Test {
          public String property {
            get { return 'hello'; }
          }
        }
        """
    val propDecl = TranslateHelpers.parseAndFindFirstNodeOfType<PropertyDeclaration>(input)

    assertNotNull(propDecl)
    assertNotNull(propDecl.getter)
    val getterMethodDecl = propDecl.getter
    assertNotNull(getterMethodDecl)
    assertWithMessage("A defined property getter should have a method body")
      .that(getterMethodDecl.body)
      .isNotNull()

    // Check parameter and return types
    assertThat(getterMethodDecl.parameterDeclarations).hasSize(0)
    assertThat(getterMethodDecl.returnType.asCodeString()).isEqualTo("String")
  }

  @Test
  fun definedSetter_has_body_and_correctTypes() {
    val input =
      """
        class Test {
          public String property {
            set { property = value; }
          }
        }
        """
    val propDecl = TranslateHelpers.parseAndFindFirstNodeOfType<PropertyDeclaration>(input)

    assertNotNull(propDecl)
    assertNotNull(propDecl.setter)
    val setterMethodDecl = propDecl.setter
    assertNotNull(setterMethodDecl)
    assertWithMessage("A defined property setter should have a method body")
      .that(setterMethodDecl.body)
      .isNotNull()

    // Check parameter and return types
    assertThat(setterMethodDecl.returnType.isVoid()).isTrue()
    assertThat(setterMethodDecl.parameterDeclarations).hasSize(1)
    val paramDecl = setterMethodDecl.parameterDeclarations.first()
    assertThat(paramDecl.type.asCodeString()).isEqualTo("String")
    assertThat(paramDecl.id.asCodeString()).isEqualTo("value")
  }

  @Test
  fun constructorMethod_returns_void() {
    val input = """
        class Test {
          Test(String x) { }
        }
        """
    val methodDecl = TranslateHelpers.parseAndFindFirstNodeOfType<MethodDeclaration>(input)

    assertNotNull(methodDecl)
    assertThat(methodDecl.id.asCodeString()).isEqualTo("Test")
    assertThat(methodDecl.parameterDeclarations).hasSize(1)
    assertThat(methodDecl.returnType.isVoid()).isTrue()
  }
}
