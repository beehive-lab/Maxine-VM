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

package com.sun.max.ins.view;

import javax.swing.*;

import com.sun.max.ins.gui.*;


/**
 * Methods for creating views of a particular kind.
 */
public interface InspectionViewFactory<Inspector_Kind extends AbstractView> {

    /**
     * Gets a menu that includes actions for creating new views.
     * If there are existing activated views of the same kind, then
     * these are listed and selecting one of them causes the view
     * to be highlighted.
     * If there are any activated views of the same kind, the menu includes an
     * entry (at the end, after a separator) that deactivates all
     * the views.
     * <p>
     * Returns null if no such menu is appropriate or supported.  For example,
     * there is typically only one way to activate a singleton view and so
     * a menu of commands may not be useful.
     *
     * @return the menu that displays all activated views that can be highlighted
     */
    JMenu viewMenu();
}
