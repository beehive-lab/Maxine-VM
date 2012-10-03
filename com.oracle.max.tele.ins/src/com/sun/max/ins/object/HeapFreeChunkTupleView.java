/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.gcx.*;

/**
 * An object view specialized for displaying a low-level heap quasi-object in the VM of type {@link HeapFreeChunk}.
 * Although the object is only four words long, including header, it is specially formatted by some GC implementations
 * to represent a span of free space.  The amount of free space is indicated in the length field of the object.  A standard
 * tuple view would only show the two fields, but this view is specialized to display all the rest of the free space as if
 * it were more fields.</p>
 * <p>
 * Unlike the ordinary tuple view, no provision is made for alternate textual display of the obejct's value.</p>
 * Unlike all ordinary object views, provision is made so that the length might change, if merged with following free space.
 *
 * @see TeleHeapFreeChunk
 * @see HeapFreeChunk
 */
public class HeapFreeChunkTupleView extends ObjectView<HeapFreeChunkTupleView> {

    private final TeleHeapFreeChunk teleHFC;
    Size totalFreeSize;
    private ObjectScrollPane fieldsPane;

    HeapFreeChunkTupleView(Inspection inspection, MaxObject object) {
        super(inspection, object);
        this.teleHFC = (TeleHeapFreeChunk) object;
        this.totalFreeSize = teleHFC.size();
        createFrame(true);
    }

    @Override
    protected void createViewContent() {
        super.createViewContent();

        final int numPadWords = totalFreeSize.minus(teleHFC.objectSize()).dividedBy(vm().platform().nBytesInWord()).toInt();

        fieldsPane = ObjectScrollPane.createTupleFieldsPaddedPane(inspection(), this, numPadWords);
        getContentPane().add(fieldsPane);

        // View-specific menus
        final InspectorMenu objectMenu = makeMenu(MenuKind.OBJECT_MENU);
        objectMenu.add(defaultMenuItems(MenuKind.OBJECT_MENU));
    }

    @Override
    protected void refreshState(boolean force) {
        super.refreshState(force);
        if (totalFreeSize == teleHFC.size()) {
            fieldsPane.refresh(force);
        } else {
            totalFreeSize = teleHFC.size();
            reconstructView();
        }
    }
}
