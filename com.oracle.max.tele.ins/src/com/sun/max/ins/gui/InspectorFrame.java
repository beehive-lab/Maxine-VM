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

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.AbstractView.MenuKind;
import com.sun.max.ins.util.*;

/**
 * An abstraction over the kinds of window system containers that can
 * hold an {@link AbstractView}, either a simple internal frame suitable
 * for a {@link JDesktopPane} or the contents of a single tab in a
 * "tabbed pane".
 *
 * @author Michael Van De Vanter
 */
public interface InspectorFrame extends RootPaneContainer, Prober {

    /**
     * Gets the view for the content held in this frame.
     *
     * @return the view that owns this frame
     */
    AbstractView view();

    /**
     * Returns the Swing component that implements this frame.
     *
     * @return a component that implements this frame
     */
    JComponent getJComponent();

    /**
     * Sets the title being displayed on the frame; does nothing if the
     * frame is not displaying any titles.
     *
     * @param title the text to display as the frame title.
     * @see JInternalFrame#setTitle(String)
     */
    void setTitle(String title);

    /**
     * Returns the title being displayed, if any, on the frame.
     *
     * @return the contents of the frame title, null if none being displayed.
     * @see JInternalFrame#getTitle()
     */
    String getTitle();

    /**
     * Finds, and creates if doesn't exist, a named menu on the frame's menu bar.
     * <br>
     * <strong>Note:</strong> the menus will appear left to right on the
     * frame's menu bar in the order in which they were created.
     *
     * @param menuKind the type (and name) of the menu being requested.
     * @return a menu, possibly new, in the menu bar.
     * @throws InspectorError if the frame has no menu bar.
     */
    InspectorMenu makeMenu(MenuKind menuKind) throws InspectorError;

    /**
     * Makes this frame the one currently selected in the window system.
     */
    void setSelected();

    /**
     * Returns whether this frame is the currently "selected" or active
     * frame, either in the window system or within a tabbed collection
     * of frames.
     *
     * @return if this frame is currently selected
     * @see JInternalFrame#isSelected()
     */
    boolean isSelected();

    /**
     * @return whether this frame is currently visible.
     */
    boolean isVisible();

    /**
     * Makes this frame completely visible, in front of any others, either in
     * a desktop pane or in a tabbed pane.
     */
    void moveToFront();

    /**
     * Causes this frame to display a menu bar background color
     * that reveals current state information concerning the Inspector.
     */
    void setStateColor(Color color);

    /**
     * Draws attention to this frame by changing the color of the surrounding frame
     * for a short time.
     *
     * @param borderFlashColor a color to show briefly around the border of the frame.
     */
    void flash(Color borderFlashColor);

    /**
     * Returns the size of this component.
     *
     * @return an object that indicates the size of the component.
     * @see Component#getSize();
     */
    Dimension getSize();

    /**
     * Sets the preferred size of the frame.
     *
     * @param preferredSize
     */
    void setPreferredSize(Dimension preferredSize);

    /**
     * Causes this frame to recompute the layout of its contents.
     * @see JInternalFrame#pack()
     */
    void pack();

    /**
     * Marks frame as needing to lay out contents again.
     * @see Container#invalidate()
     */
    void invalidate();

    /**
     * Causes this component to be painted as soon as possible.
     * @see Component#repaint()
     */
    void repaint();

    /**
     * Removes this frame and the view state associated with it.
     */
    void dispose();

}
