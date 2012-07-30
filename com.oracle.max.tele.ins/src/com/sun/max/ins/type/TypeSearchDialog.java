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
package com.sun.max.ins.type;

import java.util.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.tele.*;
import com.sun.max.vm.type.*;

/**
 * A dialog for selecting a type available via the {@linkplain MaxVM#loadableTypeDescriptors()} including those that may not have yet
 * been loaded into the {@linkplain VmClassRegistry Inspector class registry}.
 */
public final class TypeSearchDialog extends FilteredListDialog<TypeDescriptor> {

    private static final class TypeDescriptorItem extends FilteredListItem<TypeDescriptor> {

        private final TypeDescriptor typeDescriptor;

        TypeDescriptorItem(Inspection inspection, TypeDescriptor typeDescriptor) {
            super(inspection);
            this.typeDescriptor = typeDescriptor;
        }

        @Override
        public TypeDescriptor object() {
            return typeDescriptor;
        }

        @Override
        public String toString() {
            return typeDescriptor.toJavaString();
        }
    }

    /**
     * Rebuilds the list from scratch.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void rebuildList(String filterText) {
        if (!filterText.isEmpty()) {
            final String filter = filterText.toLowerCase();
            for (TypeDescriptorItem typeDescriptorItem : typeItems) {
                if (ClassActorSearchDialog.match(filter, typeDescriptorItem.typeDescriptor) != null) {
                    listModel.addElement(typeDescriptorItem);
                }
            }
        }
    }

    private TypeSearchDialog(Inspection inspection) {
        this(inspection, "Select Class", "Class Name");
    }

    private TypeSearchDialog(Inspection inspection, String title, String actionName) {
        super(inspection, title == null ? "Select Class" : title, "Class Name", actionName, false);
        final ArrayList<TypeDescriptorItem> items = new ArrayList<TypeDescriptorItem>();
        for (TypeDescriptor typeDescriptor : vm().classes().loadableTypeDescriptors()) {
            items.add(new TypeDescriptorItem(inspection, typeDescriptor));
        }
        typeItems = items;
    }

    private final Iterable<TypeDescriptorItem> typeItems;

    /**
     * Displays a dialog for selecting a type available via the {@linkplain MaxVM#loadableTypeDescriptors()} including those that may
     * not have yet been loaded into the {@linkplain VmClassRegistry Inspector class registry}.
     *
     * @return the type or null if the user canceled the dialog
     */
    public static TypeDescriptor show(Inspection inspection) {
        final TypeSearchDialog dialog = new TypeSearchDialog(inspection);
        dialog.setVisible(true);
        return dialog.selectedObject();
    }

    /**
     * Displays a dialog for selecting a type available via the {@linkplain MaxVM#loadableTypeDescriptors()} including those that may
     * not have yet been loaded into the {@linkplain VmClassRegistry Inspector class registry}.
     *
     * @param inspection
     * @param title Title string for the dialog frame.
     * @param actionName Name of the action, appears on on the button to activate
     * @return the type or null if the user canceled the dialog
     */
    public static TypeDescriptor show(Inspection inspection, String title, String actionName) {
        final TypeSearchDialog dialog = new TypeSearchDialog(inspection, title, actionName);
        dialog.setVisible(true);
        return dialog.selectedObject();
    }
}
