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
package com.sun.max.vm.debug;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;

public final class DebugHeap {

    private DebugHeap() {
    }

    public static final int UNINITIALIZED = 0xdeadbeef;

    public static final long LONG_OBJECT_TAG = 0xcccccccceeeeeeeeL;

    public static final int INT_OBJECT_TAG = 0xcccceeee;

    public static boolean isValidCellTag(Word word) {
        if (Word.width() == WordWidth.BITS_64) {
            if (word.asAddress().toLong() == LONG_OBJECT_TAG) {
                return true;
            }
        }
        return word.asAddress().toInt() == INT_OBJECT_TAG;
    }

    @INLINE
    public static void writeCellTag(Pointer cell) {
        if (VMConfiguration.hostOrTarget().debugging()) {
            if (Word.width() == WordWidth.BITS_64) {
                cell.setLong(-1, DebugHeap.LONG_OBJECT_TAG);
            } else {
                cell.setInt(-1, DebugHeap.INT_OBJECT_TAG);
            }
        }
    }

    @INLINE
    public static Pointer checkDebugCellTag(RuntimeMemoryRegion from, Pointer cell) {
        if (VMConfiguration.hostOrTarget().debugging()) {
            if (!isValidCellTag(cell.getWord(0))) {
                Log.print("Invalid object tag @ ");
                Log.print(cell);
                Log.print("(start + ");
                Log.print(cell.minus(from.start()).asOffset().toInt());
                Log.println(")");
                FatalError.unexpected("INVALID CELL TAG");
            }
            return cell.plusWords(1);
        }
        return cell;
    }

}
