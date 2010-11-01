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
package com.sun.max.vm.trampoline;

import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;

public final class DynamicTrampoline {

    public final int dispatchTableIndex;

    @CONSTANT_WHEN_NOT_ZERO
    private TargetMethod trampoline;

    public DynamicTrampoline(int dispatchTableIndex, TargetMethod trampoline) {
        this.dispatchTableIndex = dispatchTableIndex;
        this.trampoline = trampoline;
        assert trampoline == null || trampoline.classMethodActor.isVirtualTrampoline() || trampoline.classMethodActor.isInterfaceTrampoline();
    }

    public void initTrampoline(TargetMethod trampoline) {
        this.trampoline = trampoline;
    }

    public TargetMethod trampolineTargetMethod() {
        return trampoline;
    }

    /**
     * Gets the {@link CallEntryPoint#VTABLE_ENTRY_POINT} in the virtual or interface method selected by
     * a given receiver object and the {@link #dispatchTableIndex} of this trampoline. This will compile
     * the selected method first if necessary. In addition, this method patches the relevant vtable or itable
     * entry to contain the returned address.
     *
     * @param receiver the receiver of a virtual or interface invocation
     */
    private Address getMethodEntryPoint(Object receiver) {
        final Hub hub = ObjectAccess.readHub(receiver);
        if (trampoline.classMethodActor.isVirtualTrampoline()) {
            final VirtualMethodActor selectedCallee = hub.classActor.getVirtualMethodActorByVTableIndex(dispatchTableIndex);
            if (selectedCallee.isAbstract()) {
                throw new AbstractMethodError();
            }
            final Address vtableEntryPoint = CompilationScheme.Static.compile(selectedCallee, VTABLE_ENTRY_POINT);
            hub.setWord(dispatchTableIndex, vtableEntryPoint);
            return vtableEntryPoint;
        }
        final VirtualMethodActor selectedCallee = hub.classActor.getVirtualMethodActorByIIndex(dispatchTableIndex);
        if (selectedCallee.isAbstract()) {
            throw new AbstractMethodError();
        }
        final Address itableEntryPoint = CompilationScheme.Static.compile(selectedCallee, VTABLE_ENTRY_POINT);
        hub.setWord(hub.iTableStartIndex + dispatchTableIndex, itableEntryPoint);
        return itableEntryPoint;
    }

    @FOLD
    private DynamicTrampolineExit trampolineExit() {
        return vmConfig().trampolineScheme().dynamicTrampolineExit();
    }

    /**
     * Fixup the dispatch table of the receiver of the method invoked via the trampoline, and return the exit point of the trampoline.
     * The exit point will be used by the trampoline epilogue to overwrite its RIP.
     * The exit point is either the entry point of the method invoked via the trampoline, or the adapter frame, if there is one. If the
     * latter, the trampoline will exit to the entry point of the selected method upon return from the adapter. To this end, the RIP of the adapter is patched to
     * the entry point of the method invoked via the trampoline. Note that in this case the stack frame is temporarily in an inconsistent
     * state (i.e., during the ret instruction), and stack walkers (other than inspection) must not access the stack of the current thread.
     * @param receiver
     * @param stackPointer
     */
    @NEVER_INLINE
    public Address trampolineReturnAddress(Object receiver,  Pointer stackPointer) {
        final Address vtableEntryPoint = getMethodEntryPoint(receiver);
        return trampolineExit().trampolineReturnAddress(this, vtableEntryPoint, stackPointer);
    }

    public static final CriticalMethod trampolineReturnAddress = new CriticalMethod(DynamicTrampoline.class, "trampolineReturnAddress", null);
}
