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
package com.sun.max.vm.runtime;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * A token that "guards" the resolution of a symbol to an {@link Actor}.
 *
 * This pattern of use is intended:
 *
 * if (guard.value == null) { guard.value = ... } return guard.value;
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class ResolutionGuard {

    /**
     * A resolution guard for a symbol read from a constant pool. The constant pool is
     * used to provide the correct class loader and protection domain for resolving
     * the symbol.
     */
    public static class InPool extends ResolutionGuard {
        public final ConstantPool pool;
        public final int cpi;

        public InPool(ConstantPool constantPool, int cpi) {
            this.pool = constantPool;
            this.cpi = cpi;
            assert cpi >= 0 : "must be a valid constant pool index!";
        }
        @Override
        public String toString() {
            if (value != null) {
                return getClass().getSimpleName() + "[" + value + "]";
            }

            return getClass().getSimpleName() + "[" + pool.at(cpi).valueString(pool) + "]";
        }
    }

    /**
     * A class symbol resolution guard. The symbol is resolved using the loader and
     * protection domain of in the context of an accessing class.
     */
    public static class InAccessingClass extends ResolutionGuard {
        public final TypeDescriptor type;
        public final ClassActor accessingClass;

        public InAccessingClass(TypeDescriptor type, ClassActor accessingClass) {
            this.type = type;
            this.accessingClass = accessingClass;
        }

        public InAccessingClass(UnresolvedType.ByAccessingClass unresolvedType) {
            this.type = unresolvedType.typeDescriptor;
            this.accessingClass = unresolvedType.accessingClass;
        }

        public ClassActor resolve() {
            return type.resolve(accessingClass.classLoader);
        }

        @Override
        public String toString() {
            if (value != null) {
                return getClass().getSimpleName() + "[" + value + "]";
            }

            return getClass().getSimpleName() + "[" + type + "]";
        }
    }

    @CONSTANT_WHEN_NOT_ZERO
    public Actor value;
}
