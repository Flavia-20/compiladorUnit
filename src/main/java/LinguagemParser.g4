parser grammar LinguagemParser;
@header {
    package antlr;
}
options { tokenVocab=meuLexico; }

@members {
    private static final long LIMITE_CTE_SEM_SINAL = Short.MAX_VALUE;
    private static final long LIMITE_CTE_COM_SINAL_NEGATIVO = -(long) Short.MIN_VALUE;

    private void validarConstanteSemSinal(Token numero) {
        long valor = Long.parseLong(numero.getText());

        if (valor > LIMITE_CTE_SEM_SINAL) {
            erroConstante(numero);
        }
    }

    private void validarConstanteAssinada(Token sinal, Token numero) {
        long valor = Long.parseLong(numero.getText());
        long limite = sinal.getText().equals("-")
            ? LIMITE_CTE_COM_SINAL_NEGATIVO
            : LIMITE_CTE_SEM_SINAL;

        if (valor > limite) {
            erroConstante(numero);
        }
    }

    private void erroConstante(Token numero) {
        throw new RuntimeException(
            "Erro Lexico: Constante " + numero.getText() +
            " excede 2 bytes na linha " + numero.getLine() +
            ", coluna " + numero.getCharPositionInLine() + "."
        );
    }
}

prog: PROGRAM ID PVIG decls cmdComp PONTO EOF;

decls: VAR listDecl | ;
listDecl: declTip+;
declTip: listId DPONTOS tip PVIG;
listId: ID (VIG ID)*;
tip: INTEGER | BOOLEAN | STRING;

cmdComp: BEGIN listCmd? END;
listCmd: cmd (PVIG cmd)* PVIG?;

cmd: cmdCasado | cmdAberto;

cmdCasado
    : cmdBasico
    | IF expr THEN cmdCasado ELSE cmdCasado
    | WHILE expr DO cmdCasado
    ;

cmdAberto
    : IF expr THEN cmd
    | IF expr THEN cmdCasado ELSE cmdAberto
    | WHILE expr DO cmdAberto
    ;

cmdBasico: cmdRead | cmdWrite | cmdAtrib | cmdComp;

cmdRead: READ ABPAR listId FPAR;
cmdWrite: WRITE ABPAR listW FPAR;
listW: elemW (VIG elemW)*;
elemW: expr;

cmdAtrib: ID ATRIB expr;

expr: CADEIA | exprLogica;
exprLogica: exprRel (OPLOG exprRel)*;
exprRel: exprArit (OPREL exprArit)?;
exprArit: termo (OPAD termo)*;
termo: fator (OPMULT fator)*;

fator: OPNEG fator
     | cteAssinada
     | fatorPrimario
     ;

cteAssinada: sinal=OPAD numero=CTE { validarConstanteAssinada($sinal, $numero); };

fatorPrimario
     : ID
     | numero=CTE { validarConstanteSemSinal($numero); }
     | TRUE
     | FALSE
     | ABPAR exprLogica FPAR
     ;
