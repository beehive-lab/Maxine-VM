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

import javax.swing.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.program.*;
import com.sun.max.tele.debug.*;
import com.sun.max.vm.value.*;

/**
 * A tabbed container for {@link RegistersInspector}s.
 *
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class RegistersInspectorContainer extends TabbedInspector<RegistersInspector, RegistersInspectorContainer> {

    /**
     * @return the singleton container instance, if it exists; null otherwise.
     */
    public static RegistersInspectorContainer get(Inspection inspection) {
        return UniqueInspector.find(inspection, RegistersInspectorContainer.class);
    }

    /**
     * Return the singleton RegistersInspectorContainer, creating it if necessary.
     * Policy is to populate container automatically at creation with an inspector for each existing thread.
     */
    private static RegistersInspectorContainer make(Inspection inspection) {
        RegistersInspectorContainer registersInspectorContainer = get(inspection);
        if (registersInspectorContainer == null) {
            Trace.begin(1, "[RegistersInspector] initializing");
            registersInspectorContainer = new RegistersInspectorContainer(inspection, Residence.INTERNAL);
            for (TeleNativeThread thread : inspection.teleVM().teleProcess().threads()) {
                registersInspectorContainer.add(new RegistersInspector(inspection, thread, registersInspectorContainer));
            }
            Trace.end(1, "[RegistersInspector] initializing");
        }
        registersInspectorContainer.updateThreadFocus(inspection.focus().thread());
        return registersInspectorContainer;
    }


    /**
     * Find an existing registers inspector for a thread in the {@link TeleVM}.
     * Will create a fully populated RegistersInspectorContainer if one doesn't already exist.
     * @return null if registers inspector for thread doesn't exist
     */
    public static RegistersInspector getInspector(Inspection inspection, TeleNativeThread teleNativeThread) {
        final RegistersInspectorContainer registersInspectorContainer = make(inspection);
        for (RegistersInspector registersInspector : registersInspectorContainer) {
            if (registersInspector.teleNativeThread().equals(teleNativeThread)) {
                return registersInspector;
            }
        }
        return null;
    }

    /**
     * Returns the registers inspector for a thread in the {@link TeleVM}, creating it if necessary.
     * Creates a RegistersInspectorContainer, if one doesn't already exist, and populates
     * it with registers inspectors for every existing thread.
     */
    public static RegistersInspector makeInspector(Inspection inspection, TeleNativeThread teleNativeThread) {
        final RegistersInspectorContainer registersInspectorContainer = make(inspection);
        // if the container is newly created, it will create an inspector for every thread, so wait to check
        RegistersInspector registersInspector = getInspector(inspection, teleNativeThread);
        if (registersInspector == null) {
            registersInspector = new RegistersInspector(inspection, teleNativeThread, registersInspectorContainer);
            registersInspectorContainer.add(registersInspector);
        }
        return registersInspector;
    }

    private boolean _threadSetNeedsUpdate;

    @Override
    public void threadSetChanged(long epoch) {
        _threadSetNeedsUpdate = true;
        super.threadSetChanged(epoch);
    }

    private final ChangeListener _tabChangeListener = new ChangeListener() {

        public void stateChanged(ChangeEvent event) {
            // A RegistersInspector tab has become visible that was not visible before.
            final RegistersInspector selectedInspector = getSelected();
            // Decide whether to propagate the new thread selection.
            // If the new tab selection agrees with the global thread
            // selection, then this change is just an initialization or update notification.
            if (selectedInspector != null) {
                inspection().focus().setThread(selectedInspector.teleNativeThread());
            }
        }
    };

    private RegistersInspectorContainer(Inspection inspection, Residence residence) {
        super(inspection, residence, inspection.geometry().registersFrameDefaultLocation(), inspection.geometry().registersFramePrefSize(), "registersInspector");
        _threadSetNeedsUpdate = true;
        addChangeListener(_tabChangeListener);
    }

    @Override
    public String getTextForTitle() {
        return "Registers";
    }

    @Override
    public void refreshView(long epoch, boolean force) {
        if (isShowing()) {
            if (_threadSetNeedsUpdate) {
                // Mark all inspectors for possible deletion if their thread is no longer active
                for (RegistersInspector registersInspector : this) {
                    registersInspector.setMarked(true);
                }
                for (TeleNativeThread thread : teleVM().teleProcess().threads()) {
                    final UniqueInspector.Key<RegistersInspector> key = UniqueInspector.Key.create(RegistersInspector.class, LongValue.from(thread.id()));
                    final RegistersInspector registersInspector = UniqueInspector.find(inspection(), key);
                    if (registersInspector == null) {
                        add(new RegistersInspector(inspection(), thread, this));
                    } else {
                        registersInspector.setMarked(false);
                    }
                }
                // Any remaining marked inspectors should be deleted as the threads have gone away
                for (RegistersInspector registersInspector : this) {
                    if (registersInspector.marked()) {
                        close(registersInspector);
                    }
                }
                _threadSetNeedsUpdate = false;
            }
            updateThreadFocus(focus().thread());
            super.refreshView(epoch, force);
        }
    }

    @Override
    public void add(RegistersInspector registersInspector) {
        super.add(registersInspector, registersInspector.getTextForTitle());
        registersInspector.frame().invalidate();
        registersInspector.frame().repaint();
    }


    @Override
    public void threadFocusSet(TeleNativeThread oldTeleNativeThread, TeleNativeThread teleNativeThread) {
        updateThreadFocus(teleNativeThread);
    }

    /**
     * Change the selected tab, if needed, to agree with the global thread selection.
     */
    public void updateThreadFocus(TeleNativeThread selectedThread) {
        for (RegistersInspector inspector : this) {
            if (inspector.teleNativeThread().equals(selectedThread)) {
                if (!isSelected(inspector)) {
                    // Select and highlight the tabbed inspector; triggers change event  that will cause it to be refreshed.
                    inspector.highlight();
                }
                break;
            }
        }
    }

    @Override
    public void inspectorClosing() {
        Trace.line(1, tracePrefix() + " closing");
        removeChangeListener(_tabChangeListener);
        super.inspectorClosing();
    }

    @Override
    public void vmProcessTerminated() {
        dispose();
    }

}
