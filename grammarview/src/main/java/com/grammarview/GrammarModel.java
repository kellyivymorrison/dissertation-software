// Copyright (c) 2026 Kelly Morrison
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License version 3.

package com.grammarview;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * GrammarModel represents the parsed structure of a YACC/Bison grammar,
 * including its rules, non-terminals, and metadata such as nullability and recursion.
 */
public class GrammarModel {

    /**
     * Represents a single alternative within a grammar rule.
     */
    public static class RuleAlternative {
        private final List<String> symbols;

        public RuleAlternative(List<String> symbols) {
            this.symbols = new ArrayList<>(symbols);
        }

        public List<String> getSymbols() {
            return symbols;
        }

        public boolean isEmpty() {
            return symbols.isEmpty();
        }

        public boolean contains(String symbolName) {
            return symbols.contains(symbolName);
        }
    }

    /**
     * Internal representation of a Grammar Rule.
     */
    public static class Rule {
        public String name;
        /** List of alternatives. */
        public List<RuleAlternative> alternatives = new ArrayList<>();

        public Rule(String name) {
            this.name = name;
        }

        public void addAlternative(List<String> symbols) {
            this.alternatives.add(new RuleAlternative(symbols));
        }
    }

    private final List<Rule> rules;
    private final Set<String> nonTerminals = new HashSet<>();
    private final Set<String> recursiveRules = new HashSet<>();
    private final Set<String> nullableRules = new HashSet<>();

    public GrammarModel(List<Rule> rules) {
        this.rules = rules;
        calculateMetadata();
    }

    public List<Rule> getRules() {
        return rules;
    }

    public Set<String> getNonTerminals() {
        return nonTerminals;
    }

    public boolean isRecursive(String ruleName) {
        return recursiveRules.contains(ruleName);
    }

    public boolean isNullable(String ruleName) {
        return nullableRules.contains(ruleName);
    }

    public Rule findRule(String name) {
        for (Rule rule : rules) {
            if (rule.name.equals(name)) {
                return rule;
            }
        }
        return null;
    }

    /**
     * Analyzes grammar metadata for rendering hints (recursion and nullability).
     */
    private void calculateMetadata() {
        nonTerminals.clear();
        recursiveRules.clear();
        nullableRules.clear();

        for (Rule rule : rules) {
            nonTerminals.add(rule.name);
            for (RuleAlternative alt : rule.alternatives) {
                if (alt.contains(rule.name)) {
                    recursiveRules.add(rule.name);
                }
            }
        }

        // Iterative nullability calculation (Fixed-point iteration)
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Rule rule : rules) {
                if (nullableRules.contains(rule.name)) continue;

                for (RuleAlternative alt : rule.alternatives) {
                    if (alt.isEmpty()) {
                        if (nullableRules.add(rule.name)) {
                            changed = true;
                        }
                        break;
                    } else {
                        // Check if all items in this alternative are nullable non-terminals
                        boolean allNullable = true;
                        for (String item : alt.getSymbols()) {
                            if (!nullableRules.contains(item)) {
                                allNullable = false;
                                break;
                            }
                        }
                        if (allNullable) {
                            if (nullableRules.add(rule.name)) {
                                changed = true;
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
}
