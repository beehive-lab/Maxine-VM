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
package com.sun.max.tele.object;

import static com.sun.max.platform.Platform.*;

import java.io.*;
import java.util.*;

import com.oracle.max.hcfdis.*;
import com.oracle.max.vm.ext.t1x.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.bytecode.Bytes;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiTargetMethod.CodeAnnotation;
import com.sun.cri.ri.*;
import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.io.*;
import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.MaxMachineCodeRoutine.InstructionMap;
import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.method.CodeLocation.MachineCodeLocation;
import com.sun.max.tele.method.*;
import com.sun.max.tele.reference.*;
import com.sun.max.tele.type.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.TargetMethod.FrameAccess;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for an object of type {@link TargetMethod} in the VM.
 * The object itself is allocated on the VM heap.  It extends {@link MemoryRegion}, by which it describes the
 * area allocated for it in the code cache.  This location can change for compilations that are allocated in
 * managed code cache regions.
 * <p>
 * The {@link TargetMethod} stores a method compilation into its allocated area in the form of three arrays
 * (one or two of which may be omitted) to which the {@link TargetMethod} holds references.
 * <p>
 * When this surrogate is first created, it records, in addition to the reference to the {@link TargetMethod},
 * only a bare minimum of information about the compiled code, mainly just its location in the code cache.  This
 * limitation keeps the overhead low, since an instance of this class is eagerly created for every compilation
 * discovered in the VM. It also avoids creating any other instances of {@link TeleObject}, which can lead to infinite
 * regress in the presence of mutually referential objects, notably with instances of {@link TeleClassMethodActor}.
 * <p>
 * The first time this object is refreshed, it gets an instance of {@link TeleClassMethodActor} that refers to the
 * {@link ClassMethodActor} in the VM that owns the compilation represented by this object.
 * <p>
 * The full contents of the compiled code are copied from the VM, disassembled, and cached lazily: only when
 * needed. The caches are flushed eagerly, whenever an update determines that the code has been changed (i.e. patched),
 * but the new contents are only loaded lazily, as needed.
 * <p>
 * A method compilation is loaded by (restricted) deep copying the {@link TargetMethod} from the VM, and caching the local
 * instance.
 *
 * @see VmCodeCacheAccess
 * @see VmCodeCacheRegion
 * @see TeleClassMethodActor
 */
public final class TeleTargetMethod extends TeleRuntimeMemoryRegion implements TargetMethodAccess {

    private static final int TRACE_VALUE = 2;
    private static final List<TargetCodeInstruction> EMPTY_TARGET_INSTRUCTIONS =  Collections.emptyList();
    private static final MachineCodeLocation[] EMPTY_MACHINE_CODE_LOCATIONS = {};
    private static final Integer[] EMPTY_SAFEPOINTS = {};
    private static final CiDebugInfo[] EMPTY_DEBUG_INFO_MAP = {};
    private static final int[] EMPTY_INT_ARRAY = {};
    private static final RiMethod[] EMPTY_METHOD_ARRAY = {};
    private static final List<Integer> EMPTY_INTEGER_LIST = Collections.emptyList();

    /**
     * The data produced by a compilation is stored into an area of memory allocated
     * from some part of the {@linkplain VmCodeCacheAccess code cache}, where it is stored in
     * standard VM object format.  In particular, it is stored as three contiguous arrays
     * in the code cache allocation, although two of them might be omitted if not needed.
     * <p>
     * The {@link TargetMethod} holds standard object {@link Reference}s to these three
     * arrays.
     * <p>
     * No other kinds of objects should ever appear in a {@linkplain VmCodeCacheRegion
     * code cache region}, so this enum completely describes the possibilities.
     */
    public static enum CodeCacheReferenceKind {
        /**
         * Reference possibly held in a {@link TargetMethod} to an instance of {@code byte[]}
         * in the code cache holding scalar literals needed by the target code.  This will be null
         * in the following situations:
         * <ul>
         * <li>During the creation of a new method compilation, until the code cache area is allocated,
         * the method is compiled, and the scalar literals stored into the code cache;</li>
         * <li>If there are no scalar literals associated with the target code; and</li>
         * <li>If the compilation has been evicted.</li>
         * </ul>
         *
         * @see TargetMethod#scalarLiterals()
         */
        SCALAR_LITERALS,

        /**
         * Reference possibly held in a {@link TargetMethod} to an instance of {@code Object[]}
         * in the code cache holding reference literals needed by the target code. This will be null
         * in the following situations:
         * <ul>
         * <li>During the creation of a new method compilation, until the code cache area is allocated,
         * the method is compiled, and the reference literals stored into the code cache;</li>
         * <li>If there are no reference literals associated with the target code; and</li>
         * <li>If the compilation has been evicted.</li>
         * </ul>
         *
         * @see TargetMethod#referenceLiterals()
         */
        REFERENCE_LITERALS,

        /**
         * Reference possibly held in a {@link TargetMethod} to an instance of {@code byte[]}
         * in the code cache holding the target code.  This will be null
         * in the following situations:
         * <ul>
         * <li>During the creation of a new method compilation, until the code cache area is allocated,
         * the method is compiled, and the code stored into the code cache;</li>
         * <li>If the compilation has been evicted.</li>
         * </ul>
         *
         * @see TargetMethod#code()
         */
        CODE;
    }


    /**
     * Adapter for bytecode scanning that only knows the constant pool
     * index argument of the last method invocation instruction scanned.
     */
    private static final class MethodRefIndexFinder extends BytecodeAdapter  {

        int methodRefIndex = -1;

        public MethodRefIndexFinder reset() {
            methodRefIndex = -1;
            return this;
        }

        @Override
        protected void invokestatic(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokespecial(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokevirtual(int index) {
            methodRefIndex = index;
        }

        @Override
        protected void invokeinterface(int index, int count) {
            methodRefIndex = index;
        }

        public int methodRefIndex() {
            return methodRefIndex;
        }
    }

    /**
     * A cache that encapsulates a {@link TargetMethod} instance copied from the VM, together
     * with derived information that is needed when examining the machine code.
     * <p>
     * The machine code in a {@link TargetMethod} can change in the VM at runtime.
     * There are two general cases where the state of the {@link TargetMethod} in the VM changes:
     * <ol>
     * <li>When a method compilation begins, the code is empty and the code's starting location is set to zero.
     * When the compilation is complete, it is copied into the appropriate place in the code cache and
     * the starting location is assigned.</li>
     * <li>The compiled code can be patched in place, for example when a method call is resolved.</li>
     * </ol>
     * <p>
     * This cache object is effectively immutable, so every method on it is thread-safe.
     * A new cache must be created each time the {@link TeleTargetMethod} object in the VM is
     * discovered to have changed.  This is done lazily.
     * <p>
     * There are three states for this cache:
     * <ul>
     * <li><i>Unloaded, Dirty</i>:  the initial state where {@code targetMethod = null && generation = 0}. This
     * represents the most common case, where we have identified a {@link TargetMethod} in the code cache, but
     * we have as yet had no reason to examine the compiled code itself.</li>
     * <li><i>Loaded, Current</i>: {@code targetMethod != null && generation == TeleTargetMethod.codeGeneration}.
     * In this state, all derived information has been computed and is cached; it is available without
     * need to read from VM memory.</li>
     * <li><i>Loaded, Dirty</i>:  {@code targetMethod != null && generation < TeleTargetMethod.codeGeneration}.
     * In this state, derived cached information is presumed to be out of date (the code in the VM has changed)
     * and unreliable. Every attempt
     * to use the cached information should be preceded by an attempt to reload the cache; if the reload fails
     * then the old information can be used for the time being.</li>
     * </ul>
     * Once a {@link TargetMethod} has been loaded, it remains in the "loaded" state, alternating between "current" and "dirty"
     * as the cached code is compared with the code in the VM during each update cycle.
     * <p>
     * This constructor requires locked access to the VM.
     *
     * @see TargetMethod
     * @see TeleVM#tryLock()
     *
     */
    private final class TargetMethodCache implements InstructionMap {

        /**
         * A limited deep copy of the {@link TargetMethod} in the VM, reflecting its state at some point during
         * its lifetime.
         */
        private final TargetMethod targetMethodCopy;


        /**
         * The version number of this cache, with a new one presumed to be
         * created each time the code in the VM is discovered to have changed.  Note that this might
         * not agree with the actual number of times the code has changed, since it may have changed
         * more than once in a single VM execution cycle.
         * <p>
         * Generation 0 corresponds to the initial state, where the data has not been loaded yet.
         * <p>
         * Generation -1 corresponds to the "evicted" state, where the code has been removed from
         * the code cache and the compilation is no longer available for use.
         */
        private final int codeGenerationCount;

        /**
         * A copy of the machine code for the compilation.
         */
        private final byte[] code;

        /**
         * Location of the first byte of the machine code for the compilation in VM memory.
         *
         * @see TargetMethod#codeStart()
         */
        private final Pointer codeStart;

        /**
         *  For decoding inline data in this target method's code.
         *
         *  @see TargetMethod#inlineDataDecoder()
         */
        private final InlineDataDecoder inlineDataDecoder;

        /**
         * The entry point used for <i>standard</i> calls in this target method to JVM compiled/interpreted code.
         *
         * @see TargetMethod#callEntryPoint
         */
        private final Address callEntryPoint;

        /**
         * Assembly language instructions that have been disassembled from the method's target code.
         */
        private final List<TargetCodeInstruction> instructions;

        /**
         * The number of disassembled instructions in the method.
         */
        private final int instructionCount;

        /**
         *  Map: bytecode positions to target code positions, null map if information not available.
         *
         *  @see TargetMethod#bciToPosMap()
         */
        private final int[] bciToPosMap;

        /**
         *  Map: target instruction index -> location of the instruction in VM memory.
         */
        private final MachineCodeLocation[] indexToLocation;

        /**
         * Map:  target instruction index -> the safepoint at the instruction, null if not a safepoint.
         */
        private final Integer[] indexToSafepoint;

        /**
         * Map: target instruction index -> debug info, if available; else null.
         */
        private final CiDebugInfo[] indexToDebugInfoMap;

        /**
         * Map: target instruction index -> the specific opcode implemented by the group of instructions starting
         * with this one, if known; else empty.
         */
        private final int[] indexToOpcode;

        /**
         * Map: target instruction index -> constant pool index of {@Link MethodRefConstant} if this is a call instruction; else null.
         */
        private final RiMethod[] indexToCallee;

        /**
         * Unmodifiable list of indexes for instructions that are labeled.
         */
        private final List<Integer> labelIndexes;

        private final MethodRefIndexFinder methodRefIndexFinder = new MethodRefIndexFinder();

        /**
         * Create a cache that holds a copy of a {@link TargetMethod} at some stage in its life, along
         * with information derived from that copy.
         * <p>
         * This constructor should be called with the vm lock held.
         *
         * @param targetMethodCopy a local copy of the {@link TargetMethod} from the VM.
         * @param generationCount number of this cache in the sequence of caches: 0 = no information; 1 VM's initial state.
         * @param teleClassMethodActor access to the {@link ClassMethodActor} in the VM of which this {@link TargetMethod}
         * is a compilation.
         */
        private TargetMethodCache(TargetMethod targetMethodCopy, int generationCount, TeleClassMethodActor teleClassMethodActor) {
            assert (targetMethodCopy == null && generationCount <= 0) || (targetMethodCopy != null && generationCount > 0);
            this.targetMethodCopy = targetMethodCopy;
            this.codeGenerationCount = generationCount;

            if (targetMethodCopy == null || targetMethodCopy.codeLength() == 0) {
                // Create a null cache as a placeholder until loading is required.
                // The cache is also null if the target method doesn't have any code yet,
                // which can be observed during the allocation part of target method creation.
                this.code = null;
                this.codeStart = Pointer.zero();
                this.inlineDataDecoder = null;
                this.callEntryPoint = Address.zero();

                this.instructions = EMPTY_TARGET_INSTRUCTIONS;
                this.instructionCount = 0;
                this.bciToPosMap = null;
                this.indexToLocation = EMPTY_MACHINE_CODE_LOCATIONS;
                this.indexToSafepoint = EMPTY_SAFEPOINTS;
                this.indexToDebugInfoMap = EMPTY_DEBUG_INFO_MAP;
                this.indexToOpcode = EMPTY_INT_ARRAY;
                this.indexToCallee = EMPTY_METHOD_ARRAY;
                this.labelIndexes = EMPTY_INTEGER_LIST;
            } else {

                // The size of the compilation's machine code in bytes.
                final int targetCodeLength = targetMethodCopy.codeLength();

                this.code = targetMethodCopy.code();
                this.codeStart = targetMethodCopy.codeStart().toPointer();

                if (codeStart.isZero()) {
                    this.callEntryPoint = Address.zero();
                } else {
                    Address callEntryAddress = codeStart;
                    if (MaxineVM.vm().compilationBroker.needsAdapters()) {
                        final Reference callEntryPointReference = fields().TargetMethod_callEntryPoint.readReference(reference());
                        final TeleObject teleCallEntryPoint = objects().makeTeleObject(callEntryPointReference);
                        if (teleCallEntryPoint != null) {
                            final CallEntryPoint callEntryPoint = (CallEntryPoint) teleCallEntryPoint.deepCopy();
                            if (callEntryPoint != null) {
                                callEntryAddress = codeStart.plus(callEntryPoint.offset());
                            }
                        }
                    }
                    this.callEntryPoint = callEntryAddress;
                }

                CodeAnnotation[] annotations = targetMethodCopy.annotations();
                if (annotations != null && annotations.length > 0) {
                    inlineDataDecoder = HexCodeFileDis.makeInlineDataDecoder(annotations);
                } else {
                    inlineDataDecoder = null;
                }



                // Disassemble the target code
                this.instructions = TeleDisassembler.decode(platform(), codeStart, code, inlineDataDecoder);
                this.instructionCount = this.instructions.size();

                // Get the raw bytecodes from which the method was compiled, if available.
                byte[] bytecodes = null;
                if (teleClassMethodActor != null) {
                    final TeleCodeAttribute teleCodeAttribute = teleClassMethodActor.getTeleCodeAttribute();
                    if (teleCodeAttribute != null) {
                        bytecodes = teleCodeAttribute.readBytecodes();
                    }
                }

                // Safepoint position locations in compiled code (by byte offset from the start): null if not available

                final Safepoints safepoints = targetMethodCopy.safepoints();
                // Build map:  target instruction position (bytes offset from start) -> the safepoint, 0 if not a safepoint.
                int[] posToSafepointMap = null;
                if (safepoints != null && safepoints.size() > 0) {
                    posToSafepointMap = new int[targetCodeLength];
                    for (int safepointIndex = 0; safepointIndex < safepoints.size(); safepointIndex++) {
                        posToSafepointMap[safepoints.posAt(safepointIndex)] = safepoints.safepointAt(safepointIndex);
                    }
                }

                // Get the precise map between bytecode and machine code instructions, null if not available
                this.bciToPosMap = targetMethodCopy.bciToPosMap();

                 // Build map:  target instruction position (bytes offset from start) -> debug infos (as much as can be determined).
                final CiDebugInfo[] posToDebugInfoMap = new CiDebugInfo[targetCodeLength];
                if (safepoints != null && safepoints.size() > 0) {
                    for (int safepointIndex = 0; safepointIndex < safepoints.size(); ++safepointIndex) {
                        posToDebugInfoMap[safepoints.posAt(safepointIndex)] = getDebugInfoAtSafepointIndex(safepointIndex);
                    }
                }
                if (bciToPosMap != null) {
                    int bci = 0; // position cursor in the original bytecode stream, used if we have a bytecode-> machine code map
                    // Iterate over target code instructions, moving along the bytecode cursor to match
                    for (int instructionIndex = 0; instructionIndex < instructionCount; instructionIndex++) {
                        final TargetCodeInstruction instruction = instructions.get(instructionIndex);
                        // offset in bytes of this target code instruction from beginning of the target code
                        final int pos = instruction.position;
                        // To check if we're crossing a bytecode boundary in the machine code,
                        // compare the offset of the instruction at the current row with the offset recorded
                        // for the start of bytecode template.
                        if (bci < bciToPosMap.length && pos == bciToPosMap[bci]) {
                            // This is the start of the machine code block implementing the next bytecode
                            CiFrame frame = new CiFrame(null, classMethodActor(), bci, false, new CiValue[0], 0, 0, 0);
                            posToDebugInfoMap[pos] = new CiDebugInfo(frame, null, null);
                            do {
                                ++bci;
                            } while (bci < bciToPosMap.length && (bciToPosMap[bci] == 0 || bciToPosMap[bci] == pos));
                        }
                    }
                }

                // Now build maps based on target instruction index
                indexToLocation = new MachineCodeLocation[instructionCount];
                indexToSafepoint = new Integer[instructionCount];
                indexToDebugInfoMap = new CiDebugInfo[instructionCount];
                indexToOpcode = new int[instructionCount];
                Arrays.fill(indexToOpcode, -1);
                indexToCallee = new RiMethod[instructionCount];

                // Also build list of instruction indices where there are labels
                final List<Integer> labels = new ArrayList<Integer>();

                int bci = 0; // position cursor in the original bytecode stream, used if we have a bytecode -> target code map
                for (int instructionIndex = 0; instructionIndex < instructionCount; instructionIndex++) {
                    final TargetCodeInstruction instruction = instructions.get(instructionIndex);
                    indexToLocation[instructionIndex] = codeLocationFactory().createMachineCodeLocation(instruction.address, "native target code instruction");
                    if (instruction.label != null) {
                        labels.add(instructionIndex);
                    }

                    // offset in bytes of this machine code instruction from beginning
                    final int pos = instruction.position;

                    // Ensure that the reported instruction position is legitimate.
                    // The disassembler sometimes seems to report wild positions
                    // when disassembling random binary; this can happen when
                    // viewing some unknown native code whose length we must guess.
                    if (pos < 0 || pos >= targetCodeLength) {
                        continue;
                    }

                    indexToDebugInfoMap[instructionIndex] = posToDebugInfoMap[pos];

                    if (posToSafepointMap != null) {
                        final int safepoint = posToSafepointMap[pos];
                        if (safepoint != 0) {
                            // We're at a safepoint
                            indexToSafepoint[instructionIndex] = safepoint;
                            CiDebugInfo info = indexToDebugInfoMap[instructionIndex];
                            final CiCodePos codePos = info == null ? null : info.codePos;
                            if (codePos != null && codePos.bci >= 0) {
                                ClassMethodActor method = (ClassMethodActor) codePos.method;
                                RiMethod callee = method.codeAttribute().calleeAt(codePos.bci);
                                indexToCallee[instructionIndex - 1] = callee;
                            }
                        }
                    }
                    if (bciToPosMap != null) {
                        // Add more information if we have a precise map from bytecode to machine code instructions
                        // To check if we're crossing a bytecode boundary in the JITed code, compare the offset of the instruction at the current row with the offset recorded by the JIT
                        // for the start of bytecode template.
                        if (bci < bciToPosMap.length && pos == bciToPosMap[bci]) {
                            if (bci == bytecodes.length) {
                                indexToOpcode[instructionIndex] = Integer.MAX_VALUE;
                            } else {
                                // This is the start of the machine code block implementing the next bytecode
                                int opcode = Bytes.beU1(bytecodes, bci);
                                if (opcode == Bytecodes.WIDE) {
                                    opcode = Bytes.beU1(bytecodes, bci + 1);
                                }
                                indexToOpcode[instructionIndex] = opcode;
                                // Move bytecode position cursor to start of next instruction
                                do {
                                    ++bci;
                                } while (bci < bciToPosMap.length && (bciToPosMap[bci] == 0 || bciToPosMap[bci] == pos));
                            }
                        }
                    }
                }
                labelIndexes = Collections.unmodifiableList(labels);
            }
        }

        public int length() {
            return instructionCount;
        }

        public TargetCodeInstruction instruction(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructionCount) {
                throw new IllegalArgumentException();
            }
            return instructions.get(index);
        }

        public int findInstructionIndex(Address address) {
            if (instructionCount > 0 && address.greaterEqual(instructions.get(0).address)) {
                for (int index = 1; index < instructionCount; index++) {
                    instructions.get(index);
                    if (address.lessThan(instructions.get(index).address)) {
                        return index - 1;
                    }
                }
                final TargetCodeInstruction lastInstruction = instructions.get(instructionCount - 1);
                if (address.lessThan(lastInstruction.address.plus(lastInstruction.bytes.length))) {
                    return instructionCount - 1;
                }
            }
            return -1;
        }

        public MachineCodeLocation instructionLocation(int index) {
            if (index < 0 || index >= instructionCount) {
                throw new IllegalArgumentException();
            }
            return indexToLocation[index];
        }

        public boolean isSafepoint(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructionCount) {
                throw new IllegalArgumentException();
            }
            return indexToSafepoint[index] != null;
        }

        public boolean isCall(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructionCount) {
                throw new IllegalArgumentException();
            }
            if (index + 1 >= instructionCount) {
                return false;
            }
            final Integer safepoint = indexToSafepoint[index + 1];
            return safepoint != null && Safepoints.isCall(safepoint);
        }

        public boolean isNativeCall(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructionCount) {
                throw new IllegalArgumentException();
            }
            if (index + 1 >= instructionCount) {
                return false;
            }
            final Integer safepoint = indexToSafepoint[index + 1];
            return safepoint != null && Safepoints.NATIVE_CALL.isSet(safepoint);
        }

        public boolean isBytecodeBoundary(int index) throws IllegalArgumentException {
            assert targetMethodCopy != null;
            if (index < 0 || index >= instructionCount) {
                throw new IllegalArgumentException();
            }
            return indexToOpcode[index] >= 0;
        }

        public CiDebugInfo debugInfoAt(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructionCount) {
                throw new IllegalArgumentException();
            }
            return indexToDebugInfoMap[index];
        }

        public int opcode(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructionCount) {
                throw new IllegalArgumentException();
            }
            return indexToOpcode[index];
        }

        public RiMethod calleeAt(int index) {
            if (index < 0 || index >= instructionCount) {
                throw new IllegalArgumentException();
            }
            return indexToCallee[index];
        }

        public List<Integer> labelIndexes() {
            return labelIndexes;
        }

        public int[] bciToMachineCodePositionMap() {
            return bciToPosMap;
        }

        /**
         * @return whether the {@link TargetMethod} being cached has been copied from the VM yet.
         */
        private boolean isLoaded() {
            return targetMethodCopy != null;
        }

        /**
         * Gets the debug info available for a given safepoint.
         *
         * @param safepointIndex a safepoint index
         * @return the debug info available for {@code safepointIndex} or null if there is none
         * @see TargetMethod#debugInfoAt(int, FrameAccess)
         */
        private CiDebugInfo getDebugInfoAtSafepointIndex(final int safepointIndex) {
            return VmClassAccess.usingTeleClassIDs(new Function<CiDebugInfo>() {
                @Override
                public CiDebugInfo call() throws Exception {
                    return targetMethodCopy.debugInfoAt(safepointIndex, null);
                }
            });
        }

        /**
         * @param bytecodes
         * @param bci byte index into bytecodes
         * @return if a call instruction, the index into the constant pool of the called {@link MethodRefConstant}; else -1.
         */
        private int findCalleeCPIndex(byte[] bytecodes, int bci) {
            if (bytecodes == null || bci >= bytecodes.length) {
                return -1;
            }
            final BytecodeScanner bytecodeScanner = new BytecodeScanner(methodRefIndexFinder.reset());
            bytecodeScanner.scanInstruction(bytecodes, bci);
            return methodRefIndexFinder.methodRefIndex();
        }

    }

    /**
     * Gets all target methods that encapsulate code compiled for a given method, either as a top level compilation or
     * as a result of inlining.
     *
     * TODO: Once inlining dependencies are tracked, this method needs to use them.
     *
     * @param vm the VM to search
     * @param methodKey the key denoting a method for which the target methods are being requested
     * @return local surrogates for all {@link TargetMethod}s in the VM that include code compiled for the
     *         method matching {@code methodKey}
     */
    public static List<TeleTargetMethod> get(MaxVM vm, MethodKey methodKey) {
        TeleClassActor teleClassActor = vm.classes().findTeleClassActor(methodKey.holder());
        if (teleClassActor != null) {
            final List<TeleTargetMethod> result = new LinkedList<TeleTargetMethod>();
            for (TeleClassMethodActor teleClassMethodActor : teleClassActor.getTeleClassMethodActors()) {
                if (teleClassMethodActor.compilationCount() > 0) {
                    ClassMethodActor classMethodActor = teleClassMethodActor.classMethodActor();
                    if (classMethodActor.name.equals(methodKey.name()) && classMethodActor.descriptor.equals(methodKey.signature())) {
                        for (TeleTargetMethod teleTargetMethod : teleClassMethodActor.compilations()) {
                            result.add(teleTargetMethod);
                        }
                    }
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    /**
     * A representation of the Java language entity, if any, from which this method
     * was compiled.
     */
    private TeleClassMethodActor teleClassMethodActor = null;

    /**
     * Absolute origin of an array of scalar literals referred to by target code,
     * allocated (if non-empty) in the code cache allocation for this method
     * The location might change if the code cache allocation is moved, or become specially marked as
     * <em>wiped</em> if the compilation does not survive an eviction cycle.  That special marking is
     * done by assignment of a distinguished empty array to the field.
     * <p>
     * This value is null only until the first successful read of their values from the object in the VM.
     *
     * @see CodeCacheReferenceKind#SCALAR_LITERALS
     * @see TargetMethod
     * @see TargetMethod#wipe()
     */
    private Address scalarLiteralArrayOrigin = null;

    /**
     * Absolute origin of an array of reference literals referred to by target code,
     * allocated (if non-empty) in the code cache allocation for this method
     * The location might change if the code cache allocation is moved, or become specially marked as
     * <em>wiped</em> if the compilation does not survive an eviction cycle.  That special marking is
     * done by assignment of a distinguished empty array to the field.
     * <p>
     * This value is null only until the first successful read of their values from the object in the VM.
     *
     * @see CodeCacheReferenceKind#REFERENCE_LITERALS
     * @see TargetMethod
     * @see TargetMethod#wipe()
     */
    private Address referenceLiteralArrayOrigin = null;

    /**
     * Absolute origin of a byte array containing target code,
     * allocated in the code cache allocation for this method
     * The location might change if the code cache allocation is moved, or become specially marked as
     * <em>wiped</em> if the compilation does not survive an eviction cycle.  That special marking is
     * done by assignment of a distinguished empty array to the field.
     * <p>
     * This value is null only until the first successful read of their values from the object in the VM.
     *
     * @see CodeCacheReferenceKind#CODE
     * @see TargetMethod
     * @see TargetMethod#wipe()
     */
    private Address codeByteArrayOrigin = null;

    /**
     * Absolute location in VM memory of the first byte in
     * this compilation's target code.
     */
    private Address codeStartAddress = null;

    /**
     * Absolute location in VM memory immediately
     * after the final byte in this compilation's target code.
     */
    private Address codeEndAddress = null;


    /**
     * The location in VM memory of the fixed sentinel assigned to the
     * code field of the target method when the code is evicted.
     *
     * @see TargetMethod#wipe()
     */
    private static Address codeWipedSentinelAddress = Address.zero();


    /**
     * A representation of the VM's code cache region in which the {@link TargetRegion}
     * is allocated.
     */
    private VmCodeCacheRegion codeCacheRegion = null;

    /**
     * The cache of the actual compiled code in the {@link TargetMethod} is represented as
     * a single immutable object for thread safety.  The cache object encapsulates a copy
     * of the {@link TargetMethod} along with a collection of derived information.
     * <p>
     * A generation count of the cache is kept.  The initial state of the cache is
     * generation 0, contains no {@link TargetMethod}, and is considered to be "unloaded".
     * The initial state of the VM is considered to be generation 1.
     * As soon as the cache object is replaced, the state of the cache is henceforth
     * considered "loaded".
     * <p>
     * The cache is intended to include only the kind of detailed information about the
     * method's machine code that is needed under uncommon circumstances, for example when
     * a user is inspecting the code directly.  The cache is not loaded (and after that
     * reloaded) unless the detailed information is needed.
     * <p>
     * The cache field is protected by the method {@link #targetMethod()}, which refreshes it
     * only if needed.  Only those methods that deal with cache consistency should access
     * this variable directly; all others should use {@link #targetMethodCache}.
     *
     * @see #targetMethodCache
     * @see #isLoaded()
     * @see #isCurrent()
     */
    private volatile TargetMethodCache targetMethodCache = new TargetMethodCache(null, 0, null);

    /**
     * Counter for the versions of the code held by a{@link TargetMethod} during its lifetime
     * in the VM, starting with
     * its original version, which we call 1. Note that the initial (null) instance of the cache
     * is set to generation 0, which means that the cache begins life as not current.  This is important
     * so that we do perform expensive derivations on the code that are needed far less often than
     * simple meta information about the code.
     */
    private int vmCodeGenerationCount = 1;

    /**
     * Counter for the number of completed code evictions that have been seen in this
     * method's code cache region.
     */
    private long evictionCount = 0;

    /**
     * Set permanently to {@code true} when an update detects that the code for this
     * compilation has not survived an eviction cycle.  The test depends in the management
     * being used for the code cache region managing this compilation.
     */
    private boolean evicted = false;

    protected TeleTargetMethod(TeleVM vm, Reference targetMethodReference) {
        super(vm, targetMethodReference);

        // Delay initialization of classMethodActor because of the circularity that
        // the compilation history of the classMethodActor refers to this.

        // Register every method compilation, so that they can be located by code address.
        // Note that this depends on the basic location information already being read by
        // superclass constructors.
        vm.machineCode().registerCompilation(this);
    }

    /**
     * Assigns to the representation of a VM {@link TargetMethod} the representation of
     * the code cache region in which it has been discovered to have been allocated.
     * <p>
     * It is assumed that this is only set once, as target methods are assumed not to
     * move among code cache regions.
     *
     * @param codeCacheRegion a code cache region in the VM
     */
    public void setCodeCacheRegion(VmCodeCacheRegion codeCacheRegion) {
        assert this.codeCacheRegion == null;
        this.codeCacheRegion = codeCacheRegion;
    }

    /**
     * Gets the cache of information information about the {@link TargetMethod} in the VM,
     * attempting to update it if needed.
     *
     * @return the most recent cache of the information that we can get
     */
    private TargetMethodCache targetMethodCache() {
        if (!isCurrent() && vm().tryLock()) {
            try {
                final TargetMethod targetMethodCopy = (TargetMethod) deepCopy();
                targetMethodCache = new TargetMethodCache(targetMethodCopy, vmCodeGenerationCount, teleClassMethodActor);
            } catch (DataIOError dataIOError) {
                if (teleClassMethodActor == null) {
                    Trace.line(TRACE_VALUE, "WARNING: failed to update class actor");
                } else {
                    Trace.line(TRACE_VALUE, "WARNING: failed to update TargetMethod for " + teleClassMethodActor.getName());
                }
            } finally {
                vm().unlock();
            }
        }
        return targetMethodCache;
    }

    private Class compilationClass = null;

    /**
     * @return whether the current cache is up to date with the state of the {@link TargetMethod}
     * in the VM.
     */
    private boolean isCurrent() {
        return targetMethodCache.codeStart.isNotZero() && targetMethodCache.codeGenerationCount == vmCodeGenerationCount;
    }

    private void setEvicted() {
        evicted = true;
        targetMethodCache = new TargetMethodCache(null, -1, null);
    }

    public boolean isEvicted() {
        return evicted;
    }

    /**
     * Determines whether there is machine code in this compilation at a specified memory
     * location in the VM, always {@code false} if this compilation has been evicted or
     * relocated elsewhere.
     *
     * @param address an absolute memory location in the VM.
     * @return whether there is machine code at the address
     * @throws IllegalArgumentException if the location is not within the code cache
     * memory allocated for this compilation.
     */
    public boolean isValidCodeLocation(Address address) throws IllegalArgumentException {
        if (isEvicted()) {
            return false;
        }
        if (!contains(address)) {
            throw new IllegalArgumentException("Address " + address.to0xHexString() + " not in code cache allocation");
        }
        return address.greaterEqual(codeStartAddress) && address.lessThan(codeEndAddress);
    }

    /**
     * Return the absolute origin location of each of the arrays
     * holding compilation data that are stored in the compilation's
     * code cache allocation.
     * <p>
     * In the current implementation, these pointers in the
     * {@link TargetMethod} are set to special statically allocated
     * sentinels ("wiped") when the compilation is evicted.
     */
    public Address codeCacheObjectOrigin(CodeCacheReferenceKind kind) {
        switch (kind) {
            case SCALAR_LITERALS:
                return scalarLiteralArrayOrigin;
            case REFERENCE_LITERALS:
                return referenceLiteralArrayOrigin;
            case CODE:
                return codeByteArrayOrigin;
            default:
                TeleError.unknownCase();
                return null;
        }
    }

    /** {@inheritDoc}
     * <p>
     * Compiled machine code generally doesn't change, so the code and disassembled instructions are cached.
     * This update checks for cases where the code has changed since last seen, i.e. has been patched.
     * <p>
     * If the code has been changed since the last time we saw it, then just make a note that the generation
     * in the VM is one greater than the one cached; this isn't a true generation counter for the code, since
     * it only gets updated when (a) it has been noticed to be different than the cache, and (b) there is a
     * call to get the cache, which triggers a refill of the cache from the VM.
     *
     * @see #targetMethod()
     */
    @Override
    protected boolean updateObjectCache(long epoch, StatsPrinter statsPrinter) {
        if (!super.updateObjectCache(epoch, statsPrinter)) {
            return false;
        }
        if (evicted) {
            return true;
        }
        // TODO (mlvdv) consider optimizing this to avoid the pointer reads if we know there hasn't been a code eviction
        try {
            // Start by updating field caches
            if (teleClassMethodActor == null) {
                final Reference classMethodActorReference = fields().TargetMethod_classMethodActor.readReference(reference());
                teleClassMethodActor = (TeleClassMethodActor) objects().makeTeleObject(classMethodActorReference);
            }
            if (codeWipedSentinelAddress.isZero()) {
                codeWipedSentinelAddress = fields().TargetMethod_WIPED_CODE.readWord(vm()).asAddress();
            }
            // Read some fields using low level machinery to avoid circularity with Reference creation
            if (scalarLiteralArrayOrigin == null || checkForRelocation()) {
                scalarLiteralArrayOrigin = reference().readWord(fields().TargetMethod_scalarLiterals.fieldActor().offset()).asAddress();
            }
            if (referenceLiteralArrayOrigin == null || checkForRelocation()) {
                referenceLiteralArrayOrigin = reference().readWord(fields().TargetMethod_referenceLiterals.fieldActor().offset()).asAddress();
            }
            if (codeByteArrayOrigin == null || checkForRelocation()) {
                codeByteArrayOrigin = reference().readWord(fields().TargetMethod_code.fieldActor().offset()).asAddress();
                // Get the absolute location of all target code bytes.
                // Use low level machinery; we dont' want to create a {@link TeleObject} for every one of them.
                final RemoteTeleReference codeByteArrayRef = referenceManager().makeTemporaryRemoteReference(codeByteArrayOrigin);
                final int length = Layout.readArrayLength(codeByteArrayRef);
                final ArrayLayout byteArrayLayout = Layout.layoutScheme().byteArrayLayout;
                codeStartAddress = codeByteArrayOrigin.plus(byteArrayLayout.getElementOffsetFromOrigin(0));
                codeEndAddress = codeByteArrayOrigin.plus(byteArrayLayout.getElementOffsetFromOrigin(length));
            }
        } catch (DataIOError dataIOError) {
            // If something goes wrong, delay the cache update until next time.
        }
        // See if we have been evicted since last cycle by checking if the code pointer has been "wiped".
        // TODO (mlvdv) optimize by only checking if there has indeed been an eviction cycle completed since the
        // last check, assuming that the last check wasn't *in* an eviction cycle.
        if (codeWipedSentinelAddress.isNotZero() && codeByteArrayOrigin != null && codeByteArrayOrigin.equals(codeWipedSentinelAddress)) {
            setEvicted();
            return true;
        }
        if (!targetMethodCache.isLoaded()) {
            // Don't update if we've never loaded the code; delay that until actually needed.
            return true;
        }
        if (!isCurrent()) {
            // If we've already discovered that the loaded copy is not current, don't bother to
            // check again. It won't be reloaded until needed.
            return true;
        }
        try {
            // Test for a patch to the target code since the last time we looked.
            final Reference byteArrayReference = fields().TargetMethod_code.readReference(reference());
            final TeleArrayObject teleByteArrayObject = (TeleArrayObject) objects().makeTeleObject(byteArrayReference);
            final byte[] codeInVM = (byte[]) teleByteArrayObject.shallowCopy();
            if (!Arrays.equals(codeInVM, targetMethodCache.code)) {
                // The code in the VM is different than in the cache.
                // Set the VM generation count to one more than the cached copy.  This
                // makes the cache not "current", essentially marking it dirty.
                vmCodeGenerationCount = targetMethodCache.codeGenerationCount + 1;
                Trace.line(1, tracePrefix() + "TargetMethod patched for " + getRegionName());
            }
        } catch (DataIOError dataIOError) {
            // If something goes wrong, delay the cache update until next time.
            return false;
        }
        return true;
    }

    /**
     * Determines whether we have ever copied information about the {@link TargetMethod} from
     * the VM, which is done only on demand.
     *
     * @return whether a copy of the {@link TargetMethod} in the VM has been created and cached.
     */
    public boolean isLoaded() {
        return targetMethodCache.isLoaded();
    }

    /**
     * @return count of the generation of the {@link TargetMethod} in the VM, as of the last
     * check.
     */
    public int vmCodeGenerationCount() {
        return vmCodeGenerationCount;
    }

    /**
     * Determines whether this is a baseline compilation; if not, it can be assumed to be an optimized compilation.
     */
    public boolean isBaseline() {
        if (compilationClass == null) {
            compilationClass = classActorForObjectType().javaClass();
        }
        return compilationClass == T1XTargetMethod.class;
    }

    /**
     * @return a local copy of the {@link TargetMethod} in the VM, the most recent generation.
     */
    public TargetMethod targetMethod() {
        return targetMethodCache().targetMethodCopy;
    }

    /**
     * @return surrogate for the {@link ClassMethodActor} in the VM for which this code was compiled.
     */
    public TeleClassMethodActor getTeleClassMethodActor() {
        return teleClassMethodActor;
    }

    public InstructionMap getInstructionMap() {
        return targetMethodCache();
    }

    /**
     * Gets VM memory location of the first instruction in the method.
     *
     * @see TargetMethod#codeStart()
     */
    public Pointer getCodeStart() {
        return targetMethodCache().codeStart;
    }

    /**
     * Gets the call entry memory location in the VM for this method.
     *
     * @return {@link Address#zero()} if this target method has not yet been compiled
     */
    public Address callEntryPoint() {
        return targetMethodCache().callEntryPoint;
    }

    /**
     * Gets the local mirror of the class method actor associated with this target method. This may be null.
     */
    public ClassMethodActor classMethodActor() {
        return teleClassMethodActor == null ? null : teleClassMethodActor.classMethodActor();
    }

    public int[] bciToPosMap() {
        return targetMethodCache().bciToPosMap;
    }

    /**
     * Gets the debug info available for a given safepoint index.
     *
     * @param safepointIndex a safepoint index
     * @return the debug info available for {@code safepointIndex} or null if there is none
     * @see TargetMethod#debugInfoAt(int, FrameAccess)
     */
    public CiDebugInfo getDebugInfoAtSafepointIndex(final int safepointIndex) {
        return targetMethodCache().getDebugInfoAtSafepointIndex(safepointIndex);
    }

    /**
     * Gets the name of the source variable corresponding to a stack slot, if any.
     *
     * @param slot a stack slot
     * @return the Java source name for the frame slot, null if not available.
     */
    public String sourceVariableName(MaxStackFrame.Compiled javaStackFrame, int slot) {
        return null;
    }

    // [tw] Warning: duplicated code!
    public MachineCodeInstructionArray getTargetCodeInstructions() {
        final List<TargetCodeInstruction> instructions = targetMethodCache.instructions;
        final MachineCodeInstruction[] result = new MachineCodeInstruction[instructions.size()];
        for (int i = 0; i < result.length; i++) {
            final TargetCodeInstruction ins = instructions.get(i);
            result[i] = new MachineCodeInstruction(ins.mnemonic, ins.position, ins.address.toLong(), ins.label, ins.bytes, ins.operands, ins.getTargetAddressAsLong());
        }
        return new MachineCodeInstructionArray(result);
    }

    public MethodProvider getMethodProvider() {
        return this.teleClassMethodActor;
    }

    @Override
    public ReferenceTypeProvider getReferenceType() {
        return vm().vmAccess().getReferenceType(getClass());
    }

    // TODO (mlvdv)  the renaming was probably wrong; rethink all this.
    /** {@inheritDoc}
     * <p>
     * A {@link TargetMethod} (method compilation) region does not move (see class comment)
     * unless it is in a managed code cache region; we might not know, however, until we
     * have been told the region in which the code is allocated.
     */
    @Override
    public boolean checkForRelocation() {
        return true;
//        if (codeCacheRegion == null) {
//            // For some reason we don't yet have information about the code cache
//            // region in which this target method is allocated.  Be conservative.
//            return true;
//        }
//        if (!codeCacheRegion.isManaged()) {
//            // In an unmanaged code region, code is assumed to never move.
//            return false;
//        }
//        if (codeCacheRegion.isInEviction()) {
//            // If we are in the middle of an eviction, always check.
//            return true;
//        }
//        if (codeCacheRegion.evictionCount() > evictionCount) {
//            // A new code eviction has completed
//            evictionCount = codeCacheRegion.evictionCount();
//            return true;
//        }
//        return false;
    }

    public void writeSummary(PrintStream printStream) {
        final IndentWriter writer = new IndentWriter(new OutputStreamWriter(printStream));
        writer.println("code for: " + classMethodActor().format("%H.%n(%p)"));
        writer.println("compilation: " + (isBaseline() ? "BASELINE" : "OPTIMIZED"));
        writer.flush();
        final Platform platform = platform();
        final TargetMethodCache tmCache = targetMethodCache();
        final InlineDataDecoder inlineDataDecoder = tmCache.inlineDataDecoder;
        final Address startAddress = tmCache.codeStart;
        final byte[] code = tmCache.code;
        final DisassemblyPrinter disassemblyPrinter = new DisassemblyPrinter(false);
        com.sun.max.asm.dis.Disassembler.disassemble(printStream, code, platform.isa, platform.wordWidth(), startAddress.toLong(), inlineDataDecoder, disassemblyPrinter);
    }

}
