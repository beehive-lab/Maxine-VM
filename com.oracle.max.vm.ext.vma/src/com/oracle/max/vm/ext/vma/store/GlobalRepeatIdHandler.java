/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.store;

import java.util.concurrent.*;

public class GlobalRepeatIdHandler extends RepeatIdHandler {
    /**
     * Maintains the last id for the set of threads in the trace, when not in {@link #perThread} mode.
     */
    private static ConcurrentMap<String, LastId> repeatedIds = new ConcurrentHashMap<String, LastId>();

    @Override
    protected LastId getLastId(String threadName) {
        LastId lastId = repeatedIds.get(threadName);
        if (lastId == null) {
            lastId = new LastId();
            LastId winner = repeatedIds.putIfAbsent(threadName, lastId);
            // Another thread may have beaten us to it.
            if (winner != null) {
                lastId = winner;
            }
        }
        return lastId;
    }
}
