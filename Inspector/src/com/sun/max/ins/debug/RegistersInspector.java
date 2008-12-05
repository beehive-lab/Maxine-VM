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

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.debug.*;
import com.sun.max.vm.value.*;

/**
 * An inspector for thread specific registers in the {@link TeleVM}.
 *
 * @author Doug Simon
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class RegistersInspector extends UniqueInspector<RegistersInspector> {

    /**
     * Finds an existing registers inspector for a thread.
     * @return null if doesn't exist
     */
    public static RegistersInspector get(Inspection inspection, TeleNativeThread teleNativeThread) {
        return RegistersInspectorContainer.getInspector(inspection, teleNativeThread);
    }

    /**
     * Displays and highlights a RegistersInspector for the currently selected thread.
     * @return a possibly new inspector
     */
    public static RegistersInspector make(Inspection inspection) {
        return make(inspection, inspection.focus().thread());
    }

    /**
     * Display and highlight a RegistersInspector for a thread.
     * @return a possibly new inspector
     */
    public static RegistersInspector make(Inspection inspection, TeleNativeThread teleNativeThread) {
        final RegistersInspector registersInspector = RegistersInspectorContainer.makeInspector(inspection, teleNativeThread);
        registersInspector.highlight();
        return registersInspector;
    }

    private final TeleNativeThread _teleNativeThread;
    private RegistersInspectorContainer _parent;
    private RegisterPanel _integerRegisterPanel;
    private RegisterPanel _stateRegisterPanel;
    private RegisterPanel _floatingPointRegisterPanel;

    public RegistersInspector(Inspection inspection, TeleNativeThread teleNativeThread, RegistersInspectorContainer parent) {
        super(inspection, parent.residence(),  LongValue.from(teleNativeThread.id()));
        _parent = parent;
        _teleNativeThread = teleNativeThread;
        createFrame(null);
    }

    @Override
    public void createView(long epoch) {
        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        contentPane.setOpaque(true);

        final TeleIntegerRegisters integerRegisters = _teleNativeThread.integerRegisters();
        _integerRegisterPanel = RegisterPanel.createIntegerRegisterPanel(inspection(), integerRegisters);
        contentPane.add(_integerRegisterPanel);

        final TeleStateRegisters stateRegisters = _teleNativeThread.stateRegisters();
        _stateRegisterPanel = RegisterPanel.createStateRegisterPanel(inspection(), stateRegisters);
        contentPane.add(_stateRegisterPanel);

        final TeleFloatingPointRegisters floatingPointRegisters = _teleNativeThread.floatingPointRegisters();
        _floatingPointRegisterPanel = RegisterPanel.createFloatingPointRegisterPanel(inspection(), floatingPointRegisters);
        contentPane.add(_floatingPointRegisterPanel);

        frame().getContentPane().add(new JScrollPane(contentPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        redisplay();
        refreshView(epoch, true);
    }

    private void redisplay() {
        _integerRegisterPanel.setOpaque(true);
        _integerRegisterPanel.setBackground(style().defaultBackgroundColor());
        _integerRegisterPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, style().defaultBorderColor()));
        _integerRegisterPanel.redisplay();

        _stateRegisterPanel.setOpaque(true);
        _stateRegisterPanel.setBackground(style().defaultBackgroundColor());
        _stateRegisterPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, style().defaultBorderColor()));
        _stateRegisterPanel.redisplay();

        _floatingPointRegisterPanel.setOpaque(true);
        _floatingPointRegisterPanel.setBackground(style().defaultBackgroundColor());
        _floatingPointRegisterPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, style().defaultBorderColor()));
        _floatingPointRegisterPanel.redisplay();
    }

    @Override
    public void moveToFront() {
        if (_parent != null) {
            _parent.setSelected(this);
        } else {
            super.moveToFront();
        }
    }

    @Override
    public void refreshView(long epoch, boolean force) {
        _integerRegisterPanel.refresh(epoch, force);
        _stateRegisterPanel.refresh(epoch, force);
        _floatingPointRegisterPanel.refresh(epoch, force);
        super.refreshView(epoch, force);
    }

    public void viewConfigurationChanged(long epoch) {
        redisplay();
    }

    @Override
    public synchronized void setResidence(Residence residence) {
        final Residence current = residence();
        super.setResidence(residence);
        if (current != residence) {
            if (residence == Residence.INTERNAL) {
                if (_parent != null) {
                    // coming back from EXTERNAL, need to redock
                    _parent.add(this);
                }
                moveToFront();
            } else if (residence == Residence.EXTERNAL) {
                frame().setTitle("Registers " + getTextForTitle());
            }
        }
    }

    public RegisterPanel integerRegisterPanel() {
        return _integerRegisterPanel;
    }

    TeleNativeThread teleNativeThread() {
        return _teleNativeThread;
    }

    @Override
    public String getTextForTitle() {
        return inspection().nameDisplay().longName(_teleNativeThread);
    }

}
