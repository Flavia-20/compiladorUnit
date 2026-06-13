package org.example.compilador.intermediario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import antlr.LinguagemParser;
import antlr.LinguagemParserBaseVisitor;
import org.example.compilador.intermediario.CodigoIntermediario.InstrucaoTAC;
import org.example.compilador.semantico.DadosSemanticos.ResultadoSemantico;
import org.example.compilador.semantico.DadosSemanticos.TabelaSimbolos;
import org.example.compilador.semantico.DadosSemanticos.Tipo;

public class GeradorCodigoIntermediario extends LinguagemParserBaseVisitor<String> {
    private final TabelaSimbolos tabelaSimbolos;
    private final List<InstrucaoTAC> instrucoes = new ArrayList<>();
    private final Map<String, Tipo> tiposTemporarios = new HashMap<>();
    private int contadorTemp;
    private int contadorRotulo;

    public GeradorCodigoIntermediario(ResultadoSemantico resultadoSemantico) {
        this.tabelaSimbolos = resultadoSemantico.getTabelaSimbolos();
    }

    public CodigoIntermediario gerar(LinguagemParser.ProgContext arvore) {
        visit(arvore);
        return new CodigoIntermediario(tabelaSimbolos, instrucoes, tiposTemporarios);
    }

    @Override
    public String visitProg(LinguagemParser.ProgContext ctx) {
        visit(ctx.cmdComp());
        return null;
    }

    @Override
    public String visitCmd(LinguagemParser.CmdContext ctx) {
        if (ctx.cmdCasado() != null) {
            visit(ctx.cmdCasado());
        } else if (ctx.cmdAberto() != null) {
            visit(ctx.cmdAberto());
        }

        return null;
    }

    @Override
    public String visitCmdCasado(LinguagemParser.CmdCasadoContext ctx) {
        if (ctx.cmdBasico() != null) {
            visit(ctx.cmdBasico());
            return null;
        }

        if (ctx.IF() != null) {
            String condicao = visit(ctx.expr());
            String rotuloElse = novoRotulo();
            String rotuloFim = novoRotulo();

            emitir(InstrucaoTAC.seFalso(condicao, rotuloElse));
            visit(ctx.cmdCasado(0));
            emitir(InstrucaoTAC.desvio(rotuloFim));
            emitir(InstrucaoTAC.rotulo(rotuloElse));
            visit(ctx.cmdCasado(1));
            emitir(InstrucaoTAC.rotulo(rotuloFim));
            return null;
        }

        if (ctx.WHILE() != null) {
            String rotuloInicio = novoRotulo();
            String rotuloFim = novoRotulo();

            emitir(InstrucaoTAC.rotulo(rotuloInicio));
            String condicao = visit(ctx.expr());
            emitir(InstrucaoTAC.seFalso(condicao, rotuloFim));
            visit(ctx.cmdCasado(0));
            emitir(InstrucaoTAC.desvio(rotuloInicio));
            emitir(InstrucaoTAC.rotulo(rotuloFim));
        }

        return null;
    }

    @Override
    public String visitCmdAberto(LinguagemParser.CmdAbertoContext ctx) {
        if (ctx.WHILE() != null) {
            String rotuloInicio = novoRotulo();
            String rotuloFim = novoRotulo();

            emitir(InstrucaoTAC.rotulo(rotuloInicio));
            String condicao = visit(ctx.expr());
            emitir(InstrucaoTAC.seFalso(condicao, rotuloFim));
            visit(ctx.cmdAberto());
            emitir(InstrucaoTAC.desvio(rotuloInicio));
            emitir(InstrucaoTAC.rotulo(rotuloFim));
            return null;
        }

        String condicao = visit(ctx.expr());

        if (ctx.ELSE() != null) {
            String rotuloElse = novoRotulo();
            String rotuloFim = novoRotulo();

            emitir(InstrucaoTAC.seFalso(condicao, rotuloElse));
            visit(ctx.cmdCasado());
            emitir(InstrucaoTAC.desvio(rotuloFim));
            emitir(InstrucaoTAC.rotulo(rotuloElse));
            visit(ctx.cmdAberto());
            emitir(InstrucaoTAC.rotulo(rotuloFim));
            return null;
        }

        String rotuloFim = novoRotulo();
        emitir(InstrucaoTAC.seFalso(condicao, rotuloFim));
        visit(ctx.cmd());
        emitir(InstrucaoTAC.rotulo(rotuloFim));
        return null;
    }

    @Override
    public String visitCmdRead(LinguagemParser.CmdReadContext ctx) {
        for (int i = 0; i < ctx.listId().ID().size(); i++) {
            emitir(InstrucaoTAC.leitura(ctx.listId().ID(i).getText()));
        }

        return null;
    }

    @Override
    public String visitCmdWrite(LinguagemParser.CmdWriteContext ctx) {
        for (LinguagemParser.ElemWContext elem : ctx.listW().elemW()) {
            emitir(InstrucaoTAC.escrita(visit(elem.expr())));
        }

        return null;
    }

    @Override
    public String visitCmdAtrib(LinguagemParser.CmdAtribContext ctx) {
        String valor = visit(ctx.expr());
        emitir(InstrucaoTAC.atribuicao(ctx.ID().getText(), valor));
        return null;
    }

    @Override
    public String visitExpr(LinguagemParser.ExprContext ctx) {
        if (ctx.CADEIA() != null) {
            return ctx.CADEIA().getText();
        }

        return visit(ctx.exprLogica());
    }

    @Override
    public String visitExprLogica(LinguagemParser.ExprLogicaContext ctx) {
        String atual = visit(ctx.exprRel(0));

        for (int i = 0; i < ctx.OPLOG().size(); i++) {
            String direita = visit(ctx.exprRel(i + 1));
            String temp = novoTemp(Tipo.BOOLEAN);

            emitir(InstrucaoTAC.binaria(temp, atual, ctx.OPLOG(i).getText().toUpperCase(), direita));
            atual = temp;
        }

        return atual;
    }

    @Override
    public String visitExprRel(LinguagemParser.ExprRelContext ctx) {
        String esquerda = visit(ctx.exprArit(0));

        if (ctx.OPREL() == null) {
            return esquerda;
        }

        String direita = visit(ctx.exprArit(1));
        String temp = novoTemp(Tipo.BOOLEAN);
        emitir(InstrucaoTAC.binaria(temp, esquerda, ctx.OPREL().getText(), direita));
        return temp;
    }

    @Override
    public String visitExprArit(LinguagemParser.ExprAritContext ctx) {
        String atual = visit(ctx.termo(0));

        for (int i = 0; i < ctx.OPAD().size(); i++) {
            String direita = visit(ctx.termo(i + 1));
            String temp = novoTemp(Tipo.INTEGER);

            emitir(InstrucaoTAC.binaria(temp, atual, ctx.OPAD(i).getText(), direita));
            atual = temp;
        }

        return atual;
    }

    @Override
    public String visitTermo(LinguagemParser.TermoContext ctx) {
        String atual = visit(ctx.fator(0));

        for (int i = 0; i < ctx.OPMULT().size(); i++) {
            String direita = visit(ctx.fator(i + 1));
            String temp = novoTemp(Tipo.INTEGER);

            emitir(InstrucaoTAC.binaria(temp, atual, ctx.OPMULT(i).getText(), direita));
            atual = temp;
        }

        return atual;
    }

    @Override
    public String visitFator(LinguagemParser.FatorContext ctx) {
        if (ctx.OPNEG() != null) {
            String temp = novoTemp(Tipo.BOOLEAN);
            emitir(InstrucaoTAC.unaria(temp, ctx.OPNEG().getText(), visit(ctx.fator())));
            return temp;
        }

        if (ctx.cteAssinada() != null) {
            return visit(ctx.cteAssinada());
        }

        return visit(ctx.fatorPrimario());
    }

    @Override
    public String visitCteAssinada(LinguagemParser.CteAssinadaContext ctx) {
        if (ctx.sinal.getText().equals("-")) {
            return "-" + ctx.numero.getText();
        }

        return ctx.numero.getText();
    }

    @Override
    public String visitFatorPrimario(LinguagemParser.FatorPrimarioContext ctx) {
        if (ctx.ID() != null) {
            return ctx.ID().getText();
        }

        if (ctx.CTE() != null) {
            return ctx.CTE().getText();
        }

        if (ctx.TRUE() != null) {
            return "true";
        }

        if (ctx.FALSE() != null) {
            return "false";
        }

        return visit(ctx.exprLogica());
    }

    private String novoTemp(Tipo tipo) {
        String temp = "t_" + contadorTemp++;
        tiposTemporarios.put(temp, tipo);
        return temp;
    }

    private String novoRotulo() {
        return "L_" + contadorRotulo++;
    }

    private void emitir(InstrucaoTAC instrucao) {
        instrucoes.add(instrucao);
    }
}
