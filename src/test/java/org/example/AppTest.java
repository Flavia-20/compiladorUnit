package org.example;

import antlr.LinguagemParser;
import antlr.meuLexico;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.example.compilador.codigofinal.GeradorCodigoFinal;
import org.example.compilador.intermediario.CodigoIntermediario;
import org.example.compilador.intermediario.GeradorCodigoIntermediario;
import org.example.compilador.otimizacao.Otimizador;
import org.example.compilador.semantico.AnalisadorSemantico;
import org.example.compilador.semantico.DadosSemanticos.ResultadoSemantico;
import org.example.compilador.semantico.DadosSemanticos.Simbolo;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    public void testPipelineCompletaComProgramaValido()
    {
        LinguagemParser.ProgContext tree = parse(
                "PROGRAM teste;\n" +
                "VAR\n" +
                "x, y: INTEGER;\n" +
                "flag: BOOLEAN;\n" +
                "msg: STRING;\n" +
                "BEGIN\n" +
                "x := 2 + 3 * 4;\n" +
                "y := x + 1;\n" +
                "flag := x > y;\n" +
                "msg := \"ok\";\n" +
                "IF flag THEN WRITE(msg) ELSE WRITE(y);\n" +
                "WHILE flag DO BEGIN flag := false END\n" +
                "END.");

        ResultadoSemantico resultadoSemantico = new AnalisadorSemantico().analisar(tree);
        CodigoIntermediario intermediario = new GeradorCodigoIntermediario(resultadoSemantico).gerar(tree);
        CodigoIntermediario otimizado = new Otimizador().otimizar(intermediario);
        String codigoFinal = new GeradorCodigoFinal().gerar(otimizado);
        Simbolo x = resultadoSemantico.getTabelaSimbolos().buscar("x");
        Simbolo y = resultadoSemantico.getTabelaSimbolos().buscar("y");
        Simbolo flag = resultadoSemantico.getTabelaSimbolos().buscar("flag");
        Simbolo msg = resultadoSemantico.getTabelaSimbolos().buscar("msg");

        assertTrue(intermediario.formatar().contains("flag ="));
        assertTrue(intermediario.formatar().contains("IF"));
        assertTrue(otimizado.formatar().contains("x = 14"));
        assertEquals(0, x.getDeslocamento());
        assertEquals(2, y.getDeslocamento());
        assertEquals(4, flag.getDeslocamento());
        assertEquals(5, msg.getDeslocamento());
        assertTrue(codigoFinal.contains(".data"));
        assertTrue(codigoFinal.contains("x dw 0"));
        assertTrue(codigoFinal.contains("flag db 0"));
        assertTrue(codigoFinal.contains(".text"));
        assertTrue(codigoFinal.contains("mov word ptr [x], 14"));
        assertTrue(codigoFinal.contains("call _print"));
    }

    public void testAnaliseSemanticaDetectaErrosPrincipais()
    {
        LinguagemParser.ProgContext tree = parse(
                "PROGRAM teste;\n" +
                "VAR\n" +
                "x: INTEGER;\n" +
                "x: BOOLEAN;\n" +
                "BEGIN\n" +
                "y := true;\n" +
                "x := true;\n" +
                "IF x THEN WRITE(x)\n" +
                "END.");

        try {
            new AnalisadorSemantico().analisar(tree);
            fail("Era esperado erro semantico.");
        } catch (RuntimeException e) {
            String message = e.getMessage();
            assertTrue(message.contains("ja declarada"));
            assertTrue(message.contains("nao declarada"));
            assertTrue(message.contains("atribuicao invalida"));
            assertTrue(message.contains("condicao do IF"));
        }
    }

    public void testIdentificadorLongoEhTruncado()
    {
        meuLexico lexer = new meuLexico(CharStreams.fromString(
                "PROGRAM identificadorMuitoGrande;\n" +
                "BEGIN\n" +
                "END."));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();

        boolean encontrouIdentificadorTruncado = false;

        for (Token token : tokens.getTokens()) {
            if ("identificadormui".equals(token.getText())) {
                encontrouIdentificadorTruncado = true;
                break;
            }
        }

        assertTrue(encontrouIdentificadorTruncado);
    }

    public void testOtimizacaoAplicaReducaoDeForca()
    {
        LinguagemParser.ProgContext tree = parse(
                "PROGRAM teste;\n" +
                "VAR\n" +
                "x, y: INTEGER;\n" +
                "BEGIN\n" +
                "x := y * 4;\n" +
                "WRITE(x)\n" +
                "END.");

        ResultadoSemantico resultadoSemantico = new AnalisadorSemantico().analisar(tree);
        CodigoIntermediario intermediario = new GeradorCodigoIntermediario(resultadoSemantico).gerar(tree);
        CodigoIntermediario otimizado = new Otimizador().otimizar(intermediario);

        assertTrue(otimizado.formatar().contains("<< 2"));
    }

    private LinguagemParser.ProgContext parse(String source)
    {
        meuLexico lexer = new meuLexico(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LinguagemParser parser = new LinguagemParser(tokens);
        LinguagemParser.ProgContext tree = parser.prog();

        assertEquals(0, parser.getNumberOfSyntaxErrors());
        return tree;
    }
}
