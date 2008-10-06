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
/*VCSID=159a1f06-e386-4c5a-9328-1923e942a166*/
/**
 * @author Bernd Mathiske
 */

#include "debug.h"

#include "c.h"

void c_initialize(void) {
    debug_ASSERT((Unsigned1) -1 > 0);
    debug_ASSERT(sizeof(Unsigned1) == 1);

    debug_ASSERT((Unsigned2) -1 > 0);
    debug_ASSERT(sizeof(Unsigned2) == 2);

    debug_ASSERT((Unsigned4) -1 > 0);
    debug_ASSERT(sizeof(Unsigned4) == 4);

    debug_ASSERT((Unsigned8) -1 > 0);
    debug_ASSERT(sizeof(Unsigned8) == 8);

    debug_ASSERT((Signed1) -1 < 0);
    debug_ASSERT(sizeof(Signed1) == 1);

    debug_ASSERT((Signed2) -1 < 0);
    debug_ASSERT(sizeof(Signed2) == 2);

    debug_ASSERT((Signed4) -1 < 0);
    debug_ASSERT(sizeof(Signed4) == 4);

    debug_ASSERT((Signed8) -1 < 0);
    debug_ASSERT(sizeof(Signed8) == 8);
}

int c_unimplemented_impl(const char* function, const char* file, int line) {
    extern void exit(int);

    debug_println("unimplemented (%s in %s:%d)", function, file, line);
    exit(1);
    return -1;
}
