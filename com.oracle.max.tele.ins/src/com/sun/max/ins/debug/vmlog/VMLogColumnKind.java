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
package com.sun.max.ins.debug.vmlog;

import com.sun.max.ins.debug.*;
import com.sun.max.tele.*;
import com.sun.max.vm.log.*;


/**
 * Defines the columns that can be displayed describing VM log entries.
 * @see VMLog
 */
enum VMLogColumnKind implements ColumnKind {
    ID("Id", "unique id", true, 5) {

        @Override
        public boolean canBeMadeInvisible() {
            return false;
        }
    },
    THREAD("Thread", "thread that created the entry", true, -1),
    OPERATION("Operation", "operation name", true, -1) {

        @Override
        public boolean canBeMadeInvisible() {
            return false;
        }
    },
    ARG1("Arg1", "argument 1", true, -1),
    ARG2("Arg2", "argument 2", true, -1),
    ARG3("Arg3", "argument 3", true, -1),
    ARG4("Arg4", "argument 4", true, -1),
    ARG5("Arg5", "argument 5", true, -1),
    ARG6("Arg6", "argument 6", true, -1),
    ARG7("Arg7", "argument 7", true, -1),
    ARG8("Arg8", "argument 8", true, -1);

    private final String label;
    private final String toolTipText;
    private final boolean defaultVisibility;
    private final int minWidth;

    private VMLogColumnKind(String label, String toolTipText, boolean defaultVisibility, int minWidth) {
        this.label = label;
        this.toolTipText = toolTipText;
        this.defaultVisibility = defaultVisibility;
        this.minWidth = minWidth;
        assert defaultVisibility || canBeMadeInvisible();
    }

    public boolean isSupported(MaxVM vm) {
        return true;
    }

    public String label() {
        return label;
    }

    public String toolTipText() {
        return toolTipText;
    }

    public int minWidth() {
        return minWidth;
    }

    @Override
    public String toString() {
        return label;
    }

    public boolean canBeMadeInvisible() {
        return true;
    }

    public boolean defaultVisibility() {
        return defaultVisibility;
    }
}
