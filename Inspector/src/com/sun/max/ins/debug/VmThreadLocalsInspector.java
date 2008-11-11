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
 * An inspector for VM thread locals in the {@link TeleVM}.
 *
 * @author Doug Simon
 */
public final class VmThreadLocalsInspector extends UniqueInspector<VmThreadLocalsInspector> {

    /**
     * Finds an existing thread locals inspector for a thread.
     *
     * @return null if doesn't exist
     */
    public static VmThreadLocalsInspector get(Inspection inspection, TeleNativeThread teleNativeThread) {
        return VmThreadLocalsInspectorContainer.getInspector(inspection, teleNativeThread);
    }

    /**
     * Displays and highlights a thread locals inspector for the currently selected thread.
     */
    public static VmThreadLocalsInspector make(Inspection inspection) {
        return make(inspection, inspection.focus().thread());
    }

    /**
     * Display and highlight a thread locals inspector for a thread.
     */
    public static VmThreadLocalsInspector make(Inspection inspection, TeleNativeThread teleNativeThread) {
        final VmThreadLocalsInspector threadLocalsInspector = VmThreadLocalsInspectorContainer.makeInspector(inspection, teleNativeThread);
        if (threadLocalsInspector != null) {
            threadLocalsInspector.highlight();
            return threadLocalsInspector;
        }
        return null;
    }

    private final TeleNativeThread _teleNativeThread;
    private VmThreadLocalsInspectorContainer _parent;

    private VMThreadLocalsPanel _enabledThreadLocals;
    private VMThreadLocalsPanel _disabledThreadLocals;
    private VMThreadLocalsPanel _triggeredThreadLocals;

    public VmThreadLocalsInspector(Inspection inspection, TeleNativeThread teleNativeThread, VmThreadLocalsInspectorContainer parent) {
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

        final JTabbedPane tabbedPane = new JTabbedPane();

        final TeleVMThreadLocalValues enabledVmThreadLocalValues = _teleNativeThread.stack().enabledVmThreadLocalValues();
        final TeleVMThreadLocalValues disabledVmThreadLocalValues = _teleNativeThread.stack().disabledVmThreadLocalValues();
        final TeleVMThreadLocalValues triggeredVmThreadLocalValues = _teleNativeThread.stack().triggeredVmThreadLocalValues();

        _enabledThreadLocals = new VMThreadLocalsPanel(inspection(), enabledVmThreadLocalValues);
        _disabledThreadLocals = new VMThreadLocalsPanel(inspection(), disabledVmThreadLocalValues);
        _triggeredThreadLocals = new VMThreadLocalsPanel(inspection(), triggeredVmThreadLocalValues);

        tabbedPane.add("Enabled", _enabledThreadLocals);
        tabbedPane.add("Disabled", _disabledThreadLocals);
        tabbedPane.add("Triggered", _triggeredThreadLocals);

        contentPane.add(tabbedPane);

        frame().getContentPane().add(contentPane);
        refreshView(epoch, true);
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
        _enabledThreadLocals.refresh(epoch, force);
        _disabledThreadLocals.refresh(epoch, force);
        _triggeredThreadLocals.refresh(epoch, force);
        super.refreshView(epoch, force);
    }

    public void viewConfigurationChanged(long epoch) {
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
                frame().setTitle("Registers " + getTitle());
            }
        }
    }

    TeleNativeThread teleNativeThread() {
        return _teleNativeThread;
    }

    @Override
    public String getTitle() {
        return inspection().nameDisplay().longName(_teleNativeThread);
    }

}
