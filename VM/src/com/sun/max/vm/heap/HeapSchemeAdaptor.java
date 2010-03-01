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

import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.management.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.management.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Class to capture common methods for heap scheme implementations.
 *
 * @author Mick Jordan
 */
public abstract class HeapSchemeAdaptor extends AbstractVMScheme implements HeapScheme {


    public static class GarbageCollectorMXBeanAdaptor extends MemoryManagerMXBeanAdaptor implements GarbageCollectorMXBean  {
        public GarbageCollectorMXBeanAdaptor(String name) {
            super(name);
        }

        public GcInfo getLastGcInfo() {
            return null;
        }

        public long getCollectionCount() {
            return 0;
        }

        public long getCollectionTime() {
            return 0;
        }

    }

    /**
     * Switch to turn off allocation globally.
     */
    protected boolean allocationEnabled = true;

    public HeapSchemeAdaptor(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    public boolean decreaseMemory(Size amount) {
        return false;
    }

    public boolean increaseMemory(Size amount) {
        return false;
    }

    public void disableAllocationForCurrentThread() {
        FatalError.unimplemented();
    }

    public void enableAllocationForCurrentThread() {
        FatalError.unimplemented();
    }

    @INLINE(override = true)
    public boolean usesTLAB() {
        return false;
    }

    public void disableImmortalMemoryAllocation() {
        final Pointer enabledVmThreadLocals = VmThread.currentVmThreadLocals().getWord(VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer();
        enabledVmThreadLocals.setWord(IMMORTAL_ALLOCATION_ENABLED.index, Word.zero());
    }

    public void enableImmortalMemoryAllocation() {
        final Pointer enabledVmThreadLocals = VmThread.currentVmThreadLocals().getWord(VmThreadLocal.SAFEPOINTS_ENABLED_THREAD_LOCALS.index).asPointer();
        enabledVmThreadLocals.setWord(IMMORTAL_ALLOCATION_ENABLED.index, Word.allOnes());
    }

    public long maxObjectInspectionAge() {
        FatalError.unimplemented();
        return 0;
    }

    public boolean isInitialized() {
        return MaxineVM.isRunning();
    }

    public GarbageCollectorMXBean getGarbageCollectorMXBean() {
        return new GarbageCollectorMXBeanAdaptor("Invalid") {
            @Override
            public boolean isValid() {
                return false;
            }
        };
    }

    public void reportPristineMemoryFailure(String memoryAreaName, Size numberOfBytes) {
        Log.println("Error occurred during initialization of VM");
        Log.print("Failed to allocate");
        Log.print(numberOfBytes.toLong());
        Log.print(" bytes of memory for ");
        Log.println(memoryAreaName);
        MaxineVM.native_exit(1);
    }
}
