// Copyright (c) 2026 Kelly Morrison
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License version 3.

package com.grammarview;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * GrammarViewApp is a command-line utility that generates a visual representation 
 * of a YACC/Bison grammar in PDF format.
 */
@Command(name = "grammarview", mixinStandardHelpOptions = true, version = "grammarview 1.0",
        description = "Produces a PDF view of a YACC grammar.")
public class GrammarViewApp implements Callable<Integer> {

    // --- Exit Codes ---
    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_GENERAL_ERROR = 1;
    private static final int EXIT_USAGE_ERROR = 2;
    private static final int EXIT_FILE_NOT_FOUND = 3;
    private static final int EXIT_PARSE_ERROR = 4;
    private static final int EXIT_PDF_ERROR = 5;

    @Parameters(index = "0", description = "The YACC file to process.")
    private File yaccFile;

    @Option(names = {"-legend"}, description = "Add a legend page to the PDF.")
    private boolean legend;

    @Option(names = {"-p", "--portrait"}, description = "Use portrait orientation (default: landscape).")
    private boolean portrait;

    @Option(names = {"--page-size"}, defaultValue = "LETTER", 
            description = "Page size (LETTER, LEGAL, A0, A1, A2, A3, A4, A5, A6).")
    private String pageSizeName;

    @Option(names = {"-s", "--font-size"}, defaultValue = "12", 
            description = "Font size in points (default: 12, range: 6-32).")
    private float fontSize;

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose logging.")
    private boolean verbose;

    private static class GrammarParseException extends Exception {
        GrammarParseException(String message) {
            super(message);
        }
    }

    @Override
    public Integer call() {
        try {
            if (verbose) {
                System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
            }
            if (!yaccFile.exists()) {
                System.err.println("Error: File not found: " + yaccFile.getPath());
                return EXIT_FILE_NOT_FOUND;
            }
            if (fontSize < 6 || fontSize > 32) {
                System.err.println("Error: Font size must be between 6 and 32 points.");
                return EXIT_USAGE_ERROR;
            }
            System.out.println("Processing YACC file: " + yaccFile.getName());
            
            GrammarModel model = parseYacc(yaccFile);
            
            PdfGenerator generator = new PdfGenerator(yaccFile.getName(), fontSize, pageSizeName, portrait, legend);
            generator.generate(model);
            
            return EXIT_SUCCESS;
        } catch (Exception e) {
            System.err.println("Error: An unexpected error occurred: " + e.getMessage());
            if (verbose) e.printStackTrace(System.err);
            return EXIT_GENERAL_ERROR;
        }
    }

    /**
     * Uses the generated ANTLR4 parser to extract rules from the YACC source.
     */
    public GrammarModel parseYacc(File file) throws Exception {
        CharStream input;
        try {
            input = CharStreams.fromPath(file.toPath());
        } catch (IOException e) {
            throw new Exception("Failed to read YACC file: " + e.getMessage());
        }

        BisonLexer lexer = new BisonLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        BisonParser parser = new BisonParser(tokens);
        
        ParseTree tree = parser.input_();

        if (parser.getNumberOfSyntaxErrors() > 0) {
            throw new GrammarParseException("Found " + parser.getNumberOfSyntaxErrors() + " syntax errors in the YACC grammar.");
        }

        List<GrammarModel.Rule> rules = new ArrayList<>();
        ParseTreeWalker walker = new ParseTreeWalker();
        
        walker.walk(new BisonParserBaseListener() {
            @Override
            public void enterRules(BisonParser.RulesContext ctx) {
                if (ctx.id() == null || ctx.rhses_1() == null) return;
                
                GrammarModel.Rule rule = new GrammarModel.Rule(ctx.id().getText());
                for (BisonParser.RhsContext rhsCtx : ctx.rhses_1().rhs()) {
                    List<String> items = new ArrayList<>();
                    for (int i = 0; i < rhsCtx.getChildCount(); i++) {
                        ParseTree child = rhsCtx.getChild(i);
                        if (child instanceof BisonParser.SymbolContext) {
                            items.add(child.getText());
                        }
                    }
                    rule.addAlternative(items);
                }
                rules.add(rule);
            }
        }, tree);

        return new GrammarModel(rules);
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new GrammarViewApp()).execute(args);
        System.exit(exitCode);
    }
}
