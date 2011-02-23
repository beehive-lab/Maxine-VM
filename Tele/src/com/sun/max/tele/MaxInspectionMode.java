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

import com.sun.max.vm.hosted.*;

/**
 *  Modes in which the VM can be inspected.
 *
 * @author Mick Jordan
 * @author Michael Van De Vanter
 */
public enum MaxInspectionMode {

    /**
     * Create and start a new process to execute the VM, passing arguments.
     */
    CREATE("Create", "Create and start a new process to execute the VM, passing arguments."),

    /**
     * Attach to an existing VM process that is already running or core-dumped.
     */
    ATTACH("Attach", "Attach to an existing VM process that is already running or core-dumped."),

    /**
     * Attach to an existing VM process that is waiting to be started.
     * The process exists, the arguments have been supplied, but it
     * has not executed the VM, but it waiting for the Inspector to release it.
     */
    ATTACHWAITING("Attach/waiting", "Attach to an existing VM process that is waiting to be started."),

    /**
     * Browse a VM image, as produced by the {@link BootImageGenerator}, statically with no process.
     */
    IMAGE("Static image", "Browse a VM boot image statically, with no process");

    private final String label;
    private final String description;

    private MaxInspectionMode(String label, String description) {
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
