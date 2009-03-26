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
import javax.swing.event.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;
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
    private JTabbedPane _tabbedPane;

    private final ThreadLocalsViewPreferences _globalPreferences;
    private final ThreadLocalsViewPreferences _instancePreferences;

    public ThreadLocalsInspector(Inspection inspection, TeleNativeThread teleNativeThread, ThreadLocalsInspectorContainer parent) {
        super(inspection, parent.residence(),  LongValue.from(teleNativeThread.handle()));
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

        _tabbedPane = new JTabbedPane();

        for (Safepoint.State state : Safepoint.State.CONSTANTS) {
            final TeleVMThreadLocalValues values = _teleNativeThread.threadLocalsFor(state);
            if (values != null) {
                final ThreadLocalsPanel panel = new ThreadLocalsPanel(this, values, _instancePreferences);
                _tabbedPane.add(state.toString(), panel);
            }
        }

        _tabbedPane.addChangeListener(new ChangeListener() {
            // Do a refresh whenever there's a tab change, so that the newly exposed pane is sure to be current
            public void stateChanged(ChangeEvent event) {
                refreshView(teleVM().epoch(), true);
            }
        });

        frame().setContentPane(_tabbedPane);
    }

    @Override
    public void moveToFront() {
        if (_parent != null) {
            _parent.setSelected(this);
        } else {
            super.moveToFront();
        }
    }

    private ThreadLocalsPanel threadLocalsPanelFor(Safepoint.State state) {
        for (Component component : _tabbedPane.getComponents()) {
            final ThreadLocalsPanel threadLocalsPanel = (ThreadLocalsPanel) component;
            if (threadLocalsPanel._teleVMThreadLocalValues.safepointState() == state) {
                return threadLocalsPanel;
            }
        }
        return null;
    }

    @Override
    public void refreshView(long epoch, boolean force) {

        boolean panelsAddedOrRemoved = false;
        for (Safepoint.State state : Safepoint.State.CONSTANTS) {
            final TeleVMThreadLocalValues values = _teleNativeThread.threadLocalsFor(state);
            final ThreadLocalsPanel panel = threadLocalsPanelFor(state);
            if (values != null) {
                if (panel == null) {
                    _tabbedPane.add(state.toString(), new ThreadLocalsPanel(this, values, _instancePreferences));
                    panelsAddedOrRemoved = true;
                }
            } else {
                if (panel != null) {
                    _tabbedPane.remove(panel);
                    panelsAddedOrRemoved = true;
                }
            }
        }
        if (panelsAddedOrRemoved) {
            reconstructView();
        }

        // Only need to refresh the panel that's visible, as long as we refresh them when they become visible
        final ThreadLocalsPanel threadLocalsPanel = (ThreadLocalsPanel) _tabbedPane.getSelectedComponent();
        if (threadLocalsPanel != null) {
            threadLocalsPanel.refresh(epoch, force);
        }
        super.refreshView(epoch, force);
    }

    public void viewConfigurationChanged(long epoch) {
        reconstructView();
    }

    @Override
    public void addressFocusChanged(Address oldAddress, Address address) {
        refreshView(teleVM().epoch(), true);
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
                frame().setTitle("Thread Locals View " + getTextForTitle());
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
