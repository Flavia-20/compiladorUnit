package org.example;

import antlr.LinguagemParser;
import antlr.LinguagemParserBaseVisitor;

public class GeradorCodigo extends LinguagemParserBaseVisitor<String> {

    private final StringBuilder codigo = new StringBuilder();

    public String getCodigoGerado() {
        return codigo.toString();
    }

    @Override
    public String visitProg(LinguagemParser.ProgContext ctx) {
        codigo.append("INICIO_PROGRAMA ").append(ctx.ID().getText()).append("\n");

        visit(ctx.decls());
        visit(ctx.cmdComp());

        codigo.append("FIM_PROGRAMA\n");

        return null;
    }

    @Override
    public String visitDeclTip(LinguagemParser.DeclTipContext ctx) {
        String tipo = ctx.tip().getText();
        LinguagemParser.ListIdContext listIdCtx = ctx.listId();

        while (listIdCtx != null) {
            String nomeVariavel = listIdCtx.ID().getText();
            codigo.append("DECLARAR ").append(nomeVariavel).append(" ").append(tipo).append("\n");

            listIdCtx = listIdCtx.listId();
        }

        return null;
    }

    @Override
    public String visitCmdAtrib(LinguagemParser.CmdAtribContext ctx) {
        String variavel = ctx.ID().getText();
        String expressao = visit(ctx.expr());

        codigo.append("ATRIBUIR ").append(variavel).append(" ").append(expressao).append("\n");

        return null;
    }

    @Override
    public String visitCmdRead(LinguagemParser.CmdReadContext ctx) {
        LinguagemParser.ListIdContext listIdCtx = ctx.listId();

        while (listIdCtx != null) {
            String nomeVariavel = listIdCtx.ID().getText();
            codigo.append("LER ").append(nomeVariavel).append("\n");

            listIdCtx = listIdCtx.listId();
        }

        return null;
    }

    @Override
    public String visitCmdWrite(LinguagemParser.CmdWriteContext ctx) {
        LinguagemParser.ListWContext listWCtx = ctx.listW();

        while (listWCtx != null) {
            String valor = visit(listWCtx.elemW());
            codigo.append("ESCREVER ").append(valor).append("\n");

            listWCtx = listWCtx.listW();
        }

        return null;
    }

    @Override
    public String visitElemW(LinguagemParser.ElemWContext ctx) {
        if (ctx.CADEIA() != null) {
            return ctx.CADEIA().getText();
        }

        return visit(ctx.expr());
    }

    @Override
    public String visitCmdIf(LinguagemParser.CmdIfContext ctx) {
        String condicao = visit(ctx.expr());

        codigo.append("SE ").append(condicao).append(" ENTAO\n");

        visit(ctx.cmd(0));

        if (ctx.ELSE() != null) {
            codigo.append("SENAO\n");
            visit(ctx.cmd(1));
        }

        codigo.append("FIM_SE\n");

        return null;
    }

    @Override
    public String visitCmdWhile(LinguagemParser.CmdWhileContext ctx) {
        String condicao = visit(ctx.expr());

        codigo.append("ENQUANTO ").append(condicao).append(" FACA\n");

        visit(ctx.cmd());

        codigo.append("FIM_ENQUANTO\n");

        return null;
    }

    @Override
    public String visitExpr(LinguagemParser.ExprContext ctx) {
        if (ctx.fator() != null) {
            return visit(ctx.fator());
        }

        String esquerda = visit(ctx.expr(0));
        String direita = visit(ctx.expr(1));

        if (ctx.OPAD() != null) {
            return "(" + esquerda + " " + ctx.OPAD().getText() + " " + direita + ")";
        }

        if (ctx.OPMULT() != null) {
            return "(" + esquerda + " " + ctx.OPMULT().getText() + " " + direita + ")";
        }

        if (ctx.OPREL() != null) {
            return "(" + esquerda + " " + ctx.OPREL().getText() + " " + direita + ")";
        }

        if (ctx.OPLOG() != null) {
            return "(" + esquerda + " " + ctx.OPLOG().getText() + " " + direita + ")";
        }

        return "";
    }

    @Override
    public String visitFator(LinguagemParser.FatorContext ctx) {
        if (ctx.ID() != null) {
            return ctx.ID().getText();
        }

        if (ctx.CTE() != null) {
            return ctx.CTE().getText();
        }

        if (ctx.TRUE() != null) {
            return "TRUE";
        }

        if (ctx.FALSE() != null) {
            return "FALSE";
        }

        if (ctx.OPNEG() != null) {
            return "~" + visit(ctx.fator());
        }

        if (ctx.ABPAR() != null) {
            return "(" + visit(ctx.expr()) + ")";
        }

        return "";
    }
}