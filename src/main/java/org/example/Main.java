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
    public static void main(String[] args) {
        try {
            // 1. Carrega o arquivo de texto
            String arquivoTeste = "/Users/joaodionizio/GitHub/compiladorUnit/src/main/java/Sucesso.txt";
            CharStream input = CharStreams.fromFileName(arquivoTeste);

            // 2. Inicia o Lexer
            meuLexico lexer = new meuLexico(input);

            BaseErrorListener caoDeGuarda = new BaseErrorListener() {
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
            lexer.addErrorListener(caoDeGuarda);

            // 3. Cria os tokens
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // 4. Imprime os tokens
            tokens.fill();

            System.out.println("=========================================");
            System.out.println("           TOKENS IDENTIFICADOS          ");
            System.out.println("=========================================");

            for (Token token : tokens.getTokens()) {
                if (token.getType() == Token.EOF) {
                    continue;
                }

                String tipo = meuLexico.VOCABULARY.getSymbolicName(token.getType());
                String valor = token.getText();

                System.out.printf(
                        "%-15s -> %s%n",
                        tipo,
                        valor
                );
            }

            tokens.seek(0);
            // ====================================================================

            // 5. Inicia o Parser
            LinguagemParser parser = new LinguagemParser(tokens);

            parser.removeErrorListeners();
            parser.addErrorListener(caoDeGuarda);

            // 6. Roda a análise sintática
            ParseTree tree = parser.prog();

            // 7. Roda a análise semântica
            System.out.println("\nIniciando Análise Semântica...");
            SemanticoVisitor semantico = new SemanticoVisitor();
            semantico.visit(tree);
            // ====================================================================

            //  8. Gerador de código
            System.out.println("Iniciando Geração de Código...");
            GeradorCodigo gerador = new GeradorCodigo();
            gerador.visit(tree);

            // ====================================================================

            System.out.println("=========================================");
            System.out.println("           RESULTADO DA ANÁLISE          ");
            System.out.println("=========================================");
            System.out.println(tree.toStringTree(parser));
            System.out.println("=========================================");
            System.out.println("\n✅ Compilação concluída com sucesso (0 erros)!");

        } catch (RuntimeException e) {
            System.err.println("\n❌ A COMPILAÇÃO FOI INTERROMPIDA!");
            System.err.println("Motivo: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("\n❌ Erro ao ler o arquivo: " + e.getMessage());
        }
    }
}