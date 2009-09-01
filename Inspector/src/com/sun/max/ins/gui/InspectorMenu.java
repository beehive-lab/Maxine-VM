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

import javax.swing.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.program.*;

/**
 * A menu that can be manifest in the GUI by a {@linkplain JPopupMenu pop-up menu} or a {@linkplain JMenu standard menu}.
 *
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public final class InspectorMenu implements Prober {

    // TODO (mlvdv) not clear that we need this polymorphic menu any more; all uses seem to be one or the other.

    private final String name;
    private AppendableSequence<InspectorMenuItems> inspectorMenuItems;

    private final JPopupMenu popupMenu;
    private final JMenu standardMenu;

    /**
     * Creates a menu that can be used either as a standard pull-down or a pop-up menu.
     * <br>
     * Menu items may have state that gets update when refreshed.
     */
    public InspectorMenu(String name) {
        this.name = name;
        popupMenu =  new JPopupMenu(name);
        standardMenu = new JMenu(name);
    }

    /**
     * Creates an unnamed menu.
     * <br>
     * Menu items may have state that gets update when refreshed.
     * <br>
     * <strong>Menus created with this constructor have no name and must not be added to a {@link InspectorMenuBar}.
     */
    public InspectorMenu() {
        this(null);
    }

    public int length() {
        return standardMenu.getItemCount();
    }

    public JPopupMenu popupMenu() {
        return popupMenu;
    }

    public JMenu standardMenu() {
        return standardMenu;
    }

    public String name() {
        return name;
    }

    /**
     * Adds to the menu an untyped object that can be either an {@link InspectorAction}
     * or a {@link JMenu}.
     */
    public void add(Object object) {
        if (object instanceof InspectorAction) {
            final InspectorAction action = (InspectorAction) object;
            this.add(action);
        } else if (object instanceof JMenu) {
            final JMenu menu = (JMenu) object;
            this.add(menu);
        } else {
            ProgramError.unexpected("Inappropriate argument");
        }
    }

    public void add(InspectorAction action) {
        action.append(standardMenu);
        action.append(popupMenu);
    }

    public void add(InspectorMenuItems inspectorMenuItems) {
        addSeparator();
        inspectorMenuItems.addTo(this);
        if (this.inspectorMenuItems == null) {
            this.inspectorMenuItems = new LinkSequence<InspectorMenuItems>();
        }
        this.inspectorMenuItems.append(inspectorMenuItems);
    }

    public void add(InspectorMenu inspectorMenu) {
        standardMenu.add(inspectorMenu.standardMenu);
        popupMenu.add(inspectorMenu.standardMenu);
    }

    /**
     * For menu items that do not change any Inspector or VM state, only local view state.
     * @param menuItem
     */
    public void add(JMenuItem menuItem) {
        standardMenu.add(menuItem);
        popupMenu.add(menuItem);
    }

    public void addSeparator() {
        standardMenu.addSeparator();
        popupMenu.addSeparator();
    }

    public void refresh(boolean force) {
        if (this.inspectorMenuItems != null) {
            for (InspectorMenuItems inspectorMenuItems : this.inspectorMenuItems) {
                inspectorMenuItems.refresh(force);
            }
        }
    }

    public void redisplay() {
    }

}
