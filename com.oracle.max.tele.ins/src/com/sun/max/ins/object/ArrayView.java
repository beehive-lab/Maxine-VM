/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.object;

import java.util.*;

import com.sun.max.ins.*;
import com.sun.max.tele.*;
import com.sun.max.vm.layout.*;

/**
 * An object view specialized for displaying a low-level object
 * in the VM, constructed using {@link ArrayLayout}.
 */
public final class ArrayView extends ObjectView<ArrayView> {

    private ObjectScrollPane elementsPane;

    ArrayView(Inspection inspection, MaxObject object) {
        super(inspection, object);
        createFrame(true);
    }

    @Override
    protected void createViewContent() {
        elementsPane = ObjectScrollPane.createArrayElementsPane(inspection(), this);
        super.createViewContent();
        getContentPane().add(elementsPane);
        // Force the title to be recomputed, just in case the new pane is eliding
        setTitle();

        // Opportunity for view-specific Object menu
        makeMenu(MenuKind.OBJECT_MENU).add(defaultMenuItems(MenuKind.OBJECT_MENU));
    }

    @Override
    protected void refreshState(boolean force) {
        elementsPane.refresh(force);
        super.refreshState(force);
    }

    @Override
    protected List<InspectorAction> extraViewMenuActions() {
        return elementsPane.extraViewMenuActions();
    }

    @Override
    protected boolean isElided() {
        return elementsPane != null && elementsPane.isElided();
    }

}
