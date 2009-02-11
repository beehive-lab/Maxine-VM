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
package com.sun.max.vm.heap;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.object.*;

/**
 * The dynamic Java object heap.
 *
 * @author Bernd Mathiske
 */
public final class Heap {

    private Heap() {
    }

    private static final VMSizeOption _maxHeapSizeOption = new VMSizeOption("-Xmx", Size.G, "The maximum heap size.", MaxineVM.Phase.PRISTINE);

    private static final VMSizeOption _initialHeapSizeOption = new InitialHeapSizeOption();

    static class InitialHeapSizeOption extends VMSizeOption {
        @PROTOTYPE_ONLY
        public InitialHeapSizeOption() {
            super("-Xms", Size.M.times(512), "The initial heap size.", MaxineVM.Phase.PRISTINE);
        }
        @Override
        public boolean check() {
            return !(isPresent() && _maxHeapSizeOption.isPresent() && getValue().greaterThan(_maxHeapSizeOption.getValue()));
        }
        @Override
        public void printErrorMessage() {
            Log.print("initial heap size must not be greater than max heap size");
        }
    }

    private static final VMOption _disableGCOption = new VMOption("-XX:DisableGC", "Disable garbage collection.", MaxineVM.Phase.PRISTINE);

    /**
     * Return the maximum heap size specified by the "-Xmx" command line option.
     * @return the size of the maximum heap specified on the command line
     */
    public static Size maxSize() {
        if (_maxHeapSizeOption.isPresent() || _maxHeapSizeOption.getValue().greaterThan(_initialHeapSizeOption.getValue())) {
            return _maxHeapSizeOption.getValue();
        }
        return _initialHeapSizeOption.getValue();
    }

    /**
     * Return the initial heap size specified by the "-Xms" command line option.
     * @return the size of the initial heap specified on the command line
     */
    public static Size initialSize() {
        if (_initialHeapSizeOption.isPresent() || _initialHeapSizeOption.getValue().lessThan(_maxHeapSizeOption.getValue())) {
            return _initialHeapSizeOption.getValue();
        }
        return _maxHeapSizeOption.getValue();
    }

    /**
     * Returns whether the "-verbose:gc" option was specified.
     * @return {@code true} if the user specified the "-verbose:gc" command line option; {@code false}
     * otherwise
     */
    public static boolean verbose() {
        return VerboseVMOption.verboseGC();
    }

    /**
     * Determines if garbage collection activity should be traced at a level useful for debugging.
     */
    @INLINE
    public static boolean traceGC() {
        return _traceGC;
    }

    /**
     * Determines if garbage collection root scanning should be traced at a level useful for debugging.
     */
    @INLINE
    public static boolean traceGCRootScanning() {
        return _traceGCRootScanning;
    }

    private static boolean _traceGC;
    private static boolean _traceGCRootScanning;

    private static final VMOption _traceGCOption = new VMOption("-XX:TraceGC", "Trace garbage collection activity for debugging purposes.", MaxineVM.Phase.STARTING) {
        @Override
        public boolean parseValue(Pointer optionValue) {
            if (CString.equals(optionValue, "")) {
                _traceGC = true;
                _traceGCRootScanning = true;
            } else if (CString.equals(optionValue, ":RootScanning")) {
                _traceGCRootScanning = true;
            } else {
                return false;
            }
            return true;
        }
        @Override
        public void printHelp() {
            VMOptions.printHelpForOption("-XX:TraceGC[:RootScanning]", "", _help);
        }
    };

    /**
     * Returns whether the "-XX:DisableGC" option was specified.
     *
     * @return {@code true} if the user specified the "-XX:DisableGC" command line option; {@code false}
     * otherwise
     * @return
     */
    public static boolean gcDisabled() {
        return _disableGCOption.isPresent();
    }

    @INSPECTED
    private static final LinearAllocatorHeapRegion _bootHeapRegion = new LinearAllocatorHeapRegion(Address.zero(), Size.fromInt(Integer.MAX_VALUE), "Heap-Boot");

    @INLINE
    public static LinearAllocatorHeapRegion bootHeapRegion() {
        return _bootHeapRegion;
    }

    @UNSAFE
    @FOLD
    private static HeapScheme heapScheme() {
        return VMConfiguration.hostOrTarget().heapScheme();
    }

    /**
     * @see HeapScheme#isGcThread(Thread)
     */
    public static boolean isGcThread(Thread thread) {
        return heapScheme().isGcThread(thread);
    }

    public static void initializeAuxiliarySpace(Pointer primordialVmThreadLocals, Pointer auxiliarySpace) {
        heapScheme().initializeAuxiliarySpace(primordialVmThreadLocals, auxiliarySpace);
    }

    public static void initializeVmThread(Pointer vmThreadLocals) {
        heapScheme().initializeVmThread(vmThreadLocals);
    }

    @INLINE
    public static Object createArray(DynamicHub hub, int length) {
        return heapScheme().createArray(hub, length);
    }

    @INLINE
    public static Object createTuple(Hub hub) {
        return heapScheme().createTuple(hub);
    }

    @INLINE
    public static Object createHybrid(DynamicHub hub) {
        return heapScheme().createHybrid(hub);
    }

    @INLINE
    public static Hybrid expandHybrid(Hybrid hybrid, int length) {
        return heapScheme().expandHybrid(hybrid, length);
    }

    @INLINE
    public static Object clone(Object object) {
        return heapScheme().clone(object);
    }

    @INLINE
    public static boolean contains(Address address) {
        return heapScheme().contains(address);
    }

    private static boolean _collecting;

    public static boolean collectGarbage(Size requestedFreeSpace) {
        return heapScheme().collectGarbage(requestedFreeSpace);
    }

    public static Size reportFreeSpace() {
        return heapScheme().reportFreeSpace();
    }

    public static void runFinalization() {
        heapScheme().runFinalization();
    }

    @INLINE
    public static boolean pin(Object object) {
        return heapScheme().pin(object);
    }

    @INLINE
    public static void unpin(Object object) {
        heapScheme().unpin(object);
    }

    @INLINE
    public static boolean isPinned(Object object) {
        return heapScheme().isPinned(object);
    }
}
