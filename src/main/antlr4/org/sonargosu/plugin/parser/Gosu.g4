/*
 * ANTLR 4 port of the official Gosu grammars (gosu-core/.../parser/ebnf/Gosu.g and GosuProg.g,
 * Copyright 2013 Guidewire Software, Inc., Apache License 2.0).
 *
 * Differences from the ANTLR 3 originals:
 *  - backtracking and syntactic predicates (=>) are gone; ANTLR 4's ALL(*) prediction replaces them.
 *  - Gosu.g and GosuProg.g are merged: 'compilationUnit' parses classes/interfaces/enums/enhancements
 *    (.gs, .gsx), 'program' parses programs (.gsp).
 *  - Multi-character '<' / '>' operators (<=, >=, <<, >>, >>>, <<=, >>=, >>>=) are still built from
 *    single-character tokens so nested generics close correctly, but the "tokens must be adjacent"
 *    predicates were dropped: the analyzer is slightly more permissive than the compiler.
 *  - Comments go to the HIDDEN channel instead of being skipped.
 *  - Identifiers accept Unicode letters.
 *
 * Additions for Gosu features newer than the 2013 grammar (found by parsing the gosu-lang sources):
 *  - annotation type declarations:          annotation Foo { function value() : String = "" }
 *  - the 'reified' modifier:                 reified function cast<N>(t : Type<N>) : N[]
 *  - feature literals in uses:               uses gw.util.science.UnitConstants#min
 *  - interface members with bodies:          property get Min() : Integer { return 0 }
 *  - short property declarations:            property Name : String
 *  - rational literals:                      3.14r
 *  - unit/binder expressions:                5 kg, kg m/s   (operands juxtaposed on one line)
 *  - programs extending a base class:        extends MyProgramBase   (first line of a .gsp)
 */
grammar Gosu;

@parser::members {
  /** True when the next token starts on the same line as the previous one. */
  private boolean sameLine() {
    return _input.LT(-1).getLine() == _input.LT(1).getLine();
  }

  /** Binder expressions ("5 kg"): the right operand must be a name or number on the same line. */
  private boolean canBind() {
    int type = _input.LT(1).getType();
    return sameLine() && (type == Ident || type == NumberLiteral);
  }
}

// ---------------------------------------------------------------------------------------------
// Entry points
// ---------------------------------------------------------------------------------------------

compilationUnit : header modifiers typeDeclaration EOF ;

program : header ('extends' classOrInterfaceType)? ( modifiers typeDeclaration | statementTop )* EOF ;

typeDeclaration : gClass | gInterfaceOrStructure | gEnum | gEnhancement | gAnnotation ;

statementTop : statement
             | modifiers functionDefn functionBody
             | modifiers propertyDefn functionBody
             ;

// ---------------------------------------------------------------------------------------------
// Declarations
// ---------------------------------------------------------------------------------------------

header : classpathStatements? typeLoaderStatements? ('package' namespaceStatement)? usesStatementList? ;

classpathStatements : ('classpath' StringLiteral)+ ;

typeLoaderStatements : ('typeloader' typeLiteral)+ ;

// Optional use-site target: @get:MyAnno, @receiver:MyAnno
annotation : '@' (idAll ':')? idAll ('.' idAll)* annotationArguments? ;

gClass : 'class' id typeVariableDefs ('extends' classOrInterfaceType)? ('implements' classOrInterfaceType (',' classOrInterfaceType)*)? classBody ;

gInterfaceOrStructure : ('interface' | 'structure') id typeVariableDefs ('extends' classOrInterfaceType (',' classOrInterfaceType)*)? interfaceBody ;

gEnum : 'enum' id typeVariableDefs ('implements' classOrInterfaceType (',' classOrInterfaceType)*)? enumBody ;

gEnhancement : 'enhancement' id typeVariableDefs ':' classOrInterfaceType ('[' ']')* enhancementBody ;

gAnnotation : 'annotation' id annotationBody ;

annotationBody : '{' ( modifiers (functionDefn ('=' expression)? | fieldDefn) ';'? )* '}' ;

classBody : '{' classMembers '}' ;

enhancementBody : '{' enhancementMembers '}' ;

interfaceBody : '{' interfaceMembers '}' ;

enumBody : '{' enumConstants? classMembers '}' ;

enumConstants : enumConstant (',' enumConstant)* ','? ';'? ;

enumConstant : id optionalArguments ;

// Bodies are optional: interfaces may have default and static functions/properties.
interfaceMembers : ( modifiers
                     ( functionDefn functionBody?
                     | propertyDefn functionBody?
                     | fieldDefn
                     | gClass
                     | gInterfaceOrStructure
                     | gEnum
                     | gAnnotation
                     ) ';'?
                   )* ;

classMembers : declaration* ;

declaration : modifiers
              ( functionDefn functionBody?
              | constructorDefn functionBody
              | propertyDefn functionBody?
              | fieldDefn
              | delegateDefn
              | gClass
              | gInterfaceOrStructure
              | gEnum
              | gAnnotation
              ) ';'?
            ;

enhancementMembers : ( modifiers
                       ( functionDefn functionBody
                       | propertyDefn functionBody
                       ) ';'?
                     )* ;

delegateDefn : 'delegate' id delegateStatement ;

delegateStatement : (':' typeLiteral)? 'represents' typeLiteral (',' typeLiteral)* ('=' expression)? ;

optionalType : (':' (typeLiteral | blockTypeLiteral) | blockTypeLiteral)? ;

fieldDefn : 'var' id optionalType ('as' 'readonly'? id)? ('=' expression)? ;

propertyDefn : 'property' ('get' | 'set') id parameters (':' typeLiteral)?
             | 'property' ('get' | 'set')? id ':' typeLiteral ('=' expression)?
             ;

dotPathWord : idAll ('.' idAll)* ;

namespaceStatement : dotPathWord ';'* ;

usesStatementList : ('uses' usesStatement)+ ;

// Static imports: uses a.B#member, uses a.B#*, uses a.B#method(String, int)
usesStatement : dotPathWord ('.' '*' | '#' ('*' | idAll ('(' (typeLiteral (',' typeLiteral)*)? ')')?))? ';'* ;

typeVariableDefs : ('<' typeVariableDefinition (',' typeVariableDefinition)* '>')? ;

typeVariableDefinition : id ('extends' typeLiteralList)? ;

functionBody : statementBlock ;

parameters : '(' parameterDeclarationList? ')' ;

functionDefn : 'function' id typeVariableDefs parameters (':' typeLiteral)? ;

constructorDefn : 'construct' parameters (':' typeLiteral)? ;

modifiers : ( annotation
            | 'private'
            | 'internal'
            | 'protected'
            | 'public'
            | 'static'
            | 'abstract'
            | 'override'
            | 'final'
            | 'transient'
            | 'reified'
            )* ;

// ---------------------------------------------------------------------------------------------
// Statements
// ---------------------------------------------------------------------------------------------

statement : ( ifStatement
            | tryCatchFinallyStatement
            | throwStatement
            | 'continue'
            | 'break'
            | returnStatement
            | forEachStatement
            | whileStatement
            | doWhileStatement
            | switchStatement
            | usingStatement
            | assertStatement
            | 'final' localVarStatement
            | localVarStatement
            | evalExpr
            | assignmentOrMethodCall
            | statementBlock
            ) ';'?
          | ';'
          ;

ifStatement : 'if' '(' expression ')' statement ';'? ('else' statement)? ;

tryCatchFinallyStatement : 'try' statementBlock ( catchClause+ ('finally' statementBlock)? | 'finally' statementBlock ) ;

catchClause : 'catch' '(' 'var'? id (':' typeLiteral)? ')' statementBlock ;

assertStatement : 'assert' expression (':' expression)? ;

usingStatement : 'using' '(' (localVarStatement (',' localVarStatement)* | expression) ')' statementBlock ('finally' statementBlock)? ;

// The original only takes the expression when it is not followed by '='; ALL(*) sorts that out.
returnStatement : 'return' expression? ;

whileStatement : 'while' '(' expression ')' statement ;

doWhileStatement : 'do' statement 'while' '(' expression ')' ;

switchStatement : 'switch' '(' expression ')' '{' switchBlockStatementGroup* '}' ;

switchBlockStatementGroup : ('case' expression ':' | 'default' ':') statement* ;

throwStatement : 'throw' expression ;

localVarStatement : 'var' id optionalType ('=' expression)? ;

forEachStatement : ('foreach' | 'for') '(' (expression indexVar? | 'var'? id 'in' expression indexRest?) ')' statement ;

indexRest : indexVar iteratorVar
          | iteratorVar indexVar
          | indexVar
          | iteratorVar
          ;

indexVar : 'index' id ;

iteratorVar : 'iterator' id ;

thisSuperExpr : 'this' | 'super' ;

assignmentOrMethodCall : (newExpr | thisSuperExpr | typeLiteralExpr | parenthExpr | StringLiteral)
                         indirectMemberAccess
                         (incrementOp | assignmentOp expression)?
                       ;

statementBlock : statementBlockBody ;

statementBlockBody : '{' statement* '}' ;

// ---------------------------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------------------------

blockTypeLiteral : blockLiteral ;

blockLiteral : '(' (blockLiteralArg (',' blockLiteralArg)*)? ')' (':' typeLiteral)? ;

blockLiteralArg : id ('=' expression | blockTypeLiteral)
                | (id ':')? typeLiteral ('=' expression)?
                ;

typeLiteral : type ('&' type)* ;

typeLiteralType : typeLiteral ;

typeLiteralExpr : typeLiteral ;

typeLiteralList : typeLiteral ;

type : classOrInterfaceType ('[' ']')*
     | 'block' blockLiteral
     ;

// idAll after a dot: package names may be keywords (java.util.function.Function)
classOrInterfaceType : idclassOrInterfaceType typeArguments ('.' idAll typeArguments)* ;

typeArguments : ('<' typeArgument (',' typeArgument)* '>')? ;

typeArgument : typeLiteralType
             | '?' (('extends' | 'super') typeLiteralType)?
             ;

// ---------------------------------------------------------------------------------------------
// Expressions (lowest to highest precedence)
// ---------------------------------------------------------------------------------------------

expression : conditionalExpr ;

conditionalExpr : conditionalOrExpr ('?' conditionalExpr ':' conditionalExpr | '?:' conditionalExpr)? ;

conditionalOrExpr : conditionalAndExpr (orOp conditionalAndExpr)* ;

conditionalAndExpr : bitwiseOrExpr (andOp bitwiseOrExpr)* ;

bitwiseOrExpr : bitwiseXorExpr ('|' bitwiseXorExpr)* ;

bitwiseXorExpr : bitwiseAndExpr ('^' bitwiseAndExpr)* ;

bitwiseAndExpr : equalityExpr ('&' equalityExpr)* ;

equalityExpr : relationalExpr (equalityOp relationalExpr)* ;

relationalExpr : intervalExpr (relOp intervalExpr | 'typeis' typeLiteralType)* ;

intervalExpr : bitshiftExpr (intervalOp bitshiftExpr)? ;

bitshiftExpr : additiveExpr (bitshiftOp additiveExpr)* ;

additiveExpr : multiplicativeExpr (additiveOp multiplicativeExpr)* ;

// The second alternative is a binder/unit expression: "5 kg", "kg m/s".
multiplicativeExpr : typeAsExpr (multiplicativeOp typeAsExpr | {canBind()}? typeAsExpr)* ;

typeAsExpr : unaryExpr (typeAsOp typeLiteral)* ;

unaryExpr : ('+' | '-' | '!-') unaryExprNotPlusMinus
          | unaryExprNotPlusMinus
          ;

unaryExprNotPlusMinus : unaryOp unaryExpr
                      | '\\' blockExpr
                      | evalExpr
                      | primaryExpr
                      ;

// statementBlock first: "\ -> { a() \n b() }" must not be read as a { ... } collection literal.
blockExpr : parameterDeclarationList? '->' (statementBlock | expression) ;

parameterDeclarationList : parameterDeclaration (',' parameterDeclaration)* ;

parameterDeclaration : annotation* 'final'? id ((':' (typeLiteral | blockTypeLiteral) ('=' expression)?) | blockTypeLiteral | '=' expression)? ;

annotationArguments : arguments ;

arguments : '(' (argExpression (',' argExpression)*)? ')' ;

optionalArguments : arguments? ;

argExpression : namedArgumentExpression | expression ;

namedArgumentExpression : ':' idAll '=' expression ;

evalExpr : 'eval' '(' expression ')' ;

featureLiteral : '#' (id | 'construct') typeArguments optionalArguments ;

standAloneDataStructureInitialization : '{' initializerExpression? '}' ;

primaryExpr : ( newExpr
              | thisSuperExpr
              | literal
              | typeLiteralExpr
              | parenthExpr
              | standAloneDataStructureInitialization
              )
              indirectMemberAccess
            ;

parenthExpr : '(' expression ')' ;

newExpr : 'new' classOrInterfaceType?
          ( arguments ('{' (initializer | anonymousInnerClass) '}')?
          | '[' ( ']' ('[' ']')* arrayInitializer
                | expression ']' ('[' expression ']')* ('[' ']')*
                )
          )
        ;

anonymousInnerClass : classMembers ;

arrayInitializer : '{' (expression (',' expression)*)? '}' ;

initializer : (initializerExpression | objectInitializer)? ;

initializerExpression : mapInitializerList | arrayValueList ;

arrayValueList : expression (',' expression)* ;

mapInitializerList : expression '->' expression (',' expression '->' expression)* ;

objectInitializer : initializerAssignment (',' initializerAssignment)* ;

initializerAssignment : ':' idAll '=' expression ;

// A call's argument list must start on the same line as the callee; otherwise
// "foo()\n(x as String).length()" would be read as foo()(x as String).
indirectMemberAccess : ( ('.' | '?.' | '*.') idAll typeArguments
                       | featureLiteral
                       | ('[' | '?[') expression ']'
                       | {sameLine()}? arguments
                       )*
                     ;

literal : NumberLiteral
        | 'NaN'
        | 'Infinity'
        | featureLiteral
        | StringLiteral
        | CharLiteral
        | 'true'
        | 'false'
        | 'null'
        ;

// ---------------------------------------------------------------------------------------------
// Operators
// ---------------------------------------------------------------------------------------------

orOp : '||' | 'or' ;

andOp : '&&' | 'and' ;

assignmentOp : '='
             | '+='
             | '-='
             | '*='
             | '/='
             | '&='
             | '&&='
             | '|='
             | '||='
             | '^='
             | '%='
             | '<' '<' '='
             | '>' '>' '>' '='
             | '>' '>' '='
             ;

incrementOp : '++' | '--' ;

equalityOp : '===' | '!==' | '==' | '!=' | '<>' ;

intervalOp : '..' | '|..' | '..|' | '|..|' ;

relOp : '<' '='
      | '>' '='
      | '<'
      | '>'
      ;

bitshiftOp : '<' '<'
           | '>' '>' '>'
           | '>' '>'
           ;

additiveOp : '+' | '-' | '?+' | '?-' | '!+' | '!-' ;

multiplicativeOp : '*' | '/' | '%' | '?*' | '!*' | '?/' | '?%' ;

typeAsOp : 'typeas' | 'as' ;

unaryOp : '~' | '!' | 'not' | 'typeof' | 'statictypeof' ;

// ---------------------------------------------------------------------------------------------
// Identifiers: Gosu lets many keywords double as names
// ---------------------------------------------------------------------------------------------

id : Ident
   | 'true' | 'false' | 'NaN' | 'Infinity' | 'null'
   | 'length' | 'exists' | 'startswith' | 'contains' | 'where' | 'find' | 'as' | 'except'
   | 'index' | 'iterator' | 'get' | 'set' | 'assert'
   | 'private' | 'internal' | 'protected' | 'public' | 'abstract' | 'hide' | 'final' | 'static'
   | 'readonly' | 'outer' | 'execution' | 'request' | 'session' | 'application'
   | 'void' | 'block' | 'enhancement' | 'classpath' | 'typeloader' | 'annotation' | 'reified'
   ;

idclassOrInterfaceType : Ident
                       | 'true' | 'false' | 'NaN' | 'Infinity' | 'null'
                       | 'length' | 'exists' | 'startswith' | 'contains' | 'where' | 'find' | 'as' | 'except'
                       | 'index' | 'iterator' | 'get' | 'set' | 'assert'
                       | 'private' | 'internal' | 'protected' | 'public' | 'abstract' | 'hide' | 'final' | 'static'
                       | 'readonly' | 'outer' | 'execution' | 'request' | 'session' | 'application'
                       | 'void' | 'enhancement' | 'classpath' | 'typeloader' | 'annotation' | 'reified'
                       ;

idAll : id
      | 'and' | 'or' | 'not' | 'in' | 'var' | 'delegate' | 'represents'
      | 'typeof' | 'statictypeof' | 'typeis' | 'typeas'
      | 'package' | 'uses' | 'if' | 'else' | 'unless' | 'foreach' | 'for' | 'while' | 'do'
      | 'continue' | 'break' | 'return' | 'construct' | 'function' | 'property'
      | 'try' | 'catch' | 'finally' | 'throw' | 'new' | 'switch' | 'case' | 'default' | 'eval'
      | 'override' | 'extends' | 'transient' | 'implements'
      | 'class' | 'interface' | 'structure' | 'enum' | 'using'
      ;

// ---------------------------------------------------------------------------------------------
// Lexer
// ---------------------------------------------------------------------------------------------

Ident : Letter (Letter | Digit)* ;

NumberLiteral : HexLiteral
              | BinLiteral
              | IntOrFloatPointLiteral
              ;

fragment BinLiteral : ('0b' | '0B') [01]+ IntegerTypeSuffix? ;

fragment HexLiteral : ('0x' | '0X') HexDigit+ [sSlL]? ;

// "1." is a float, but in "1..10" and "1.toString()" the dot is not part of the number.
fragment IntOrFloatPointLiteral
  : '.' Digit+ Exponent? FloatTypeSuffix?
  | Digit+ '.' Digit+ Exponent? FloatTypeSuffix?
  | Digit+ '.' {_input.LA(1) != '.' && !Character.isLetter(_input.LA(1))}?
  | Digit+ Exponent FloatTypeSuffix?
  | Digit+ FloatTypeSuffix
  | Digit+ IntegerTypeSuffix
  | Digit+
  ;

CharLiteral : '\'' (EscapeSequence | ~['\\\r\n]) '\'' ;

StringLiteral : DqString | SqString ;

// Template expressions ${...} may contain strings, which may contain templates: "${ "${42}" }"
fragment DqString : '"' (EscapeSequence | ~[\\"$\r\n] | '$' TemplateBody | '$' ~[{"\r\n])* ('"' | '$"') ;

fragment SqString : '\'' (EscapeSequence | ~[\\'$\r\n] | '$' TemplateBody | '$' ~[{'\r\n])* ('\'' | '$\'') ;

fragment TemplateBody : '{' (DqString | SqString | TemplateBody | ~[{}"'])* '}' ;

fragment HexDigit : [0-9a-fA-F] ;

// r/R = gw.util.Rational
fragment IntegerTypeSuffix : 'bi' | 'BI' | [lLsSbBrR] ;

fragment FloatTypeSuffix : 'bd' | 'BD' | [fFdDrR] ;

fragment Letter : [\p{L}_$] ;

fragment Digit : [0-9] ;

fragment Exponent : [eE] [+-]? Digit+ ;

// A backslash before a line break continues the string on the next line.
fragment EscapeSequence
  : '\\' [vabtnfr"'\\$<]
  | '\\' ('\r'? '\n' | '\r')
  | UnicodeEscape
  | OctalEscape
  ;

fragment OctalEscape
  : '\\' [0-3] [0-7] [0-7]
  | '\\' [0-7] [0-7]
  | '\\' [0-7]
  ;

fragment UnicodeEscape : '\\' 'u' HexDigit HexDigit HexDigit HexDigit ;

HASHBANG : '#!' ~[\r\n]* -> channel(HIDDEN) ;

WS : [ \t\r\n\f]+ -> skip ;

COMMENT : '/*' .*? '*/' -> channel(HIDDEN) ;

LINE_COMMENT : '//' ~[\r\n]* -> channel(HIDDEN) ;

// Anything else is handed to the parser, which reports it as a syntax error.
ErrorChar : . ;
