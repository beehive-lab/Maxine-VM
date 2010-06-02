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
package com.sun.max.lang;

import java.util.concurrent.*;

/**
 * Creates a function wrapper for a method that returns a value and may throw a checked exception.
 * This interface extends {@link Callable} so that {@code Function}s can be used with an {@link ExecutorService}.
 * 
 * 
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public interface Function<T> extends Callable<T> {

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result (which will be {@code null} if {@code Result_Type} is {@link Void})
     * @throws Exception if unable to compute a result
     */
    T call() throws Exception;

}
