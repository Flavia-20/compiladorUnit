package org.example.compilador.codigofinal;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.example.compilador.intermediario.CodigoIntermediario;
import org.example.compilador.intermediario.CodigoIntermediario.InstrucaoTAC;
import org.example.compilador.semantico.DadosSemanticos.Simbolo;
import org.example.compilador.semantico.DadosSemanticos.Tipo;

public class GeradorCodigoFinal {
    private final Map<String, String> literaisString = new LinkedHashMap<>();

    public String gerar(CodigoIntermediario programa) {
        literaisString.clear();
        registrarLiteraisString(programa);

        StringBuilder codigo = new StringBuilder();

        codigo.append(".data").append(System.lineSeparator());
        declararVariaveis(programa, codigo);
        declararTemporarios(programa, codigo);
        declararStrings(codigo);

        codigo.append(System.lineSeparator());
        codigo.append(".text").append(System.lineSeparator());
        codigo.append("_start:").append(System.lineSeparator());
        codigo.append("    ; Rotinas externas esperadas: _print_integer, _print_boolean, _print_string").append(System.lineSeparator());
        codigo.append("    ; Rotinas externas esperadas: _read_integer, _read_boolean, _read_string, _copy_string")
                .append(System.lineSeparator());

        for (InstrucaoTAC instrucao : programa.getInstrucoes()) {
            gerarInstrucao(programa, instrucao, codigo);
        }

        codigo.append("    ; fim do programa").append(System.lineSeparator());
        return codigo.toString();
    }

    private void declararVariaveis(CodigoIntermediario programa, StringBuilder codigo) {
        for (Simbolo simbolo : programa.getTabelaSimbolos().todos()) {
            if (simbolo.getTipo() == Tipo.INTEGER) {
                codigo.append(simbolo.getNome()).append(" dw 0");
            } else if (simbolo.getTipo() == Tipo.BOOLEAN) {
                codigo.append(simbolo.getNome()).append(" db 0");
            } else if (simbolo.getTipo() == Tipo.STRING) {
                codigo.append(simbolo.getNome()).append(" db 256 dup(0)");
            }

            codigo.append(" ; deslocamento ").append(simbolo.getDeslocamento()).append(System.lineSeparator());
        }
    }

    private void declararTemporarios(CodigoIntermediario programa, StringBuilder codigo) {
        for (String temp : temporariosUsados(programa)) {
            Tipo tipo = programa.getTiposTemporarios().get(temp);

            if (tipo == Tipo.BOOLEAN) {
                codigo.append(temp).append(" db 0");
            } else {
                codigo.append(temp).append(" dw 0");
            }

            codigo.append(System.lineSeparator());
        }
    }

    private void declararStrings(StringBuilder codigo) {
        for (Map.Entry<String, String> entrada : literaisString.entrySet()) {
            codigo.append(entrada.getValue())
                    .append(" db \"")
                    .append(textoDaString(entrada.getKey()))
                    .append("\", 0")
                    .append(System.lineSeparator());
        }
    }

    private void gerarInstrucao(CodigoIntermediario programa, InstrucaoTAC instrucao, StringBuilder codigo) {
        switch (instrucao.getOperacao()) {
            case ATRIBUICAO:
                gerarAtribuicao(programa, instrucao, codigo);
                break;
            case BINARIA:
                gerarBinaria(programa, instrucao, codigo);
                break;
            case UNARIA:
                gerarUnaria(programa, instrucao, codigo);
                break;
            case ROTULO:
                codigo.append(instrucao.getResultado()).append(":").append(System.lineSeparator());
                break;
            case DESVIO:
                codigo.append("    jmp ").append(instrucao.getResultado()).append(System.lineSeparator());
                break;
            case SE_FALSO:
                codigo.append("    mov al, ").append(valorBooleano(programa, instrucao.getArgumento1()))
                        .append(System.lineSeparator());
                codigo.append("    cmp al, 0").append(System.lineSeparator());
                codigo.append("    je ").append(instrucao.getResultado()).append(System.lineSeparator());
                break;
            case LEITURA:
                gerarLeitura(programa, instrucao.getResultado(), codigo);
                break;
            case ESCRITA:
                gerarEscrita(programa, instrucao.getArgumento1(), codigo);
                break;
            default:
                break;
        }
    }

    private void gerarAtribuicao(CodigoIntermediario programa, InstrucaoTAC instrucao, StringBuilder codigo) {
        Tipo tipoDestino = tipoDe(programa, instrucao.getResultado());

        if (tipoDestino == Tipo.STRING) {
            codigo.append("    push ").append(valorString(programa, instrucao.getArgumento1())).append(System.lineSeparator());
            codigo.append("    push offset ").append(instrucao.getResultado()).append(System.lineSeparator());
            codigo.append("    call _copy_string").append(System.lineSeparator());
            return;
        }

        if (tipoDestino == Tipo.BOOLEAN) {
            codigo.append("    mov al, ").append(valorBooleano(programa, instrucao.getArgumento1()))
                    .append(System.lineSeparator());
            codigo.append("    mov byte ptr [").append(instrucao.getResultado()).append("], al")
                    .append(System.lineSeparator());
            return;
        }

        if (ehInteiro(instrucao.getArgumento1())) {
            codigo.append("    mov word ptr [").append(instrucao.getResultado()).append("], ")
                    .append(instrucao.getArgumento1()).append(System.lineSeparator());
        } else {
            codigo.append("    mov ax, ").append(valorInteiro(programa, instrucao.getArgumento1()))
                    .append(System.lineSeparator());
            codigo.append("    mov word ptr [").append(instrucao.getResultado()).append("], ax")
                    .append(System.lineSeparator());
        }
    }

    private void gerarBinaria(CodigoIntermediario programa, InstrucaoTAC instrucao, StringBuilder codigo) {
        String operador = instrucao.getOperador();

        if (ehOperadorRelacional(operador)) {
            gerarComparacao(programa, instrucao, codigo);
            return;
        }

        if (operador.equalsIgnoreCase("AND") || operador.equalsIgnoreCase("OR")) {
            codigo.append("    mov al, ").append(valorBooleano(programa, instrucao.getArgumento1()))
                    .append(System.lineSeparator());
            codigo.append("    ").append(operador.equalsIgnoreCase("AND") ? "and" : "or")
                    .append(" al, ").append(valorBooleano(programa, instrucao.getArgumento2()))
                    .append(System.lineSeparator());
            codigo.append("    mov byte ptr [").append(instrucao.getResultado()).append("], al")
                    .append(System.lineSeparator());
            return;
        }

        codigo.append("    mov ax, ").append(valorInteiro(programa, instrucao.getArgumento1()))
                .append(System.lineSeparator());

        if (operador.equals("+")) {
            codigo.append("    add ax, ").append(valorInteiro(programa, instrucao.getArgumento2())).append(System.lineSeparator());
        } else if (operador.equals("-")) {
            codigo.append("    sub ax, ").append(valorInteiro(programa, instrucao.getArgumento2())).append(System.lineSeparator());
        } else if (operador.equals("*")) {
            codigo.append("    mov bx, ").append(valorInteiro(programa, instrucao.getArgumento2())).append(System.lineSeparator());
            codigo.append("    imul bx").append(System.lineSeparator());
        } else if (operador.equals("/")) {
            codigo.append("    mov bx, ").append(valorInteiro(programa, instrucao.getArgumento2())).append(System.lineSeparator());
            codigo.append("    cwd").append(System.lineSeparator());
            codigo.append("    idiv bx").append(System.lineSeparator());
        } else if (operador.equals("<<")) {
            codigo.append("    shl ax, ").append(instrucao.getArgumento2()).append(System.lineSeparator());
        }

        codigo.append("    mov word ptr [").append(instrucao.getResultado()).append("], ax")
                .append(System.lineSeparator());
    }

    private void gerarComparacao(CodigoIntermediario programa, InstrucaoTAC instrucao, StringBuilder codigo) {
        Tipo tipoEsquerda = tipoDe(programa, instrucao.getArgumento1());

        if (tipoEsquerda == Tipo.BOOLEAN) {
            codigo.append("    mov al, ").append(valorBooleano(programa, instrucao.getArgumento1()))
                    .append(System.lineSeparator());
            codigo.append("    cmp al, ").append(valorBooleano(programa, instrucao.getArgumento2()))
                    .append(System.lineSeparator());
        } else {
            codigo.append("    mov ax, ").append(valorInteiro(programa, instrucao.getArgumento1()))
                    .append(System.lineSeparator());
            codigo.append("    cmp ax, ").append(valorInteiro(programa, instrucao.getArgumento2()))
                    .append(System.lineSeparator());
        }

        codigo.append("    ").append(instrucaoSet(instrucao.getOperador())).append(" al")
                .append(System.lineSeparator());
        codigo.append("    mov byte ptr [").append(instrucao.getResultado()).append("], al")
                .append(System.lineSeparator());
    }

    private void gerarUnaria(CodigoIntermediario programa, InstrucaoTAC instrucao, StringBuilder codigo) {
        codigo.append("    mov al, ").append(valorBooleano(programa, instrucao.getArgumento1()))
                .append(System.lineSeparator());
        codigo.append("    xor al, 1").append(System.lineSeparator());
        codigo.append("    mov byte ptr [").append(instrucao.getResultado()).append("], al")
                .append(System.lineSeparator());
    }

    private void gerarLeitura(CodigoIntermediario programa, String variavel, StringBuilder codigo) {
        Tipo tipo = tipoDe(programa, variavel);

        if (tipo == Tipo.INTEGER) {
            codigo.append("    call _read_integer").append(System.lineSeparator());
            codigo.append("    mov word ptr [").append(variavel).append("], ax").append(System.lineSeparator());
        } else if (tipo == Tipo.BOOLEAN) {
            codigo.append("    call _read_boolean").append(System.lineSeparator());
            codigo.append("    mov byte ptr [").append(variavel).append("], al").append(System.lineSeparator());
        } else if (tipo == Tipo.STRING) {
            codigo.append("    push offset ").append(variavel).append(System.lineSeparator());
            codigo.append("    call _read_string").append(System.lineSeparator());
        }
    }

    private void gerarEscrita(CodigoIntermediario programa, String valor, StringBuilder codigo) {
        Tipo tipo = tipoDe(programa, valor);

        if (tipo == Tipo.INTEGER) {
            codigo.append("    push ").append(valorInteiro(programa, valor)).append(System.lineSeparator());
            codigo.append("    call _print_integer").append(System.lineSeparator());
        } else if (tipo == Tipo.BOOLEAN) {
            codigo.append("    mov al, ").append(valorBooleano(programa, valor)).append(System.lineSeparator());
            codigo.append("    push ax").append(System.lineSeparator());
            codigo.append("    call _print_boolean").append(System.lineSeparator());
        } else if (tipo == Tipo.STRING) {
            codigo.append("    push ").append(valorString(programa, valor)).append(System.lineSeparator());
            codigo.append("    call _print_string").append(System.lineSeparator());
        }
    }

    private void registrarLiteraisString(CodigoIntermediario programa) {
        for (InstrucaoTAC instrucao : programa.getInstrucoes()) {
            registrarLiteralString(instrucao.getArgumento1());
            registrarLiteralString(instrucao.getArgumento2());
        }
    }

    private void registrarLiteralString(String valor) {
        if (ehTexto(valor) && !literaisString.containsKey(valor)) {
            literaisString.put(valor, "str_" + literaisString.size());
        }
    }

    private Set<String> temporariosUsados(CodigoIntermediario programa) {
        Set<String> temporarios = new LinkedHashSet<>();

        for (InstrucaoTAC instrucao : programa.getInstrucoes()) {
            adicionarTemporario(programa, instrucao.getResultado(), temporarios);
            adicionarTemporario(programa, instrucao.getArgumento1(), temporarios);
            adicionarTemporario(programa, instrucao.getArgumento2(), temporarios);
        }

        return temporarios;
    }

    private void adicionarTemporario(CodigoIntermediario programa, String valor, Set<String> temporarios) {
        if (programa.getTiposTemporarios().containsKey(valor)) {
            temporarios.add(valor);
        }
    }

    private Tipo tipoDe(CodigoIntermediario programa, String valor) {
        if (valor == null) {
            return Tipo.INVALIDO;
        }

        Simbolo simbolo = programa.getTabelaSimbolos().buscar(valor);
        if (simbolo != null) {
            return simbolo.getTipo();
        }

        Tipo tipoTemporario = programa.getTiposTemporarios().get(valor);
        if (tipoTemporario != null) {
            return tipoTemporario;
        }

        if (ehInteiro(valor)) return Tipo.INTEGER;
        if (ehBooleano(valor)) return Tipo.BOOLEAN;
        if (ehTexto(valor)) return Tipo.STRING;

        return Tipo.INVALIDO;
    }

    private String valorInteiro(CodigoIntermediario programa, String valor) {
        if (ehInteiro(valor)) {
            return valor;
        }

        return "word ptr [" + valor + "]";
    }

    private String valorBooleano(CodigoIntermediario programa, String valor) {
        if ("true".equalsIgnoreCase(valor)) {
            return "1";
        }

        if ("false".equalsIgnoreCase(valor)) {
            return "0";
        }

        return "byte ptr [" + valor + "]";
    }

    private String valorString(CodigoIntermediario programa, String valor) {
        if (ehTexto(valor)) {
            return "offset " + literaisString.get(valor);
        }

        return "offset " + valor;
    }

    private boolean ehOperadorRelacional(String operador) {
        return operador.equals("<") || operador.equals("<=") || operador.equals(">")
                || operador.equals(">=") || operador.equals("==") || operador.equals("<>");
    }

    private String instrucaoSet(String operador) {
        if (operador.equals("<")) return "setl";
        if (operador.equals("<=")) return "setle";
        if (operador.equals(">")) return "setg";
        if (operador.equals(">=")) return "setge";
        if (operador.equals("==")) return "sete";
        return "setne";
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

    private String textoDaString(String valor) {
        return valor.substring(1, valor.length() - 1);
    }
}
