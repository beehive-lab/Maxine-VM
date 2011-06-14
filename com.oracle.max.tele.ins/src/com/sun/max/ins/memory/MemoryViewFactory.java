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
import com.sun.max.ins.memory.MemoryView.ViewMode;
import com.sun.max.ins.view.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;


/**
 * Methods for creating views on memory in different variations.  Some are direct
 * actions; others are for possibly interactive {@link InspectorAction}s
 * that can be put on menus.
 */
public interface MemoryViewFactory extends InspectionViewFactory<MemoryView>{

    /**
     * Creates a view on a designated, named region of memory, with the view
     * mode set to {@link ViewMode#WORD}.
     *
     * @param memoryRegion description of VM memory to be viewed
     * @param regionName optional name for region, defaults to region's own name
     * @return a memory view
     *      */
    MemoryView makeView(MaxMemoryRegion memoryRegion, String regionName);

    /**
     * Creates a view on the memory holding an object, with the view
     * mode set to {@link ViewMode#OBJECT}.
     *
     * @param teleObject surrogate for the object in VM whose memory should be viewed
     * @return a memory view
     */
    MemoryView makeView(TeleObject teleObject);

    /**
     * Creates a view on a page of memory, with the view mode set to {@link ViewMode#PAGE}.
     *
     * @param address starting location of VM memory to be displayed
     * @return a memory view
     */
    MemoryView makeView(Address address);

    /**
     * Gets an interactive action that makes a view on a page of memory, starting
     * at a dialog-specified location, with the view mode set to {@link ViewMode#PAGE}.
     *
     * @return an action that creates a memory view
     */
    InspectorAction makeViewAction();

    /**
     * Gets an action that makes a view on a region of memory, with the view
     * mode set to {@link ViewMode#WORD}.
     *
     * @param memoryRegion description of VM memory to be viewed
     * @param regionName optional name for region, defaults to region's own name
     * @param actionTitle an optional title for the action that would appear in a menu
     * @return an action that creates a memory view
     */
    InspectorAction makeViewAction(MaxMemoryRegion memoryRegion, String regionName, String actionTitle);

    /**
     * Gets an action that makes a view on the memory holding an object, with the view
     * mode set to {@link ViewMode#OBJECT}.
     *
     * @param teleObject surrogate for the object in VM whose memory should be viewed
     * @param actionTitle an optional title for the action that would appear in a menu
     * @return an action that creates a memory view
     */
    InspectorAction makeViewAction(TeleObject teleObject, String actionTitle);

    /**
     * Gets an action that makes a view on a page of memory, with the view mode
     * set to {@link ViewMode#PAGE}.
     *
     * @param address starting location of VM memory to be displayed
     * @param actionTitle an optional title for the action that would appear in a menu
     * @return an action that creates a memory view
     */
    InspectorAction makeViewAction(Address address, String actionTitle);

}
