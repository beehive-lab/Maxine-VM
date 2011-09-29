/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.jvmti;

import com.sun.max.unsafe.*;

/**
 * Names and offsets to the native struct used to hold the JVMTIEnv implementation fields.
 * Must match the definition in jvmti.c
 */
enum JvmtiEnvImplFields {
    FUNCTIONS(0),
    CALLBACKS(8),
    CAPABILITIES(16), // the capabilities that are active for this environment
    EVENTMASK(24); // global event enabled mask

    int offset;

    JvmtiEnvImplFields(int offset) {
        this.offset = offset;
    }

    /**
     * Return the value of the field at our offset from {@code base}.
     */
    Word get(Pointer base) {
        return base.readWord(offset);
    }

    Pointer getPtr(Pointer base) {
        return base.readWord(offset).asPointer();
    }

    void set(Pointer base, Word value) {
        base.writeWord(offset, value);
    }
}
