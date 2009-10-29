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

import com.sun.max.ins.*;
import com.sun.max.ins.gui.Inspector.*;
import com.sun.max.program.*;

public interface InspectorFrame extends RootPaneContainer, Prober {

    /**
     * Gets the inspector for the view.
     *
     * @return the inspector that owns this frame
     */
    Inspector inspector();

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
     * @throws ProgramError if the frame has no menu bar.
     */
    InspectorMenu makeMenu(MenuKind menuKind) throws ProgramError;

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
     * Makes this frame completely visible, in front of any others, either in
     * a desktop pane or in a tabbed pane.
     */
    void moveToFront();

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
