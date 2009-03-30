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
package com.sun.max.ins.debug;

import java.awt.*;

import javax.swing.*;

import com.sun.max.gui.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.value.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.value.*;

/**
 * A panel for displaying a list of registers:  symbolic name and value.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
// TODO (mlvdv) Use background for register heat, shades of yellow?
public abstract class RegisterPanel extends InspectorPanel {

    private final TeleRegisters _registers;

    private static final Color[] _heatColors = {Color.BLACK, Color.BLUE, Color.MAGENTA, Color.RED};

    private final TextLabel[] _nameLabels;
    private final int[] _heat;
    private final WordValueLabel[] _registerValueLabels;
    private Address[] _oldValues;

    protected abstract WordValueLabel.ValueMode registerLabelValueMode(Symbol register);

    protected RegisterPanel(Inspection inspection, TeleRegisters registers) {
        super(inspection);
        _registers = registers;
        final int registerCount = _registers.symbolizer().numberOfValues();
        _nameLabels = new TextLabel[registerCount];
        _heat = new int[registerCount];
        _registerValueLabels = new WordValueLabel[registerCount];
        _oldValues = new Address[registerCount];
        setLayout(new SpringLayout());
        for (Symbol register : _registers.symbolizer()) {
            final int index = register.value();

            final TextLabel nameLabel = new TextLabel(inspection, register.name());
            add(nameLabel);
            _nameLabels[index] = nameLabel;

            final WordValueLabel wordValueLabel = new WordValueLabel(inspection, registerLabelValueMode(register));
            add(wordValueLabel);
            _registerValueLabels[index] = wordValueLabel;
            _oldValues[index] = Address.zero();
        }
        refresh(teleVM().epoch(), true);
        SpringUtilities.makeCompactGrid(this, getComponentCount() / 2, 2, 0, 0, 5, 1);
    }

    public TeleRegisters registers() {
        return _registers;
    }

    public static  RegisterPanel createIntegerRegisterPanel(Inspection inspection, TeleIntegerRegisters registers) {
        return new IntegerRegisterPanel(inspection, registers);
    }

    public static  RegisterPanel createFloatingPointRegisterPanel(Inspection inspection, TeleFloatingPointRegisters registers) {
        return new FloatingPointRegisterPanel(inspection, registers);
    }

    public static  RegisterPanel createStateRegisterPanel(Inspection inspection, TeleStateRegisters registers) {
        return new StateRegisterPanel(inspection, registers);
    }

    @Override
    public final void refresh(long epoch, boolean force) {
        for (Symbol register : _registers.symbolizer()) {
            final int index = register.value();
            final Address newValue = _registers.get(register);
            if (newValue.equals(_oldValues[index])) {
                if (_heat[index] > 0) {
                    _heat[index]--;
                    _nameLabels[index].setForeground(_heatColors[_heat[index]]);
                }
            } else {
                _oldValues[index] = newValue;
                _heat[index] = _heatColors.length - 1;
                _registerValueLabels[index].setValue(new WordValue(newValue));
                _nameLabels[index].setForeground(_heatColors[_heat[index]]);
            }
        }
    }

    @Override
    public final void redisplay() {
        for (WordValueLabel wordValueLabel : _registerValueLabels) {
            wordValueLabel.redisplay();
        }
        // when heat colors become configurable, revise those if needed.
    }
}
