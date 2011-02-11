/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins;


/**
 * Notification service for changes to state in the VM and inspection session.
 *
 *
 * @author Michael Van De Vanter
 */
public interface InspectionListener {

    /**
     * Notifies that  VM state has potentially changed and should be revisited.
     *
     * @param force suspend caching behavior; reload state unconditionally.
     */
    void vmStateChanged(boolean force);

    /**
     * Notifies that the set and/or status (enabled/disabled) of breakpoints in the VM has changed.
     */
    void breakpointStateChanged();

    /**
     * Notifies that the set of watchpoints in the VM has changed.
     */
    void watchpointSetChanged();

    /**
     * Notifies that an important aspect of view style/parameters/configuration have changed,
     * and that views should be reconstructed if needed.  This notification does
     * <strong>not</strong> imply any change of VM state.
     */
    void viewConfigurationChanged();

    /**
     * Notifies that the running process associated with a VM has
     * stopped running.
     */
    void vmProcessTerminated();

    /**
     * Notifies that the inspection session is shutting down.  This notification
     * will arrive before the final saving of inspection settings.
     *
     * @see InspectionSettings
     */
    void inspectionEnding();

}
