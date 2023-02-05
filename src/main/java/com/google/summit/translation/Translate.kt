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
import com.google.summit.ast.declaration.EnumDeclaration
import com.google.summit.ast.declaration.EnumValue
import com.google.summit.ast.declaration.FieldDeclaration
import com.google.summit.ast.declaration.FieldDeclarationGroup
import com.google.summit.ast.declaration.InterfaceDeclaration
import com.google.summit.ast.declaration.MethodDeclaration
import com.google.summit.ast.declaration.ParameterDeclaration
import com.google.summit.ast.declaration.PropertyDeclaration
import com.google.summit.ast.declaration.TriggerDeclaration
import com.google.summit.ast.declaration.TypeDeclaration
import com.google.summit.ast.declaration.VariableDeclaration
import com.google.summit.ast.declaration.VariableDeclarationGroup
import com.google.summit.ast.expression.ArrayExpression
import com.google.summit.ast.expression.AssignExpression
import com.google.summit.ast.expression.BinaryExpression
import com.google.summit.ast.expression.CallExpression
import com.google.summit.ast.expression.CastExpression
import com.google.summit.ast.expression.Expression
import com.google.summit.ast.expression.FieldExpression
import com.google.summit.ast.expression.LiteralExpression
import com.google.summit.ast.expression.NewExpression
import com.google.summit.ast.expression.SoqlOrSoslBinding
import com.google.summit.ast.expression.SoqlExpression
import com.google.summit.ast.expression.SoslExpression
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
import com.google.summit.ast.modifier.HasModifiers
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
import org.antlr.v4.runtime.misc.Interval
import org.antlr.v4.runtime.tree.ParseTree
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
    try {
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
      logger.atInfo().log("Translated %s successfully. Created %d nodes.", file, newNodeCount)
      return cu
    } catch (e: Exception) {
      logger.atInfo().log("Failed to translate %s.", file)
      throw e
    }
  }

  /** Exception for any unexpected translation errors. */
  class TranslationException(val tree: ParseTree, msg: String, cause: Throwable? = null) :
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
    matchExactlyOne(ruleBeingChecked = ctx, ctx.BEFORE(), ctx.AFTER())
    matchExactlyOne(
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
    EnumDeclaration(
      visitId(ctx.id()),
      translateOptionalList(ctx.enumConstants(), ::visitEnumConstants),
      toSourceLocation(ctx)
    )

  /** Translates the 'enumConstants' grammar rule and returns a list of AST [EnumValue]s. */
  override fun visitEnumConstants(ctx: ApexParser.EnumConstantsContext): List<EnumValue> =
    ctx.id().map { EnumValue(visitId(it)) }

  /** Translates the 'typeDeclaration' grammar rule and returns an AST [TypeDeclaration]. */
  override fun visitTypeDeclaration(ctx: ApexParser.TypeDeclarationContext): TypeDeclaration {
    matchExactlyOne(
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
      else -> toKeywordModifier(ctx)
    }
  }

  /** Creates a [KeywordModifier] from the text of a [ParseTree]. */
  private fun toKeywordModifier(ctx: ParseTree) =
    KeywordModifier(
      keyword = KeywordModifier.keywordFromString(ctx.text)
          ?: throw TranslationException(ctx, "Unexpected modifier keyword: " + ctx.text),
      loc = toSourceLocation(ctx)
    )

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
    matchExactlyOne(
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

  /** Translates the 'classBody' grammar rule and returns an AST [Node] list. */
  override fun visitClassBody(ctx: ApexParser.ClassBodyContext): List<Node> =
    ctx.classBodyDeclaration().mapNotNull { visitClassBodyDeclaration(it) }

  /** Translates the 'classBodyDeclaration' grammar rule and optionally returns an AST [Node]. */
  override fun visitClassBodyDeclaration(ctx: ApexParser.ClassBodyDeclarationContext): Node? {
    matchExactlyOne(ruleBeingChecked = ctx, ctx.memberDeclaration(), ctx.block(), ctx.SEMI())

    return when {
      ctx.memberDeclaration() != null -> {
        val member = visitMemberDeclaration(ctx.memberDeclaration())
        (member as HasModifiers).modifiers = ctx.modifier().map { visitModifier(it) }
        member
      }
      ctx.block() != null -> {
        // This is an anonymous initializer. Define a method with a special name.
        val methodDecl =
          MethodDeclaration(
            id = Identifier(MethodDeclaration.ANONYMOUS_INITIALIZER_NAME, SourceLocation.UNKNOWN),
            returnType = TypeRef.createVoid(),
            parameterDeclarations = emptyList(),
            body = visitBlock(ctx.block()),
            isConstructor = false,
            loc = toSourceLocation(ctx)
          )

        if (ctx.STATIC() != null) {
          methodDecl.modifiers = listOf(toKeywordModifier(ctx.STATIC()))
        }

        methodDecl
      }
      ctx.SEMI() != null -> null
      else -> throw TranslationException(ctx, "Unreachable case reached")
    }
  }

  /** Translates the 'memberDeclaration' grammar rule and returns an AST [Node]. */
  override fun visitMemberDeclaration(ctx: ApexParser.MemberDeclarationContext): Node {
    matchExactlyOne(
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
      ctx.methodDeclaration() != null -> visitMethodDeclaration(ctx.methodDeclaration())
      ctx.fieldDeclaration() != null -> visitFieldDeclaration(ctx.fieldDeclaration())
      ctx.constructorDeclaration() != null ->
        visitConstructorDeclaration(ctx.constructorDeclaration())
      ctx.classDeclaration() != null -> visitClassDeclaration(ctx.classDeclaration())
      ctx.interfaceDeclaration() != null -> visitInterfaceDeclaration(ctx.interfaceDeclaration())
      ctx.enumDeclaration() != null -> visitEnumDeclaration(ctx.enumDeclaration())
      ctx.propertyDeclaration() != null -> visitPropertyDeclaration(ctx.propertyDeclaration())
      else -> throw TranslationException(ctx, "Unreachable case reached")
    }
  }

  /**
   * Translates the 'fieldDeclaration' grammar rule and returns an AST [FieldDeclarationGroup].
   *
   * This grammar rule can include multiple [FieldDeclaration]s separated by commas.
   */
  override fun visitFieldDeclaration(ctx: ApexParser.FieldDeclarationContext) =
    FieldDeclarationGroup(
      type = visitTypeRef(ctx.typeRef()),
      declarations =
        visitVariableDeclarators(ctx.variableDeclarators()).map {
          FieldDeclaration(it.id, it.initializer, it.loc)
        },
      loc = toSourceLocation(ctx)
    )

  /** Translates the 'methodDeclaration' grammar rule and returns an AST [MethodDeclaration]. */
  override fun visitMethodDeclaration(ctx: ApexParser.MethodDeclarationContext): MethodDeclaration {
    return MethodDeclaration(
      id = visitId(ctx.id()),
      returnType =
        when {
          ctx.VOID() != null -> TypeRef.createVoid()
          ctx.typeRef() != null -> visitTypeRef(ctx.typeRef())
          else -> throw TranslationException(ctx, "Method should return void or a type")
        },
      parameterDeclarations = visitFormalParameters(ctx.formalParameters()),
      body =
        when {
          ctx.block() != null -> visitBlock(ctx.block())
          ctx.SEMI() != null -> null
          else -> throw TranslationException(ctx, "Unreachable case reached")
        },
      isConstructor = false,
      loc = toSourceLocation(ctx)
    )
  }

  /**
   * Translates the 'constructorDeclaration' grammar rule and returns an AST [MethodDeclaration].
   */
  override fun visitConstructorDeclaration(
    ctx: ApexParser.ConstructorDeclarationContext
  ): MethodDeclaration =
    MethodDeclaration(
      id = visitQualifiedName(ctx.qualifiedName()),
      // Although the `new` expression returns a value, the constructor initializer does not.
      returnType = TypeRef.createVoid(),
      parameterDeclarations = visitFormalParameters(ctx.formalParameters()),
      body = visitBlock(ctx.block()),
      isConstructor = true,
      loc = toSourceLocation(ctx)
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
    val decl =
      MethodDeclaration(
        id = visitId(ctx.id()),
        returnType =
          when {
            ctx.VOID() != null -> TypeRef.createVoid()
            ctx.typeRef() != null -> visitTypeRef(ctx.typeRef())
            else -> throw TranslationException(ctx, "Method should return void or a type")
          },
        parameterDeclarations = visitFormalParameters(ctx.formalParameters()),
        body = null,
        isConstructor = false,
        loc = toSourceLocation(ctx)
      )
    decl.modifiers = ctx.modifier().map { visitModifier(it) }
    return decl
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
      return MethodDeclaration(
          id,
          returnType = type,
          parameterDeclarations = emptyList(),
          body,
          isConstructor = false,
          loc
        )
        .also { it.modifiers = modifiers }
    }
  }

  /** This class summarizes a property setter. */
  data class PropertySetter(override val id: Identifier, override val body: CompoundStatement?) :
    PropertyAccessor() {

    override fun toMethodDeclaration(type: TypeRef): MethodDeclaration {
      return MethodDeclaration(
          id,
          returnType = TypeRef.createVoid(),
          parameterDeclarations =
            listOf(
              ParameterDeclaration(
                Identifier("value", SourceLocation.UNKNOWN),
                type,
                SourceLocation.UNKNOWN
              )
            ),
          body,
          isConstructor = false,
          loc
        )
        .also { it.modifiers = modifiers }
    }
  }

  /** Translates the 'propertyBlock' grammar rule and returns an AST [MethodDeclaration]. */
  override fun visitPropertyBlock(ctx: ApexParser.PropertyBlockContext): PropertyAccessor {
    matchExactlyOne(ruleBeingChecked = ctx, ctx.getter(), ctx.setter())
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
  data class VariableDeclarator(
    val id: Identifier,
    val initializer: Expression?,
    val loc: SourceLocation
  )

  /** Translates the 'variableDeclarators' grammar rule and returns a [VariableDeclarator] list. */
  override fun visitVariableDeclarators(
    ctx: ApexParser.VariableDeclaratorsContext
  ): List<VariableDeclarator> = ctx.variableDeclarator().map { visitVariableDeclarator(it) }

  /** Translates the 'variableDeclarator' grammar rule and returns a [VariableDeclarator]. */
  override fun visitVariableDeclarator(
    ctx: ApexParser.VariableDeclaratorContext
  ): VariableDeclarator =
    VariableDeclarator(
      visitId(ctx.id()),
      translateOptional(ctx.expression(), ::visitExpression),
      toSourceLocation(ctx)
    )

  /** Translates the 'literal' grammar rule and returns an AST [LiteralExpression]. */
  override fun visitLiteral(ctx: ApexParser.LiteralContext): LiteralExpression {
    matchExactlyOne(
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
      VariableDeclarationGroup.of(
        id = exceptionVariableName,
        type = TypeRef.createFromQualifiedName(ctx.qualifiedName().getText()),
        modifiers = ctx.modifier().map { visitModifier(it) },
        initializer = null,
        loc = exceptionVariableName.getSourceLocation() // approximate as a location of name
      ),
      visitBlock(ctx.block()),
      toSourceLocation(ctx)
    )
  }

  /**
   * Translates the 'localVariableDeclaration' grammar rule and returns an AST [VariableDeclarationGroup]
   * list.
   *
   * The source location does not include the type or modifiers; it only includes the identifier and
   * the initializer. Including the type when there are multiple declarators would require the
   * source location ranges to overlap: this is undesirable for some use cases.
   */
  override fun visitLocalVariableDeclaration(
    ctx: ApexParser.LocalVariableDeclarationContext,
  ) =
    VariableDeclarationGroup(
      visitTypeRef(ctx.typeRef()),
      visitVariableDeclarators(ctx.variableDeclarators()).map { declarator ->
        VariableDeclaration(
          declarator.id,
          declarator.initializer,
          declarator.loc
        )
      },
      toSourceLocation(ctx)
    ).apply { modifiers = ctx.modifier().map { visitModifier(it) } }

  /** Translates the 'whenControl' grammar rule and returns an AST [SwitchStatement.When]. */
  override fun visitWhenControl(ctx: ApexParser.WhenControlContext): SwitchStatement.When {
    val whenValue = ctx.whenValue()
    matchExactlyOne(
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
      VariableDeclarationGroup.of(
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
    matchExactlyOne(
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
          val absoluteValue = ctx.IntegerLiteral().text.toInt() // value without sign
          val integerVal =
            LiteralExpression.IntegerVal(absoluteValue, toSourceLocation(ctx.IntegerLiteral()))

          if (ctx.SUB() != null) {
            UnaryExpression(value = integerVal, op = UnaryExpression.Operator.NEGATION, loc)
          } else {
            integerVal
          }
        }
        ctx.LongLiteral() != null ->
          LiteralExpression.LongVal(text.replace("[lL]$".toRegex(), "").toLong(), loc)
        ctx.StringLiteral() != null ->
          LiteralExpression.StringVal(text.removeSurrounding("'", "'"), loc)
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
    matchExactlyOne(
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
    matchExactlyOne(ruleBeingChecked = ctx, ctx.THIS(), ctx.SUPER(), ctx.id())
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
    val matchedTerminal = matchExactlyOne(ruleBeingChecked = ctx, ctx.MUL(), ctx.DIV())

    return BinaryExpression(
      visitExpression(ctx.expression().first()),
      opString = matchedTerminal.text,
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
    matchExactlyOne(
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
    val matchedTerminal = matchExactlyOne(ruleBeingChecked = ctx, ctx.ADD(), ctx.SUB())

    return BinaryExpression(
      visitExpression(ctx.expression().first()),
      opString = matchedTerminal.text,
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
    val matchedTerminal =
      matchExactlyOne(
        ruleBeingChecked = ctx,
        ctx.TRIPLEEQUAL(),
        ctx.TRIPLENOTEQUAL(),
        ctx.EQUAL(),
        ctx.NOTEQUAL(),
        ctx.LESSANDGREATER(),
      )

    return BinaryExpression(
      visitExpression(ctx.expression().first()),
      opString = matchedTerminal.text,
      visitExpression(ctx.expression().last()),
      toSourceLocation(ctx)
    )
  }

  /** Translates the 'expression#postOpExpression' grammar rule and returns an AST [Expression]. */
  override fun visitPostOpExpression(ctx: ApexParser.PostOpExpressionContext): Expression {
    matchExactlyOne(ruleBeingChecked = ctx, ctx.INC(), ctx.DEC())
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
    matchExactlyOne(ruleBeingChecked = ctx, ctx.TILDE(), ctx.BANG())
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
    matchExactlyOne(ruleBeingChecked = ctx, ctx.ADD(), ctx.SUB(), ctx.INC(), ctx.DEC())
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

  /**
   * Translates the 'primary#voidPrimary' grammar rule and returns an AST [Expression].
   *
   * This grammar rule appeared in apex-parser@2.16, but more info is needed about
   * semantic meaning.
   */
  override fun visitVoidPrimary(ctx: ApexParser.VoidPrimaryContext): Expression =
    throw TranslationException(ctx, "void.<class> not yet translated")

  /** Translates the 'primary#idPrimary' grammar rule and returns an AST [Expression]. */
  override fun visitIdPrimary(ctx: ApexParser.IdPrimaryContext): Expression {
    val id = visitId(ctx.id())
    return VariableExpression(id, id.getSourceLocation())
  }

  /** Translates the 'primary#soqlPrimary' grammar rule and returns an AST [Expression]. */
  override fun visitSoqlPrimary(ctx: ApexParser.SoqlPrimaryContext) =
    visitSoqlLiteral(ctx.soqlLiteral())

  /** Translates the 'primary#soslPrimary' grammar rule and returns an AST [Expression]. */
  override fun visitSoslPrimary(ctx: ApexParser.SoslPrimaryContext) =
    visitSoslLiteral(ctx.soslLiteral())

  // END PRIMARY

  // BEGIN SOQL/SOSL

  /** Translates the 'soqlLiteral' grammar rule and returns an AST [SoqlExpression]. */
  override fun visitSoqlLiteral(ctx: ApexParser.SoqlLiteralContext) =
    SoqlExpression(toSourceString(ctx.query()),
                   visitQuery(ctx.query()).bindings,
                   toSourceLocation(ctx))

  /** Translates the 'soslLiteral' grammar rule and returns an AST [SoslExpression]. */
  override fun visitSoslLiteral(ctx: ApexParser.SoslLiteralContext): SoslExpression {
    val bindings = listOf(
      *visitSoslClauses(ctx.soslClauses()).bindings.toTypedArray(),
      ctx.boundExpression()?.let { visitBoundExpression(it) },
    ).filterNotNull()
    return SoslExpression(
        toSourceString(ctx).trim().removeSurrounding("[","]"),
        bindings,
        toSourceLocation(ctx)
    )
  }

  /** Translates the 'boundExpression' grammar rule and returns a [SoqlOrSoslBinding]. */
  override fun visitBoundExpression(ctx: ApexParser.BoundExpressionContext) =
    SoqlOrSoslBinding(visitExpression(ctx.expression()))

  /**
   * This class captures details about a SOQL or SOSL fragment.
   *
   * We don't (yet) fully represent parsed SOQL or SOSL. TODO(b/216117963)
   * We do aggregate the set of bound expressions.
   *
   * @property bindings is the list of bound expressions
   */
  data class SoqlFragment(val bindings: List<SoqlOrSoslBinding>) {
    /** Constructs a SOQL fragment with one expression binding. */
    constructor(binding: SoqlOrSoslBinding) : this(listOf(binding))

    companion object {
      /** Returns a [SoqlFragment] with no expression bindings. */
      fun withNoBindings() = SoqlFragment(emptyList())

      /** Constructs a SOQL fragment from its sub-fragments. */
      fun mergeOf(vararg fragments: SoqlFragment?) =
        SoqlFragment(listOf(*fragments).filterNotNull().map { it.bindings }.flatten())

      /** Constructs a SOQL fragment from its sub-fragments. */
      fun mergeOf(fragments: List<SoqlFragment?>) =
        SoqlFragment(fragments.filterNotNull().map { it.bindings }.flatten())
    }
  }

  // NOTE: the SOQL/SOSL visitors below simply propagate the list of bound expressions up to
  // the primary.

  /** Translates the 'query' grammar rule and returns a [SoqlFragment]. */
  override fun visitQuery(ctx: ApexParser.QueryContext) =
    SoqlFragment.mergeOf(
      ctx.selectList()?.let { visitSelectList(it) },
      ctx.fromNameList()?.let { visitFromNameList(it) },
      ctx.usingScope()?.let { visitUsingScope(it) },
      ctx.whereClause()?.let { visitWhereClause(it) },
      ctx.withClause()?.let { visitWithClause(it) },
      ctx.groupByClause()?.let { visitGroupByClause(it) },
      ctx.orderByClause()?.let { visitOrderByClause(it) },
      ctx.limitClause()?.let { visitLimitClause(it) },
      ctx.offsetClause()?.let { visitOffsetClause(it) },
      ctx.allRowsClause()?.let { visitAllRowsClause(it) },
      ctx.forClauses()?.let { visitForClauses(it) },
      ctx.updateList()?.let { visitUpdateList(it) },
    )

  /** Translates the 'subQuery' grammar rule and returns a [SoqlFragment]. */
  override fun visitSubQuery(ctx: ApexParser.SubQueryContext) =
    SoqlFragment.mergeOf(
      ctx.subFieldList()?.let { visitSubFieldList(it) },
      ctx.fromNameList()?.let { visitFromNameList(it) },
      ctx.whereClause()?.let { visitWhereClause(it) },
      ctx.orderByClause()?.let { visitOrderByClause(it) },
      ctx.limitClause()?.let { visitLimitClause(it) },
      ctx.forClauses()?.let { visitForClauses(it) },
      ctx.updateList()?.let { visitUpdateList(it) },
    )

  /** Translates the 'selectList' grammar rule and returns a [SoqlFragment]. */
  override fun visitSelectList(ctx: ApexParser.SelectListContext) =
    SoqlFragment.mergeOf(ctx.selectEntry().map { visitSelectEntry(it) })

  /** Translates the 'selectEntry' grammar rule and returns a [SoqlFragment]. */
  override fun visitSelectEntry(ctx: ApexParser.SelectEntryContext) =
    SoqlFragment.mergeOf(
      ctx.fieldName()?.let { visitFieldName(it) },
      ctx.soqlId()?.let { visitSoqlId(it) },
      ctx.soqlFunction()?.let { visitSoqlFunction(it) },
      ctx.subQuery()?.let { visitSubQuery(it) },
      ctx.typeOf()?.let { visitTypeOf(it) },
    )

  /** Translates the 'fieldName' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitFieldName(ctx: ApexParser.FieldNameContext) = SoqlFragment.withNoBindings()

  /** Translates the 'fromNameList' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitFromNameList(ctx: ApexParser.FromNameListContext) =
    SoqlFragment.withNoBindings()

  /** Translates the 'subFieldList' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitSubFieldList(ctx: ApexParser.SubFieldListContext) =
    SoqlFragment.withNoBindings()

  /** Translates the 'subFieldEntry' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitSubFieldEntry(ctx: ApexParser.SubFieldEntryContext) =
    SoqlFragment.withNoBindings()

  /** Translates the 'soqlFieldsParameter' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitSoqlFieldsParameter(ctx: ApexParser.SoqlFieldsParameterContext) =
    SoqlFragment.withNoBindings()

  /** Translates the 'soqlFunction' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitSoqlFunction(ctx: ApexParser.SoqlFunctionContext) =
    SoqlFragment.withNoBindings()

  /** Translates the 'dateFieldName' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitDateFieldName(ctx: ApexParser.DateFieldNameContext) =
    SoqlFragment.withNoBindings()

  /** Translates the 'typeOf' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitTypeOf(ctx: ApexParser.TypeOfContext) = SoqlFragment.withNoBindings()

  /** Translates the 'whenClause' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitWhenClause(ctx: ApexParser.WhenClauseContext) = SoqlFragment.withNoBindings()

  /** Translates the 'elseClause' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitElseClause(ctx: ApexParser.ElseClauseContext) = SoqlFragment.withNoBindings()

  /** Translates the 'fieldNameList' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitFieldNameList(ctx: ApexParser.FieldNameListContext) =
    SoqlFragment.withNoBindings()

  /** Translates the 'usingScope' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitUsingScope(ctx: ApexParser.UsingScopeContext) = SoqlFragment.withNoBindings()

  /** Translates the 'whereClause' grammar rule and returns a [SoqlFragment]. */
  override fun visitWhereClause(ctx: ApexParser.WhereClauseContext) =
    visitLogicalExpression(ctx.logicalExpression())

  /** Translates the 'logicalExpression' grammar rule and returns a [SoqlFragment]. */
  override fun visitLogicalExpression(ctx: ApexParser.LogicalExpressionContext): SoqlFragment =
    SoqlFragment.mergeOf(
      ctx.conditionalExpression().map { visitConditionalExpression(it) }
    )

  /** Translates the 'conditionalExpression' grammar rule and returns a [SoqlFragment]. */
  override fun visitConditionalExpression(ctx: ApexParser.ConditionalExpressionContext) =
    SoqlFragment.mergeOf(
      ctx.logicalExpression()?.let { visitLogicalExpression(it) },
      ctx.fieldExpression()?.let { visitFieldExpression(it) },
    )

  /** Translates the 'fieldExpression' grammar rule and returns a [SoqlFragment]. */
  override fun visitFieldExpression(ctx: ApexParser.FieldExpressionContext) = 
  SoqlFragment.mergeOf(
    ctx.fieldName()?.let { visitFieldName(it) },
    ctx.value()?.let { visitValue(it) },
    ctx.soqlFunction()?.let { visitSoqlFunction(it) },
  )

  /** Translates the 'value' grammar rule and returns a [SoqlFragment]. */
  override fun visitValue(ctx: ApexParser.ValueContext): SoqlFragment = SoqlFragment.mergeOf(
    ctx.dateFormula()?.let { visitDateFormula(it) },
    ctx.subQuery()?.let { visitSubQuery(it) },
    ctx.valueList()?.let { visitValueList(it) },
    ctx.boundExpression()?.let { SoqlFragment(visitBoundExpression(it)) }
  )

  /** Translates the 'valueList' grammar rule and returns a [SoqlFragment]. */
  override fun visitValueList(ctx: ApexParser.ValueListContext) = SoqlFragment.mergeOf(
    ctx.value().map { visitValue(it) }
  )

  /** Translates the 'withClause' grammar rule and returns a [SoqlFragment]. */
  override fun visitWithClause(ctx: ApexParser.WithClauseContext) = SoqlFragment.mergeOf(
    ctx.filteringExpression()?.let { visitFilteringExpression(it) },
    ctx.logicalExpression()?.let { visitLogicalExpression(it) },
  )

  /** Translates the 'filteringExpression' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitFilteringExpression(ctx: ApexParser.FilteringExpressionContext) =
    SoqlFragment.withNoBindings()

  /** Translates the 'dataCategorySelection' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitDataCategorySelection(ctx: ApexParser.DataCategorySelectionContext) =
    SoqlFragment.withNoBindings()

  /** Translates the 'dataCategoryName' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitDataCategoryName(ctx: ApexParser.DataCategoryNameContext) =
    SoqlFragment.withNoBindings()

  /** Translates the 'filteringSelector' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitFilteringSelector(ctx: ApexParser.FilteringSelectorContext) =
    SoqlFragment.withNoBindings()

  /** Translates the 'groupByClause' grammar rule and returns a [SoqlFragment]. */
  override fun visitGroupByClause(ctx: ApexParser.GroupByClauseContext) = SoqlFragment.mergeOf(
    ctx.selectList()?.let { visitSelectList(it) },
    ctx.logicalExpression()?.let { visitLogicalExpression(it) },
    *ctx.fieldName().map { visitFieldName(it) }.toTypedArray(),
  )

  /** Translates the 'orderByClause' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitOrderByClause(ctx: ApexParser.OrderByClauseContext) =
    SoqlFragment.withNoBindings()

  /** Translates the 'fieldOrderList' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitFieldOrderList(ctx: ApexParser.FieldOrderListContext) =
    SoqlFragment.withNoBindings()

  /** Translates the 'fieldOrder' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitFieldOrder(ctx: ApexParser.FieldOrderContext) = SoqlFragment.withNoBindings()

  /** Translates the 'limitClause' grammar rule and returns a [SoqlFragment]. */
  override fun visitLimitClause(ctx: ApexParser.LimitClauseContext) = SoqlFragment.mergeOf(
    ctx.boundExpression()?.let { SoqlFragment(visitBoundExpression(it)) }
  )

  /** Translates the 'offsetClause' grammar rule and returns a [SoqlFragment]. */
  override fun visitOffsetClause(ctx: ApexParser.OffsetClauseContext) = SoqlFragment.mergeOf(
    ctx.boundExpression()?.let { SoqlFragment(visitBoundExpression(it)) }
  )

  /** Translates the 'allRows' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitAllRowsClause(ctx: ApexParser.AllRowsClauseContext) =
    SoqlFragment.withNoBindings()

  /** Translates the 'forClauses' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitForClauses(ctx: ApexParser.ForClausesContext) = SoqlFragment.withNoBindings()

  /** Translates the 'dateFormula' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitDateFormula(ctx: ApexParser.DateFormulaContext) = SoqlFragment.withNoBindings()

  /** Translates the 'soqlId' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitSoqlId(ctx: ApexParser.SoqlIdContext) = SoqlFragment.withNoBindings()

  /** Translates the 'soslClauses' grammar rule and returns a [SoqlFragment]. */
  override fun visitSoslClauses(ctx: ApexParser.SoslClausesContext) =
    SoqlFragment.mergeOf(
      ctx.searchGroup()?.let { visitSearchGroup(it) },
      ctx.fieldSpecList()?.let { visitFieldSpecList(it) },
      ctx.filteringExpression()?.let { visitFilteringExpression(it) },
      ctx.networkList()?.let { visitNetworkList(it) },
      ctx.limitClause()?.let { visitLimitClause(it) },
      ctx.updateList()?.let { visitUpdateList(it) },
    )

  /** Translates the 'fieldSpecList' grammar rule and returns a [SoqlFragment]. */
  override fun visitFieldSpecList(ctx: ApexParser.FieldSpecListContext): SoqlFragment =
    SoqlFragment.mergeOf(
      ctx.fieldSpec()?.let { visitFieldSpec(it) },
      *ctx.fieldSpecList().map { visitFieldSpecList(it) }.toTypedArray(),
    )

  /** Translates the 'fieldSpec' grammar rule and returns a [SoqlFragment]. */
  override fun visitFieldSpec(ctx: ApexParser.FieldSpecContext) = SoqlFragment.mergeOf(
    *ctx.soslId().map { visitSoslId(it) }.toTypedArray(),
    ctx.fieldList()?.let { visitFieldList(it) },
    ctx.fieldOrderList()?.let { visitFieldOrderList(it) },
    ctx.limitClause()?.let { visitLimitClause(it) },
    ctx.offsetClause()?.let { visitOffsetClause(it) },
  )

  /** Translates the 'searchGroup' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitSearchGroup(ctx: ApexParser.SearchGroupContext) = SoqlFragment.withNoBindings()

  /** Translates the 'fieldList' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitFieldList(ctx: ApexParser.FieldListContext) = SoqlFragment.withNoBindings()

  /** Translates the 'updateList' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitUpdateList(ctx: ApexParser.UpdateListContext) = SoqlFragment.withNoBindings()

  /** Translates the 'updateType' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitUpdateType(ctx: ApexParser.UpdateTypeContext) = SoqlFragment.withNoBindings()

  /** Translates the 'networkList' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitNetworkList(ctx: ApexParser.NetworkListContext) = SoqlFragment.withNoBindings()

  /** Translates the 'soslId' grammar rule and returns a [SoqlFragment] with no bindings. */
  override fun visitSoslId(ctx: ApexParser.SoslIdContext) = SoqlFragment.withNoBindings()

  // END SOQL/SOSL

  // BEGIN STATEMENT

  /** Translates the 'statement' grammar rule and returns an AST [Statement]. */
  override fun visitStatement(ctx: ApexParser.StatementContext): Statement {
    val matched =
      matchExactlyOne(
        ruleBeingChecked = ctx,
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
    val bodyStatement = if (ctx.statement() != null) {
      visitStatement(ctx.statement())
    } else {
      CompoundStatement(emptyList(), CompoundStatement.Scoping.SCOPE_BOUNDARY, SourceLocation.UNKNOWN)
    }

    val enhancedForControl = forControl.enhancedForControl()
    if (enhancedForControl != null) {
      // Enhanced for loop
      val elementDeclarationGroup =
        VariableDeclarationGroup.of(
          visitId(enhancedForControl.id()),
          visitTypeRef(enhancedForControl.typeRef()),
          modifiers = emptyList(),
          initializer = null,
          toSourceLocation(enhancedForControl)
        )
      return EnhancedForLoopStatement(
        elementDeclarationGroup,
        collection = visitExpression(enhancedForControl.expression()),
        bodyStatement,
        loc
      )
    } else {
      // Traditional for loop
      val declarationGroup = forControl.forInit()?.localVariableDeclaration()?.let{visitLocalVariableDeclaration(it)}
      val initializations =
        translateOptionalList(forControl.forInit()?.expressionList(), ::visitExpressionList)
      val updates =
        translateOptionalList(forControl.forUpdate()?.expressionList(), ::visitExpressionList)
      return ForLoopStatement(
        declarationGroup,
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
      body = if (ctx.statement() != null) {
        visitStatement(ctx.statement())
      } else {
        CompoundStatement(emptyList(), CompoundStatement.Scoping.SCOPE_BOUNDARY, SourceLocation.UNKNOWN)
      },
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

  /** Translates the 'accessLevel' grammar rule and returns an AST [DmlStatement.AccessLevel]. */
  override fun visitAccessLevel(ctx: ApexParser.AccessLevelContext): DmlStatement.AccessLevel =
    when {
      ctx.SYSTEM() != null -> DmlStatement.AccessLevel.SYSTEM_MODE
      ctx.USER() != null -> DmlStatement.AccessLevel.USER_MODE
      else -> throw TranslationException(ctx, "Unreachable case reached")
    }

  /** Translates the 'insertStatement' grammar rule and returns an AST [Statement]. */
  override fun visitInsertStatement(ctx: ApexParser.InsertStatementContext): Statement =
    DmlStatement.Insert(
      visitExpression(ctx.expression()),
      ctx.accessLevel()?.let{ visitAccessLevel(it) },
      toSourceLocation(ctx)
    )

  /** Translates the 'updateStatement' grammar rule and returns an AST [Statement]. */
  override fun visitUpdateStatement(ctx: ApexParser.UpdateStatementContext): Statement =
    DmlStatement.Update(
      visitExpression(ctx.expression()),
      ctx.accessLevel()?.let{ visitAccessLevel(it) },
      toSourceLocation(ctx)
    )

  /** Translates the 'deleteStatement' grammar rule and returns an AST [Statement]. */
  override fun visitDeleteStatement(ctx: ApexParser.DeleteStatementContext): Statement =
    DmlStatement.Delete(
      visitExpression(ctx.expression()),
      ctx.accessLevel()?.let{ visitAccessLevel(it) },
      toSourceLocation(ctx)
    )

  /** Translates the 'undeleteStatement' grammar rule and returns an AST [Statement]. */
  override fun visitUndeleteStatement(ctx: ApexParser.UndeleteStatementContext): Statement =
    DmlStatement.Undelete(
      visitExpression(ctx.expression()),
      ctx.accessLevel()?.let{ visitAccessLevel(it) },
      toSourceLocation(ctx)
    )

  /** Translates the 'upsertStatement' grammar rule and returns an AST [Statement]. */
  override fun visitUpsertStatement(ctx: ApexParser.UpsertStatementContext): Statement =
    DmlStatement.Upsert(
      visitExpression(ctx.expression()),
      translateOptional(ctx.qualifiedName(), ::visitQualifiedName),
      ctx.accessLevel()?.let{ visitAccessLevel(it) },
      toSourceLocation(ctx)
    )

  /** Translates the 'mergeStatement' grammar rule and returns an AST [Statement]. */
  override fun visitMergeStatement(ctx: ApexParser.MergeStatementContext): Statement =
    DmlStatement.Merge(
      value = visitExpression(ctx.expression().first()),
      from = visitExpression(ctx.expression().last()),
      access = ctx.accessLevel()?.let{ visitAccessLevel(it) },
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

  /**
   * Matches one of [trees] and throws a [TranslationException] unless exactly one is non-null.
   * @return the matched [SyntaxTree]
   */
  private fun <T : SyntaxTree> matchExactlyOne(
    ruleBeingChecked: ParserRuleContext,
    vararg trees: T?
  ): T {
    val nonNullTrees = trees.filterNotNull()
    if (nonNullTrees.size != 1) {
      throw TranslationException(
        ruleBeingChecked,
        "Exactly one rule should match, but found ${nonNullTrees.size}"
      )
    }
    return nonNullTrees.first()
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
    val firstToken = tokens.get(interval.a)
    // The end of the last token in the interval is the start of the next token--
    // except for the EOF token which has no next token.
    val nextToken = tokens.get(min(interval.b + 1, tokens.size() - 1))

    return SourceLocation(
      firstToken.line,
      firstToken.charPositionInLine,
      nextToken.line,
      nextToken.charPositionInLine,
    )
  }

  /** Gets the original source string for any parser rule context. */
  private fun toSourceString(context: ParserRuleContext): String {
    val stream = context.start.getInputStream()
    return stream.getText(Interval(context.start.getStartIndex(), context.stop.getStopIndex()))
  }

    private companion object {
    val logger = FluentLogger.forEnclosingClass()
  }
}
