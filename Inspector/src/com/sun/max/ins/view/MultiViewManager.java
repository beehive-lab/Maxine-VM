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
 * Manager for a kind of Inspector view that may have an arbitrary
 * number of instances active.
 *
 * @author Michael Van De Vanter
 */
public interface MultiViewManager<Inspector_Kind extends Inspector> extends ViewManager<Inspector_Kind> {

    /**
     * Gets a menu for showing existing views of this kind, bringing
     * forward and highlighting the one selected, as well as for creating
     * new views.  The menu is populated
     * dynamically, creating the list of activated views when the menu
     * is displayed.  If there are no activated views, then the menu is
     * empty.  If there are any activated views, the menu includes an
     * entry (at the end, after a separator) that deactivates all the views.
     *
     * @return the menu that displays all activated views that can be highlighted
     */
    JMenu multiViewMenu();

    /**
     * Disposes all existing views of this kind.
     */
    void deactivateAllViews();

}
