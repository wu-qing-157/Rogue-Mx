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

type: simpleType ('[]')*;

expression: '('expression')' #Parentheses
    | expression '.' Identifier #MemberAccess
    | New simpleType ('[' expression? ']')* #NewAccess
    | expression '(' expressionList ')' #FunctionCall
    | expression '[' expression ']' #IndexAccess
    | expression ('++'|'--') #SuffixOperator
    | <assoc=right> ('++'|'--'|'+'|'-'|'!'|'~') expression #PrefixOperator
    | expression op=('*'|'/'|'%') expression #MultiOperator
    | expression op=('+'|'-') expression #PlusOperator
    | expression op=('<<'|'>>'|'>>>') expression #ShiftOperator
    | expression op=('<'|'>'|'<='|'>=') expression #CompareOperator
    | expression op=('=='|'!=') expression #EqualOperator
    | expression '&' expression #NumericalAndOperator
    | expression '^' expression #NumericalXorOperator
    | expression '|' expression #NumericalOrOperator
    | expression '&&' expression #LogicalAndOperator
    | expression '||' expression #BinaryOperator
    | <assoc=right> expression '?' expression':' expression #TernaryOperator
    | <assoc=right> expression op=('='|'+='|'-='|'*='|'/='|'%='|'&='|'^='|'|='|'<<='|'>>='|'>>>=') expression #AssignOperator
    | Identifier #Identifiers
    | constant #Constants;

expressionList: (expression(','expression)*)?;

statement: expression ';' #SimpleStatement
    | (Return expression?|Break|Continue) ';' #ControlStatement
    | If '(' expression ')' (statement|block) #IfStatement
    | If '(' expression ')' (statement|block) Else (statement|block) #IfElseStatement
    | While '(' expression ')' (statement|block) #WhileStatement
    | For '(' variableDefinition expression? ';' expression ')' (statement|block) #ForStatement;

block: '{' (statement|variableDefinition)* '}';

functionDefinition: type Identifier '(' parameterList ')' block;

parameterList: (type Identifier(',' type Identifier)*)?;

classDefinition: Class Identifier '{' variableDefinition* '}';

variableDefinition: type Identifier ('=' expression)? (',' Identifier ('=' expression)?)* ';';

program: (classDefinition | functionDefinition | variableDefinition)* EOF;
