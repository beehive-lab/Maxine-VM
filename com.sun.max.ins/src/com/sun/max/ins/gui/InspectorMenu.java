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

import java.util.*;

import javax.swing.*;

import com.sun.max.ins.*;
import com.sun.max.ins.util.*;

/**
 * A menu that can be manifest in the GUI by a {@linkplain JMenu standard menu}.
 * <br>
 * The important characteristic of this menu is that it can be refreshed,
 * in case it depends on any state (e.g. for being enabled).
 */
public class InspectorMenu extends JMenu implements Prober {

    private final List<InspectorMenuItems> menuItems = new ArrayList<InspectorMenuItems>();
    private List<InspectorAction> actions = new ArrayList<InspectorAction>();
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
            InspectorError.unexpected("Inappropriate argument");
        }
    }

    public void add(InspectorAction action) {
        assert action != null;
        actions.add(action);
        super.add(action);
    }

    public void add(InspectorMenuItems inspectorMenuItems) {
        menuItems.add(inspectorMenuItems);
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
