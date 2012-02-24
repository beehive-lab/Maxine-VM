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
package com.sun.max.vm.classfile.constant;

import static com.sun.max.vm.classfile.constant.PoolConstantFactory.*;

import java.io.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.ConstantPool.Tag;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * Interface denoting a class entry in a constant pool.
 */
public interface ClassConstant extends PoolConstant<ClassConstant>, ValueConstant<ClassConstant>, PoolConstantKey<ClassConstant>, ResolvableConstant<ClassConstant, ClassActor> {

    TypeDescriptor typeDescriptor();

    ClassActor resolve(ConstantPool pool, int index);

    ClassConstant key(ConstantPool pool);

    public static final class Resolved extends AbstractClassConstant {

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
        public ClassConstant key(ConstantPool pool) {
            return this;
        }

        public Value value(ConstantPool pool, int index) {
            return ReferenceValue.from(classActor);
        }
    }

    public static class Unresolved extends AbstractClassConstant {

        private final TypeDescriptor typeDescriptor;

        Unresolved(TypeDescriptor typeDescriptor) {
            this.typeDescriptor = typeDescriptor;
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
            return typeDescriptor.isResolvableWithoutClassLoading(pool.classLoader());
        }

        @Override
        public ClassConstant key(ConstantPool pool) {
            return this;
        }

        public Value value(ConstantPool pool, int index) {
            return ReferenceValue.from(resolve(pool, index));
        }
    }
}

abstract class AbstractClassConstant extends AbstractPoolConstant<ClassConstant> implements ClassConstant {

    @Override
    public Tag tag() {
        return Tag.CLASS;
    }

    @Override
    public int hashCode() {
        return typeDescriptor().hashCode();
    }

    public String valueString(ConstantPool pool) {
        return typeDescriptor().toJavaString();
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof ClassConstant && ((ClassConstant) object).typeDescriptor().equals(typeDescriptor());
    }


    @Override
    public void writeOn(DataOutputStream stream, ConstantPoolEditor editor, int index) throws IOException {
        super.writeOn(stream, editor, index);
        final String classDescriptor;
        final String string = editor.pool().classAt(index).typeDescriptor().toString();
        if (string.charAt(0) == 'L') {
            // Strip 'L' and ';' surrounding class name
            classDescriptor = string.substring(1, string.length() - 1);
        } else {
            classDescriptor = string;
        }
        final int classIndex = editor.indexOf(makeUtf8Constant(classDescriptor));
        stream.writeShort(classIndex);
    }

}
