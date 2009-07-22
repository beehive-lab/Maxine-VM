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
package com.sun.max.util;

import java.util.*;


/**
 * A value, along with a history of its previous values.
 * Time is specified as number of generations back
 * from the current generation, which is always generation 0.
 *
 * @author Michael Van De Vanter
 */
public interface ValueHistory<Value_Type> {

    /**
     * Adds a new value, which becomes the current generation.
     * The generation of all previously recorded values increases by 1.
     */
    void add(Value_Type newValue);

    /**
     * @return the "current" value (at generation 0).
     * Error if no values have been recorded.
     */
    Value_Type get();

    /**
     * @return the age, in generations, of the current value, since recording began.
     * 0 if different from immediate predecessor; -1 if no different value ever recorded
     * Comparison uses {@linkplain Object#equals(Object) equals}.
     */
    int getAge();

    /**
     * @return The value at a specified generation.
     * Error if generation does not exist.
     */
    Value_Type get(int generation);

    /**
     * @return iteration of the values recorded in the history, starting with the current
     * generation and proceeding backward in time.
     */
    Iterator<Value_Type> values();

    /**
     * @return the number of generations recorded; initially 0.
     */
    int getSize();

    /**
     * @return the maximum number of generations that can be recorded.
     */
    int getLimit();

}
