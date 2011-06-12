/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.ins.memory;

import com.sun.max.ins.*;
import com.sun.max.ins.view.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;


/**
 * Methods for creating byte views on memory in different variations.  Some are direct
 * actions; others are for possibly interactive {@link InspectorAction}s
 * that can be put on menus.
 */
public interface MemoryBytesViewFactory extends InspectionViewFactory<MemoryBytesView>{

    /**
     * Creates a bytes view on the memory starting at an address, using
     * some default length..
     *
     * @param address location in VM memory where view starts
     * @return a memory bytes view
     */
    MemoryBytesView makeView(Address address);

    /**
     * Creates a bytes view on the memory holding an object.
     *
     * @param teleObject surrogate for the object in VM whose memory should be viewed
     * @return a memory bytes view
     */
    MemoryBytesView makeView(TeleObject teleObject);

    /**
     * Gets an interactive action that makes a byte view on memory, starting
     * at a dialog-specified location.
     *
     * @return an action that creates a memory bytes view
     */
    InspectorAction makeViewAction();

    /**
     * Gets an action that creates a bytes view of VM memory.
     *
     * @param address starting location in VM memory of view
     * @param actionTitle an optional title for the action that would appear in a menu
     * @return an action that creates a memory bytes view
     */
    InspectorAction makeViewAction(Address address, String actionTitle);

}
