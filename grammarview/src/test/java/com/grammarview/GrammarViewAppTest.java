// Copyright (c) 2026 Kelly Morrison
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License version 3.

package com.grammarview;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class GrammarViewAppTest {

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
