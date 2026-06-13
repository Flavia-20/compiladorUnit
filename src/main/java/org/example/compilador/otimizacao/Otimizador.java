package org.example.compilador.otimizacao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.example.compilador.intermediario.CodigoIntermediario;
import org.example.compilador.intermediario.CodigoIntermediario.InstrucaoTAC;
import org.example.compilador.intermediario.CodigoIntermediario.InstrucaoTAC.Operacao;
import org.example.compilador.semantico.DadosSemanticos.Simbolo;

public class Otimizador {
    public CodigoIntermediario otimizar(CodigoIntermediario programa) {
        List<InstrucaoTAC> instrucoes = aplicarOtimizacoesSimples(programa);
        instrucoes = removerTemporariosMortos(programa, instrucoes);
        return new CodigoIntermediario(programa.getTabelaSimbolos(), instrucoes, programa.getTiposTemporarios());
    }

    private List<InstrucaoTAC> aplicarOtimizacoesSimples(CodigoIntermediario programa) {
        List<InstrucaoTAC> resultado = new ArrayList<>();
        Map<String, String> constantes = new HashMap<>();

        for (InstrucaoTAC instrucao : programa.getInstrucoes()) {
            switch (instrucao.getOperacao()) {
                case ATRIBUICAO:
                    otimizarAtribuicao(instrucao, resultado, constantes);
                    break;
                case BINARIA:
                    otimizarBinaria(instrucao, resultado, constantes);
                    break;
                case UNARIA:
                    otimizarUnaria(instrucao, resultado, constantes);
                    break;
                case SE_FALSO:
                    resultado.add(InstrucaoTAC.seFalso(trocarPorConstante(instrucao.getArgumento1(), constantes),
                            instrucao.getResultado()));
                    limparVariaveis(programa, constantes);
                    break;
                case ESCRITA:
                    resultado.add(InstrucaoTAC.escrita(trocarPorConstante(instrucao.getArgumento1(), constantes)));
                    break;
                case LEITURA:
                    constantes.remove(instrucao.getResultado());
                    resultado.add(instrucao);
                    break;
                case ROTULO:
                case DESVIO:
                    limparVariaveis(programa, constantes);
                    resultado.add(instrucao);
                    break;
                default:
                    resultado.add(instrucao);
                    break;
            }
        }

        return resultado;
    }

    private void otimizarAtribuicao(InstrucaoTAC instrucao, List<InstrucaoTAC> resultado,
                                    Map<String, String> constantes) {
        String valor = trocarPorConstante(instrucao.getArgumento1(), constantes);
        resultado.add(InstrucaoTAC.atribuicao(instrucao.getResultado(), valor));

        if (ehLiteral(valor)) {
            constantes.put(instrucao.getResultado(), valor);
        } else {
            constantes.remove(instrucao.getResultado());
        }
    }

    private void otimizarBinaria(InstrucaoTAC instrucao, List<InstrucaoTAC> resultado,
                                 Map<String, String> constantes) {
        String esquerda = trocarPorConstante(instrucao.getArgumento1(), constantes);
        String direita = trocarPorConstante(instrucao.getArgumento2(), constantes);
        String dobrado = dobrarConstante(esquerda, instrucao.getOperador(), direita);

        if (dobrado != null) {
            resultado.add(InstrucaoTAC.atribuicao(instrucao.getResultado(), dobrado));
            constantes.put(instrucao.getResultado(), dobrado);
            return;
        }

        String simplificado = simplificarAlgebra(esquerda, instrucao.getOperador(), direita);

        if (simplificado != null) {
            resultado.add(InstrucaoTAC.atribuicao(instrucao.getResultado(), simplificado));

            if (ehLiteral(simplificado)) {
                constantes.put(instrucao.getResultado(), simplificado);
            } else {
                constantes.remove(instrucao.getResultado());
            }

            return;
        }

        InstrucaoTAC reducaoForca = reduzirForca(instrucao.getResultado(), esquerda, instrucao.getOperador(), direita);

        if (reducaoForca != null) {
            resultado.add(reducaoForca);
            constantes.remove(instrucao.getResultado());
            return;
        }

        resultado.add(InstrucaoTAC.binaria(instrucao.getResultado(), esquerda, instrucao.getOperador(), direita));
        constantes.remove(instrucao.getResultado());
    }

    private void otimizarUnaria(InstrucaoTAC instrucao, List<InstrucaoTAC> resultado,
                                Map<String, String> constantes) {
        String valor = trocarPorConstante(instrucao.getArgumento1(), constantes);
        String dobrado = dobrarUnaria(instrucao.getOperador(), valor);

        if (dobrado != null) {
            resultado.add(InstrucaoTAC.atribuicao(instrucao.getResultado(), dobrado));
            constantes.put(instrucao.getResultado(), dobrado);
            return;
        }

        resultado.add(InstrucaoTAC.unaria(instrucao.getResultado(), instrucao.getOperador(), valor));
        constantes.remove(instrucao.getResultado());
    }

    private List<InstrucaoTAC> removerTemporariosMortos(CodigoIntermediario programa, List<InstrucaoTAC> instrucoes) {
        List<InstrucaoTAC> invertidas = new ArrayList<>();
        Set<String> vivos = new HashSet<>();

        for (int i = instrucoes.size() - 1; i >= 0; i--) {
            InstrucaoTAC instrucao = instrucoes.get(i);
            boolean produzTemporario = produzTemporario(programa, instrucao);

            if (produzTemporario && !vivos.contains(instrucao.getResultado())) {
                continue;
            }

            if (produzTemporario) {
                vivos.remove(instrucao.getResultado());
            }

            adicionarUso(instrucao.getArgumento1(), vivos);
            adicionarUso(instrucao.getArgumento2(), vivos);
            invertidas.add(instrucao);
        }

        List<InstrucaoTAC> resultado = new ArrayList<>();

        for (int i = invertidas.size() - 1; i >= 0; i--) {
            resultado.add(invertidas.get(i));
        }

        return resultado;
    }

    private boolean produzTemporario(CodigoIntermediario programa, InstrucaoTAC instrucao) {
        if (instrucao.getOperacao() != Operacao.ATRIBUICAO
                && instrucao.getOperacao() != Operacao.BINARIA
                && instrucao.getOperacao() != Operacao.UNARIA) {
            return false;
        }

        return programa.getTiposTemporarios().containsKey(instrucao.getResultado());
    }

    private void adicionarUso(String valor, Set<String> vivos) {
        if (valor != null && !ehLiteral(valor)) {
            vivos.add(valor);
        }
    }

    private String trocarPorConstante(String valor, Map<String, String> constantes) {
        if (valor == null) {
            return null;
        }

        String constante = constantes.get(valor);
        return constante == null ? valor : constante;
    }

    private String dobrarConstante(String esquerda, String operador, String direita) {
        if (ehInteiro(esquerda) && ehInteiro(direita)) {
            int a = Integer.parseInt(esquerda);
            int b = Integer.parseInt(direita);

            if (operador.equals("+")) return String.valueOf(a + b);
            if (operador.equals("-")) return String.valueOf(a - b);
            if (operador.equals("*")) return String.valueOf(a * b);
            if (operador.equals("/") && b != 0) return String.valueOf(a / b);
            if (operador.equals("<")) return booleano(a < b);
            if (operador.equals("<=")) return booleano(a <= b);
            if (operador.equals(">")) return booleano(a > b);
            if (operador.equals(">=")) return booleano(a >= b);
            if (operador.equals("==")) return booleano(a == b);
            if (operador.equals("<>")) return booleano(a != b);
        }

        if (ehBooleano(esquerda) && ehBooleano(direita)) {
            boolean a = Boolean.parseBoolean(esquerda);
            boolean b = Boolean.parseBoolean(direita);

            if (operador.equalsIgnoreCase("AND")) return booleano(a && b);
            if (operador.equalsIgnoreCase("OR")) return booleano(a || b);
            if (operador.equals("==")) return booleano(a == b);
            if (operador.equals("<>")) return booleano(a != b);
        }

        if (ehTexto(esquerda) && ehTexto(direita)) {
            if (operador.equals("==")) return booleano(esquerda.equals(direita));
            if (operador.equals("<>")) return booleano(!esquerda.equals(direita));
        }

        return null;
    }

    private String dobrarUnaria(String operador, String valor) {
        if (operador.equals("~") && ehBooleano(valor)) {
            return booleano(!Boolean.parseBoolean(valor));
        }

        return null;
    }

    private String simplificarAlgebra(String esquerda, String operador, String direita) {
        if (operador.equals("+") && ehInteiro(direita) && Integer.parseInt(direita) == 0) return esquerda;
        if (operador.equals("+") && ehInteiro(esquerda) && Integer.parseInt(esquerda) == 0) return direita;
        if (operador.equals("-") && ehInteiro(direita) && Integer.parseInt(direita) == 0) return esquerda;
        if (operador.equals("/") && ehInteiro(direita) && Integer.parseInt(direita) == 1) return esquerda;

        if (operador.equals("*") && ehInteiro(direita)) {
            int valor = Integer.parseInt(direita);
            if (valor == 1) return esquerda;
            if (valor == 0) return "0";
        }

        if (operador.equals("*") && ehInteiro(esquerda)) {
            int valor = Integer.parseInt(esquerda);
            if (valor == 1) return direita;
            if (valor == 0) return "0";
        }

        return null;
    }

    private InstrucaoTAC reduzirForca(String destino, String esquerda, String operador, String direita) {
        if (!operador.equals("*")) {
            return null;
        }

        if (ehInteiro(direita) && ehPotenciaDeDois(Integer.parseInt(direita))) {
            return InstrucaoTAC.binaria(destino, esquerda, "<<", String.valueOf(log2(Integer.parseInt(direita))));
        }

        if (ehInteiro(esquerda) && ehPotenciaDeDois(Integer.parseInt(esquerda))) {
            return InstrucaoTAC.binaria(destino, direita, "<<", String.valueOf(log2(Integer.parseInt(esquerda))));
        }

        return null;
    }

    private boolean ehPotenciaDeDois(int valor) {
        return valor > 1 && (valor & (valor - 1)) == 0;
    }

    private int log2(int valor) {
        int resultado = 0;

        while (valor > 1) {
            valor = valor / 2;
            resultado++;
        }

        return resultado;
    }

    private void limparVariaveis(CodigoIntermediario programa, Map<String, String> constantes) {
        for (Simbolo simbolo : programa.getTabelaSimbolos().todos()) {
            constantes.remove(simbolo.getNome());
        }
    }

    private boolean ehLiteral(String valor) {
        return ehInteiro(valor) || ehBooleano(valor) || ehTexto(valor);
    }

    private boolean ehInteiro(String valor) {
        return valor != null && valor.matches("-?[0-9]+");
    }

    private boolean ehBooleano(String valor) {
        return "true".equalsIgnoreCase(valor) || "false".equalsIgnoreCase(valor);
    }

    private boolean ehTexto(String valor) {
        return valor != null && valor.length() >= 2 && valor.startsWith("\"") && valor.endsWith("\"");
    }

    private String booleano(boolean valor) {
        return valor ? "true" : "false";
    }
}
