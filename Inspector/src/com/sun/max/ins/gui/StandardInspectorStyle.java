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

    private final int _defaultFontSize;
    private final int _defaultRowHeight;
    private final String _name;

    private Font _defaultBoldFont;
    private Font _defaultMonospacedFont;

    private int _defaultTitleFontSize;
    private Font _defaultTitleFont;

    private Font _flagsFont;

    private final Color _paleGray = InspectorStyle.CoolGray2;
    private final Color _paleBlue = InspectorStyle.SunBlue3;
    private final Color _backgroundGray = new Color(238, 238, 238);
    private final Color _defaultTextColor = InspectorStyle.Black;


    public StandardInspectorStyle(Inspection inspection, int defaultFontSize, int defaultRowHeight) {
        super(inspection);
        _defaultFontSize = defaultFontSize;
        _defaultRowHeight = defaultRowHeight;
        _name = "Standard-" + defaultFontSize;
    }

    @Override
    public String name() {
        return _name;
    }

    @Override
    public String toString() {
        return name();
    }

    private Font defaultBoldFont() {
        if (_defaultBoldFont == null) {
            _defaultBoldFont = new Font("SansSerif", Font.BOLD, defaultTextFontSize());
        }
        return _defaultBoldFont;
    }

    private Font defaultMonospacedFont() {
        if (_defaultMonospacedFont == null) {
            _defaultMonospacedFont = new Font("Monospaced", Font.PLAIN, defaultTextFontSize());
        }
        return _defaultMonospacedFont;
    }

    @Override
    public Color defaultBackgroundColor() {
        return _backgroundGray;
    }

    // Default text
    private Font _defaultFont;

    public Font defaultFont() {
        if (_defaultFont == null) {
            _defaultFont = new Font("SansSerif", Font.PLAIN, defaultTextFontSize());
        }
        return _defaultFont;
    }
    public int defaultTextFontSize() {
        return _defaultFontSize;
    }
    @Override
    public Color defaultTextColor() {
        return _defaultTextColor;
    }
    @Override
    public Color defaultTextBackgroundColor() {
        return defaultBackgroundColor();
    }

    // Plain text labels
    @Override
    public Font textLabelFont() {
        return defaultBoldFont();
    }

    // Defaults for textual titles
    @Override
    public Font textTitleFont() {
        if (_defaultTitleFont == null) {
            _defaultTitleFont = new Font("Serif", Font.BOLD, textTitleFontSize());
        }
        return _defaultTitleFont;
    }
    @Override
    public int textTitleFontSize() {
        if (_defaultTitleFontSize == 0) {
            _defaultTitleFontSize =  defaultTextFontSize() + 2;
        }
        return _defaultTitleFontSize;
    }
    @Override
    public Color textTitleColor() {
        return defaultTextColor();
    }

    // Defaults for integers displayed in decimal
    @Override
    public Font decimalDataFont() {
        return defaultFont();
    }
    @Override
    public int decimalDataFontSize() {
        return defaultTextFontSize();
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
    public int hexDataFontSize() {
        return defaultTextFontSize();
    }
    @Override
    public Color hexDataColor() {
        return defaultTextColor();
    }

    // Special styles for interpreted data values
    private Color _nullDataColor = InspectorStyle.SaddleBrown;
    private Color _wordValidReferenceDataColor = InspectorStyle.ForestGreen;
    private Color _wordUncheckedReferenceDataColor = InspectorStyle.LightGreen;
    private Color _wordInvalidReferenceDataColor = InspectorStyle.Red;
    private Color _wordStackLocationDataColor = InspectorStyle.MediumOrchid;
    private Color _invalidDataColor = InspectorStyle.Red;
    private Color _wordCallEntryPointColor = InspectorStyle.MediumBlue;
    private Color _wordCallReturnPointColor = new Color(64, 64, 192);
    private Color _wordUncheckedCallPointColor = new Color(96, 96, 148);
    private Color _wordSelectedColor = InspectorStyle.Blue;

    @Override
    public Font wordDataFont() {
        return hexDataFont();
    }

    @Override
    public int wordDataFontSize() {
        return hexDataFontSize();
    }

    @Override
    public Color wordDataColor() {
        return hexDataColor();
    }
    @Override
    public Color wordNullDataColor() {
        return _nullDataColor;
    }
    @Override
    public Color wordValidObjectReferenceDataColor() {
        return _wordValidReferenceDataColor;
    }
    @Override
    public Color wordUncheckedReferenceDataColor() {
        return _wordUncheckedReferenceDataColor;
    }
    @Override
    public Color wordInvalidObjectReferenceDataColor() {
        return _wordInvalidReferenceDataColor;
    }
    @Override
    public  Color wordInvalidDataColor() {
        return _invalidDataColor;
    }
    @Override
    public Color wordStackLocationDataColor() {
        return _wordStackLocationDataColor;
    }
    @Override
    public Color wordCallEntryPointColor() {
        return _wordCallEntryPointColor;
    }
    @Override
    public Color wordCallReturnPointColor() {
        return _wordCallReturnPointColor;
    }
    @Override
    public Color wordUncheckedCallPointColor() {
        return _wordUncheckedCallPointColor;
    }
    @Override
    public Font wordAlternateTextFont() {
        return javaNameFont();
    }
    @Override
    public Color wordSelectedColor() {
        return _wordSelectedColor;
    }
    @Override
    public Font wordFlagsFont() {
        if (_flagsFont == null) {
            _flagsFont = new Font("Serif", Font.PLAIN, defaultTextFontSize());
        }
        return _flagsFont;
    }


    // Display of primitive Java data values
    @Override
    public Font primitiveDataFont() {
        return defaultFont();
    }
    @Override
    public int primitiveDataFontSize() {
        return defaultTextFontSize();
    }

    // Display of char values

    // Display of string values
    @Override
    public Font stringDataFont() {
        return defaultFont();
    }
    @Override
    public int stringDataFontSize() {
        return defaultTextFontSize();
    }
    @Override
    public Color stringDataColor() {
        return defaultTextColor();
    }
    @Override
    public int maxStringDisplayLength() {
        return 20;
    }

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
    private final Color _codeAlternateBackgroundColor = _paleBlue;
    private final Color _codeStopBackgroundColor = _paleGray;
    @Override
    public Font defaultCodeFont() {
        return defaultFont();
    }
    @Override
    public int defaultCodeFontSize() {
        return defaultTextFontSize();
    }
    @Override
    public Color defaultCodeBackgroundColor() {
        return defaultBackgroundColor();
    }
    @Override
    public Color defaultCodeAlternateBackgroundColor() {
        return _codeAlternateBackgroundColor;
    }
    @Override
    public Color defaultCodeStopBackgroundColor() {
        return _codeStopBackgroundColor;
    }

    // Display of machine code
    @Override
    public Font defaultTextFont() {
        return defaultBoldFont();
    }
    @Override
    public int targetCodeFontSize() {
        return defaultTextFontSize();
    }

    // Display of bytecodes
    private Font _bytecodeMnemonicFont;
    private Font _bytecodeOperandFont;

    @Override
    public Font bytecodeMnemonicFont() {
        if (_bytecodeMnemonicFont == null) {
            _bytecodeMnemonicFont = new Font("Serif", Font.ITALIC, defaultTextFontSize() + 1);
        }
        return _bytecodeMnemonicFont;
    }
    @Override
    public Font bytecodeOperandFont() {
        if (_bytecodeOperandFont == null) {
            _bytecodeOperandFont = new Font("SansSerif", Font.PLAIN, defaultTextFontSize());
        }
        return _bytecodeOperandFont;
    }
    @Override
    public int bytecodeFontSize() {
        return defaultTextFontSize() + 1;
    }

    @Override
    public Color bytecodeMethodEntryColor() {
        return wordCallEntryPointColor();
    }


    // Display of source code


    // Debugger interaction

    private static final Color _vmTerminatedBackgroundColor = InspectorStyle.LightCoral;

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
        return _vmTerminatedBackgroundColor;
    }

    private static final Color _debugSelectionBorderColor = InspectorStyle.Blue;
    private static final Color _debugBreakpointBorderColor = InspectorStyle.Orange;
    private static final Color _debugIPTextColor = InspectorStyle.Red;
    private static final Color _debugIPTagColor = InspectorStyle.Red;
    private static final Color _debugCallReturnTextColor = InspectorStyle.DarkOrange;
    private static final Color _debugCallReturnTagColor = InspectorStyle.DarkOrange;

    @Override
    public Color debugSelectedCodeBorderColor() {
        return _debugSelectionBorderColor;
    }

    private static final Border _debugEnabledTargetBreakpointTagBorder =
        BorderFactory.createLineBorder(_debugBreakpointBorderColor, 3);
    @Override
    public Border debugEnabledTargetBreakpointTagBorder() {
        return _debugEnabledTargetBreakpointTagBorder;
    }

    private static final Border _debugDisabledTargetBreakpointTagBorder =
        BorderFactory.createMatteBorder(3, 3, 3, 3, IconFactory.createPixelatedIcon(_debugBreakpointBorderColor));
    @Override
    public Border debugDisabledTargetBreakpointTagBorder() {
        return _debugDisabledTargetBreakpointTagBorder;
    }

    private static final Border _debugEnabledBytecodeBreakpointTagBorder =
        BorderFactory.createLineBorder(_debugBreakpointBorderColor, 3);
    @Override
    public Border debugEnabledBytecodeBreakpointTagBorder() {
        return _debugEnabledBytecodeBreakpointTagBorder;
    }

    private static final Border _debugDisabledBytecodeBreakpointTagBorder =
        BorderFactory.createMatteBorder(3, 3, 3, 3, IconFactory.createPixelatedIcon(_debugBreakpointBorderColor));
    @Override
    public Border debugDisabledBytecodeBreakpointTagBorder() {
        return _debugDisabledBytecodeBreakpointTagBorder;
    }

    @Override
    public Color debugIPTextColor() {
        return _debugIPTextColor;
    }
    @Override
    public  Color debugIPTagColor() {
        return _debugIPTagColor;
    }
    @Override
    public Color debugCallReturnTextColor() {
        return _debugCallReturnTextColor;
    }
    @Override
    public Color debugCallReturnTagColor() {
        return _debugCallReturnTagColor;
    }

    // Search
    private final Color _searchPatternFailedColor = InspectorStyle.Pink;
    @Override
    public Color searchFailedBackground() {
        return _searchPatternFailedColor;
    }
    private final Color _searchRowMatchedBackground = new Color(255, 255, 200);  // light yellow, but not as light as X11 LightYellow
    @Override
    public Color searchMatchedBackground() {
        return _searchRowMatchedBackground;
    }

    // Table-based Views
    @Override
    public Dimension memoryTableIntercellSpacing() {
        return zeroTableIntercellSpacing();
    }
    @Override
    public int defaultTableRowHeight() {
        return _defaultRowHeight;
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
