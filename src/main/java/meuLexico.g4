lexer grammar meuLexico;
@header {
    package antlr;
}

options { caseInsensitive = true; }

@members {
    private static final long LIMITE_CTE_COM_SINAL_NEGATIVO = -(long) Short.MIN_VALUE;

    private void validarConstanteBruta() {
        try {
            long valor = Long.parseLong(getText());

            if (valor > LIMITE_CTE_COM_SINAL_NEGATIVO) {
                erroLexico("Constante " + getText() + " excede 2 bytes");
            }
        } catch (NumberFormatException e) {
            erroLexico("Constante " + getText() + " excede 2 bytes");
        }
    }

    private void erroLexico(String mensagem) {
        throw new RuntimeException(
            "Erro Lexico: " + mensagem +
            " na linha " + getLine() +
            ", coluna " + _tokenStartCharPositionInLine + "."
        );
    }
}

// Comentarios de linha
COMMENT: '//' ~[\r\n]* -> skip;

// Espacos em branco entre tokens
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


// Identificadores com no maximo 16 caracteres
ID: [a-z][a-z0-9]* {
    if (getText().length() > 16) {
     setText(getText().substring(0, 16));
    }
} ;

// O sinal fica na gramatica sintatica como OPAD CTE, para nao quebrar 2+3.
CTE: [0-9]+ { validarConstanteBruta(); };

CADEIA: '"' ~["\r\n]* '"';

CADEIA_NAO_FECHADA: '"' ~["\r\n]* {
    erroLexico("cadeia nao fechada");
};

CARACTERE_INVALIDO: . {
    erroLexico("caractere invalido '" + getText() + "'");
};
