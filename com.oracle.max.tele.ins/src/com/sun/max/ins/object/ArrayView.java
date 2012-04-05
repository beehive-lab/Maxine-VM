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
package com.sun.max.ins.object;

import java.util.*;

import com.sun.max.ins.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.layout.*;

/**
 * An object view specialized for displaying a low-level object
 * in the VM, constructed using {@link ArrayLayout}.
 */
public final class ArrayView extends ObjectView<ArrayView> {

    private ObjectScrollPane elementsPane;

    ArrayView(Inspection inspection, TeleObject teleObject) {
        super(inspection, teleObject);
        createFrame(true);
    }

    @Override
    protected void createViewContent() {
        final TeleArrayObject teleArrayObject = (TeleArrayObject) teleObject();
        elementsPane = ObjectScrollPane.createArrayElementsPane(inspection(), teleArrayObject, instanceViewPreferences);
        super.createViewContent();
        getContentPane().add(elementsPane);

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

}
