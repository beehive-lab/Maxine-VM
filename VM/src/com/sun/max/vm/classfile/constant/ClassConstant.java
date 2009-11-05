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
package com.sun.max.vm.classfile.constant;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.ConstantPool.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Interface denoting a class entry in a constant pool.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public interface ClassConstant extends PoolConstant<ClassConstant>, ValueConstant<ClassConstant>, PoolConstantKey<ClassConstant>, ResolvableConstant<ClassConstant, ClassActor> {

    TypeDescriptor typeDescriptor();

    ClassActor resolve(ConstantPool pool, int index);

    ClassConstant key(ConstantPool pool);

    public static final class Resolved extends AbstractPoolConstant<ClassConstant> implements ClassConstant {

        @INSPECTED
        public final ClassActor classActor;

        public Resolved(ClassActor classActor) {
            this.classActor = classActor;
        }

        public TypeDescriptor typeDescriptor() {
            return classActor.typeDescriptor;
        }

        public ClassActor resolve(ConstantPool pool, int index) {
            return classActor;
        }

        public boolean isResolvableWithoutClassLoading(ConstantPool pool) {
            return true;
        }

        public boolean isResolved() {
            return true;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof ClassConstant && ((ClassConstant) object).typeDescriptor().equals(typeDescriptor());
        }

        @Override
        public int hashCode() {
            return typeDescriptor().hashCode();
        }

        @Override
        public ClassConstant key(ConstantPool pool) {
            return this;
        }

        @Override
        public Tag tag() {
            return Tag.CLASS;
        }

        public Value value(ConstantPool pool, int index) {
            return ReferenceValue.from(classActor);
        }

        public String valueString(ConstantPool pool) {
            return typeDescriptor().toJavaString();
        }
    }

    public static class Unresolved extends AbstractPoolConstant<ClassConstant> implements ClassConstant {

        private final TypeDescriptor typeDescriptor;

        Unresolved(TypeDescriptor typeDescriptor) {
            this.typeDescriptor = typeDescriptor;
        }

        @Override
        public Tag tag() {
            return Tag.CLASS;
        }

        public boolean isResolved() {
            return false;
        }

        public TypeDescriptor typeDescriptor() {
            return typeDescriptor;
        }

        public ClassActor resolve(ConstantPool pool, int index) {
            try {
                try {
                    final ClassActor classActor = this.typeDescriptor.resolve(pool.classLoader());
                    final ClassActor holder = pool.holder();
                    if (holder != null) {
                        // This handles the 'incompleteness' of a constant pool during class file loading.
                        // The class file loader must ensure that it explicitly performs this check later.
                        classActor.checkAccessBy(holder);
                    }

                    pool.updateAt(index, new Resolved(classActor));
                    return classActor;
                } catch (RuntimeException e) {
                    throw (NoClassDefFoundError) new NoClassDefFoundError(this.typeDescriptor.toJavaString()).initCause(e);
                }
            } catch (VirtualMachineError e) {
                // Comment from Hotspot:
                // Just throw the exception and don't prevent these classes from
                // being loaded for virtual machine errors like StackOverflow
                // and OutOfMemoryError, etc.
                // Needs clarification to section 5.4.3 of the JVM spec (see 6308271)
                throw e;
            }
        }

        public boolean isResolvableWithoutClassLoading(ConstantPool pool) {
            return typeDescriptor.isResolvableWithoutClassLoading(pool.holder(), pool.classLoader());
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof ClassConstant && ((ClassConstant) object).typeDescriptor().equals(typeDescriptor);
        }

        @Override
        public int hashCode() {
            return typeDescriptor.hashCode();
        }

        @Override
        public ClassConstant key(ConstantPool pool) {
            return this;
        }

        public Value value(ConstantPool pool, int index) {
            return ReferenceValue.from(resolve(pool, index));
        }

        public String valueString(ConstantPool pool) {
            return typeDescriptor().toJavaString();
        }

    }
}
