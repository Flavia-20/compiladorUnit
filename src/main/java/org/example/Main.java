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
import org.antlr.v4.runtime.tree.ParseTree;

public class Main {
    public static void main(String[] args) {
        try {
            // 1. Carrega o arquivo de texto
            String arquivoTeste = "/Users/joaodionizio/GitHub/compiladorUnit/src/main/java/erroLexico.txt";
            CharStream input = CharStreams.fromFileName(arquivoTeste);

            // 2. Inicia o Lexer
            meuLexico lexer = new meuLexico(input);

            // ====================================================================
            // 🛡️ O CÃO DE GUARDA (CUSTOM ERROR LISTENER) 🛡️
            // ====================================================================
            BaseErrorListener caoDeGuarda = new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                        int line, int charPositionInLine,
                                        String msg, RecognitionException e) {
                    // Aqui é onde a mágica acontece: Lança uma exceção e MATA o programa
                    throw new RuntimeException("Erro na linha " + line + ", coluna " + charPositionInLine + ": " + msg);
                }
            };

            // Remove o comportamento padrão bonzinho e adiciona o nosso no Lexer
            lexer.removeErrorListeners();
            lexer.addErrorListener(caoDeGuarda);
            // ====================================================================

            // 3. Cria os tokens
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // 4. Inicia o Parser
            LinguagemParser parser = new LinguagemParser(tokens);

            // Adiciona o Cão de Guarda no Parser também (para erros de sintaxe)
            parser.removeErrorListeners();
            parser.addErrorListener(caoDeGuarda);

            // 5. Roda a compilação
            ParseTree tree = parser.prog();

            // Se chegou aqui, significa que o Cão de Guarda não mordeu (nenhum erro)
            System.out.println("=========================================");
            System.out.println("           RESULTADO DA ANÁLISE          ");
            System.out.println("=========================================");
            System.out.println(tree.toStringTree(parser));
            System.out.println("=========================================");
            System.out.println("\n✅ Compilação concluída com sucesso (0 erros)!");

        } catch (RuntimeException e) {
            // Captura a exceção lançada pelo Cão de Guarda e imprime no console
            System.err.println("\n❌ A COMPILAÇÃO FOI INTERROMPIDA FATALMENTE!");
            System.err.println("Motivo: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("\n❌ Erro ao ler o arquivo: " + e.getMessage());
        }
    }
}
