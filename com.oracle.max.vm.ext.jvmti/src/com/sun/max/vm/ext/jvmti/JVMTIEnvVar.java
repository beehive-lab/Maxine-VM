/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.ext.jvmti;

import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;

/**
 * Access to environment variables when no heap is available.
 */
public class JVMTIEnvVar {
    final Pointer environ = MaxineVM.native_environment();

    private static final int MAX_ENVIRON_SIZE = 8 * Ints.K;

    static Pointer getValue(Pointer variablePtr) {
        final Pointer environ = MaxineVM.native_environment();

        for (int offset = 0; offset < MAX_ENVIRON_SIZE; offset += Word.size()) {
            final Pointer nameValuePair = environ.readWord(offset).asPointer();
            if (nameValuePair.isZero()) {
                // end of list
                break;
            }
            int length;
            byte value = 0;
            for (length = 0; length < MAX_ENVIRON_SIZE; length++) {
                value = nameValuePair.getByte(length);
                if (value == 0) {
                    break;
                } else if (value == (byte) '=') {
                    break;
                }
            }
            if (length == 0 || value == 0) {
                break;
            }
            // length is number of bytes in the name
            if (variablePtr.getByte(length) == 0 && Memory.equals(nameValuePair, variablePtr, Size.fromInt(length))) {
                return nameValuePair.plus(length + 1);
            }
        }
        return Pointer.zero();
    }

}
