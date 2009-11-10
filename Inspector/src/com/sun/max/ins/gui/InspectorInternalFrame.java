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

import com.sun.max.ins.gui.Inspector.*;
import com.sun.max.program.*;

/**
 * A internal frame controlled by an {@linkplain Inspector inspector}.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
final class InspectorInternalFrame extends JInternalFrame implements InspectorFrame {

    private final Inspector inspector;
    private final InspectorMenuBar menuBar;

    /**
     * Creates an internal frame, with content pane, for an Inspector intended to be in
     * a {@link JDesktopPane}.
     * <br>
     * The frame has an optional menu bar.  It is a program error to call {@link #makeMenu(MenuKind)}
     * if no menu bar is present.
     *
     * @param inspector
     * @param addMenuBar should the frame have a menu bar installed.
     * @see #makeMenu(MenuKind)
     */
    public InspectorInternalFrame(Inspector inspector, boolean addMenuBar) {
        this.inspector = inspector;
        menuBar = addMenuBar ? new InspectorMenuBar(inspector.inspection()) : null;
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
        if (menuBar != null) {
            menuBar.refresh(force);
        }
    }

    public void redisplay() {
    }

    public Inspector inspector() {
        return inspector;
    }

    public InspectorMenu makeMenu(MenuKind menuKind) throws ProgramError {
        ProgramError.check(menuBar != null);
        return menuBar.makeMenu(menuKind);
    }

    public void setSelected() {
        try {
            if (isIcon()) {
                setIcon(false);
            }
            setSelected(true);
        } catch (PropertyVetoException e) {
            ProgramError.unexpected();
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

    public void setStateColor(Color color) {
        if (menuBar != null) {
            menuBar.setBackground(color);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        inspector.inspectorClosing();
    }

}
