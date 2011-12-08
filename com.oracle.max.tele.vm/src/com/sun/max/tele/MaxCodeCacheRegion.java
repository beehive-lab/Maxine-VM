/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.sun.max.unsafe.*;

/**
 * Access to an individual allocation area of the compiled code cache.
 */
public interface MaxCodeCacheRegion extends MaxEntity<MaxCodeCacheRegion> {

    /**
     * @return whether this code cache region is in the boot image.
     */
    boolean isBootRegion();

    /**
     * Gets the total number of compilations currently allocated in this code cache region,
     * equal to the length of {@link #compilations()}.
     */
    int compilationCount();

    /**
     * Gets all compilations for which memory in this code cache has been allocated.
     *
     * @return all known compilations currently allocated in this code cache region, {@code length = #compilationCount()}
     */
    List<MaxCompilation> compilations();

    /**
     * Get the method compilation, if any, whose code cache allocation includes
     * a given address in the VM, whether or not there is target code at the
     * specific location.  Null if there is no such compilation.
     *
     * @param address memory location in the VM
     * @return a  method compilation whose code cache allocation includes the address, null if none
     */
    MaxCompilation findCompilation(Address address);

}
