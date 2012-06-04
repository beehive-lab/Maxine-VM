/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.tele.data.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.field.*;
import com.sun.max.tele.heap.*;
import com.sun.max.tele.method.CodeLocation.VmCodeLocationManager;
import com.sun.max.tele.method.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.type.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.type.*;

/**
 * An object that refer to some aspect of the VM state, with
 * convenience methods for access to other aspects.
 */
public interface TeleVMAccess {

    /**
     * @return the instance of VM being managed by this code.
     */
    MaxVM vm();

    /**
     * Gets access to the registry of loaded classes in the
     * VM and related information.
     *
     * @see ClassRegistry
     * @see VmClassAccess
     */
    VmClassAccess classes();

    /**
     * Gets access to low level reading & writing methods
     * for VM memory.
     */
    VmMemoryIO memory();

    /**
     * Gets access to information and services for managing
     * object {@link Reference}s in the VM.
     *
     * @see Reference
     * @see TeleRererence
     */
    VmReferenceManager referenceManager();

    /**
     * Gets access to predefined accessors to specific fields
     * in specific classes.
     */
    VmFieldAccess fields();

    /**
     * Gets access to predefined accessors for specific methods
     * in specific classes.
     */
    MaxMethods methods();

    /**
     * Gets the manager for information about objects in the VM.
     *
     * @return the singleton manager for object information.
     */
    VmObjectAccess objects();

    /**
     * Gets access to information about the heap in the VM.
     */
    VmHeapAccess heap();

    /**
     * Gets access to information about compiled code in the VM.
     */
    VmCodeCacheAccess codeCache();

    /**
     * Gets the manager for creating and managing code location information in the VM.
     * <p>
     * Thread-safe
     *
     * @return the singleton manager for information about code locations in the VM.
     */
    VmCodeLocationManager codeLocations();

    /**
     * Gets the factory for creating and managing VM breakpoints.
     * <p>
     * Thread-safe
     *
     * @return the singleton factory for creating and managing VM breakpoints
     */
    VmBreakpointManager breakpointManager();

    /**
     * Gets the factory for creating and managing VM watchpoints; null
     * if watchpoints are not supported on this platform.
     * <p>
     * Thread-safe
     *
     * @return the singleton factory for creating and managing VM watchpoints, or
     * null if watchpoints not supported.
     */
    VmWatchpoint.VmWatchpointManager watchpointManager();

}
