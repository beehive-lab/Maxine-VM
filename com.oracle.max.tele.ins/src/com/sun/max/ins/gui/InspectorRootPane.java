/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;

import com.sun.max.ins.gui.AbstractView.MenuKind;
import com.sun.max.ins.util.*;

/**
 * A frame suitable for use by an {@linkplain AbstractView view}.
 * This is a minimal frame without window system decoration, suitable
 * for used in a tabbed container of views.
 */
final class InspectorRootPane extends JRootPane implements InspectorFrame {

    private final AbstractView view;
    private final TabbedView parent;

    private String title = null;

    /**
     * Creates a simple frame, with content pane, for a view intended to be in a
     * tabbed frame.
     * <p>
     * The frame has an optional menu bar.  It is a program error to call {@link #makeMenu(MenuKind)}
     * if no menu bar is present.
     *
     * @param view the view that owns this frame
     * @param parent the tabbed frame that will own this frame.
     * @param addMenuBar  should the frame have a menu bar installed.
     * @see #makeMenu(MenuKind)
     */
    public InspectorRootPane(AbstractView view, TabbedView parent, boolean addMenuBar) {
        this.view = view;
        this.parent = parent;
        this.menuBar = addMenuBar ? new InspectorMenuBar(view.inspection()) : null;
        setJMenuBar(menuBar);
    }

    public JComponent getJComponent() {
        return this;
    }

    private InspectorMenuBar menuBar() {
        return (InspectorMenuBar) getJMenuBar();
    }

    /** {@inheritDoc}
     * <p>
     * The frame itself has no display state that would be sensitive to VM state,
     * but there may be menu items that might, for example to enable/disable
     * certain commands.
     */
    public void refresh(boolean force) {
        if (menuBar() != null) {
            menuBar().refresh(force);
        }
    }

    /** {@inheritDoc}
     * <p>
     * The window system does not need to be explicitly redisplayed when some
     * display parameter is changed; that is handled by the window system itself
     * once we've set it.
     */
    public void redisplay() {
    }

    public InspectorView view() {
        return view;
    }

    public InspectorMenu makeMenu(MenuKind menuKind) throws InspectorError {
        InspectorError.check(menuBar() != null);
        return menuBar().makeMenu(menuKind);
    }

    public void clearMenus() {
        menuBar.removeAll();
    }

    public void setSelected() {
        parent.setSelected(view);
    }

    public boolean isSelected() {
        return parent.isSelected() && parent.isSelected(view);
    }

    public void flash(Color borderFlashColor, int n) {
        Component pane = getContentPane();
        if (pane instanceof JScrollPane) {
            final JScrollPane scrollPane = (JScrollPane) pane;
            pane = scrollPane.getViewport();
        }
        for (int i = 0; i < n; i++) {
            final Graphics g = pane.getGraphics();
            g.setPaintMode();
            g.setColor(borderFlashColor);
            for (int r = 0; r < 5; r++) {
                g.drawRect(r, r, pane.getWidth() - (r * 2), pane.getHeight() - (r * 2));
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            g.dispose();
            invalidate();
            repaint();
        }
    }

    public void setStateColor(Color color) {
        if (menuBar() != null) {
            menuBar().setBackground(color);
        }
    }

    public void dispose() {
        parent.remove(this);
        view.viewClosing();
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
