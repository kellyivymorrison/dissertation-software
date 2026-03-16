// Copyright (c) 2026 Kelly Morrison
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License version 3.

package com.grammarview;

/**
 * Constants used for the layout and rendering dimensions of the generated PDF document.
 * This class is not intended to be instantiated.
 */
public final class PdfLayoutConstants {

    // --- Page Layout ---

    /** Horizontal margin from the edge of the page. */
    public static final float MARGIN_X = 50f;

    /** Vertical margin from the edge of the page. */
    public static final float MARGIN_Y = 50f;

    /** Initial Y offset from the top edge of the page where the content starts. */
    public static final float INITIAL_Y_OFFSET = 100f;

    /** The Y coordinate limit below which a new page is triggered. */
    public static final float BOTTOM_OVERFLOW_LIMIT = 50f;

    // --- Rule and Symbol Layout ---

    /** Horizontal offset for the start of the Right-Hand Side (RHS) of a rule. */
    public static final float RHS_START_X_OFFSET = 40f;

    /** Length of the connecting line between symbols in an alternative. */
    public static final float RHS_ITEM_LINK_LENGTH = 15f;

    /** Vertical spacing between different alternatives of a single rule. */
    public static final float SPACING_ALTERNATIVES = 10f;

    /** Vertical spacing between different grammar rules. */
    public static final float SPACING_RULES = 20f;

    /** Horizontal offset when a long rule wraps to a new line. */
    public static final float RHS_WRAP_X_OFFSET = 40f;

    /** Radius of the curves used when wrapping long rules. */
    public static final float WRAP_RADIUS = 5f;

    // --- Symbol Dimensions and Padding ---

    /** Horizontal padding inside the boxes representing symbols. */
    public static final float BOX_PADDING_X = 10f;

    /** Vertical padding inside the boxes representing symbols. */
    public static final float BOX_PADDING_Y = 6f;

    /** Offset for the shadow effect behind symbols. */
    public static final float SHADOW_OFFSET = 2f;

    /** Offset between stacked boxes representing recursive symbols. */
    public static final float RECURSION_STACK_OFFSET = 4f;

    /** Indentation for the special shape used for the start rule. */
    public static final float START_RULE_INDENT = 10f;

    /** Radius for the corners of terminal symbol boxes. */
    public static final float ROUNDED_RECT_RADIUS = 5f;

    // --- Typography ---

    /** Font size used for the document title. */
    public static final float FONT_SIZE_TITLE = 18f;

    /** Default font size for grammar rules and symbols. */
    public static final float FONT_SIZE_DEFAULT = 12f;

    /** Conversion factor for PDF font units. */
    public static final float FONT_UNIT_CONVERSION = 1000f;

    // --- Legend ---

    /** Vertical line spacing used in the legend page. */
    public static final float LEGEND_LINE_SPACING = 20f;

    private PdfLayoutConstants() {
        // Private constructor to prevent instantiation of utility class.
    }
}
