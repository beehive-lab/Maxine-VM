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
 * @author Bernd Mathiske
 */

#include <stdlib.h>
#include "log.h"

#include "c.h"

void c_initialize(void) {
    c_ASSERT((Unsigned1) -1 > 0);
    c_ASSERT(sizeof(Unsigned1) == 1);

    c_ASSERT((Unsigned2) -1 > 0);
    c_ASSERT(sizeof(Unsigned2) == 2);

    c_ASSERT((Unsigned4) -1 > 0);
    c_ASSERT(sizeof(Unsigned4) == 4);

    c_ASSERT((Unsigned8) -1 > 0);
    c_ASSERT(sizeof(Unsigned8) == 8);

    c_ASSERT((Signed1) -1 < 0);
    c_ASSERT(sizeof(Signed1) == 1);

    c_ASSERT((Signed2) -1 < 0);
    c_ASSERT(sizeof(Signed2) == 2);

    c_ASSERT((Signed4) -1 < 0);
    c_ASSERT(sizeof(Signed4) == 4);

    c_ASSERT((Signed8) -1 < 0);
    c_ASSERT(sizeof(Signed8) == 8);
}

int _c_unimplemented(const char* function, const char* file, int line) {
    extern void exit(int);

    log_println("unimplemented (%s in %s:%d)", function, file, line);
    exit(1);
    return -1;
}

void _c_assert(Boolean condition, char *conditionString, char *fileName, int lineNumber) {
    if (!condition) {
        log_println("assert %s[%d]: %s", fileName, lineNumber, conditionString);
        exit(1);
    }
}
