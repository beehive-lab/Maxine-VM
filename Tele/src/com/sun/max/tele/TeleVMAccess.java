/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.tele.debug.*;
import com.sun.max.tele.method.*;

/**
 * An object that refer to some aspect of the VM state, with
 * convenience methods for access to other aspects.
 *
 * @author Michael Van De Vanter
 */
public interface TeleVMAccess {

    /**
     * @return the instance of VM being managed by this code.
     */
    TeleVM vm();

    /**
     * Gets the manager for locating and managing code related information in the VM.
     * <br>
     * Thread-safe
     *
     * @return the singleton manager for information about code in the VM.
     */
    CodeManager codeManager();

    /**
     * Gets the factory for creating and managing VM breakpoints.
     * <br>
     * Thread-safe
     *
     * @return the singleton factory for creating and managing VM breakpoints
     */
    TeleBreakpointManager breakpointManager();

    /**
     * Gets the factory for creating and managing VM watchpoints; null
     * if watchpoints are not supported on this platform.
     * <br>
     * Thread-safe
     *
     * @return the singleton factory for creating and managing VM watchpoints, or
     * null if watchpoints not supported.
     */
    TeleWatchpoint.WatchpointManager watchpointManager();
}
