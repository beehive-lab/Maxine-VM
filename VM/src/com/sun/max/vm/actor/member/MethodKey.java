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
package com.sun.max.vm.actor.member;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * Describes a method or constructor declared by a class in terms of the {@linkplain #holder() declaring class},
 * {@linkplain #name() name} and {@linkplain #signature() signature} of the method. Note that other properties of
 * methods are not captured by a method key including the access flags and the class loader associated with the method.
 *
 * @author Doug Simon
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
