/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.compiler.deopt;

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.util.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.deps.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.Cursor;
import com.sun.max.vm.thread.*;

/**
 * Mechanism for applying deoptimization to one or more invalidated target methods.
 * <ol>
 * <li>
 * Atomically mark invalidated target method with DEOPT flag (subsequent compilation must
 * produce a baseline target method, trampolines never link to the invalidated version).
 * Any current recompilation must check for this flag before adding the recompiled method
 * to the target state. Currently, this cannot occur in Maxine as only opt methods can
 * have deopt points and opt methods are never recompiled. We will need to revisit this once the latter is no longer true.
 * </li>
 * <li>
 * Find all references to invalidated target method(s) in dispatch tables (e.g. vtables,
 * itables etc) and revert to trampoline references. Concurrent patching ok here as it is atomic.
 * </li>
 * <li>Global safepoint (for deopt).</li>
 * <li>
 * Find all references to invalidated target method (i.e. at call sites) and revert to trampolines
 * (this code patching is why we must be at a safepoint). That is, we are not guaranteed to be
 * patching call sites that have their offset aligned appropriately for an atomic update.
 * </li>
 * Find all active frames for invalidated method. Increment 'deoptCounter' on the invalidated
 * method for each frame encountered apart from top frame(s). Counter is decremented each time
 * deopt stub replaces the deopt'ed frame. The deopted/invalidated method can be reclaimed in
 * the code cache once the counter is zero.
 * <ul>
 *    <li>For each non-top frame, patch callee return address to deopt stub.</li>
 *    <li>For each top-frame get T1X version (compiling if necessary) and deopt immediately.</li>
 * </ul>
 * </li>
 * <li>Resume from safepoint.</li>
 */
public class Deoptimizer extends VmOperation implements TargetMethod.Closure {

    public static boolean TraceDeopt;
    static {
        VMOptions.addFieldOption("-XX:", "TraceDeopt", "Trace deoptimization.");
    }

    /**
     * The set of invalidated dependencies denoting the target methods to be deoptimized.
     */
    private final ArrayList<Dependencies> invalidatedDeps;

    public Deoptimizer(ArrayList<Dependencies> invalidatedDeps) {
        super("Deoptimization", null, Mode.Safepoint);

        // Allow GC during deopt
        this.allowsNestedOperations = true;

        this.invalidatedDeps = invalidatedDeps;
    }

    /**
     * Determines if a given target method is being deoptimized.
     */
    boolean isInvalidated(TargetMethod tm) {
        for (Dependencies deps : invalidatedDeps) {
            if (tm == deps.targetMethod()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Performs deoptimization for the set of methods this object was constructed with.
     */
    public void go() {
        for (Dependencies deps : invalidatedDeps) {
            final TargetMethod tm = deps.targetMethod();
            if (TraceDeopt) {
                Log.println("DEOPT: processing " + tm);
            }

            // Atomically marks method as non-entrant, preventing them from being linked at call sites or patched into dispatch tables
            tm.setDeoptInfo(new DeoptInfo(tm));

            // Find all references to invalidated target method(s) in dispatch tables (e.g. vtables, itables etc) and revert to trampoline references.
            // Concurrent patching ok here as it is atomic.

            final ClassMethodActor method = tm.classMethodActor;
            assert method != null : "de-opting target method with null class method: " + tm;
            if (method instanceof VirtualMethodActor) {
                VirtualMethodActor virtualMethod = (VirtualMethodActor) method;
                final int vtableIndex = virtualMethod.vTableIndex();
                if (vtableIndex >= 0) {
                    final Address trampoline = vm().stubs.virtualTrampoline(vtableIndex);
                    ClassActor.Closure c = new ClassActor.Closure() {
                        @Override
                        public boolean doClass(ClassActor classActor) {
                            DynamicHub hub = classActor.dynamicHub();
                            if (hub.getWord(vtableIndex).equals(tm.getEntryPoint(VTABLE_ENTRY_POINT))) {
                                hub.setWord(vtableIndex, trampoline);
                                if (TraceDeopt) {
                                    Log.println("DEOPT:   patched vtable[" + vtableIndex + "] of " + classActor + " with trampoline");
                                }
                            }
                            final int lastITableIndex = hub.iTableStartIndex + hub.iTableLength;
                            for (int i = hub.iTableStartIndex; i < lastITableIndex; i++) {
                                if (hub.getWord(i).equals(tm.getEntryPoint(VTABLE_ENTRY_POINT))) {
                                    int iIndex = i - hub.iTableStartIndex;
                                    hub.setWord(i, vm().stubs.interfaceTrampoline(iIndex));
                                    if (TraceDeopt) {
                                        Log.println("DEOPT:   patched itable[" + iIndex + "] of " + classActor + " with trampoline");
                                    }
                                }
                            }
                            return true;
                        }
                    };
                    c.doClass(method.holder());

                    assert DependenciesManager.classHierarchyLock.isWriteLockedByCurrentThread();
                    method.holder().allSubclassesDo(c);
                }
            }
        }

        submit();
    }

    private static TargetMethod asTargetMethod(Object directCallee) {
        if (directCallee instanceof TargetMethod) {
            return (TargetMethod) directCallee;
        } else {
            return ((ClassMethodActor) directCallee).currentTargetMethod();
        }
    }

    @Override
    public boolean doTargetMethod(TargetMethod tm) {
        for (int i = 0; i < tm.numberOfDirectCalls(); i++) {
            Object directCallee = tm.directCallees()[i];
            if (isInvalidated(asTargetMethod(directCallee))) {
                if (TraceDeopt) {
                    Log.println("DEOPT:   patched direct call " + i + " in " + tm);
                }
                tm.resetDirectCall(i);
            }
        }
        return true;
    }

    @Override
    protected void doIt() {
        // Process code cache
        Code.bootCodeRegion().doAllTargetMethods(this);
        Code.getCodeManager().getRuntimeCodeRegion().doAllTargetMethods(this);

        doAllThreads();
    }

    @Override
    public void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
        Pointer tla = vmThread.tla();
        final boolean threadWasInNative = TRAP_INSTRUCTION_POINTER.load(tla).isZero();
        if (threadWasInNative) {
            walkStack(Pointer.zero(), vmThread.tla(), ip, sp, fp);
        }
    }

    @Override
    protected void doAtSafepointBeforeBlocking(final Pointer trapState) {
        // note that this procedure always runs with safepoints disabled
        final Pointer tla = Safepoint.getLatchRegister();
        final Pointer etla = ETLA.load(tla);

        TrapStateAccess trapStateAccess = vm().trapStateAccess;
        Pointer ip = trapStateAccess.getPC(trapState);
        Pointer sp = trapStateAccess.getSP(trapState);
        Pointer fp = trapStateAccess.getFP(trapState);

        walkStack(trapState, etla, ip, sp, fp);
    }

    static class Patch {
        final Pointer patchPointer;
        final Address value;
        final Pointer savePointer;
        final Patch next;
        public Patch(Patch next, Pointer patchPointer, Address value, Pointer savePointer) {
            this.next = next;
            this.patchPointer = patchPointer;
            this.value = value;
            this.savePointer = savePointer;
        }

        void apply() {
            Word oldValue = patchPointer.readWord(0);
            if (TraceDeopt) {
                Log.print("DEOPT: patch @ ");
                Log.print(patchPointer);
                Log.print(": ");
                Log.print(oldValue);
                Log.print("-> ");
                Log.print(value);
                Log.print(", saved old value @ ");
                Log.println(savePointer);
            }
            patchPointer.writeWord(0, value);
            savePointer.writeWord(0, oldValue);
        }
    }

    class FrameVisitor extends RawStackFrameVisitor {
        final Pointer trapState;
        Patch patches;

        public FrameVisitor(Pointer trapState) {
            this.trapState = trapState;
        }

        @Override
        public boolean visitFrame(Cursor current, Cursor callee) {
            TargetMethod tm = current.targetMethod();
            if (tm != null && isInvalidated(tm)) {
                int deoptStubOffset = tm.deoptHandlerOffset();
                assert deoptStubOffset != -1 : "cannot deopt method without a deopt stub: " + tm;
                Address deoptStub = tm.codeStart().plus(deoptStubOffset);
                Pointer savePointer = current.sp().plus(tm.deoptReturnAddressOffset());
                if (!current.isTopFrame()) {
                    Pointer patchPointer = callee.targetMethod().returnAddressPointer(callee);
                    patches = new Patch(patches, patchPointer, deoptStub, savePointer);
                } else {
                    assert !trapState.isZero();
                    final TrapStateAccess trapStateAccess = vm().trapStateAccess;
                    Pointer patchPointer = trapStateAccess.getPCPointer(trapState);
                    patches = new Patch(patches, patchPointer, deoptStub, savePointer);
                }
            }
            return true;
        }

    }

    private void walkStack(final Pointer trapState, final Pointer etla, Pointer ip, Pointer sp, Pointer fp) {
        if (TraceDeopt) {
            Log.println("DEOPT[" + Thread.currentThread().getName() + "]: walking stack of " + VmThread.fromTLA(etla).getName());
        }

        VmStackFrameWalker walker = new VmStackFrameWalker(etla);
        FrameVisitor fv = new FrameVisitor(trapState);
        walker.inspect(ip, sp, fp, fv);

        for (Patch patch = fv.patches; patch != null; patch = patch.next) {
            patch.apply();
        }

        if (!trapState.isZero()) {
            TargetMethod tm = Code.codePointerToTargetMethod(ip);
            if (tm != null && isInvalidated(tm)) {
                // Top frame is for invalidated method - deopt immediately
                FatalError.unimplemented();
            }
        }
    }
}
