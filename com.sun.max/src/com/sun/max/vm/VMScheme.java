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
package com.sun.max.vm;

import java.util.*;


/**
 */
public interface VMScheme {

    String name();

    /**
     * Gets the class embodying the specification of the scheme implemented by this object.
     */
    Class<? extends VMScheme> specification();

    /**
     * Gets a very brief description of this configuration.
     * Often, simply returning the name of the implementation class is sufficient.
     */
    String about();

    /**
     * Performs any scheme specific actions when entering a given VM phase.
     *
     * @param phase the VM phase that has just been entered
     */
    void initialize(MaxineVM.Phase phase);

    /**
     * Gets the set of system properties which if non-null are used to configure this scheme.
     * The returned properties object provides the property names as well as the
     * values reflecting the current configuration of this scheme.
     *
     * @return {@code null} if this scheme doesn't use any system properties to configure itself
     */
    Properties properties();
}
