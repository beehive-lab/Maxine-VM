package com.sun.max.tele;

import com.sun.max.collect.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.heap.gcx.ms.*;

/**
 * Implementation details about the heap in the VM, specialized for the mark-sweep implementation.
 *
 * @author Laurent Daynes
 *
 */
public final class TeleMSHeapScheme extends AbstractTeleVMHolder implements TeleHeapScheme {

    TeleMSHeapScheme(TeleVM teleVM) {
        super(teleVM);
    }

    public Class heapSchemeClass() {
        return MSHeapScheme.class;
    }

    public Offset gcForwardingPointerOffset() {
        // MS is a non-moving collector. Doesn't do any forwarding.
        return null;
    }

    public Pointer getForwardedOrigin(Pointer origin) {
        // MS is a non-moving collector. Doesn't do any forwarding.
       return origin;
    }

    public Pointer getTrueLocationFromPointer(Pointer pointer) {
        return pointer;
    }

    public boolean isForwardingPointer(Pointer pointer) {
        return false;
    }

    public boolean isInLiveMemory(Address address) {
        if (teleVM().isInGC()) {
            // Unclear what the semantics of this should be during GC.
            // We should be able to tell past the marking phase if an address point to a live object.
            // But what about during the marking phase ? The only thing that can be told is that
            // what was dead before marking begin should still be dead during marking.
            return true;
        }
        // TODO:
        // This requires the inspector to know intimately about the heap structures.
        // The current MS scheme  linearly allocate over chunk of free space discovered during the past MS.
        // However, it doesn't maintain these as "linearly allocating memory region". This could be done by formatting
        // all reusable free space as such (instead of the chunk of free list as is done now). in any case.
        return false;
    }

    public boolean isObjectForwarded(Pointer origin) {
        return false;
    }

    public Sequence<MaxCodeLocation> inspectableMethods() {
        // TODO
        return Sequence.Static.empty(MaxCodeLocation.class);
    }

}
