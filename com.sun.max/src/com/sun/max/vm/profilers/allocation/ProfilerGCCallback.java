package com.sun.max.vm.profilers.allocation;

import com.sun.max.vm.MaxineVM;
import com.sun.max.vm.heap.Heap;

public class ProfilerGCCallback implements Heap.GCCallback {

    static {
        Heap.registerGCCallback(new ProfilerGCCallback());
    }

    @Override
    public void gcCallback(Heap.GCCallbackPhase gcCallbackPhase) {
        if (gcCallbackPhase == Heap.GCCallbackPhase.BEFORE) {
            //TODO: move here pre-gc actions
        }else if (gcCallbackPhase == Heap.GCCallbackPhase.AFTER) {
            //TODO: move here pre-gc actions
        }
    }
}
