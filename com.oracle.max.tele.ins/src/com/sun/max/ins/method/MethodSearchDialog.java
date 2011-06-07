/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.*;
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
 *
 * @author Doug Simon
 * @author Bernd Mathiske
 */
public final class MethodSearchDialog extends FilteredListDialog<MethodKey> {

    @Override
    protected MethodKey noSelectedObject() {
        return null;
    }

    @Override
    protected MethodKey convertSelectedItem(Object listItem) {
        final Class<MethodKey> type = null;
        return Utils.cast(type, listItem);
    }

    @Override
    protected void rebuildList(String filterText) {
        final String filter = filterText.toLowerCase();
        for (MethodKey methodKey : methodKeys) {
            final String methodName = methodKey.name().string;
            if (filter.endsWith(" ")) {
                if (methodName.equalsIgnoreCase(Strings.chopSuffix(filter, 1))) {
                    listModel.addElement(methodKey);
                }
            } else if (methodName.toLowerCase().contains(filter)) {
                listModel.addElement(methodKey);
            }
        }
    }

    private final List<MethodKey> methodKeys;

    private MethodKey createMethodKey(SignatureDescriptor signature, TypeDescriptor holder, Utf8Constant name) {
        return new DefaultMethodKey(holder, name, signature) {
            @Override
            public String toString() {
                return name + signature.toJavaString(false, true);
            }
        };
    }

    private MethodSearchDialog(Inspection inspection, TypeDescriptor holderTypeDescriptor, String title, String actionName) {
        super(inspection, title == null ? "Select Method" : title, "Method Name", actionName, false);
        methodKeys = new ArrayList<MethodKey>();
        final String className = holderTypeDescriptor.toJavaString();
        try {
            final Class javaClass = Classes.load(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER, className);
            for (Constructor constructor : javaClass.getDeclaredConstructors()) {
                methodKeys.add(createMethodKey(SignatureDescriptor.create(Void.TYPE, constructor.getParameterTypes()), holderTypeDescriptor, SymbolTable.INIT));
            }
            for (Method method : javaClass.getDeclaredMethods()) {
                methodKeys.add(createMethodKey(SignatureDescriptor.create(method.getReturnType(), method.getParameterTypes()), holderTypeDescriptor, SymbolTable.makeSymbol(method.getName())));
            }
            final ClassActor classActor = ClassActor.fromJava(javaClass);
            if (classActor.hasClassInitializer()) {
                methodKeys.add(createMethodKey(SignatureDescriptor.create(Void.TYPE), holderTypeDescriptor, SymbolTable.CLINIT));
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
