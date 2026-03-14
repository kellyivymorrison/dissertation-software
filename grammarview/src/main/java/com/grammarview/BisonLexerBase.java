// Copyright (c) 2026 Kelly Morrison
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License version 3.

package com.grammarview;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;

public abstract class BisonLexerBase extends Lexer {

    int percent_percent_count;

    protected BisonLexerBase(CharStream input) {
        super(input);
    }

    public void NextMode()
    {
        ++percent_percent_count;
        if (percent_percent_count == 1) {
            return;
        } else if (percent_percent_count == 2) {
            this.pushMode(BisonLexer.EpilogueMode);
            return;
        } else {
            this.setType(BisonLexer.PercentPercent);
            return;
        }
    }

    @Override
    public void reset() {
        percent_percent_count = 0;
        super.reset();
    }   
}
