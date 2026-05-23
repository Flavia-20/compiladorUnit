parser grammar LinguagemParser;
@header {
    package antrl;
}
options { tokenVocab=meuLexico; }

// Ela diz: "Aceite qualquer um desses tokens, várias vezes, até o fim do arquivo (EOF)"
//prog: (ID | CTE | CADEIA | PROGRAM | BEGIN | END | VAR | WRITE | READ | WHILE | IF | THEN | ATRIB | OPAD | OPMULT | OPREL | PVIG | PONTO | VIG)* EOF ;
prog: PROGRAM ID PVIG decls cmdComp PONTO;
decls:  /* vazio */ | VAR listDecl;
listDecl: declTip | declTip listDecl;
declTip: listId DPONTOS tip PVIG;
listId: ID | ID VIG listId;
tip: INTEGER | BOOLEAN | CADEIA;

cmdComp: BEGIN  listCmd END;
listCmd: cmd | cmd PVIG listCmd;
cmd: cmdIf | cmdWhile | cmdRead | cmdWrite | cmdAtrib | cmdComp;

cmdIf: IF expr THEN cmd | IF expr THEN cmd ELSE cmd;

cmdWhile: WHILE expr DO cmd;

cmdRead: READ ABPAR listId FPAR;
cmdWrite: WRITE ABPAR listW FPAR;
listW: elemW | elemW VIG listW;
elemW: expr | CADEIA;

cmdAtrib: ID ATRIB expr;

//expr: expr OPREL expr | expr OPAD expr | expr OPMULT | expr;
//expr: ID | CTE | ABPAR expr FPAR | TRUE | FALSE | OPNEG expr;


// Regra principal de Expressão (com a precedência matemática correta)
expr: expr OPMULT expr   // Prioridade 1: Multiplicação/Divisão
    | expr OPAD expr     // Prioridade 2: Soma/Subtração
    | expr OPREL expr    // Prioridade 3: Comparações (>, <, ==)
    | fator              // Prioridade 4: Os valores básicos
    ;

// Regra auxiliar 'fator' para resolver o problema do OPNEG que a professora pediu
fator: OPNEG fator       // O OPNEG só pode ser aplicado aos itens abaixo
     | ID
     | CTE
     | TRUE
     | FALSE
     | ABPAR expr FPAR
     ;
