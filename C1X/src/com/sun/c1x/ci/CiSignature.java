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
package com.sun.c1x.ci;

import com.sun.c1x.value.BasicType;

/**
 * The <code>CiSignature</code> class represents a method signature.
 *
 * @author Ben L. Titzer
 */
public interface CiSignature {
    /**
     * Gets the number of arguments to this signature, not including a receiver object.
     * @return the number of arguments
     */
    int arguments();

    /**
     * Gets the argument type at the specified position.
     * @param index the index into the parameters, with {@code 0} indicating the first parameter
     * @return the compiler interface type of the argument at the specified position
     */
    CiType argumentTypeAt(int index);

    /**
     * Gets the argument basic type at the specified position.
     * @param index the index into the parameters, with {@code 0} indicating the first parameter
     * @return the basic type of the argument at the specified position
     */
    BasicType argumentBasicTypeAt(int index);

    /**
     * Gets the return type of this signature.
     * @return the compiler interface type representing the return type
     */
    CiType returnType();

    /**
     * Gets the return basic type of this signature.
     * @return the basic type representing the return type
     */
    BasicType returnBasicType();

    /**
     * Converts this signature to a string.
     * @return the signature as a string
     */
    String asString();

    /**
     * Gets the size, in Java slots, of the arguments to this signature.
     * @param withReceiver {@code true} if to add a slot for a receiver object; {@code false} not to include the receiver
     * @return the size of the arguments in slots
     */
    int argumentSize(boolean withReceiver);
}
