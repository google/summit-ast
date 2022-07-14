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

import com.google.common.flogger.FluentLogger
import com.google.summit.ast.CompilationUnit
import com.google.summit.ast.Identifier
import com.google.summit.ast.Node
import com.google.summit.ast.SourceLocation
import com.google.summit.ast.TypeRef
import com.google.summit.ast.declaration.ClassDeclaration
import com.google.summit.ast.declaration.Declaration
import com.google.summit.ast.declaration.EnumDeclaration
import com.google.summit.ast.declaration.FieldDeclaration
import com.google.summit.ast.declaration.InterfaceDeclaration
import com.google.summit.ast.declaration.MethodDeclaration
import com.google.summit.ast.declaration.ParameterDeclaration
import com.google.summit.ast.declaration.PropertyDeclaration
import com.google.summit.ast.declaration.TriggerDeclaration
import com.google.summit.ast.declaration.TypeDeclaration
import com.google.summit.ast.declaration.VariableDeclaration
import com.google.summit.ast.expression.ArrayExpression
import com.google.summit.ast.expression.AssignExpression
import com.google.summit.ast.expression.BinaryExpression
import com.google.summit.ast.expression.CallExpression
import com.google.summit.ast.expression.CastExpression
import com.google.summit.ast.expression.Expression
import com.google.summit.ast.expression.FieldExpression
import com.google.summit.ast.expression.LiteralExpression
import com.google.summit.ast.expression.NewExpression
import com.google.summit.ast.expression.SoqlOrSoslExpression
import com.google.summit.ast.expression.SuperExpression
import com.google.summit.ast.expression.TernaryExpression
import com.google.summit.ast.expression.ThisExpression
import com.google.summit.ast.expression.TypeRefExpression
import com.google.summit.ast.expression.UnaryExpression
import com.google.summit.ast.expression.VariableExpression
import com.google.summit.ast.initializer.ConstructorInitializer
import com.google.summit.ast.initializer.Initializer
import com.google.summit.ast.initializer.MapInitializer
import com.google.summit.ast.initializer.SizedArrayInitializer
import com.google.summit.ast.initializer.ValuesInitializer
import com.google.summit.ast.modifier.AnnotationModifier
import com.google.summit.ast.modifier.ElementArgument
import com.google.summit.ast.modifier.ElementValue
import com.google.summit.ast.modifier.KeywordModifier
import com.google.summit.ast.modifier.Modifier
import com.google.summit.ast.statement.BreakStatement
import com.google.summit.ast.statement.CompoundStatement
import com.google.summit.ast.statement.ContinueStatement
import com.google.summit.ast.statement.DmlStatement
import com.google.summit.ast.statement.DoWhileLoopStatement
import com.google.summit.ast.statement.EnhancedForLoopStatement
import com.google.summit.ast.statement.ExpressionStatement
import com.google.summit.ast.statement.ForLoopStatement
import com.google.summit.ast.statement.IfStatement
import com.google.summit.ast.statement.ReturnStatement
import com.google.summit.ast.statement.RunAsStatement
import com.google.summit.ast.statement.Statement
import com.google.summit.ast.statement.SwitchStatement
import com.google.summit.ast.statement.ThrowStatement
import com.google.summit.ast.statement.TryStatement
import com.google.summit.ast.statement.VariableDeclarationStatement
import com.google.summit.ast.statement.WhileLoopStatement
import com.nawforce.apexparser.ApexParser
import com.nawforce.apexparser.ApexParserBaseVisitor
import kotlin.math.min
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.TokenStream
import org.antlr.v4.runtime.tree.SyntaxTree

/**
 * Translates an Apex parse tree into an abstract syntax tree (AST).
 *
 * NOTE: This is a WIP, and only a subset of the Apex language is represented or translated.
 *
 * The translation is accomplished with a single-pass depth-first traversal of the parse tree, using
 * the ANTLR-generated [com.nawforce.apexparser.ApexParserVisitor] interface. Each translated
 * grammar rule returns the corresponding AST object.
 *
 * @property file path (or other descriptor) that is being translated
 * @property tokens the parsed token stream to compute source locations
 */
class Translate(val file: String, private val tokens: TokenStream) : ApexParserBaseVisitor<Any>() {

  /**
   * Returns the translated CompilationUnit and its AST.
   *
   * @param tree the top-level grammar rule
   * @throws TranslationException on unexpected errors
   */
  @Throws(TranslationException::class)
  fun translate(tree: ParserRuleContext): CompilationUnit {
    logger.atInfo().log("Translating %s", file)
    val prevNodeCount = Node.totalCount
    val cu =
      when (tree) {
        is ApexParser.CompilationUnitContext -> visitCompilationUnit(tree)
        is ApexParser.TriggerUnitContext -> visitTriggerUnit(tree)
        else -> throw IllegalArgumentException("Unexpected parse tree")
      }
    val newNodeCount = Node.totalCount - prevNodeCount
    val reachableNodeCount = Node.setNodeParents(cu)
    if (reachableNodeCount != newNodeCount) {
      throw TranslationException(
        tree,
        """|Number of created nodes $newNodeCount should match number of
           |reachable nodes $reachableNodeCount"""
          .trimMargin()
          .replace("\n", " ")
      )
    }
    logger.atInfo().log("Translated AST successfully. Created %d nodes.", newNodeCount)
    return cu
  }

  /** Exception for any unexpected translation errors. */
  class TranslationException(val ctx: ParserRuleContext, msg: String, cause: Throwable? = null) :
    Exception(msg, cause)

  /** Translates the 'id' grammar rule and returns an AST [Identifier]. */
  override fun visitId(ctx: ApexParser.IdContext): Identifier =
    Identifier(ctx.text, toSourceLocation(ctx))

  /** Translates the 'anyId' grammar rule and returns an AST [Identifier]. */
  override fun visitAnyId(ctx: ApexParser.AnyIdContext): Identifier =
    Identifier(ctx.text, toSourceLocation(ctx))

  /** Translates the 'compilationUnit' grammar rule and returns an AST [CompilationUnit]. */
  override fun visitCompilationUnit(ctx: ApexParser.CompilationUnitContext): CompilationUnit =
    CompilationUnit(visitTypeDeclaration(ctx.typeDeclaration()), file, toSourceLocation(ctx))

  /** Translates the 'triggerUnit' grammar rule and returns an AST [CompilationUnit]. */
  override fun visitTriggerUnit(ctx: ApexParser.TriggerUnitContext): CompilationUnit {
    if (ctx.id().size != 2) {
      throw TranslationException(ctx, "TriggerUnit rule should include 2 identifiers")
    }

    val loc = toSourceLocation(ctx)
    return CompilationUnit(
      TriggerDeclaration(
        id = visitId(ctx.id().get(0)),
        target = visitId(ctx.id().get(1)),
        cases = ctx.triggerCase().map { visitTriggerCase(it) },
        body = visitBlock(ctx.block()),
        loc = loc
      ),
      file,
      loc
    )
  }

  /**
   * Translates the 'triggerCase' grammar rule and returns an AST [TriggerDeclaration.TriggerCase].
   */
  override fun visitTriggerCase(
    ctx: ApexParser.TriggerCaseContext
  ): TriggerDeclaration.TriggerCase {
    throwUnlessExactlyOneNotNull(ruleBeingChecked = ctx, ctx.BEFORE(), ctx.AFTER())
    throwUnlessExactlyOneNotNull(
      ruleBeingChecked = ctx,
      ctx.INSERT(),
      ctx.UPDATE(),
      ctx.DELETE(),
      ctx.UNDELETE()
    )
    return if (ctx.BEFORE() != null) {
      when {
        ctx.INSERT() != null -> TriggerDeclaration.TriggerCase.TRIGGER_BEFORE_INSERT
        ctx.UPDATE() != null -> TriggerDeclaration.TriggerCase.TRIGGER_BEFORE_UPDATE
        ctx.DELETE() != null -> TriggerDeclaration.TriggerCase.TRIGGER_BEFORE_DELETE
        ctx.UNDELETE() != null -> TriggerDeclaration.TriggerCase.TRIGGER_BEFORE_UNDELETE
        else -> throw TranslationException(ctx, "Unreachable case reached")
      }
    } else {
      when {
        ctx.INSERT() != null -> TriggerDeclaration.TriggerCase.TRIGGER_AFTER_INSERT
        ctx.UPDATE() != null -> TriggerDeclaration.TriggerCase.TRIGGER_AFTER_UPDATE
        ctx.DELETE() != null -> TriggerDeclaration.TriggerCase.TRIGGER_AFTER_DELETE
        ctx.UNDELETE() != null -> TriggerDeclaration.TriggerCase.TRIGGER_AFTER_UNDELETE
        else -> throw TranslationException(ctx, "Unreachable case reached")
      }
    }
  }

  /** Translates the 'classDeclaration' grammar rule and returns an AST [ClassDeclaration]. */
  override fun visitClassDeclaration(ctx: ApexParser.ClassDeclarationContext): ClassDeclaration =
    ClassDeclaration(
      visitId(ctx.id()),
      translateOptional(ctx.typeRef(), ::visitTypeRef),
      translateOptionalList(ctx.typeList(), ::visitTypeList),
      visitClassBody(ctx.classBody()),
      toSourceLocation(ctx)
    )

  /**
   * Translates the 'interfaceDeclaration' grammar rule and returns an AST [InterfaceDeclaration].
   */
  override fun visitInterfaceDeclaration(
    ctx: ApexParser.InterfaceDeclarationContext
  ): InterfaceDeclaration =
    InterfaceDeclaration(
      visitId(ctx.id()),
      translateOptionalList(ctx.typeList(), ::visitTypeList),
      visitInterfaceBody(ctx.interfaceBody()),
      toSourceLocation(ctx)
    )

  /** Translates the 'enumDeclaration' grammar rule and returns an AST [EnumDeclaration]. */
  override fun visitEnumDeclaration(ctx: ApexParser.EnumDeclarationContext): EnumDeclaration =
    EnumDeclaration(visitId(ctx.id()), toSourceLocation(ctx))

  /** Translates the 'typeDeclaration' grammar rule and returns an AST [TypeDeclaration]. */
  override fun visitTypeDeclaration(ctx: ApexParser.TypeDeclarationContext): TypeDeclaration {
    // Check mutual exclusivity in grammar
    throwUnlessExactlyOneNotNull(
      ruleBeingChecked = ctx,
      ctx.classDeclaration(),
      ctx.interfaceDeclaration(),
      ctx.enumDeclaration()
    )

    val decl =
      when {
        ctx.classDeclaration() != null -> visitClassDeclaration(ctx.classDeclaration())
        ctx.interfaceDeclaration() != null -> visitInterfaceDeclaration(ctx.interfaceDeclaration())
        ctx.enumDeclaration() != null -> visitEnumDeclaration(ctx.enumDeclaration())
        else -> throw TranslationException(ctx, "Unreachable case reached")
      }
    decl.modifiers = ctx.modifier().map { visitModifier(it) }
    return decl
  }

  /** Translates the 'modifier' grammar rule and returns an AST [Modifier]. */
  override fun visitModifier(ctx: ApexParser.ModifierContext): Modifier {
    return when {
      ctx.annotation() != null -> visitAnnotation(ctx.annotation())
      else ->
        KeywordModifier(
          KeywordModifier.keywordFromString(ctx.text)
            ?: throw TranslationException(ctx, "Unexpected modifier keyword: " + ctx.text),
          toSourceLocation(ctx)
        )
    }
  }

  /** Translates the 'annotation' grammar rule and returns an AST [AnnotationModifier]. */
  override fun visitAnnotation(ctx: ApexParser.AnnotationContext): AnnotationModifier {
    if (ctx.elementValuePairs() != null && ctx.elementValue() != null) {
      throw TranslationException(ctx, "At most one value should be present")
    }

    return AnnotationModifier(
      visitQualifiedName(ctx.qualifiedName()),
      args =
        when {
          // Named arguments
          ctx.elementValuePairs() != null -> visitElementValuePairs(ctx.elementValuePairs())
          // Single unnamed argument
          ctx.elementValue() != null -> listOf(unnamedArgument(ctx.elementValue()))
          // No arguments
          else -> emptyList()
        },
      toSourceLocation(ctx)
    )
  }

  /** Translates the 'elementValuePairs' grammar rule and returns an AST [ElementArgument] list. */
  override fun visitElementValuePairs(
    ctx: ApexParser.ElementValuePairsContext
  ): List<ElementArgument> = ctx.elementValuePair().map { visitElementValuePair(it) }

  /** Translates the 'elementValuePair' grammar rule and returns an AST [ElementArgument]. */
  override fun visitElementValuePair(ctx: ApexParser.ElementValuePairContext): ElementArgument =
    ElementArgument.named(
      visitId(ctx.id()),
      visitElementValue(ctx.elementValue()),
      toSourceLocation(ctx)
    )

  /** Translates an 'elementValue' into an unnamed argument. */
  private fun unnamedArgument(ctx: ApexParser.ElementValueContext): ElementArgument =
    ElementArgument.unnamed(visitElementValue(ctx), toSourceLocation(ctx))

  /** Translates the 'elementValue' grammar rule and returns an AST [ElementValue]. */
  override fun visitElementValue(ctx: ApexParser.ElementValueContext): ElementValue {
    throwUnlessExactlyOneNotNull(
      ruleBeingChecked = ctx,
      ctx.expression(),
      ctx.annotation(),
      ctx.elementValueArrayInitializer()
    )

    val loc = toSourceLocation(ctx)

    return when {
      ctx.expression() != null ->
        ElementValue.ExpressionValue(visitExpression(ctx.expression()), loc)
      ctx.annotation() != null ->
        ElementValue.AnnotationValue(visitAnnotation(ctx.annotation()), loc)
      ctx.elementValueArrayInitializer() != null ->
        ElementValue.ArrayValue(
          visitElementValueArrayInitializer(ctx.elementValueArrayInitializer()),
          loc
        )
      else -> throw TranslationException(ctx, "Unreachable case reached")
    }
  }

  /**
   * Translates the 'elementValueArrayInitializer' grammar rule and returns an AST [ElementValue]
   * list.
   */
  override fun visitElementValueArrayInitializer(
    ctx: ApexParser.ElementValueArrayInitializerContext
  ): List<ElementValue> = ctx.elementValue().map { visitElementValue(it) }

  /** Translates the 'qualifiedName' grammar rule and returns an AST [Identifier]. */
  override fun visitQualifiedName(ctx: ApexParser.QualifiedNameContext): Identifier =
    Identifier(ctx.text, toSourceLocation(ctx))

  /** Translates the 'typeRef' grammar rule and returns an AST [TypeRef]. */
  override fun visitTypeRef(ctx: ApexParser.TypeRefContext): TypeRef {
    val arrayNesting = ctx.arraySubscripts()?.LBRACK()?.size ?: 0 // count left brackets
    return TypeRef(ctx.typeName().map { visitTypeName(it) }, arrayNesting)
  }

  /** Translates the 'typeName' grammar rule and returns an AST [TypeRef.Component]. */
  override fun visitTypeName(ctx: ApexParser.TypeNameContext): TypeRef.Component {
    val matchedTerminal = ctx.LIST() ?: ctx.SET() ?: ctx.MAP()
    val id =
      when {
        ctx.id() != null -> visitId(ctx.id())
        matchedTerminal != null ->
          Identifier(matchedTerminal.text, toSourceLocation(matchedTerminal))
        else -> throw TranslationException(ctx, "Unexpected type name")
      }
    return TypeRef.Component(
      id,
      translateOptionalList(ctx.typeArguments(), ::visitTypeArguments),
    )
  }

  /** Translates the 'typeArguments' grammar rule and returns an AST [TypeRef] list. */
  override fun visitTypeArguments(ctx: ApexParser.TypeArgumentsContext): List<TypeRef> =
    visitTypeList(ctx.typeList())

  /** Translates the 'typeList' grammar rule and returns an AST [TypeRef] list. */
  override fun visitTypeList(ctx: ApexParser.TypeListContext): List<TypeRef> =
    ctx.typeRef().map { visitTypeRef(it) }

  /** Translates the 'classBody' grammar rule and returns an AST [Declaration] list. */
  override fun visitClassBody(ctx: ApexParser.ClassBodyContext): List<Declaration> =
    ctx.classBodyDeclaration().flatMap { visitClassBodyDeclaration(it) }

  /**
   * Translates the 'classBodyDeclaration' grammar rule and returns an optional AST [Declaration].
   */
  override fun visitClassBodyDeclaration(
    ctx: ApexParser.ClassBodyDeclarationContext
  ): List<Declaration> {
    // Check mutual exclusivity in grammar
    throwUnlessExactlyOneNotNull(
      ruleBeingChecked = ctx,
      ctx.memberDeclaration(),
      ctx.block(),
      ctx.SEMI()
    )

    return when {
      ctx.memberDeclaration() != null -> {
        val members = visitMemberDeclaration(ctx.memberDeclaration())
        members.forEach { member -> member.modifiers = ctx.modifier().map { visitModifier(it) } }
        members
      }
      ctx.block() != null -> {
        // This is an anonymous initializer. Define a method with a special name.
        listOf(
          MethodDeclaration(
            Identifier(MethodDeclaration.ANONYMOUS_INITIALIZER_NAME, SourceLocation.UNKNOWN),
            returnType = TypeRef.createVoid(),
            parameterDeclarations = emptyList(),
            visitBlock(ctx.block()),
            toSourceLocation(ctx)
          )
        )
      }
      ctx.SEMI() != null -> emptyList()
      else -> throw TranslationException(ctx, "Unreachable case reached")
    }
  }

  /** Translates the 'memberDeclaration' grammar rule and returns an AST [Declaration] list. */
  override fun visitMemberDeclaration(ctx: ApexParser.MemberDeclarationContext): List<Declaration> {
    // Check mutual exclusivity in grammar
    throwUnlessExactlyOneNotNull(
      ruleBeingChecked = ctx,
      ctx.methodDeclaration(),
      ctx.fieldDeclaration(),
      ctx.constructorDeclaration(),
      ctx.classDeclaration(),
      ctx.interfaceDeclaration(),
      ctx.enumDeclaration(),
      ctx.propertyDeclaration()
    )

    return when {
      ctx.methodDeclaration() != null -> listOf(visitMethodDeclaration(ctx.methodDeclaration()))
      ctx.fieldDeclaration() != null -> visitFieldDeclaration(ctx.fieldDeclaration())
      ctx.constructorDeclaration() != null ->
        listOf(visitConstructorDeclaration(ctx.constructorDeclaration()))
      ctx.classDeclaration() != null -> listOf(visitClassDeclaration(ctx.classDeclaration()))
      ctx.interfaceDeclaration() != null ->
        listOf(visitInterfaceDeclaration(ctx.interfaceDeclaration()))
      ctx.enumDeclaration() != null -> listOf(visitEnumDeclaration(ctx.enumDeclaration()))
      ctx.propertyDeclaration() != null ->
        listOf(visitPropertyDeclaration(ctx.propertyDeclaration()))
      else -> throw TranslationException(ctx, "Unreachable case reached")
    }
  }

  /**
   * Translates the 'fieldDeclaration' grammar rule and returns an AST [FieldDeclaration] list.
   *
   * This grammar rule can include multiple fields declarations separated by commas.
   *
   * The source locations of a FieldDeclaration may include declarators that declare other fields.
   * Consider for example:
   * ```
   *    `Type variableA = 1, variableB = 2;`
   * ```
   * The (contiguous) source range for variableB should include the type, which will then span the
   * unrelated variableA.
   */
  override fun visitFieldDeclaration(
    ctx: ApexParser.FieldDeclarationContext
  ): List<FieldDeclaration> {
    val loc = toSourceLocation(ctx) // declared outside of `map` so it gets reused
    return visitVariableDeclarators(ctx.variableDeclarators()).map {
      FieldDeclaration(it.id, visitTypeRef(ctx.typeRef()), it.initializer, loc)
    }
  }

  /** Translates the 'methodDeclaration' grammar rule and returns an AST [MethodDeclaration]. */
  override fun visitMethodDeclaration(ctx: ApexParser.MethodDeclarationContext): MethodDeclaration {
    return MethodDeclaration(
      visitId(ctx.id()),
      returnType =
        when {
          ctx.VOID() != null -> TypeRef.createVoid()
          ctx.typeRef() != null -> visitTypeRef(ctx.typeRef())
          else -> throw TranslationException(ctx, "Method should return void or a type")
        },
      visitFormalParameters(ctx.formalParameters()),
      body =
        when {
          ctx.block() != null -> visitBlock(ctx.block())
          ctx.SEMI() != null -> null
          else -> throw TranslationException(ctx, "Unreachable case reached")
        },
      toSourceLocation(ctx)
    )
  }

  /**
   * Translates the 'constructorDeclaration' grammar rule and returns an AST [MethodDeclaration].
   */
  override fun visitConstructorDeclaration(
    ctx: ApexParser.ConstructorDeclarationContext
  ): MethodDeclaration =
    MethodDeclaration(
      visitQualifiedName(ctx.qualifiedName()),
      // Although the `new` expression returns a value, the constructor initializer does not.
      TypeRef.createVoid(),
      visitFormalParameters(ctx.formalParameters()),
      visitBlock(ctx.block()),
      toSourceLocation(ctx)
    )

  /** Translates the 'interfaceBody' grammar rule and returns an AST [MethodDeclaration] list. */
  override fun visitInterfaceBody(ctx: ApexParser.InterfaceBodyContext): List<MethodDeclaration> =
    ctx.interfaceMethodDeclaration().map { visitInterfaceMethodDeclaration(it) }

  /**
   * Translates the 'interfaceMethodDeclaration' grammar rule and returns an AST [MethodDeclaration]
   * .
   */
  override fun visitInterfaceMethodDeclaration(
    ctx: ApexParser.InterfaceMethodDeclarationContext
  ): MethodDeclaration {
    return MethodDeclaration(
      visitId(ctx.id()),
      when {
        ctx.VOID() != null -> TypeRef.createVoid()
        ctx.typeRef() != null -> visitTypeRef(ctx.typeRef())
        else -> throw TranslationException(ctx, "Method should return void or a type")
      },
      visitFormalParameters(ctx.formalParameters()),
      body = null,
      toSourceLocation(ctx)
    )
  }

  /** Translates the 'propertyDeclaration' grammar rule and returns an AST [PropertyDeclaration]. */
  override fun visitPropertyDeclaration(
    ctx: ApexParser.PropertyDeclarationContext
  ): PropertyDeclaration {
    val accessors = ctx.propertyBlock().map { visitPropertyBlock(it) }

    val getters = accessors.filterIsInstance<PropertyGetter>()
    if (getters.size > 1) {
      throw TranslationException(ctx, "There should only be zero or one getter")
    }
    val getterDecl =
      getters.singleOrNull()?.let { it.toMethodDeclaration(visitTypeRef(ctx.typeRef())) }

    val setters = accessors.filterIsInstance<PropertySetter>()
    if (setters.size > 1) {
      throw TranslationException(ctx, "There should only be zero or one setter")
    }
    val setterDecl =
      setters.singleOrNull()?.let { it.toMethodDeclaration(visitTypeRef(ctx.typeRef())) }

    return PropertyDeclaration(
      visitId(ctx.id()),
      visitTypeRef(ctx.typeRef()),
      getterDecl,
      setterDecl,
      toSourceLocation(ctx)
    )
  }

  /**
   * This class summarizes a property accessor, either a getter or a setter.
   *
   * It is intended to be converted into a [MethodDeclaration] when we visit the production rule
   * containing the property's type.
   */
  sealed class PropertyAccessor {
    abstract val id: Identifier
    abstract val body: CompoundStatement?
    var modifiers: List<Modifier> = emptyList()
    var loc: SourceLocation = SourceLocation.UNKNOWN

    /** Converts this accessor to a [MethodDeclaration] given the property type. */
    abstract fun toMethodDeclaration(type: TypeRef): MethodDeclaration
  }

  /** This class summarizes a property getter. */
  data class PropertyGetter(override val id: Identifier, override val body: CompoundStatement?) :
    PropertyAccessor() {

    override fun toMethodDeclaration(type: TypeRef): MethodDeclaration {
      return MethodDeclaration(id, type, emptyList(), body, loc).also { it.modifiers = modifiers }
    }
  }

  /** This class summarizes a property setter. */
  data class PropertySetter(override val id: Identifier, override val body: CompoundStatement?) :
    PropertyAccessor() {

    override fun toMethodDeclaration(type: TypeRef): MethodDeclaration {
      return MethodDeclaration(
          id,
          TypeRef.createVoid(),
          listOf(
            ParameterDeclaration(
              Identifier("value", SourceLocation.UNKNOWN),
              type,
              SourceLocation.UNKNOWN
            )
          ),
          body,
          loc
        )
        .also { it.modifiers = modifiers }
    }
  }

  /** Translates the 'propertyBlock' grammar rule and returns an AST [MethodDeclaration]. */
  override fun visitPropertyBlock(ctx: ApexParser.PropertyBlockContext): PropertyAccessor {
    throwUnlessExactlyOneNotNull(ruleBeingChecked = ctx, ctx.getter(), ctx.setter())
    return when {
      ctx.getter() != null -> visitGetter(ctx.getter())
      ctx.setter() != null -> visitSetter(ctx.setter())
      else -> throw TranslationException(ctx, "Unreachable case reached")
    }.apply {
      this.modifiers = ctx.modifier().map { visitModifier(it) }
      this.loc = toSourceLocation(ctx)
    }
  }

  /** Translates the 'getter' grammar rule and returns a [PropertyGetter]. */
  override fun visitGetter(ctx: ApexParser.GetterContext): PropertyGetter =
    PropertyGetter(
      Identifier(ctx.GET().text, toSourceLocation(ctx.GET())),
      translateOptional(ctx.block(), ::visitBlock)
    )

  /** Translates the 'setter' grammar rule and returns a [PropertySetter]. */
  override fun visitSetter(ctx: ApexParser.SetterContext): PropertySetter =
    PropertySetter(
      Identifier(ctx.SET().text, toSourceLocation(ctx.SET())),
      translateOptional(ctx.block(), ::visitBlock)
    )

  /**
   * Translates the 'formalParameters' grammar rule and returns an AST [ParameterDeclaration] list.
   */
  override fun visitFormalParameters(
    ctx: ApexParser.FormalParametersContext
  ): List<ParameterDeclaration> =
    translateOptionalList(ctx.formalParameterList(), ::visitFormalParameterList)

  /**
   * Translates the 'formalParametersList' grammar rule and returns an AST [ParameterDeclaration]
   * list.
   */
  override fun visitFormalParameterList(
    ctx: ApexParser.FormalParameterListContext
  ): List<ParameterDeclaration> = ctx.formalParameter().map { visitFormalParameter(it) }

  /** Translates the 'formalParameter' grammar rule and returns an AST [ParameterDeclaration]. */
  override fun visitFormalParameter(ctx: ApexParser.FormalParameterContext): ParameterDeclaration {
    val decl =
      ParameterDeclaration(visitId(ctx.id()), visitTypeRef(ctx.typeRef()), toSourceLocation(ctx))
    decl.modifiers = ctx.modifier().map { visitModifier(it) }
    return decl
  }

  /**
   * A declarator is the variable name and an optional initializer expression.
   *
   * This is temporary translation data and does not persist in the resulting AST.
   */
  data class VariableDeclarator(val id: Identifier, val initializer: Expression?)

  /** Translates the 'variableDeclarators' grammar rule and returns a VariableDeclarator list. */
  override fun visitVariableDeclarators(
    ctx: ApexParser.VariableDeclaratorsContext
  ): List<VariableDeclarator> = ctx.variableDeclarator().map { visitVariableDeclarator(it) }

  /** Translates the 'variableDeclarator' grammar rule and returns a VariableDeclarator. */
  override fun visitVariableDeclarator(
    ctx: ApexParser.VariableDeclaratorContext
  ): VariableDeclarator =
    VariableDeclarator(visitId(ctx.id()), translateOptional(ctx.expression(), ::visitExpression))

  /** Translates the 'literal' grammar rule and returns an AST [LiteralExpression]. */
  override fun visitLiteral(ctx: ApexParser.LiteralContext): LiteralExpression {
    throwUnlessExactlyOneNotNull(
      ruleBeingChecked = ctx,
      ctx.IntegerLiteral(),
      ctx.LongLiteral(),
      ctx.NumberLiteral(),
      ctx.StringLiteral(),
      ctx.BooleanLiteral(),
      ctx.NULL()
    )

    val loc = toSourceLocation(ctx)
    val text = ctx.text
    try {
      return when {
        ctx.IntegerLiteral() != null -> LiteralExpression.IntegerVal(text.toInt(), loc)
        ctx.LongLiteral() != null ->
          // Trim required trailing 'L' character
          LiteralExpression.LongVal(text.replace("[lL]$".toRegex(), "").toLong(), loc)
        ctx.NumberLiteral() != null ->
          // Trim optional trailing 'D' character
          LiteralExpression.DoubleVal(text.replace("[dD]$".toRegex(), "").toDouble(), loc)
        // Trim single quotes
        ctx.StringLiteral() != null ->
          LiteralExpression.StringVal(text.removeSurrounding("'", "'"), loc)
        ctx.BooleanLiteral() != null -> LiteralExpression.BooleanVal(text.toBoolean(), loc)
        ctx.NULL() != null -> LiteralExpression.NullVal(loc)
        else -> throw TranslationException(ctx, "Unreachable case reached")
      }
    } catch (e: NumberFormatException) {
      throw TranslationException(ctx, "Literal '$text' format is incorrect", cause = e)
    }
  }

  /** Throws on the 'dotMethodCall' grammar rule. */
  override fun visitDotMethodCall(ctx: ApexParser.DotMethodCallContext) {
    throw TranslationException(
      ctx,
      "The 'dotMethodCall' rule should be handled as part of 'dotExpression''"
    )
  }

  /** Throws on the 'forControl' grammar rule. */
  override fun visitForControl(ctx: ApexParser.ForControlContext) {
    throw TranslationException(
      ctx,
      "The 'forControl' rule should be handled as part of 'forStatement''"
    )
  }

  /** Throws on the 'forInit' grammar rule. */
  override fun visitForInit(ctx: ApexParser.ForInitContext) {
    throw TranslationException(
      ctx,
      "The 'forInit' rule should be handled as part of 'forStatement'"
    )
  }

  /** Throws on the 'forUpdate' grammar rule. */
  override fun visitForUpdate(ctx: ApexParser.ForUpdateContext) {
    throw TranslationException(
      ctx,
      "The 'forUpdate' rule should be handled as part of 'forStatement'"
    )
  }

  /** Throws on the 'enhancedForControl' grammar rule. */
  override fun visitEnhancedForControl(ctx: ApexParser.EnhancedForControlContext) {
    throw TranslationException(
      ctx,
      "The 'enhancedForControl' rule should be handled as part of 'forStatement'"
    )
  }

  /** Translates the 'expressionList' grammar rule and returns an AST [Expression] list. */
  override fun visitExpressionList(ctx: ApexParser.ExpressionListContext): List<Expression> =
    ctx.expression().map { visitExpression(it) }

  /** Translates the 'block' grammar rule and returns an AST [CompoundStatement]. */
  override fun visitBlock(ctx: ApexParser.BlockContext): CompoundStatement =
    CompoundStatement(
      ctx.statement().map { visitStatement(it) },
      CompoundStatement.Scoping.SCOPE_BOUNDARY,
      toSourceLocation(ctx)
    )

  /** Translates the 'finallyBlock' grammar rule and returns an AST [CompoundStatement]. */
  override fun visitFinallyBlock(ctx: ApexParser.FinallyBlockContext): CompoundStatement =
    visitBlock(ctx.block())

  /** Translates the 'catchClause' grammar rule and returns an AST [TryStatement.CatchBlock]. */
  override fun visitCatchClause(ctx: ApexParser.CatchClauseContext): TryStatement.CatchBlock {
    val exceptionVariableName = visitId(ctx.id())
    return TryStatement.CatchBlock(
      VariableDeclaration(
        exceptionVariableName,
        TypeRef.createFromQualifiedName(ctx.qualifiedName().getText()),
        modifiers = ctx.modifier().map { visitModifier(it) },
        initializer = null,
        exceptionVariableName.getSourceLocation() // approximate as a location of name
      ),
      visitBlock(ctx.block()),
      toSourceLocation(ctx)
    )
  }

  /**
   * Translates the 'localVariableDeclaration' grammar rule and returns an AST [VariableDeclaration]
   * list.
   */
  override fun visitLocalVariableDeclaration(
    ctx: ApexParser.LocalVariableDeclarationContext
  ): List<VariableDeclaration> {
    val loc = toSourceLocation(ctx) // reuse object multiple times
    return visitVariableDeclarators(ctx.variableDeclarators()).map { declarator ->
      VariableDeclaration(
        declarator.id,
        visitTypeRef(ctx.typeRef()),
        ctx.modifier().map { visitModifier(it) },
        declarator.initializer,
        loc
      )
    }
  }

  /** Translates the 'whenControl' grammar rule and returns an AST [SwitchStatement.When]. */
  override fun visitWhenControl(ctx: ApexParser.WhenControlContext): SwitchStatement.When {
    val whenValue = ctx.whenValue()
    throwUnlessExactlyOneNotNull(
      ruleBeingChecked = whenValue,
      whenValue.ELSE(),
      whenValue.id().firstOrNull(),
      whenValue.whenLiteral().firstOrNull()
    )

    val statement = visitBlock(ctx.block())
    return when {
      whenValue.ELSE() != null -> SwitchStatement.WhenElse(statement)
      whenValue.id().isNotEmpty() -> whenValue.toWhenType(statement)
      whenValue.whenLiteral().isNotEmpty() ->
        SwitchStatement.WhenValue(whenValue.whenLiteral().map { visitWhenLiteral(it) }, statement)
      else -> throw TranslationException(ctx, "Unreachable case reached")
    }
  }

  /**
   * Translates the type case of the 'whenValue' grammar rule into an AST [SwitchStatement.WhenType]
   * .
   */
  private fun ApexParser.WhenValueContext.toWhenType(
    statement: Statement
  ): SwitchStatement.WhenType {
    if (this.id().size != 2) {
      throw TranslationException(this, "When type clauses should have 2 identifiers")
    }
    val typeIdentifier = visitId(this.id().first())
    val nameIdentifier = visitId(this.id().last())

    val variableDecl =
      VariableDeclaration(
        nameIdentifier,
        TypeRef.createFromUnqualifiedName(typeIdentifier),
        emptyList(),
        initializer = null,
        toSourceLocation(this)
      )
    return SwitchStatement.WhenType(
      TypeRef.createFromUnqualifiedName(typeIdentifier.copy()),
      variableDecl,
      statement
    )
  }

  /** Throws on the 'whenValue' grammar rule. */
  override fun visitWhenValue(ctx: ApexParser.WhenValueContext) {
    throw TranslationException(ctx, "The WhenValue rule should be handled as part of WhenControl")
  }

  /** Translates the 'whenLiteral' grammar rule into an AST [Expression]. */
  override fun visitWhenLiteral(ctx: ApexParser.WhenLiteralContext): Expression {
    throwUnlessExactlyOneNotNull(
      ruleBeingChecked = ctx,
      ctx.IntegerLiteral(),
      ctx.LongLiteral(),
      ctx.StringLiteral(),
      ctx.NULL(),
      ctx.id()
    )

    val loc = toSourceLocation(ctx)
    val text = ctx.text
    return try {
      when {
        ctx.IntegerLiteral() != null -> {
          val signMultiplier =
            if (ctx.SUB() != null) {
              -1
            } else {
              1
            }
          LiteralExpression.IntegerVal(text.toInt() * signMultiplier, loc)
        }
        ctx.LongLiteral() != null ->
          LiteralExpression.LongVal(text.replace("[lL]$".toRegex(), "").toLong(), loc)
        ctx.StringLiteral() != null -> LiteralExpression.StringVal(text, loc)
        ctx.NULL() != null -> LiteralExpression.NullVal(loc)
        // An identifier is an enum value
        ctx.id() != null -> VariableExpression(visitId(ctx.id()), loc)
        else -> throw TranslationException(ctx, "Unreachable case reached")
      }
    } catch (e: NumberFormatException) {
      throw TranslationException(ctx, "Literal '$text' format is incorrect", cause = e)
    }
  }

  /** Translates the 'creator' grammar rule and returns an AST [Initializer]. */
  override fun visitCreator(ctx: ApexParser.CreatorContext): Initializer {
    throwUnlessExactlyOneNotNull(
      ruleBeingChecked = ctx,
      ctx.noRest(),
      ctx.classCreatorRest(),
      ctx.arrayCreatorRest(),
      ctx.mapCreatorRest(),
      ctx.setCreatorRest()
    )

    val type =
      TypeRef(
        ctx.createdName().idCreatedNamePair().map { visitIdCreatedNamePair(it) },
        arrayNesting =
          if (ctx.arrayCreatorRest() != null) {
            1
          } else {
            0
          }
      )
    val loc = toSourceLocation(ctx)

    return when {
      // One corner case is that this can also apply to Map objects
      ctx.noRest() != null -> ValuesInitializer(emptyList(), type, loc)
      ctx.classCreatorRest() != null ->
        ConstructorInitializer(visitArguments(ctx.classCreatorRest().arguments()), type, loc)
      ctx.arrayCreatorRest() != null && ctx.arrayCreatorRest().expression() != null ->
        SizedArrayInitializer(visitExpression(ctx.arrayCreatorRest().expression()), type, loc)
      ctx.arrayCreatorRest() != null && ctx.arrayCreatorRest().arrayInitializer() != null ->
        ValuesInitializer(
          visitArrayInitializer(ctx.arrayCreatorRest().arrayInitializer()),
          type,
          loc
        )
      ctx.mapCreatorRest() != null ->
        MapInitializer(
          ctx.mapCreatorRest().mapCreatorRestPair().map { visitMapCreatorRestPair(it) },
          type,
          loc
        )
      ctx.setCreatorRest() != null ->
        ValuesInitializer(ctx.setCreatorRest().expression().map { visitExpression(it) }, type, loc)
      else -> throw TranslationException(ctx, "Unreachable case reached")
    }
  }

  /** Translates the 'arrayInitializer' grammar rule and returns an AST [Expression] list. */
  override fun visitArrayInitializer(ctx: ApexParser.ArrayInitializerContext): List<Expression> =
    ctx.expression().map { visitExpression(it) }

  /** Translates the 'mapCreatorRestPair' grammar rule and returns an AST [Expression] pair. */
  override fun visitMapCreatorRestPair(
    ctx: ApexParser.MapCreatorRestPairContext
  ): Pair<Expression, Expression> =
    Pair(visitExpression(ctx.expression(0)), visitExpression(ctx.expression(1)))

  /** Translates the 'idCreatedNamePair' grammar rule and returns an AST [TypeRef.Component]. */
  override fun visitIdCreatedNamePair(ctx: ApexParser.IdCreatedNamePairContext): TypeRef.Component =
    TypeRef.Component(
      visitAnyId(ctx.anyId()),
      translateOptionalList(ctx.typeList(), ::visitTypeList)
    )

  /** Throws on visit to the 'createdName' grammar rule. */
  override fun visitCreatedName(ctx: ApexParser.CreatedNameContext) {
    throw TranslationException(ctx, "The 'createdName' rule should be handled as part of 'creator'")
  }

  /** Throws on visiting the 'classCreatorRest' grammar rule. */
  override fun visitClassCreatorRest(ctx: ApexParser.ClassCreatorRestContext) {
    throw TranslationException(
      ctx,
      "The 'classCreatorRest' rule should be handled as part of 'creator'"
    )
  }

  /** Translates the 'arguments' grammar rule and returns an AST [Expression] list. */
  override fun visitArguments(ctx: ApexParser.ArgumentsContext): List<Expression> =
    translateOptionalList(ctx.expressionList(), ::visitExpressionList)

  /** Translates the 'methodCall' grammar rule and returns an AST [CallExpression]. */
  override fun visitMethodCall(ctx: ApexParser.MethodCallContext): CallExpression {
    throwUnlessExactlyOneNotNull(ruleBeingChecked = ctx, ctx.THIS(), ctx.SUPER(), ctx.id())
    return CallExpression(
      receiver = null,
      when {
        // Encode these keywords as identifiers rather than creating a special type.
        // They are unambiguous because they are protected keywords.
        ctx.THIS() != null -> Identifier(ctx.THIS().getText(), toSourceLocation(ctx.THIS()))
        ctx.SUPER() != null -> Identifier(ctx.SUPER().getText(), toSourceLocation(ctx.SUPER()))
        ctx.id() != null -> visitId(ctx.id())
        else -> throw TranslationException(ctx, "Unreachable case reached")
      },
      translateOptionalList(ctx.expressionList(), ::visitExpressionList),
      isSafe = false,
      toSourceLocation(ctx)
    )
  }

  // BEGIN EXPRESSION

  fun visitExpression(ctx: ApexParser.ExpressionContext): Expression =
    visit(ctx) as? Expression
      ?: throw TranslationException(ctx, "Expression translation should return an Expression")

  /** Translates the 'parExpression' grammar rule and returns an AST [Expression]. */
  override fun visitParExpression(ctx: ApexParser.ParExpressionContext): Expression =
    visitExpression(ctx.expression())

  /** Translates the 'expression#primaryExpression' grammar rule and returns an AST [Expression]. */
  override fun visitPrimaryExpression(ctx: ApexParser.PrimaryExpressionContext): Expression =
    visitPrimary(ctx.primary())

  /** Translates the 'expression#arth1Expression' grammar rule and returns an AST [Expression]. */
  override fun visitArth1Expression(ctx: ApexParser.Arth1ExpressionContext): Expression {
    val matchedTerminals = listOfNotNull(ctx.MUL(), ctx.DIV(), ctx.MOD())
    // Check mutual exclusivity
    if (matchedTerminals.size != 1) {
      throw TranslationException(ctx, "${ctx.text} should match exactly one terminal")
    }

    return BinaryExpression(
      visitExpression(ctx.expression().first()),
      opString = matchedTerminals.first().text,
      visitExpression(ctx.expression().last()),
      toSourceLocation(ctx)
    )
  }

  /** Translates the 'expression#dotExpression' grammar rule and returns an AST [Expression]. */
  override fun visitDotExpression(ctx: ApexParser.DotExpressionContext): Expression {
    val obj = visitExpression(ctx.expression())
    val loc = toSourceLocation(ctx)
    if (ctx.dotMethodCall() != null) {
      return CallExpression(
        receiver = obj,
        visitAnyId(ctx.dotMethodCall().anyId()),
        args = translateOptionalList(ctx.dotMethodCall().expressionList(), ::visitExpressionList),
        isSafe = (ctx.QUESTIONDOT() != null),
        loc = loc
      )
    } else if (ctx.anyId() != null) {
      return FieldExpression(
        obj,
        visitAnyId(ctx.anyId()),
        isSafe = (ctx.QUESTIONDOT() != null),
        loc
      )
    }
    throw TranslationException(ctx, "Either the 'anyId' or 'dotMethodCall' rule should match")
  }

  /** Translates the 'expression#bitOrExpression' grammar rule and returns an AST [Expression]. */
  override fun visitBitOrExpression(ctx: ApexParser.BitOrExpressionContext): Expression =
    BinaryExpression(
      visitExpression(ctx.expression().first()),
      BinaryExpression.Operator.BITWISE_OR,
      visitExpression(ctx.expression().last()),
      toSourceLocation(ctx)
    )
  /** Translates the 'expression#arrayExpression' grammar rule and returns an AST [Expression]. */
  override fun visitArrayExpression(ctx: ApexParser.ArrayExpressionContext): Expression =
    ArrayExpression(
      array = visitExpression(ctx.expression().first()),
      index = visitExpression(ctx.expression().last()),
      toSourceLocation(ctx)
    )

  /** Translates the 'expression#newExpression' grammar rule and returns an AST [Expression]. */
  override fun visitNewExpression(ctx: ApexParser.NewExpressionContext): Expression =
    NewExpression(visitCreator(ctx.creator()), toSourceLocation(ctx))

  /** Translates the 'expression#assignExpression' grammar rule and returns an AST [Expression]. */
  override fun visitAssignExpression(ctx: ApexParser.AssignExpressionContext): Expression {
    throwUnlessExactlyOneNotNull(
      ruleBeingChecked = ctx,
      ctx.ASSIGN(),
      ctx.ADD_ASSIGN(),
      ctx.SUB_ASSIGN(),
      ctx.MUL_ASSIGN(),
      ctx.DIV_ASSIGN(),
      ctx.AND_ASSIGN(),
      ctx.OR_ASSIGN(),
      ctx.XOR_ASSIGN(),
      ctx.RSHIFT_ASSIGN(),
      ctx.URSHIFT_ASSIGN(),
      ctx.LSHIFT_ASSIGN(),
      ctx.MOD_ASSIGN(),
    )

    return AssignExpression(
      visitExpression(ctx.expression().first()),
      visitExpression(ctx.expression().last()),
      when {
        ctx.ASSIGN() != null -> null // plain assignment
        ctx.ADD_ASSIGN() != null -> BinaryExpression.Operator.ADDITION
        ctx.SUB_ASSIGN() != null -> BinaryExpression.Operator.SUBTRACTION
        ctx.MUL_ASSIGN() != null -> BinaryExpression.Operator.MULTIPLICATION
        ctx.DIV_ASSIGN() != null -> BinaryExpression.Operator.DIVISION
        ctx.AND_ASSIGN() != null -> BinaryExpression.Operator.BITWISE_AND
        ctx.OR_ASSIGN() != null -> BinaryExpression.Operator.BITWISE_OR
        ctx.XOR_ASSIGN() != null -> BinaryExpression.Operator.BITWISE_XOR
        ctx.RSHIFT_ASSIGN() != null -> BinaryExpression.Operator.RIGHT_SHIFT_SIGNED
        ctx.URSHIFT_ASSIGN() != null -> BinaryExpression.Operator.RIGHT_SHIFT_UNSIGNED
        ctx.LSHIFT_ASSIGN() != null -> BinaryExpression.Operator.LEFT_SHIFT
        ctx.MOD_ASSIGN() != null -> BinaryExpression.Operator.MODULO
        else -> throw TranslationException(ctx, "Unreachable case reached")
      },
      toSourceLocation(ctx)
    )
  }

  /**
   * Translates the 'expression#methodCallExpression' grammar rule and returns an AST [Expression].
   */
  override fun visitMethodCallExpression(ctx: ApexParser.MethodCallExpressionContext): Expression =
    visitMethodCall(ctx.methodCall())

  /** Translates the 'expression#bitNotExpression' grammar rule and returns an AST [Expression]. */
  override fun visitBitNotExpression(ctx: ApexParser.BitNotExpressionContext): Expression =
    BinaryExpression(
      visitExpression(ctx.expression().first()),
      BinaryExpression.Operator.BITWISE_XOR,
      visitExpression(ctx.expression().last()),
      toSourceLocation(ctx)
    )

  /** Translates the 'expression#arth2Expression' grammar rule and returns an AST [Expression]. */
  override fun visitArth2Expression(ctx: ApexParser.Arth2ExpressionContext): Expression {
    val matchedTerminals = listOfNotNull(ctx.ADD(), ctx.SUB())
    // Check mutual exclusivity
    if (matchedTerminals.size != 1) {
      throw TranslationException(ctx, "${ctx.text} should match exactly one terminal")
    }

    return BinaryExpression(
      visitExpression(ctx.expression().first()),
      opString = matchedTerminals.first().text,
      visitExpression(ctx.expression().last()),
      toSourceLocation(ctx)
    )
  }

  /** Translates the 'expression#logAndExpression' grammar rule and returns an AST [Expression]. */
  override fun visitLogAndExpression(ctx: ApexParser.LogAndExpressionContext): Expression =
    BinaryExpression(
      visitExpression(ctx.expression().first()),
      BinaryExpression.Operator.LOGICAL_AND,
      visitExpression(ctx.expression().last()),
      toSourceLocation(ctx)
    )

  /** Translates the 'expression#castExpression' grammar rule and returns an AST [Expression]. */
  override fun visitCastExpression(ctx: ApexParser.CastExpressionContext): Expression =
    CastExpression(
      visitExpression(ctx.expression()),
      visitTypeRef(ctx.typeRef()),
      toSourceLocation(ctx)
    )

  /** Translates the 'expression#bitAndExpression' grammar rule and returns an AST [Expression]. */
  override fun visitBitAndExpression(ctx: ApexParser.BitAndExpressionContext): Expression =
    BinaryExpression(
      visitExpression(ctx.expression().first()),
      BinaryExpression.Operator.BITWISE_AND,
      visitExpression(ctx.expression().last()),
      toSourceLocation(ctx)
    )

  /** Translates the 'expression#cmpExpression' grammar rule and returns an AST [Expression]. */
  override fun visitCmpExpression(ctx: ApexParser.CmpExpressionContext): Expression =
    BinaryExpression(
      visitExpression(ctx.expression().first()),
      opString = listOfNotNull(ctx.GT(), ctx.LT(), ctx.ASSIGN()).joinToString("") { it.text },
      visitExpression(ctx.expression().last()),
      toSourceLocation(ctx)
    )

  /** Translates the 'expression#bitExpression' grammar rule and returns an AST [Expression]. */
  override fun visitBitExpression(ctx: ApexParser.BitExpressionContext): Expression =
    BinaryExpression(
      visitExpression(ctx.expression().first()),
      opString = (ctx.GT() + ctx.LT()).joinToString("") { it.text },
      visitExpression(ctx.expression().last()),
      toSourceLocation(ctx)
    )

  /** Translates the 'expression#logOrExpression' grammar rule and returns an AST [Expression]. */
  override fun visitLogOrExpression(ctx: ApexParser.LogOrExpressionContext): Expression =
    BinaryExpression(
      visitExpression(ctx.expression().first()),
      BinaryExpression.Operator.LOGICAL_OR,
      visitExpression(ctx.expression().last()),
      toSourceLocation(ctx)
    )

  /** Translates the 'expression#condExpression' grammar rule and returns an AST [Expression]. */
  override fun visitCondExpression(ctx: ApexParser.CondExpressionContext): Expression =
    TernaryExpression(
      visitExpression(ctx.expression().get(0)),
      visitExpression(ctx.expression().get(1)),
      visitExpression(ctx.expression().get(2)),
      toSourceLocation(ctx)
    )

  /**
   * Translates the 'expression#equalityExpression' grammar rule and returns an AST [Expression].
   */
  override fun visitEqualityExpression(ctx: ApexParser.EqualityExpressionContext): Expression {
    val matchedTerminals =
      listOfNotNull(
        ctx.TRIPLEEQUAL(),
        ctx.TRIPLENOTEQUAL(),
        ctx.EQUAL(),
        ctx.NOTEQUAL(),
        ctx.LESSANDGREATER(),
      )
    // Check mutual exclusivity
    if (matchedTerminals.size != 1) {
      throw TranslationException(ctx, "${ctx.text} should match exactly one terminal")
    }

    return BinaryExpression(
      visitExpression(ctx.expression().first()),
      opString = matchedTerminals.first().text,
      visitExpression(ctx.expression().last()),
      toSourceLocation(ctx)
    )
  }

  /** Translates the 'expression#postOpExpression' grammar rule and returns an AST [Expression]. */
  override fun visitPostOpExpression(ctx: ApexParser.PostOpExpressionContext): Expression {
    throwUnlessExactlyOneNotNull(ruleBeingChecked = ctx, ctx.INC(), ctx.DEC())
    val op =
      when {
        ctx.INC() != null -> UnaryExpression.Operator.POST_INCREMENT
        ctx.DEC() != null -> UnaryExpression.Operator.POST_DECREMENT
        else -> throw TranslationException(ctx, "Unreachable case reached")
      }
    return UnaryExpression(visitExpression(ctx.expression()), op, toSourceLocation(ctx))
  }

  /** Translates the 'expression#negExpression' grammar rule and returns an AST [Expression]. */
  override fun visitNegExpression(ctx: ApexParser.NegExpressionContext): Expression {
    throwUnlessExactlyOneNotNull(ruleBeingChecked = ctx, ctx.TILDE(), ctx.BANG())
    val op =
      when {
        ctx.TILDE() != null -> UnaryExpression.Operator.BITWISE_NOT
        ctx.BANG() != null -> UnaryExpression.Operator.LOGICAL_COMPLEMENT
        else -> throw TranslationException(ctx, "Unreachable case reached")
      }
    return UnaryExpression(visitExpression(ctx.expression()), op, toSourceLocation(ctx))
  }

  /** Translates the 'expression#preOpExpression' grammar rule and returns an AST [Expression]. */
  override fun visitPreOpExpression(ctx: ApexParser.PreOpExpressionContext): Expression {
    throwUnlessExactlyOneNotNull(ruleBeingChecked = ctx, ctx.ADD(), ctx.SUB(), ctx.INC(), ctx.DEC())
    val op =
      when {
        ctx.ADD() != null -> UnaryExpression.Operator.PLUS
        ctx.SUB() != null -> UnaryExpression.Operator.NEGATION
        ctx.INC() != null -> UnaryExpression.Operator.PRE_INCREMENT
        ctx.DEC() != null -> UnaryExpression.Operator.PRE_DECREMENT
        else -> throw TranslationException(ctx, "Unreachable case reached")
      }
    return UnaryExpression(visitExpression(ctx.expression()), op, toSourceLocation(ctx))
  }

  /**
   * Translates the 'expression#subExpression' grammar rule and returns an AST [Expression].
   *
   * This is production rule for pairs of parenthesis. These determine operator associativity and
   * dictate the parse tree structure, but that structure has the needed information. We don't
   * bother to explicitly represent these in the AST.
   */
  override fun visitSubExpression(ctx: ApexParser.SubExpressionContext): Expression =
    visitExpression(ctx.expression())

  /**
   * Translates the 'expression#instanceOfExpression' grammar rule and returns an AST [Expression].
   */
  override fun visitInstanceOfExpression(ctx: ApexParser.InstanceOfExpressionContext): Expression =
    BinaryExpression(
      visitExpression(ctx.expression()),
      BinaryExpression.Operator.INSTANCEOF,
      TypeRefExpression(visitTypeRef(ctx.typeRef()), toSourceLocation(ctx.typeRef())),
      toSourceLocation(ctx)
    )

  // END EXPRESSION

  // BEGIN PRIMARY

  /** Translates the 'primary' grammar rule and returns an AST [Expression]. */
  fun visitPrimary(ctx: ApexParser.PrimaryContext): Expression =
    visit(ctx) as? Expression
      ?: throw TranslationException(ctx, "Primary translation should return an Expression")

  /** Translates the 'primary#thisPrimary' grammar rule and returns an AST [Expression]. */
  override fun visitThisPrimary(ctx: ApexParser.ThisPrimaryContext): Expression =
    ThisExpression(toSourceLocation(ctx))

  /** Translates the 'primary#superPrimary' grammar rule and returns an AST [Expression]. */
  override fun visitSuperPrimary(ctx: ApexParser.SuperPrimaryContext): Expression =
    SuperExpression(toSourceLocation(ctx))

  /** Translates the 'primary#literalPrimary' grammar rule and returns an AST [Expression]. */
  override fun visitLiteralPrimary(ctx: ApexParser.LiteralPrimaryContext): Expression =
    visitLiteral(ctx.literal())

  /**
   * Translates the 'primary#typeRefPrimary' grammar rule and returns an AST [Expression].
   *
   * An example is: `CustomObject.class`
   */
  override fun visitTypeRefPrimary(ctx: ApexParser.TypeRefPrimaryContext): Expression =
    TypeRefExpression(visitTypeRef(ctx.typeRef()), toSourceLocation(ctx))

  /** Translates the 'primary#idPrimary' grammar rule and returns an AST [Expression]. */
  override fun visitIdPrimary(ctx: ApexParser.IdPrimaryContext): Expression {
    val id = visitId(ctx.id())
    return VariableExpression(id, id.getSourceLocation())
  }

  /** Translates the 'primary#soqlPrimary' grammar rule and returns an AST [Expression]. */
  override fun visitSoqlPrimary(ctx: ApexParser.SoqlPrimaryContext): Expression =
    // TODO(b/216117963): Translate the body of the SOQL query.
    SoqlOrSoslExpression(toSourceLocation(ctx))

  /** Translates the 'primary#soslPrimary' grammar rule and returns an AST [Expression]. */
  override fun visitSoslPrimary(ctx: ApexParser.SoslPrimaryContext): Expression =
    // TODO(b/216117963): Translate the body of the SOSL query.
    SoqlOrSoslExpression(toSourceLocation(ctx))

  // END PRIMARY

  // BEGIN STATEMENT

  /** Translates the 'statement' grammar rule and returns an AST [Statement]. */
  override fun visitStatement(ctx: ApexParser.StatementContext): Statement {
    val statementRules =
      listOfNotNull(
        ctx.block(),
        ctx.ifStatement(),
        ctx.switchStatement(),
        ctx.forStatement(),
        ctx.whileStatement(),
        ctx.doWhileStatement(),
        ctx.tryStatement(),
        ctx.returnStatement(),
        ctx.throwStatement(),
        ctx.breakStatement(),
        ctx.continueStatement(),
        ctx.insertStatement(),
        ctx.updateStatement(),
        ctx.deleteStatement(),
        ctx.undeleteStatement(),
        ctx.upsertStatement(),
        ctx.mergeStatement(),
        ctx.runAsStatement(),
        ctx.localVariableDeclarationStatement(),
        ctx.expressionStatement(),
      )

    // Check mutual exclusivity
    if (statementRules.size != 1) {
      throw TranslationException(
        ctx,
        "Exactly one rule should match, but matched: ${statementRules.map{it::class}}"
      )
    }
    val matched = statementRules.first()

    return visit(matched) as? Statement
      ?: throw TranslationException(matched, "Statement translation should return a Statement")
  }

  /** Translates the 'ifStatement' grammar rule and returns an AST [Statement]. */
  override fun visitIfStatement(ctx: ApexParser.IfStatementContext): Statement =
    IfStatement(
      visitParExpression(ctx.parExpression()),
      visitStatement(ctx.statement()[0]),
      translateOptional(ctx.statement().getOrNull(1), ::visitStatement),
      toSourceLocation(ctx)
    )

  /** Translates the 'switchStatement' grammar rule and returns an AST [Statement]. */
  override fun visitSwitchStatement(ctx: ApexParser.SwitchStatementContext): Statement =
    SwitchStatement(
      visitExpression(ctx.expression()),
      ctx.whenControl().map { visitWhenControl(it) },
      toSourceLocation(ctx)
    )

  /** Translates the 'forStatement' grammar rule and returns an AST [Statement]. */
  override fun visitForStatement(ctx: ApexParser.ForStatementContext): Statement {
    val loc = toSourceLocation(ctx)
    val forControl = ctx.forControl()
    val bodyStatement = visitStatement(ctx.statement())

    val enhancedForControl = forControl.enhancedForControl()
    if (enhancedForControl != null) {
      // Enhanced for loop
      val elementDeclaration =
        VariableDeclaration(
          visitId(enhancedForControl.id()),
          visitTypeRef(enhancedForControl.typeRef()),
          modifiers = emptyList(),
          initializer = null,
          toSourceLocation(enhancedForControl)
        )
      return EnhancedForLoopStatement(
        elementDeclaration,
        collection = visitExpression(enhancedForControl.expression()),
        bodyStatement,
        loc
      )
    } else {
      // Traditional for loop
      val declarations =
        translateOptionalList(
          forControl.forInit()?.localVariableDeclaration(),
          ::visitLocalVariableDeclaration
        )
      val initializations =
        translateOptionalList(forControl.forInit()?.expressionList(), ::visitExpressionList)
      val updates =
        translateOptionalList(forControl.forUpdate()?.expressionList(), ::visitExpressionList)
      return ForLoopStatement(
        declarations,
        initializations,
        updates,
        condition = translateOptional(forControl.expression(), ::visitExpression),
        bodyStatement,
        loc
      )
    }
  }

  /** Translates the 'whileStatement' grammar rule and returns an AST [Statement]. */
  override fun visitWhileStatement(ctx: ApexParser.WhileStatementContext): Statement =
    WhileLoopStatement(
      condition = visitParExpression(ctx.parExpression()),
      body = visitStatement(ctx.statement()),
      toSourceLocation(ctx)
    )

  /** Translates the 'doWhileStatement' grammar rule and returns an AST [Statement]. */
  override fun visitDoWhileStatement(ctx: ApexParser.DoWhileStatementContext): Statement =
    DoWhileLoopStatement(
      condition = visitParExpression(ctx.parExpression()),
      body = visitStatement(ctx.statement()),
      toSourceLocation(ctx)
    )

  /** Translates the 'tryStatement' grammar rule and returns an AST [Statement]. */
  override fun visitTryStatement(ctx: ApexParser.TryStatementContext): Statement =
    TryStatement(
      ctx.catchClause()?.map { visitCatchClause(it) } ?: emptyList(),
      visitBlock(ctx.block()),
      translateOptional(ctx.finallyBlock(), ::visitFinallyBlock),
      toSourceLocation(ctx)
    )

  /** Translates the 'returnStatement' grammar rule and returns an AST [Statement]. */
  override fun visitReturnStatement(ctx: ApexParser.ReturnStatementContext): Statement =
    ReturnStatement(translateOptional(ctx.expression(), ::visitExpression), toSourceLocation(ctx))

  /** Translates the 'throwStatement' grammar rule and returns an AST [Statement]. */
  override fun visitThrowStatement(ctx: ApexParser.ThrowStatementContext): Statement =
    ThrowStatement(visitExpression(ctx.expression()), toSourceLocation(ctx))

  /** Translates the 'breakStatement' grammar rule and returns an AST [Statement]. */
  override fun visitBreakStatement(ctx: ApexParser.BreakStatementContext): Statement =
    BreakStatement(toSourceLocation(ctx))

  /** Translates the 'continueStatement' grammar rule and returns an AST [Statement]. */
  override fun visitContinueStatement(ctx: ApexParser.ContinueStatementContext): Statement =
    ContinueStatement(toSourceLocation(ctx))

  /** Translates the 'insertStatement' grammar rule and returns an AST [Statement]. */
  override fun visitInsertStatement(ctx: ApexParser.InsertStatementContext): Statement =
    DmlStatement.Insert(visitExpression(ctx.expression()), toSourceLocation(ctx))

  /** Translates the 'updateStatement' grammar rule and returns an AST [Statement]. */
  override fun visitUpdateStatement(ctx: ApexParser.UpdateStatementContext): Statement =
    DmlStatement.Update(visitExpression(ctx.expression()), toSourceLocation(ctx))

  /** Translates the 'deleteStatement' grammar rule and returns an AST [Statement]. */
  override fun visitDeleteStatement(ctx: ApexParser.DeleteStatementContext): Statement =
    DmlStatement.Delete(visitExpression(ctx.expression()), toSourceLocation(ctx))

  /** Translates the 'undeleteStatement' grammar rule and returns an AST [Statement]. */
  override fun visitUndeleteStatement(ctx: ApexParser.UndeleteStatementContext): Statement =
    DmlStatement.Undelete(visitExpression(ctx.expression()), toSourceLocation(ctx))

  /** Translates the 'upsertStatement' grammar rule and returns an AST [Statement]. */
  override fun visitUpsertStatement(ctx: ApexParser.UpsertStatementContext): Statement =
    DmlStatement.Upsert(
      visitExpression(ctx.expression()),
      translateOptional(ctx.qualifiedName(), ::visitQualifiedName),
      toSourceLocation(ctx)
    )

  /** Translates the 'mergeStatement' grammar rule and returns an AST [Statement]. */
  override fun visitMergeStatement(ctx: ApexParser.MergeStatementContext): Statement =
    DmlStatement.Merge(
      value = visitExpression(ctx.expression().first()),
      from = visitExpression(ctx.expression().last()),
      loc = toSourceLocation(ctx)
    )

  /** Translates the 'runAsStatement' grammar rule and returns an AST [Statement]. */
  override fun visitRunAsStatement(ctx: ApexParser.RunAsStatementContext): Statement =
    RunAsStatement(
      translateOptionalList(ctx.expressionList(), ::visitExpressionList),
      visitBlock(ctx.block()),
      toSourceLocation(ctx)
    )

  /**
   * Translates the 'localVariableDeclarationStatement' grammar rule and returns an AST [Statement].
   */
  override fun visitLocalVariableDeclarationStatement(
    ctx: ApexParser.LocalVariableDeclarationStatementContext
  ): Statement =
    VariableDeclarationStatement(
      visitLocalVariableDeclaration(ctx.localVariableDeclaration()),
      toSourceLocation(ctx)
    )

  /** Translates the 'expressionStatement' grammar rule and returns an AST [Statement]. */
  override fun visitExpressionStatement(ctx: ApexParser.ExpressionStatementContext): Statement =
    ExpressionStatement(visitExpression(ctx.expression()), toSourceLocation(ctx))

  // END STATEMENT

  /** Throws a [TranslationException] unless exactly one of [trees] is non-null. */
  private fun throwUnlessExactlyOneNotNull(
    ruleBeingChecked: ParserRuleContext,
    vararg trees: SyntaxTree?
  ) {
    if (trees.filterNotNull().size != 1) {
      throw TranslationException(ruleBeingChecked, "Mutual exclusion violation")
    }
  }

  /**
   * Translates a generic optional expansion into an optional AST node, via the provided visitor
   * method.
   *
   * @param ctx the optional rule expansion
   * @param visitMethod method to translate the non-optional rule into a non-optional AST node
   * @return an optional AST node that is null if [ctx] is null
   */
  private fun <S : ParserRuleContext, T : Node> translateOptional(
    ctx: S?,
    visitMethod: (S) -> T
  ): T? = if (ctx != null) visitMethod(ctx) else null

  /**
   * Translates a generic optional rule expansion into a (possibly empty) AST list, via the provided
   * visitor method.
   *
   * @param ctx the optional rule expansion
   * @param visitMethod method to translate the non-optional rule into an AST list
   * @return an AST list that is empty if [ctx] is null
   */
  private fun <S : ParserRuleContext, T : Node> translateOptionalList(
    ctx: S?,
    visitMethod: (S) -> List<T>
  ): List<T> = if (ctx != null) visitMethod(ctx) else emptyList()

  /** Gets a source location for any grammar rule. */
  private fun toSourceLocation(tree: SyntaxTree): SourceLocation {
    val interval = tree.sourceInterval
    // The end of the last token in the interval is the start of the next token--
    // except for the EOF token which has no next token.
    val endToken = min(interval.b + 1, tokens.size() - 1)
    return SourceLocation(
      tokens.get(interval.a).line,
      tokens.get(interval.a).charPositionInLine,
      tokens.get(endToken).line,
      tokens.get(endToken).charPositionInLine
    )
  }

  private companion object {
    val logger = FluentLogger.forEnclosingClass()
  }
}
