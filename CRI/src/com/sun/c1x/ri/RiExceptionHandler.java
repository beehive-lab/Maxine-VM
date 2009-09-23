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
package com.sun.c1x.ri;

/**
 * This interface represents an exception handler within the bytecode.
 *
 * @author Ben L. Titzer
 */
public interface RiExceptionHandler {
    /**
     * Gets the start bytecode index of the protected range of this handler.
     * @return the start bytecode index
     */
    int startBCI();

    /**
     * Gets the end bytecode index of the protected range of this handler.
     * @return the end bytecode index
     */
    int endBCI();

    /**
     * Gets the bytecode index of the handler block of this handler.
     * @return the handler block bytecode index
     */
    int handlerBCI();

    /**
     * Gets the index into the constant pool representing the type of exceptions
     * caught by this handler.
     * @return the constant pool index of the catch type
     */
    int catchClassIndex();

    /**
     * Checks whether this handler catches all exceptions.
     * @return {@code true} if this handler catches all exceptions
     */
    boolean isCatchAll();

    /**
     * The type of exceptions that are caught by this exception handler.
     *
     * @return the exception type
     */
    RiType catchKlass();
}
