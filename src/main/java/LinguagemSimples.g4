parser grammar LinguagemSimples;
options { tokenVocab=lexicoSimples; }

// Ela diz: "Aceite qualquer um desses tokens, várias vezes, até o fim do arquivo (EOF)"
prog: (ID | CTE | CADEIA | PROGRAM | BEGIN | END | VAR | WRITE | READ | WHILE | IF | THEN | ATRIB | OPAD | OPMULT | OPREL | PVIG | PONTO | VIG)* EOF ;

