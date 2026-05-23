parser grammar LinguagemParser;
options { tokenVocab=meuLexico; }

// Ela diz: "Aceite qualquer um desses tokens, várias vezes, até o fim do arquivo (EOF)"
prog: (ID | CTE | CADEIA | PROGRAM | BEGIN | END | VAR | WRITE | READ | WHILE | IF | THEN | ATRIB | OPAD | OPMULT | OPREL | PVIG | PONTO | VIG)* EOF ;
//prog: PROGRAM ID PVIG Decls CmdComp Ponto;
//decls:  /* vazio */ | | VAR listDecl;
/*listDecl:;
decltTip:;
listId:;
tip:;

cmdCop:;
listCmd:;
cmd:;

//cmdIf:

cmdWhile:;

cmdRead:;
cmdWhrite:;
listW:;
elemW:;

cmdAtrib:;

expr:;*/
//expr:;