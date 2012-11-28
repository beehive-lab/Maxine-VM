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
package com.sun.max.tele.method;

import java.io.*;

import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;

/**
 * A cache of summary information about code cache allocations for method compilations in the VM,
 * organized for efficient lookup by memory address.
 * <p>
 * <strong>Note:</strong> Compilation data for a method, described by an instance of {@link TargetMethod},
 * includes both code, represented as a {@code byte[]} in standard VM object format, as well as up to two
 * other arrays, also stored in standard VM object format.  As a consequence, there are memory locations
 * in the code cache allocation for a method compilation that do not point to code. This map does not
 * distinguish between the two kinds of location, but rather treats the complete code cache allocation
 * for a method compilation as a unit.
 * <p>
 * <strong>Note:</strong> This implementation assumes that the code cache allocation for a compilation does not move.
 *
 * @see CodeRegion
 * @see TargetMethod
 */
final class AddressToCompilationMap {

    private SortedMemoryRegionList<MaxEntityMemoryRegion<MaxCompilation>> compilationMemoryRegions;

    /**
     * Creates a map for efficient retrieval of compilations in the VM by code address, assuming that
     * compiled code does not move.
     */
    AddressToCompilationMap(MaxVM vm) {
        compilationMemoryRegions = new SortedMemoryRegionList<MaxEntityMemoryRegion<MaxCompilation>>();
    }

    /**
     * Adds an entry to the code cache allocation map that represents a VM method compilation.
     *
     * @param teleCompilation the method compilation whose memory region is to be added to this registry
     * @throws IllegalArgumentException when the code's memory overlaps one already in this registry.
     */
    void add(TeleCompilation teleCompilation) {
        final MaxEntityMemoryRegion<MaxCompilation> memoryRegion = teleCompilation.memoryRegion();
        assert memoryRegion.start().isNotZero();
        compilationMemoryRegions.add(memoryRegion);
    }

    /**
     * Locates a method compilation, by an address in its code cache allocation.  Note that the
     * specified memory location may contain code but may also contain other data produced by the
     * compiler that is not code.
     *
     * @return the method compilation in the map whose code cache allocation includes the specified address, null if none.
     * @see TargetMethod
     */
    TeleCompilation find(Address address) {
        final MaxEntityMemoryRegion<MaxCompilation> compilationRegion = compilationMemoryRegions.find(address);
        if (compilationRegion != null) {
            final MaxCompilation compilation = compilationRegion.owner();
            if (compilation != null) {
                return (TeleCompilation) compilation;
            }
        }
        return null;
    }

    /**
     * @return the number of method compilations in the map
     */
    int size() {
        return compilationMemoryRegions.size();
    }

    /**
     * Remove all method compilations from the map.
     */
    void clear() {
        compilationMemoryRegions = new SortedMemoryRegionList<MaxEntityMemoryRegion<MaxCompilation>>();
    }

    void writeSummary(PrintStream printStream) {
        Address lastEndAddress = null;
        for (MaxEntityMemoryRegion<MaxCompilation> compilationMemoryRegion : compilationMemoryRegions) {
            final MaxCompilation maxCompilation = compilationMemoryRegion.owner();
            final String name = maxCompilation.entityDescription();
            if (lastEndAddress != null && !lastEndAddress.equals(compilationMemoryRegion.start())) {
                printStream.println(lastEndAddress.toHexString() + "--" + compilationMemoryRegion.start().minus(1).toHexString() + ": ");
            }
            lastEndAddress = compilationMemoryRegion.end();
            printStream.println(compilationMemoryRegion.start().toHexString() + "--" + compilationMemoryRegion.end().minus(1).toHexString() + ":  " + name);
        }
    }
}
