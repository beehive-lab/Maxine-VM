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
    private Border defaultPaneBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, defaultBorderColor());
    public Border defaultPaneBorder() {
        return defaultPaneBorder;
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

    // Defaults for integers displayed in decimal
    public Font decimalDataFont() {
        return defaultFont();
    }

    // Defaults for integers displayed in hex
    public Font hexDataFont() {
        return defaultFont();
    }

    // Special styles for interpreted machine word values
    public Font wordDataFont() {
        return hexDataFont();
    }

    public Color wordNullDataColor() {
        return null;
    }
    public Color wordValidObjectReferenceDataColor() {
        return null;
    }
    public Color wordUncheckedReferenceDataColor() {
        return null;
    }
    public Color wordInvalidObjectReferenceDataColor() {
        return null;
    }
    public  Color wordInvalidDataColor() {
        return null;
    }
    public Color wordStackLocationDataColor() {
        return null;
    }
    public Color wordCallEntryPointColor() {
        return null;
    }
    public Color wordCallReturnPointColor() {
        return null;
    }
    public Color wordUncheckedCallPointColor() {
        return null;
    }
    public Font wordAlternateTextFont() {
        return defaultFont();
    }
    public Color wordSelectedColor() {
        return null;
    }
    public Font wordFlagsFont() {
        return defaultFont();
    }


    // Display of primitive Java data
    public Font primitiveDataFont() {
        return defaultFont();
    }
    public Color primitiveDataColor() {
        return null;
    }

    // Display of char values
    public Font charDataFont() {
        return defaultFont();
    }

    // Display of string values
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
    public Color javaUnresolvedNameColor() {
        return null;
    }


    // Default display of any kind of code

    private static final Icon codeViewCloseIcon = IconFactory.createCrossIcon(16, 16);
    public Icon codeViewCloseIcon() {
        return codeViewCloseIcon;
    }

    // Display of machine code

    // Display of  bytecodes
    public Font bytecodeMnemonicFont() {
        return bytecodeOperandFont();
    }
    public Font bytecodeOperandFont() {
        return null;
    }
    public int maxBytecodeOperandDisplayLength() {
        return maxStringInlineDisplayLength();
    }
    public Color bytecodeMethodEntryColor() {
        return null;
    }

    // Debugger interaction

    private static final Dimension debugTagIconSize = new Dimension(16, 12);

    public Color debugSelectedCodeBorderColor() {
        return Color.black;
    }

    public Color debugDefaultTagColor() {
        return InspectorStyle.Black;
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
        return null;
    }
    public  Color debugIPTagColor() {
        return InspectorStyle.Black;
    }
    private static final Icon debugIPTagIcon = IconFactory.createRightArrow(debugTagIconSize);
    public Icon debugIPTagIcon() {
        return debugIPTagIcon;
    }

    public Color debugCallReturnTextColor() {
        return null;
    }
    public Color debugCallReturnTagColor() {
        return InspectorStyle.Black;
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

    private final Icon generalAboutIcon = createImageIcon("/toolbarButtonGraphics/general/About16.gif", "About");
    public Icon generalAboutIcon() {
        return generalAboutIcon;
    }
    private final Icon generalCopyIcon = createImageIcon("/toolbarButtonGraphics/general/Copy16.gif", "Copy");
    public Icon generalCopyIcon() {
        return generalCopyIcon;
    }
    private final Icon generalCutIcon = createImageIcon("/toolbarButtonGraphics/general/Cut16.gif", "Cut");
    public Icon generalCutIcon() {
        return generalCutIcon;
    }
    private final Icon generalDeleteIcon = createImageIcon("/toolbarButtonGraphics/general/Delete16.gif", "Delete");
    public Icon generalDeleteIcon() {
        return generalDeleteIcon;
    }
    private final Icon generalEditIcon = createImageIcon("/toolbarButtonGraphics/general/Edit16.gif", "Edit");
    public Icon generalEditIcon() {
        return generalEditIcon;
    }
    private final Icon generalFindIcon = createImageIcon("/toolbarButtonGraphics/general/Find16.gif", "Find");
    public Icon generalFindIcon() {
        return generalFindIcon;
    }
    private final Icon generalFindAgainIcon = createImageIcon("/toolbarButtonGraphics/general/FindAgain16.gif", "FindAgain");
    public Icon generalFindAgainIcon() {
        return generalFindAgainIcon;
    }
    private final Icon generalHelpIcon = createImageIcon("/toolbarButtonGraphics/general/Help16.gif", "Help");
    public Icon generalHelpIcon() {
        return generalHelpIcon;
    }
    private final Icon generalInformationIcon = createImageIcon("/toolbarButtonGraphics/general/Information16.gif", "Information");
    public Icon generalInformationIcon() {
        return generalInformationIcon;
    }
    private final Icon generalPreferencesIcon = createImageIcon("/toolbarButtonGraphics/general/Preferences16.gif", "Preferences");
    public Icon generalPreferencesIcon() {
        return generalPreferencesIcon;
    }
    private final Icon generalRedoIcon = createImageIcon("/toolbarButtonGraphics/general/Redo16.gif", "Redo");
    public Icon generalRedoIcon() {
        return generalRedoIcon;
    }
    private final Icon generalRefreshIcon = createImageIcon("/toolbarButtonGraphics/general/Refresh16.gif", "Refresh");
    public Icon generalRefreshIcon() {
        return generalRefreshIcon;
    }
    private final Icon generalRemoveIcon = createImageIcon("/toolbarButtonGraphics/general/Remove16.gif", "Remove");
    public Icon generalRemoveIcon() {
        return generalRemoveIcon;
    }
    private final Icon generalReplaceIcon = createImageIcon("/toolbarButtonGraphics/general/Replace16.gif", "Replace");
    public Icon generalReplaceIcon() {
        return generalReplaceIcon;
    }
    private final Icon generalUndoIcon = createImageIcon("/toolbarButtonGraphics/general/Undo16.gif", "Undo");
    public Icon generalUndoIcon() {
        return generalUndoIcon;
    }

    private final Icon navigationUpIcon = createImageIcon("/toolbarButtonGraphics/navigation/Up16.gif", "Move up");
    private final Icon navigationDownIcon = createImageIcon("/toolbarButtonGraphics/navigation/Down16.gif", "Move down");
    private final Icon navigationForwardIcon = createImageIcon("/toolbarButtonGraphics/navigation/Forward16.gif", "Move forward");
    private final Icon navigationBackIcon = createImageIcon("/toolbarButtonGraphics/navigation/Back16.gif", "Move back");
    private final Icon navigationHomeIcon = createImageIcon("/toolbarButtonGraphics/navigation/Home16.gif", "Move home");
    public Icon navigationUpIcon() {
        return navigationUpIcon;
    }
    public Icon navigationDownIcon() {
        return navigationDownIcon;
    }
    public Icon navigationForwardIcon() {
        return navigationForwardIcon;
    }
    public Icon navigationBackIcon() {
        return navigationBackIcon;
    }
    public Icon navigationHomeIcon() {
        return navigationHomeIcon;
    }

    private final Icon mediaStepBackIcon = createImageIcon("/toolbarButtonGraphics/media/StepBack16.gif", "Step back");
    private final Icon mediaStepForwardIcon = createImageIcon("/toolbarButtonGraphics/media/StepForward16.gif", "Step forward");
    public Icon mediaStepBackIcon() {
        return mediaStepBackIcon;
    }
    public Icon mediaStepForwardIcon() {
        return mediaStepForwardIcon;
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
        return InspectorStyle.Black;
    }
    public Color memoryRegisterTagTextColor() {
        //return debugCallReturnTextColor();
        // the above would be more consistent,
        // but for now opt for consistency with the memory word inspector
        // the current colors for return value aren't too good anyway
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
    public Icon searchNextMatchButtonIcon() {
        return navigationForwardIcon();
    }
    public Icon searchPreviousMatchButtonIcon() {
        return navigationBackIcon();
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

    private static final double DARKEN1_FACTOR = 0.95;
    public Color darken1(Color color) {
        return new Color(Math.max((int) (color.getRed()  * DARKEN1_FACTOR), 0),
            Math.max((int) (color.getGreen() * DARKEN1_FACTOR), 0),
            Math.max((int) (color.getBlue() * DARKEN1_FACTOR), 0));
    }

    private static final double DARKEN2_FACTOR = 0.8;
    public Color darken2(Color color) {
        return new Color(Math.max((int) (color.getRed()  * DARKEN2_FACTOR), 0),
            Math.max((int) (color.getGreen() * DARKEN2_FACTOR), 0),
            Math.max((int) (color.getBlue() * DARKEN2_FACTOR), 0));
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
