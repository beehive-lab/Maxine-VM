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
package com.sun.max.vm.compiler.instrument;

import com.sun.max.vm.profile.*;

/**
 * Implements a counter that is associated with some location in the code
 * (typically bytecode). This can be used to instrument various branches of the
 * bytecode to determine dynamic frequency counts. Note that this implementation
 * is not synchronized, which means that some updates can be lost. Like some
 * other instrumentation facilities, this should be considered an approximation.
 *
 * @author Ben L. Titzer
 */
public class LocationCounter extends Counter {
    /**
     * The starting location in the original code (e.g. bytecode) that this counter corresponds to.
     */
    protected final int start;

    /**
     * The ending location in the original code that this counter corresponds to.
     */
    protected final int end;

    /**
     * Constructs a new location counter corresponding to the specified source range.
     * @param start the starting location in the original code
     * @param end the ending location in the original code
     */
    public LocationCounter(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public LocationCounter(int pos) {
        this(pos, pos);
    }

    /**
     * Get the current count.
     * @return the current count
     */
    public int getCount() {
        return _count;
    }
}
