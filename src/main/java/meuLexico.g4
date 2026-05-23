lexer grammar meuLexico;
@header {
    package antlr;
}

options { caseInsensitive = true; }
// Ignora  comentários (/ comentario /)
COMMENT: '//' ~[\r\n]* -> skip;
// 3. Regra para ignorar espaços em branco (O PDF do seu projeto exige isso)
WS: [ \t\r\n]+ -> skip ;

// Regras para tokens
PROGRAM: 'PROGRAM';
INTEGER: 'INTEGER';
BOOLEAN: 'BOOLEAN';
BEGIN: 'BEGIN';
END: 'END';
WHILE: 'WHILE';
DO: 'DO';
READ: 'READ';
VAR: 'VAR';
FALSE: 'FALSE';
TRUE: 'TRUE';
WRITE: 'WRITE';
STRING: 'STRING';

IF: 'IF';
THEN: 'THEN';
ELSE: 'ELSE';


//REGRAS OPERADORES
OPREL: '<' | '<=' | '>' | '>=' | '==' | '<>';
OPAD: '+' | '-';
OPMULT: '*' | '/';
OPLOG: 'OR' | 'AND';
OPNEG: '~';


//REGRAS PONTUACOES
ATRIB: ':=';
PVIG: ';';
PONTO: '.';
DPONTOS: ':';
VIG: ',';
ABPAR: '(';
FPAR: ')';


//NUMERO_INTEIRO: ('+' | '-')?[0-9]+;
//PARA NÃO ULTRAPASSAR OS 16 CARACTERES
ID: [a-z][a-z0-9]* {
    if (getText().length() > 16) {
     setText(getText().substring(0, 16));
    }
} ;
CTE: ('+' | '-')? [0-9]+ {
    try {
        int valor = Integer.parseInt(getText());

        if (valor < -32768 || valor > 32767) {
            System.err.println("Erro Léxico: Constante " + getText() + " excede 2 bytes!");
        }
    } catch(NumberFormatException e) {
        System.err.println("Erro Léxico: Constante " + getText() + " excede 2 bytes!");
    }
};

CADEIA: '"' .*? '"';

