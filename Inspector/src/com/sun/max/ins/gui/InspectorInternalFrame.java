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
import com.sun.max.ins.gui.Inspector.*;

/**
 * A internal frame controlled by an {@linkplain Inspector inspector}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
final class InspectorInternalFrame extends JInternalFrame implements InspectorFrameInterface {

    // TODO (mlvdv) Generalize Inspector Frame so that it doesn't have to be a JInternal Frame, for
    // example when adding to Tabbed Containers.  In that case, a JRootPane would do.
    // What would be missing, if this happened?  no title,
    // pack() could be implemented as in JInternalFrame:
    // setSize(getPreferredSize());
    // validate();

    private final Inspector inspector;

    private final InspectorMenuBar menuBar;

    /**
     * Creates an internal frame for an Inspector.
     * @param inspector
     */
    public InspectorInternalFrame(Inspector inspector) {
        this.inspector = inspector;
        menuBar = new InspectorMenuBar(inspector.inspection());
        setJMenuBar(menuBar);
        setResizable(true);
        setClosable(true);
        setIconifiable(true);
        setVisible(false);

        addInternalFrameListener(new InternalFrameAdapter() {

            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                InspectorInternalFrame.this.inspector.inspectorGetsWindowFocus();
            }

            @Override
            public void internalFrameDeactivated(InternalFrameEvent e) {
                InspectorInternalFrame.this.inspector.inspectorLosesWindowFocus();
            }
        });
    }

    public JComponent getJComponent() {
        return this;
    }

    public void refresh(boolean force) {
        menuBar.refresh(force);
    }

    public void redisplay() {
    }

    public Inspector inspector() {
        return inspector;
    }

    /**
     * Finds, and creates if doesn't exist, a named menu on the frame's menu bar.
     * <br>
     * <strong>Note:</strong> the menus will appear left to right on the
     * frame's menu bar in the order in which they were created.
     *
     * @param name the name of the menu
     * @return a menu, possibly new, in the menu bar.
     */
    public InspectorMenu makeMenu(MenuKind menuKind) {
        return menuBar.makeMenu(menuKind);
    }

    public void setSelected() {
        try {
            setSelected(true);
        } catch (PropertyVetoException e) {
        }
    }

    public void flash(Color borderFlashColor) {
        Component pane = getContentPane();
        if (pane instanceof JScrollPane) {
            final JScrollPane scrollPane = (JScrollPane) pane;
            pane = scrollPane.getViewport();
        }
        final Graphics g = pane.getGraphics();
        g.setPaintMode();
        g.setColor(borderFlashColor);
        for (int i = 0; i < 5; i++) {
            g.drawRect(i, i, pane.getWidth() - (i * 2), pane.getHeight() - (i * 2));
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
        }
        g.dispose();
        invalidate();
        repaint();
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
