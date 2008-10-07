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
/*VCSID=50fb3ccc-780c-4c31-80e5-caadbb4c7353*/
package com.sun.max.memory;

import com.sun.max.unsafe.*;

/**
 * A buffer is a block of memory at a fixed address
 * that is allocated for a very short period of time,
 * typically for the duration of some IO function.
 *
 * // TODO: better implementation that takes advantage of the above timing hint and support by Heap interface
 *
 * @author Bernd Mathiske
 */
public final class MemoryBuffer {
    private MemoryBuffer() {
    }

    public static Pointer allocate(Size size) {
        return Memory.allocate(size);
    }

    public static void deallocate(Pointer buffer) {
        Memory.deallocate(buffer);
    }
}
