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

package com.sun.max.ins.object;

import com.sun.max.ins.*;


/**
 * Methods for creating views on objects in different variations.  Some are direct
 * actions; others are for possibly interactive {@link InspectorAction}s
 * that can be put on menus.
 *
 * @author Michael Van De Vanter
 */
public interface ObjectViewFactory {

    /**
     * Gets an interactive action that makes a view on an object in
     * VM memory, starting at a dialog-specified location.
     *
     * @return an action that creates an object view
     */
    InspectorAction makeViewAction();

    /**
     * Gets an interactive action that makes a view on an object in
     * VM memory, identified by a dialog-specified numeric ID.
     *
     * @return an action that creates an object view
     */
    InspectorAction makeViewByIDAction();

}
