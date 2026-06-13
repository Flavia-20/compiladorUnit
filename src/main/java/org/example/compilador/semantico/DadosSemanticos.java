package org.example.compilador.semantico;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.ParserRuleContext;

public class DadosSemanticos {
    public enum Tipo {
        INTEGER,
        BOOLEAN,
        STRING,
        INVALIDO,
        VAZIO
    }

    public static class Simbolo {
        private final String nome;
        private final Tipo tipo;
        private final int deslocamento;
        private final int linha;
        private final int coluna;

        public Simbolo(String nome, Tipo tipo, int deslocamento, int linha, int coluna) {
            this.nome = nome;
            this.tipo = tipo;
            this.deslocamento = deslocamento;
            this.linha = linha;
            this.coluna = coluna;
        }

        public String getNome() {
            return nome;
        }

        public Tipo getTipo() {
            return tipo;
        }

        public int getDeslocamento() {
            return deslocamento;
        }

        public int getLinha() {
            return linha;
        }

        public int getColuna() {
            return coluna;
        }
    }

    public static class TabelaSimbolos {
        private final HashMap<String, Simbolo> simbolos = new HashMap<>();
        private final TabelaSimbolos escopoPai;

        public TabelaSimbolos() {
            this(null);
        }

        public TabelaSimbolos(TabelaSimbolos escopoPai) {
            this.escopoPai = escopoPai;
        }

        public boolean declarar(Simbolo simbolo) {
            if (simbolos.containsKey(simbolo.getNome())) {
                return false;
            }

            simbolos.put(simbolo.getNome(), simbolo);
            return true;
        }

        public Simbolo buscar(String nome) {
            Simbolo simbolo = simbolos.get(nome);

            if (simbolo != null) {
                return simbolo;
            }

            if (escopoPai != null) {
                return escopoPai.buscar(nome);
            }

            return null;
        }

        public boolean contem(String nome) {
            return buscar(nome) != null;
        }

        public boolean contemNoEscopoAtual(String nome) {
            return simbolos.containsKey(nome);
        }

        public TabelaSimbolos getEscopoPai() {
            return escopoPai;
        }

        public Collection<Simbolo> todos() {
            return simbolos.values();
        }
    }

    public static class ResultadoSemantico {
        private final TabelaSimbolos tabelaSimbolos;
        private final Map<ParserRuleContext, Tipo> tiposExpressoes;

        public ResultadoSemantico(TabelaSimbolos tabelaSimbolos, Map<ParserRuleContext, Tipo> tiposExpressoes) {
            this.tabelaSimbolos = tabelaSimbolos;
            this.tiposExpressoes = tiposExpressoes;
        }

        public TabelaSimbolos getTabelaSimbolos() {
            return tabelaSimbolos;
        }

        public Tipo getTipo(ParserRuleContext contexto) {
            Tipo tipo = tiposExpressoes.get(contexto);
            return tipo == null ? Tipo.INVALIDO : tipo;
        }
    }
}
