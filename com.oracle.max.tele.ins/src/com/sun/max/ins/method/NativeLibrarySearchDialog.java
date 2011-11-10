/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.method;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.method.NativeCodeLibraries.*;
import com.sun.max.lang.*;
import com.sun.max.tele.*;


public class NativeLibrarySearchDialog extends FilteredListDialog<LibInfo> {
    @Override
    protected LibInfo noSelectedObject() {
        return null;
    }

    @Override
    protected LibInfo convertSelectedItem(Object listItem) {
        return (LibInfo) listItem;
    }

    @Override
    protected void rebuildList(String filterText) {
        final String filter = filterText.toLowerCase();
        for (LibInfo info : NativeCodeLibraries.getLibs((TeleVM) vm())) {
            if (filter.endsWith(" ")) {
                if (info.path.equalsIgnoreCase(Strings.chopSuffix(filter, 1))) {
                    listModel.addElement(info);
                }
            } else if (info.path.toLowerCase().contains(filter)) {
                listModel.addElement(info);
            }
        }
    }

    private NativeLibrarySearchDialog(Inspection inspection, String title, String actionName) {
        super(inspection, title == null ? "Select Native Library" : title, "Filter text", actionName, false);
        rebuildList();
    }

    /**
     * Displays a dialog to let the use select one or more native functions in the VM.
     *
     * @param title for dialog window
     * @param actionName name to appear on button
     * @param multi allow multiple selections if true
     * @return references to the selected instances of {@link NativeCodeLibraries.LibInfo}, null if user canceled.
     */
    public static NativeCodeLibraries.LibInfo show(Inspection inspection, String title, String actionName) {
        final NativeLibrarySearchDialog dialog = new NativeLibrarySearchDialog(inspection, title, actionName);
        dialog.setVisible(true);
        return dialog.selectedObject();
    }

}
