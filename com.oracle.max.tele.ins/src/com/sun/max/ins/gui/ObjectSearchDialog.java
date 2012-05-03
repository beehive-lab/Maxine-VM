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
package com.sun.max.ins.gui;

import com.sun.max.ins.*;
import com.sun.max.tele.*;

/**
 * An abstract class for dialogs that enables the user to search for an object in the VM.
 * The dialog is composed of a list of names of objects and a text field that can be used to filter the list.
 */
public abstract class ObjectSearchDialog extends FilteredListDialog<MaxObject> {

    @Override
    protected MaxObject noSelectedObject() {
        return null;
    }

    protected ObjectSearchDialog(Inspection inspection, String title, String filterFieldLabel, String actionName, boolean multiSelection) {
        super(inspection, title, filterFieldLabel, actionName, multiSelection);
    }

    protected ObjectSearchDialog(Inspection inspection, String title, String filterFieldLabel, boolean multiSelection) {
        this(inspection, title, filterFieldLabel, null, multiSelection);
    }

}
