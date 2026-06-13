package org.example.compilador.semantico;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import antlr.LinguagemParser;
import antlr.LinguagemParserBaseVisitor;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.example.compilador.semantico.DadosSemanticos.ResultadoSemantico;
import org.example.compilador.semantico.DadosSemanticos.Simbolo;
import org.example.compilador.semantico.DadosSemanticos.TabelaSimbolos;
import org.example.compilador.semantico.DadosSemanticos.Tipo;

public class AnalisadorSemantico extends LinguagemParserBaseVisitor<Tipo> {
    private final TabelaSimbolos tabelaGlobal = new TabelaSimbolos();
    private TabelaSimbolos escopoAtual = tabelaGlobal;
    private final List<String> erros = new ArrayList<>();
    private final Map<ParserRuleContext, Tipo> tiposExpressoes = new HashMap<>();
    private int proximoDeslocamento;

    public ResultadoSemantico analisar(LinguagemParser.ProgContext arvore) {
        visit(arvore);

        if (!erros.isEmpty()) {
            throw new RuntimeException(montarMensagemDeErro());
        }

        return new ResultadoSemantico(tabelaGlobal, tiposExpressoes);
    }

    @Override
    public Tipo visitProg(LinguagemParser.ProgContext ctx) {
        visit(ctx.decls());
        visit(ctx.cmdComp());
        return Tipo.VAZIO;
    }

    @Override
    public Tipo visitDeclTip(LinguagemParser.DeclTipContext ctx) {
        Tipo tipo = tipoDaDeclaracao(ctx.tip());

        for (TerminalNode id : ctx.listId().ID()) {
            Token token = id.getSymbol();
            String nome = token.getText();

            if (escopoAtual.contemNoEscopoAtual(nome)) {
                Simbolo antigo = escopoAtual.buscar(nome);
                registrarErro(token, "variavel '" + nome + "' ja declarada anteriormente na linha "
                        + antigo.getLinha() + ", coluna " + antigo.getColuna());
            } else {
                Simbolo simbolo = new Simbolo(nome, tipo, proximoDeslocamento,
                        token.getLine(), token.getCharPositionInLine());
                escopoAtual.declarar(simbolo);
                proximoDeslocamento += tamanhoDoTipo(tipo);
            }
        }

        return Tipo.VAZIO;
    }

    @Override
    public Tipo visitCmdComp(LinguagemParser.CmdCompContext ctx) {
        TabelaSimbolos escopoAnterior = escopoAtual;
        escopoAtual = new TabelaSimbolos(escopoAnterior);

        if (ctx.listCmd() != null) {
            visit(ctx.listCmd());
        }

        escopoAtual = escopoAnterior;
        return Tipo.VAZIO;
    }

    @Override
    public Tipo visitCmd(LinguagemParser.CmdContext ctx) {
        if (ctx.cmdCasado() != null) {
            visit(ctx.cmdCasado());
        } else if (ctx.cmdAberto() != null) {
            visit(ctx.cmdAberto());
        }

        return Tipo.VAZIO;
    }

    @Override
    public Tipo visitCmdCasado(LinguagemParser.CmdCasadoContext ctx) {
        if (ctx.cmdBasico() != null) {
            visit(ctx.cmdBasico());
            return Tipo.VAZIO;
        }

        if (ctx.IF() != null) {
            validarCondicaoBooleana(ctx.expr(), ctx.IF().getSymbol(), "IF");
            visit(ctx.cmdCasado(0));
            visit(ctx.cmdCasado(1));
            return Tipo.VAZIO;
        }

        if (ctx.WHILE() != null) {
            validarCondicaoBooleana(ctx.expr(), ctx.WHILE().getSymbol(), "WHILE");
            visit(ctx.cmdCasado(0));
        }

        return Tipo.VAZIO;
    }

    @Override
    public Tipo visitCmdAberto(LinguagemParser.CmdAbertoContext ctx) {
        if (ctx.WHILE() != null) {
            validarCondicaoBooleana(ctx.expr(), ctx.WHILE().getSymbol(), "WHILE");
            visit(ctx.cmdAberto());
            return Tipo.VAZIO;
        }

        validarCondicaoBooleana(ctx.expr(), ctx.IF().getSymbol(), "IF");

        if (ctx.ELSE() != null) {
            visit(ctx.cmdCasado());
            visit(ctx.cmdAberto());
        } else {
            visit(ctx.cmd());
        }

        return Tipo.VAZIO;
    }

    @Override
    public Tipo visitCmdRead(LinguagemParser.CmdReadContext ctx) {
        for (TerminalNode id : ctx.listId().ID()) {
            buscarVariavelDeclarada(id.getSymbol());
        }

        return Tipo.VAZIO;
    }

    @Override
    public Tipo visitCmdWrite(LinguagemParser.CmdWriteContext ctx) {
        visit(ctx.listW());
        return Tipo.VAZIO;
    }

    @Override
    public Tipo visitCmdAtrib(LinguagemParser.CmdAtribContext ctx) {
        Token tokenVariavel = ctx.ID().getSymbol();
        Simbolo variavel = buscarVariavelDeclarada(tokenVariavel);
        Tipo tipoExpressao = visit(ctx.expr());

        if (variavel != null && tipoExpressao != Tipo.INVALIDO && variavel.getTipo() != tipoExpressao) {
            registrarErro(tokenVariavel, "atribuicao invalida: variavel '" + variavel.getNome()
                    + "' e " + variavel.getTipo() + ", mas a expressao e " + tipoExpressao);
        }

        return Tipo.VAZIO;
    }

    @Override
    public Tipo visitExpr(LinguagemParser.ExprContext ctx) {
        if (ctx.CADEIA() != null) {
            return guardarTipo(ctx, Tipo.STRING);
        }

        return guardarTipo(ctx, visit(ctx.exprLogica()));
    }

    @Override
    public Tipo visitExprLogica(LinguagemParser.ExprLogicaContext ctx) {
        Tipo tipoAtual = visit(ctx.exprRel(0));

        for (int i = 0; i < ctx.OPLOG().size(); i++) {
            Tipo tipoDireita = visit(ctx.exprRel(i + 1));
            Token operador = ctx.OPLOG(i).getSymbol();

            exigirBooleano(tipoAtual, operador, "operador logico '" + operador.getText() + "'");
            exigirBooleano(tipoDireita, operador, "operador logico '" + operador.getText() + "'");
            tipoAtual = Tipo.BOOLEAN;
        }

        return guardarTipo(ctx, tipoAtual);
    }

    @Override
    public Tipo visitExprRel(LinguagemParser.ExprRelContext ctx) {
        Tipo tipoEsquerda = visit(ctx.exprArit(0));

        if (ctx.OPREL() == null) {
            return guardarTipo(ctx, tipoEsquerda);
        }

        Tipo tipoDireita = visit(ctx.exprArit(1));
        Token operador = ctx.OPREL().getSymbol();
        String textoOperador = operador.getText();

        if (textoOperador.equals("<") || textoOperador.equals("<=")
                || textoOperador.equals(">") || textoOperador.equals(">=")) {
            exigirInteiro(tipoEsquerda, operador, "operador relacional '" + textoOperador + "'");
            exigirInteiro(tipoDireita, operador, "operador relacional '" + textoOperador + "'");
        } else if (tipoEsquerda != Tipo.INVALIDO && tipoDireita != Tipo.INVALIDO && tipoEsquerda != tipoDireita) {
            registrarErro(operador, "comparacao invalida entre " + tipoEsquerda + " e " + tipoDireita);
        }

        return guardarTipo(ctx, Tipo.BOOLEAN);
    }

    @Override
    public Tipo visitExprArit(LinguagemParser.ExprAritContext ctx) {
        Tipo tipoAtual = visit(ctx.termo(0));

        for (int i = 0; i < ctx.OPAD().size(); i++) {
            Token operador = ctx.OPAD(i).getSymbol();
            Tipo tipoDireita = visit(ctx.termo(i + 1));

            exigirInteiro(tipoAtual, operador, "operador aritmetico '" + operador.getText() + "'");
            exigirInteiro(tipoDireita, operador, "operador aritmetico '" + operador.getText() + "'");
            tipoAtual = Tipo.INTEGER;
        }

        return guardarTipo(ctx, tipoAtual);
    }

    @Override
    public Tipo visitTermo(LinguagemParser.TermoContext ctx) {
        Tipo tipoAtual = visit(ctx.fator(0));

        for (int i = 0; i < ctx.OPMULT().size(); i++) {
            Token operador = ctx.OPMULT(i).getSymbol();
            Tipo tipoDireita = visit(ctx.fator(i + 1));

            exigirInteiro(tipoAtual, operador, "operador aritmetico '" + operador.getText() + "'");
            exigirInteiro(tipoDireita, operador, "operador aritmetico '" + operador.getText() + "'");
            tipoAtual = Tipo.INTEGER;
        }

        return guardarTipo(ctx, tipoAtual);
    }

    @Override
    public Tipo visitFator(LinguagemParser.FatorContext ctx) {
        if (ctx.OPNEG() != null) {
            Tipo tipo = visit(ctx.fator());
            exigirBooleano(tipo, ctx.OPNEG().getSymbol(), "operador de negacao logica");
            return guardarTipo(ctx, Tipo.BOOLEAN);
        }

        if (ctx.cteAssinada() != null) {
            return guardarTipo(ctx, Tipo.INTEGER);
        }

        return guardarTipo(ctx, visit(ctx.fatorPrimario()));
    }

    @Override
    public Tipo visitCteAssinada(LinguagemParser.CteAssinadaContext ctx) {
        return guardarTipo(ctx, Tipo.INTEGER);
    }

    @Override
    public Tipo visitFatorPrimario(LinguagemParser.FatorPrimarioContext ctx) {
        if (ctx.ID() != null) {
            Simbolo simbolo = buscarVariavelDeclarada(ctx.ID().getSymbol());
            return guardarTipo(ctx, simbolo == null ? Tipo.INVALIDO : simbolo.getTipo());
        }

        if (ctx.CTE() != null) {
            return guardarTipo(ctx, Tipo.INTEGER);
        }

        if (ctx.TRUE() != null || ctx.FALSE() != null) {
            return guardarTipo(ctx, Tipo.BOOLEAN);
        }

        return guardarTipo(ctx, visit(ctx.exprLogica()));
    }

    private void validarCondicaoBooleana(LinguagemParser.ExprContext expressao, Token comando, String nomeComando) {
        Tipo tipo = visit(expressao);

        if (tipo != Tipo.INVALIDO && tipo != Tipo.BOOLEAN) {
            registrarErro(comando, "condicao do " + nomeComando + " deve ser BOOLEAN, mas recebeu " + tipo);
        }
    }

    private Simbolo buscarVariavelDeclarada(Token token) {
        Simbolo simbolo = escopoAtual.buscar(token.getText());

        if (simbolo == null) {
            registrarErro(token, "variavel '" + token.getText() + "' nao declarada");
        }

        return simbolo;
    }

    private void exigirInteiro(Tipo tipo, Token token, String contexto) {
        if (tipo != Tipo.INVALIDO && tipo != Tipo.INTEGER) {
            registrarErro(token, contexto + " exige INTEGER, mas recebeu " + tipo);
        }
    }

    private void exigirBooleano(Tipo tipo, Token token, String contexto) {
        if (tipo != Tipo.INVALIDO && tipo != Tipo.BOOLEAN) {
            registrarErro(token, contexto + " exige BOOLEAN, mas recebeu " + tipo);
        }
    }

    private Tipo tipoDaDeclaracao(LinguagemParser.TipContext ctx) {
        if (ctx.INTEGER() != null) {
            return Tipo.INTEGER;
        }

        if (ctx.BOOLEAN() != null) {
            return Tipo.BOOLEAN;
        }

        return Tipo.STRING;
    }

    private int tamanhoDoTipo(Tipo tipo) {
        if (tipo == Tipo.INTEGER) {
            return 2;
        }

        if (tipo == Tipo.BOOLEAN) {
            return 1;
        }

        return 256;
    }

    private Tipo guardarTipo(ParserRuleContext contexto, Tipo tipo) {
        tiposExpressoes.put(contexto, tipo);
        return tipo;
    }

    private void registrarErro(Token token, String mensagem) {
        erros.add("linha " + token.getLine() + ", coluna " + token.getCharPositionInLine() + ": " + mensagem);
    }

    private String montarMensagemDeErro() {
        StringBuilder mensagem = new StringBuilder("Erros semanticos encontrados:");

        for (String erro : erros) {
            mensagem.append(System.lineSeparator()).append("- ").append(erro);
        }

        return mensagem.toString();
    }
}
