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
package com.sun.max.tele.thread;

import com.sun.max.vm.thread.*;

/**
 * Java access to the NativeThreadLocalsStruct in Native/share/threadLocals.h for use by Inspector.
 * Unlike {@link VmThreadLocal} we use a simple enum as we are only interested in the field offsets.
 *
 * @author Mick Jordan
 */
public enum NativeThreadLocal {
    STACKBASE(0),
    STACKSIZE(8),
    HANDLE(16),
    TLBLOCK(24),
    TLBLOCKSIZE(32),
    STACK_YELLOW_ZONE(40),
    STACK_RED_ZONE(48),
    STACK_BLUE_ZONE(56),
    OSDATA(64);

    public int offset;

    NativeThreadLocal(int offset) {
        this.offset = offset;
    }

}
