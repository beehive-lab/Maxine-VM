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
package com.sun.max.asm;

/**
 * An {@code InlineDataRecorder} is interested in knowing which positions in some assembled code contain inline data (as opposed to valid instructions).
 *
 * @author Doug Simon
 */
public interface InlineDataRecorder {

    /**
     * Records the starting position and size of some inline data written to an instruction stream. The value of
     * {@code startPosition} is guaranteed to increase or stay the same for successive calls to this method during the
     * assembling of a single instruction stream (e.g. assembling a single method or function).
     */
    void record(int startPosition, int size);
}
