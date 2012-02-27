/*
 * Copyright (c) 2012, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.heap.gcx.rset;

import com.sun.max.unsafe.*;


public final class NullDeadSpaceListener extends DeadSpaceListener {
    private static NullDeadSpaceListener theNullDeadSpaceListener = new NullDeadSpaceListener();

    public static NullDeadSpaceListener nullDeadSpaceListener() {
        return theNullDeadSpaceListener;
    }

    private NullDeadSpaceListener() {
    }
    @Override
    public void notifyCoalescing(Address deadSpace, Size numDeadBytes) {
    }

    @Override
    public void notifySplitLive(Address leftDeadSpace, Size leftSize, Address rightDeadSpace) {
    }

    @Override
    public void notifySplitDead(Address leftDeadSpace, Size leftSize, Address rightDeadSpace) {
    }

    @Override
    public void notifyRefill(Address deadSpace, Size numDeadBytes) {
    }

    @Override
    public void notifyRetireDeadSpace(Address deadSpace, Size numDeadBytes) {
    }

    @Override
    public void notifyRetireFreeSpace(Address deadSpace, Size numDeadBytes) {
    }
}
