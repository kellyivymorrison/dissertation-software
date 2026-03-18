// Copyright (c) 2026 Kelly Morrison
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License version 3.

package com.grammarview;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GrammarViewAppTest {

    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_GENERAL_ERROR = 1;
    private static final int EXIT_USAGE_ERROR = 2;
    private static final int EXIT_FILE_NOT_FOUND = 3;
    private static final int EXIT_PARSE_ERROR = 4;
    private static final int EXIT_PDF_ERROR = 5;

    @Test
    public void testExitSuccess() {
        // GIVEN: A valid YACC file
        File testFile = new File("examples/test.y");
        
        // WHEN: Executing the app with the valid file
        GrammarViewApp app = new GrammarViewApp();
        CommandLine cmd = new CommandLine(app);
        int exitCode = cmd.execute(testFile.getPath(), "--output", "target/test-output");

        // THEN: The exit code should be SUCCESS
        assertEquals(EXIT_SUCCESS, exitCode);
    }

    @Test
    public void testExitFileNotFound() {
        // GIVEN: A non-existent file
        String nonExistentFile = "non_existent.y";
        
        // WHEN: Executing the app
        GrammarViewApp app = new GrammarViewApp();
        CommandLine cmd = new CommandLine(app);
        int exitCode = cmd.execute(nonExistentFile);

        // THEN: The exit code should be FILE_NOT_FOUND
        assertEquals(EXIT_FILE_NOT_FOUND, exitCode);
    }

    @Test
    public void testExitUsageError() {
        // GIVEN: An invalid font size
        File testFile = new File("examples/test.y");
        
        // WHEN: Executing the app with an out-of-range font size
        GrammarViewApp app = new GrammarViewApp();
        CommandLine cmd = new CommandLine(app);
        int exitCode = cmd.execute(testFile.getPath(), "--font-size", "5");

        // THEN: The exit code should be USAGE_ERROR
        assertEquals(EXIT_USAGE_ERROR, exitCode);
    }

    @Test
    public void testExitParseError() throws Exception {
        // GIVEN: A YACC file with syntax errors
        File invalidFile = File.createTempFile("invalid", ".y");
        try {
            Files.writeString(invalidFile.toPath(), "invalid grammar content");

            // WHEN: Executing the app
            GrammarViewApp app = new GrammarViewApp();
            CommandLine cmd = new CommandLine(app);
            int exitCode = cmd.execute(invalidFile.getPath());

            // THEN: The exit code should be PARSE_ERROR
            assertEquals(EXIT_PARSE_ERROR, exitCode);
        } finally {
            invalidFile.delete();
        }
    }

    @Test
    public void testExitPdfError() throws Exception {
        // GIVEN: A valid YACC file but a mocked PdfGenerator that fails with IOException
        File testFile = new File("examples/test.y");
        
        try (MockedConstruction<PdfGenerator> mocked = mockConstruction(PdfGenerator.class,
                (mock, context) -> {
                    doThrow(new IOException("Mocked PDF generation failure")).when(mock).generate(any());
                })) {
            
            // WHEN: Executing the app
            GrammarViewApp app = new GrammarViewApp();
            CommandLine cmd = new CommandLine(app);
            int exitCode = cmd.execute(testFile.getPath(), "--output", "target/test-output-fail");

            // THEN: The exit code should be PDF_ERROR (5)
            assertEquals(EXIT_PDF_ERROR, exitCode);
        }
    }

    @Test
    public void testExitGeneralError() throws Exception {
        // GIVEN: A valid YACC file but a mocked PdfGenerator that fails with an unexpected RuntimeException
        File testFile = new File("examples/test.y");
        
        try (MockedConstruction<PdfGenerator> mocked = mockConstruction(PdfGenerator.class,
                (mock, context) -> {
                    doThrow(new RuntimeException("Unexpected mock failure")).when(mock).generate(any());
                })) {
            
            // WHEN: Executing the app
            GrammarViewApp app = new GrammarViewApp();
            CommandLine cmd = new CommandLine(app);
            int exitCode = cmd.execute(testFile.getPath(), "--output", "target/test-output-general-fail");

            // THEN: The exit code should be GENERAL_ERROR (1)
            assertEquals(EXIT_GENERAL_ERROR, exitCode);
        }
    }

    @Test
    public void testParseTest2Y() throws Exception {
        // GIVEN: A GrammarViewApp instance and the test2.y example file
        GrammarViewApp app = new GrammarViewApp();
        File testFile = new File("examples/test2.y");
        assertTrue(testFile.exists(), "Test file test2.y should exist");

        // WHEN: The YACC file is parsed into a GrammarModel
        GrammarModel model = app.parseYacc(testFile);
        List<GrammarModel.Rule> rules = model.getRules();

        // THEN: Verify the structural properties of the parsed grammar
        
        // Verify there are 8 rules
        assertEquals(8, rules.size(), "There should be 8 rules");

        // Verify that "march" is the start symbol (first rule)
        assertEquals("march", rules.get(0).name, "The start symbol should be 'march'");

        // Verify that "embellishment" is nullable
        GrammarModel.Rule embellishment = model.findRule("embellishment");
        assertNotNull(embellishment, "Rule 'embellishment' should exist");
        assertTrue(model.isNullable("embellishment"), "Rule 'embellishment' should be nullable");

        // Verify that "steps" is recursive
        GrammarModel.Rule steps = model.findRule("steps");
        assertNotNull(steps, "Rule 'steps' should exist");
        assertTrue(model.isRecursive("steps"), "Rule 'steps' should be recursive");
    }

    @Test
    public void testParseTestY() throws Exception {
        // GIVEN: A GrammarViewApp instance and the test.y example file
        GrammarViewApp app = new GrammarViewApp();
        File testFile = new File("examples/test.y");
        assertTrue(testFile.exists(), "Test file test.y should exist");

        // WHEN: The YACC file is parsed into a GrammarModel
        GrammarModel model = app.parseYacc(testFile);
        List<GrammarModel.Rule> rules = model.getRules();

        // THEN: Verify the structural properties of the parsed grammar
        
        // Verify there are 5 rules
        assertEquals(5, rules.size(), "There should be 5 rules");

        // Verify that "program" is the start symbol
        assertEquals("program", rules.get(0).name, "The start symbol should be 'program'");

        // Verify that "stmt_list" is nullable
        GrammarModel.Rule stmtList = model.findRule("stmt_list");
        assertNotNull(stmtList, "Rule 'stmt_list' should exist");
        assertTrue(model.isNullable("stmt_list"), "Rule 'stmt_list' should be nullable");

        // Verify that "stmt_list" and "expr" are recursive
        assertTrue(model.isRecursive("stmt_list"), "Rule 'stmt_list' should be recursive");
        GrammarModel.Rule expr = model.findRule("expr");
        assertNotNull(expr, "Rule 'expr' should exist");
        assertTrue(model.isRecursive("expr"), "Rule 'expr' should be recursive");
        
        // Verify that "term" is NOT recursive
        GrammarModel.Rule term = model.findRule("term");
        assertNotNull(term, "Rule 'term' should exist");
        assertFalse(model.isRecursive("term"), "Rule 'term' should NOT be recursive");
    }

    @Test
    public void testRecursiveNullability() throws Exception {
        // GIVEN: A grammar where a rule is nullable because its RHS items are nullable
        // A : /* empty */ ;
        // B : A ;
        // C : B B ;
        GrammarViewApp app = new GrammarViewApp();
        File tempFile = File.createTempFile("nullability", ".y");
        try {
            java.nio.file.Files.writeString(tempFile.toPath(), """
                %%
                A : ;
                B : A ;
                C : B B ;
                %%
                """);

            // WHEN: The grammar is parsed into a GrammarModel
            GrammarModel model = app.parseYacc(tempFile);

            // THEN: A, B, and C should all be nullable
            assertTrue(model.isNullable("A"), "Rule 'A' should be nullable (empty RHS)");
            assertTrue(model.isNullable("B"), "Rule 'B' should be nullable (RHS is A, which is nullable)");
            assertTrue(model.isNullable("C"), "Rule 'C' should be nullable (RHS is B B, and B is nullable)");
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void testParseTest3Y() throws Exception {
        // GIVEN: A GrammarViewApp instance and the test3.y example file
        GrammarViewApp app = new GrammarViewApp();
        File testFile = new File("examples/test3.y");
        assertTrue(testFile.exists(), "Test file test3.y should exist");

        // WHEN: The YACC file is parsed into a GrammarModel
        GrammarModel model = app.parseYacc(testFile);

        // THEN: Verify the structural properties of the parsed grammar
        assertEquals(2, model.getRules().size(), "There should be 2 rules");
        assertEquals("rule", model.getRules().get(0).name, "The first rule should be 'rule'");
        assertEquals(26, model.getRules().get(0).alternatives.get(0).getSymbols().size(), "The first rule should have 26 symbols in its first alternative");
    }
}
