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

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;
import static com.sun.max.vm.compiler.target.Stub.Type.*;
import static com.sun.max.vm.stack.VMFrameLayout.*;

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.RuntimeCompiler.Nature;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.TargetMethod.FrameAccess;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * Mechanism for applying deoptimization to one or more target methods.
 * There are two separate parts to deoptimization:
 * <ul>
 * <li><b>Mark methods for deoptimization.</b>
 * <ol>
 * <li>A deoptimization {@linkplain #Deoptimization(ArrayList) request} specifies a set of methods, <i>M</i>, to be deoptimized.</li>
 * <li>Each method in <i>M</i> is {@linkplain TargetMethod#invalidate(InvalidationMarker) invalidated}.</li>
 * <li>Any vtable or itable entry denoting a method in <i>M</i> is reverted to a {@link Stubs#virtualTrampoline(int) vtable}
 * or {@link Stubs#interfaceTrampoline(int) itable} trampoline.</li>
 * <li>
 * Patch the entry points of all invalidated methods to {@linkplain TargetMethod#redirectTo(TargetMethod) redirect} the caller
 * to the {@linkplain Stubs#staticTrampoline() static trampoline}. This ensures all direct calls to an
 * invalidated method will be re-linked the next time they are executed.
 * </li>
 * <li>Scan each thread looking for frames executing a method in <i>M</i>:
 * <ul>
 *    <li>For each non-top frame executing a method in <i>M</i>, patch the callee's return address to the relevant {@linkplain Stubs#deoptStub(CiKind, boolean) stub}.</li>
 *    <li>For each top-frame executing a method in <i>M</i>, patch the return address in the trap stub to {@link Stubs#deoptStubForSafepointPoll()}.</li>
 * </ul>
 * </li>
 * </ol>
 * All but step 1 above are performed in a {@linkplain #doIt() VM operation} (i.e. all threads have been stopped at a safepoint).
 * <p>
 * One optimization applied is for each thread to perform step 5 on itself just
 * {@linkplain VmOperation#doAtSafepointBeforeBlocking before} suspending. This is
 * analogous to each thread preparing its own reference map when being stopped
 * for a garbage collection.
 *
 * <li><b>Convert the frame of an optimized method into one or more deoptimized frames.</b>
 * <p>
 * This occurs when one of the deoptimization stubs is executed. A deoptimization stub is executed either as the result of the
 * return address patching described above or by a (compiled) call to the {@linkplain Stubs#genUncommonTrapStub() uncommon trap} stub.
 * All the deoptimization stubs eventually route through to {@link #deoptimize(Pointer, Pointer, Pointer, Pointer, CiCalleeSaveLayout, CiConstant) deoptimize()}
 * which constructs the deoptimized frames, unrolls them onto the stack and continues execution in the appropriate deoptimized frame.
 * </i>
 * </ul>
 */
public class Deoptimization extends VmOperation {

    /**
     * Option for enabling use of deoptimization.
     */
    public static boolean UseDeopt = true;

    /**
     * A VM option for triggering deoptimization at fixed intervals.
     */
    public static int DeoptimizeALot;
    public static boolean TraceDeopt;
    static {
        VMOptions.addFieldOption("-XX:", "UseDeopt", "Enable deoptimization.");
        VMOptions.addFieldOption("-XX:", "TraceDeopt", "Trace deoptimization.");
        VMOptions.addFieldOption("-XX:", "DeoptimizeALot", Deoptimization.class,
            "Invalidate and deoptimize a selection of executing optimized methods every <n> milliseconds. " +
            "A value of 0 disables this mechanism.");
    }

    /**
     * The set of target methods to be deoptimized.
     */
    private final ArrayList<TargetMethod> methods;

    /**
     * Creates an object to deoptimize a given set of methods.
     *
     * @param methods the set of methods to be deoptimized (must not contain duplicates)
     */
    public Deoptimization(ArrayList<TargetMethod> methods) {
        super("Deoptimization", null, Mode.Safepoint);
        this.methods = methods;
    }

    /**
     * Mark methods for deoptimization.
     */
    public void go() {
        submit();
    }

    @Override
    protected void doIt() {
        Stub staticTrampoline = vm().stubs.staticTrampoline();
        int i = 0;
        while (i < methods.size()) {
            TargetMethod tm = methods.get(i);
            if (TraceDeopt) {
                Log.println("DEOPT: processing " + tm);
            }
            if (!tm.invalidate(new InvalidationMarker(tm))) {
                methods.remove(i);
                if (TraceDeopt) {
                    Log.println("DEOPT: ignoring previously invalidated method " + tm);
                }
            } else {

                // Atomically marks method as invalidated
                tm.invalidate(new InvalidationMarker(tm));

                // Find all references to invalidated target method(s) in dispatch tables (e.g. vtables, itables etc) and revert to trampoline references.
                // Concurrent patching ok here as it is atomic.
                patchDispatchTables(tm);

                tm.redirectTo(staticTrampoline);
                if (TraceDeopt) {
                    Log.print("DEOPT: patched entry points of ");
                    Log.printMethod(tm, true);
                }

                i++;
            }
        }

        // Scan the stacks to patch return addresses
        doAllThreads();
    }

    /**
     * Find all instances of a given (invalidated) target method in dispatch tables
     * (e.g. vtables, itables etc) and revert these entries to be trampolines.
     * Concurrent patching ok here as it is atomic.
     */
    static void patchDispatchTables(final TargetMethod tm) {
        final ClassMethodActor method = tm.classMethodActor;
        assert method != null : "de-opting target method with null class method: " + tm;
        if (method instanceof VirtualMethodActor) {
            VirtualMethodActor virtualMethod = (VirtualMethodActor) method;
            final int vtableIndex = virtualMethod.vTableIndex();
            if (vtableIndex >= 0) {
                // must cast this from CodePointer to Address because the closure will create a field, which is not allowed
                final Address trampoline = vm().stubs.virtualTrampoline(vtableIndex).toAddress();
                ClassActor.Closure c = new ClassActor.Closure() {
                    @Override
                    public boolean doClass(ClassActor classActor) {
                        DynamicHub hub = classActor.dynamicHub();
                        if (hub.getWord(vtableIndex).equals(tm.getEntryPoint(VTABLE_ENTRY_POINT))) {
                            hub.setWord(vtableIndex, trampoline);
                            logPatchVTable(vtableIndex, classActor);
                        }
                        final int lastITableIndex = hub.iTableStartIndex + hub.iTableLength;
                        for (int i = hub.iTableStartIndex; i < lastITableIndex; i++) {
                            if (hub.getWord(i).equals(tm.getEntryPoint(VTABLE_ENTRY_POINT))) {
                                int iIndex = i - hub.iTableStartIndex;
                                hub.setWord(i, vm().stubs.interfaceTrampoline(iIndex).toAddress());
                                logPatchITable(classActor, iIndex);
                            }
                        }
                        return true;
                    }
                };
                c.doClass(method.holder());
                method.holder().allSubclassesDo(c);
            }
        }
    }

    @Override
    public void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
        Patcher patcher = new Patcher(methods);
        patcher.go(vmThread, ip, sp, fp);
    }

    /**
     * Mechanism for a frame reconstruction to specify its execution state when it is resumed or returned to.
     */
    public static abstract class Continuation {

        /**
         * The target method to which this continuation applies.
         */
        TargetMethod tm;

        /**
         * Sets the continuation frame pointer.
         *
         * @param info a stack modeled as an array of frame slots
         * @param fp the continuation frame pointer. This is either a {@link CiKind#Word} value encoding an absolute
         *            address or a {@link CiKind#Jsr} value encoding an index in {@code info.slots} that subsequently
         *            needs fixing up once absolute address have been determined for the slots.
         */
        public abstract void setFP(Info info, CiConstant fp);

        /**
         * Sets the continuation stack pointer.
         *
         * @param info a stack modeled as an array of frame slots
         * @param sp the continuation stack pointer. This is either a {@link CiKind#Word} value encoding an absolute
         *            address or a {@link CiKind#Jsr} value encoding an index in {@code info.slots} that subsequently
         *            needs fixing up once absolute address have been determined for the slots.
         */
        public abstract void setSP(Info info, CiConstant sp);

        /**
         * Sets the continuation instruction pointer.
         *
         * @param info a stack modeled as an array of frame slots
         * @param ip the continuation instruction pointer
         */
        public abstract void setIP(Info info, Pointer ip);
    }

    static void traceContinuation(Pointer ip) {
        if (TraceDeopt) {
            TargetMethod tm = Code.codePointerToTargetMethod(ip);
            assert tm != null : "cannot deoptimize frame of a VM entry method";
            Log.print("DEOPT: continuation: ");
            Log.printLocation(tm, CodePointer.from(ip), true);
        }
    }

    /**
     * Mechanism for a caller frame reconstruction to specify its execution state when it is returned to.
     */
    public static class CallerContinuation extends Continuation {
        public final int callerFPIndex;
        public final int callerSPIndex;
        public final int returnAddressIndex;

        public CallerContinuation(int callerFPIndex, int callerSPIndex, int returnAddressIndex) {
            this.callerFPIndex = callerFPIndex;
            this.callerSPIndex = callerSPIndex;
            this.returnAddressIndex = returnAddressIndex;
        }

        @Override
        public void setFP(Info info, CiConstant callerFP) {
            if (callerFPIndex >= 0) {
                info.slots.set(callerFPIndex, callerFP);
            }
        }

        @Override
        public void setSP(Info info, CiConstant callerSP) {
            if (callerSPIndex >= 0) {
                info.slots.set(callerSPIndex, callerSP);
            }
        }

        @Override
        public void setIP(Info info, Pointer ip) {
            if (returnAddressIndex >= 0) {
                info.slots.set(returnAddressIndex, WordUtil.archConstant(ip));
                traceContinuation(ip);
            }
        }
    }

    /**
     * Mechanism for a top frame reconstruction to specify its execution state when it is resumed.
     */
    static class TopFrameContinuation extends Continuation {
        int fp;
        int sp;
        NativeOrVmIP ip = new NativeOrVmIP();

        @Override
        public void setFP(Info info, CiConstant fp) {
            this.fp = fp.asInt();
        }

        @Override
        public void setSP(Info info, CiConstant sp) {
            this.sp = sp.asInt();
        }

        @Override
        public void setIP(Info info, Pointer ip) {
            this.ip.derive(ip);
            traceContinuation(ip);
        }
    }

    /**
     * Overwrites the current thread's stack with deoptimized frames and continues
     * execution in the top frame at {@code info.pc}.
     *
     * This method is called from {@link Stubs#unroll(Info, int)}.
     */
    @NEVER_INLINE
    public static void unroll(Info info) {
        ArrayList<CiConstant> slots = info.slots;
        Pointer sp = info.slotsAddr.plus(info.slotsSize() - STACK_SLOT_SIZE);
        logUnroll(info, slots, sp);
        for (int i = slots.size() - 1; i >= 0; --i) {
            CiConstant c = slots.get(i);
            if (c.kind.isObject()) {
                Object obj = c.asObject();
                sp.writeWord(0, Reference.fromJava(obj).toOrigin());
            } else if (c.kind.isLong()) {
                sp.writeLong(0, c.asLong());
            } else if (c.kind.isDouble()) {
                sp.writeDouble(0, c.asDouble());
            } else {
                sp.writeWord(0, Address.fromLong(c.asLong()));
            }
            sp = sp.minus(STACK_SLOT_SIZE);
        }

        // Checkstyle: stop
        if (info.returnValue == null) {
            // Re-enable safepoints
            SafepointPoll.enable();

            Stubs.unwind(info.ip.asPointer(), info.sp, info.fp);
        } else {
            if (StackReferenceMapPreparer.VerifyRefMaps || TraceDeopt || DeoptimizeALot != 0) {
                StackReferenceMapPreparer.verifyReferenceMaps(VmThread.current(), info.ip.vmIP(), info.sp, info.fp);
            }

            // Re-enable safepoints
            SafepointPoll.enable();

            switch (info.returnValue.kind.stackKind()) {
                case Int:     Stubs.unwindInt(info.ip.asPointer(), info.sp, info.fp, info.returnValue.asInt());
                case Float:   Stubs.unwindFloat(info.ip.asPointer(), info.sp, info.fp, info.returnValue.asFloat());
                case Long:    Stubs.unwindLong(info.ip.asPointer(), info.sp, info.fp, info.returnValue.asLong());
                case Double:  Stubs.unwindDouble(info.ip.asPointer(), info.sp, info.fp, info.returnValue.asDouble());
                case Object:  Stubs.unwindObject(info.ip.asPointer(), info.sp, info.fp, info.returnValue.asObject());
                default:      FatalError.unexpected("unexpected return kind: " + info.returnValue.kind.stackKind());
            }
        }
        // Checkstyle: resume
    }

    @NEVER_INLINE // makes inspecting easier
    private static void logUnroll(Info info, ArrayList<CiConstant> slots, Pointer sp) {
        if (TraceDeopt) {
            Log.println("DEOPT: Unrolling frames");
            for (int i = slots.size() - 1; i >= 0; --i) {
                CiConstant c = slots.get(i);
                String name = info.slotNames.get(i);
                Log.print("DEOPT: ");
                Log.print(sp);
                Log.print("[" + name);
                for (int pad = name.length(); pad < 12; pad++) {
                    Log.print(' ');
                }
                Log.print("]: ");
                Log.println(c);
                sp = sp.minus(STACK_SLOT_SIZE);
            }

            Log.print("DEOPT: unrolling: ip=");
            Log.print(info.ip);
            Log.print(", sp=");
            Log.print(info.sp);
            Log.print(", fp=");
            Log.print(info.fp);
            if (info.returnValue != null) {
                Log.print(", returnValue=");
                Log.print(info.returnValue);
            }
            Log.println();
        }
    }

    /**
     * Called from the {@code int} {@linkplain Stubs#deoptStub(CiKind, boolean) deoptimization stub} for.
     */
    @NEVER_INLINE
    public static void deoptimizeInt(Pointer ip, Pointer sp, Pointer fp, Pointer csa, int returnValue) {
        deoptimizeOnReturn(CodePointer.from(ip), sp, fp, csa, CiConstant.forInt(returnValue));
    }

    /**
     * Called from the {@code float} {@linkplain Stubs#deoptStub(CiKind, boolean) deoptimization stub} for.
     */
    @NEVER_INLINE
    public static void deoptimizeFloat(Pointer ip, Pointer sp, Pointer fp, Pointer csa, float returnValue) {
        deoptimizeOnReturn(CodePointer.from(ip), sp, fp, csa, CiConstant.forFloat(returnValue));
    }

    /**
     * Called from the {@code long} {@linkplain Stubs#deoptStub(CiKind, boolean) deoptimization stub} for.
     */
    @NEVER_INLINE
    public static void deoptimizeLong(Pointer ip, Pointer sp, Pointer fp, Pointer csa, long returnValue) {
        deoptimizeOnReturn(CodePointer.from(ip), sp, fp, csa, CiConstant.forLong(returnValue));
    }

    /**
     * Called from the {@code double} {@linkplain Stubs#deoptStub(CiKind, boolean) deoptimization stub} for.
     */
    @NEVER_INLINE
    public static void deoptimizeDouble(Pointer ip, Pointer sp, Pointer fp, Pointer csa, double returnValue) {
        deoptimizeOnReturn(CodePointer.from(ip), sp, fp, csa, CiConstant.forDouble(returnValue));
    }

    /**
     * Called from the {@code Word} {@linkplain Stubs#deoptStub(CiKind, boolean) deoptimization stub} for.
     */
    @NEVER_INLINE
    public static void deoptimizeWord(Pointer ip, Pointer sp, Pointer fp, Pointer csa, Word returnValue) {
        deoptimizeOnReturn(CodePointer.from(ip), sp, fp, csa, WordUtil.archConstant(returnValue));
    }

    /**
     * Called from the {@code Object} {@linkplain Stubs#deoptStub(CiKind, boolean) deoptimization stub} for.
     */
    @NEVER_INLINE
    public static void deoptimizeObject(Pointer ip, Pointer sp, Pointer fp, Pointer csa, Object returnValue) {
        deoptimizeOnReturn(CodePointer.from(ip), sp, fp, csa, CiConstant.forObject(returnValue));
    }

    /**
     * Called from the {@code void} {@linkplain Stubs#deoptStub(CiKind, boolean) deoptimization stub} for.
     */
    @NEVER_INLINE
    public static void deoptimizeVoid(Pointer ip, Pointer sp, Pointer fp, Pointer csa) {
        deoptimizeOnReturn(CodePointer.from(ip), sp, fp, csa, null);
    }

    static boolean stackIsWalkable(StackFrameWalker sfw, Pointer ip, Pointer sp, Pointer fp) {
        sfw.inspect(ip, sp, fp, new RawStackFrameVisitor.Default());
        return true;
    }

    /**
     * Stack visitor used to patch return addresses denoting a method being deoptimized.
     */
    public static class Patcher extends com.sun.max.vm.stack.RawStackFrameVisitor {
        /**
         * The set of methods being deoptimized.
         */
        final ArrayList<TargetMethod> methods;

        public Patcher(ArrayList<TargetMethod> methods) {
            this.methods = methods;
        }

        private ClassMethodActor lastCalleeMethod;

        /**
         * Walk the stack of a given thread and patch all return addresses denoting
         * one of the methods in {@link #methods}.
         *
         * @param thread the thread whose stack is to be walked
         */
        public void go(VmThread thread, Pointer ip, Pointer sp, Pointer fp) {
            VmStackFrameWalker sfw = new VmStackFrameWalker(thread.tla());
            sfw.inspect(ip, sp, fp, this);

            assert stackIsWalkable(sfw, ip, sp, fp);
        }

        @Override
        public boolean visitFrame(StackFrameCursor current, StackFrameCursor callee) {
            TargetMethod tm = current.targetMethod();
            TargetMethod calleeTM = callee.targetMethod();
            boolean deopt = methods.contains(tm);
            if (deopt && calleeTM.is(TrapStub)) {
                // Patches the return address in the trap frame to trigger deoptimization
                // of the top frame when the trap stub returns.
                Stub stub = vm().stubs.deoptStubForSafepointPoll();
                CodePointer to = stub.codeStart();
                Pointer save = current.sp().plus(DEOPT_RETURN_ADDRESS_OFFSET);
                Pointer patch = tm.returnAddressPointer(callee);
                CodePointer from = CodePointer.from(patch.readWord(0));
                assert !to.equals(from);
                logPatchReturnAddress(tm, "TRAP STUB", stub, to, save, patch, from);
                patch.writeWord(0, to.toAddress());
                save.writeWord(0, from.toAddress());
            } else {
                if (calleeTM != null && calleeTM.classMethodActor != null) {
                    lastCalleeMethod = calleeTM.classMethodActor;
                }
                if (deopt) {
                    patchReturnAddress(current, callee, lastCalleeMethod);
                }
            }
            return true;
        }
    }

    /**
     * Encapsulates various info used during deoptimization of a single optimized frame.
     */
    public static class Info extends com.sun.max.vm.stack.RawStackFrameVisitor {

        /**
         * Method being deoptimized.
         */
        final TargetMethod tm;

        // The following are initially the details of the frame being deoptimized.
        // Just before unrolling, they are then modified to reflect the top
        // deoptimized frame.
        NativeOrVmIP ip = new NativeOrVmIP();
        Pointer sp;
        Pointer fp;

        /**
         * The address in the stack at which the {@linkplain #slots slots} of the
         * deoptimized frame(s) are unrolled.
         */
        Pointer slotsAddr;

        // The following are the details of the caller of the frame being deoptimized.

        /**
         * The actual return address in frame being deoptimized. This will differ from
         * {@link #callerIP} when the caller is (also) marked for deoptimization.
         */
        NativeOrVmIP returnIP = new NativeOrVmIP();

        /**
         * The instruction pointer in the caller of the frame being deoptimized.
         */
        NativeOrVmIP callerIP = new NativeOrVmIP();

        /**
         * The stack pointer in the caller (frame) of the frame being deoptimized.
         */
        Pointer callerSP;

        /**
         * The frame pointer in the caller (frame) of the frame being deoptimized.
         */
        Pointer callerFP;

        /**
         * The return value.
         */
        CiConstant returnValue;

        /**
         * The slots of the deoptimized frame(s).
         */
        final ArrayList<CiConstant> slots = new ArrayList<CiConstant>();

        /**
         * Names of the deoptimized stack slots. Only used if {@link Deoptimization#TraceDeopt} is enabled.
         */
        final ArrayList<String> slotNames = TraceDeopt ? new ArrayList<String>() : null;

        /**
         * Creates a context for deoptimizing a frame executing a given method.
         *
         * @param thread the frame's thread
         * @param ip the execution point in the frame
         * @param sp the stack pointer of the frame
         * @param fp the frame pointer of the frame
         */
        public Info(VmThread thread, Pointer ip, Pointer sp, Pointer fp) {
            this.ip.derive(ip);
            this.tm = this.ip.targetMethod();
            this.sp = sp;
            this.fp = fp;

            VmStackFrameWalker sfw = new VmStackFrameWalker(thread.tla());
            sfw.inspect(ip, sp, fp, this);

            assert stackIsWalkable(sfw, ip, sp, fp);
        }

        @Override
        public boolean visitFrame(StackFrameCursor current, StackFrameCursor callee) {

            if (current.isTopFrame()) {
                // Ensure that the top frame of the walk is the method being deoptimized
                assert tm == current.targetMethod();
                assert ip.asPointer() == current.ipAsPointer();
                assert sp == current.sp();
                assert fp == current.fp();
                return true;
            }

            assert callee.isTopFrame();
            TargetMethod calleeTM = callee.targetMethod();
            assert calleeTM == tm;

            callerSP = current.sp();
            callerFP = current.fp();
            callerIP.derive(current.ipAsPointer());

            // The caller may (also) be marked for deoptimization. If so, then the callee's
            // return address slot may denote a deopt stub address. However,
            // the stack frame walker "recovers" the original return address which is
            // reflected in current.ip(). We must use the actual return address after
            // deoptimizing the callee and so the code below by-passes any
            // stack frame walker "recovery".
            returnIP.derive(calleeTM.returnAddressPointer(callee).readWord(0).asPointer());
            return false;
        }

        public TargetMethod callerTM() {
            return callerIP.targetMethod();
        }

        public void addSlot(CiConstant slot, String name) {
            slots.add(slot);
            if (slotNames != null) {
                slotNames.add(name);
            }
        }

        public int slotsSize() {
            return slotsCount() * STACK_SLOT_SIZE;
        }

        public int slotsCount() {
            return slots.size();
        }
    }

    /**
     * Patches the return address in a callee frame to trigger deoptimization of an invalidated caller.
     *
     * @param caller the frame of the method to be deoptimized
     * @param callee the callee frame whose return address is to be patched
     * @param calleeMethod the class method actor that is being called. This is required in addition to {@code callee}
     *            in the case where {@code callee} is an adapter frame
     */
    static void patchReturnAddress(StackFrameCursor caller, StackFrameCursor callee, ClassMethodActor calleeMethod) {
        assert calleeMethod != null;
        TargetMethod tm = caller.targetMethod();
        Stub stub = vm().stubs.deoptStub(calleeMethod.descriptor().returnKind(true), callee.targetMethod().is(CompilerStub));
        CodePointer to = stub.codeStart();
        Pointer save = caller.sp().plus(DEOPT_RETURN_ADDRESS_OFFSET);
        Pointer patch = callee.targetMethod().returnAddressPointer(callee);
        CodePointer from = CodePointer.from(patch.readWord(0));
        assert !to.equals(from);
        logPatchReturnAddress(tm, callee.targetMethod(), stub, to, save, patch, from);
        patch.writeWord(0, to.toAddress());
        save.writeWord(0, from.toAddress());
    }

    /**
     * The fixed offset in a method's frame where the original return address is saved when the callee's
     * return address slot is {@linkplain #patchReturnAddress(StackFrameCursor, StackFrameCursor) patched} with the address
     * of a {@linkplain Stubs#deoptStub(CiKind, boolean) deoptimization stub}.
     */
    public static final int DEOPT_RETURN_ADDRESS_OFFSET = 0;

    /**
     * Deoptimizes a method that is being returned to via a {@linkplain Stubs#deoptStub(CiKind, boolean) deoptimization stub}.
     *
     * @param ip the address in the method returned to
     * @param sp the stack pointer of the frame executing the method
     * @param fp the frame pointer of the frame executing the method
     * @param csa the callee save area. This is non-null iff deoptimizing upon return from a compiler stub.
     * @param returnValue the value being returned (will be {@code null} if returning from a void method)
     */
    private static void deoptimizeOnReturn(CodePointer ip, Pointer sp, Pointer fp, Pointer csa, CiConstant returnValue) {
        deoptimize(ip, sp, fp, csa, csa.isZero() ? null : vm().registerConfigs.compilerStub.csl, returnValue);
    }

    /**
     * Deoptimizes a method executing in a given frame.
     * Constructs the deoptimized frames, unrolls them onto the stack and continues execution
     * in the top most deoptimized frame.
     *
     * @param ip the continuation address in the method
     * @param sp the stack pointer of the frame executing the method
     * @param fp the frame pointer of the frame executing the method
     * @param csa the callee save area. This is non-null iff deoptimizing upon return from a compiler stub.
     * @param csl the layout of the callee save area pointed to by {@code csa}
     * @param returnValue the value being returned (will be {@code null} if returning from a void method or not deoptimizing upon return)
     */
    public static void deoptimize(CodePointer ip, Pointer sp, Pointer fp, Pointer csa, CiCalleeSaveLayout csl, CiConstant returnValue) {
        SafepointPoll.disable();
        Info info = new Info(VmThread.current(), ip.toPointer(), sp, fp);

        if (TraceDeopt) {
            Log.println("DEOPT: Deoptimizing " + info.tm);
            Throw.stackDump("DEOPT: Raw stack frames:");
            new Throwable("DEOPT: Bytecode stack frames:").printStackTrace(Log.out);
        }

        if (StackReferenceMapPreparer.VerifyRefMaps || TraceDeopt || DeoptimizeALot != 0) {
            StackReferenceMapPreparer.verifyReferenceMapsForThisThread();
        }

        TargetMethod tm = info.tm;
        Throwable pendingException = VmThread.current().pendingException();

        int safepointIndex = tm.findSafepointIndex(ip);
        assert safepointIndex >= 0 : "no safepoint index for " + tm + "+" + tm.posFor(ip);

        if (TraceDeopt) {
            Log.println("DEOPT: " + tm + ", safepointIndex=" + safepointIndex + ", pos=" + tm.safepoints().posAt(safepointIndex));
        }

        FrameAccess fa = new FrameAccess(csl, csa, sp, fp, info.callerSP, info.callerFP);
        CiDebugInfo debugInfo = tm.debugInfoAt(safepointIndex, fa);
        CiFrame topFrame = debugInfo.frame();
        FatalError.check(topFrame != null, "No frame info found at deopt site: " + tm.posFor(ip));

        if (pendingException != null) {
            topFrame = unwindToHandlerFrame(topFrame, pendingException);
            assert topFrame != null : "could not (re)find handler for " + pendingException +
                                       " thrown at " + tm + "+" + ip.to0xHexString();
        }

        if (TraceDeopt) {
            // Trace the frame states in terms of the locations holding the frame values
            logFrames(unwindToHandlerFrame(tm.debugInfoAt(safepointIndex, null).frame(), pendingException), "locations");
        }

        if (TraceDeopt) {
            // Trace the frame states in terms of the frame values
            logFrames(topFrame, "values");
        }

        // Construct the deoptimized frames for each frame in the debug info
        final TopFrameContinuation topCont = new TopFrameContinuation();
        Continuation cont = topCont;
        for (CiFrame frame = topFrame; frame != null; frame = frame.caller()) {
            ClassMethodActor method = (ClassMethodActor) frame.method;
            TargetMethod compiledMethod = vm().compilationBroker.compile(method, Nature.BASELINE);
            FatalError.check(compiledMethod.isBaseline(), compiledMethod + " should be a deopt target");
            cont.tm = compiledMethod;
            boolean reexecute = false;
            if (frame == topFrame && !Safepoints.isCall(tm.safepoints().safepointAt(safepointIndex))) {
                reexecute = true;
            }
            cont = compiledMethod.createDeoptimizedFrame(info, frame, cont, pendingException, reexecute);

            // The exception (if any) must be handled in the top frame
            pendingException = null;
        }

        // On AMD64, all compilers agree on the registers used for return values (i.e. RAX and XMM0).
        // As such there is no current need to reconstruct an adapter frame between the lowest
        // deoptimized frame and the frame of its caller. This may need to be revisited for
        // other platforms so use an assertion for now.
        assert platform().isa == ISA.AMD64;

        // Fix up the caller details for the bottom most deoptimized frame
        cont.tm = info.callerTM();
        cont.setIP(info, info.returnIP.asPointer());
        cont.setSP(info, WordUtil.archConstant(info.callerSP));
        cont.setFP(info, WordUtil.archConstant(info.callerFP));

        int slotsSize = info.slotsSize();
        Pointer slotsAddrs = sp.plus(tm.frameSize() + STACK_SLOT_SIZE).minus(slotsSize);
        info.slotsAddr = slotsAddrs;

        // Fix up slots referring to other slots (the references are encoded as CiKind.Jsr values)
        ArrayList<CiConstant> slots = info.slots;
        for (int i = 0; i < slots.size(); i++) {
            CiConstant c = slots.get(i);
            if (c.kind.isJsr()) {
                int slotIndex = c.asInt();
                Pointer slotAddr = slotsAddrs.plus(slotIndex * STACK_SLOT_SIZE);
                slots.set(i, WordUtil.archConstant(slotAddr));
            }
        }

        // Compute the physical frame details for the top most deoptimized frame
        sp = slotsAddrs.plus(topCont.sp * STACK_SLOT_SIZE);
        fp = slotsAddrs.plus(topCont.fp * STACK_SLOT_SIZE);

        // Set the deopt continuation to the top most deoptimized frame
        info.ip.copyFrom(topCont.ip);
        info.sp = sp;
        info.fp = fp;
        info.returnValue = returnValue;

        // Compute the stack space between the current frame (executing this method) and the
        // the top most deoptimized frame and use it to ensure that the unroll method
        // executes with enough stack space below it to unroll the deoptimized frames
        int used = info.sp.minus(VMRegister.getCpuStackPointer()).toInt() + tm.frameSize();
        int frameSize = Platform.target().alignFrameSize(Math.max(slotsSize - used, 0));
        Stubs.unroll(info, frameSize);
        FatalError.unexpected("should not reach here");
    }

    /**
     * Finds the frame containing a handler for an exception thrown at the current BCI of the frame.
     *
     * @param topFrame the frame to start searching in
     * @param exception the exception being thrown
     * @return the frame that catches {@code exception}
     */
    static CiFrame unwindToHandlerFrame(CiFrame topFrame, Throwable exception) {
        if (exception == null) {
            return topFrame;
        }
        // Unwind to frame with handler
        for (CiFrame frame = topFrame; frame != null; frame = frame.caller()) {
            ClassMethodActor method = (ClassMethodActor) frame.method;
            CiExceptionHandler[] handlers = method.exceptionHandlers();
            if (handlers != null) {
                int bci = frame.bci;
                for (CiExceptionHandler h : handlers) {
                    if (h.startBCI <= bci && bci < h.endBCI) {
                        ClassActor catchType = (ClassActor) h.catchType;
                        if (catchType == null || catchType.isAssignableFrom(ObjectAccess.readClassActor(exception))) {
                            return frame.withEmptyStack();
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Deoptimizes a method that was trapped at a safepoint poll.
     *
     * @param ip the trap address
     * @param sp the trap stack pointer
     * @param fp the trap frame pointer
     * @param csa the callee save area
     */
    public static void deoptimizeAtSafepoint(Pointer ip, Pointer sp, Pointer fp, Pointer csa) {
        FatalError.check(!csa.isZero(), "callee save area expected for deopt at safepoint");
        deoptimize(CodePointer.from(ip), sp, fp, csa, vm().registerConfigs.trapStub.csl, null);
    }

    /**
     * Deoptimizes at an {@link Stubs#genUncommonTrapStub() uncommon trap}.
     *
     * @param ip the address of the uncommon trap
     * @param sp the stack pointer of the frame executing the method
     * @param fp the frame pointer of the frame executing the method
     */
    public static void uncommonTrap(Pointer csa, Pointer ip, Pointer sp, Pointer fp) {
        FatalError.check(!csa.isZero(), "callee save area expected for uncommon trap");
        deoptimize(CodePointer.from(ip), sp, fp, csa, vm().registerConfigs.uncommonTrapStub.getCalleeSaveLayout(), null);
    }

    @NEVER_INLINE // makes inspecting easier
    static void logPatchITable(ClassActor classActor, int iIndex) {
        if (TraceDeopt) {
            Log.println("DEOPT:   patched itable[" + iIndex + "] of " + classActor + " with trampoline");
        }
    }

    @NEVER_INLINE // makes inspecting easier
    static void logPatchVTable(final int vtableIndex, ClassActor classActor) {
        if (TraceDeopt) {
            Log.println("DEOPT:   patched vtable[" + vtableIndex + "] of " + classActor + " with trampoline");
        }
    }

    @NEVER_INLINE // makes inspecting easier
    public static void logPatchReturnAddress(TargetMethod tm, Object callee, Stub stub, CodePointer to, Pointer save, Pointer patch, CodePointer from) {
        if (TraceDeopt) {
            Log.println("DEOPT: patched return address @ " + patch.to0xHexString() + " of call to " + callee +
                            ": " + from.to0xHexString() + '[' + tm + '+' + from.minus(tm.codeStart()).toInt() + ']' +
                            " -> " + to.to0xHexString() + '[' + stub + "], saved old value @ " + save.to0xHexString());
        }
    }

    @NEVER_INLINE // makes inspecting easier
    static void logFrames(CiFrame topFrame, String label) {
        Log.println("DEOPT: --- " + label + " start ---");
        Log.println(topFrame);
        Log.println("DEOPT: --- " + label + " end ---");
    }
}
