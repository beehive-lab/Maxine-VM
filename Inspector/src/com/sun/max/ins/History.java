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
package com.sun.max.ins;

import com.sun.max.annotate.*;


/**
 * 
 * Experimental; not in use yet
 * 
 * Interface for a record of changes to an object
 * over discrete generations, where generation 1
 * is the most recent, or "current" generation.
 * 
 * A strong presumption is that the value at each generation,
 * once recorded, does not change.
 * 
 * "Change" is defined by implementation, e.g. identity vs. equals vs.
 * a custom equality predicate.
 * 
 * Implementations may choose to perform "change" comparisons
 * eagerly (recording the outcome with each addition to the history), lazily,
 * or by some other scheme.
 * 
 * @author Michael Van De Vanter
 *
 */
@Hypothetical
public interface History<Type> {

    /**
     * @return Number of generations of history kept, minimum 1.
     */
    int nGenerations();

    /**
     * @return Value from a specified generation.
     *   1 <= generation < nGenerations();
     */
    Type value(int generation);

    /**
     * @return Current value:  generation=1.
     */
    Type current();

    /**
     * @return Whether the value from a specified generation differs from its predecessor.
     *   1 <= generation < nGenerations() -1;
     */
    boolean modified(int generation);

    /**
     * @return Whether the current generation differs from its predecessor.
     */
    boolean modified();
}
