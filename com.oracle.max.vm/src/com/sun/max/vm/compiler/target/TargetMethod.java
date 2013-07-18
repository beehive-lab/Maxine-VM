/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
import static com.sun.max.vm.compiler.target.Safepoints.*;

import java.io.*;
import java.util.*;

import com.oracle.max.asm.target.amd64.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiTargetMethod.Call;
import com.sun.cri.ci.CiTargetMethod.CodeAnnotation;
import com.sun.cri.ci.CiTargetMethod.DataPatch;
import com.sun.cri.ci.CiTargetMethod.Safepoint;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.code.CodeManager.Lifespan;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.RuntimeCompiler.Nature;
import com.sun.max.vm.compiler.deopt.*;
import com.sun.max.vm.compiler.deopt.Deoptimization.Continuation;
import com.sun.max.vm.compiler.deopt.Deoptimization.Info;
import com.sun.max.vm.compiler.target.TargetBundleLayout.ArrayField;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.profile.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.ti.*;

/**
 * Represents machine code produced and managed by the VM. The machine code
 * is either produced by a compiler or manually assembled. Examples of the
 * latter are {@linkplain Stub stubs} and {@linkplain Adapter adapters}.
 */
public abstract class TargetMethod extends MemoryRegion {

    /**
     * Implemented by a client wanting to do something to a target method.
     */
    public interface Closure {
        /**
         * Processes a given target method.
         *
         * @param targetMethod the class to process
         * @return {@code false} if target method processing (e.g. iteration) by the caller should be stopped
         */
        boolean doTargetMethod(TargetMethod targetMethod);
    }

    /**
     * Call back for use with {@link TargetMethod#forEachCodePos(CodePosClosure, CodePointer)}.
     */
    public interface CodePosClosure {
        /**
         * Processes a given bytecode position.
         *
         * @param method the method actor on which this functionality is to be evaluated.
         * @param bci the index in the method's bytecode.
         * @return true if the caller should continue to the next code position (if any), false if it should terminate now
         */
        boolean doCodePos(ClassMethodActor method, int bci);
    }

    /**
     * The (bytecode) method from which this target method was compiled.
     * This will be {@code null} iff this target method is a {@link Stub} or
     * and {@link Adapter}.
     */
    @INSPECTED
    public final ClassMethodActor classMethodActor;

    protected Safepoints safepoints = Safepoints.NO_SAFEPOINTS;

    /**
     * @see #directCallees()
     */
    protected Object[] directCallees = NO_DIRECT_CALLEES;

    /**
     * @see #scalarLiterals()
     */
    @INSPECTED
    protected byte[] scalarLiterals;

    /**
     * @see #referenceLiterals()
     */
    @INSPECTED(deepCopied = false)
    protected Object[] referenceLiterals;

    /**
     * Pointer to a byte array, stored in the code cache allocation, that contains
     * the target code for the compilation.  When the code has been evicted (i.e.
     * not survived a code cache eviction cycle), this value is set to the sentinel
     * value {@link #WIPED_CODE}.
     * <p>
     * <strong>Note:</strong> The Inspector compares this field to the (fixed) sentinel
     * pointer for its test that determines whether the code has been evicted.
     * @see #code()
     */
    @INSPECTED
    private byte[] code;

    /**
     * @see #codeStart()
     */
    @INSPECTED
    private Pointer codeStart = Pointer.zero();

    /**
     * This field is used to implement {@linkplain CodeEviction code eviction}. It serves the following roles
     * during different phase of code eviction:
     * <ul>
     * <li>As a mark field during code eviction, to identify survivors. The mark is set iff the value of this field is {@link Address#allOnes()}.</li>
     * <li>To record the old value of {@link #start()} prior the target method being relocated.</li>
     * </ul>
     */
    @INSPECTED
    protected Address oldStart = Address.zero();

    /**
     * If non-null, then this method has been invalidated.
     * Set only by deoptimization operation at safepoint.
     *
     * @see #invalidated()
     */
    private InvalidationMarker invalidated;

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
     * Gets the code attribute from which this target method was compiled. This may differ from
     * the code attribute of {@link #classMethodActor} in the case where it was rewritten.
     */
    public CodeAttribute codeAttribute() {
        return classMethodActor == null ? null : classMethodActor.codeAttribute();
    }

    /**
     * Notify this method that it survived an {@linkplain CodeEviction eviction cycle}.
     */
    public void survivedEviction() {
        // empty
    }

    /**
     * @return true if this method has been {@linkplain CodeEviction evicted} (i.e., its
     * machine code is no longer present in the code cache).
     */
    public boolean wasEvicted() {
        return false;
    }

    /**
     * @return the number of survived {@linkplain CodeEviction eviction cycles}.
     */
    public int timesRelocated() {
        return 0;
    }

    /**
     * Marks this method as invalidated.
     *
     * @return true if this was the first attempt to invalidate the method. If not, then the invalidation marker for
     *         this method was not updated.
     * @see #invalidated()
     */
    public boolean invalidate(InvalidationMarker marker) {
        assert marker != null;
        assert isHosted() || VmThread.current().isVmOperationThread();
        if (invalidated == null) {
            invalidated = marker;
            return true;
        }
        return false;
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
     * Iterates over the bytecode locations for the inlining chain rooted at a given instruction pointer.
     *
     * @param cpc a closure called for each bytecode location in the inlining chain rooted at {@code ip} (inner most
     *            callee first)
     * @param ip a pointer to an instruction within this method
     * @return the number of bytecode locations iterated over (i.e. the number of times
     *         {@link CodePosClosure#doCodePos(ClassMethodActor, int)} was called
     */
    public int forEachCodePos(CodePosClosure cpc, CodePointer ip) {
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
        public Pointer callerSP;

        /**
         * Frame pointer.
         */
        public Pointer callerFP;

        public FrameAccess(CiCalleeSaveLayout csl, Pointer csa, Pointer sp, Pointer fp, Pointer callerSP, Pointer callerFP) {
            this.csl = csl;
            this.csa = csa;
            this.sp = sp;
            this.fp = fp;
            this.callerSP = callerSP;
            this.callerFP = callerFP;
        }

        /**
         * For use when walking stacks in callee/caller order.
         * @param callerSP
         * @param callerFP
         */
        public void setCallerInfo(Pointer callerSP, Pointer callerFP) {
            this.callerSP = callerSP;
            this.callerFP = callerFP;
        }
    }

    /**
     * Gets the debug info available for a given safepoint. If {@code fa != null}, then the {@linkplain CiFrame#values values} in the
     * returned object are {@link CiConstant}s wrapping values from the thread's stack denoted by {@code fa}. Otherwise,
     * they are {@link CiStackSlot}, {@link CiRegister} and {@link CiConstant} values describing how to extract values
     * from a live frame.
     * <p>
     * Note that the returned object is only suitable for deoptimization if an
     * optimizing compiler produced this target method. Otherwise, the frame info
     * in the returned object may include slots that are not actually live as
     * the given safepoint.
     *
     * @param safepointIndex an index of a safepoint within this method
     * @param fa access to a live frame (may be {@code null})
     * @return the debug info at the denoted safepoint of {@code null} if none available
     */
    public CiDebugInfo debugInfoAt(int safepointIndex, FrameAccess fa) {
        return null;
    }

    /**
     * Gets an array containing the direct callees of this method.
     * The array can contain instances of {@link ClassMethodActor} and {@link TargetMethod} side by side.
     * In case a callee is an actual method, it is represented by a {@link ClassMethodActor}.
     * In case it is a stub or the adapter, a {@link TargetMethod} is used.
     *
     * @return entities referenced by direct call instructions
     * @see Safepoints#nextDirectCall(int)
     */
    public final Object[] directCallees() {
        return directCallees;
    }

    /**
     * Helper method to turn an entry from a directCallees array into a {@link TargetMethod}.
     */
    public static TargetMethod directCalleeFrom(Object callee) {
        if (callee == null) {
            return null;
        }
        return callee instanceof TargetMethod ? (TargetMethod) callee : ((ClassMethodActor) callee).currentTargetMethod();
    }

    /**
     * Gets the call entry point to be used for a direct call from this target method. By default, the
     * {@linkplain #callEntryPoint call entry point} of this target method will be used.
     *
     * @param safepointIndex the index of the call in {@link #safepoints() safepoints}
     */
    protected CallEntryPoint callEntryPointForDirectCall(int safepointIndex) {
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
     *
     * Note: if this array is assigned to a variable outside of its target method, care should be taken that no
     * safepoints occur between the assignment and uses of the variable. At safepoints, code eviction (see {@link CodeEviction}) might
     * take place, potentially invalidating the address of the array, so that client code accessing the variable
     * would end up working on invalid addresses.
     */
    @INLINE
    public final byte[] code() {
        return code;
    }

    public final int codeLength() {
        return (code == null) ? 0 : code.length;
    }

    /**
     * Gets the address of the first instruction in this target method's {@linkplain #code() compiled code array}
     * in the form of an eviction-safe {@link CodePointer}.
     */
    @INLINE
    public final CodePointer codeStart() {
        return CodePointer.from(codeStart);
    }

    /**
     * Gets the address of a particular instruction in this target method's {@linkplain #code() compiled code array}
     * in the form of an eviction-safe {@link CodePointer}.
     *
     * @param pos the code position. A value of 0 implies the first instruction. This must be a value in the range {@code [0 .. codeLength())}.
     */
    @INLINE
    public final CodePointer codeAt(int pos) {
        FatalError.asert(pos >= 0 && pos < codeLength());
        return CodePointer.from(codeStart.plus(pos));
    }

    @INLINE
    public final Address oldStart() {
        return oldStart;
    }

    public final void setOldStart(Address ocs) {
        oldStart = ocs;
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

    protected final void setSafepoints(Safepoints safepoints, Object[] directCallees) {
        this.safepoints = safepoints;
        this.directCallees = directCallees;
        assert safepoints.numberOfDirectCalls() == directCallees.length : safepoints.numberOfDirectCalls() + " !=  " + directCallees.length;
    }

    protected final void setFrameSize(int frameSize) {
        assert frameSize != -1 : "invalid frame size!";
        this.frameSize = frameSize;
    }

    /**
     * Gets this method's life span, i.e., a value indicating how long the machine code is expected to live.
     * For baseline methods, this is short - they might be evicted once the code cache meets contention.
     * Class initialisers are run only once, they have a one-shot life span.
     * Optimised methods, adapters, and stubs are expected to live for a long time.
     */
    public abstract Lifespan lifespan();

    protected void initCodeBuffer(CiTargetMethod ciTargetMethod, boolean install) {
        // Create the arrays for the scalar and the object reference literals
        Literals literals = new Literals(ciTargetMethod.dataReferences);

        // Allocate and set the code and data buffer
        final TargetBundleLayout targetBundleLayout = new TargetBundleLayout(literals.scalars.length, literals.objects.length, ciTargetMethod.targetCodeSize());
        if (install) {
            Code.allocate(targetBundleLayout, this);
        } else {
            Code.allocateInHeap(targetBundleLayout, this);
        }

        if (literals.scalars.length != 0 && literals.scalarsAlignment != 0) {
            Pointer scalars = targetBundleLayout.firstElementPointer(start, ArrayField.scalarLiterals);
            Address alignedScalars = scalars.alignUp(literals.scalarsAlignment);
            if (!scalars.equals(alignedScalars)) {
                assert alignedScalars.greaterThan(scalars);
                literals.relocateScalars(alignedScalars.minus(scalars).toInt());
            }
        }

        setData(literals.scalars, literals.objects, ciTargetMethod.targetCode());

        // Patch relative instructions in the code buffer
        assert lifespan() == Lifespan.LONG : "code may move: must protect direct code pointers";
        patchInstructions(targetBundleLayout, ciTargetMethod, literals);
    }

    /**
     * Used to serialize a list of {@link DataPatch}es to data structures
     * that are co-located with the code that accesses the data.
     */
    static class Literals {
        /**
         * Map from scalar data indexes to the position of the scalar data in {@link #scalars}.
         */
        final int[] scalarsMap;

        /**
         * Serialized scalar literals.
         */
        final byte[] scalars;

        /**
         * The object literals.
         */
        final IdentityHashMap<Object, Integer> objectPool;
        final Object[] objects;

        /**
         * The largest alignment requirement of any data in {@link #scalars}. This will be
         * 0 if there are no alignment requirements.
         */
        final int scalarsAlignment;

        /**
         * Gets the index in {@link TargetMethod#scalarLiterals} of the scalar data at
         * index {@code index} in {@link CiTargetMethod#dataReferences}.
         */
        int scalarPos(int dataIndex) {
            return scalarsMap[dataIndex];
        }

        /**
         * Relocates the scalar literals by shifting the contents of {@link #scalars} by {@code delta}
         * to the right. This assumes that at least {@code delta} padding was put into the end of {@link #scalars}.
         */
        void relocateScalars(int delta) {
            assert delta > 0;
            for (int i = 0; i < scalarsMap.length; i++) {
                if (scalarsMap[i] >= 0) {
                    scalarsMap[i] += delta;
                }
            }
            for (int i = scalars.length - 1; i >= delta; --i) {
                scalars[i] = scalars[i - delta];
            }
        }

        public Literals(List<DataPatch> dataReferences) {
            objectPool = new IdentityHashMap<Object, Integer>(dataReferences.size());
            ArrayList<Object> objectsBuffer = new ArrayList<Object>(dataReferences.size());
            ByteArrayOutputStream scalarsBuffer = new ByteArrayOutputStream();
            Endianness endianness = platform().endianness();
            scalarsMap = new int[dataReferences.size()];
            int dataIndex = 0;
            int scalarsAlignment = 0;

            // Data patches need to be sorted in decreasing order of their alignment requirements
            for (DataPatch site : dataReferences) {
                if (site.alignment != 0) {
                    Collections.sort(dataReferences, new Comparator<DataPatch>() {
                        @Override
                        public int compare(DataPatch o1, DataPatch o2) {
                            return o2.alignment - o1.alignment;
                        }
                    });
                    break;
                }
            }

            for (DataPatch site : dataReferences) {
                final CiConstant data = site.constant;
                if (!data.kind.isObject()) {
                    if (site.alignment != 0) {
                        while ((scalarsBuffer.size() & (site.alignment - 1)) != 0) {
                            scalarsBuffer.write(0);
                        }
                        if (site.alignment > scalarsAlignment) {
                            scalarsAlignment = site.alignment;
                        }
                    }
                    scalarsMap[dataIndex] = scalarsBuffer.size();

                } else {
                    assert site.alignment == 0 : "Alignment for object literals not supported";
                    scalarsMap[dataIndex] = -1;
                }
                try {
                    switch (data.kind) {
                        case Double:
                            endianness.writeLong(scalarsBuffer, Double.doubleToRawLongBits(data.asDouble()));
                            break;

                        case Float:
                            endianness.writeInt(scalarsBuffer, Float.floatToRawIntBits(data.asFloat()));
                            break;

                        case Int:
                            endianness.writeInt(scalarsBuffer, data.asInt());
                            break;

                        case Long:
                            endianness.writeLong(scalarsBuffer, data.asLong());
                            break;

                        case Object: {
                            Object object = data.asObject();
                            assert object != null;
                            if (!objectPool.containsKey(object)) {
                                objectPool.put(object, objectsBuffer.size());
                                objectsBuffer.add(object);
                            }
                            break;
                        }
                        default:
                            throw new IllegalArgumentException("Unknown constant type!");
                    }

                } catch (IOException e) {
                    throw (InternalError) new InternalError("Error serializing " + data).initCause(e);
                }
                dataIndex++;
            }

            final int guaranteedAlignment = Word.size();
            int padding = scalarsAlignment - guaranteedAlignment;
            while (padding > 0) {
                scalarsBuffer.write(0);
                padding--;
            }

            scalars = scalarsBuffer.toByteArray();
            this.scalarsAlignment = scalarsAlignment;
            objects = objectsBuffer.toArray();
        }
    }

    private void patchInstructions(TargetBundleLayout targetBundleLayout, CiTargetMethod ciTargetMethod, Literals literals) {
        Offset codeStart = targetBundleLayout.cellOffset(TargetBundleLayout.ArrayField.code);

        Offset scalarDiff = Offset.zero();
        if (this.scalarLiterals != null) {
            Offset scalarStart = targetBundleLayout.cellOffset(TargetBundleLayout.ArrayField.scalarLiterals);
            scalarDiff = scalarStart.minus(codeStart).asOffset();
        }

        Offset referenceDiff = Offset.zero();
        if (this.referenceLiterals() != null) {
            Offset referenceStart = targetBundleLayout.cellOffset(TargetBundleLayout.ArrayField.referenceLiterals);
            referenceDiff = referenceStart.minus(codeStart).asOffset();
        }

        int dataIndex = 0;
        for (DataPatch site : ciTargetMethod.dataReferences) {
            switch (site.constant.kind) {
                case Double: // fall through
                case Float: // fall through
                case Int: // fall through
                case Long: {
                    assert site.alignment == 0 || targetBundleLayout.firstElementPointer(start, ArrayField.scalarLiterals).plus(literals.scalarPos(dataIndex)).isAligned(site.alignment) : "patching to a scalar address that is not aligned";
                    patchRelativeInstruction(site.pcOffset, scalarDiff.plus(literals.scalarPos(dataIndex) - site.pcOffset).toInt());
                    break;
                }
                case Object: {
                    int index = literals.objectPool.get(site.constant.asObject());
                    patchRelativeInstruction(site.pcOffset, referenceDiff.plus(index * Word.size() - site.pcOffset).toInt());
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unknown constant type!");
            }

            dataIndex++;
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

    protected CiDebugInfo[] initSafepoints(CiTargetMethod ciTargetMethod) {
        Adapter adapter = null;
        int adapterCount = 0;

        AdapterGenerator generator = AdapterGenerator.forCallee(this);
        if (generator != null) {
            adapter = generator.make(classMethodActor);
            if (adapter != null) {
                adapterCount = 1;
            }
        }

        int total = ciTargetMethod.safepoints.size() + adapterCount;
        int directCalls = 0;
        for (Safepoint safepoint : ciTargetMethod.safepoints) {
            if (safepoint instanceof Call && ((Call) safepoint).direct) {
                directCalls++;
            }
        }

        int index = 0;
        int[] safepoints = new int[total];
        Object[] directCallees = new Object[directCalls + adapterCount];
        CiDebugInfo[] debugInfos = new CiDebugInfo[total];

        int dcIndex = 0;
        if (adapter != null) {
            directCallees[index] = adapter;
            int callPos = adapter.callOffsetInPrologue();
            int safepointPos = safepointPosForCall(callPos, adapter.callSizeInPrologue());
            safepoints[index] = Safepoints.make(safepointPos, callPos, DIRECT_CALL);
            dcIndex++;
            index++;
        }

        for (Safepoint safepoint : ciTargetMethod.safepoints) {
            int encodedSafepoint;
            if (safepoint instanceof Call) {
                Call call = (Call) safepoint;
                int causePos = call.pcOffset;
                int safepointPos = safepointPosForCall(call.pcOffset, call.size);
                if (call.direct) {
                    directCallees[dcIndex++] = CallTarget.directCallee(call.target);
                    if (CallTarget.isTemplateCall(call.target)) {
                        encodedSafepoint = Safepoints.make(safepointPos, causePos, DIRECT_CALL, TEMPLATE_CALL);
                    } else {
                        encodedSafepoint = Safepoints.make(safepointPos, causePos, DIRECT_CALL);
                    }
                } else {
                    int attrMask = INDIRECT_CALL.mask;
                    if (CallTarget.isTemplateCall(call.target)) {
                        attrMask |= TEMPLATE_CALL.mask;
                    } else if (CallTarget.isSymbol(call.target) || (classMethodActor.isNative() && classMethodActor == call.target)) {
                        // The first term is for C1X, the second for Graal, which reuses the MethodActor for the
                        // native method itself to identify the real native function, because Graal's IndirectCall
                        // requires a ResolvedJavaMethod (as opposed to a String symbol) and there isn't one for the native function (obviously).
                        attrMask |= NATIVE_CALL.mask;
                        classMethodActor.nativeFunction.setCallSite(this, safepointPos);
                    }
                    encodedSafepoint = Safepoints.make(safepointPos, causePos, attrMask);
                }
            } else {
                int safepointPos = safepoint.pcOffset;
                encodedSafepoint = Safepoints.make(safepointPos);
            }
            debugInfos[index] = safepoint.debugInfo;
            safepoints[index] = encodedSafepoint;
            index++;
        }

        setSafepoints(new Safepoints(safepoints), directCallees);
        return debugInfos;
    }

    /**
     * Completes the definition of this target method as the result of compilation.
     *
     * @param scalarLiterals a byte array encoding the scalar data accessed by this target via code relative offsets
     * @param objectLiterals an object array encoding the object references accessed by this target via code relative
     *            offsets
     * @param codeBuffer the buffer containing the compiled code. The compiled code is in the first {@code this.code.length} bytes of {@code codeBuffer}.
     */
    protected final void setData(byte[] scalarLiterals, Object[] objectLiterals, byte[] codeBuffer) {

        assert !codeStart.isZero() : "Must call setCodeArrays() first";

        // Copy scalar literals
        if (scalarLiterals != null && scalarLiterals.length > 0) {
            assert scalarLiterals.length != 0;
            System.arraycopy(scalarLiterals, 0, this.scalarLiterals, 0, this.scalarLiterals.length);
        }

        // Copy object literals
        if (objectLiterals != null && objectLiterals.length > 0) {
            System.arraycopy(objectLiterals, 0, this.referenceLiterals, 0, this.referenceLiterals.length);
        }

        // now copy the code
        System.arraycopy(codeBuffer, 0, this.code, 0, this.code.length);
    }

    public final ClassMethodActor callSiteToCallee(CodePointer callSite) {
        final int callPos = callSite.minus(codeStart).toInt();
        int dcIndex = 0;
        for (int i = 0; i < safepoints.size(); i++) {
            if (safepoints.isSetAt(DIRECT_CALL, i)) {
                if (safepoints.causePosAt(i) == callPos && directCallees[dcIndex] instanceof ClassMethodActor) {
                    return (ClassMethodActor) directCallees[dcIndex];
                }
                dcIndex++;
            }
        }
        final boolean lockDisabledSafepoints = Log.lock();
        Log.print("Could not find callee in ");
        Log.print(this);
        Log.print(" for call site: ");
        Log.print(callSite.toHexString());
        Log.print(" [");
        Log.print(codeStart());
        Log.print(" + ");
        Log.print(callPos);
        Log.println("]");
        dcIndex = 0;
        for (int i = 0; i < safepoints.size(); i++) {
            if (safepoints.isSetAt(DIRECT_CALL, i)) {
                if (safepoints.causePosAt(i) == callPos) {
                    Log.print("* ");
                } else {
                    Log.print("  ");
                }
                Log.print("safepoint # ");
                Log.print(i);
                Log.print(", direct call # ");
                Log.print(dcIndex);
                Log.print(" ");
                Log.println(directCallees[dcIndex]);
                dcIndex++;
            }
        }
        Log.unlock(lockDisabledSafepoints);
        FatalError.breakpoint();
        throw FatalError.unexpected("could not find callee for call site: " + callSite.toHexString());
    }

    /**
     * Returns an absolute address in the machine code of this method, corresponding to the kind of entry point
     * passed as parameter.
     *
     * Note that this method returns an absolute code address. Its usage should occur soon after the invocation of
     * this method, without any safepoints in between. At safepoints, code eviction (see {@link CodeEviction}) might
     * take place, potentially invalidating absolute code addresses.
     *
     * @param callEntryPoint the kind of {@link CallEntryPoint} for which the address is to be returned
     * @return the entry address ({@link CodePointer}) in this method according to the {@code callEntryPoint}
     */
    public CodePointer getEntryPoint(CallEntryPoint callEntryPoint) {
        return callEntryPoint.in(this);
    }

    /**
     * Resets a direct call site, make it point to the static trampoline again.
     *
     * @param safepointIndex the index of the call in {@link #safepoints() safepoints}
     * @param dcIndex the index of the call in {@link #directCallees()}
     *
     * @return {@code true} if the call site was not already pointing to the static trampoline
     */
    public final boolean resetDirectCall(int safepointIndex, int dcIndex) {
        Object callee = directCallees[dcIndex];
        assert !(callee instanceof Adapter);
        final int offset = getCallEntryOffset(callee, safepointIndex);
        final int callPos = safepoints.causePosAt(safepointIndex);
        CodePointer trampoline = vm().stubs.staticTrampoline().codeAt(offset);
        return !patchCallSite(callPos, trampoline).equals(trampoline);
    }

    /**
     * Patches the entry point(s) of this target method with direct jump(s) to the
     * corresponding entry points of {@code tm}.
     * <p>
     * <b>This operation can only be performed when at a global safepoint as the patching
     *    is not guaranteed to be atomic.</b>
     *
     * @param tm the target of the jump instruction(s) to be patched in
     */
    public void redirectTo(TargetMethod tm) {
        throw FatalError.unexpected("Cannot patch entry points of " + getClass().getSimpleName() + " " + this);
    }

    /**
     * Links all the calls from this target method to other methods for which the exact method actor is known. Linking a
     * call means patching the operand of a call instruction that specifies the address of the target code to call. In
     * the case of a callee for which there is no target code available (i.e. it has not yet been compiled or it has
     * been evicted from the code cache), the address of a static trampoline is patched
     * into the call instruction.
     *
     * @return true if target code was available for all the direct callees
     */
    public final boolean linkDirectCalls() {
        boolean linkedAll = true;
        if (directCallees.length != 0) {
            int dcIndex = 0;
            for (int safepointIndex = safepoints.nextDirectCall(0); safepointIndex >= 0; safepointIndex = safepoints.nextDirectCall(safepointIndex + 1)) {
                Object currentDirectCallee = directCallees[dcIndex];
                final int offset = getCallEntryOffset(currentDirectCallee, safepointIndex);
                if (currentDirectCallee == null) {
                    // template call
                    assert classMethodActor.isTemplate();
                } else if (MaxineVM.isHosted()) {
                    final TargetMethod callee = getTargetMethod(currentDirectCallee);
                    if (callee == null) {
                        if (classMethodActor.isTemplate()) {
                            assert currentDirectCallee == classMethodActor : "unlinkable call in a template must be a template call";
                            // leave call site unpatched
                        } else {
                            linkedAll = false;
                            patchStaticTrampoline(safepointIndex, offset);
                        }
                    } else {
                        int callPos = safepoints.causePosAt(safepointIndex);
                        fixupCallSite(callPos, callee.codeAt(offset));
                    }
                } else {
                    FatalError.breakpoint();
                    final TargetMethod callee = getTargetMethod(currentDirectCallee);
                    if (callee == null || (!Code.bootCodeRegion().contains(callee.codeStart) && !(callee instanceof Adapter))) {
                        linkedAll = false;
                        patchStaticTrampoline(safepointIndex, offset);
                    } else {
                        int callPos = safepoints.causePosAt(safepointIndex);
                        fixupCallSite(callPos, callee.codeAt(offset));
                    }
                }
                dcIndex++;
            }
        }

        return linkedAll;
    }

    private void patchStaticTrampoline(final int safepointIndex, final int offset) {
        final int callPos = safepoints.causePosAt(safepointIndex);
        final CodePointer callSite = codeAt(callPos);
        if (!isPatchableCallSite(callSite)) {
            FatalError.unexpected(classMethodActor + ": call site calling static trampoline must be patchable: 0x" + callSite.toHexString() +
                            " [0x" + codeStart.toHexString() + "+" + callPos + "]");
        }
        fixupCallSite(callPos, vm().stubs.staticTrampoline().codeAt(offset));
    }

    public final TargetMethod getTargetMethod(Object o) {
        TargetMethod result = null;
        if (o instanceof ClassMethodActor) {
            ClassMethodActor cma = (ClassMethodActor) o;
            if (cma == classMethodActor) {
                // recursive call
                return this;
            }
            result = cma.currentTargetMethod();
        } else if (o instanceof TargetMethod) {
            result = (TargetMethod) o;
        }
        return result;
    }

    private int getCallEntryOffset(Object callee, int safepointIndex) {
        if (callee instanceof Adapter) {
            return 0;
        }
        final CallEntryPoint callEntryPoint = callEntryPointForDirectCall(safepointIndex);
        return callEntryPoint.offsetInCallee();
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
     * @see Safepoints
     */
    public final Safepoints safepoints() {
        return safepoints;
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
    public int posFor(CodePointer ip) {
        final int pos = (int) (ip.toLong() - codeStart.toLong());
        if (pos >= 0 && pos <= code.length) {
            return pos;
        }
        return -1;
    }

    /**
     * Gets a mapping from bytecode positions to target code positions. The bytecode positions are in terms of
     * the bytecode for this target method's {@link #classMethodActor}.
     * A non-zero value
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

    public int findSafepointIndex(CodePointer ip) {
        final int pos = posFor(ip);
        if (safepoints == null || pos < 0 || pos > code.length) {
            return -1;
        }
        return findSafepointIndex(pos);
    }

    public int findSafepointIndex(int pos) {
        return safepoints.indexOf(pos);
    }

    @Override
    public final String toString() {
        return (classMethodActor == null) ? regionName() : classMethodActor.format("%H.%n(%p)");
    }

    public String name() {
        return regionName();
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

    /**
     * Modifies the call site at the specified offset to use the new specified entry point.
     * The modification must tolerate the execution of the target method by concurrently running threads.
     *
     * @param callOffset offset to a call site relative to the start of the code of this target method
     * @param callEntryPoint entry point the call site should call after patching
     * @return the entry point of the call prior to patching
     */
    public abstract CodePointer patchCallSite(int callOffset, CodePointer callEntryPoint);

    /**
     * Fixup a call site in the method. This differs from the above in that the call site is updated before
     * any thread can see it. Thus there isn't any concurrency between modifying the call site and threads
     * trying to run it.
     *
     * @param callOffset offset to a call site relative to the start of the code of this target method
     * @param callEntryPoint entry point the call site should call after fixup
     * @return the entry point of the call prior to patching
     */
    public abstract CodePointer fixupCallSite(int callOffset, CodePointer callEntryPoint);

    /**
     * Indicates whether a call site can be patched safely when multiple threads may execute this target method concurrently.
     * @param callSite offset to a call site relative to the start of the code of this target method.
     * @return true if mt-safe patching is possible on the specified call site.
     */
    public abstract boolean isPatchableCallSite(CodePointer callSite);

    /**
     * Prepares the reference map for the current frame (and potentially for registers stored in a callee frame).
     *
     * @param current the current stack frame
     * @param callee the callee stack frame (ignoring any interposing {@linkplain Adapter adapter} frame)
     * @param preparer the reference map preparer which receives the reference map
     */
    public abstract void prepareReferenceMap(StackFrameCursor current, StackFrameCursor callee, FrameReferenceMapVisitor preparer);

    /**
     * Attempts to catch an exception thrown by this method or a callee method. If a handler exists,
     * then the stack is unwound and execution is resumed at the handler.
     * <p>
     * In the case that is an {@link #invalidated() invalidated} method, the same unwinding
     * occurs but executed is redirected to an appropriate deoptimization stub.
     * The value of {@code current.ip()} is saved in the {@linkplain Deoptimization#DEOPT_RETURN_ADDRESS_OFFSET
     * rescue} slot. This is required so that the frame state associated with the call site is used when
     * deoptimizing. This frame state matches the state on entry to the handler
     * except that the operand stack is cleared (the exception object is explicitly retrieved and pushed by
     * the handler).
     *
     * @param current the current stack frame
     * @param callee the callee stack frame (ignoring any interposing {@linkplain Adapter adapter} frame)
     * @param throwable the exception thrown
     */
    public abstract void catchException(StackFrameCursor current, StackFrameCursor callee, Throwable throwable);

    /**
     * Similar to {@link #catchException} but simply checks if there is a handler for {@code exception}
     * and returns its code address if so, otherwise {@link CodePointer#zero}.
     * @param throwAddress the throw address
     */
    public abstract CodePointer throwAddressToCatchAddress(CodePointer throwAddress, Throwable throwable);

    public static class CatchExceptionInfo {
        public CodePointer codePointer;
        public int bci;
    }

    /**
     * Similar to {@link #catchException} save that the stack is not unwound, and just the info on the catch
     * location is returned.
     * @param current
     * @param throwable
     * @param info instance in which to store the info
     * @return {@code true} iff the exception is handled by this method
     */
    public boolean catchExceptionInfo(StackFrameCursor current, Throwable throwable, CatchExceptionInfo info) {
        return false;
    }

    /**
     * Accepts a visitor for this stack frame. As this only ever happens in Inspector contexts, this method is
     * annotated with {@link HOSTED_ONLY}.
     *
     * @param current the current stack frame
     * @param visitor the visitor which will visit the frame
     * @return {@code true} if the visitor indicates the stack walk should continue
     */
    @HOSTED_ONLY
    public abstract boolean acceptStackFrameVisitor(StackFrameCursor current, StackFrameVisitor visitor);

    /**
     * Advances the stack frame cursor from this frame to the next frame.
     * @param current the current stack frame cursor
     */
    public abstract void advance(StackFrameCursor current);

    /**
     * Gets a pointer to the memory word holding the return address in a frame of this target method.
     *
     * @param frame an activation frame for this target method
     */
    public abstract Pointer returnAddressPointer(StackFrameCursor frame);

    /**
     * Finalize reference maps if necessary.
     */
    public void finalizeReferenceMaps() {

    }

    /**
     * Determines if this a {@link Nature#BASELINE} target method.
     */
    public boolean isBaseline() {
        return false;
    }

    /**
     * Determines if this method has been instrumented by a {@link VMTIHandler tooling interface}.
     */
    public boolean isInstrumented() {
        return false;
    }

    /**
     * Creates a deoptimized frame for this method. This can only be called if {@link #isBaseline()}
     * returns {@code true} for this object.
     *
     * @param info details of current deoptimization
     * @param frame debug info from which the slots of the deoptimized are initialized
     * @param callee used to notify callee of the execution state the deoptimized frame when it is returned to
     * @param exception if non-null, this is an in-flight exception that must be handled by the deoptimized frame
     * @param reexecute specifies if the instruction at {@code frame.bci} is to be re-executed (ignored if {@code exception != null})
     * @return object for notifying the deoptimized frame's caller of continuation state
     */
    public Continuation createDeoptimizedFrame(Info info, CiFrame frame, Continuation callee, Throwable exception, boolean reexecute) {
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
     * Determines whether this method has a type profile.
     *
     * @return {@code true} if there is a type profile associated with this method.
     */
    public boolean hasTypeProfile() {
        return profile() != null && profile().rawInfo() != null;
    }

    /**
     * Determines whether the entry count of this method is within the threshold
     * denoted by {@link MethodInstrumentation#PROTECTION_PERCENTAGE}.
     */
    public boolean withinInvocationThreshold() {
        return profile() != null && profile().protectedEntryCount();
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
     * Invalidate this method's machine code and literals by letting them reference "wiped" sentinels.
     */
    public void wipe() {
        code = WIPED_CODE;
        scalarLiterals = WIPED_SCALAR_LITERALS;
        referenceLiterals = WIPED_REFERENCE_LITERALS;
    }

    public boolean isWiped() {
        return code == WIPED_CODE;
    }

    /**
     * Determines if this is a stub of a given type.
     */
    public final boolean is(Stub.Type type) {
        return stubType() == type;
    }

    /**
     * Sentinel value for the {@link #code} field denoting that it is no longer valid.
     * <p>
     * <strong>Note:</strong> The Inspector compares the value in the code field against this
     * sentinel's fixed location to determine whether the method's code has been evicted.
     * That requires that the sentinel's value not change, so it should always be in an
     * unmanaged heap (presumed to be in the boot heap automatically).
     */
    @INSPECTED
    private static final byte[] WIPED_CODE = {};

    /**
     * Sentinel value for the {@link #scalarLiterals} field denoting that it is no longer valid.
     */
    private static final byte[] WIPED_SCALAR_LITERALS = {};

    /**
     * Sentinel value for the {@link #referenceLiterals} field denoting that it is no longer valid.
     */
    private static final Object[] WIPED_REFERENCE_LITERALS = {};

    /**
     * Marks this target method for preservation during {@linkplain CodeEviction code eviction}.
     */
    public final void mark() {
        oldStart = Address.allOnes().asAddress();
    }

    public final void unmark() {
        oldStart = Address.zero();
    }

    /**
     * Determines if this method was marked to survive {@linkplain CodeEviction code eviction}.
     */
    public final boolean isMarked() {
        return oldStart.equals(Address.allOnes().asAddress());
    }

    /**
     * Determines if this method is protected from eviction.
     */
    public boolean isProtected() {
        return false;
    }

    /**
     * Protect this method from eviction.
     * This is not supported for all kinds of target methods.
     */
    public void protect() {
        // intentionally empty
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
