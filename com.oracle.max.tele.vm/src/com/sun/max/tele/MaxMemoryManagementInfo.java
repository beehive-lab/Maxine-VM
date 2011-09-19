/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.tele.memory.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;

/**
 * A description of the status of a memory location with respect to the VM's memory management,
 * information that is intended to support the debugging of memory management.
 */
public interface MaxMemoryManagementInfo  {

    /**
     * @return the status of the memory location with respect to the VM's memory management.
     */
    MaxMemoryStatus status();

    /**
     * @return a very short string presenting information about a memory location in the VM
     * with respect to memory management, intended to be useful for debugging memory management.
     */
    String terseInfo();

    /**
     * @return a textual description, shorter than a line, summarizing  information about
     * a memory
     * location in the VM with respect to memory management, intended to be useful for debugging memory management.
     */
    String shortDescription();

    /**
     * @return the location in VM memory whose status is being described.
     */
    Address address();

    /**
     * @return a {@link TeleHeapScheme} specific {@link TeleObject} that represents the memory management info, if any.
     */
    TeleObject tele();

}
