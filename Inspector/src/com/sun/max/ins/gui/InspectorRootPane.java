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

import javax.swing.*;

import com.sun.max.ins.gui.Inspector.*;
import com.sun.max.program.*;


/**
 * A frame suitable for use by an {@linkplain Inspector inspector}.
 * This is a minimal frame without window system decoration, suitable
 * for used in a tabbed container of inspectors.
 *
 * @author Michael Van De Vanter
 */
final class InspectorRootPane<Inspector_Type extends Inspector> extends JRootPane implements InspectorFrame {

    private final Inspector_Type inspector;
    private final TabbedInspector<Inspector_Type> parent;
    private final InspectorMenuBar menuBar;

    private String title = null;

    /**
     * Creates a simple frame, with content pane, for an Inspector intended to be in a
     * tabbed frame.
     * <br>
     * The frame has an optional menu bar.  It is a program error to call {@link #makeMenu(MenuKind)}
     * if no menu bar is present.
     *
     * @param inspector the inspector that owns this frame
     * @param parent the tabbed frame that will own this frame.
     * @param addMenuBar  should the frame have a menu bar installed.
     * @see #makeMenu(MenuKind)
     */
    public InspectorRootPane(Inspector_Type inspector, TabbedInspector<Inspector_Type> parent, boolean addMenuBar) {
        this.inspector = inspector;
        this.parent = parent;
        menuBar = addMenuBar ? new InspectorMenuBar(inspector.inspection()) : null;
        setJMenuBar(menuBar);
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
        parent.setSelected(inspector);
    }

    public boolean isSelected() {
        return parent.isSelected() && parent.isSelected(inspector);
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

    public void dispose() {
        parent.remove(this);
        inspector.inspectorClosing();
    }

    public String getTitle() {
        return title;
    }

    public void moveToFront() {
        parent.moveToFront();
        setSelected();
    }

    public void pack() {
        setSize(getPreferredSize());
        validate();
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
