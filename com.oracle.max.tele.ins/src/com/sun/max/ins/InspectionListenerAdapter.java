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
 * An abstract adapter class for receiving inspection events.
 * The methods in this class are empty.  This class exists
 * as a convenience for creating listener objects.
 *
 * Extend this class, override the methods of interest, and
 * register with the inspection via
 * {@link Inspection#addInspectionListener(InspectionListener)} and
 * {@link Inspection#removeInspectionListener(InspectionListener)}.
 */
public abstract class InspectionListenerAdapter implements InspectionListener {

    public void vmStateChanged(boolean force) {
    }

    public void breakpointStateChanged() {
    }

    public void watchpointSetChanged() {
    }

    public void viewConfigurationChanged() {
    }

    public void vmProcessTerminated() {
    }

    public void inspectionEnding() {
    }
}
