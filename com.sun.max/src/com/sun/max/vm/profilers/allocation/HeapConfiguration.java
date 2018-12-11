package com.sun.max.vm.profilers.allocation;

import com.sun.max.vm.VMConfiguration;
import com.sun.max.vm.heap.HeapScheme;
import com.sun.max.vm.heap.sequential.semiSpace.SemiSpaceHeapScheme;

public class HeapConfiguration {

    public int virtSpaces;
    public long[] vSpacesStartAddr;
    public long[] vSpacesEndAddr;

    public HeapConfiguration() {
        //get heap scheme from the vm
        HeapScheme heapScheme = VMConfiguration.vmConfig().heapScheme();

        if (heapScheme.name().equals("SemiSpaceHeapScheme")) {
            SemiSpaceHeapScheme sshs = (SemiSpaceHeapScheme) heapScheme;
            virtSpaces = 2;
            vSpacesStartAddr = new long[virtSpaces];
            vSpacesEndAddr = new long[virtSpaces];
            vSpacesStartAddr[0] = sshs.getToSpace().start().toLong();
            vSpacesEndAddr[0] = sshs.getToSpace().end().toLong();
            vSpacesStartAddr[1] = sshs.getFromSpace().start().toLong();
            vSpacesEndAddr[1] = sshs.getFromSpace().end().toLong();
        }
        else {
            //TODO: implementation for further heap scheme support
            virtSpaces = 0;
        }
    }
}
