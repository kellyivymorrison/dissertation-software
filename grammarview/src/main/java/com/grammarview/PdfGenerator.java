// Copyright (c) 2026 Kelly Morrison
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License version 3.

package com.grammarview;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

/**
 * PdfGenerator handles the rendering of a GrammarModel into a PDF document.
 * This class is immutable.
 */
public final class PdfGenerator {

    private final String yaccFileName;
    private final float fontSize;
    private final String pageSizeName;
    private final boolean portrait;
    private final boolean legend;

    public PdfGenerator(String yaccFileName, float fontSize, String pageSizeName, boolean portrait, boolean legend) {
        this.yaccFileName = yaccFileName;
        this.fontSize = fontSize;
        this.pageSizeName = pageSizeName;
        this.portrait = portrait;
        this.legend = legend;
    }

    /**
     * Renders the extracted grammar rules into a multi-page PDF document.
     */
    public void generate(GrammarModel model) throws IOException {
        String outputFileName = yaccFileName + ".pdf";
        System.out.println("Generating PDF: " + outputFileName + " with font size " + fontSize + ", size " + pageSizeName);
        List<GrammarModel.Rule> rules = model.getRules();
        PDRectangle baseSize = getPageSize(pageSizeName);
        PDRectangle pageSize = portrait ? baseSize : new PDRectangle(baseSize.getHeight(), baseSize.getWidth());
        PDType1Font boldFont = PDType1Font.HELVETICA_BOLD;
        float capHeight = boldFont.getFontDescriptor().getCapHeight() / PdfLayoutConstants.FONT_UNIT_CONVERSION * fontSize;

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(pageSize);
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            PdfSymbolRenderer renderer = new PdfSymbolRenderer(contentStream, boldFont);

            // Title
            contentStream.beginText();
            contentStream.setFont(boldFont, PdfLayoutConstants.FONT_SIZE_TITLE);
            contentStream.newLineAtOffset(PdfLayoutConstants.MARGIN_X, pageSize.getHeight() - PdfLayoutConstants.MARGIN_Y);
            contentStream.showText("Grammar View: " + yaccFileName);
            contentStream.endText();

            float yOffset = pageSize.getHeight() - PdfLayoutConstants.INITIAL_Y_OFFSET;

            for (int ruleIdx = 0; ruleIdx < rules.size(); ruleIdx++) {
                GrammarModel.Rule rule = rules.get(ruleIdx);
                float ruleHeight = calculateRuleHeight(rule, boldFont, fontSize, pageSize.getWidth());

                if (yOffset - ruleHeight < PdfLayoutConstants.BOTTOM_OVERFLOW_LIMIT) {
                    contentStream.close();
                    page = new PDPage(pageSize);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    renderer = new PdfSymbolRenderer(contentStream, boldFont);
                    yOffset = pageSize.getHeight() - PdfLayoutConstants.MARGIN_Y;
                }

                boolean isStartRule = (ruleIdx == 0);
                boolean isRecursive = model.isRecursive(rule.name);
                boolean isNullable = model.isNullable(rule.name);

                float ruleNameWidth = boldFont.getStringWidth(rule.name) / PdfLayoutConstants.FONT_UNIT_CONVERSION * fontSize;
                float boxWidth = ruleNameWidth + (PdfLayoutConstants.BOX_PADDING_X * 2);
                float boxHeight = fontSize + (PdfLayoutConstants.BOX_PADDING_Y * 2);
                float xPos = PdfLayoutConstants.MARGIN_X;
                float boxY = yOffset - PdfLayoutConstants.BOX_PADDING_Y;

                contentStream.setNonStrokingColor(PdfSymbolRenderer.COLOR_SHADOW);
                if (isStartRule) {
                    renderer.drawLHSStartShape(xPos + PdfLayoutConstants.SHADOW_OFFSET, boxY - PdfLayoutConstants.SHADOW_OFFSET, boxWidth, boxHeight, true);
                } else {
                    contentStream.addRect(xPos + PdfLayoutConstants.SHADOW_OFFSET, boxY - PdfLayoutConstants.SHADOW_OFFSET, boxWidth, boxHeight);
                    contentStream.fill();
                }

                Color lhsColor = isNullable ? PdfSymbolRenderer.COLOR_LIGHT_GRAY : PdfSymbolRenderer.COLOR_VIVID_YELLOW;
                renderer.drawNonTerminalShape(xPos, boxY, boxWidth, boxHeight, lhsColor, isRecursive, isStartRule);
                renderer.drawCenteredText(rule.name, xPos, boxY, boxWidth, boxHeight, fontSize, capHeight, isStartRule);

                float lineStartX = xPos + boxWidth;
                float lineStartY = boxY + (boxHeight / 2);
                float midX = lineStartX + PdfLayoutConstants.RHS_START_X_OFFSET / 2;

                for (int i = 0; i < rule.alternatives.size(); i++) {
                    List<String> items = rule.alternatives.get(i).getSymbols();
                    float currentX = xPos + boxWidth + PdfLayoutConstants.RHS_START_X_OFFSET;
                    float currentY = yOffset;
                    float targetY = (currentY - PdfLayoutConstants.BOX_PADDING_Y) + (boxHeight / 2);

                    contentStream.setStrokingColor(PdfSymbolRenderer.COLOR_STROKE);
                    contentStream.moveTo(lineStartX, lineStartY);
                    contentStream.lineTo(midX, lineStartY);
                    contentStream.lineTo(midX, targetY);
                    contentStream.lineTo(currentX, targetY);
                    contentStream.stroke();

                    if (!items.isEmpty()) {
                        for (int j = 0; j < items.size(); j++) {
                            String itemText = items.get(j);
                            float itemWidth = boldFont.getStringWidth(itemText) / PdfLayoutConstants.FONT_UNIT_CONVERSION * fontSize;
                            float rectWidth = itemWidth + (PdfLayoutConstants.BOX_PADDING_X * 2);
                            float rectHeight = fontSize + (PdfLayoutConstants.BOX_PADDING_Y * 2);

                            if (currentX + rectWidth > (pageSize.getWidth() - PdfLayoutConstants.MARGIN_X)) {
                                float xWrapLeft = PdfLayoutConstants.MARGIN_X + PdfLayoutConstants.RHS_WRAP_X_OFFSET;
                                float xWrapRight = pageSize.getWidth() - PdfLayoutConstants.MARGIN_X;
                                float yDrop = (rectHeight + PdfLayoutConstants.SPACING_ALTERNATIVES) * 0.75f;
                                float yTop = targetY;
                                float yMid = targetY - yDrop;
                                float yBottom = targetY - (yDrop * 2);

                                renderer.drawWrappingCurve(currentX, xWrapRight, xWrapLeft, yTop, yMid, yBottom, PdfLayoutConstants.WRAP_RADIUS);
                                yOffset -= (yDrop * 2);
                                currentY -= (yDrop * 2);
                                targetY -= (yDrop * 2);
                                currentX = xWrapLeft + (PdfLayoutConstants.WRAP_RADIUS * 2);
                            }

                            float itemBoxY = currentY - PdfLayoutConstants.BOX_PADDING_Y;
                            boolean isItemRecursive = model.isRecursive(itemText);
                            boolean isItemNullable = model.isNullable(itemText);
                            boolean isTerminal = !model.getNonTerminals().contains(itemText) || itemText.startsWith("'") || itemText.startsWith("\"");

                            if (isTerminal) {
                                renderer.drawRoundedRectangle(currentX, itemBoxY, rectWidth, rectHeight, PdfLayoutConstants.ROUNDED_RECT_RADIUS, PdfSymbolRenderer.COLOR_LIGHT_BLUE);
                            } else {
                                Color itemColor = isItemNullable ? PdfSymbolRenderer.COLOR_LIGHT_GRAY : PdfSymbolRenderer.COLOR_VIVID_YELLOW;
                                renderer.drawNonTerminalShape(currentX, itemBoxY, rectWidth, rectHeight, itemColor, isItemRecursive, false);
                            }
                            renderer.drawCenteredText(itemText, currentX, itemBoxY, rectWidth, rectHeight, fontSize, capHeight, false);

                            currentX += rectWidth;
                            if (j < items.size() - 1) {
                                float nextLineEndX = currentX + PdfLayoutConstants.RHS_ITEM_LINK_LENGTH;
                                if (nextLineEndX <= (pageSize.getWidth() - PdfLayoutConstants.MARGIN_X)) {
                                    contentStream.moveTo(currentX, targetY);
                                    contentStream.lineTo(nextLineEndX, targetY);
                                    contentStream.stroke();
                                    currentX = nextLineEndX;
                                }
                            }
                        }
                    }
                    if (i < rule.alternatives.size() - 1) {
                        yOffset -= (boxHeight + PdfLayoutConstants.SPACING_ALTERNATIVES);
                    }
                }
                yOffset -= (boxHeight + PdfLayoutConstants.SPACING_RULES + (isRecursive ? 10 : 0));
            }

            if (legend) {
                contentStream.close();
                page = new PDPage(pageSize);
                document.addPage(page);
                contentStream = new PDPageContentStream(document, page);
                contentStream.beginText();
                contentStream.setFont(boldFont, PdfLayoutConstants.FONT_SIZE_TITLE);
                contentStream.newLineAtOffset(PdfLayoutConstants.MARGIN_X, pageSize.getHeight() - PdfLayoutConstants.MARGIN_Y);
                contentStream.showText("Legend (All text is BOLD)");
                contentStream.endText();
                float legendY = pageSize.getHeight() - PdfLayoutConstants.INITIAL_Y_OFFSET;
                String[] legendTexts = {
                    "Yellow Rectangular: Rule Definition or Non-terminal Symbol",
                    "Light Gray Rectangular: Nullable Symbol (Has at least one empty RHS)",
                    "Light Blue Rounded Rect: Terminal Item (Token/Literal)",
                    "Stacked Symbols: Recursive Symbol (Appears on its own RHS)",
                    "< Shape: Start/Top-level Rule",
                    "Empty Line: Epsilon (Represents an empty rule)"
                };
                for (String text : legendTexts) {
                    contentStream.beginText();
                    contentStream.setFont(boldFont, fontSize);
                    contentStream.newLineAtOffset(PdfLayoutConstants.MARGIN_X, legendY);
                    contentStream.showText(text);
                    contentStream.endText();
                    legendY -= PdfLayoutConstants.LEGEND_LINE_SPACING;
                }
            }
            contentStream.close();
            document.save(outputFileName);
        }
    }

    private float calculateRuleHeight(GrammarModel.Rule rule, PDType1Font font, float fontSize, float pageWidth) throws IOException {
        float boxHeight = fontSize + (PdfLayoutConstants.BOX_PADDING_Y * 2);
        float totalHeight = 0;
        float ruleNameWidth = font.getStringWidth(rule.name) / PdfLayoutConstants.FONT_UNIT_CONVERSION * fontSize;
        float lhsWidth = ruleNameWidth + (PdfLayoutConstants.BOX_PADDING_X * 2);

        for (int i = 0; i < rule.alternatives.size(); i++) {
            List<String> items = rule.alternatives.get(i).getSymbols();
            float currentX = PdfLayoutConstants.MARGIN_X + lhsWidth + PdfLayoutConstants.RHS_START_X_OFFSET;
            float altHeight = boxHeight;

            for (int j = 0; j < items.size(); j++) {
                String itemText = items.get(j);
                float itemWidth = font.getStringWidth(itemText) / PdfLayoutConstants.FONT_UNIT_CONVERSION * fontSize;
                float rectWidth = itemWidth + (PdfLayoutConstants.BOX_PADDING_X * 2);

                if (currentX + rectWidth > (pageWidth - PdfLayoutConstants.MARGIN_X)) {
                    float yDrop = (boxHeight + PdfLayoutConstants.SPACING_ALTERNATIVES) * 0.75f;
                    altHeight += (yDrop * 2);
                    currentX = PdfLayoutConstants.MARGIN_X + PdfLayoutConstants.RHS_WRAP_X_OFFSET + (PdfLayoutConstants.WRAP_RADIUS * 2);
                }
                currentX += rectWidth;
                if (j < items.size() - 1) {
                    currentX += PdfLayoutConstants.RHS_ITEM_LINK_LENGTH;
                }
            }
            totalHeight += altHeight;
            if (i < rule.alternatives.size() - 1) {
                totalHeight += PdfLayoutConstants.SPACING_ALTERNATIVES;
            }
        }
        return Math.max(boxHeight, totalHeight);
    }

    private PDRectangle getPageSize(String name) {
        switch (name.toUpperCase()) {
            case "A0": return PDRectangle.A0;
            case "A1": return PDRectangle.A1;
            case "A2": return PDRectangle.A2;
            case "A3": return PDRectangle.A3;
            case "A4": return PDRectangle.A4;
            case "A5": return PDRectangle.A5;
            case "A6": return PDRectangle.A6;
            case "LEGAL": return PDRectangle.LEGAL;
            case "LETTER":
            default: return PDRectangle.LETTER;
        }
    }
}
