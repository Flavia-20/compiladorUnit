package org.example;

import antlr.LinguagemParser;
import antlr.LinguagemParserBaseVisitor;
import java.util.HashMap;
import java.util.Map;

public class SemanticoVisitor extends LinguagemParserBaseVisitor<String> {

    private Map<String, String> tabelaSimbolos = new HashMap<>();

    @Override
    public String visitDeclTip(LinguagemParser.DeclTipContext ctx) {
        String tipo = ctx.tip().getText();
        LinguagemParser.ListIdContext listIdCtx = ctx.listId();

        while (listIdCtx != null) {
            String nomeVariavel = listIdCtx.ID().getText();

            if (tabelaSimbolos.containsKey(nomeVariavel)) {
                throw new RuntimeException("Erro Semântico: A variável '" + nomeVariavel + "' já foi declarada anteriormente!");
            }

            tabelaSimbolos.put(nomeVariavel, tipo);
            listIdCtx = listIdCtx.listId();
        }

        return null;
    }

    @Override
    public String visitCmdAtrib(LinguagemParser.CmdAtribContext ctx) {
        String nomeVariavel = ctx.ID().getText();

        if (!tabelaSimbolos.containsKey(nomeVariavel)) {
            throw new RuntimeException("Erro Semântico: Uso de variável não declarada '" + nomeVariavel + "'.");
        }

        String tipoVariavel = tabelaSimbolos.get(nomeVariavel);
        String tipoExpressao = visit(ctx.expr());

        if (!tipoVariavel.equals(tipoExpressao)) {
            throw new RuntimeException("Erro Semântico: Incompatibilidade de tipos! A variável '" +
                    nomeVariavel + "' é do tipo " + tipoVariavel +
                    ", mas tentou receber um valor do tipo " + tipoExpressao + ".");
        }

        return null;
    }

    @Override
    public String visitCmdRead(LinguagemParser.CmdReadContext ctx) {
        LinguagemParser.ListIdContext listIdCtx = ctx.listId();

        while (listIdCtx != null) {
            String nomeVariavel = listIdCtx.ID().getText();

            if (!tabelaSimbolos.containsKey(nomeVariavel)) {
                throw new RuntimeException("Erro Semântico: variável '" + nomeVariavel + "' usada no READ não foi declarada.");
            }

            listIdCtx = listIdCtx.listId();
        }

        return null;
    }

    @Override
    public String visitExpr(LinguagemParser.ExprContext ctx) {
        if (ctx.fator() != null) {
            return visit(ctx.fator());
        }

        String tipoEsquerda = visit(ctx.expr(0));
        String tipoDireita = visit(ctx.expr(1));

        if (ctx.OPAD() != null || ctx.OPMULT() != null) {
            if (!tipoEsquerda.equals("INTEGER") || !tipoDireita.equals("INTEGER")) {
                throw new RuntimeException("Erro Semântico: Operação aritmética exige valores INTEGER.");
            }
            return "INTEGER";
        }

        if (ctx.OPREL() != null) {
            if (!tipoEsquerda.equals(tipoDireita)) {
                throw new RuntimeException("Erro Semântico: Comparação entre tipos incompatíveis.");
            }
            return "BOOLEAN";
        }

        if (ctx.OPLOG() != null) {
            if (!tipoEsquerda.equals("BOOLEAN") || !tipoDireita.equals("BOOLEAN")) {
                throw new RuntimeException("Erro Semântico: Operação lógica exige valores BOOLEAN.");
            }
            return "BOOLEAN";
        }

        return null;
    }

    @Override
    public String visitFator(LinguagemParser.FatorContext ctx) {
        if (ctx.TRUE() != null || ctx.FALSE() != null) {
            return "BOOLEAN";
        }

        if (ctx.CTE() != null) {
            validarConstanteInteira(ctx);
            return "INTEGER";
        }

        if (ctx.ID() != null) {
            String nome = ctx.ID().getText();

            if (!tabelaSimbolos.containsKey(nome)) {
                throw new RuntimeException("Erro Semântico: Variável não declarada usada na expressão: '" + nome + "'.");
            }

            return tabelaSimbolos.get(nome);
        }

        if (ctx.ABPAR() != null) {
            return visit(ctx.expr());
        }

        if (ctx.OPNEG() != null) {
            String tipo = visit(ctx.fator());

            if (!tipo.equals("BOOLEAN")) {
                throw new RuntimeException("Erro Semântico: operador ~ exige valor BOOLEAN.");
            }

            return "BOOLEAN";
        }

        return null;
    }

    @Override
    public String visitCmdIf(LinguagemParser.CmdIfContext ctx) {
        String tipoCondicao = visit(ctx.expr());

        if (!tipoCondicao.equals("BOOLEAN")) {
            throw new RuntimeException("Erro Semântico: condição do IF deve ser BOOLEAN.");
        }

        visit(ctx.cmd(0));

        if (ctx.ELSE() != null) {
            visit(ctx.cmd(1));
        }

        return null;
    }

    @Override
    public String visitCmdWhile(LinguagemParser.CmdWhileContext ctx) {
        String tipoCondicao = visit(ctx.expr());

        if (!tipoCondicao.equals("BOOLEAN")) {
            throw new RuntimeException("Erro Semântico: condição do WHILE deve ser BOOLEAN.");
        }

        visit(ctx.cmd());

        return null;
    }

    private void validarConstanteInteira(LinguagemParser.FatorContext ctx) {
        String texto = ctx.CTE().getText();
        boolean negativo = ctx.OPAD() != null && ctx.OPAD().getText().equals("-");

        try {
            long valor = Long.parseLong(texto);
            long limite = negativo ? 32768 : 32767;

            if (valor > limite) {
                String sinal = negativo ? "-" : "";
                throw new RuntimeException(
                        "Erro Léxico: Constante " + sinal + texto + " excede 2 bytes."
                );
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException(
                    "Erro Léxico: Constante " + texto + " excede 2 bytes."
            );
        }
    }
}