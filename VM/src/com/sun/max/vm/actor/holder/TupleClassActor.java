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

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.type.*;

/**
 * Internal representations of classes, which have instances that are tuples.
 *
 * A "tuple" is an object with fields, as opposed to an array, which has only elements.
 *
 * @author Bernd Mathiske
 */
public class TupleClassActor extends ReferenceClassActor {

    @INSPECTED
    private final ConstantPool constantPool;

    protected TupleClassActor(Kind kind,
                              ConstantPool constantPool,
                              ClassLoader classLoader,
                              Utf8Constant name,
                              char majorVersion,
                              char minorVersion,
                              int flags,
                              ClassActor superClassActor,
                              InterfaceActor[] interfaceActors,
                              FieldActor[] fieldActors,
                              MethodActor[] methodActors,
                              Utf8Constant genericSignature,
                              byte[] runtimeVisibleAnnotationsBytes,
                              String sourceFileName,
                              TypeDescriptor[] innerClasses,
                              TypeDescriptor outerClass, EnclosingMethodInfo enclosingMethodInfo) {
        super(kind,
              Layout.tupleLayout(),
              classLoader,
              name,
              majorVersion,
              minorVersion,
              flags,
              JavaTypeDescriptor.getDescriptorForWellFormedTupleName(name.toString()),
              getSuperClassActor(superClassActor, name),
              NO_COMPONENT_CLASS_ACTOR,
              interfaceActors,
              fieldActors,
              methodActors,
              genericSignature,
              runtimeVisibleAnnotationsBytes,
              sourceFileName,
              innerClasses,
              outerClass,
              enclosingMethodInfo);
        this.constantPool = constantPool;
        constantPool.setHolder(this);
    }

    @Override
    public final ConstantPool constantPool() {
        return constantPool;
    }

    /**
     * Severs the {@link Word} types from the standard Java class hierarchy by making the superClassActor of {@link Word} be null.
     */
    @INLINE
    private static ClassActor getSuperClassActor(ClassActor superClassActor, Utf8Constant name) {
        if (MaxineVM.isHosted()) {
            if (JavaTypeDescriptor.WORD == JavaTypeDescriptor.getDescriptorForJavaString(name.toString())) {
                return null;
            }
        }
        return superClassActor;
    }

    @Override
    protected Size layoutFields(SpecificLayout specificLayout) {
        final TupleLayout tupleLayout = (TupleLayout) specificLayout;
        return tupleLayout.layoutFields(superClassActor, localInstanceFieldActors());
    }

    public static TupleClassActor fromJava(final Class javaClass) {
        return (TupleClassActor) ClassActor.fromJava(javaClass);
    }

    public Object newInstance() {
        try {
            return ClassMethodActor.findVirtual(this, SymbolTable.INIT.toString()).invokeConstructor().asObject();
        } catch (InstantiationException intantiationException) {
            throw ProgramError.unexpected(intantiationException);
        } catch (IllegalAccessException illegalAccessException) {
            throw ProgramError.unexpected(illegalAccessException);
        } catch (InvocationTargetException invocationTargetException) {
            throw ProgramError.unexpected(invocationTargetException);
        }
    }
}
