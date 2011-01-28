/*
 * Copyright (c) 2009, 2009, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.tele.debug;

/**
 * The possible states of a {@link TeleProcess}.
 *
 * NOTE: This must be kept in sync with ProcessState in Native/tele/teleProcess.h.
 *
 * @author Michael Van De Vanter
 */
public enum ProcessState {

    /**
     * Represents a non-process or an unknown process state.
     */
    UNKNOWN,

    /**
     * The process is live, but paused.
     */
    STOPPED,

    /**
     * The process is live and running.
     */
    RUNNING,

    /**
     * The process was live, but has died.
     */
    TERMINATED;

    public static final ProcessState[] VALUES = values();
}
