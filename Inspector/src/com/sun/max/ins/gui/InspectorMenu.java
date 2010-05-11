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
 * A menu that can be manifest in the GUI by a {@linkplain JMenu standard menu}.
 * <br>
 * The important characteristic of this menu is that it can be refreshed,
 * in case it depends on any state (e.g. for being enabled).
 *
 * @author Michael Van De Vanter
 */
public class InspectorMenu extends JMenu implements Prober {

    private final AppendableSequence<InspectorMenuItems> menuItems = new LinkSequence<InspectorMenuItems>();
    private AppendableSequence<InspectorAction> actions = new LinkSequence<InspectorAction>();
    private final String name;
    /**
     * Creates a standard menu that can be used on the Inspector menu bar.
     * <br>
     * Menu items may have state that gets update when refreshed.
     */
    public InspectorMenu(String name) {
        super(name);
        this.name = name;
    }

    /**
     * @return the name given the menu at creation; used as key to find the menu.
     */
    public String getMenuName() {
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
        actions.append(action);
        super.add(action);
    }

    public void add(InspectorMenuItems inspectorMenuItems) {
        menuItems.append(inspectorMenuItems);
        inspectorMenuItems.addTo(this);
    }

    public void refresh(boolean force) {
        for (InspectorMenuItems inspectorMenuItems : this.menuItems) {
            inspectorMenuItems.refresh(force);
        }
        for (InspectorAction action : actions) {
            action.refresh(force);
        }
    }

    public void redisplay() {
    }

}
