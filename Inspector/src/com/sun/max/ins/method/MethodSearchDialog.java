/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.ins.method;

import java.lang.reflect.*;

import com.sun.max.collect.*;
import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.actor.member.MethodKey.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.prototype.*;
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
        return StaticLoophole.cast(type, listItem);
    }

    @Override
    protected void rebuildList(String filterText) {
        final String filter = filterText.toLowerCase();
        for (MethodKey methodKey : _methodKeys) {
            final String methodName = methodKey.name().string();
            if (filter.endsWith(" ")) {
                if (methodName.equalsIgnoreCase(Strings.chopSuffix(filter, 1))) {
                    _listModel.addElement(methodKey);
                }
            } else if (methodName.toLowerCase().contains(filter)) {
                _listModel.addElement(methodKey);
            }
        }
    }

    private final AppendableSequence<MethodKey> _methodKeys;

    private MethodKey createMethodKey(SignatureDescriptor signature, TypeDescriptor holder, Utf8Constant name) {
        return new DefaultMethodKey(holder, name, signature) {
            @Override
            public String toString() {
                return _name + _signature.toJavaString(false, true);
            }
        };
    }

    private MethodSearchDialog(Inspection inspection, TypeDescriptor holderTypeDescriptor, String title, String actionName) {
        super(inspection, title == null ? "Select Method" : title, "Method Name", actionName, false);
        _methodKeys = new ArrayListSequence<MethodKey>();
        final String className = holderTypeDescriptor.toJavaString();
        try {
            final Class javaClass = Classes.load(PrototypeClassLoader.PROTOTYPE_CLASS_LOADER, className);
            for (Constructor constructor : javaClass.getDeclaredConstructors()) {
                _methodKeys.append(createMethodKey(SignatureDescriptor.create(Void.TYPE, constructor.getParameterTypes()), holderTypeDescriptor, SymbolTable.INIT));
            }
            for (Method method : javaClass.getDeclaredMethods()) {
                _methodKeys.append(createMethodKey(SignatureDescriptor.create(method.getReturnType(), method.getParameterTypes()), holderTypeDescriptor, SymbolTable.makeSymbol(method.getName())));
            }
            final ClassActor classActor = ClassActor.fromJava(javaClass);
            if (classActor.hasClassInitializer()) {
                _methodKeys.append(createMethodKey(SignatureDescriptor.create(Void.TYPE), holderTypeDescriptor, SymbolTable.CLINIT));
            }
        } catch (Error error) {
            ProgramWarning.message("Error loading class " + className + ": " + error);
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
