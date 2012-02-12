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
package com.sun.max.vm.actor.member;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * Describes a method or constructor declared by a class in terms of the {@linkplain #holder() declaring class},
 * {@linkplain #name() name} and {@linkplain #signature() signature} of the method. Note that other properties of
 * methods are not captured by a method key including the access flags and the class loader associated with the method.
 */
public interface MethodKey  {

    TypeDescriptor holder();

    Utf8Constant name();

    SignatureDescriptor signature();

    Comparator<MethodKey> SORT_BY_NAME_AND_SIGNATURE = new Comparator<MethodKey>() {
        public int compare(MethodKey methodKey1, MethodKey methodKey2) {
            final int nameComparison = methodKey1.name().compareTo(methodKey2.name());
            if (nameComparison != 0) {
                return nameComparison;
            }
            return methodKey1.signature().compareTo(methodKey2.signature());
        }
    };

    HashEquivalence<MethodKey> EQUIVALENCE = new HashEquivalence<MethodKey>() {
        public boolean equivalent(MethodKey object1, MethodKey object2) {
            return object1.holder().equals(object2.holder()) &&
                object1.name().equals(object2.name()) &&
                object1.signature().equals(object2.signature());
        }
        public int hashCode(MethodKey object) {
            return object.name().hashCode() ^ object.signature().hashCode();
        }
    };

    public abstract static class AbstractMethodKey implements MethodKey {
        @Override
        public boolean equals(Object object) {
            if (object instanceof MethodKey) {
                final MethodKey otherMethodKey = (MethodKey) object;
                return EQUIVALENCE.equivalent(this, otherMethodKey);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return holder().hashCode() ^ name().hashCode();
        }

        @Override
        public String toString() {
            return toString(false);
        }

        public String toString(boolean qualified) {
            return holder().toJavaString() + "." + name() + signature().toJavaString(qualified, true);
        }
    }

    public static class DefaultMethodKey extends AbstractMethodKey {
        protected final TypeDescriptor holder;
        protected final Utf8Constant name;
        protected final SignatureDescriptor signature;

        public DefaultMethodKey(MethodActor methodActor) {
            this(methodActor.holder().typeDescriptor, methodActor.name, methodActor.descriptor());
        }

        public DefaultMethodKey(TypeDescriptor holder, Utf8Constant name, SignatureDescriptor signature) {
            this.holder = holder;
            this.name = name;
            this.signature = signature;
        }

        public TypeDescriptor holder() {
            return holder;
        }

        public Utf8Constant name() {
            return name;
        }

        public SignatureDescriptor signature() {
            return signature;
        }
    }

    public static class MethodActorKey extends AbstractMethodKey {
        protected final MethodActor methodActor;

        public MethodActorKey(MethodActor methodActor) {
            this.methodActor = methodActor;
        }

        public TypeDescriptor holder() {
            return methodActor.holder().typeDescriptor;
        }

        public Utf8Constant name() {
            return methodActor.name;
        }

        public SignatureDescriptor signature() {
            return methodActor.descriptor();
        }
    }
}
