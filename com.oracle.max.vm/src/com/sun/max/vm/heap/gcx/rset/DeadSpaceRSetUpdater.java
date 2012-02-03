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

/**
 * Class implementing update to the remembered set when a new dead space is identified.
 * Remembered Set implementation may provide a sub-class of this class to update
 * remembered set when splitting dead space into dead and live space, or when coalescing
 * multiple live / dead space into a single live/dead space.
 */
public class DeadSpaceRSetUpdater {
    public static DeadSpaceRSetUpdater nullDeadSpaceRSetUpdater() {
        return new DeadSpaceRSetUpdater();
    }

    protected DeadSpaceRSetUpdater() {
    }

    /**
     * Update the remembered set covering a dead reclaimed area.
     * @param deadSpace start of the dead area
     * @param numDeadBytes size of the dead area
     */
    public void updateRSet(Address deadSpace, Size numDeadBytes) {
        // Default. Do nothing.
    }
}
