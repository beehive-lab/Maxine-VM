/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.type.JavaTypeDescriptor.AtomicTypeDescriptor;
import com.sun.max.vm.type.JavaTypeDescriptor.WordTypeDescriptor;

/**
 * An {@linkplain RiType#isResolved() unresolved} type. An unresolved type is
 * derived from a {@linkplain UnresolvedType.InPool constant pool entry} or
 * from a {@linkplain TypeDescriptor type descriptor} and an associated
 * accessing class.
 */
public abstract class UnresolvedType implements RiType {

    /**
     * Gets a {@link RiType}. This method will return a {@linkplain RiType#isResolved() resolved}
     * type if possible but without triggering any class loading or resolution.
     *
     * @param typeDescriptor a type descriptor
     * @param accessingClass the context of the type lookup. If accessing class is resolved, its class loader
     *        is used to retrieve an existing resolved type. This value can be {@code null} if the caller does
     *        not care for a resolved type.
     * @return a {@link RiType} object for {@code typeDescriptor}
     */
    public static RiType toRiType(TypeDescriptor typeDescriptor, RiType accessingClass) {
        if (typeDescriptor instanceof AtomicTypeDescriptor) {
            final AtomicTypeDescriptor atom = (AtomicTypeDescriptor) typeDescriptor;
            return ClassActor.fromJava(atom.toKind().javaClass);
        } else if (typeDescriptor instanceof WordTypeDescriptor) {
            final WordTypeDescriptor word = (WordTypeDescriptor) typeDescriptor;
            if (word.javaClass instanceof Class) {
                return ClassActor.fromJava((Class) word.javaClass);
            }
        } else if (accessingClass != null) {
            if (accessingClass instanceof ClassActor) {
                ClassLoader loader = ((ClassActor) accessingClass).classLoader;
                if (typeDescriptor.isResolvableWithoutClassLoading(loader)) {
                    return typeDescriptor.resolve(loader);
                }
            }
        }
        return new ByAccessingClass(typeDescriptor, (ClassActor) accessingClass);
    }

    /**
     * An unresolved type corresponding to a constant pool entry.
     */
    public static final class InPool extends UnresolvedType {
        public final ConstantPool pool;
        public final int cpi;
        public InPool(TypeDescriptor typeDescriptor, ConstantPool pool, int cpi) {
            super(typeDescriptor);
            this.pool = pool;
            this.cpi = cpi;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof InPool) {
                InPool other = (InPool) o;
                return typeDescriptor.equals(other.typeDescriptor) &&
                       pool == other.pool && cpi == other.cpi;
            }
            return false;
        }
    }

    /**
     * An unresolved type corresponding to a type descriptor and an accessing class.
     */
    public static final class ByAccessingClass extends UnresolvedType {
        public final ClassActor accessingClass;
        public ByAccessingClass(TypeDescriptor typeDescriptor, ClassActor accessingClass) {
            super(typeDescriptor);
            this.accessingClass = accessingClass;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ByAccessingClass) {
                ByAccessingClass other = (ByAccessingClass) o;
                return typeDescriptor.equals(other.typeDescriptor) &&
                       accessingClass.equals(other.accessingClass);
            }
            return false;
        }
    }

    /**
     * The symbol for the unresolved type.
     */
    public final TypeDescriptor typeDescriptor;

    /**
     * Creates a new unresolved type for a specified type descriptor.
     *
     * @param typeDescriptor the type's descriptor
     * @param pool the constant pool containing the unresolved type reference
     * @param cpi the index in {@code constantPool} of the unresolved type reference
     */
    public UnresolvedType(TypeDescriptor typeDescriptor) {
        this.typeDescriptor = typeDescriptor;
    }

    public String name() {
        return typeDescriptor.string;
    }

    public boolean isResolved() {
        return false;
    }

    public RiType componentType() {
        return UnresolvedType.toRiType(typeDescriptor.componentTypeDescriptor(), null);
    }

    /**
     * Gets the compiler interface type representing an array of this compiler interface type.
     * @return the compiler interface type representing an array with elements of this compiler interface type
     */
    public RiType arrayOf() {
        return UnresolvedType.toRiType(JavaTypeDescriptor.getArrayDescriptorForDescriptor(typeDescriptor, 1), null);
    }

    @Override
    public CiKind kind(boolean architecture) {
        return WordUtil.ciKind(typeDescriptor.toKind(), architecture);
    }

    private static boolean isFinalOrPrimitive(ClassActor classActor) {
        return classActor.isFinal() || classActor.isPrimitiveClassActor();
    }

    @Override
    public final int hashCode() {
        return typeDescriptor.hashCode();
    }

    @Override
    public abstract boolean equals(Object o);

    @Override
    public String toString() {
        return name() + " [unresolved]";
    }

    public CiKind getRepresentationKind(RiType.Representation r) {
        // all portions of a type are represented by objects in Maxine
        return CiKind.Object;
    }
}
