/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.maxri;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.compiler.CallEntryPoint.*;
import static com.sun.max.vm.compiler.deopt.Deoptimization.*;
import static com.sun.max.vm.compiler.target.Stub.Type.*;
import static com.sun.max.vm.stack.StackReferenceMapPreparer.*;

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiCallingConvention.Type;
import com.sun.cri.ci.CiRegister.RegisterFlag;
import com.sun.cri.ci.CiTargetMethod.Call;
import com.sun.cri.ci.CiTargetMethod.CodeAnnotation;
import com.sun.cri.ci.CiTargetMethod.ExceptionHandler;
import com.sun.cri.ci.CiTargetMethod.Mark;
import com.sun.cri.ci.CiTargetMethod.Safepoint;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.deopt.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.amd64.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.Cursor;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * This class implements a {@link TargetMethod target method} for
 * the Maxine VM compiled by a compiler written against the
 * CRI project.
 */
public final class MaxTargetMethod extends TargetMethod implements Cloneable {

    /**
     * An array of pairs denoting the code positions protected by an exception handler.
     * A pair {@code {p,h}} at index {@code i} in this array specifies that code position
     * {@code h} is the handler for an exception of type {@code t} occurring at position
     * {@code p} where {@code t} is the element at index {@code i / 2} in {@link #exceptionClassActors}.
     */
    private int[] exceptionPositionsToCatchPositions;

    /**
     * @see #exceptionPositionsToCatchPositions
     */
    private ClassActor[] exceptionClassActors;

    /**
     * Debug info.
     */
    private DebugInfo debugInfo;

    private final CodeAnnotation[] annotations;

    @HOSTED_ONLY
    private CiTargetMethod bootstrappingCiTargetMethod;

    public MaxTargetMethod(ClassMethodActor classMethodActor, CiTargetMethod ciTargetMethod, boolean install) {
        super(classMethodActor, CallEntryPoint.OPTIMIZED_ENTRY_POINT);
        assert classMethodActor != null;
        List<CodeAnnotation> annotations = ciTargetMethod.annotations();
        this.annotations = annotations == null ? null : annotations.toArray(new CodeAnnotation[annotations.size()]);
        init(ciTargetMethod, install);
    }

    private void init(CiTargetMethod ciTargetMethod, boolean install) {

        if (isHosted()) {
            // Save the target method for later gathering of calls and duplication
            this.bootstrappingCiTargetMethod = ciTargetMethod;
        }

        for (Mark mark : ciTargetMethod.marks) {
            FatalError.unexpected("Unknown mark in code generated for " + this + ": " + mark);
        }

        if (classMethodActor != null) {
            int customStackAreaOffset = ciTargetMethod.customStackAreaOffset();
            if (customStackAreaOffset != DEOPT_RETURN_ADDRESS_OFFSET) {
                throw new InternalError("custom stack area offset should be " + DEOPT_RETURN_ADDRESS_OFFSET + ", not " + customStackAreaOffset);
            }
        }

        initCodeBuffer(ciTargetMethod, install);
        initFrameLayout(ciTargetMethod);
        CiDebugInfo[] debugInfos = initSafepoints(ciTargetMethod);
        initExceptionTable(ciTargetMethod);

        debugInfo = new DebugInfo(debugInfos, this);

        if (!isHosted()) {
            if (install) {
                linkDirectCalls();
            } else {
                // the displacement between a call site in the heap and a code cache location may not fit in the offset operand of a call
            }
        }
    }

    @Override
    public CodeAnnotation[] annotations() {
        return annotations;
    }

    /**
     * Gets the size (in bytes) of a bit map covering all the registers that may store references.
     * The bit position of a register in the bit map is the register's {@linkplain CiRegister#encoding encoding}.
     */
    @FOLD
    public static int regRefMapSize() {
        return ByteArrayBitMap.computeBitMapSize(target().arch.registerReferenceMapBitCount);
    }

    /**
     * @return the size of an activation frame for this target method in words.
     */
    private int frameWords() {
        return frameSize() / Word.size();
    }

    @Override
    public VMFrameLayout frameLayout() {
        if (platform().isa == ISA.AMD64) {
            return AMD64TargetMethodUtil.frameLayout(this);
        } else {
            throw FatalError.unimplemented();
        }
    }

    /**
     * @return the size (in bytes) of a reference map covering an activation frame for this target method.
     */
    public int frameRefMapSize() {
        return ByteArrayBitMap.computeBitMapSize(frameWords());
    }

    /**
     * @return the number of bytes in {@link #refMaps} corresponding to one safepoint.
     */
    public int totalRefMapSize() {
        return regRefMapSize() + frameRefMapSize();
    }

    public DebugInfo debugInfo() {
        return debugInfo;
    }

    private void initExceptionTable(CiTargetMethod ciTargetMethod) {
        if (ciTargetMethod.exceptionHandlers.size() > 0) {
            exceptionPositionsToCatchPositions = new int[ciTargetMethod.exceptionHandlers.size() * 2];
            exceptionClassActors = new ClassActor[ciTargetMethod.exceptionHandlers.size()];

            int z = 0;
            for (ExceptionHandler handler : ciTargetMethod.exceptionHandlers) {
                exceptionPositionsToCatchPositions[z * 2] = handler.pcOffset;
                exceptionPositionsToCatchPositions[z * 2 + 1] = handler.handlerPos;
                exceptionClassActors[z] = (handler.exceptionType == null) ? null : (ClassActor) handler.exceptionType;
                z++;
            }
        }
    }

    @Override
    public boolean isPatchableCallSite(Address callSite) {
        if (platform().isa == ISA.AMD64) {
            return AMD64TargetMethodUtil.isPatchableCallSite(callSite);
        } else {
            throw FatalError.unimplemented();
        }
    }

    @Override
    public Address fixupCallSite(int callOffset, Address callEntryPoint) {
        if (platform().isa == ISA.AMD64) {
            return AMD64TargetMethodUtil.fixupCall32Site(this, callOffset, callEntryPoint);
        } else {
            throw FatalError.unimplemented();
        }
    }

    @Override
    public Address patchCallSite(int callOffset, Address callEntryPoint) {
        if (platform().isa == ISA.AMD64) {
            return AMD64TargetMethodUtil.mtSafePatchCallDisplacement(this, codeStart().plus(callOffset), callEntryPoint.asAddress());
        } else {
            throw FatalError.unimplemented();
        }
    }

    @Override
    public void redirectTo(TargetMethod tm) {
        if (platform().isa == ISA.AMD64) {
            AMD64TargetMethodUtil.patchWithJump(this, OPTIMIZED_ENTRY_POINT.offset(), OPTIMIZED_ENTRY_POINT.in(tm));
            if (VMConfiguration.vmConfig().needsAdapters()) {
                AMD64TargetMethodUtil.patchWithJump(this, BASELINE_ENTRY_POINT.offset(), BASELINE_ENTRY_POINT.in(tm));
            }
        } else {
            throw FatalError.unimplemented();
        }
    }

    @Override
    public Address throwAddressToCatchAddress(boolean isTopFrame, Address throwAddress, Class<? extends Throwable> throwableClass) {
        final int exceptionPos = throwAddress.minus(codeStart).toInt();
        int count = getExceptionHandlerCount();
        for (int i = 0; i < count; i++) {
            int codePos = getExceptionPosAt(i);
            int catchPos = getCatchPosAt(i);
            ClassActor catchType = getCatchTypeAt(i);

            if (codePos == exceptionPos && checkType(throwableClass, catchType)) {
                return codeStart.plus(catchPos);
            }
        }
        return Address.zero();
    }

    private boolean checkType(Class<? extends Throwable> throwableClass, ClassActor catchType) {
        return catchType == null || catchType.isAssignableFrom(ClassActor.fromJava(throwableClass));
    }

    /**
     * Gets the exception code position of an entry in the exception handler table.
     *
     * @param i the index of the requested exception handler table entry
     * @return the exception code position of element {@code i} in the exception handler table
     */
    private int getExceptionPosAt(int i) {
        return exceptionPositionsToCatchPositions[i * 2];
    }

    /**
     * Gets the exception handler code position of an entry in the exception handler table.
     *
     * @param i the index of the requested exception handler table entry
     * @return the exception handler position of element {@code i} in the exception handler table
     */
    private int getCatchPosAt(int i) {
        return exceptionPositionsToCatchPositions[i * 2 + 1];
    }

    /**
     * Gets the exception type of an entry in the exception handler table.
     *
     * @param i the index of the requested exception handler table entry
     * @return the exception type of element {@code i} in the exception handler table
     */
    private ClassActor getCatchTypeAt(int i) {
        return exceptionClassActors[i];
    }

    /**
     * Gets the number of entries in the exception handler table.
     */
    private int getExceptionHandlerCount() {
        return exceptionClassActors == null ? 0 : exceptionClassActors.length;
    }

    @HOSTED_ONLY
    private void gatherInlinedMethods(Safepoint safepoint, Set<MethodActor> inlinedMethods) {
        CiDebugInfo debugInfo = safepoint.debugInfo;
        if (debugInfo != null) {
            for (CiCodePos pos = debugInfo.codePos; pos != null; pos = pos.caller) {
                inlinedMethods.add((MethodActor) pos.method);
            }
        }
    }

    @Override
    @HOSTED_ONLY
    public void gatherCalls(Set<MethodActor> directCalls, Set<MethodActor> virtualCalls, Set<MethodActor> interfaceCalls, Set<MethodActor> inlinedMethods) {
        // first gather methods in the directCallees array
        if (directCallees != null) {
            for (Object o : directCallees) {
                if (o instanceof MethodActor) {
                    directCalls.add((MethodActor) o);
                }
            }
        }

        // iterate over direct calls
        for (Safepoint safepoint : bootstrappingCiTargetMethod.safepoints) {
            if (safepoint instanceof Call) {
                Call call = (Call) safepoint;
                MethodActor callee = CallTarget.asMethodActor(call.target);
                if (callee != null) {
                    if (call.direct) {
                        directCalls.add(callee);
                    } else {
                        if (callee.holder().isInterface()) {
                            interfaceCalls.add(callee);
                        } else {
                            virtualCalls.add(callee);
                        }
                    }
                }
            }
            gatherInlinedMethods(safepoint, inlinedMethods);
        }
    }

    @HOSTED_ONLY
    private ClassMethodActor getClassMethodActor(CiRuntimeCall runtimeCall, RiMethod method) {
        if (method != null) {
            return (ClassMethodActor) method;
        }

        assert runtimeCall != null : "A call can either be a call to a method or a runtime call";
        return MaxRuntimeCalls.getClassMethodActor(runtimeCall);
    }

    /**
     * Prepares the reference map for this frame.
     *
     * @param current the current frame
     * @param callee the callee frame
     * @param preparer the reference map preparer
     */
    @Override
    public void prepareReferenceMap(Cursor current, Cursor callee, StackReferenceMapPreparer preparer) {
        CiCalleeSaveLayout csl = callee.csl();
        Pointer csa = callee.csa();
        TargetMethod calleeTM = callee.targetMethod();
        if (calleeTM != null) {
            Stub.Type st = calleeTM.stubType();
            if (st == StaticTrampoline || st == VirtualTrampoline || st == InterfaceTrampoline) {
                prepareTrampolineRefMap(current, callee, preparer);
            } else if (calleeTM.is(TrapStub) && Trap.Number.isStackOverflow(csa)) {
                // a method can never catch stack overflow for itself so there
                // is no need to scan the references in the trapped method
                return;
            }
        }

        int safepointIndex = findSafepointIndex(current.ip());
        if (safepointIndex < 0) {
            // this is very bad.
            throw FatalError.unexpected("could not find safepoint index");
        }

        int frameRefMapSize = frameRefMapSize();
        if (!csa.isZero()) {
            // the callee contains register state from this frame;
            // use register reference maps in this method to fill in the map for the callee
            Pointer slotPointer = csa;
            int byteIndex = debugInfo.regRefMapStart(safepointIndex);
            preparer.tracePrepareReferenceMap(this, safepointIndex, slotPointer, "registers");

            // Need to translate from register numbers (as stored in the reg ref maps) to frame slots.
            for (int i = 0; i < regRefMapSize(); i++) {
                int b = debugInfo.data[byteIndex] & 0xff;
                int reg = i * 8;
                while (b != 0) {
                    if ((b & 1) != 0) {
                        int offset = csl.offsetOf(reg);
                        if (traceStackRootScanning()) {
                            Log.print("    register: ");
                            Log.println(csl.registers[reg].name);
                        }
                        preparer.setReferenceMapBits(callee, slotPointer.plus(offset), 1, 1);
                    }
                    reg++;
                    b = b >>> 1;
                }
                byteIndex++;
            }
        }

        // prepare the map for this stack frame
        Pointer slotPointer = current.sp();
        preparer.tracePrepareReferenceMap(this, safepointIndex, slotPointer, "frame");
        int byteIndex = debugInfo.frameRefMapStart(safepointIndex);
        for (int i = 0; i < frameRefMapSize; i++) {
            preparer.setReferenceMapBits(current, slotPointer, debugInfo.data[byteIndex] & 0xff, Bytes.WIDTH);
            slotPointer = slotPointer.plusWords(Bytes.WIDTH);
            byteIndex++;
        }
    }

    /**
     * Prepares the reference map for the frame of a call to a trampoline from an OPT compiled method.
     *
     * An opto-compiled caller may pass some arguments in registers.  The trampoline is polymorphic, i.e. it does not have any
     * helpful maps regarding the actual callee.  It does store all potential parameter registers on its stack, though,
     * and recovers them before returning.  We mark those that contain references.
     *
     * @param current
     * @param callee
     * @param preparer
     */
    public static void prepareTrampolineRefMap(Cursor current, Cursor callee, StackReferenceMapPreparer preparer) {
        RiRegisterConfig registerConfig = vm().registerConfigs.trampoline;
        TargetMethod trampoline = callee.targetMethod();
        ClassMethodActor calledMethod = null;
        TargetMethod targetMethod = current.targetMethod();

        CiCalleeSaveLayout csl = callee.csl();
        Pointer csa = callee.csa();
        FatalError.check(csl != null && !csa.isZero(), "trampoline must have callee save area");
        CiRegister[] regs = registerConfig.getCallingConventionRegisters(Type.JavaCall, RegisterFlag.CPU);

        // figure out what method the caller is trying to call
        if (trampoline.is(StaticTrampoline)) {
            int dcIndex = 0;
            Safepoints safepoints = targetMethod.safepoints();
            int safepointPos = targetMethod.posFor(current.ip());
            for (int safepointIndex = safepoints.nextDirectCall(0); safepointIndex >= 0; safepointIndex = safepoints.nextDirectCall(safepointIndex + 1)) {
                if (safepoints.posAt(safepointIndex) == safepointPos) {
                    calledMethod = (ClassMethodActor) targetMethod.directCallees()[dcIndex];
                    break;
                }
                dcIndex++;
            }
            if (calledMethod == null) {
                // this is very bad.
                throw FatalError.unexpected("could not find stop index");
            }
        } else {
            // this is a virtual or interface call; figure out the receiver method based on the
            // virtual or interface index
            Object receiver = csa.plus(csl.offsetOf(regs[0])).getReference().toJava();
            ClassActor classActor = ObjectAccess.readClassActor(receiver);
            // The virtual dispatch trampoline stubs put the virtual dispatch index into the
            // scratch register and then saves it to the stack.
            int index = vm().stubs.readVirtualDispatchIndexFromTrampolineFrame(csa);
            if (trampoline.is(VirtualTrampoline)) {
                calledMethod = classActor.getVirtualMethodActorByVTableIndex(index);
            } else {
                assert trampoline.is(InterfaceTrampoline);
                calledMethod = classActor.getVirtualMethodActorByIIndex(index);
            }
        }

        int regIndex = 0;
        if (!calledMethod.isStatic()) {
            // set a bit for the receiver object
            int offset = csl.offsetOf(regs[regIndex++]);
            preparer.setReferenceMapBits(current, csa.plus(offset), 1, 1);
        }

        SignatureDescriptor sig = calledMethod.descriptor();
        for (int i = 0; i < sig.numberOfParameters() && regIndex < regs.length; ++i) {
            TypeDescriptor arg = sig.parameterDescriptorAt(i);
            CiRegister reg = regs[regIndex];
            Kind kind = arg.toKind();
            if (kind.isReference) {
                // set a bit for this parameter
                int offset = csl.offsetOf(reg);
                preparer.setReferenceMapBits(current, csa.plus(offset), 1, 1);
            }
            if (kind != Kind.FLOAT && kind != Kind.DOUBLE) {
                // Only iterating over the integral arg registers
                regIndex++;
            }
        }
    }

    /**
     * Attempt to catch an exception that has been thrown with this method on the call stack.
     * @param current the current stack frame
     * @param callee the callee stack frame
     * @param throwable the exception being thrown
     */
    @Override
    public void catchException(Cursor current, Cursor callee, Throwable throwable) {
        Pointer ip = current.ip();
        Pointer sp = current.sp();
        Pointer fp = current.fp();
        Address catchAddress = throwAddressToCatchAddress(current.isTopFrame(), ip, throwable.getClass());
        if (!catchAddress.isZero()) {
            if (StackFrameWalker.TraceStackWalk) {
                Log.print("StackFrameWalk: Handler position for exception at position ");
                Log.print(ip.minus(codeStart()).toInt());
                Log.print(" is ");
                Log.println(catchAddress.minus(codeStart()).toInt());
            }

            if (invalidated() != null) {
                // Instead of unwinding to the invalidated method, execution is redirected to the void deopt stub.
                // And the original return address (i.e. current.ip()) is saved in the DEOPT_RETURN_ADDRESS_OFFSET
                // slot instead of the handler address. This is required so that the debug info associated with
                // the call site is used during deopt. This debug info matches the state on entry to the handler
                // except that the stack is empty (the exception object is explicitly retrieved and pushed by
                // the handler in the deoptimized code).
                current.sp().writeWord(DEOPT_RETURN_ADDRESS_OFFSET, ip);
                Stub stub = vm().stubs.deoptStub(CiKind.Void, false);
                Pointer deoptStub = stub.codeStart().asPointer();
                if (Deoptimization.TraceDeopt) {
                    Log.println("DEOPT: changed exception handler address " + catchAddress.to0xHexString() + " in " + this + " to redirect to deopt stub " +
                                    deoptStub.to0xHexString() + " [sp=" + sp.to0xHexString() + ", fp=" + fp.to0xHexString() + "]");
                }
                catchAddress = deoptStub;
            }

            TargetMethod calleeMethod = callee.targetMethod();
            // Reset the stack walker
            current.stackFrameWalker().reset();

            // Store the exception for the handler
            VmThread.current().storeExceptionForHandler(throwable, this, posFor(catchAddress));

            if (calleeMethod != null && calleeMethod.registerRestoreEpilogueOffset() != -1) {
                unwindToCalleeEpilogue(catchAddress, sp, calleeMethod);
            } else {
                Stubs.unwind(catchAddress, sp, fp);
            }
            throw ProgramError.unexpected("Should not reach here, unwind must jump to the exception handler!");
        }
    }

    @NEVER_INLINE
    public static void unwindToCalleeEpilogue(Address catchAddress, Pointer stackPointer, TargetMethod lastJavaCallee) {
        // Overwrite return address of callee with catch address
        final Pointer returnAddressPointer = stackPointer.minus(Word.size());
        returnAddressPointer.setWord(catchAddress);

        Address epilogueAddress = lastJavaCallee.codeStart().plus(lastJavaCallee.registerRestoreEpilogueOffset());

        final Pointer calleeStackPointer = stackPointer.minus(Word.size()).minus(lastJavaCallee.frameSize());
        Stubs.unwind(epilogueAddress, calleeStackPointer, Pointer.zero());
    }

    /**
     * Accept a visitor for this frame.
     * @param current the current stack frame
     * @param visitor the visitor
     * @return {@code true} if the stack walker should continue walking, {@code false} if the visitor is finished visiting
     */
    @Override
    public boolean acceptStackFrameVisitor(Cursor current, StackFrameVisitor visitor) {
        if (platform().isa == ISA.AMD64) {
            return AMD64TargetMethodUtil.acceptStackFrameVisitor(current, visitor);
        }
        throw FatalError.unimplemented();
    }

    /**
     * Advances the cursor to the caller's frame.
     * @param current the current frame
     */
    @Override
    public void advance(Cursor current) {
        if (platform().isa == ISA.AMD64) {
            CiCalleeSaveLayout csl = calleeSaveLayout();
            Pointer csa = Pointer.zero();
            if (csl != null) {
                // See FrameMap
                csa = current.sp().plus(frameSize() - csl.size);
            }
            AMD64TargetMethodUtil.advance(current, csl, csa);
        } else {
            throw FatalError.unimplemented();
        }
    }

    @Override
    public Pointer returnAddressPointer(Cursor frame) {
        if (platform().isa == ISA.AMD64) {
            return AMD64TargetMethodUtil.returnAddressPointer(frame);
        } else {
            throw FatalError.unimplemented();
        }
    }

    @Override
    public int forEachCodePos(CodePosClosure cpc, Pointer ip) {
        int index = findSafepointIndex(ip);
        if (index < 0) {
            return 0;
        }

        return debugInfo.forEachCodePos(cpc, index);
    }

    @Override
    public CiDebugInfo debugInfoAt(int safepointIndex, FrameAccess fa) {
        return debugInfo.infoAt(safepointIndex, fa, true);
    }
}
