/*
 * Copyright (c) 2017-2018, APT Group, School of Computer Science,
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

import static com.sun.max.vm.classfile.constant.ConstantPool.Tag.*;
import static com.sun.max.vm.jdk.JDK_java_lang_invoke_MethodHandleNatives.*;

import java.lang.invoke.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.constant.ConstantPool.*;
import com.sun.max.vm.type.*;

/**
 * $4.4.8.
 */
public interface MethodHandleConstant extends ResolvableConstant<MethodHandleConstant, MethodHandle> {

    final class Unresolved extends AbstractPoolConstant<MethodHandleConstant> implements PoolConstantKey<MethodHandleConstant>, MethodHandleConstant {
        private ReferenceKind referenceKind;
        private int           referenceIndex;

        public Unresolved(int referenceKind, int referenceIndex) {
            this.referenceKind = ReferenceKind.fromClassfile(referenceKind);
            this.referenceIndex = referenceIndex;
        }

        @Override
        public boolean isResolved() {
            return false;
        }

        @Override
        public boolean isResolvableWithoutClassLoading(ConstantPool pool) {
            return true;
        }

        @Override
        public Tag tag() {
            return Tag.METHOD_HANDLE;
        }

        @Override
        public PoolConstantKey<MethodHandleConstant> key(ConstantPool pool) {
            return this;
        }

        @Override
        public String valueString(ConstantPool pool) {
            return "referenceKind=\"" + referenceKind.name() + "\",referenceIndex=\"" + referenceIndex + "\"";
        }

        /**
         * Implementation ported from resolve_constant_at_impl in ConstantPoolOop.cpp, link_method_handle_constant and
         * find_method_handle_type in systemDictionary.cpp.
         * <p>
         * $5.4.3.5
         *
         * @param pool
         * @param index
         * @return
         */
        public MethodHandle resolve(ConstantPool pool, int index) {
            MemberRefConstant calleeRef  = (MemberRefConstant) pool.constants()[referenceIndex];
            Class             caller     = pool.holder().javaClass();
            String            name       = calleeRef.name(pool).toString();
            Descriptor        descriptor = calleeRef.descriptor(pool);

            assert (referenceKind.isFieldRef() && calleeRef.tag() == FIELD_REF)
                    || ((referenceKind == ReferenceKind.REF_invokeVirtual || referenceKind == ReferenceKind.REF_newInvokeSpecial) && calleeRef.tag() == METHOD_REF)
                    || ((referenceKind == ReferenceKind.REF_invokeStatic || referenceKind == ReferenceKind.REF_invokeSpecial) && (calleeRef.tag() == METHOD_REF || calleeRef.tag() == INTERFACE_METHOD_REF))
                    || (referenceKind.isInterfaceRef() && calleeRef.tag() == INTERFACE_METHOD_REF)
                    : "Corrupted constant pool, methodHandle constant references can only be fields, methods, or interface methods! calleeref = " + calleeRef.tag() + " referenceKind = " + referenceKind;

            // 1. Resolve the callee
            Class callee = ((MemberActor) calleeRef.resolve(pool, referenceIndex)).holder().javaClass();
            // 2. Resolve the type
            Object type;
            if (descriptor.string.charAt(0) == '(') {
                SignatureDescriptor signature = SignatureDescriptor.create(descriptor.string);
                type = signature.getMethodHandleType(pool.classLoader());
            } else {
                type = JavaTypeDescriptor.resolveToJavaClass(descriptor, pool.classLoader());
            }
            // 3. Create a MethodType
            MethodHandle methodHandle = linkMethodHandleConstant(caller, referenceKind.value(), callee, name, type);
            pool.updateAt(index, new Resolved(methodHandle));
            return methodHandle;
        }
    }

    final class Resolved extends AbstractPoolConstant<MethodHandleConstant> implements PoolConstantKey<MethodHandleConstant>, MethodHandleConstant {

        @INSPECTED
        private final MethodHandle methodHandle;

        Resolved(MethodHandle methodHandle) {
            this.methodHandle = methodHandle;
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
        public PoolConstantKey<MethodHandleConstant> key(ConstantPool pool) {
            return this;
        }

        @Override
        public Tag tag() {
            return Tag.METHOD_HANDLE;
        }

        @Override
        public String valueString(ConstantPool pool) {
            return "methodHandle=\"" + methodHandle + "\"";
        }

        public MethodHandle resolve(ConstantPool pool, int index) {
            return methodHandle;
        }
    }
}
