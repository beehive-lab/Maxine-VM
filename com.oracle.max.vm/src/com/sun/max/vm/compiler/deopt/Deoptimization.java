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
import static com.sun.max.vm.VMConfiguration.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;
import static com.sun.max.vm.compiler.CompilationScheme.CompilationFlag.*;
import static com.sun.max.vm.compiler.deps.DependenciesManager.*;
import static com.sun.max.vm.compiler.target.Stub.Type.*;
import static com.sun.max.vm.stack.VMFrameLayout.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.TargetMethod.FrameAccess;
import com.sun.max.vm.compiler.target.TargetMethod.FrameInfo;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.Cursor;
import com.sun.max.vm.thread.*;

/**
 * Mechanism for applying deoptimization to one or more invalidated target methods.
 * The algorithm for applying deoptimization to a set of invalidated methods is as follows:
 * <ol>
 * <li>
 * Atomically {@linkplain TargetMethod#invalidate(InvalidationMarker) invalidate} the target methods.
 * </li>
 * <li>
 * Find all references to the invalidated target methods in dispatch tables (e.g. vtables,
 * itables etc) and revert to trampoline references. Concurrent patching is ok here as it is
 * an atomic updated to a constant value.
 * </li>
 * <li>{@link #submit() Submit} deoptimization VM operation.</li>
 * <li>
 * Scan each thread looking for frames executing an invalidated method:
 * <ul>
 *    <li>For each non-top frame, patch the callee's return address to the relevant {@linkplain Stubs#deoptStub(CiKind, boolean) stub}.</li>
 *    <li>For each top-frame get baseline version (compiling first if necessary) and deoptimize immediately.</li>
 * </ul>
 * One optimization applied is for each thread to perform the above two operations on itself just
 * {@linkplain VmOperation#doAtSafepointBeforeBlocking before} suspending. This is
 * analogous to each thread preparing its own reference map when being stopped
 * for a garbage collection. In the case of a thread deoptimizing its top-frame,
 * it continues execution until the next safepoint.
 * </li>
 * <li>
 * Find all references to invalidated target methods (i.e. at call sites) and revert them to trampolines
 * (this code patching is why we must be at a safepoint). That is, we are not guaranteed to be
 * patching call sites that have their offset aligned appropriately for an atomic update.
 * </li>
 * <li>Resume from safepoint.</li>
 */
public class Deoptimization extends VmOperation implements TargetMethod.Closure {

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
     * The set of invalidated target methods to be deoptimized.
     */
    private final ArrayList<TargetMethod> methods;

    /**
     * Creates an object to deoptimize a given set of methods.
     *
     * @param methods the set of methods to be deoptimized (must not contain duplicates). If {@code null}, then a
     *            set of currently active optimized methods are selected.
     */
    public Deoptimization(ArrayList<TargetMethod> methods) {
        super(methods.isEmpty() ? "DeoptimizeALot" : "Deoptimization", null, Mode.Safepoint);

        // Allow GC during deopt
        this.allowsNestedOperations = true;

        this.methods = methods;
    }

    /**
     * Performs deoptimization for the set of methods this object was constructed with.
     */
    public void go() {
        if (!methods.isEmpty()) {
            processMethods(methods);
        }
        submit();
    }

    private static void processMethods(ArrayList<TargetMethod> methods) {
        for (TargetMethod tm : methods) {
            if (TraceDeopt) {
                Log.println("DEOPT: processing " + tm);
            }

            // Atomically marks method as invalidated
            tm.invalidate(new InvalidationMarker(tm));

            // Find all references to invalidated target method(s) in dispatch tables (e.g. vtables, itables etc) and revert to trampoline references.
            // Concurrent patching ok here as it is atomic.
            patchDispatchTables(tm);
        }
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
                final Address trampoline = vm().stubs.virtualTrampoline(vtableIndex);
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
                                hub.setWord(i, vm().stubs.interfaceTrampoline(iIndex));
                                logPatchITable(classActor, iIndex);
                            }
                        }
                        return true;
                    }
                };
                c.doClass(method.holder());

                assert classHierarchyLock.isWriteLockedByCurrentThread() || VmThread.current().isVmOperationThread();
                method.holder().allSubclassesDo(c);
            }
        }
    }

    private static boolean isInvalidated(Object directCallee) {
        TargetMethod tm;
        if (directCallee instanceof TargetMethod) {
            tm = (TargetMethod) directCallee;
        } else {
            tm = TargetState.currentTargetMethod(((ClassMethodActor) directCallee).targetState, false);
        }
        return tm != null && tm.invalidated() != null;
    }

    @Override
    public boolean doTargetMethod(TargetMethod tm) {
        for (int i = 0; i < tm.numberOfDirectCalls(); i++) {
            Object directCallee = tm.directCallees()[i];
            if (directCallee != null && isInvalidated(directCallee)) {
                if (tm.resetDirectCall(i)) {
                    if (TraceDeopt) {
                        Log.println("DEOPT:   reset direct call " + i + " in " + tm + " to " + directCallee);
                    }
                }
            }
        }
        return true;
    }

    /**
     * Utility to select some active methods to deoptimize when {@link Deoptimization#DeoptimizeALot} is not 0.
     */
    class DeoptimizeALotSelector extends RawStackFrameVisitor {
        boolean doneSelecting;
        @Override
        public boolean visitFrame(Cursor current, Cursor callee) {
            if (!current.isTopFrame()) {
                TargetMethod tm = current.targetMethod();
                if (tm != null &&
                    tm.classMethodActor != null &&
                    !Code.bootCodeRegion().contains(tm.codeStart()) &&
                    !tm.isDeoptimizationTarget() &&
                    !tm.classMethodActor.isUnsafe() &&
                    tm.invalidated() == null &&
                    !methods.contains(tm)) {
                    methods.add(tm);
                }
            }
            return true;
        }
    }

    private DeoptimizeALotSelector deoptimizeALotSelector;

    @Override
    protected void doIt() {
        if (methods.isEmpty()) {
            deoptimizeALotSelector = new DeoptimizeALotSelector();
            doAllThreads();
            deoptimizeALotSelector.doneSelecting = true;

            if (methods.isEmpty()) {
                return;
            }

            if (TraceDeopt) {
                Log.println("DEOPT: DeoptimizeALot selected methods:");
                for (TargetMethod tm : methods) {
                    Log.println("DEOPT:   " + tm + " [" + tm.codeStart().to0xHexString() + "]");
                }
            }
            processMethods(methods);
        }

        // Process code cache
        if (TraceDeopt) {
            Log.println("DEOPT: processing code cache");
        }
        Code.bootCodeRegion().doAllTargetMethods(this);
        Code.getCodeManager().getRuntimeCodeRegion().doAllTargetMethods(this);

        doAllThreads();
        deoptimizeALotSelector = null;
    }

    @Override
    public void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
        if (deoptimizeALotSelector != null && !deoptimizeALotSelector.doneSelecting) {
            new VmStackFrameWalker(vmThread.tla()).inspect(ip, sp, fp, deoptimizeALotSelector);
        } else {
            Pointer tla = vmThread.tla();
            final boolean threadWasInNative = TRAP_INSTRUCTION_POINTER.load(tla).isZero();
            if (threadWasInNative || deoptimizeALotSelector != null) {
                // If a thread was in native or this is a DeoptimizeALot deopt, then
                // the thread did not do return address patching
                new Info(vmThread, ip, sp, fp, this);
            }
        }
    }

    @Override
    protected void doAtSafepointBeforeBlocking(Pointer trapFrame) {
        Safepoint.disable();

        TrapFrameAccess tfa = vm().trapFrameAccess;
        Pointer ip = tfa.getPC(trapFrame);
        Pointer sp = tfa.getSP(trapFrame);
        Pointer fp = tfa.getFP(trapFrame);

        Info info = new Info(VmThread.current(), ip, sp, fp, this);

        TargetMethod tm = info.tm;
        if (tm != null && methods.contains(tm)) {
            Pointer csa = tfa.getCalleeSaveArea(trapFrame);
            deoptimize(info, vm().registerConfigs.trapStub.getCalleeSaveLayout(), csa, null);
        }
    }

    /**
     * Constructs the deoptimized frames, unrolls them onto the stack and continues execution
     * in the top most deoptimized frame.
     *
     * @param info information about the optimized frame being deoptimized
     * @param csl the layout of the callee save area pointed to by {@code csa}
     * @param csa the address of the callee save area (may be zero)
     * @param returnValue the value being returned. This will be {@code null} if returning from a void method
     *        or deoptimization is taking place at an uncommon trap or during exception unwinding
     */
    private static void deoptimize(Info info, CiCalleeSaveLayout csl, Pointer csa, CiConstant returnValue) {
        // Note: all stack related terminology in this method and its comments is logical, not physical.
        // That is, stacks grow "upwards" towards the "top most" frame. One most systems, this
        // correlates with stacks physically growing down to lower addresses.

        assert Safepoint.isDisabled() : "safepoints must be disabled when deoptimizing";

        if (StackReferenceMapPreparer.VerifyRefMaps || TraceDeopt || DeoptimizeALot != 0) {
            StackReferenceMapPreparer.verifyReferenceMapsForThisThread();
            System.gc();
        }

        TargetMethod tm = info.tm;
        Pointer sp = info.sp;
        Pointer fp = info.fp;
        Pointer ip = info.ip;
        Throwable pendingException = VmThread.current().pendingException();

        int stopIndex = tm.findClosestStopIndex(ip);
        assert stopIndex >= 0 : "no stop index for " + tm + "+" + tm.posFor(ip);

        if (TraceDeopt) {
            Log.println("DEOPT: " + tm + ", stopIndex=" + stopIndex + ", pos=" + tm.stopPosition(stopIndex));
        }

        if (TraceDeopt) {
            logFrames(tm.debugInfoAt(stopIndex, null).frame(), "frame location values");
        }

        FrameAccess fa = new FrameAccess(csl, csa, sp, fp, info.callerSP, info.callerFP);
        CiDebugInfo debugInfo = tm.debugInfoAt(stopIndex, fa);
        CiFrame topFrame = debugInfo.frame();

        if (TraceDeopt) {
            logFrames(topFrame, "frame values");
        }

        // Construct the deoptimized frames for each frame in the debuf info
        final TopFrameContinuation topCont = new TopFrameContinuation();
        Continuation cont = topCont;
        for (CiFrame frame = topFrame; frame != null; frame = frame.caller()) {
            ClassMethodActor method = (ClassMethodActor) frame.method;
            TargetMethod compiledMethod = vmConfig().compilationScheme().synchronousCompile(method, DEOPTIMIZING.mask);
            if (cont == topCont) {
                topCont.tm = compiledMethod;
            }
            cont = compiledMethod.createDeoptimizedFrame(info, frame, cont);
        }

        // On AMD64, both T1X and C1X agree on the registers used for return values (i.e. RAX and XMM0).
        // As such there is no current need to reconstruct an adapter frame between the lowest
        // deoptimized frame and the frame of its caller.

        // Fix up the caller details for the bottom most deoptimized frame
        cont.setIP(info, info.callerIP);
        cont.setSP(info, CiConstant.forWord(info.callerSP.toLong()));
        cont.setFP(info, CiConstant.forWord(info.callerFP.toLong()));

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
                slots.set(i, CiConstant.forWord(slotAddr.toLong()));
            }
        }

        // Compute the physical frame details for the top most deoptimized frame
        sp = slotsAddrs.plus(topCont.sp * STACK_SLOT_SIZE);
        fp = slotsAddrs.plus(topCont.fp * STACK_SLOT_SIZE);

        // Redirect execution to the handler in the top most deoptimized frame if we were
        // unwinding to an invalidated frame
        if (pendingException != null) {
            Address handler = topCont.tm.throwAddressToCatchAddress(false, topCont.ip, pendingException.getClass());
            String exception = pendingException.getClass().getSimpleName();
            assert !handler.isZero() : "could not (re)find handler for " + exception +
                                       " thrown at " + tm + "+" + ip.to0xHexString();
            FrameInfo fi = new FrameInfo(sp, fp);
            topCont.tm.adjustFrameForHandler(fi);

            // Set the deopt continuation to the handler with the correctly adjusted frame
            info.ip = handler.asPointer();
            info.sp = fi.sp;
            info.fp = fi.fp;
            if (TraceDeopt) {
                Log.println("DEOPT: redirected deopt continuation to handler for " + exception + " at " + handler.to0xHexString());
            }
        } else {
            // Set the deopt continuation to the top most deoptimized frame
            info.ip = topCont.ip;
            info.sp = sp;
            info.fp = fp;
            info.returnValue = returnValue;
        }

        // Compute the stack space between the current frame (executing this method) and the
        // the top most deoptimized frame and use it to ensure that the unroll method
        // executes with enough stack space below it to unroll the deoptimized frames
        int used = info.sp.minus(VMRegister.getCpuStackPointer()).toInt() + tm.frameSize();
        int frameSize = Platform.target().alignFrameSize(Math.max(slotsSize - used, 0));
        Stubs.unroll(info, frameSize);
        FatalError.unexpected("should not reach here");
    }

    /**
     * Mechanism for a frame reconstruction to specify its execution state when it is resumed or returned to.
     */
    public static abstract class Continuation {

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

    /**
     * Mechanism for a caller frame reconstruction to specify its execution state when it is returned to.
     */
    public static class CallerContinuation extends Continuation {
        public final int callerFPIndex;
        public final int callerSPIndex;
        public final int returnAddressIndex;
        public final CiKind returnKind;

        public CallerContinuation(int callerFPIndex, int callerSPIndex, int returnAddressIndex, CiKind returnKind) {
            this.callerFPIndex = callerFPIndex;
            this.callerSPIndex = callerSPIndex;
            this.returnAddressIndex = returnAddressIndex;
            this.returnKind = returnKind;
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
        public void setIP(Info info, Pointer returnAddress) {
            if (returnAddressIndex >= 0) {
                info.slots.set(returnAddressIndex, CiConstant.forWord(returnAddress.toLong()));
            }
        }
    }

    /**
     * Mechanism for a top frame reconstruction to specify its execution state when it is resumed.
     */
    static class TopFrameContinuation extends Continuation {
        TargetMethod tm;
        int fp;
        int sp;
        Pointer ip;

        @Override
        public void setFP(Info slots, CiConstant fp) {
            this.fp = fp.asInt();
        }

        @Override
        public void setSP(Info slots, CiConstant sp) {
            this.sp = sp.asInt();
        }

        @Override
        public void setIP(Info slots, Pointer ip) {
            this.ip = ip;
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
            } else {
                sp.writeWord(0, Address.fromLong(c.asLong()));
            }
            sp = sp.minus(STACK_SLOT_SIZE);
        }

        if (StackReferenceMapPreparer.VerifyRefMaps || TraceDeopt || DeoptimizeALot != 0) {
            StackReferenceMapPreparer.verifyReferenceMaps(VmThread.current(), info.ip, info.sp, info.fp);
        }

        // Re-enable safepoints
        Safepoint.enable();


        // Checkstyle: stop
        if (info.returnValue == null) {
            Stubs.unwind(info.ip, info.sp, info.fp);
        } else {
            switch (info.returnValue.kind.stackKind()) {
                case Int:     Stubs.unwindInt(info.ip, info.sp, info.fp, info.returnValue.asInt());
                case Float:   Stubs.unwindFloat(info.ip, info.sp, info.fp, info.returnValue.asFloat());
                case Long:    Stubs.unwindLong(info.ip, info.sp, info.fp, info.returnValue.asLong());
                case Double:  Stubs.unwindDouble(info.ip, info.sp, info.fp, info.returnValue.asDouble());
                case Object:  Stubs.unwindObject(info.ip, info.sp, info.fp, info.returnValue.asObject());
                case Word:    Stubs.unwindWord(info.ip, info.sp, info.fp, Address.fromLong(info.returnValue.asLong()));
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
        deoptimizeOnReturn(ip, sp, fp, CiConstant.forInt(returnValue), csa);
    }

    /**
     * Called from the {@code float} {@linkplain Stubs#deoptStub(CiKind, boolean) deoptimization stub} for.
     */
    @NEVER_INLINE
    public static void deoptimizeFloat(Pointer ip, Pointer sp, Pointer fp, Pointer csa, float returnValue) {
        deoptimizeOnReturn(ip, sp, fp, CiConstant.forFloat(returnValue), csa);
    }

    /**
     * Called from the {@code long} {@linkplain Stubs#deoptStub(CiKind, boolean) deoptimization stub} for.
     */
    @NEVER_INLINE
    public static void deoptimizeLong(Pointer ip, Pointer sp, Pointer fp, Pointer csa, long returnValue) {
        deoptimizeOnReturn(ip, sp, fp, CiConstant.forLong(returnValue), csa);
    }

    /**
     * Called from the {@code double} {@linkplain Stubs#deoptStub(CiKind, boolean) deoptimization stub} for.
     */
    @NEVER_INLINE
    public static void deoptimizeDouble(Pointer ip, Pointer sp, Pointer fp, Pointer csa, double returnValue) {
        deoptimizeOnReturn(ip, sp, fp, CiConstant.forDouble(returnValue), csa);
    }

    /**
     * Called from the {@code Word} {@linkplain Stubs#deoptStub(CiKind, boolean) deoptimization stub} for.
     */
    @NEVER_INLINE
    public static void deoptimizeWord(Pointer ip, Pointer sp, Pointer fp, Pointer csa, Word returnValue) {
        deoptimizeOnReturn(ip, sp, fp, CiConstant.forWord(returnValue.asAddress().toLong()), csa);
    }

    /**
     * Called from the {@code Object} {@linkplain Stubs#deoptStub(CiKind, boolean) deoptimization stub} for.
     */
    @NEVER_INLINE
    public static void deoptimizeObject(Pointer ip, Pointer sp, Pointer fp, Pointer csa, Object returnValue) {
        deoptimizeOnReturn(ip, sp, fp, CiConstant.forObject(returnValue), csa);
    }

    /**
     * Called from the {@code void} {@linkplain Stubs#deoptStub(CiKind, boolean) deoptimization stub} for.
     */
    @NEVER_INLINE
    public static void deoptimizeVoid(Pointer ip, Pointer sp, Pointer fp, Pointer csa) {
        deoptimizeOnReturn(ip, sp, fp, null, csa);
    }

    /**
     * Encapsulates various state related to a deoptimization.
     */
    public static class Info extends RawStackFrameVisitor {
        final Deoptimization patchContext;

        /**
         * Method being deoptimized.
         */
        final TargetMethod tm;

        // The following are initially the details of the frame being deoptimized.
        // Just before unrolling, they are then modified to reflect the top
        // deoptimized frame.
        Pointer ip;
        Pointer sp;
        Pointer fp;

        /**
         * The address in the stack at which the {@linkplain #slots slots} of the
         * deoptimized frame(s) are unrolled.
         */
        Pointer slotsAddr;

        // The following are the details of the caller of the frame being deoptimized.
        TargetMethod callerTM;
        Pointer callerIP;
        Pointer callerSP;
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
         * @param patchContext if non-null, this context is used to {@linkplain Deoptimization#patchReturnAddress(Cursor, Cursor) patch}
         *        the return addresses of all callee (frames) of invalidated methods
         */
        public Info(VmThread thread, Pointer ip, Pointer sp, Pointer fp, Deoptimization patchContext) {
            this.tm = Code.codePointerToTargetMethod(ip);
            this.ip = ip;
            this.sp = sp;
            this.fp = fp;
            this.patchContext = patchContext;

            VmStackFrameWalker sfw = new VmStackFrameWalker(thread.tla());
            sfw.inspect(ip, sp, fp, this);

            assert stackIsWalkable(sfw, ip, sp, fp);
        }

        private boolean stackIsWalkable(StackFrameWalker sfw, Pointer ip, Pointer sp, Pointer fp) {
            sfw.inspect(ip, sp, fp, new RawStackFrameVisitor.Default());
            return true;
        }

        private ClassMethodActor lastCalleeMethod;

        @Override
        public boolean visitFrame(Cursor current, Cursor callee) {

            if (current.isTopFrame()) {
                // Ensure that the top frame of the walk is the method being deoptimized
                assert tm == current.targetMethod();
                assert ip == current.ip();
                assert sp == current.sp();
                assert fp == current.fp();
                return true;
            } else {
                TargetMethod calleeTM = callee.targetMethod();
                if (calleeTM != null && calleeTM.classMethodActor != null) {
                    lastCalleeMethod = calleeTM.classMethodActor;
                }
                TargetMethod tm = current.targetMethod();
                if (patchContext != null) {
                    if (tm != null && patchContext.methods.contains(tm)) {
                        patchReturnAddress(current, callee, lastCalleeMethod);
                    }
                }

                if (callee.isTopFrame()) {
                    callerTM = tm;
                    callerSP = current.sp();
                    callerFP = current.fp();

                    if (tm != null && tm.invalidated() != null) {
                        // The caller is (also) being deoptimized so we need to fetch the deopt stub
                        // that was patched into the callee's return address slot and ensure that it
                        // is returned to after deoptimization
                        callerIP = calleeTM.returnAddressPointer(callee).readWord(0).asPointer();
                        Pointer stub = vm().stubs.deoptStub(calleeTM.classMethodActor.resultKind().ciKind, calleeTM.is(CompilerStub)).codeStart();
                        assert stub == callerIP : tm + " stub=" + stub.to0xHexString() + " callerIP=" + callerIP.to0xHexString();
                    } else {
                        this.callerIP = current.ip();
                    }
                    if (patchContext == null) {
                        return false;
                    }
                }
            }
            return true;
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
    static void patchReturnAddress(Cursor caller, Cursor callee, ClassMethodActor calleeMethod) {
        assert calleeMethod != null;
        TargetMethod tm = caller.targetMethod();
        Stub stub = vm().stubs.deoptStub(calleeMethod.resultKind().ciKind, callee.targetMethod().is(CompilerStub));
        Pointer to = stub.codeStart().asPointer();
        Pointer save = caller.sp().plus(DEOPT_RETURN_ADDRESS_OFFSET);
        Pointer patch = callee.targetMethod().returnAddressPointer(callee);
        Address from = patch.readWord(0).asAddress();
        assert to != from;
        logPatchReturnAddress(tm, calleeMethod, stub, to, save, patch, from);
        patch.writeWord(0, to);
        save.writeWord(0, from);
    }

    /**
     * The fixed offset in a method's frame where the original return address is saved when the callee's
     * return address slot is {@linkplain #patchReturnAddress(Cursor, Cursor) patched} with the address
     * of a {@linkplain Stubs#deoptStub(CiKind, boolean) deoptimization stub}.
     */
    public static final int DEOPT_RETURN_ADDRESS_OFFSET = 0;

    /**
     * Deoptimizes a method that is being returned to via a {@linkplain Stubs#deoptStub(CiKind, boolean) deoptimization stub}.
     *
     * @param ip the address in the method returned to
     * @param sp the stack pointer of the frame executing the method
     * @param fp the frame pointer of the frame executing the method
     * @param returnValue the value being returned (will be {@code null} if returning from a void method)
     * @param csa the callee save area. This is non-null iff deoptimizing upon return from a compiler stub.
     */
    public static void deoptimizeOnReturn(Pointer ip, Pointer sp, Pointer fp, CiConstant returnValue, Pointer csa) {
        Safepoint.disable();

        Info info = new Info(VmThread.current(), ip, sp, fp, null);

        if (TraceDeopt) {
            Log.println("DEOPT: Deoptimizing " + info.tm);
            Throw.stackDump("DEOPT: Raw stack frames:");
            new Throwable("DEOPT: Bytecode stack frames:").printStackTrace(Log.out);
        }

        CiCalleeSaveLayout csl = csa.isZero() ? null : vm().registerConfigs.compilerStub.csl;
        deoptimize(info, csl, csa, returnValue);
    }

    /**
     * Deoptimizes at an {@link Stubs#genUncommonTrapStub() uncommon trap}.
     *
     * @param ip the address of the uncommon trap
     * @param sp the stack pointer of the frame executing the method
     * @param fp the frame pointer of the frame executing the method
     */
    public static void uncommonTrap(Pointer csa, Pointer ip, Pointer sp, Pointer fp) {
        Safepoint.disable();
        Info info = new Info(VmThread.current(), ip, sp, fp, null);

        if (TraceDeopt) {
            Log.println("DEOPT: Deoptimizing " + info.tm);
            Throw.stackDump("DEOPT: Raw stack frames:");
            new Throwable("DEOPT: Bytecode stack frames:").printStackTrace(Log.out);
        }

        CiCalleeSaveLayout csl = vm().registerConfigs.uncommonTrapStub.getCalleeSaveLayout();
        deoptimize(info, csl, csa, null);
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
    static void logPatchReturnAddress(TargetMethod tm, ClassMethodActor calleeMethod, Stub stub, Address to, Pointer save, Pointer patch, Address from) {
        if (TraceDeopt) {
            Log.println("DEOPT: patched return address @ " + patch.to0xHexString() + " of call to " + calleeMethod +
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
