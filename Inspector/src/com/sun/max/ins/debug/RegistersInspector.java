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
import com.sun.max.ins.InspectionSettings.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;

/**
 * A singleton inspector that displays specific register contents for the thread in the {@link TeleVM} that is the current user focus.
 *
 * @author Doug Simon
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class RegistersInspector extends Inspector {

    // Set to null when inspector closed.
    private static RegistersInspector _registersInspector;

    /**
     * Displays and highlights the (singleton) inspector, creating it if needed.
     */
    public static RegistersInspector make(Inspection inspection) {
        if (_registersInspector == null) {
            _registersInspector = new RegistersInspector(inspection);
        }
        _registersInspector.highlight();
        return _registersInspector;
    }

    private final SaveSettingsListener _saveSettingsListener = createBasicSettingsClient(this, "registersInspector");

    private TeleNativeThread _teleNativeThread;
    private InspectorPanel _contentPane;
    private RegisterPanel _integerRegisterPanel;
    private RegisterPanel _stateRegisterPanel;
    private RegisterPanel _floatingPointRegisterPanel;

    public RegistersInspector(Inspection inspection) {
        super(inspection);
        Trace.begin(1,  tracePrefix() + " initializing");
        createFrame(null);
        refreshView(inspection.teleVM().epoch(), true);
        if (!inspection.settings().hasComponentLocation(_saveSettingsListener)) {
            frame().setLocation(inspection().geometry().registersFrameDefaultLocation());
            frame().getContentPane().setPreferredSize(inspection().geometry().registersFramePrefSize());
        }
        Trace.end(1,  tracePrefix() + " initializing");
    }

    @Override
    public SaveSettingsListener saveSettingsListener() {
        return _saveSettingsListener;
    }

    @Override
    protected void createView(long epoch) {
        _teleNativeThread = inspection().focus().thread();
        if (_teleNativeThread == null) {
            _contentPane = null;
            frame().setTitle(getTextForTitle());
        } else {
            _contentPane = new InspectorPanel(inspection());
            _contentPane.setLayout(new BoxLayout(_contentPane, BoxLayout.Y_AXIS));
            frame().setTitle(getTextForTitle() + " " + inspection().nameDisplay().longName(_teleNativeThread));

            final TeleIntegerRegisters integerRegisters = _teleNativeThread.integerRegisters();
            _integerRegisterPanel = RegisterPanel.createIntegerRegisterPanel(inspection(), integerRegisters);
            _contentPane.add(_integerRegisterPanel);

            final TeleStateRegisters stateRegisters = _teleNativeThread.stateRegisters();
            _stateRegisterPanel = RegisterPanel.createStateRegisterPanel(inspection(), stateRegisters);
            _contentPane.add(_stateRegisterPanel);

            final TeleFloatingPointRegisters floatingPointRegisters = _teleNativeThread.floatingPointRegisters();
            _floatingPointRegisterPanel = RegisterPanel.createFloatingPointRegisterPanel(inspection(), floatingPointRegisters);
            _contentPane.add(_floatingPointRegisterPanel);
        }
        frame().getContentPane().add(new InspectorScrollPane(inspection(), _contentPane));
    }

    @Override
    public String getTextForTitle() {
        return "Registers: ";
    }

    private long _lastRefreshEpoch = -1;

    @Override
    public void refreshView(long epoch, boolean force) {
        if (epoch > _lastRefreshEpoch || force) {
            _lastRefreshEpoch = epoch;
            _integerRegisterPanel.refresh(epoch, force);
            _stateRegisterPanel.refresh(epoch, force);
            _floatingPointRegisterPanel.refresh(epoch, force);
        }
        super.refreshView(epoch, force);
    }

    @Override
    public void viewConfigurationChanged(long epoch) {
        reconstructView();
    }

    @Override
    public void threadFocusSet(TeleNativeThread oldTeleNativeThread, TeleNativeThread teleNativThread) {
        reconstructView();
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        _registersInspector = null;
        super.inspectorClosing();
    }

}
