/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
    log_println("unimplemented (%s in %s:%d)", function, file, line);
    exit(1);
    return -1;
}

void _c_assert(boolean condition, char *conditionString, char *fileName, int lineNumber) {
    if (!condition) {
        log_println("assert %s[%d]: %s", fileName, lineNumber, conditionString);
        exit(1);
    }
}
