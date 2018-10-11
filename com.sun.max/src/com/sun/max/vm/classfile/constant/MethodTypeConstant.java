/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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

import java.lang.invoke.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.classfile.constant.ConstantPool.*;
import com.sun.max.vm.type.*;

/**
 * #4.4.9.
 */
public interface MethodTypeConstant extends ResolvableConstant<MethodTypeConstant, MethodType> {

    final class Unresolved extends AbstractPoolConstant<MethodTypeConstant> implements PoolConstantKey<MethodTypeConstant>, MethodTypeConstant {
        private Utf8Constant descriptor;

        Unresolved(Utf8Constant descriptor) {
            this.descriptor = descriptor;
        }

        @Override
        public boolean isResolved() {
            return false;
        }

        @Override
        public boolean isResolvableWithoutClassLoading(ConstantPool pool) {
            ProgramWarning.message("isResolvableWithoutClassLoading for MethodTypeConstant Not implemented yet");
            return true;
        }

        @Override
        public Tag tag() {
            return Tag.METHOD_TYPE;
        }

        @Override
        public PoolConstantKey<MethodTypeConstant> key(ConstantPool pool) {
            return this;
        }

        @Override
        public String valueString(ConstantPool pool) {
            return "desciptor=\"" + descriptor + "\"";
        }

        private SignatureDescriptor signature() {
            try {
                return SignatureDescriptor.create(descriptor.toString());
            } catch (ClassCastException e) {
                // This just means another thread beats us in the race to convert descriptor to a real Descriptor object.
                // If descriptor still has the wrong Descriptor type, then the following cast will catch it.
                // Using an exception handler obviates the need for synchronization.
            }
            return null;
        }

        public MethodType resolve(ConstantPool pool, int index) {
            MethodType methodType = signature().getMethodHandleType(pool.classLoader());
            pool.updateAt(index, new MethodTypeConstant.Resolved(methodType));
            return methodType;
        }
    }

    final class Resolved extends AbstractPoolConstant<MethodTypeConstant> implements PoolConstantKey<MethodTypeConstant>, MethodTypeConstant {

        @INSPECTED
        private final MethodType methodType;

        Resolved(MethodType methodType) {
            this.methodType = methodType;
        }

        @Override
        public boolean isResolved() {
            return true;
        }

        @Override
        public boolean isResolvableWithoutClassLoading(ConstantPool pool) {
            return true;
        }

        @Override
        public PoolConstantKey<MethodTypeConstant> key(ConstantPool pool) {
            return this;
        }

        @Override
        public Tag tag() {
            return Tag.METHOD_TYPE;
        }

        @Override
        public String valueString(ConstantPool pool) {
            return "methodType=\"" + methodType + "\"";
        }

        public MethodType resolve(ConstantPool pool, int index) {
            return methodType;
        }
    }

//    private String descriptorString() {
//        return descriptor.toString();
//    }
//
//    public TypeDescriptor type() {
//        if (descriptor instanceof Utf8Constant) {
//            try {
//                descriptor = JavaTypeDescriptor.parseTypeDescriptor(descriptor.toString());
//            } catch (ClassCastException e) {
//                // This just means another thread beats us in the race to convert descriptor to a real Descriptor object.
//                // If descriptor still has the wrong Descriptor type, then the cast in the return statement will catch it.
//                // Using an exception handler obviates the need for synchronization.
//            }
//        }
//        try {
//            return (TypeDescriptor) descriptor;
//        } catch (ClassCastException e) {
//            throw classFormatError(descriptor + " is not a valid field type descriptor");
//        }
//    }
//
//    public Descriptor descriptor() {
//        if (descriptor instanceof Descriptor) {
//            return (Descriptor) descriptor;
//        }
//        if (descriptorString().charAt(0) == '(') {
//            return signature();
//        }
//        return type();
//    }

}
