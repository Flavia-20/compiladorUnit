package org.example.compilador.intermediario;

import java.util.List;
import java.util.Map;

import org.example.compilador.semantico.DadosSemanticos.TabelaSimbolos;
import org.example.compilador.semantico.DadosSemanticos.Tipo;

public class CodigoIntermediario {
    private final TabelaSimbolos tabelaSimbolos;
    private final List<InstrucaoTAC> instrucoes;
    private final Map<String, Tipo> tiposTemporarios;

    public CodigoIntermediario(TabelaSimbolos tabelaSimbolos, List<InstrucaoTAC> instrucoes,
                               Map<String, Tipo> tiposTemporarios) {
        this.tabelaSimbolos = tabelaSimbolos;
        this.instrucoes = instrucoes;
        this.tiposTemporarios = tiposTemporarios;
    }

    public TabelaSimbolos getTabelaSimbolos() {
        return tabelaSimbolos;
    }

    public List<InstrucaoTAC> getInstrucoes() {
        return instrucoes;
    }

    public Map<String, Tipo> getTiposTemporarios() {
        return tiposTemporarios;
    }

    public String formatar() {
        StringBuilder texto = new StringBuilder();

        for (InstrucaoTAC instrucao : instrucoes) {
            texto.append(instrucao).append(System.lineSeparator());
        }

        return texto.toString();
    }

    public static class InstrucaoTAC {
        public enum Operacao {
            ATRIBUICAO,
            BINARIA,
            UNARIA,
            ROTULO,
            DESVIO,
            SE_FALSO,
            LEITURA,
            ESCRITA
        }

        private final Operacao operacao;
        private final String resultado;
        private final String argumento1;
        private final String operador;
        private final String argumento2;

        private InstrucaoTAC(Operacao operacao, String resultado, String argumento1, String operador,
                             String argumento2) {
            this.operacao = operacao;
            this.resultado = resultado;
            this.argumento1 = argumento1;
            this.operador = operador;
            this.argumento2 = argumento2;
        }

        public static InstrucaoTAC atribuicao(String destino, String valor) {
            return new InstrucaoTAC(Operacao.ATRIBUICAO, destino, valor, null, null);
        }

        public static InstrucaoTAC binaria(String destino, String esquerda, String operador, String direita) {
            return new InstrucaoTAC(Operacao.BINARIA, destino, esquerda, operador, direita);
        }

        public static InstrucaoTAC unaria(String destino, String operador, String valor) {
            return new InstrucaoTAC(Operacao.UNARIA, destino, valor, operador, null);
        }

        public static InstrucaoTAC rotulo(String rotulo) {
            return new InstrucaoTAC(Operacao.ROTULO, rotulo, null, null, null);
        }

        public static InstrucaoTAC desvio(String rotulo) {
            return new InstrucaoTAC(Operacao.DESVIO, rotulo, null, null, null);
        }

        public static InstrucaoTAC seFalso(String condicao, String rotulo) {
            return new InstrucaoTAC(Operacao.SE_FALSO, rotulo, condicao, null, null);
        }

        public static InstrucaoTAC leitura(String variavel) {
            return new InstrucaoTAC(Operacao.LEITURA, variavel, null, null, null);
        }

        public static InstrucaoTAC escrita(String valor) {
            return new InstrucaoTAC(Operacao.ESCRITA, null, valor, null, null);
        }

        public Operacao getOperacao() {
            return operacao;
        }

        public String getResultado() {
            return resultado;
        }

        public String getArgumento1() {
            return argumento1;
        }

        public String getOperador() {
            return operador;
        }

        public String getArgumento2() {
            return argumento2;
        }

        @Override
        public String toString() {
            switch (operacao) {
                case ATRIBUICAO:
                    return resultado + " = " + argumento1;
                case BINARIA:
                    return resultado + " = " + argumento1 + " " + operador + " " + argumento2;
                case UNARIA:
                    return resultado + " = " + operador + argumento1;
                case ROTULO:
                    return resultado + ":";
                case DESVIO:
                    return "GOTO " + resultado;
                case SE_FALSO:
                    return "IF " + argumento1 + " == 0 GOTO " + resultado;
                case LEITURA:
                    return "READ(" + resultado + ")";
                case ESCRITA:
                    return "WRITE(" + argumento1 + ")";
                default:
                    return "";
            }
        }
    }
}
