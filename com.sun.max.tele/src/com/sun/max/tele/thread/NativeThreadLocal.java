/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.thread;

import com.sun.max.vm.thread.*;

/**
 * Java access to the NativeThreadLocalsStruct in Native/share/threadLocals.h for use by Inspector.
 * Unlike {@link VmThreadLocal} we use a simple enum as we are only interested in the field offsets.
 */
public enum NativeThreadLocal {
    STACKBASE(0),
    STACKSIZE(8),
    HANDLE(16),
    TLBLOCK(24),
    TLBLOCKSIZE(32),
    STACK_YELLOW_ZONE(40),
    STACK_RED_ZONE(48),
    STACK_RED_ZONE_VMPROTECTED(56),
    STACK_BLUE_ZONE(64),
    OSDATA(72);

    public static final int SIZE = 80;
    public int offset;

    NativeThreadLocal(int offset) {
        this.offset = offset;
    }

}
