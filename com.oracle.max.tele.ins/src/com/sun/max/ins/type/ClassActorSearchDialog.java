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
import com.sun.max.lang.*;
import com.sun.max.tele.object.*;
import com.sun.max.vm.type.*;

/**
 * A dialog to let the user select a class in the {@linkplain VmClassRegistry Inspector class registry}.
 */
public final class ClassActorSearchDialog extends ObjectSearchDialog<TeleClassActor> {

    private final static class ClassActorListItem extends FilteredListItem<TeleClassActor> {

        final TypeDescriptor typeDescriptor;
        final String name;

        public ClassActorListItem(Inspection inspection, TypeDescriptor typeDescriptor) {
            super(inspection);
            this.typeDescriptor = typeDescriptor;
            this.name = typeDescriptor.toJavaString();
        }

        @Override
        public TeleClassActor object() {
            return inspection().vm().classes().findTeleClassActor(JavaTypeDescriptor.getDescriptorForJavaString(name));
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int compareTo(FilteredListItem o) {
            final ClassActorListItem classActorListItem = (ClassActorListItem) o;
            return name.compareTo(classActorListItem.name);
        }
    }

    /**
     * Determines if a given filter matches a given type descriptor. The matching ignores case and is performed against
     * the {@linkplain TypeDescriptor#toJavaString() Java class name} derived from the type descriptor.
     *
     * @param filter a filter consisting only of {@linkplain String#toLowerCase() lower case} characters
     * @param typeDescriptor the type descriptor to match {@code filter} against
     * @return the {@linkplain TypeDescriptor#toJavaString() Java class name} derived from {@code typeDescriptor} if
     *         there is a match, null otherwise
     */
    public static String match(String filter, TypeDescriptor typeDescriptor) {
        final String className = typeDescriptor.toJavaString();
        if (filter.indexOf('.') == -1) {
            final int length = filter.length();
            String simpleClassName = typeDescriptor.toJavaString(false);
            int lastDollar = simpleClassName.lastIndexOf('$');
            if (lastDollar != -1) {
                simpleClassName = simpleClassName.substring(lastDollar + 1);
            }
            if (!filter.isEmpty() && filter.charAt(length - 1) == ' ') {
                if (simpleClassName.equalsIgnoreCase(Strings.chopSuffix(filter, 1))) {
                    return className;
                }
            }
            if (simpleClassName.toLowerCase().contains(filter)) {
                return className;
            }
        }
        if (className.toLowerCase().contains(filter)) {
            return className;
        }
        return null;
    }

    private final ClassActorListItem[] classActorListItems;

    /**
     * Rebuilds the list from scratch.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void rebuildList(String filterText) {
        if (!filterText.isEmpty()) {
            final String filter = filterText.toLowerCase();
            for (ClassActorListItem classActorListItem : classActorListItems) {
                if (match(filter, classActorListItem.typeDescriptor) != null) {
                    listModel.addElement(classActorListItem);
                }
            }
        }
    }

    private ClassActorSearchDialog(Inspection inspection) {
        this(inspection, "Select Class", "Class Name");
    }

    private ClassActorSearchDialog(Inspection inspection, String title, String actionName) {
        super(inspection, title == null ? "Select Class" : title, "Class Name", actionName, false);
        final Set<TypeDescriptor> typeDescriptors = vm().classes().typeDescriptors();
        classActorListItems = new ClassActorListItem[typeDescriptors.size()];
        int i = 0;
        for (TypeDescriptor typeDescriptor : typeDescriptors) {
            classActorListItems[i++] = new ClassActorListItem(inspection, typeDescriptor);
        }
        Arrays.sort(classActorListItems);
    }

    /**
     * Displays a dialog to let the user select a class from the {@linkplain VmClassRegistry Inspector class registry}.
     *
     * @return the reference to the selected class actor or {@link XXX_TeleReference#ZERO} if the user canceled the dialog
     */
    public static TeleClassActor show(Inspection inspection) {
        final ClassActorSearchDialog dialog = new ClassActorSearchDialog(inspection);
        dialog.setVisible(true);
        return dialog.selectedObject();
    }

    /**
     * Displays a dialog to let the user select a class from the {@linkplain VmClassRegistry Inspector class registry}.
     * @param inspection
     * @param title Title string for the dialog frame.
     * @param actionName Name of the action, appears on on the button to activate
     * @return
     */
    public static TeleClassActor show(Inspection inspection, String title, String actionName) {
        final ClassActorSearchDialog dialog = new ClassActorSearchDialog(inspection, title, actionName);
        dialog.setVisible(true);
        return dialog.selectedObject();
    }

}
