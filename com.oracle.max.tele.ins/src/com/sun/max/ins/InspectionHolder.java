/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.ins.gui.*;
import com.sun.max.ins.view.*;
import com.sun.max.tele.*;

/**
 * Convenience methods for access to classes holding various parts of the interactive inspection session.
 */
public interface InspectionHolder {

    /**
     * @return holder of the interactive inspection state for this session
     */
    Inspection inspection();

    /**
     * @return the VM associated with this inspection
     */
    MaxVM vm();

    /**
     * @return access to basic GUI services for the session.
     */
    InspectorGUI gui();

    /**
     * @return information about the user focus of attention in the view state.
     */
    InspectionFocus focus();

    /**
     * @return access to view management
     */
    InspectionViews views();

    /**
     * @return access to {@link InspectorAction}s of general use.
     */
    InspectionActions actions();

    /**
     * @return access to various kinds of user preferences
     */
    InspectionPreferences preference();
}
