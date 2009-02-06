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
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.vm.value.*;

/**
 * An inspector for VM thread locals in the {@link TeleVM}.
 *
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class ThreadLocalsInspector extends UniqueInspector<ThreadLocalsInspector> {

    /**
     * Finds an existing thread locals inspector for a thread.
     *
     * @return null if doesn't exist
     */
    public static ThreadLocalsInspector get(Inspection inspection, TeleNativeThread teleNativeThread) {
        return ThreadLocalsInspectorContainer.getInspector(inspection, teleNativeThread);
    }

    /**
     * Displays and highlights a thread locals inspector for the currently selected thread.
     */
    public static ThreadLocalsInspector make(Inspection inspection) {
        return make(inspection, inspection.focus().thread());
    }

    /**
     * Display and highlight a thread locals inspector for a thread.
     */
    public static ThreadLocalsInspector make(Inspection inspection, TeleNativeThread teleNativeThread) {
        final ThreadLocalsInspector threadLocalsInspector = ThreadLocalsInspectorContainer.makeInspector(inspection, teleNativeThread);
        if (threadLocalsInspector != null) {
            threadLocalsInspector.highlight();
            return threadLocalsInspector;
        }
        return null;
    }

    private final TeleNativeThread _teleNativeThread;
    private ThreadLocalsInspectorContainer _parent;

    private ThreadLocalsPanel _enabledThreadLocalsPanel;
    private ThreadLocalsPanel _disabledThreadLocalsPanel;
    private ThreadLocalsPanel _triggeredThreadLocalsPanel;

    private final ThreadLocalsViewPreferences _globalPreferences;
    private final ThreadLocalsViewPreferences _instancePreferences;

    public ThreadLocalsInspector(Inspection inspection, TeleNativeThread teleNativeThread, ThreadLocalsInspectorContainer parent) {
        super(inspection, parent.residence(),  LongValue.from(teleNativeThread.id()));
        _parent = parent;
        _teleNativeThread = teleNativeThread;

        _globalPreferences = ThreadLocalsViewPreferences.globalPreferences(inspection());
        _instancePreferences = new ThreadLocalsViewPreferences(_globalPreferences) {
            @Override
            public void setIsVisible(ThreadLocalsColumnKind columnKind, boolean visible) {
                super.setIsVisible(columnKind, visible);
                reconstructView();
            }
        };

        createFrame(new InspectorMenu());
        frame().menu().addSeparator();
        frame().menu().add(new InspectorAction(inspection, "View Options") {
            @Override
            public void procedure() {
                new TableColumnVisibilityPreferences.Dialog<ThreadLocalsColumnKind>(inspection(), "Thread Locals View Options", _instancePreferences, _globalPreferences);
            }
        });
        refreshView(inspection.teleVM().epoch(), true);
    }

    @Override
    public void createView(long epoch) {

        final JTabbedPane tabbedPane = new JTabbedPane();

        final TeleVMThreadLocalValues enabledVmThreadLocalValues = _teleNativeThread.stack().enabledVmThreadLocalValues();
        final TeleVMThreadLocalValues disabledVmThreadLocalValues = _teleNativeThread.stack().disabledVmThreadLocalValues();
        final TeleVMThreadLocalValues triggeredVmThreadLocalValues = _teleNativeThread.stack().triggeredVmThreadLocalValues();

        _enabledThreadLocalsPanel = new ThreadLocalsPanel(this, enabledVmThreadLocalValues, _instancePreferences);
        _disabledThreadLocalsPanel = new ThreadLocalsPanel(this, disabledVmThreadLocalValues, _instancePreferences);
        _triggeredThreadLocalsPanel = new ThreadLocalsPanel(this, triggeredVmThreadLocalValues, _instancePreferences);

        tabbedPane.add("Enabled", _enabledThreadLocalsPanel);
        tabbedPane.add("Disabled", _disabledThreadLocalsPanel);
        tabbedPane.add("Triggered", _triggeredThreadLocalsPanel);

        frame().setContentPane(tabbedPane);
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
        _enabledThreadLocalsPanel.refresh(epoch, force);
        _disabledThreadLocalsPanel.refresh(epoch, force);
        _triggeredThreadLocalsPanel.refresh(epoch, force);
        super.refreshView(epoch, force);
    }

    public void viewConfigurationChanged(long epoch) {
        reconstructView();
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

    TeleNativeThread teleNativeThread() {
        return _teleNativeThread;
    }

    @Override
    public String getTextForTitle() {
        return inspection().nameDisplay().longName(_teleNativeThread);
    }

}
