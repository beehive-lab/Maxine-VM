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
package com.sun.max.asm.sparc;

/**
 * Software traps for SPARC's trap instruction.
 * See /usr/include/sys/trap.h on Solaris.
 *
 * @author Laurent Daynes
 */
public enum SoftwareTrap {
    ST_OSYSCALL(0),
    ST_BREAKPOINT(0x1),
    ST_DIV0(0x2),
    ST_FLUSH_WINDOWS(0x3),
    ST_CLEAN_WINDOWS(0x4),
    ST_RANGE_CHECK(0x5),
    ST_FIX_ALIGN(0x6),
    ST_INT_OVERFLOW(0x7),
    ST_SYSCALL(0x8),

    ST_DTRACE_PID(0x38),
    ST_DTRACE_PROBE(0x39),
    ST_DTRACE_RETURN(0x3a);

    private final int _trapNumber;
    private SoftwareTrap(int trapNumber) {
        _trapNumber = trapNumber;
    }

    public int trapNumber() {
        return _trapNumber;
    }
}
