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

import com.sun.max.ins.*;
import com.sun.max.program.*;


/**
 * A specialization of popup menus for use in the Inspector.
 *
 * @author Michael Van De Vanter
 */
public class InspectorPopupMenu extends JPopupMenu {

    public InspectorPopupMenu(String name) {
        super(name);
    }

    public InspectorPopupMenu() {
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

    public void add(InspectorPopupMenuItems menuItems) {
        for (Object item : menuItems) {
            this.add(item);
        }
    }
}
