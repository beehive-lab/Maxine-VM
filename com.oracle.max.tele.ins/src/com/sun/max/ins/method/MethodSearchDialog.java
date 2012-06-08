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
package com.sun.max.ins.method;

import java.lang.reflect.*;
import java.util.*;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.ins.util.*;
import com.sun.max.lang.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.actor.member.MethodKey.DefaultMethodKey;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

/**
 * A dialog to let the user select a reference to a method actor defined by a given class actor.
 */
public final class MethodSearchDialog extends FilteredListDialog<MethodKey> {

    private static final class MethodKeyItem extends FilteredListItem<MethodKey> {

        static MethodKeyItem create(Inspection inspection, SignatureDescriptor signature, TypeDescriptor holder, Utf8Constant name) {
            return new MethodKeyItem(inspection, new DefaultMethodKey(holder, name, signature));
        }

        private final MethodKey methodKey;
        private final String name;

        public MethodKeyItem(Inspection inspection, MethodKey methodKey) {
            super(inspection);
            this.methodKey = methodKey;
            this.name = methodKey.name() + methodKey.signature().toJavaString(false, true);
        }

        @Override
        public MethodKey object() {
            return methodKey;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void rebuildList(String filterText) {
        final String filter = filterText.toLowerCase();
        for (MethodKeyItem methodKeyItem : methodKeyItems) {
            final String methodName = methodKeyItem.methodKey.name().string;
            if (filter.endsWith(" ")) {
                if (methodName.equalsIgnoreCase(Strings.chopSuffix(filter, 1))) {
                    listModel.addElement(methodKeyItem);
                }
            } else if (methodName.toLowerCase().contains(filter)) {
                listModel.addElement(methodKeyItem);
            }
        }
    }

    private final List<MethodKeyItem> methodKeyItems;

    private MethodSearchDialog(Inspection inspection, TypeDescriptor holderTypeDescriptor, String title, String actionName) {
        super(inspection, title == null ? "Select Method" : title, "Method Name", actionName, false);
        methodKeyItems = new ArrayList<MethodKeyItem>();
        final String className = holderTypeDescriptor.toJavaString();
        try {
            final Class javaClass = Classes.load(HostedVMClassLoader.HOSTED_VM_CLASS_LOADER, className);
            for (Constructor constructor : javaClass.getDeclaredConstructors()) {
                methodKeyItems.add(MethodKeyItem.create(inspection, SignatureDescriptor.create(Void.TYPE, constructor.getParameterTypes()), holderTypeDescriptor, SymbolTable.INIT));
            }
            for (Method method : javaClass.getDeclaredMethods()) {
                methodKeyItems.add(MethodKeyItem.create(inspection, SignatureDescriptor.create(method.getReturnType(), method.getParameterTypes()), holderTypeDescriptor, SymbolTable.makeSymbol(method.getName())));
            }
            final ClassActor classActor = ClassActor.fromJava(javaClass);
            if (classActor.hasClassInitializer()) {
                methodKeyItems.add(MethodKeyItem.create(inspection, SignatureDescriptor.create(Void.TYPE), holderTypeDescriptor, SymbolTable.CLINIT));
            }
        } catch (Error error) {
            InspectorWarning.message(inspection, "Error loading class " + className, error);
        }
        rebuildList();
    }

    /**
     * Displays a dialog to let the user select a method from a class specified by a type description.
     */
    public static MethodKey show(Inspection inspection, TypeDescriptor holderTypeDescriptor, String title, String actionName) {
        final MethodSearchDialog dialog = new MethodSearchDialog(inspection, holderTypeDescriptor, title, actionName);
        dialog.setVisible(true);
        return dialog.selectedObject();
    }

    /**
     * Displays a dialog to let the user select a method from a class specified by a type description.
     */
    public static MethodKey show(Inspection inspection, TypeDescriptor holder) {
        return show(inspection, holder, null, null);
    }
}
