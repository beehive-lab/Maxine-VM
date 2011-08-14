/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler.target;

import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.compiler.target.Stops.*;

import java.io.*;
import java.util.*;

import com.oracle.max.asm.target.amd64.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiTargetMethod.CodeAnnotation;
import com.sun.cri.ci.CiTargetMethod.DataPatch;
import com.sun.max.annotate.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.deopt.Deoptimization.Continuation;
import com.sun.max.vm.compiler.deopt.Deoptimization.Info;
import com.sun.max.vm.compiler.deopt.*;
import com.sun.max.vm.compiler.target.TargetBundleLayout.ArrayField;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.profile.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.Cursor;

/**
 * Represents machine code produced and managed by the VM. The machine code
 * is either produced by a compiler or manually assembled. Examples of the
 * latter are {@linkplain Stub stubs} and {@linkplain Adapter adapters}.
 */
public abstract class TargetMethod extends MemoryRegion {

    /**
     * Implemented by a client wanting to do something to a target method.
     */
    public static interface Closure {
        /**
         * Processes a given target method.
         *
         * @param targetMethod the class to process
         * @return {@code false} if target method processing (e.g. iteration) by the caller should be stopped
         */
        boolean doTargetMethod(TargetMethod targetMethod);
    }

    /**
     * The (bytecode) method from which this target method was compiled.
     * This will be {@code null} iff this target method is a {@link Stub} or
     * and {@link Adapter}.
     */
    @INSPECTED
    public final ClassMethodActor classMethodActor;

    protected Stops stops = Stops.NO_STOPS;

    /**
     * @see #directCallees()
     */
    protected Object[] directCallees;

    protected byte[] scalarLiterals;

    @INSPECTED(deepCopied = false)
    protected Object[] referenceLiterals;

    @INSPECTED
    protected byte[] code;

    @INSPECTED
    protected Pointer codeStart = Pointer.zero();

    /**
     * If non-null, then this method has been invalidated.
     *
     * @see #invalidated()
     */
    volatile private InvalidationMarker invalidated;

    /**
     * The frame size (in bytes) of an activation of this target method. This does not
     * include the space occupied by a return address (if the arch uses one).
     */
    private int frameSize = -1;

    /**
     * The offset of the code restoring the saved registers and returning to the caller.
     * When unwinding the stack to an exception handler in the caller of such a method, control must be returned
     * to this address (after the return address has been patched with the address of the exception handler).
     * A value of {@code -1} means this is not a callee-save target method.
     */
    private int registerRestoreEpilogueOffset = -1;

    public TargetMethod(String description, CallEntryPoint callEntryPoint) {
        assert this instanceof Stub || this instanceof Adapter;
        this.classMethodActor = null;
        this.callEntryPoint = callEntryPoint;
        setRegionName(description);
    }

    public TargetMethod(ClassMethodActor classMethodActor, CallEntryPoint callEntryPoint) {
        assert classMethodActor != null;
        assert !(this instanceof Stub);
        assert !(this instanceof Adapter);
        this.classMethodActor = classMethodActor;
        this.callEntryPoint = callEntryPoint;
        setRegionName(classMethodActor.name.toString());
    }

    public int registerRestoreEpilogueOffset() {
        return registerRestoreEpilogueOffset;
    }

    protected void setRegisterRestoreEpilogueOffset(int x) {
        registerRestoreEpilogueOffset = x;
    }

    public final ClassMethodActor classMethodActor() {
        return classMethodActor;
    }

    /**
     * Marks this method as invalidated.
     *
     * @see #invalidated()
     */
    public void invalidate(InvalidationMarker marker) {
        assert marker != null;
        assert this.invalidated == null : "cannot invalidate target method more than once: " + this;
        this.invalidated = marker;
    }

    /**
     * Determines if this method has been invalidated.
     * Invalidated target methods are never used when linking an unlinked
     * call site or when resolving a dispatch table entry (e.g. vtable or itable).
     * In addition, threads currently executing an invalidated method are required
     * to apply deoptimization as soon as the execution context re-enters
     * (via returning or stack unwinding during exception throwing) the method.
     *
     * @return a non-null {@link InvalidationMarker} object iff this method has been invalidated
     */
    public InvalidationMarker invalidated() {
        return invalidated;
    }

    /**
     * Gets the bytecode locations for the inlining chain rooted at a given instruction pointer. The first bytecode
     * location in the returned sequence is the one at the closest position less or equal to the position denoted by
     * {@code ip}.
     *
     * @param targetMethod the target method to process
     * @param ip a pointer to an instruction within this method
     * @param ipIsReturnAddress
     * @return the bytecode locations for the inlining chain rooted at {@code ip}. This will be null if
     *         no bytecode location can be determined for {@code ip}.
     */
    public static CiCodePos getCodePos(TargetMethod targetMethod, Pointer ip, boolean ipIsReturnAddress) {
        class Caller {
            final ClassMethodActor method;
            final int bci;
            final Caller next;
            Caller(ClassMethodActor method, int bci, Caller next) {
                this.method = method;
                this.bci = bci;
                this.next = next;
            }
            CiCodePos toCiCodePos(CiCodePos caller) {
                CiCodePos pos = new CiCodePos(caller, method, bci);
                if (next != null) {
                    return next.toCiCodePos(pos);
                }
                return pos;
            }
        }
        final Caller[] head = {null};
        CodePosClosure cpc = new CodePosClosure() {
            public boolean doCodePos(ClassMethodActor method, int bci) {
                head[0] = new Caller(method, bci, head[0]);
                return true;
            }
        };
        targetMethod.forEachCodePos(cpc, ip, ipIsReturnAddress);
        if (head[0] == null) {
            return null;
        }
        return head[0].toCiCodePos(null);
    }

    /**
     * Iterates over the bytecode locations for the inlining chain rooted at a given instruction pointer.
     *
     * @param cpc a closure called for each bytecode location in the inlining chain rooted at {@code ip} (inner most
     *            callee first)
     * @param ip a pointer to an instruction within this method
     * @param ipIsReturnAddress
     * @return the number of bytecode locations iterated over (i.e. the number of times
     *         {@link CodePosClosure#doCodePos(ClassMethodActor, int)} was called
     */
    public int forEachCodePos(CodePosClosure cpc, Pointer ip, boolean ipIsReturnAddress) {
        return 0;
    }

    /**
     * Provides access to live frame values.
     */
    public static class FrameAccess {
        /**
         * Layout of the callee save area.
         */
        public final CiCalleeSaveLayout csl;

        /**
         * Callee save area.
         */
        public final Pointer csa;

        /**
         * Stack pointer.
         */
        public final Pointer sp;

        /**
         * Frame pointer.
         */
        public final Pointer fp;

        /**
         * Stack pointer.
         */
        public final Pointer callerSP;

        /**
         * Frame pointer.
         */
        public final Pointer callerFP;

        public FrameAccess(CiCalleeSaveLayout csl, Pointer csa, Pointer sp, Pointer fp, Pointer callerSP, Pointer callerFP) {
            this.csl = csl;
            this.csa = csa;
            this.sp = sp;
            this.fp = fp;
            this.callerSP = callerSP;
            this.callerFP = callerFP;
        }
    }

    /**
     * Gets the debug info available for a given stop. If {@code fa != null}, then the {@linkplain CiFrame#values values} in the
     * returned object are {@link CiConstant}s wrapping values from the thread's stack denoted by {@code fa}. Otherwise,
     * they are {@link CiStackSlot}, {@link CiRegister} and {@link CiConstant} values describing how to extract values
     * from a live frame.
     *
     * @param stopIndex an index of a stop within this method
     * @param fa access to a live frame (may be {@code null})
     * @return the debug ino at the denoted stop of {@code null} if none available
     */
    public CiDebugInfo debugInfoAt(int stopIndex, FrameAccess fa) {
        return null;
    }

    public final int numberOfDirectCalls() {
        return (directCallees == null) ? 0 : directCallees.length;
    }

    /**
     * Gets an array containing the direct callees of this method.
     * The array can contain instances of {@link ClassMethodActor} and {@link TargetMethod} side by side.
     * In case a callee is an actual method, it is represented by a {@link ClassMethodActor}.
     * In case it is a stub or the adapter, a {@link TargetMethod} is used.
     *
     * @return entities referenced by direct call instructions, matched to the stop positions array above by array index
     */
    public final Object[] directCallees() {
        return directCallees;
    }

    /**
     * Gets the call entry point to be used for a direct call from this target method. By default, the
     * {@linkplain #callEntryPoint call entry point} of this target method will be used.
     *
     * @param stopIndex the index of the call in {@link #stops() stops}
     */
    protected CallEntryPoint callEntryPointForDirectCall(int stopIndex) {
        return callEntryPoint;
    }

    /**
     * @return non-object data referenced by the machine code
     */
    public final byte[] scalarLiterals() {
        return scalarLiterals;
    }

    public final int numberOfScalarLiteralBytes() {
        return (scalarLiterals == null) ? 0 : scalarLiterals.length;
    }

    /**
     * @return object references referenced by the machine code
     */
    public final Object[] referenceLiterals() {
        return referenceLiterals;
    }

    public final int numberOfReferenceLiterals() {
        return (referenceLiterals == null) ? 0 : referenceLiterals.length;
    }

    /**
     * Gets the byte array containing the target-specific machine code of this target method.
     */
    public final byte[] code() {
        return code;
    }

    public final int codeLength() {
        return (code == null) ? 0 : code.length;
    }

    /**
     * Gets the address of the first instruction in this target method's {@linkplain #code() compiled code array}.
     * <p>
     * Needs {@linkplain DataPrototype#assignRelocationFlags() relocation}.
     */
    public final Pointer codeStart() {
        return codeStart;
    }

    @HOSTED_ONLY
    public final void setCodeStart(Pointer codeStart) {
        this.codeStart = codeStart;
    }

    /**
     * Gets the size (in bytes) of the stack frame used for the local variables in
     * the method represented by this object. The stack pointer is decremented
     * by this amount when entering a method and is correspondingly incremented
     * by this amount when exiting a method.
     */
    public final int frameSize() {
        assert frameSize != -1 : "frame size not yet initialized";
        return frameSize;
    }

    /**
     * The entry point used for <i>standard</i> calls in this target method to JVM compiled/interpreted code.
     * Non-standard calls are those to external native code and calls to the runtime inserted by the
     * compiler. The former type of calls directly use the native address supplied by the {@linkplain DynamicLinker linker}
     * and the latter always uses {@link CallEntryPoint#OPTIMIZED_ENTRY_POINT}.
     */
    @INSPECTED
    public final CallEntryPoint callEntryPoint;

    public static final Object[] NO_DIRECT_CALLEES = {};

    /**
     * Assigns the arrays co-located in a {@linkplain CodeRegion code region} containing the machine code and related data.
     *
     * @param code the code
     * @param codeStart the address of the first element of {@code code}
     * @param scalarLiterals the scalar data referenced from {@code code}
     * @param referenceLiterals the reference data referenced from {@code code}
     */
    public final void setCodeArrays(byte[] code, Pointer codeStart, byte[] scalarLiterals, Object[] referenceLiterals) {
        this.scalarLiterals = scalarLiterals;
        this.referenceLiterals = referenceLiterals;
        this.code = code;
        this.codeStart = codeStart;
    }

    protected final void setStops(Stops stops, Object[] directCallees) {
        this.stops = stops;
        this.directCallees = directCallees;
        assert stops.numberOfDirectCalls() == directCallees.length : stops.numberOfDirectCalls() + " !=  " + directCallees.length;
    }

    protected final void setFrameSize(int frameSize) {
        assert frameSize != -1 : "invalid frame size!";
        this.frameSize = frameSize;
    }

    protected void initCodeBuffer(CiTargetMethod ciTargetMethod, boolean install) {
        // Create the arrays for the scalar and the object reference literals
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        List<Object> objectReferences = new ArrayList<Object>();
        int[] relativeDataPos = serializeLiterals(ciTargetMethod, output, objectReferences);
        byte[] scalarLiterals = output.toByteArray();
        Object[] referenceLiterals = objectReferences.toArray();

        // Allocate and set the code and data buffer
        final TargetBundleLayout targetBundleLayout = new TargetBundleLayout(scalarLiterals.length, referenceLiterals.length, ciTargetMethod.targetCodeSize());
        if (install) {
            Code.allocate(targetBundleLayout, this);
        } else {
            Code.allocateInHeap(targetBundleLayout, this);
        }
        this.setData(scalarLiterals, referenceLiterals, ciTargetMethod.targetCode());

        // Patch relative instructions in the code buffer
        patchInstructions(targetBundleLayout, ciTargetMethod, relativeDataPos);
    }

    private int[] serializeLiterals(CiTargetMethod ciTargetMethod, ByteArrayOutputStream output, List<Object> objectReferences) {
        Endianness endianness = platform().endianness();
        int[] relativeDataPos = new int[ciTargetMethod.dataReferences.size()];
        int z = 0;
        int currentPos = 0;
        for (DataPatch site : ciTargetMethod.dataReferences) {
            final CiConstant data = site.constant;
            relativeDataPos[z] = currentPos;

            try {
                switch (data.kind) {
                    case Double:
                        endianness.writeLong(output, Double.doubleToLongBits(data.asDouble()));
                        currentPos += Long.SIZE / Byte.SIZE;
                        break;

                    case Float:
                        endianness.writeInt(output, Float.floatToIntBits(data.asFloat()));
                        currentPos += Integer.SIZE / Byte.SIZE;
                        break;

                    case Int:
                        endianness.writeInt(output, data.asInt());
                        currentPos += Integer.SIZE / Byte.SIZE;
                        break;

                    case Long:
                        endianness.writeLong(output, data.asLong());
                        currentPos += Long.SIZE / Byte.SIZE;
                        break;

                    case Object:
                        objectReferences.add(data.asObject());
                        break;

                    default:
                        throw new IllegalArgumentException("Unknown constant type!");
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Align on double word boundary
            while (currentPos % (Platform.platform().wordWidth().numberOfBytes * 2) != 0) {
                output.write(0);
                currentPos++;
            }

            z++;
        }

        return relativeDataPos;
    }

    private void patchInstructions(TargetBundleLayout targetBundleLayout, CiTargetMethod ciTargetMethod, int[] relativeDataPositions) {
        Offset codeStart = targetBundleLayout.cellOffset(TargetBundleLayout.ArrayField.code);

        Offset dataDiff = Offset.zero();
        if (this.scalarLiterals != null) {
            Offset dataStart = targetBundleLayout.cellOffset(TargetBundleLayout.ArrayField.scalarLiterals);
            dataDiff = dataStart.minus(codeStart).asOffset();
        }

        Offset referenceDiff = Offset.zero();
        if (this.referenceLiterals() != null) {
            Offset referenceStart = targetBundleLayout.cellOffset(TargetBundleLayout.ArrayField.referenceLiterals);
            referenceDiff = referenceStart.minus(codeStart).asOffset();
        }

        int objectReferenceIndex = 0;
        int refSize = Platform.platform().wordWidth().numberOfBytes;

        int z = 0;
        for (DataPatch site : ciTargetMethod.dataReferences) {

            switch (site.constant.kind) {

                case Double: // fall through
                case Float: // fall through
                case Int: // fall through
                case Long:
                    patchRelativeInstruction(site.pcOffset, dataDiff.plus(relativeDataPositions[z] - site.pcOffset).toInt());
                    break;

                case Object:
                    patchRelativeInstruction(site.pcOffset, referenceDiff.plus(objectReferenceIndex * refSize - site.pcOffset).toInt());
                    objectReferenceIndex++;
                    break;

                default:
                    throw new IllegalArgumentException("Unknown constant type!");
            }

            z++;
        }
    }

    private void patchRelativeInstruction(int codePos, int displacement) {
        if (platform().isa == ISA.AMD64) {
            X86InstructionDecoder.patchRelativeInstruction(code(), codePos, displacement);
        } else {
            throw FatalError.unimplemented();
        }
    }

    protected void initFrameLayout(CiTargetMethod ciTargetMethod) {
        this.setFrameSize(ciTargetMethod.frameSize());
        this.setRegisterRestoreEpilogueOffset(ciTargetMethod.registerRestoreEpilogueOffset());
    }

    protected CiDebugInfo[] initStops(CiTargetMethod ciTargetMethod) {
        Adapter adapter = null;
        int adapterCount = 0;

        AdapterGenerator generator = AdapterGenerator.forCallee(this);
        if (generator != null) {
            adapter = generator.make(classMethodActor);
            if (adapter != null) {
                adapterCount = 1;
            }
        }

        int numberOfIndirectCalls = ciTargetMethod.indirectCalls.size();
        int numberOfSafepoints = ciTargetMethod.safepoints.size();
        int total = ciTargetMethod.directCalls.size() + numberOfIndirectCalls + numberOfSafepoints + adapterCount;

        int index = 0;
        int[] stops = new int[total];
        Object[] directCallees = new Object[ciTargetMethod.directCalls.size() + adapterCount];

        CiDebugInfo[] debugInfos = new CiDebugInfo[total];

        if (adapter != null) {
            directCallees[index] = adapter;
            int callPos = adapter.callOffsetInPrologue();
            int stopPos = stopPosForCall(callPos, adapter.callSizeInPrologue());
            stops[index] = Stops.make(stopPos, callPos, Stops.DIRECT_CALL);
            index++;
        }

        for (CiTargetMethod.Call site : ciTargetMethod.directCalls) {
            int causePos = site.pcOffset;
            int stopPos = Stops.stopPosForCall(site);
            Object directCallee = CallTarget.directCallee(site.target);
            directCallees[index] = directCallee;
            if (CallTarget.isTemplateCall(site.target)) {
                stops[index] = Stops.make(stopPos, causePos, DIRECT_CALL, TEMPLATE_CALL);
            } else {
                stops[index] = Stops.make(stopPos, causePos, DIRECT_CALL);
            }
            debugInfos[index] = site.debugInfo;
            index++;
        }

        for (CiTargetMethod.Call site : ciTargetMethod.indirectCalls) {
            int causePos = site.pcOffset;
            int stopPos = Stops.stopPosForCall(site);
            if (CallTarget.isTemplateCall(site.target)) {
                stops[index] = Stops.make(stopPos, causePos, INDIRECT_CALL, TEMPLATE_CALL);
            } else if (CallTarget.isSymbol(site.target)) {
                stops[index] = Stops.make(stopPos, causePos, INDIRECT_CALL, NATIVE_CALL);
                ClassMethodActor caller = (ClassMethodActor) site.debugInfo.codePos.method;
                assert caller.isNative();
                caller.nativeFunction.setCallSite(this, stopPos);
            } else {
                stops[index] = Stops.make(stopPos, causePos, INDIRECT_CALL);
            }
            debugInfos[index] = site.debugInfo;
            index++;
        }

        for (CiTargetMethod.Safepoint site : ciTargetMethod.safepoints) {
            int stopPos = site.pcOffset;
            int causePos = stopPos;
            stops[index] = Stops.make(stopPos, causePos, SAFEPOINT);
            debugInfos[index] = site.debugInfo;
            index++;
        }

        setStops(new Stops(stops), directCallees);
        return debugInfos;
    }

    /**
     * Completes the definition of this target method as the result of compilation.
     *
     * @param scalarLiterals a byte array encoding the scalar data accessed by this target via code relative offsets
     * @param referenceLiterals an object array encoding the object references accessed by this target via code relative
     *            offsets
     * @param codeBuffer the buffer containing the compiled code. The compiled code is in the first {@code this.code.length} bytes of {@code codeBuffer}.
     */
    protected final void setData(byte[] scalarLiterals, Object[] referenceLiterals, byte[] codeBuffer) {

        assert !codeStart.isZero() : "Must call setCodeArrays() first";

        // Copy scalar literals
        if (scalarLiterals != null && scalarLiterals.length > 0) {
            assert scalarLiterals.length != 0;
            System.arraycopy(scalarLiterals, 0, this.scalarLiterals, 0, this.scalarLiterals.length);
        }

        // Copy reference literals
        if (referenceLiterals != null && referenceLiterals.length > 0) {
            System.arraycopy(referenceLiterals, 0, this.referenceLiterals, 0, this.referenceLiterals.length);
        }

        // now copy the code
        System.arraycopy(codeBuffer, 0, this.code, 0, this.code.length);
    }

    public final ClassMethodActor callSiteToCallee(Address callSite) {
        int callPos = callSite.minus(codeStart).toInt();
        int dcIndex = 0;
        for (int i = 0; i < stops.length(); i++) {
            if (stops.isSetAt(DIRECT_CALL, i)) {
                if (stops.causePosAt(i) == callPos && directCallees[dcIndex] instanceof ClassMethodActor) {
                    return (ClassMethodActor) directCallees[dcIndex];
                }
                dcIndex++;
            }
        }
        throw FatalError.unexpected("could not find callee for call site: " + callSite.toHexString());
    }

    public Word getEntryPoint(CallEntryPoint callEntryPoint) {
        return callEntryPoint.in(this);
    }

    /**
     * Resets a direct call site, make it point to the static trampoline again.
     *
     * @param stopIndex the index of the call in {@link #stops() stops}
     * @param dcIndex the index of the call in {@link #directCallees()}
     *
     * @return {@code true} if the call site was not already pointing to the static trampoline
     */
    public final boolean resetDirectCall(int stopIndex, int dcIndex) {
        Object callee = directCallees[dcIndex];
        assert !(callee instanceof Adapter);
        final int offset = getCallEntryOffset(callee, stopIndex);
        final int callPos = stops.causePosAt(stopIndex);
        Pointer trampoline = vm().stubs.staticTrampoline().codeStart.plus(offset);
        return !patchCallSite(callPos, trampoline).equals(trampoline);
    }

    /**
     * Links all the calls from this target method to other methods for which the exact method actor is known. Linking a
     * call means patching the operand of a call instruction that specifies the address of the target code to call. In
     * the case of a callee for which there is no target code available (i.e. it has not yet been compiled or it has
     * been evicted from the code cache), the address of a {@linkplain StaticTrampoline static trampoline} is patched
     * into the call instruction.
     *
     * @return true if target code was available for all the direct callees
     */
    public final boolean linkDirectCalls() {
        boolean linkedAll = true;
        if (directCallees != null) {
            int dcIndex = 0;
            for (int stopIndex = stops.nextDirectCall(0); stopIndex >= 0; stopIndex = stops.nextDirectCall(stopIndex + 1)) {
                Object currentDirectCallee = directCallees[dcIndex];
                final int offset = getCallEntryOffset(currentDirectCallee, stopIndex);
                if (currentDirectCallee == null) {
                    // template call
                    assert classMethodActor.isTemplate();
                } else {
                    final TargetMethod callee = getTargetMethod(currentDirectCallee);
                    if (callee == null) {
                        if (MaxineVM.isHosted() && classMethodActor.isTemplate()) {
                            assert currentDirectCallee == classMethodActor : "unlinkable call in a template must be a template call";
                            // leave call site unpatched
                        } else {
                            linkedAll = false;
                            final int callPos = stops.causePosAt(stopIndex);
                            final Address callSite = codeStart.plus(callPos);
                            if (!isPatchableCallSite(callSite)) {
                                FatalError.unexpected(classMethodActor + ": call site calling static trampoline must be patchable: 0x" + callSite.toHexString() +
                                                " [0x" + codeStart.toHexString() + "+" + callPos + "]");
                            }
                            fixupCallSite(callPos, vm().stubs.staticTrampoline().codeStart.plus(offset));
                        }
                    } else {
                        int callPos = stops.causePosAt(stopIndex);
                        fixupCallSite(callPos, callee.codeStart().plus(offset));
                    }
                }
                dcIndex++;
            }
        }

        return linkedAll;
    }

    public final TargetMethod getTargetMethod(Object o) {
        TargetMethod result = null;
        if (o instanceof ClassMethodActor) {
            result = CompilationScheme.Static.getCurrentTargetMethod((ClassMethodActor) o);
        } else if (o instanceof TargetMethod) {
            result = (TargetMethod) o;
        }
        return result;
    }

    private int getCallEntryOffset(Object callee, int stopIndex) {
        if (callee instanceof Adapter) {
            return 0;
        }
        final CallEntryPoint callEntryPoint = callEntryPointForDirectCall(stopIndex);
        return callEntryPoint.offsetInCallee();
    }

    @HOSTED_ONLY
    protected boolean isDirectCalleeInPrologue(int directCalleeIndex) {
        return false;
    }

    public final boolean isCalleeSaved() {
        return registerRestoreEpilogueOffset >= 0;
    }

    /**
     * Gets the code annotations (if any) associated with this target method.
     *
     * @return {@code null} if there are no code annotations
     */
    public CodeAnnotation[] annotations() {
        return null;
    }

    /**
     * @see Stops
     */
    public final Stops stops() {
        return stops;
    }

    /**
     * Gets the target code position for a machine code instruction address.
     *
     * @param ip
     *                an instruction pointer that may denote an instruction in this target method
     * @return the start position of the bytecode instruction that is implemented at the instruction pointer or
     *         -1 if {@code ip} denotes an instruction that does not correlate to any bytecode. This will
     *         be the case when {@code ip} is in the adapter frame stub code, prologue or epilogue.
     */
    public final int posFor(Address ip) {
        final int pos = ip.minus(codeStart).toInt();
        if (pos >= 0 && pos <= code.length) {
            return pos;
        }
        return -1;
    }

    /**
     * Gets a mapping from bytecode positions to target code positions. A non-zero value
     * {@code val} at index {@code i} in the array encodes that there is a bytecode instruction whose opcode is at index
     * {@code i} in the bytecode array and whose target code position is {@code val}. Unless {@code i} is equal to the
     * length of the bytecode array in which case {@code val} denotes the target code position one byte past the
     * last target code byte emitted for the last bytecode instruction.
     *
     * @return {@code null} if there is no such mapping available
     */
    public int[] bciToPosMap() {
        return null;
    }

    public int findStopIndex(Pointer ip) {
        final int pos = posFor(ip);
        if (stops == null || pos < 0 || pos > code.length) {
            return -1;
        }
        return findStopIndex(pos);
    }

    public int findStopIndex(int pos) {
        return stops.indexOf(pos);
    }

    @Override
    public final String toString() {
        return (classMethodActor == null) ? regionName() : classMethodActor.format("%H.%n(%p)");
    }

    public String name() {
        return regionName();
    }

    /**
     * Traces the metadata of the compiled code represented by this object. In particular, the
     * {@linkplain #traceExceptionHandlers(IndentWriter) exception handlers}, the
     * {@linkplain #traceDirectCallees(IndentWriter) direct callees}, the #{@linkplain #traceScalarBytes(IndentWriter, TargetBundle) scalar data},
     * the {@linkplain #traceReferenceLiterals(IndentWriter, TargetBundle) reference literals} and the address of the
     * array containing the {@linkplain #code() compiled code}.
     *
     * @param writer where the trace is written
     */
    public void traceBundle(IndentWriter writer) {
        final TargetBundleLayout targetBundleLayout = TargetBundleLayout.from(this);
        writer.println("Layout:");
        writer.println(Strings.indent(targetBundleLayout.toString(), writer.indentation()));
        traceExceptionHandlers(writer);
        traceDirectCallees(writer);
        traceScalarBytes(writer, targetBundleLayout);
        traceReferenceLiterals(writer, targetBundleLayout);
        traceDebugInfo(writer);
        writer.println("Code cell: " + targetBundleLayout.cell(start(), ArrayField.code).toString());
    }

    /**
     * Traces the {@linkplain #directCallees() direct callees} of the compiled code represented by this object.
     *
     * @param writer where the trace is written
     */
    public final void traceDirectCallees(IndentWriter writer) {
        if (directCallees != null) {
            assert stops != null && directCallees.length <= stops.length();
            writer.println("Direct Calls: ");
            writer.indent();
            for (int i = 0; i < directCallees.length; i++) {
                writer.println(stops.posAt(i) + " -> " + directCallees[i]);
            }
            writer.outdent();
        }
    }

    /**
     * Traces the {@linkplain #scalarLiterals() scalar data} addressed by the compiled code represented by this object.
     *
     * @param writer where the trace is written
     */
    public final void traceScalarBytes(IndentWriter writer, final TargetBundleLayout targetBundleLayout) {
        if (scalarLiterals != null) {
            writer.println("Scalars:");
            writer.indent();
            for (int i = 0; i < scalarLiterals.length; i++) {
                final Pointer pointer = targetBundleLayout.cell(start(), ArrayField.scalarLiterals).plus(ArrayField.scalarLiterals.arrayLayout.getElementOffsetInCell(i));
                writer.println("[" + pointer.toString() + "] 0x" + Integer.toHexString(scalarLiterals[i] & 0xFF) + "  " + scalarLiterals[i]);
            }
            writer.outdent();
        }
    }

    /**
     * Traces the {@linkplain #referenceLiterals() reference literals} addressed by the compiled code represented by this object.
     *
     * @param writer where the trace is written
     */
    public final void traceReferenceLiterals(IndentWriter writer, final TargetBundleLayout targetBundleLayout) {
        if (referenceLiterals != null) {
            writer.println("References: ");
            writer.indent();
            for (int i = 0; i < referenceLiterals.length; i++) {
                final Pointer pointer = targetBundleLayout.cell(start(), ArrayField.referenceLiterals).plus(ArrayField.referenceLiterals.arrayLayout.getElementOffsetInCell(i));
                writer.println("[" + pointer.toString() + "] " + referenceLiterals[i]);
            }
            writer.outdent();
        }
    }

    /**
     * Analyzes the target method that this compiler produced to build a call graph. This method gathers the
     * methods called directly or indirectly by this target method as well as the methods it inlined.
     *
     * @param directCalls the set of direct calls to which this method should append
     * @param virtualCalls the set of virtual calls to which this method should append
     * @param interfaceCalls the set of interface calls to which this method should append
     * @param inlinedMethods the set of inlined methods to which this method should append
     */
    @HOSTED_ONLY
    public abstract void gatherCalls(Set<MethodActor> directCalls, Set<MethodActor> virtualCalls, Set<MethodActor> interfaceCalls, Set<MethodActor> inlinedMethods);

    public abstract Address throwAddressToCatchAddress(boolean isTopFrame, Address throwAddress, Class<? extends Throwable> throwableClass);

    /**
     * Modifies the call site at the specified offset to use the new specified entry point.
     * The modification must tolerate the execution of the target method by concurrently running threads.
     *
     * @param callSite offset to a call site relative to the start of the code of this target method
     * @param callEntryPoint entry point the call site should call after patching
     * @return the entry point of the call prior to patching
     */
    public abstract Address patchCallSite(int callOffset, Address callEntryPoint);

    /**
     * Fixup a call site in the method. This differs from the above in that the call site is updated before
     * any thread can see it. Thus there isn't any concurrency between modifying the call site and threads
     * trying to run it.
     *
     * @param callOffset offset to a call site relative to the start of the code of this target method
     * @param callEntryPoint entry point the call site should call after fixup
     * @return the entry point of the call prior to patching
     */
    public abstract Address fixupCallSite(int callOffset, Address callEntryPoint);

    /**
     * Indicates whether a call site can be patched safely when multiple threads may execute this target method concurrently.
     * @param callSite offset to a call site relative to the start of the code of this target method.
     * @return true if mt-safe patching is possible on the specified call site.
     */
    public abstract boolean isPatchableCallSite(Address callSite);

    /**
     * Traces the debug info for the compiled code represented by this object.
     * @param writer where the trace is written
     */
    public abstract void traceDebugInfo(IndentWriter writer);

    /**
     * @param writer where the trace is written
     */
    public abstract void traceExceptionHandlers(IndentWriter writer);

    /**
     * Prepares the reference map for the current frame (and potentially for registers stored in a callee frame).
     *
     * @param current the current stack frame
     * @param callee the callee stack frame (ignoring any interposing {@linkplain Adapter adapter} frame)
     * @param preparer the reference map preparer which receives the reference map
     */
    public abstract void prepareReferenceMap(Cursor current, Cursor callee, StackReferenceMapPreparer preparer);

    /**
     * The stack and frame pointers describing the extent of a physical frame.
     */
    public static class FrameInfo {
        public FrameInfo(Pointer sp, Pointer fp) {
            this.sp = sp;
            this.fp = fp;
        }
        public Pointer sp;
        public Pointer fp;
    }

    /**
     * Adjusts the stack and frame pointers for the frame about to handle an exception.
     * This is provided mainly for the benefit of deoptimization.
     */
    public void adjustFrameForHandler(FrameInfo frame) {
    }

    /**
     * Attempts to catch an exception thrown by this method or a callee method. This method should not return
     * if this method catches the exception, but instead should unwind the stack and resume execution at the handler.
     * @param current the current stack frame
     * @param callee the callee stack frame (ignoring any interposing {@linkplain Adapter adapter} frame)
     * @param throwable the exception thrown
     */
    public abstract void catchException(Cursor current, Cursor callee, Throwable throwable);

    /**
     * Accepts a visitor for this stack frame.
     * @param current the current stack frame
     * @param visitor the visitor which will visit the frame
     * @return {@code true} if the visitor indicates the stack walk should continue
     */
    public abstract boolean acceptStackFrameVisitor(Cursor current, StackFrameVisitor visitor);

    /**
     * Advances the stack frame cursor from this frame to the next frame.
     * @param current the current stack frame cursor
     */
    public abstract void advance(Cursor current);

    /**
     * Gets a pointer to the memory word holding the return address in a frame of this target method.
     *
     * @param frame an activation frame for this target method
     */
    public abstract Pointer returnAddressPointer(Cursor frame);

    /**
     * Specifies if this target method can be used to reconstruct deoptimized frames.
     */
    public boolean isDeoptimizationTarget() {
        return false;
    }

    /**
     * Creates a deoptimized frame for this method.
     *
     * @param info details of current deoptimization
     * @param frame debug info from which the slots of the deoptimized are initialized
     * @param callee used to notify callee of the execution state the deoptimized frame when it is returned to
     * @return object for notifying the deoptimized frame's caller of continuation state
     */
    public Continuation createDeoptimizedFrame(Info info, CiFrame frame, Continuation callee) {
        throw FatalError.unexpected("Cannot create deoptimized frame for " + getClass().getSimpleName() + " " + this);
    }

    /**
     * Determines if this method has been compiled under the invariant that the
     * register state upon entry to a local exception handler for an implicit
     * exception is the same as at the implicit exception point.
     */
    public boolean preserveRegistersForLocalExceptionHandler() {
        return true;
    }

    /**
     * Gets the profile data gathered during execution of this method.
     *
     * @return {@code null} if this method has no profiling info
     */
    public MethodProfile profile() {
        return null;
    }

    /**
     * Gets the stub type of this target method.
     *
     * @return {@code null} if this is not {@linkplain Stub stub}
     */
    public Stub.Type stubType() {
        return null;
    }

    /**
     * Determines if this is a stub of a give type.
     */
    public final boolean is(Stub.Type type) {
        return stubType() == type;
    }

    /**
     * Gets the layout of the CSA in this target method.
     *
     * @return {@code null} if this is not a callee-saved target method
     */
    public CiCalleeSaveLayout calleeSaveLayout() {
        if (classMethodActor != null) {
            return vm().registerConfigs.getRegisterConfig(classMethodActor).csl;
        }
        return null;
    }

    public abstract VMFrameLayout frameLayout();
}
