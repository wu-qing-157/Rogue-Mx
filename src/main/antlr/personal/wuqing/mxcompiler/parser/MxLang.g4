grammar MxLang;

StringConstant: '"' (~["\t\b\n\r\f\\] | '\\' [tbnrf\\"])* '"';
NotationSingleLine: '//' .*? ('\n'|EOF) -> skip;
NotationMultiline: '/*' .*? '*/' -> skip;

BlankCharacter : [ \r\t\n]+ -> skip;

Class: 'class';
Bool: 'bool';
Int: 'int';
String: 'string';
Null: 'null';
This: 'this';
True: 'true';
False: 'false';
Void: 'void';
If: 'if';
Else: 'else';
For: 'for';
While: 'while';
Return: 'return';
Break: 'break';
Continue: 'continue';
New: 'new';

// keyword: Bool|Int|String|Null|True|False|Void|If|Else|For|While|Return|Break|Continue;

IntegerConstant: [0-9]+;
nullConstant: Null;
boolConstant: True|False;
Identifier: [a-zA-Z_\u0080-\uffff][0-9a-zA-Z_\u0080-\uffff]*;

constant: IntegerConstant|StringConstant|nullConstant|boolConstant;

simpleType: Bool|Int|String|Void|Identifier;

arrayType: simpleType ('[]')+;

type: simpleType|arrayType;

expression: '('expression')' #Parentheses
    | expression '.' Identifier #MemberAccess
    | New simpleType ('[' expression? ']')* #NewOperator
    | expression '(' expressionList ')' #FunctionCall
    | expression '[' expression ']' #IndexAccess
    | expression ('++'|'--') #SuffixUnaryOperator
    | <assoc=right> ('++'|'--'|'+'|'-'|'!'|'~') expression #PrefixUnaryOperator
    | expression op=('*'|'/'|'%') expression #BinaryOperator
    | expression op=('+'|'-') expression #BinaryOperator
    | expression op=('<<'|'>>'|'>>>') expression #BinaryOperator
    | expression op=('<'|'>'|'<='|'>=') expression #BinaryOperator
    | expression op=('=='|'!=') expression #BinaryOperator
    | expression op='&' expression #BinaryOperator
    | expression op='^' expression #BinaryOperator
    | expression op='|' expression #BinaryOperator
    | expression op='&&' expression #BinaryOperator
    | expression op='||' expression #BinaryOperator
    | <assoc=right> expression '?' expression':' expression #TernaryOperator
    | <assoc=right> expression op=('='|'+='|'-='|'*='|'/='|'%='|'&='|'^='|'|='|'<<='|'>>='|'>>>=') expression
        #BinaryOperator
    | Identifier #Identifiers
    | constant #Constants;

expressionList: (expression(','expression)*)?;

statement: block #BlockStatement
    | expression ';' #ExpressionStatement
    | variableDeclaration #VariableDeclarationStatement
    | Return expression ';' #ReturnStatement
    | Break ';' #BreakStatement
    | Continue ';' #ContinueStatement
    | If '(' expression ')' statement #IfStatement
    | If '(' expression ')' thenStatement=statement Else elseStatement=statement #IfElseStatement
    | While '(' expression ')' statement #WhileStatement
    | For '(' (initExpression=expression|initVariableDeclaration=variableDeclaration) condition=expression ';'
        step=expression? ')' statement #ForStatement;

block: '{' statement* '}';

functionDeclaration: type Identifier '(' (parameter (',' parameter)*)? ')' block;

parameter: type Identifier;

classDeclaration: Class Identifier '{' variableDeclaration* '}';

variableDeclaration: type variable (',' variable)* ';';

variable: Identifier ('=' expression)?;

section: classDeclaration|functionDeclaration|variableDeclaration;

program: section* EOF;
