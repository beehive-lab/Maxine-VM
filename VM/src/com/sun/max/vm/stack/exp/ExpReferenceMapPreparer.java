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
package com.sun.max.vm.stack.exp;

import com.sun.max.unsafe.Pointer;

/**
 * This interface represents a callback for each frame to record its reference map.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public interface ExpReferenceMapPreparer {
    /**
     * This method is called by each frame as it prepares its reference map, informing the preparer
     * of the words within its frame that contain references.
     *
     * @param layout the stack frame layout object (for debugging)
     * @param cursor the stack frame cursor (for debugging)
     * @param start the pointer at which these reference map bits start
     * @param bitMap the bits of the bitmap, with bit 0 corresponding to word at offset 0 from the start
     * @param numBits the number of bits to consider in the map
     */
    void recordReferenceBits(ExpStackFrameLayout layout, ExpStackWalker.Cursor cursor, Pointer start, int bitMap, int numBits);
}
