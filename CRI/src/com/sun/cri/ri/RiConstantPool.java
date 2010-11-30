/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.cri.ri;

/**
 * Represents the runtime representation of the constant pool that is
 * used by the compiler when parsing bytecode. The {@code lookupXXX} methods look up a constant
 * pool entry without performing resolution, and are used during compilation.
 *
 * @author Ben L. Titzer
 */
public interface RiConstantPool {

    /**
     * Looks up a reference to a field. If {@code opcode} is non-negative, then resolution checks
     * specific to the JVM instruction it denotes are performed if the field is already resolved.
     * Should any of these checks fail, an {@linkplain RiField#isResolved() unresolved}
     * field reference is returned.
     * 
     * @param cpi the constant pool index
     * @param opcode the opcode of the instruction for which the lookup is being performed or {@code -1}
     * @return a reference to the field at {@code cpi} in this pool
     * @throws ClassFormatError if the entry at {@code cpi} is not a field
     */
    RiField lookupField(int cpi, int opcode);
    
    /**
     * Looks up a reference to a method. If {@code opcode} is non-negative, then resolution checks
     * specific to the JVM instruction it denotes are performed if the method is already resolved.
     * Should any of these checks fail, an {@linkplain RiMethod#isResolved() unresolved}
     * method reference is returned.
     * 
     * @param cpi the constant pool index
     * @param opcode the opcode of the instruction for which the lookup is being performed or {@code -1}
     * @return a reference to the method at {@code cpi} in this pool
     * @throws ClassFormatError if the entry at {@code cpi} is not a method
     */
    RiMethod lookupMethod(int cpi, int opcode);
    
    /**
     * Looks up a reference to a type. If {@code opcode} is non-negative, then resolution checks
     * specific to the JVM instruction it denotes are performed if the type is already resolved.
     * Should any of these checks fail, an {@linkplain RiType#isResolved() unresolved}
     * type reference is returned.
     * 
     * @param cpi the constant pool index
     * @param opcode the opcode of the instruction for which the lookup is being performed or {@code -1}
     * @return a reference to the compiler interface type
     */
    RiType lookupType(int cpi, int opcode);

    /**
     * Looks up a method signature.
     * 
     * @param cpi the constant pool index
     * @return the method signature at index {@code cpi} in this constant pool
     */
    RiSignature lookupSignature(int cpi);

    /**
     * Looks up a constant at the specified index.
     * @param cpi the constant pool index
     * @return the {@code CiConstant} instance representing the constant
     */
    Object lookupConstant(int cpi);
}
