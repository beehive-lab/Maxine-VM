/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.code;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
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

    /**
     * The code region that contains the boot code.
     */
    @INSPECTED
    private static final CodeRegion _bootCodeRegion = new CodeRegion(Address.fromInt(Integer.MAX_VALUE / 2).aligned(), Size.fromInt(Integer.MAX_VALUE / 4), "Code-Boot");

    /**
     * Accessor for the boot code region.
     * @return a reference to the code region that contains the boot code, with size that is "trimmed"
     *  to reflect how much of the allocation has been used.
     */
    @INLINE
    public static CodeRegion bootCodeRegion() {
        return _bootCodeRegion;
    }

    /**
     * Creates the singleton code manager for this operating system.
     * @return a new code manager
     */
    private static CodeManager createCodeManager() {
        switch (Platform.hostOrTarget().operatingSystem()) {
            case LINUX: {
                return new LowAddressCodeManager();
            }
            case GUESTVM: {
                return new VariableAddressCodeManager();
            }

            case DARWIN:
            case SOLARIS: {
                return new FixedAddressCodeManager();
            }
            default: {
                Problem.unimplemented();
                return null;
            }
        }
    }

    /**
     * The code manager singleton instance.
     */
    @INSPECTED
    private static final CodeManager _codeManager = createCodeManager();

    /**
     * Initializes the code manager.
     */
    public static void initialize() {
        _codeManager.initialize();
    }

    /**
     * Allocate space in the code manager for the specified target method, installing its code.
     *
     * @param targetMethod the target method to install into the code manager
     */
    public static void allocate(TargetMethod targetMethod) {
        _codeManager.allocate(targetMethod);
    }

    /**
     * Allocates space in this code region for a runtime stub.
     *
     * @see CodeManager#allocateRuntimeStub(RuntimeStub)
     * @param stub an object describing the size of the runtime stub (i.e. the size in bytes to allocate). If
     *            allocation is successful, the address of the memory chunk allocated (i.e. the address of the first
     *            element of the internally allocated byte array) will be accessible through the
     *            {@link MemoryRegion#start()} method of this object.
     * @return true if space was successfully allocated for the runtime stub
     */
    public static boolean allocateRuntimeStub(RuntimeStub stub) {
        return _codeManager.allocateRuntimeStub(stub);
    }

    /**
     * Determines whether any code region contains the specified address.
     *
     * @param address the address to check
     * @return {@code true} if the specified address is contained in some code region
     * managed by the code manager; {@code false} otherwise
     */
    public static boolean contains(Address address) {
        return _codeManager.codePointerToCodeRegion(address) != null;
    }

    /**
     * Looks up the target method that contains the specified code pointer.
     *
     * @param codePointer a pointer to code
     * @return a reference to the target method that contains the specified code pointer, if
     * one exists; {@code null} otherwise
     */
    @INSPECTED
    public static TargetMethod codePointerToTargetMethod(Address codePointer) {
        return _codeManager.codePointerToTargetMethod(codePointer);
    }

    /**
     * Looks up the runtime stub that contains the specified code pointer.
     *
     * @param codePointer a pointer to code
     * @return a reference to the runtime stub that contains the specified code pointer, if one exists; {@code null}
     *         otherwise
     */
    @INSPECTED
    public static RuntimeStub codePointerToRuntimeStub(Address codePointer) {
        return _codeManager.codePointerToRuntimeStub(codePointer);
    }


    /**
     * Performs code update, updating the old target method to the new target method. This typically
     * involves platform-specific code modifications to forward old code to the new code.
     *
     * @param oldTargetMethod the old target method to target to the new target method
     * @param newTargetMethod the new target method
     */
    public static void updateTargetMethod(TargetMethod oldTargetMethod, TargetMethod newTargetMethod) {
        oldTargetMethod.forwardTo(newTargetMethod);
        discardTargetMethod(oldTargetMethod);
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
     */
    public static void visitCells(CellVisitor cellVisitor) {
        _codeManager.visitCells(cellVisitor);
    }

    public static Size getCodeSize() {
        return _codeManager.getSize();
    }

    /**
     * All code memory regions, needed by the inspector.
     */
    private static MemoryRegion[] _memoryRegions = new MemoryRegion[]{};

    /**
     * Registers a new memory region with the code manager.
     *
     * @param codeRegion the code region to add
     */
    public static void registerMemoryRegion(CodeRegion codeRegion) {
        final int nextIndex = _memoryRegions.length;
        _memoryRegions = Arrays.append(_memoryRegions, new FixedMemoryRegion(codeRegion, "Code-" + nextIndex));
    }

    /**
     * Looks up any target method(s) that correspond to methods matching the specified
     * method key.
     *
     * @param methodKey the method key specifying a method
     * @return an array of all target methods that match the specified method key
     */
    @INSPECTED
    public static TargetMethod[] methodKeyToTargetMethods(MethodKey methodKey) {
        return _codeManager.methodKeyToTargetMethods(methodKey);
    }
}
