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
package com.sun.max.ins.view;

import com.sun.max.ins.*;
import com.sun.max.ins.gui.*;

/**
 * Manager for a kind of Inspector view that is a singleton which can either be active (the instance
 * exists and is visible) or not.
 *
 * @author Michael Van De Vanter
 */
public interface SingletonViewManager extends ViewManager {

    /**
     * Activates the singleton view, newly created if necessary.
     *
     * @return the instance that implements the singleton view
     */
    Inspector activateView(Inspection inspection);

    /**
     * Disposes the existing singleton view.
     */
    void deactivateView();
}
