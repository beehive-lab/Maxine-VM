package com.sun.max.vm.heap.gcx;

import static com.sun.max.vm.VMOptions.*;

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;

/**
 * Fixed size marking stack. Used raw memory, allocated outside of the scope of the heap.
 *
 * @author Laurent Daynes
 */
public class MarkingStack {
    private static final VMIntOption markingStackSizeOption =
        register(new  VMIntOption("-XX:MarkingStackSize=", 16, "Size of the marking stack in number of references."),
                        MaxineVM.Phase.PRISTINE);

    Address base;
    Address top;
    Address end;

    static Size size() {
        return Size.fromInt(markingStackSizeOption.getValue());
    }

    MarkingStack() {
    }

    void initialize() {
        Size size = Size.fromInt(markingStackSizeOption.getValue() << Word.widthValue().log2numberOfBytes);
        base = Memory.allocate(size);
        if (base.isZero()) {
            ((HeapSchemeAdaptor) VMConfiguration.target().heapScheme()).reportPristineMemoryFailure("marking stack", size);
        }
        top = base;
        end = base.plus(size);
    }
}
