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
 * A fully specified style, designed to be evolve slowly and resemble earlier prototypes.
 *
 * @author Michael Van De Vanter
 * @see <a href="http://en.wikipedia.org/wiki/Web_colors#HTML_color_names">X11 color names</a>
 *
 */
public class StandardInspectorStyle extends InspectorStyleAdapter {

    @Override
    public String name() {
        return "Standard";
    }

    private final int _defaultFontSize = 12;
    private final Font _defaultFont = new Font("SansSerif", Font.PLAIN, _defaultFontSize);
    private final Font _defaultBoldFont = new Font("SansSerif", Font.BOLD, _defaultFontSize);
    private final Color _defaultTextColor = InspectorStyle.Black;

    private final Font _hexDataFont = new Font("Monospaced", Font.PLAIN, _defaultFontSize);

    private final int _defaultTitleFontSize = _defaultFontSize + 2;
    private final Font _defaultTitleFont = new Font("Serif", Font.BOLD, _defaultTitleFontSize);

    private final Font _flagsFont = new Font("Serif", Font.PLAIN, _defaultFontSize);




    private final Color _paleGray = InspectorStyle.CoolGray2;
    private final Color _paleBlue = InspectorStyle.SunBlue3;

    private final Color _backgroundGray = new Color(238, 238, 238); // very pale

    @Override
    public Color defaultBackgroundColor() {
        return _backgroundGray;
    }

    // Default text
    public Font defaultFont() {
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
        return _defaultBoldFont;
    }

    // Defaults for textual titles
    @Override
    public Font textTitleFont() {
        return _defaultTitleFont;
    }
    @Override
    public int textTitleFontSize() {
        return _defaultTitleFontSize;
    }
    @Override
    public Color textTitleColor() {
        return defaultTextColor();
    }

    // Defaults for integers displayed in decimal
    @Override
    public Font decimalDataFont() {
        return _defaultFont;
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
        return _hexDataFont;
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
    private Color _nullDataColor = new Color(139, 69, 19); // X11 SaddleBrown
    private Color _wordValidReferenceDataColor = new Color(24, 139, 34); // X11 ForestGreen
    private Color _wordUncheckedReferenceDataColor = new Color(144, 238, 144); // X11 LightGreen
    private Color _wordInvalidReferenceDataColor = Color.red;
    private Color _wordStackLocationDataColor = new Color(186, 85, 211); // X11 MediumOrchid
    private Color _invalidDataColor = Color.red;
    private Color _wordCallEntryPointColor = new Color(0, 0, 205); // X11 MediumBlue
    private Color _wordCallReturnPointColor = new Color(64, 64, 192);
    private Color _wordUncheckedCallPointColor = new Color(96, 96, 148);
    private Color _wordSelectedColor = Color.blue;

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
        return _flagsFont;
    }


    // Display of primitive Java data values
    @Override
    public Font primitiveDataFont() {
        return _defaultFont;
    }
    @Override
    public int primitiveDataFontSize() {
        return _defaultFontSize;
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
        return _defaultFont;
    }
    @Override
    public Font javaClassNameFont() {
        return _defaultBoldFont;
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
        return _defaultFont;
    }
    @Override
    public int defaultCodeFontSize() {
        return _defaultFontSize;
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
        return _defaultBoldFont;
    }
    @Override
    public int targetCodeFontSize() {
        return _defaultFontSize;
    }

    // Display of bytecodes
    private final Font _bytecodeMnemonicFont = new Font("Serif", Font.ITALIC, _defaultFontSize + 1);
    private final Font _bytecodeOperandFont = new Font("SansSerif", Font.PLAIN, _defaultFontSize);

    @Override
    public Font bytecodeMnemonicFont() {
        return _bytecodeMnemonicFont;
    }
    @Override
    public Font bytecodeOperandFont() {
        return _bytecodeOperandFont;
    }
    @Override
    public int bytecodeFontSize() {
        return _defaultFontSize + 1;
    }

    @Override
    public Color bytecodeMethodEntryColor() {
        return wordCallEntryPointColor();
    }


    // Display of source code


    // Debugger interaction
    private static final Color _debugSelectionBorderColor = Color.BLUE;
    private static final Color _debugBreakpointBorderColor = Color.ORANGE;
    private static final Color _debugIPTextColor = Color.RED;
    private static final Color _debugIPTagColor = Color.RED;
    private static final Color _debugCallReturnTextColor = Color.ORANGE;
    private static final Color _debugCallReturnTagColor = Color.ORANGE;

    @Override
    public Color debugSelectedCodeBorderColor() {
        return _debugSelectionBorderColor;
    }

    private static final Border _debugEnabledTargetBreakpointTagBorder =
        BorderFactory.createLineBorder(_debugBreakpointBorderColor, 2);
    @Override
    public Border debugEnabledTargetBreakpointTagBorder() {
        return _debugEnabledTargetBreakpointTagBorder;
    }

    private static final Border _debugDisabledTargetBreakpointTagBorder =
        BorderFactory.createMatteBorder(2, 2, 2, 2, IconFactory.createPixelatedIcon(_debugBreakpointBorderColor));
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
    private final Color _searchPatternFailedColor = new Color(255, 192, 203); // X11 Pink
    @Override
    public Color searchPatternFailedColor() {
        return _searchPatternFailedColor;
    }
    private final Color _searchRowMatchedBackground = new Color(255, 255, 200);  // light yellow, but not as light as X11 LightYellow
    @Override
    public Color searchRowMatchedBackground() {
        return _searchRowMatchedBackground;
    }


}
