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

import java.io.*;
import java.util.*;

import com.sun.max.unsafe.*;

/**
 * Access to the cache in which the VM stores method compilations, described by one more more instances of
 * {@link MaxCodeCacheRegion}.
 * <p>
 * The code cache consists of a single region in the boot image (the <em>boot code cache</em>), in which the VM's
 * pre-compiled methods reside, together with one or more runtime regions that are allocated dynamically. Different
 * regions may be managed differently, for example some may be managed and some not.
 * <p>
 * An area of memory is allocated within a code regions to store the results of each compilation.
 */
public interface MaxCodeCache extends MaxEntity<MaxCodeCache> {

    /**
     * @return description of the special code cache region included in the binary boot image.
     */
    MaxCodeCacheRegion bootCodeRegion();

    /**
     * Gets descriptions all currently allocated code cache regions in the VM's compiled code cache, including the boot
     * code cache.
     *
     * @return descriptions for all code cache regions in the VM.
     */
    List<MaxCodeCacheRegion> codeCacheRegions();

    /**
     * Finds a code cache region by location, where the location could be anywhere in the code
     * cache's memory, even if unallocated or not pointing at machine code.
     *
     * @param address a memory location in the VM.
     * @return the code cache region, if any, that contains the specified location
     */
    MaxCodeCacheRegion findCodeCacheRegion(Address address);

    /**
     * Writes current statistics concerning inspection of VM's code cache.
     *
     * @param printStream stream to which to write
     * @param indent number of spaces to indent each line
     * @param verbose possibly write extended information when true
     */
    void printSessionStats(PrintStream printStream, int indent, boolean verbose);

}
