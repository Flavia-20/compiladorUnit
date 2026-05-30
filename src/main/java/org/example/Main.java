package org.example;

// Imports das suas classes geradas
import antlr.LinguagemParser;
import antlr.meuLexico;

// Imports do ANTLR
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

public class Main {
    private static void imprimirTokens(CommonTokenStream tokens) {
        System.out.println("=========================================");
        System.out.println("           TOKENS IDENTIFICADOS          ");
        System.out.println("=========================================");
        System.out.printf("%-15s %-15s %-15s%n", "TOKEN", "TIPO", "ATRIBUTO");

        for (Token token : tokens.getTokens()) {
            if (token.getType() == Token.EOF) {
                continue;
            }

            String tipo = meuLexico.VOCABULARY.getSymbolicName(token.getType());
            String lexema = token.getText();
            String atributo = obterAtributo(token);

            System.out.printf("%-15s %-15s %-15s%n", lexema, tipo, atributo);
        }
    }

    private static String obterAtributo(Token token) {
        String texto = token.getText();

        switch (token.getType()) {
            case meuLexico.ID:
            case meuLexico.CTE:
            case meuLexico.CADEIA:
                return texto;

            case meuLexico.OPAD:
                return texto.equals("+") ? "MAIS" : "MENOS";

            case meuLexico.OPMULT:
                return texto.equals("*") ? "VEZES" : "DIV";

            case meuLexico.OPLOG:
                return texto.toUpperCase();

            case meuLexico.OPNEG:
                return "NEG";

            case meuLexico.OPREL:
                switch (texto) {
                    case "<": return "MENOR";
                    case "<=": return "MENIG";
                    case ">": return "MAIOR";
                    case ">=": return "MAIG";
                    case "==": return "IGUAL";
                    case "<>": return "DIFER";
                    default: return texto;
                }

            default:
                return "-";
        }
    }

    public static void main(String[] args) {
        try {
            //  Carrega o arquivo de texto
            String arquivoTeste = "C:\\Users\\flavi\\IdeaProjects\\compiladorUnit\\src\\main\\java\\erroLexico.txt";
            CharStream input = CharStreams.fromFileName(arquivoTeste);

            //  Inicia o Lexer
            meuLexico lexer = new meuLexico(input);

            BaseErrorListener Guarda = new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer,
                                        Object offendingSymbol,
                                        int line,
                                        int charPositionInLine,
                                        String msg,
                                        RecognitionException e) {
                    throw new RuntimeException(
                            "Erro na linha " + line +
                                    ", coluna " + charPositionInLine +
                                    ": " + msg
                    );
                }
            };

            lexer.removeErrorListeners();
            lexer.addErrorListener(Guarda);

            //  Cria os tokens
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // Imprime os tokens
            tokens.fill();

            tokens.seek(0);

            // Inicia o Parser
            LinguagemParser parser = new LinguagemParser(tokens);

            parser.removeErrorListeners();
            parser.addErrorListener(Guarda);

            // Roda a análise sintática
            ParseTree tree = parser.prog();

            // Roda a análise semântica
            System.out.println("\nIniciando Análise Semântica...");
            SemanticoVisitor semantico = new SemanticoVisitor();
            semantico.visit(tree);

            // Gerador de código
            System.out.println("Iniciando Geração de Código...");
            GeradorCodigo gerador = new GeradorCodigo();
            gerador.visit(tree);

            imprimirTokens(tokens);

            System.out.println("\n=========================================");
            System.out.println("              CÓDIGO GERADO              ");
            System.out.println("=========================================");
            System.out.println(gerador.getCodigoGerado());


            System.out.println("=========================================");
            System.out.println("           RESULTADO DA ANÁLISE          ");
            System.out.println("=========================================");
            System.out.println(tree.toStringTree(parser));
            System.out.println("=========================================");
            System.out.println("\n Compilação concluída com sucesso (0 erros)!");

        } catch (RuntimeException e) {
            System.err.println("\n A COMPILAÇÃO FOI INTERROMPIDA!");
            System.err.println("Motivo: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("\n Erro ao ler o arquivo: " + e.getMessage());
        }
    }
}