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
 * An adaptor for specifying styles.
 *
 * @author Michael Van De Vanter
 */
public abstract class InspectorStyleAdapter implements InspectorStyle {

    // Window, Frame, Desktop attributes
    public Color defaultBackgroundColor() {
        return Color.WHITE;
    }
    public Color defaultBorderColor() {
        return Color.BLACK;
    }
    public Color frameBorderFlashColor() {
        return Color.RED;
    }

    // Default text
    public Color defaultTextColor() {
        return Color.BLACK;
    }
    public Color defaultTextBackgroundColor() {
        return defaultBackgroundColor();
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
    public int maxStringDisplayLength() {
        return 20;
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
    private static final Icon _codeViewCloseIcon = IconFactory.createCrossIcon(16, 16);
    public Icon codeViewCloseIcon() {
        return _codeViewCloseIcon;
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
        return maxStringDisplayLength();
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
    private static final Dimension _debugTagIconSize = new Dimension(16, 12);

    public Color debugSelectedCodeBorderColor() {
        return Color.black;
    }

    private static final Border _debugDefaultTagBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);
    public Border debugDefaultTagBorder() {
        return _debugDefaultTagBorder;
    }
    public Color debugDefaultTagColor() {
        return Color.black;
    }
    private static final Icon _debugDefaultTagIcon = IconFactory.createBlank(_debugTagIconSize);
    public Icon debugDefaultTagIcon() {
        return _debugDefaultTagIcon;
    }

    private static final Border _debugEnabledBreakpointTagBorder = BorderFactory.createLineBorder(Color.black, 2);
    public Border debugEnabledTargetBreakpointTagBorder() {
        return _debugEnabledBreakpointTagBorder;
    }

    private static final Border _debugDisabledBreakpointTagBorder = BorderFactory.createLineBorder(Color.black, 1);
    public Border debugDisabledTargetBreakpointTagBorder() {
        return _debugDisabledBreakpointTagBorder;
    }

    public Border debugEnabledBytecodeBreakpointTagBorder() {
        return debugEnabledTargetBreakpointTagBorder();
    }

    public Border debugDisabledBytecodeBreakpointTagBorder() {
        return debugDisabledTargetBreakpointTagBorder();
    }

    public Color debugIPTextColor() {
        return defaultCodeColor();
    }
    public  Color debugIPTagColor() {
        return debugDefaultTagColor();
    }
    private static final Icon _debugIPTagIcon = IconFactory.createRightArrow(_debugTagIconSize);
    public Icon debugIPTagIcon() {
        return _debugIPTagIcon;
    }

    public Color debugCallReturnTextColor() {
        return defaultCodeColor();
    }
    public Color debugCallReturnTagColor() {
        return debugDefaultTagColor();
    }
    private static final Icon _debugCallReturnTagIcon = IconFactory.createLeftArrow(_debugTagIconSize);
    public Icon debugCallReturnTagIcon() {
        return _debugCallReturnTagIcon;
    }

    private final Icon _debugPauseButtonIcon = createImageIcon("image/NB3Pause.gif", "Continue");
    private final Icon _debugContinueButtonIcon = createImageIcon("image/NB3Continue.gif", "Continue");
    private final Icon _debugRunToCursorButtonIcon = createImageIcon("image/NB3RunToCursor.gif", "Continue");
    private final Icon _debugStepInButtonIcon = createImageIcon("image/NB3StepIn.gif", "Step In");
    private final Icon _debugStepOutButtonIcon = createImageIcon("image/NB3StepOut.gif", "Step In");
    private final Icon _debugStepOverButtonIcon = createImageIcon("image/NB3StepOver.gif", "Step In");
    private final Icon _debugToggleBreakpointButtonIcon = createImageIcon("image/NB3ToggleBreakpoint.gif", "Step In");
    public Icon debugPauseButtonIcon() {
        return _debugPauseButtonIcon;
    }
    public Icon debugContinueButtonIcon() {
        return _debugContinueButtonIcon;
    }
    public Icon debugRunToCursorButtonIcon() {
        return _debugRunToCursorButtonIcon;
    }
    public Icon debugStepInButtonIcon() {
        return _debugStepInButtonIcon;
    }
    public Icon debugStepOutButtonIcon() {
        return _debugStepOutButtonIcon;
    }
    public Icon debugStepOverButtonIcon() {
        return _debugStepOverButtonIcon;
    }
    public Icon debugToggleBreakpointbuttonIcon() {
        return _debugToggleBreakpointButtonIcon;
    }

    private final Icon _debugActiveRowButtonIcon = IconFactory.createRightArrow(16, 16);
    public Icon debugActiveRowButtonIcon() {
        return _debugActiveRowButtonIcon;
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
