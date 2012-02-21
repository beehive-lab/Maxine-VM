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
package com.sun.max.vm.code;

import java.lang.management.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.management.*;
import com.sun.max.vm.runtime.*;

/**
 * Utilities for managing target code. This class offers a number of services
 * for allocating and managing the target code regions. It also offers a static
 * facade that hides the details of operations such as finding a target method
 * by a code address, allocating space for a new target method, etc.
 */
public final class Code {

    private Code() {
    }

    static boolean TraceCodeAllocation;

    /**
     * Used by the Inspector to uniquely identify the special boot code region.
     */
    @INSPECTED
    private static final String CODE_BOOT_NAME = "Code-Boot";

    /**
     * The code region that contains the boot code.
     */
    @INSPECTED
    @CONSTANT_WHEN_NOT_ZERO
    private static CodeRegion bootCodeRegion = new CodeRegion(Address.fromInt(Integer.MAX_VALUE / 2).wordAligned(), Size.fromInt(Integer.MAX_VALUE / 4), CODE_BOOT_NAME);


    /**
     * The code manager singleton instance.
     */
    @INSPECTED
    private static final CodeManager codeManager = VMConfiguration.vmConfig().heapScheme().createCodeManager();

    public static CodeManager getCodeManager() {
        return codeManager;
    }

    /**
     * Initializes the code manager and {@link CodePointer} base address.
     */
    public static void initialize() {
        codeManager.initialize();
        CodePointer.initialize(bootCodeRegion.start());
    }

    /**
     * Allocates space in a code region for the code-related arrays of a given target method
     * and {@linkplain TargetMethod#setCodeArrays(byte[], byte[], Object[]) initializes} them.
     *
     * @param targetBundleLayout describes the layout of the arrays in the allocate space
     * @param targetMethod the target method for which the code-related arrays are allocated
     */
    public static void allocate(TargetBundleLayout targetBundleLayout, TargetMethod targetMethod) {
        codeManager.allocate(targetBundleLayout, targetMethod, false, targetMethod.lifespan());
    }

    /**
     * Allocates space in the heap for the code-related arrays of a given target method
     * and {@linkplain TargetMethod#setCodeArrays(byte[], byte[], Object[]) initializes} them.
     * The target method cannot be executed. This option exists for the purpose of testing
     * or benchmarking a compiler at runtime without polluting the code cache.
     *
     * @param targetBundleLayout describes the layout of the arrays in the allocate space
     * @param targetMethod the target method for which the code-related arrays are allocated
     */
    public static void allocateInHeap(TargetBundleLayout targetBundleLayout, TargetMethod targetMethod) {
        codeManager.allocate(targetBundleLayout, targetMethod, true, targetMethod.lifespan());
    }

    /**
     * Determines whether any code region contains the specified address.
     *
     * @param address the address to check
     * @return {@code true} if the specified address is contained in some code region
     * managed by the code manager; {@code false} otherwise
     */
    public static boolean contains(Address address) {
        return codeManager.codePointerToCodeRegion(address) != null;
    }

    /**
     * Looks up the target method that contains the specified code pointer.
     *
     * @param codePointer a pointer to code
     * @return a reference to the target method that contains the specified code pointer, if
     * one exists; {@code null} otherwise
     */
    @INSPECTED
    public static TargetMethod codePointerToTargetMethod(Pointer codePointer) {
        boolean wasDisabled = SafepointPoll.disable();
        TargetMethod result = codeManager.codePointerToTargetMethod(codePointer);
        if (!wasDisabled) {
            SafepointPoll.enable();
        }
        return result;
    }

    /**
     * Removes a target method from this code manager, freeing its space when it is safe to do so.
     *
     * @param targetMethod the target method to discard
     */
    public static void discardTargetMethod(TargetMethod targetMethod) {
        // do nothing.
    }

    /**
     * Visits each cell that is managed by the code manager.
     *
     * @param cellVisitor the cell visitor to call back for each cell
     * @param includeBootCode specifies if the cells in the {@linkplain Code#bootCodeRegion() boot code region} should
     *            also be visited
     */
    public static void visitCells(CellVisitor cellVisitor, boolean includeBootCode) {
        codeManager.visitCells(cellVisitor, includeBootCode);
    }

    public static Size getRuntimeCodeRegionSize() {
        return codeManager.getRuntimeBaselineCodeRegionSize().plus(codeManager.getRuntimeOptCodeRegionSize());
    }

    public static MemoryManagerMXBean getMemoryManagerMXBean() {
        return new CodeMemoryManagerMXBean("Code");
    }

    public static CodeRegion bootCodeRegion() {
        return bootCodeRegion;
    }

    @HOSTED_ONLY
    public static void resetBootCodeRegion() {
        bootCodeRegion = new CodeRegion(bootCodeRegion.start(), bootCodeRegion.size(), bootCodeRegion.regionName());
    }

    private static class CodeMemoryManagerMXBean extends MemoryManagerMXBeanAdaptor {
        CodeMemoryManagerMXBean(String name) {
            super(name);
            add(new CodeMemoryPoolMXBean(bootCodeRegion(), this));
            add(new CodeMemoryPoolMXBean(codeManager.getRuntimeBaselineCodeRegion(), this));
            add(new CodeMemoryPoolMXBean(codeManager.getRuntimeOptCodeRegion(), this));
        }
    }

    private static class CodeMemoryPoolMXBean extends MemoryPoolMXBeanAdaptor {
        CodeMemoryPoolMXBean(CodeRegion codeRegion, MemoryManagerMXBean manager) {
            super(MemoryType.NON_HEAP, codeRegion, manager);
        }
    }

}
