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
import java.beans.*;

import javax.swing.*;
import javax.swing.event.*;

import com.sun.max.ins.*;

/**
 * An internal frame with convenience methods for positioning, a menu bar, and a default menu.
 *
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 */
public final class InternalInspectorFrame extends JInternalFrame implements InspectorFrame {

    private final Inspector inspector;

    public Inspector inspector() {
        return inspector;
    }

    private final InspectorMenuBar menuBar;

    public Container asContainer() {
        return this;
    }

    /**
     * Creates an internal frame for an Inspector.
     * @param ins
     * @param menu an optional menu, replaces default inspector menu if non-null
     */
    public InternalInspectorFrame(Inspector ins, InspectorMenu menu) {
        this.inspector = ins;

        menuBar = new InspectorMenuBar(ins.inspection());
        menuBar.add((menu == null) ? ins.createDefaultMenu() : menu);
        setJMenuBar(menuBar);

        setResizable(true);
        setClosable(true);
        setIconifiable(false);
        setVisible(false);

        addInternalFrameListener(new InternalFrameAdapter() {

            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                inspector.inspectorGetsWindowFocus();
            }

            @Override
            public void internalFrameDeactivated(InternalFrameEvent e) {
                inspector.inspectorLosesWindowFocus();
            }
        });
    }

    public void refresh(boolean force) {
        menuBar.refresh(force);
    }

    public void redisplay() {
    }

    public InspectorMenu getMenu(String name) {
        return menuBar.findMenu(name);
    }

    public void setSelected() {
        try {
            setSelected(true);
        } catch (PropertyVetoException e) {
        }
    }

    public void flash(Color borderFlashColor) {
        InspectorFrame.Static.flash(this, borderFlashColor);
    }

    @Override
    public void dispose() {
        super.dispose();
        inspector.inspectorClosing();
    }

    private InspectorAction frameClosingAction;
    private InternalFrameListener frameClosingListener;

    public void replaceFrameCloseAction(InspectorAction action) {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        if (frameClosingAction != null) {
            removeInternalFrameListener(frameClosingListener);
        }
        frameClosingAction = action;
        frameClosingListener = new InternalFrameAdapter() {
            @Override
            public void internalFrameClosing(InternalFrameEvent we) {
                frameClosingAction.perform();
            }
        };
        addInternalFrameListener(frameClosingListener);
    }

}
