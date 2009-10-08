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

/**
 * @author Doug Simon
 */
#include <unistd.h>
#include <stdlib.h>
#include <string.h>

#include "log.h"
#include "jni.h"
#include "word.h"
#include "image.h"

#include "threadLocals.h"

int theThreadLocalsSize = -1;
int theJavaFrameAnchorSize = -1;

void threadLocals_initialize(int threadLocalsSize, int javaFrameAnchorSize) {
    theThreadLocalsSize = threadLocalsSize;
    theJavaFrameAnchorSize = javaFrameAnchorSize;
}

int threadLocalsSize() {
    c_ASSERT(theThreadLocalsSize > 0);
    return theThreadLocalsSize;
}

int javaFrameAnchorSize() {
    c_ASSERT(theJavaFrameAnchorSize > 0);
    return theJavaFrameAnchorSize;
}

void threadLocals_println(ThreadLocals tl) {
    NativeThreadLocals ntl = getThreadLocal(NativeThreadLocals, tl, NATIVE_THREAD_LOCALS);
    log_println("ThreadLocals[%d: base=%p, end=%p, size=%lu, triggered=%p, enabled=%p, disabled=%p]",
                    ntl->id, ntl->stackBase, ntl->stackBase + ntl->stackSize, ntl->stackSize,
                    getThreadLocal(Address, tl, SAFEPOINTS_TRIGGERED_THREAD_LOCALS),
                    getThreadLocal(Address, tl, SAFEPOINTS_ENABLED_THREAD_LOCALS),
                    getThreadLocal(Address, tl, SAFEPOINTS_DISABLED_THREAD_LOCALS));
}

void threadLocals_printList(ThreadLocals tl) {
    while (tl != 0) {
        threadLocals_println(tl);
        tl = getThreadLocal(ThreadLocals, tl, FORWARD_LINK);
    };
}
