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

import com.sun.max.ins.*;

/**
 * An adaptor for specifying styles.
 *
 * @author Michael Van De Vanter
 */
public abstract class InspectorStyleAdapter extends AbstractInspectionHolder implements InspectorStyle {

    protected InspectorStyleAdapter(Inspection inspection) {
        super(inspection);
    }

    // Window, Frame, Desktop attributes
    public Color defaultBackgroundColor() {
        return InspectorStyle.White;
    }
    public Color defaultBorderColor() {
        return InspectorStyle.Black;
    }
    public Color frameBorderFlashColor() {
        return InspectorStyle.Red;
    }
    private Border defaultPaneTopBorder = BorderFactory.createMatteBorder(2, 0, 0, 0, defaultBorderColor());
    public Border defaultPaneTopBorder() {
        return defaultPaneTopBorder;
    }
    private Border defaultPaneBottomBorder = BorderFactory.createMatteBorder(0, 0, 2, 0, defaultBorderColor());
    public Border defaultPaneBottomBorder() {
        return defaultPaneBottomBorder;
    }
    // Default text
    public Color defaultTextColor() {
        return InspectorStyle.Black;
    }
    public Color defaultTextBackgroundColor() {
        return defaultBackgroundColor();
    }
    public Color defaultErrorTextColor() {
        return InspectorStyle.Red;
    }
    public Color defaultErrorTextBackgroundColor() {
        return InspectorStyle.Yellow;
    }

    // Plain text labels
    public Font textLabelFont() {
        return defaultFont();
    }
    public int textLabelFontSize() {
        return defaultTextFontSize();
    }
    public Color textLabelColor() {
        return defaultTextColor();
    }
    public Color textLabelBackgroundColor() {
        return defaultTextBackgroundColor();
    }

    // Defaults for textual titles
    public Font textTitleFont() {
        return defaultFont();
    }
    public int textTitleFontSize() {
        return defaultTextFontSize();
    }
    public Color textTitleColor() {
        return defaultTextColor();
    }
    public Color textTitleBackgroundColor() {
        return defaultTextBackgroundColor();
    }

    // Defaults for integers displayed in decimal
    public Font decimalDataFont() {
        return defaultFont();
    }
    public int decimalDataFontSize() {
        return defaultTextFontSize();
    }
    public Color decimalDataColor() {
        return defaultTextColor();
    }
    public Color decimalDataBackgroundColor() {
        return defaultTextBackgroundColor();
    }

    // Defaults for integers displayed in hex
    public Font hexDataFont() {
        return defaultFont();
    }
    public int hexDataFontSize() {
        return defaultTextFontSize();
    }
    public Color hexDataColor() {
        return defaultTextColor();
    }
    public Color hexDataBackgroundColor() {
        return defaultTextBackgroundColor();
    }

    // Special styles for interpreted machine word values
    public Font wordDataFont() {
        return hexDataFont();
    }
    public int wordDataFontSize() {
        return hexDataFontSize();
    }
    public Color wordDataColor() {
        return hexDataColor();
    }
    public Color wordDataBackgroundColor() {
        return hexDataBackgroundColor();
    }
    public Color wordNullDataColor() {
        return hexDataColor();
    }
    public Color wordValidObjectReferenceDataColor() {
        return hexDataColor();
    }
    public Color wordUncheckedReferenceDataColor() {
        return hexDataColor();
    }
    public Color wordInvalidObjectReferenceDataColor() {
        return hexDataColor();
    }
    public  Color wordInvalidDataColor() {
        return hexDataColor();
    }
    public Color wordStackLocationDataColor() {
        return hexDataColor();
    }
    public Color wordCallEntryPointColor() {
        return hexDataColor();
    }
    public Color wordCallReturnPointColor() {
        return hexDataColor();
    }
    public Color wordUncheckedCallPointColor() {
        return hexDataColor();
    }
    public Font wordAlternateTextFont() {
        return defaultFont();
    }
    public Color wordSelectedColor() {
        return defaultTextColor();
    }
    public Font wordFlagsFont() {
        return defaultFont();
    }


    // Display of primitive Java data
    public Font primitiveDataFont() {
        return defaultFont();
    }
    public int primitiveDataFontSize() {
        return defaultTextFontSize();
    }
    public Color primitiveDataColor() {
        return defaultTextColor();
    }
    public Color primitiveDataBackgroundColor() {
        return defaultTextBackgroundColor();
    }

    // Display of char values
    public Font charDataFont() {
        return defaultFont();
    }
    public int charDataFontSize() {
        return defaultTextFontSize();
    }
    public Color charDataColor() {
        return defaultTextColor();
    }

    // Display of string values
    public Font stringDataFont() {
        return defaultFont();
    }
    public int stringDataFontSize() {
        return defaultTextFontSize();
    }
    public Color stringDataColor() {
        return defaultTextColor();
    }
    public int maxStringInlineDisplayLength() {
        return 40;
    }
    public int maxStringFromCharArrayDisplayLength() {
        return 200;
    }

    // Names for Java entities
    public Font javaNameFont() {
        return defaultFont();
    }
    public int javaNameFontSize() {
        return defaultTextFontSize();
    }
    public Color javaNameColor() {
        return defaultTextColor();
    }
    public Color javaUnresolvedNameColor() {
        return defaultTextColor();
    }
    public Color javaNameBackgroundColor() {
        return defaultTextBackgroundColor();
    }
    public Font javaClassNameFont() {
        return javaNameFont();
    }
    public Font javaCodeFont() {
        return javaNameFont();
    }

    // Default display of any kind of code
    public Font defaultCodeFont() {
        return defaultFont();
    }
    public int defaultCodeFontSize() {
        return defaultTextFontSize();
    }
    public Color defaultCodeColor() {
        return defaultTextColor();
    }
    public Color defaultCodeBackgroundColor() {
        return defaultTextBackgroundColor();
    }
    public Color defaultCodeAlternateBackgroundColor() {
        return defaultCodeBackgroundColor();
    }
    public Color defaultCodeStopBackgroundColor() {
        return defaultCodeBackgroundColor();
    }
    private static final Icon codeViewCloseIcon = IconFactory.createCrossIcon(16, 16);
    public Icon codeViewCloseIcon() {
        return codeViewCloseIcon;
    }

    // Display of machine code
    public Font defaultTextFont() {
        return defaultCodeFont();
    }
    public int targetCodeFontSize() {
        return defaultCodeFontSize();
    }
    public Color targetCodeColor() {
        return defaultCodeColor();
    }
    public Color targetCodeBackgroundColor() {
        return defaultCodeBackgroundColor();
    }
    public Color targetCodeAlternateBackgroundColor() {
        return defaultCodeAlternateBackgroundColor();
    }
    public Color targetCodeStopBackgroundColor() {
        return defaultCodeStopBackgroundColor();
    }

    // Display of  bytecodes
    public Font bytecodeMnemonicFont() {
        return bytecodeOperandFont();
    }
    public Font bytecodeOperandFont() {
        return defaultCodeFont();
    }
    public int bytecodeFontSize() {
        return defaultCodeFontSize();
    }
    public int maxBytecodeOperandDisplayLength() {
        return maxStringInlineDisplayLength();
    }
    public Color bytecodeColor() {
        return defaultCodeColor();
    }
    public Color bytecodeBackgroundColor() {
        return defaultCodeBackgroundColor();
    }
    public Color bytecodeMethodEntryColor() {
        return defaultCodeColor();
    }

    // Display of source code
    public Font sourceCodeFont() {
        return defaultCodeFont();
    }
    public int sourceCodeFontSize() {
        return defaultCodeFontSize();
    }
    public Color sourceCodeColor() {
        return defaultCodeColor();
    }
    public Color sourcecodeBackgroundColor() {
        return defaultCodeBackgroundColor();
    }

    // Debugger interaction

    public Color vmStoppedBackgroundColor() {
        return defaultBackgroundColor();
    }
    public Color vmStoppedinGCBackgroundColor() {
        return defaultBackgroundColor();
    }
    public Color vmRunningBackgroundColor() {
        return defaultBackgroundColor();
    }
    public Color vmTerminatedBackgroundColor() {
        return defaultBackgroundColor();
    }

    private static final Dimension debugTagIconSize = new Dimension(16, 12);

    public Color debugSelectedCodeBorderColor() {
        return Color.black;
    }

    private static final Border debugDefaultTagBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);
    public Border debugDefaultTagBorder() {
        return debugDefaultTagBorder;
    }
    public Color debugDefaultTagColor() {
        return InspectorStyle.Black;
    }
    private static final Icon debugDefaultTagIcon = IconFactory.createBlank(debugTagIconSize);
    public Icon debugDefaultTagIcon() {
        return debugDefaultTagIcon;
    }

    private static final Border debugEnabledBreakpointTagBorder = BorderFactory.createLineBorder(Color.black, 2);
    public Border debugEnabledTargetBreakpointTagBorder() {
        return debugEnabledBreakpointTagBorder;
    }

    private static final Border debugDisabledBreakpointTagBorder = BorderFactory.createLineBorder(Color.black, 1);
    public Border debugDisabledTargetBreakpointTagBorder() {
        return debugDisabledBreakpointTagBorder;
    }

    public Border debugEnabledBytecodeBreakpointTagBorder() {
        return debugEnabledTargetBreakpointTagBorder();
    }

    public Border debugDisabledBytecodeBreakpointTagBorder() {
        return debugDisabledTargetBreakpointTagBorder();
    }

    public Border watchpointTagBorder() {
        return debugEnabledBreakpointTagBorder;
    }

    public Color debugIPTextColor() {
        return defaultCodeColor();
    }
    public  Color debugIPTagColor() {
        return debugDefaultTagColor();
    }
    private static final Icon debugIPTagIcon = IconFactory.createRightArrow(debugTagIconSize);
    public Icon debugIPTagIcon() {
        return debugIPTagIcon;
    }

    public Color debugCallReturnTextColor() {
        return defaultCodeColor();
    }
    public Color debugCallReturnTagColor() {
        return debugDefaultTagColor();
    }
    private static final Icon debugCallReturnTagIcon = IconFactory.createLeftArrow(debugTagIconSize);
    public Icon debugCallReturnTagIcon() {
        return debugCallReturnTagIcon;
    }

    private final Icon debugPauseButtonIcon = createImageIcon("image/NB3Pause.gif", "Continue");
    private final Icon debugContinueButtonIcon = createImageIcon("image/NB3Continue.gif", "Continue");
    private final Icon debugRunToCursorButtonIcon = createImageIcon("image/NB3RunToCursor.gif", "Continue");
    private final Icon debugStepInButtonIcon = createImageIcon("image/NB3StepIn.gif", "Step In");
    private final Icon debugStepOutButtonIcon = createImageIcon("image/NB3StepOut.gif", "Step In");
    private final Icon debugStepOverButtonIcon = createImageIcon("image/NB3StepOver.gif", "Step In");
    private final Icon debugToggleBreakpointButtonIcon = createImageIcon("image/NB3ToggleBreakpoint.gif", "Step In");
    public Icon debugPauseButtonIcon() {
        return debugPauseButtonIcon;
    }
    public Icon debugContinueButtonIcon() {
        return debugContinueButtonIcon;
    }
    public Icon debugRunToCursorButtonIcon() {
        return debugRunToCursorButtonIcon;
    }
    public Icon debugStepInButtonIcon() {
        return debugStepInButtonIcon;
    }
    public Icon debugStepOutButtonIcon() {
        return debugStepOutButtonIcon;
    }
    public Icon debugStepOverButtonIcon() {
        return debugStepOverButtonIcon;
    }
    public Icon debugToggleBreakpointbuttonIcon() {
        return debugToggleBreakpointButtonIcon;
    }

    private final Icon debugActiveRowButtonIcon = IconFactory.createRightArrow(16, 16);
    public Icon debugActiveRowButtonIcon() {
        return debugActiveRowButtonIcon;
    }

    // Display of memory locations:  object fields, array elements, thread locals, etc.
    // The standard approach is to mimic the way code displays work.

    public Color memorySelectedAddressBorderColor() {
        return debugSelectedCodeBorderColor();
    }
    public Color memoryDefaultTagTextColor() {
        return debugDefaultTagColor();
    }
    public Color memoryRegisterTagTextColor() {
        //return debugCallReturnTextColor();
        // the above would be more consistent,
        // but for now opt for consistency with the memory word inspector
        // the current colors for return value aren't too good anywy
        return wordSelectedColor();
    }
    public Border memoryEnabledWatchpointTagBorder() {
        return debugEnabledTargetBreakpointTagBorder();
    }
    public Border memoryDisabledWatchpointTagBorder() {
        return debugDisabledTargetBreakpointTagBorder();
    }


    public Color memoryWatchpointTextColor() {
        return debugIPTextColor();
    }


    // Search related
    private final Icon searchNextMatchButtonIcon = IconFactory.createDownArrow(14, 14);
    public Icon searchNextMatchButtonIcon() {
        return searchNextMatchButtonIcon;
    }
    private final Icon searchPreviousMatchButtonIcon = IconFactory.createUpArrow(14, 14);
    public Icon searchPreviousMatchButtonIcon() {
        return searchPreviousMatchButtonIcon;
    }
    public Color searchFailedBackground() {
        return Color.red;
    }
    public Color searchMatchedBackground() {
        return SunYellow3;
    }

    // Table-display related
    private final Dimension zeroTableIntercellSpacing  = new Dimension(0, 0);
    public Dimension zeroTableIntercellSpacing() {
        return zeroTableIntercellSpacing;
    }
    private final Dimension defaultTableIntercellSpacing = new Dimension(1, 1);
    public Dimension defaultTableIntercellSpacing() {
        return defaultTableIntercellSpacing;
    }
    private final int defaultTableRowHeight = 20;
    public int defaultTableRowHeight() {
        return defaultTableRowHeight;
    }
    private final boolean defaultTableShowHorizontalLines = true;
    public boolean defaultTableShowHorizontalLines() {
        return defaultTableShowHorizontalLines;
    }
    private final boolean defaultTableShowVertictalLines = true;
    public boolean defaultTableShowVerticalLines() {
        return defaultTableShowVertictalLines;
    }

    public Dimension memoryTableIntercellSpacing() {
        return defaultTableIntercellSpacing();
    }
    public int memoryTableRowHeight() {
        return defaultTableRowHeight();
    }
    public boolean memoryTableShowHorizontalLines() {
        return defaultTableShowHorizontalLines();
    }
    public boolean memoryTableShowVerticalLines() {
        return defaultTableShowVerticalLines();
    }
    public int memoryTableMaxDisplayRows() {
        return 20;
    }

    public Dimension codeTableIntercellSpacing() {
        return defaultTableIntercellSpacing();
    }
    public int codeTableRowHeight() {
        return defaultTableRowHeight();
    }
    public boolean codeTableShowHorizontalLines() {
        return defaultTableShowHorizontalLines();
    }
    public boolean codeTableShowVerticalLines() {
        return defaultTableShowVerticalLines();
    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    protected ImageIcon createImageIcon(String path, String description) {
        final java.net.URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        }
        System.err.println("Couldn't find file: " + path);
        return null;
    }


}
