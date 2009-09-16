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
package com.sun.max.vm.verifier.types;

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * A registry of canonical instances for objects used by a verifier such as verification types and subroutines.
 *
 * @author Doug Simon
 */
public interface VerificationRegistry {

    /**
     * Gets the canonical object type for a TypeDescriptor.
     * 
     * @return null if {@code typeDescriptor} denotes a {@linkplain TypeDescriptor#isPrimitive() primitive type}
     */
    ObjectType getObjectType(TypeDescriptor typeDescriptor);

    /**
     * Gets the canonical type of an uninitialized object created by a {@link Bytecode#NEW} instruction at a given
     * bytecode position.
     */
    UninitializedNewType getUninitializedNewType(int position);

    /**
     * Gets the canonical type for a TypeDescriptor.
     */
    VerificationType getVerificationType(TypeDescriptor typeDescriptor);

    /**
     * Gets the canonical representation of a subroutine entered at a given position.
     */
    Subroutine getSubroutine(int entryPosition, int maxLocals);

    /**
     * Clears all recorded subroutines.
     * 
     * @return the number of recorded subroutines cleared
     */
    int clearSubroutines();

    ConstantPool constantPool();

    ClassActor resolve(TypeDescriptor type);
}
