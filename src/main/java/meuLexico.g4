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
                erroOverflowConstante();
            }
        } catch (NumberFormatException e) {
            erroOverflowConstante();
        }
    }

    private void erroOverflowConstante() {
        throw new RuntimeException(
            "Erro Semantico: overflow de constante inteira na linha " + getLine() +
            ", coluna " + _tokenStartCharPositionInLine +
            ". Valor fora do intervalo de 2 bytes com sinal (-32768 a 32767)."
        );
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
    String textoOriginal = getText();
    String textoNormalizado = textoOriginal.toLowerCase();

    if (textoNormalizado.length() > 16) {
        textoNormalizado = textoNormalizado.substring(0, 16);
        System.err.println("Aviso: identificador '" + textoOriginal
            + "' truncado para '" + textoNormalizado + "'.");
    }

    setText(textoNormalizado);
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
