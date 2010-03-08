package com.sun.max.vm.heap.gcx.ms;

import com.sun.max.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;

/**
 * Simple Mark-Sweep. Just for testing marking algorithm.
 * @see MaxPackage
 *
 * @author Laurent Daynes
 */
public class Package extends VMPackage {
    public Package() {
        registerScheme(HeapScheme.class, MSHeapScheme.class);
    }

    @Override
    public boolean isPartOfMaxineVM(VMConfiguration vmConfiguration) {
        return vmConfiguration.heapPackage.equals(this);
    }
}
