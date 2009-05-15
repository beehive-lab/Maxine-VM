/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.ins.gui;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.*;

/**
 * Visual interaction specification for Inspectors.
 *
 * @author Michael Van De Vanter
 * @see <a href="http://en.wikipedia.org/wiki/Web_colors#X11_color_names">X11 color names</a>
 */
public interface InspectorStyle {

    /*
     * Short string name of style, suitable for menus
     */
    String name();

    // Window, Frame, Desktop attributes
    /** Default background color for all inspector display elements. */
    Color defaultBackgroundColor();
    /** Default color for all custom borders used in the inspector. */
    Color defaultBorderColor();
    /** Default color used to flash borders. */
    Color frameBorderFlashColor();
    /** Default top-only border for display elements that are parts of a larger frame. */
    Border defaultPaneTopBorder();
    /** Default bottom-only border for display elements that are parts of a larger frame. */
    Border defaultPaneBottomBorder();


    // General visual attributes, for all inspections

    // Default text
    Font defaultFont();
    int defaultTextFontSize();
    Color defaultTextColor();
    Color defaultTextBackgroundColor();

    // Plain text labels
    Font textLabelFont();
    int textLabelFontSize();
    Color textLabelColor();
    Color textLabelBackgroundColor();

    // Defaults for textual titles
    Font textTitleFont();
    int textTitleFontSize();
    Color textTitleColor();
    Color textTitleBackgroundColor();

    // Defaults for integers displayed in decimal
    Font decimalDataFont();
    int decimalDataFontSize();
    Color decimalDataColor();
    Color decimalDataBackgroundColor();

    // Defaults for integers displayed in hex
    Font hexDataFont();
    int hexDataFontSize();
    Color hexDataColor();
    Color hexDataBackgroundColor();

   // Special styles for interpreted machine word  values

    /** font for displaying word data as hex. */
    Font wordDataFont();

    /** font size for displaying word data as hex. */
    int wordDataFontSize();

    /** default foreground color for displaying word data as hex. */
    Color wordDataColor();

    /** default background color for displaying word data as hex. */
    Color wordDataBackgroundColor();

    /** special foreground color for displaying the null word. */
    Color wordNullDataColor();

    /** special foreground color for displaying a word that is a valid reference. */
    Color wordValidObjectReferenceDataColor();

    /** special foreground color for displaying a word that is an unchecked reference. */
    Color wordUncheckedReferenceDataColor();

    /** special foreground color for displaying a word that is an invalid heap object reference. */
    Color wordInvalidObjectReferenceDataColor();

    /** special foreground color for displaying a word that points into a stack. */
    Color wordStackLocationDataColor();

    /** special foreground color for displaying invalid word data. */
    Color wordInvalidDataColor();

    /** special foreground color for displaying a word that is a call entry point. */
    Color wordCallEntryPointColor();

    /** special foreground color for displaying a word that is a call entry point. */
    Color wordCallReturnPointColor();

    /** special foreground color for displaying an unchecked code entry/return point. */
    Color wordUncheckedCallPointColor();

    /** font for displaying textual interpretation of word data. */
    Font wordAlternateTextFont();

    /** foreground color for displaying a selected word (memory inspector). */
    Color wordSelectedColor();

    /** font for displaying word data as flags. */
    Font wordFlagsFont();


    // Display of primitive Java data values
    Font primitiveDataFont();
    int primitiveDataFontSize();
    Color primitiveDataColor();
    Color primitiveDataBackgroundColor();

    // Display of char values
    Font charDataFont();
    int charDataFontSize();
    Color charDataColor();

    // Display of string values
    /** Font to use when displaying generic string data. */
    Font stringDataFont();
    /** Font size to use when displaying generic string data. */
    int stringDataFontSize();
    /** Foreground color to use when displaying generic string data. */
    Color stringDataColor();
    /** Maximum number of string characters to display when displaying a "hint" as to string contents. */
    int maxStringInlineDisplayLength();
    /** Maximum number of elements from a char array to display when viewing as text. */
    int maxStringFromCharArrayDisplayLength();

    // Names for Java entities
    Font javaNameFont();
    int javaNameFontSize();
    Color javaNameColor();
    Color javaNameBackgroundColor();
    Font javaClassNameFont();
    Color javaUnresolvedNameColor();
    Font javaCodeFont();

    // Default display of any kind of code
    Font defaultCodeFont();
    int defaultCodeFontSize();
    Color defaultCodeColor();
    Color defaultCodeBackgroundColor();
    Color defaultCodeAlternateBackgroundColor();
    Color defaultCodeStopBackgroundColor();
    Icon codeViewCloseIcon();

    // Display of target code
    Font defaultTextFont();
    int targetCodeFontSize();
    Color targetCodeColor();
    Color targetCodeBackgroundColor();
    Color targetCodeAlternateBackgroundColor();
    Color targetCodeStopBackgroundColor();

    // Display of bytecodes
    Font bytecodeMnemonicFont();
    Font bytecodeOperandFont();
    int bytecodeFontSize();
    int maxBytecodeOperandDisplayLength();
    Color bytecodeColor();
    Color bytecodeBackgroundColor();
    Color bytecodeMethodEntryColor();

    // Display of source code
    Font sourceCodeFont();
    int sourceCodeFontSize();
    Color sourceCodeColor();
    Color sourcecodeBackgroundColor();

    // Debugger interaction

    Color vmStoppedBackgroundColor();
    Color vmStoppedinGCBackgroundColor();
    Color vmRunningBackgroundColor();
    Color vmTerminatedBackgroundColor();

    Color debugSelectedCodeBorderColor();

    Border debugDefaultTagBorder();
    Color debugDefaultTagColor();
    Icon debugDefaultTagIcon();

    Border debugEnabledTargetBreakpointTagBorder();
    Border debugDisabledTargetBreakpointTagBorder();
    Border debugEnabledBytecodeBreakpointTagBorder();
    Border debugDisabledBytecodeBreakpointTagBorder();

    Color debugIPTextColor();
    Color debugIPTagColor();
    Icon debugIPTagIcon();

    Color debugCallReturnTextColor();
    Color debugCallReturnTagColor();
    Icon debugCallReturnTagIcon();

    Icon debugToggleBreakpointbuttonIcon();
    Icon debugStepOverButtonIcon();
    Icon debugStepInButtonIcon();
    Icon debugStepOutButtonIcon();
    Icon debugRunToCursorButtonIcon();
    Icon debugContinueButtonIcon();
    Icon debugPauseButtonIcon();

    Icon debugActiveRowButtonIcon();

    // Display of memory locations:  object fields, array elements, thread locals, etc.

    /** Color for the border that surrounds the display of a memory location that is the current selected address. */
    Color memorySelectedAddressBorderColor();

    /** Default color for the "tag" text that appears with a memory location. */
    Color memoryDefaultTagTextColor();
    /** Color for the "tag" text that identifies a memory location pointed at by one or more registers. */
    Color memoryRegisterTagTextColor();

    /** Color for the border in a "tag" memory field that shows an enabled watchpoint at the location. */
    Border memoryEnabledWatchpointTagBorder();
    /** Color for the border in a "tag" memory field that shows a disabled watchpoint at the location. */
    Border memoryDisabledWatchpointTagBorder();

    /** Color for text displaying memory contents at a location where the process has hit a watchpoint. */
    Color memoryWatchpointTextColor();


    // Search related
    /** Icon for the search button that selects the next match moving forward.  */
    Icon searchNextMatchButtonIcon();
    /** Icon for the search button that selects the next match moving backward.  */
    Icon searchPreviousMatchButtonIcon();
    /** Background color for patterns typed into a search field that don't match anything. */
    Color searchFailedBackground();
    /** Background color for rows that have been matched by a search. */
    Color searchMatchedBackground();

    // Table-display related
    /** No spacing between cells in table-based views; hides vertical and horizontal lines. */
    Dimension zeroTableIntercellSpacing();

    /** Default spacing between cells in table-based views. */
    Dimension defaultTableIntercellSpacing();
    /** Default row height in table-based views. */
    int defaultTableRowHeight();
    /** Default choice to display horizontal lines in table-based views. */
    boolean defaultTableShowHorizontalLines();
    /** Default choice to display vertical lines in table-based views. */
    boolean defaultTableShowVerticalLines();

    /** Spacing between cells in table-based memory views. */
    Dimension memoryTableIntercellSpacing();
    /** Row height in table-based object views. */
    int memoryTableRowHeight();
    /** Choice to display horizontal lines in table-based memory views. */
    boolean memoryTableShowHorizontalLines();
    /** Choice to display vertical lines in table-based memory views. */
    boolean memoryTableShowVerticalLines();
    /** Default number of rows for initial window sizing of table-based memory views. */
    int memoryTableMaxDisplayRows();

    /** Spacing between cells in table-based code views. */
    Dimension codeTableIntercellSpacing();
    /** Row height in table-based code views. */
    int codeTableRowHeight();
    /** Choice to display horizontal lines in table-based code views. */
    boolean codeTableShowHorizontalLines();
    /** Choice to display vertical lines in table-based code views. */
    boolean codeTableShowVerticalLines();

    // Standard Color Palates.  Please don't change; create new colors if needed.
    // Palate 1:  Primary colors, designed to work well together
    Color SunBlue1 = new Color(83, 130, 161);
    Color SunOrange1 = new Color(231, 111, 0);
    Color SunGreen1 = new Color(178, 188, 0);
    Color SunYellow1 = new Color(255, 199, 38);
    // Palate 2:  Darker versions of the primary colors
    Color SunBlue2 = new Color(53, 85, 107);
    Color SunOrange2 = new Color(192, 102, 0);
    Color SunGreen2 = new Color(127, 120, 0);
    Color SunYellow2 = new Color(198, 146, 0);
    // Palate 3:  Lighter versions of the primary colors
    Color SunBlue3 = new Color(163, 184, 203);
    Color SunOrange3 = new Color(237, 155, 79);
    Color SunGreen3 = new Color(197, 213, 169);
    Color SunYellow3 = new Color(248, 213, 131);
    // Neutral Colors
    Color CoolGray1 = new Color(112, 114, 119);
    Color CoolGray2 = new Color(189, 190, 192);

    // X11 Colors:
    Color LightCoral = new Color(240, 128, 128);
    Color Red = new Color(255, 0, 0);
    Color Pink = new Color(255, 192, 203);
    Color OrangeRed = new Color(255, 69, 0);
    Color DarkOrange = new Color(255, 140, 0);
    Color Orange = new Color(255, 165, 0);
    Color MediumOrchid = new Color(186, 85, 211);

    Color LightGreen = new Color(144, 238, 144);
    Color ForestGreen = new Color(24, 139, 34);
    Color Blue =  new Color(0, 0, 255);
    Color MediumBlue =  new Color(0, 0, 205);

    Color SaddleBrown = new Color(139, 69, 19);
    Color White = new Color(255, 255, 255);
    Color Gainsboro = new Color(220, 220, 220);
    Color Black = new Color(0, 0, 0);
}
