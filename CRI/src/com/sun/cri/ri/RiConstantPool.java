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

import com.sun.cri.ci.*;

/**
 * Represents the runtime representation of the constant pool that is
 * used by the compiler when parsing bytecode. The {@code lookupXXX} methods look up a constant
 * pool entry without performing resolution, and are used during compilation.
 *
 * @author Ben L. Titzer
 */
public interface RiConstantPool {

    /**
     * Resolves a reference to a compiler interface type at runtime.
     * @param cpi the constant pool index
     * @return a reference to the compiler interface type
     */
    RiType resolveType(int cpi);

    /**
     * Looks up a reference to a field.
     * 
     * @param cpi the constant pool index
     * @return a reference to the field at {@code cpi} in this pool
     * @throws ClassFormatError if the entry at {@code cpi} is not a field
     */
    RiField lookupField(int cpi);
    
    /**
     * Looks up a reference to a method.
     * 
     * @param cpi the constant pool index
     * @return a reference to the method at {@code cpi} in this pool
     * @throws ClassFormatError if the entry at {@code cpi} is not a method
     */
    RiMethod lookupMethod(int cpi);
    
    /**
     * Looks up a reference to a compiler interface type at compile time (does not
     * perform resolution). If a resolution of this constant would fail
     * at run time, the compiler expects this method to return an unresolved constant.
     * @param cpi the constant pool index
     * @return a reference to the compiler interface type
     */
    RiType lookupType(int cpi);

    /**
     * Looks up a method signature.
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

    /**
     * Constant object that can be used to identify this constant pool when it is referenced from the code.
     *
     * @return a constant object representing this constant pool
     */
    CiConstant encoding();
}
