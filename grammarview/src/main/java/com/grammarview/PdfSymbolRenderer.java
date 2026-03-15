// Copyright (c) 2026 Kelly Morrison
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License version 3.

package com.grammarview;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Color;
import java.io.IOException;

/**
 * Handles the rendering of various grammar symbols and graphical elements in the PDF.
 */
public class PdfSymbolRenderer {

    // --- Rendering Constants ---
    
    public static final Color COLOR_VIVID_YELLOW = Color.YELLOW;
    public static final Color COLOR_LIGHT_BLUE = new Color(173, 216, 230);
    public static final Color COLOR_LIGHT_GRAY = new Color(211, 211, 211);
    public static final Color COLOR_SHADOW = Color.LIGHT_GRAY;
    public static final Color COLOR_TEXT = Color.BLACK;
    public static final Color COLOR_STROKE = Color.BLACK;

    public static final float FONT_SIZE_TITLE = 18f;
    public static final float FONT_SIZE_DEFAULT = 12f;
    public static final float FONT_UNIT_CONVERSION = 1000f;

    public static final float BOX_PADDING_X = 10f;
    public static final float BOX_PADDING_Y = 6f;
    public static final float SHADOW_OFFSET = 2f;
    public static final float RECURSION_STACK_OFFSET = 4f;
    public static final float START_RULE_INDENT = 10f;
    public static final float ROUNDED_RECT_RADIUS = 5f;

    private final PDPageContentStream contentStream;
    private final PDType1Font font;

    public PdfSymbolRenderer(PDPageContentStream contentStream, PDType1Font font) {
        this.contentStream = contentStream;
        this.font = font;
    }

    /**
     * Draws a non-terminal shape, handling recursion stacking and start-rule shaping.
     */
    public void drawNonTerminalShape(float x, float y, float width, float height, Color fillColor, boolean isRecursive, boolean isStart) throws IOException {
        contentStream.setNonStrokingColor(fillColor);
        contentStream.setStrokingColor(COLOR_STROKE);

        if (isRecursive) {
            // Draw recursive stack (two offset boxes behind)
            for (int stack = 2; stack >= 1; stack--) {
                float sOffset = stack * RECURSION_STACK_OFFSET;
                if (isStart) {
                    drawLHSStartShape(x + sOffset, y + sOffset, width, height, false);
                } else {
                    contentStream.addRect(x + sOffset, y + sOffset, width, height);
                    contentStream.fillAndStroke();
                }
            }
        }

        if (isStart) {
            drawLHSStartShape(x, y, width, height, false);
        } else {
            contentStream.addRect(x, y, width, height);
            contentStream.fillAndStroke();
        }
    }

    /**
     * Draws a rounded rectangle for terminal symbols.
     */
    public void drawRoundedRectangle(float x, float y, float width, float height, float radius, Color fillColor) throws IOException {
        contentStream.setNonStrokingColor(fillColor);
        contentStream.setStrokingColor(COLOR_STROKE);
        float kappa = 0.552284749831f;
        float kRadius = radius * kappa;
        contentStream.moveTo(x + radius, y);
        contentStream.lineTo(x + width - radius, y);
        contentStream.curveTo(x + width - radius + kRadius, y, x + width, y + radius - kRadius, x + width, y + radius);
        contentStream.lineTo(x + width, y + height - radius);
        contentStream.curveTo(x + width, y + height - radius + kRadius, x + width - radius + kRadius, y + height, x + width - radius, y + height);
        contentStream.lineTo(x + radius, y + height);
        contentStream.curveTo(x + radius - kRadius, y + height, x, y + height - radius + kRadius, x, y + height - radius);
        contentStream.lineTo(x, y + radius);
        contentStream.curveTo(x, y + radius - kRadius, x + radius - kRadius, y, x + radius, y);
        contentStream.fillAndStroke();
    }

    /**
     * Draws the special < shape for the start rule.
     */
    public void drawLHSStartShape(float x, float y, float width, float height, boolean fillOnly) throws IOException {
        contentStream.moveTo(x + START_RULE_INDENT, y);
        contentStream.lineTo(x + width, y);
        contentStream.lineTo(x + width, y + height);
        contentStream.lineTo(x + START_RULE_INDENT, y + height);
        contentStream.lineTo(x, y + (height / 2));
        contentStream.closePath();
        if (fillOnly) contentStream.fill(); else contentStream.fillAndStroke();
    }

    /**
     * Renders centered text within a box.
     */
    public void drawCenteredText(String text, float x, float y, float boxWidth, float boxHeight, float fontSize, float capHeight, boolean isStartRule) throws IOException {
        float textWidth = font.getStringWidth(text) / FONT_UNIT_CONVERSION * fontSize;
        float textX = x + (boxWidth - textWidth) / 2 + (isStartRule ? 3 : 0);
        float textY = y + (boxHeight - capHeight) / 2;

        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.setNonStrokingColor(COLOR_TEXT);
        contentStream.newLineAtOffset(textX, textY);
        contentStream.showText(text);
        contentStream.endText();
    }
}
