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
package com.sun.max.tele.page;



/**
 * Interface to the virtual memory features of the x64.
 *
 * @author Mick Jordan
 *
 */

public class X64VM {

    public static final int L1_SHIFT = 12;
    public static final int L2_SHIFT = 21;
    public static final int L3_SHIFT = 30;
    public static final int L4_SHIFT = 39;

    public static final long L1_MASK = (1L << L2_SHIFT) - 1;
    public static final long L2_MASK = (1L << L3_SHIFT) - 1;
    public static final long L3_MASK = (1L << L4_SHIFT) - 1;

    public static final int L1_ENTRIES = 512;
    public static final int L2_ENTRIES = 512;
    public static final int L3_ENTRIES = 512;
    public static final int L4_ENTRIES = 512;

    public static final int PAGE_SHIFT = L1_SHIFT;
    public static final int PAGE_SIZE = 1 << PAGE_SHIFT;
    public static final int PAGE_OFFSET_MASK = PAGE_SIZE - 1;
    public static final int PAGE_MASK = ~PAGE_OFFSET_MASK;

    public static final int PAGE_PRESENT  = 0x001;
    public static final int PAGE_RW            = 0x002;
    public static final int PAGE_USER         = 0x004;
    public static final int PAGE_PWT          = 0x008;
    public static final int PAGE_PCD           = 0x010;
    public static final int PAGE_ACCESSED = 0x020;
    public static final int PAGE_DIRTY         = 0x040;
    public static final int PAGE_PAT            = 0x080;
    public static final int PAGE_PSE            = 0x080;
    public static final int PAGE_GLOBAL      = 0x100;

    public static final int PADDR_BITS = 52;
    public static final long PADDR_MASK = (1L << PADDR_BITS) - 1;

}

