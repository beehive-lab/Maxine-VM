package com.sun.max.vm.heap.gcx;

import com.sun.max.unsafe.*;


public interface EvacuationBufferProvider {
    Address refillEvacuationBuffer();
    void retireEvacuationBuffer(Address startOfSpaceLeft, Address endOfSpaceLeft);
}
