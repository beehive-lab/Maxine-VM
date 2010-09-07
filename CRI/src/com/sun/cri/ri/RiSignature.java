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
 * Represents a method signature provided by the runtime.
 *
 * @see <a href="http://java.sun.com/docs/books/jvms/second_edition/html/ClassFile.doc.html#7035">Method Descriptors</a>
 * @author Ben L. Titzer
 */
public interface RiSignature {
    /**
     * Gets the number of arguments to this signature, not including a receiver object.
     * @param receiver true if there is a receiver, false otherwise
     * @return the number of arguments
     */
    int argumentCount(boolean receiver);

    /**
     * Gets the argument type at the specified position. This method will return a
     * {@linkplain RiType#isResolved() resolved} type if possible but without
     * triggering any class loading or resolution.
     * 
     * @param index the index into the parameters, with {@code 0} indicating the first parameter
     * @param accessingClass the context of the type lookup. If accessing class is resolved, its class loader
     *        is used to retrieve an existing resolved type. This value can be {@code null} if the caller does
     *        not care for a resolved type.
     * @return the {@code index}'th argument type
     */
    RiType argumentTypeAt(int index, RiType accessingClass);

    /**
     * Gets the argument kind at the specified position.
     * @param index the index into the parameters, with {@code 0} indicating the first parameter
     * @return the kind of the argument at the specified position
     */
    CiKind argumentKindAt(int index);

    /**
     * Gets the return type of this signature. This method will return a
     * {@linkplain RiType#isResolved() resolved} type if possible but without
     * triggering any class loading or resolution.
     * 
     * @param accessingClass the context of the type lookup. If accessing class is resolved, its class loader
     *        is used to retrieve an existing resolved type. This value can be {@code null} if the caller does
     *        not care for a resolved type.
     * @return the compiler interface type representing the return type
     */
    RiType returnType(RiType accessingClass);
    
    /**
     * Gets the return kind of this signature.
     * @return the return kind
     */
    CiKind returnKind();

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
    int argumentSlots(boolean withReceiver);
}
