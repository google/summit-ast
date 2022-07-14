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

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory
import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.declaration.Declaration
import com.google.summit.ast.declaration.TypeDeclaration
import com.google.summit.ast.expression.Expression
import com.google.summit.ast.initializer.Initializer
import com.google.summit.ast.modifier.ElementValue
import com.google.summit.ast.modifier.Modifier
import com.google.summit.ast.statement.Statement
import com.google.summit.ast.statement.SwitchStatement
import kotlin.reflect.KClass

/**
 * Serializer & deserializer for AST nodes. Serialization options can be provided via the
 * parameters.
 *
 * Deserializing JSON without source locations is unsupported and may result in undefined behavior.
 *
 * Usage:
 * ```
 * val cu: CompilationUnit = ...
 * val serializer = Serializer()
 * val serialized: String = serializer.serialize(cu)
 * val deserialized: CompilationUnit? = serializer.deserialize(CompilationUnit::class.java, serialized)
 * ```
 *
 * @param format whether to add indentation and formatting to the output
 * @param removeLocations whether to filter [source locations][SourceLocation] from serialization
 */
class Serializer(format: Boolean = false, removeLocations: Boolean = false) {
  private companion object {
    /** Recursively finds the subtypes of [baseType] that are known at compile time. */
    fun <T : Any> findSubtypes(baseType: KClass<T>): List<KClass<out T>> {

      fun find(cl: KClass<out T>): List<KClass<out T>> =
        listOf(cl) + cl.sealedSubclasses.flatMap { find(it) }

      return find(baseType).filter { it != baseType }
    }

    /**
     * The name of the JSON property that indicates the runtime type of a serialized polymorphic
     * object.
     */
    const val subtypeLabel = "@type"

    /**
     * Constructs a [RuntimeTypeAdapterFactory] with the given [baseType] and all of its subtypes.
     */
    fun <T : Node> polymorphicFactory(baseType: KClass<T>): RuntimeTypeAdapterFactory<T> {
      val subtypes = findSubtypes(baseType)

      val factory = RuntimeTypeAdapterFactory.of(baseType.java, subtypeLabel)
      subtypes.forEach { factory.registerSubtype(it.java) }

      return factory
    }

    /**
     * Every `abstract`/`sealed` [Node] subclass that is referenced in a field in any other [Node].
     * Used to register [RuntimeTypeAdapterFactory] instances.
     */
    val polymorphicNodeTypes =
      listOf(
        Modifier::class,
        Statement::class,
        Expression::class,
        Declaration::class,
        TypeDeclaration::class,
        Initializer::class,
        SwitchStatement.When::class,
        ElementValue::class
      )
  }

  private val gson: Gson

  init {
    val builder = GsonBuilder().disableHtmlEscaping()

    if (format) {
      builder.setPrettyPrinting()
    }

    if (removeLocations) {
      builder.addSerializationExclusionStrategy(
        object : ExclusionStrategy {
          override fun shouldSkipField(field: FieldAttributes): Boolean = false
          override fun shouldSkipClass(c: Class<*>): Boolean = c == SourceLocation::class.java
        }
      )
    }

    polymorphicNodeTypes.forEach { builder.registerTypeAdapterFactory(polymorphicFactory(it)) }

    gson = builder.create()
  }

  /** Serializes the given [node] into a JSON string. */
  fun serialize(node: Node): String {
    return gson.toJson(node)
  }

  /** Deserializes the given [JSON string][json] into a node of type [nodeType]. */
  fun <T : Node> deserialize(nodeType: Class<T>, json: String): T? {
    val node = gson.fromJson(json, nodeType)
    if (node != null) {
      Node.setNodeParents(node)
    }
    return node
  }
}
