/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.tele;


/**
 * Possible states for the process running the {@linkplain MaxVM VM}.
 *
 * @author Michael Van De Vanter
 */
public enum MaxProcessState {

    /**
     * Null state for when there is no process.
     */
    NONE("None", "Null state for when there is no process"),

    /**
     * The process is live, but paused.
     */
    STOPPED("Stopped", "Live process, paused"),

    /**
     * The process is live and running.
     */
    RUNNING("Running", "Live process, running"),

    /**
     * The process was live, but has died.
     */
    TERMINATED("Terminated", "Process no longer live"),

    /**
     * Represents a non-process or an unknown process state.
     */
    UNKNOWN("Unknown", "Process state cannot be determined");

    private final String label;
    private final String description;

    private MaxProcessState(String label, String description) {
        this.label = label;
        this.description = description;
    }

    /**
     * @return a short, human readable name for the inspection mode
     */
    public String label() {
        return label;
    }

    /**
     * @return a short textual explanation of the inspection mode
     */
    public String description() {
        return description;
    }

}
