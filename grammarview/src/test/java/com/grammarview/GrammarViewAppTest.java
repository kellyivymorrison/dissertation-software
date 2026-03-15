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

        // WHEN: The YACC file is parsed into rule objects
        List<GrammarViewApp.Rule> rules = app.parseYacc(testFile);
        java.util.Set<String> nullableRules = calculateNullableRules(rules);

        // THEN: Verify the structural properties of the parsed grammar
        
        // Verify there are 8 rules
        assertEquals(8, rules.size(), "There should be 8 rules");

        // Verify that "march" is the start symbol (first rule)
        assertEquals("march", rules.get(0).name, "The start symbol should be 'march'");

        // Verify that "embellishment" is nullable
        GrammarViewApp.Rule embellishment = findRule(rules, "embellishment");
        assertNotNull(embellishment, "Rule 'embellishment' should exist");
        assertTrue(isNullable(embellishment, nullableRules), "Rule 'embellishment' should be nullable");

        // Verify that "steps" is recursive
        GrammarViewApp.Rule steps = findRule(rules, "steps");
        assertNotNull(steps, "Rule 'steps' should exist");
        assertTrue(isRecursive(steps), "Rule 'steps' should be recursive");
    }

    @Test
    public void testParseTestY() throws Exception {
        // GIVEN: A GrammarViewApp instance and the test.y example file
        GrammarViewApp app = new GrammarViewApp();
        File testFile = new File("examples/test.y");
        assertTrue(testFile.exists(), "Test file test.y should exist");

        // WHEN: The YACC file is parsed into rule objects
        List<GrammarViewApp.Rule> rules = app.parseYacc(testFile);
        java.util.Set<String> nullableRules = calculateNullableRules(rules);

        // THEN: Verify the structural properties of the parsed grammar
        
        // Verify there are 5 rules
        assertEquals(5, rules.size(), "There should be 5 rules");

        // Verify that "program" is the start symbol
        assertEquals("program", rules.get(0).name, "The start symbol should be 'program'");

        // Verify that "stmt_list" is nullable
        GrammarViewApp.Rule stmtList = findRule(rules, "stmt_list");
        assertNotNull(stmtList, "Rule 'stmt_list' should exist");
        assertTrue(isNullable(stmtList, nullableRules), "Rule 'stmt_list' should be nullable");

        // Verify that "stmt_list" and "expr" are recursive
        assertTrue(isRecursive(stmtList), "Rule 'stmt_list' should be recursive");
        GrammarViewApp.Rule expr = findRule(rules, "expr");
        assertNotNull(expr, "Rule 'expr' should exist");
        assertTrue(isRecursive(expr), "Rule 'expr' should be recursive");
        
        // Verify that "term" is NOT recursive
        GrammarViewApp.Rule term = findRule(rules, "term");
        assertNotNull(term, "Rule 'term' should exist");
        assertFalse(isRecursive(term), "Rule 'term' should NOT be recursive");
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
            java.nio.file.Files.writeString(tempFile.toPath(), "%% \n A : ; \n B : A ; \n C : B B ; \n %%");

            // WHEN: The grammar is parsed and nullability is calculated
            List<GrammarViewApp.Rule> rules = app.parseYacc(tempFile);
            java.util.Set<String> nullableRules = calculateNullableRules(rules);

            // THEN: A, B, and C should all be nullable
            assertTrue(nullableRules.contains("A"), "Rule 'A' should be nullable (empty RHS)");
            assertTrue(nullableRules.contains("B"), "Rule 'B' should be nullable (RHS is A, which is nullable)");
            assertTrue(nullableRules.contains("C"), "Rule 'C' should be nullable (RHS is B B, and B is nullable)");
        } finally {
            tempFile.delete();
        }
    }

    private GrammarViewApp.Rule findRule(List<GrammarViewApp.Rule> rules, String name) {
        for (GrammarViewApp.Rule rule : rules) {
            if (rule.name.equals(name)) {
                return rule;
            }
        }
        return null;
    }

    private boolean isNullable(GrammarViewApp.Rule rule, java.util.Set<String> nullableRules) {
        return nullableRules.contains(rule.name);
    }

    private java.util.Set<String> calculateNullableRules(List<GrammarViewApp.Rule> rules) {
        java.util.Set<String> nullableRules = new java.util.HashSet<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (GrammarViewApp.Rule rule : rules) {
                if (nullableRules.contains(rule.name)) continue;
                for (List<String> alt : rule.alternatives) {
                    if (alt.isEmpty()) {
                        if (nullableRules.add(rule.name)) changed = true;
                        break;
                    } else {
                        boolean allNullable = true;
                        for (String item : alt) {
                            if (!nullableRules.contains(item)) {
                                allNullable = false;
                                break;
                            }
                        }
                        if (allNullable) {
                            if (nullableRules.add(rule.name)) changed = true;
                            break;
                        }
                    }
                }
            }
        }
        return nullableRules;
    }

    private boolean isRecursive(GrammarViewApp.Rule rule) {
        for (List<String> alt : rule.alternatives) {
            if (alt.contains(rule.name)) {
                return true;
            }
        }
        return false;
    }
}
