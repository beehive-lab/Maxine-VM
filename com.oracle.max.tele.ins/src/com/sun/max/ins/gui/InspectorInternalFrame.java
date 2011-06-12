/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.ins.gui;

import java.awt.*;
import java.beans.*;

import javax.swing.*;
import javax.swing.event.*;

import com.sun.max.ins.gui.AbstractView.MenuKind;
import com.sun.max.ins.util.*;

/**
 * A internal frame controlled by an {@linkplain AbstractView view}.
 * This is the simple form of such a frame, designed to be used inside
 * a Swing {@link JDesktopPane}.  It has the usual frame decorations,
 * including a title along with affordances for resizing, minimizing,
 * and closing (which is interpreted as a request to remove or "dispose")
 * of an individual view.
 */
final class InspectorInternalFrame extends JInternalFrame implements InspectorFrame {

    private final AbstractView view;
    private final InspectorMenuBar menuBar;

    /**
     * Creates an internal frame, with content pane, for an Inspector intended to be in
     * a {@link JDesktopPane}.
     * <br>
     * The frame has an optional menu bar.  It is a program error to call {@link #makeMenu(MenuKind)}
     * if no menu bar is present.
     *
     * @param view
     * @param addMenuBar should the frame have a menu bar installed.
     * @see #makeMenu(MenuKind)
     */
    public InspectorInternalFrame(AbstractView view, boolean addMenuBar) {
        this.view = view;
        menuBar = addMenuBar ? new InspectorMenuBar(view.inspection()) : null;
        setJMenuBar(menuBar);
        setResizable(true);
        setClosable(true);
        setIconifiable(true);
        setVisible(false);

        // Catch user focus events, where a window either becomes or ceases to
        // be the currently "selected" window in the window system.
        addInternalFrameListener(new InternalFrameAdapter() {

            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                InspectorInternalFrame.this.view.viewGetsWindowFocus();
                refresh(true);
            }

            @Override
            public void internalFrameDeactivated(InternalFrameEvent e) {
                InspectorInternalFrame.this.view.viewLosesWindowFocus();
            }
        });
    }

    public JComponent getJComponent() {
        return this;
    }

    /** {@inheritDoc}
     * <p>
     * The frame itself has no display state that would be sensitive to VM
     * state, but there may be menu items that might, for example to
     * enable/disable certain commands.
     */
    public void refresh(boolean force) {
        if (menuBar != null) {
            menuBar.refresh(force);
        }
    }

    /** {@inheritDoc}
     * <p>
     * The window system does not need to be explicitly redisplayed when some
     * display preference is changed; that is handled by the window system
     * itself once we've set it.
     */
    public void redisplay() {
    }

    public AbstractView view() {
        return view;
    }

    public InspectorMenu makeMenu(MenuKind menuKind) throws InspectorError {
        InspectorError.check(menuBar != null);
        return menuBar.makeMenu(menuKind);
    }

    public void setSelected() {
        try {
            if (isIcon()) {
                setIcon(false);
            }
            setSelected(true);
        } catch (PropertyVetoException e) {
            InspectorError.unexpected();
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
        view.viewClosing();
    }

}
