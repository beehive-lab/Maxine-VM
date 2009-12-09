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

    public static ArrayClassActor forComponentClassActor(ClassActor componentClassActor) {
        final TypeDescriptor arrayTypeDescriptor = JavaTypeDescriptor.getArrayDescriptorForDescriptor(componentClassActor.typeDescriptor, 1);
        synchronized (componentClassActor.classLoader) {
            ArrayClassActor arrayClassActor = (ArrayClassActor) ClassRegistry.get(componentClassActor.classLoader, arrayTypeDescriptor, false);
            if (arrayClassActor == null) {
                arrayClassActor = ClassActorFactory.createArrayClassActor(componentClassActor);
            }
            return arrayClassActor;
        }
    }

    public static ArrayClassActor forComponentClassActor(ClassActor elementClassActor, int dimensions) {
        assert !elementClassActor.isArrayClassActor() : elementClassActor + " dim=" + dimensions;
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
    protected BitSet getSuperClassActorSerials() {
        final BitSet result = super.getSuperClassActorSerials();
        final int numberOfDimensions = numberOfDimensions();
        final ClassActor elementClassActor = elementClassActor();
        result.set(id);
        ClassActor superClassActor = elementClassActor.superClassActor;
        while (superClassActor != null) {
            result.set(superClassActor.makeID(numberOfDimensions));
            superClassActor = superClassActor.superClassActor;
        }
        for (InterfaceActor interfaceActor : elementClassActor.getAllInterfaceActors()) {
            result.set(interfaceActor.makeID(numberOfDimensions));
        }

        if (!elementClassActor.isPrimitiveClassActor()) {
            result.set(ClassRegistry.OBJECT.makeID(numberOfDimensions));
        }

        for (int dimensions = 1; dimensions < numberOfDimensions; ++dimensions) {
            result.set(ClassRegistry.OBJECT.makeID(dimensions));
            result.set(ClassRegistry.CLONEABLE.makeID(dimensions));
            result.set(ClassRegistry.SERIALIZABLE.makeID(dimensions));
        }

        return result;
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
