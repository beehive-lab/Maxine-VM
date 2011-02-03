/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.actor.holder;

import java.util.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Internal representation of array classes.
 *
 * @author Bernd Mathiske
 */
public class ArrayClassActor<Value_Type extends Value<Value_Type>> extends ReferenceClassActor {

    public static Utf8Constant arrayTypeName(ClassActor componentClassActor) {
        return SymbolTable.makeSymbol(componentClassActor + "[]");
    }

    private static int createFlags(ClassActor componentClassActor) {
        return (componentClassActor.flags() & (ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE)) | ACC_FINAL;
    }

    private static InterfaceActor[] interfaceActors;

    private static InterfaceActor[] getInterfaceActors() {
        if (interfaceActors == null) {
            interfaceActors = new InterfaceActor[]{ClassRegistry.CLONEABLE, ClassRegistry.SERIALIZABLE};
        }
        return interfaceActors;
    }

    public ArrayClassActor(ClassActor componentClassActor) {
        super(Kind.REFERENCE,
              componentClassActor.kind.arrayLayout(Layout.layoutScheme()),
              componentClassActor.classLoader,
              arrayTypeName(componentClassActor),
              componentClassActor.majorVersion,
              componentClassActor.minorVersion,
              createFlags(componentClassActor),
              JavaTypeDescriptor.getArrayDescriptorForDescriptor(componentClassActor.typeDescriptor, 1),
              ClassRegistry.OBJECT,
              componentClassActor,
              getInterfaceActors(),
              FieldActor.NONE,
              MethodActor.NONE,
              NO_GENERIC_SIGNATURE,
              NO_RUNTIME_VISIBLE_ANNOTATION_BYTES,
              NO_SOURCE_FILE_NAME,
              NO_INNER_CLASSES,
              NO_OUTER_CLASS,
              NO_ENCLOSING_METHOD_INFO);
    }

    /**
     * Gets the type representing an 1-dimensional array of a given {@linkplain ClassActor#componentClassActor() component type}.
     */
    public static ArrayClassActor forComponentClassActor(ClassActor componentClassActor) {
        if (componentClassActor.arrayClassActor == null) {
            final TypeDescriptor arrayTypeDescriptor = JavaTypeDescriptor.getArrayDescriptorForDescriptor(componentClassActor.typeDescriptor, 1);
            synchronized (componentClassActor.classLoader) {
                ArrayClassActor arrayClassActor = (ArrayClassActor) ClassRegistry.get(componentClassActor.classLoader, arrayTypeDescriptor, false);
                if (arrayClassActor == null) {
                    arrayClassActor = ClassActorFactory.createArrayClassActor(componentClassActor);
                }
                componentClassActor.arrayClassActor = arrayClassActor;
            }
        }
        return componentClassActor.arrayClassActor;
    }

    public static ArrayClassActor forComponentClassActor(ClassActor elementClassActor, int dimensions) {
        assert !elementClassActor.isArrayClass() : elementClassActor + " dim=" + dimensions;
        assert dimensions > 0;
        ArrayClassActor arrayClassActor = forComponentClassActor(elementClassActor);
        int remainingDimensions = dimensions - 1;
        while (remainingDimensions > 0) {
            arrayClassActor = forComponentClassActor(arrayClassActor);
            --remainingDimensions;
        }
        return arrayClassActor;
    }

    @Override
    public final void makeInitialized() {
        componentClassActor().makeInitialized();
    }

    @Override
    public final Utf8Constant genericSignature() {
        return SymbolTable.makeSymbol("[" + componentClassActor().genericSignature());
    }

    @Override
    protected void gatherSuperClassActorIds(HashSet<Integer> set) {
        super.gatherSuperClassActorIds(set);
        final int numberOfDimensions = numberOfDimensions();
        final ClassActor elementClassActor = elementClassActor();
        set.add(id);
        ClassActor superClassActor = elementClassActor.superClassActor;
        while (superClassActor != null) {
            set.add(superClassActor.makeID(numberOfDimensions));
            superClassActor = superClassActor.superClassActor;
        }
        for (InterfaceActor interfaceActor : elementClassActor.getAllInterfaceActors()) {
            set.add(interfaceActor.makeID(numberOfDimensions));
        }

        if (!elementClassActor.isPrimitiveClassActor()) {
            set.add(ClassRegistry.OBJECT.makeID(numberOfDimensions));
        }

        for (int dimensions = 1; dimensions < numberOfDimensions; ++dimensions) {
            set.add(ClassRegistry.OBJECT.makeID(dimensions));
            set.add(ClassRegistry.CLONEABLE.makeID(dimensions));
            set.add(ClassRegistry.SERIALIZABLE.makeID(dimensions));
        }
    }

    public ArrayClassActor forDimension(int dimension) {
        int n = numberOfDimensions();
        if (n < dimension) {
            return null;
        }
        ArrayClassActor arrayClassActor = this;
        while (n > dimension) {
            arrayClassActor = (ArrayClassActor)  arrayClassActor.componentClassActor();
            n--;
        }
        return arrayClassActor;
    }

    @Override
    protected Size layoutFields(SpecificLayout specificLayout) {
        final ArrayLayout arrayLayout = (ArrayLayout) specificLayout;
        return Size.fromInt(arrayLayout.headerSize());
    }

}
