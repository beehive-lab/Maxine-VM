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
 * A fully specified style, designed to be evolve slowly and resemble earlier prototypes.
 *
 * @author Michael Van De Vanter
 */
public class StandardInspectorStyle extends InspectorStyleAdapter {

    private final int defaultFontSize;
    private final int defaultRowHeight;
    private final String name;

    private Font defaultBoldFont;
    private Font defaultMonospacedFont;

    private int defaultTitleFontSize;
    private Font defaultTitleFont;

    private Font flagsFont;

    private final Color paleGray = InspectorStyle.CoolGray2;
    private final Color paleBlue = InspectorStyle.SunBlue3;
    private final Color backgroundGray = new Color(238, 238, 238);
    private final Color defaultTextColor = InspectorStyle.Black;


    public StandardInspectorStyle(Inspection inspection, int defaultFontSize, int defaultRowHeight) {
        super(inspection);
        this.defaultFontSize = defaultFontSize;
        this.defaultRowHeight = defaultRowHeight;
        name = "Standard-" + defaultFontSize;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name();
    }

    private Font defaultBoldFont() {
        if (defaultBoldFont == null) {
            defaultBoldFont = new Font("SansSerif", Font.BOLD, defaultTextFontSize());
        }
        return defaultBoldFont;
    }

    private Font defaultMonospacedFont() {
        if (defaultMonospacedFont == null) {
            defaultMonospacedFont = new Font("Monospaced", Font.PLAIN, defaultTextFontSize());
        }
        return defaultMonospacedFont;
    }

    @Override
    public Color defaultBackgroundColor() {
        return backgroundGray;
    }

    // Default text
    private Font defaultFont;

    public Font defaultFont() {
        if (defaultFont == null) {
            defaultFont = new Font("SansSerif", Font.PLAIN, defaultTextFontSize());
        }
        return defaultFont;
    }

    public int defaultTextFontSize() {
        return defaultFontSize;
    }
    @Override
    public Color defaultTextColor() {
        return defaultTextColor;
    }

    // Plain text labels
    @Override
    public Font textLabelFont() {
        return defaultBoldFont();
    }

    // Defaults for integers displayed in decimal
    @Override
    public Font decimalDataFont() {
        return defaultFont();
    }
    @Override
    public Color decimalDataColor() {
        return defaultTextColor();
    }

    // Defaults for integers displayed in hex
    @Override
    public Font hexDataFont() {
        return defaultMonospacedFont();
    }
    @Override
    public Color hexDataColor() {
        return defaultTextColor();
    }

    // Special styles for interpreted data values
    private Color nullDataColor = InspectorStyle.SaddleBrown;
    private Color wordValidReferenceDataColor = InspectorStyle.ForestGreen;
    private Color wordUncheckedReferenceDataColor = InspectorStyle.LightGreen;
    private Color wordInvalidReferenceDataColor = InspectorStyle.Red;
    private Color wordStackLocationDataColor = InspectorStyle.MediumOrchid;
    private Color invalidDataColor = InspectorStyle.Red;
    private Color wordCallEntryPointColor = InspectorStyle.MediumBlue;
    private Color wordCallReturnPointColor = new Color(64, 64, 192);
    private Color wordUncheckedCallPointColor = new Color(96, 96, 148);
    private Color wordSelectedColor = InspectorStyle.Blue;

    @Override
    public Font wordDataFont() {
        return hexDataFont();
    }
    @Override
    public Color wordDataColor() {
        return hexDataColor();
    }
    @Override
    public Color wordNullDataColor() {
        return nullDataColor;
    }
    @Override
    public Color wordValidObjectReferenceDataColor() {
        return wordValidReferenceDataColor;
    }
    @Override
    public Color wordUncheckedReferenceDataColor() {
        return wordUncheckedReferenceDataColor;
    }
    @Override
    public Color wordInvalidObjectReferenceDataColor() {
        return wordInvalidReferenceDataColor;
    }
    @Override
    public  Color wordInvalidDataColor() {
        return invalidDataColor;
    }
    @Override
    public Color wordStackLocationDataColor() {
        return wordStackLocationDataColor;
    }
    @Override
    public Color wordCallEntryPointColor() {
        return wordCallEntryPointColor;
    }
    @Override
    public Color wordCallReturnPointColor() {
        return wordCallReturnPointColor;
    }
    @Override
    public Color wordUncheckedCallPointColor() {
        return wordUncheckedCallPointColor;
    }
    @Override
    public Font wordAlternateTextFont() {
        return javaNameFont();
    }
    @Override
    public Color wordSelectedColor() {
        return wordSelectedColor;
    }
    @Override
    public Font wordFlagsFont() {
        if (flagsFont == null) {
            flagsFont = new Font("Serif", Font.PLAIN, defaultTextFontSize());
        }
        return flagsFont;
    }


    // Display of primitive Java data values
    @Override
    public Font primitiveDataFont() {
        return defaultFont();
    }

    // Display of char values

    // Display of string values

    // Names for Java entities
    @Override
    public Font javaNameFont() {
        return defaultFont();
    }
    @Override
    public Font javaClassNameFont() {
        return defaultBoldFont();
    }

    @Override
    public Color javaUnresolvedNameColor() {
        return Color.ORANGE;
    }

    // default display of any kind of code
    private final Color codeAlternateBackgroundColor = paleBlue;
    private final Color codeStopBackgroundColor = paleGray;
    @Override
    public Font defaultCodeFont() {
        return defaultFont();
    }
    @Override
    public Color defaultCodeAlternateBackgroundColor() {
        return codeAlternateBackgroundColor;
    }

    // Display of machine code

    // Display of bytecodes
    private Font bytecodeMnemonicFont;
    private Font bytecodeOperandFont;

    @Override
    public Font bytecodeMnemonicFont() {
        if (bytecodeMnemonicFont == null) {
            bytecodeMnemonicFont = new Font("Serif", Font.ITALIC, defaultTextFontSize() + 1);
        }
        return bytecodeMnemonicFont;
    }
    @Override
    public Font bytecodeOperandFont() {
        if (bytecodeOperandFont == null) {
            bytecodeOperandFont = new Font("SansSerif", Font.PLAIN, defaultTextFontSize());
        }
        return bytecodeOperandFont;
    }
    @Override
    public Color bytecodeMethodEntryColor() {
        return wordCallEntryPointColor();
    }


    // Display of source code


    // Debugger interaction

    private static final Color vmTerminatedBackgroundColor = InspectorStyle.LightCoral;

    @Override
    public Color vmStoppedBackgroundColor() {
        return InspectorStyle.SunBlue3;
    }
    @Override
    public Color vmStoppedinGCBackgroundColor() {
        return InspectorStyle.SunYellow3;
    }
    @Override
    public Color vmRunningBackgroundColor() {
        return InspectorStyle.SunGreen3;
    }
    @Override
    public Color vmTerminatedBackgroundColor() {
        return vmTerminatedBackgroundColor;
    }

    private static final Color debugSelectionBorderColor = InspectorStyle.Blue;
    private static final Color debugBreakpointBorderColor = InspectorStyle.Orange;
    private static final Color debugIPTextColor = InspectorStyle.Red;
    private static final Color debugIPTagColor = InspectorStyle.Red;
    private static final Color debugCallReturnTextColor = InspectorStyle.DarkOrange;
    private static final Color debugCallReturnTagColor = InspectorStyle.DarkOrange;

    @Override
    public Color debugSelectedCodeBorderColor() {
        return debugSelectionBorderColor;
    }

    private static final Border debugEnabledTargetBreakpointTagBorder =
        BorderFactory.createLineBorder(debugBreakpointBorderColor, 3);
    @Override
    public Border debugEnabledTargetBreakpointTagBorder() {
        return debugEnabledTargetBreakpointTagBorder;
    }

    private static final Border debugDisabledTargetBreakpointTagBorder =
        BorderFactory.createMatteBorder(3, 3, 3, 3, IconFactory.createPixelatedIcon(debugBreakpointBorderColor));
    @Override
    public Border debugDisabledTargetBreakpointTagBorder() {
        return debugDisabledTargetBreakpointTagBorder;
    }

    private static final Border debugEnabledBytecodeBreakpointTagBorder =
        BorderFactory.createLineBorder(debugBreakpointBorderColor, 3);
    @Override
    public Border debugEnabledBytecodeBreakpointTagBorder() {
        return debugEnabledBytecodeBreakpointTagBorder;
    }

    private static final Border debugDisabledBytecodeBreakpointTagBorder =
        BorderFactory.createMatteBorder(3, 3, 3, 3, IconFactory.createPixelatedIcon(debugBreakpointBorderColor));
    @Override
    public Border debugDisabledBytecodeBreakpointTagBorder() {
        return debugDisabledBytecodeBreakpointTagBorder;
    }

    @Override
    public Border watchpointTagBorder() {
        return debugEnabledTargetBreakpointTagBorder();
    }

    @Override
    public Color debugIPTextColor() {
        return debugIPTextColor;
    }
    @Override
    public  Color debugIPTagColor() {
        return debugIPTagColor;
    }
    @Override
    public Color debugCallReturnTextColor() {
        return debugCallReturnTextColor;
    }
    @Override
    public Color debugCallReturnTagColor() {
        return debugCallReturnTagColor;
    }

    // Search
    private final Color searchPatternFailedColor = InspectorStyle.Pink;
    @Override
    public Color searchFailedBackground() {
        return searchPatternFailedColor;
    }
    private final Color searchRowMatchedBackground = new Color(255, 255, 200);  // light yellow, but not as light as X11 LightYellow
    @Override
    public Color searchMatchedBackground() {
        return searchRowMatchedBackground;
    }

    // Table-based Views
    @Override
    public Dimension memoryTableIntercellSpacing() {
        return zeroTableIntercellSpacing();
    }
    @Override
    public int defaultTableRowHeight() {
        return defaultRowHeight;
    }
    @Override
    public boolean memoryTableShowHorizontalLines() {
        return false;
    }
    @Override
    public boolean memoryTableShowVerticalLines() {
        return false;
    }

    @Override
    public Dimension codeTableIntercellSpacing() {
        return zeroTableIntercellSpacing();
    }
    @Override
    public boolean codeTableShowHorizontalLines() {
        return false;
    }
    @Override
    public boolean codeTableShowVerticalLines() {
        return false;
    }

}
