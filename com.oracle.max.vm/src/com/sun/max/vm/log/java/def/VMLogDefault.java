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
package com.sun.max.vm.log.java.def;

import com.sun.max.vm.log.java.*;
import com.sun.max.vm.reference.*;

/**
 * Simple space inefficient implementation.
 * Allocates {@link Record records} large enough to hold the maximum number of arguments.
 * All records are considered in use, i.e., not FREE, even if they are not currently filled in (early startup).
 */
public class VMLogDefault extends VMLogArray {

    public VMLogDefault() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = new Record7();
        }
    }

    @Override
    protected Record getRecord(int argCount) {
        int myId = nextId;
        while (Reference.fromJava(this).compareAndSwapInt(nextIdOffset, myId, myId + 1) != myId) {
            myId = nextId;
        }
        Record r = buffer[myId % logSize];
        return r;
    }

}
