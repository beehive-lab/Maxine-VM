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
import static com.sun.max.vm.VMConfiguration.*;

import java.io.*;
import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.cri.bytecode.Bytes;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.io.*;
import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.MaxMachineCode.InstructionMap;
import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.method.CodeLocation.MachineCodeLocation;
import com.sun.max.tele.method.*;
import com.sun.max.tele.type.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.TargetMethod.FrameAccess;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for some flavor of {@link TargetMethod}, which is a compilation of a Java {@link ClassMethod} in
 * the VM.
 * <p>
 * When this surrogate is first created, it records only the location of the {@link TargetMehod} in VM memory. This
 * limitation keeps the overhead low, since an instance of this class is eagerly created for every compilation
 * discovered in the VM. It also avoids creating any other instances of {@link TeleObject}, which can lead to infinite
 * regress in the presence of mutually referential objects, notably with instances of {@link TeleClassMethodActor}.
 * <p>
 * The first time this object is refreshed, it gets an instance of {@link TeleClassMethodActor} that refers to the
 * {@link ClassMethodActor} in the VM that owns the compilation represented by this object.
 * <p>
 * The full contents of the {@link TargetMethod} in the VM are loaded, disassembled, and cached lazily: only when
 * needed. The caches are flushed eagerly, whenever an update determines that the code has been changed (i.e. patched),
 * but the new contents are only loaded lazily, as needed.
 * <p>
 * Content loading is performed by (restricted) deep copying the {@link TargetMethod} from the VM, and caching the local
 * instance.
 * <p>
 * <strong>Important</strong>: this implementation assumes that compilations in the VM, once created, <strong>do not
 * move in memory</strong> and <strong>are never evicted</strong>.
 * @see TeleClassMethodActor
 */
public class TeleTargetMethod extends TeleRuntimeMemoryRegion implements TargetMethodAccess {

    private static final int TRACE_VALUE = 2;
    private static final List<TargetCodeInstruction> EMPTY_TARGET_INSTRUCTIONS =  Collections.emptyList();
    private static final MachineCodeLocation[] EMPTY_MACHINE_CODE_LOCATIONS = {};
    private static final CodeStopKind[] EMPTY_CODE_STOP_KINDS = {};
    private static final CiDebugInfo[] EMPTY_DEBUG_INFO_MAP = {};
    private static final int[] EMPTY_INT_ARRAY = {};
    private static final RiMethod[] EMPTY_METHOD_ARRAY = {};
    private static final List<Integer> EMPTY_INTEGER_LIST = Collections.emptyList();

    /**
     * Reason that a particular instruction is identified as a "Stop".
     *
     * @see TargetMethod
     * @see StopPositions
     */
    private enum CodeStopKind {
        DIRECT_CALL,   // Non-native direct call
        NATIVE_CALL,   // Native direct call
        INDIRECT_CALL, // Indirect call
        SAFE;          // Safepoint
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
        private final TargetMethod targetMethod;

        /**
         * The version number of this cache, with a new one presumed to be
         * created each time the code in the VM is discovered to have changed.  Note that this might
         * not agree with the actual number of times the code has changed, since it may have changed
         * more than once in a single VM execution cycle.
         */
        private final int codeGenerationCount;

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
         * Map:  target instruction index -> the kind of stop at the instruction, null if not a stop.
         */
        private final CodeStopKind[] indexToCodeStopKind;

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
         * Map: target instruction index -> constant pool index of {@Link MethodRefConstant} if this is a call instruction; else -1.
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
         * @param targetMethod a local copy of the {@link TargetMethod} from the VM.
         * @param generationCount number of this cache in the sequence of caches: 0 = no information; 1 VM's initial state.
         * @param teleClassMethodActor access to the {@link ClassMethodActor} in the VM of which this {@link TargetMethod}
         * is a compilation.
         */
        private TargetMethodCache(TargetMethod targetMethod, int generationCount, TeleClassMethodActor teleClassMethodActor) {
            assert (targetMethod == null && generationCount == 0) || (targetMethod != null && generationCount > 0);
            this.targetMethod = targetMethod;
            this.codeGenerationCount = generationCount;

            if (targetMethod == null) {
                this.code = null;
                this.codeStart = Pointer.zero();
                this.inlineDataDecoder = null;
                this.callEntryPoint = Address.zero();

                this.instructions = EMPTY_TARGET_INSTRUCTIONS;
                this.instructionCount = 0;
                this.bciToPosMap = null;
                this.indexToLocation = EMPTY_MACHINE_CODE_LOCATIONS;
                this.indexToCodeStopKind = EMPTY_CODE_STOP_KINDS;
                this.indexToDebugInfoMap = EMPTY_DEBUG_INFO_MAP;
                this.indexToOpcode = EMPTY_INT_ARRAY;
                this.indexToCallee = EMPTY_METHOD_ARRAY;
                this.labelIndexes = EMPTY_INTEGER_LIST;
            } else {
                // The size of the compilation's machine code in bytes.
                final int targetCodeLength = targetMethod.codeLength();

                this.code = targetMethod.code();
                this.codeStart = targetMethod.codeStart();

                if (codeStart.isZero()) {
                    this.callEntryPoint = Address.zero();
                } else {
                    Address callEntryAddress = codeStart;
                    if (vmConfig().needsAdapters()) {
                        final Reference callEntryPointReference = vm().teleFields().TargetMethod_callEntryPoint.readReference(reference());
                        final TeleObject teleCallEntryPoint = heap().makeTeleObject(callEntryPointReference);
                        if (teleCallEntryPoint != null) {
                            final CallEntryPoint callEntryPoint = (CallEntryPoint) teleCallEntryPoint.deepCopy();
                            if (callEntryPoint != null) {
                                callEntryAddress = codeStart.plus(callEntryPoint.offset());
                            }
                        }
                    }
                    this.callEntryPoint = callEntryAddress;
                }

                inlineDataDecoder = targetMethod.inlineDataDecoder();

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

                // Stop position locations in compiled code (by byte offset from the start):
                // calls and safepoints, null if not available

                int[] targetStopPositions = targetMethod.stopPositions();
                final StopPositions stopPositions =
                    (targetStopPositions == null) ? null : new StopPositions(targetStopPositions);

                // Build map:  target instruction position (bytes offset from start) -> the kind of stop, null if not a stop.
                CodeStopKind[] posToStopKindMap = null;
                if (stopPositions != null) {
                    posToStopKindMap = new CodeStopKind[targetCodeLength];

                    final int directCallCount = targetMethod.numberOfDirectCalls();
                    final int indirectCallCount = targetMethod.numberOfIndirectCalls();
                    final int safepointCount = targetMethod.numberOfSafepoints();
                    assert directCallCount + indirectCallCount + safepointCount == stopPositions.length();

                    for (int stopIndex = 0; stopIndex < directCallCount; stopIndex++) {
                        if (stopPositions.isNativeFunctionCall(stopIndex)) {
                            posToStopKindMap[stopPositions.get(stopIndex)] = CodeStopKind.NATIVE_CALL;
                        } else {
                            posToStopKindMap[stopPositions.get(stopIndex)] = CodeStopKind.DIRECT_CALL;
                        }
                    }
                    for (int stopIndex = directCallCount; stopIndex < directCallCount + indirectCallCount; stopIndex++) {
                        posToStopKindMap[stopPositions.get(stopIndex)] = CodeStopKind.INDIRECT_CALL;
                    }
                    for (int stopIndex = directCallCount + indirectCallCount; stopIndex < stopPositions.length(); stopIndex++) {
                        posToStopKindMap[stopPositions.get(stopIndex)] = CodeStopKind.SAFE;
                    }
                }

                // Get the precise map between bytecode and machine code instructions, null if not available
                this.bciToPosMap = targetMethod.bciToPosMap();

                 // Build map:  target instruction position (bytes offset from start) -> debug infos (as much as can be determined).
                final CiDebugInfo[] posToDebugInfoMap = new CiDebugInfo[targetCodeLength];
                if (stopPositions != null) {
                    for (int stopIndex = 0; stopIndex < stopPositions.length(); ++stopIndex) {
                        posToDebugInfoMap[stopPositions.get(stopIndex)] = getDebugInfoAtStopIndex(stopIndex);
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
                            CiFrame frame = new CiFrame(null, classMethodActor(), bci, new CiValue[0], 0, 0, 0);
                            posToDebugInfoMap[pos] = new CiDebugInfo(frame, null, null);
                            do {
                                ++bci;
                            } while (bci < bciToPosMap.length && (bciToPosMap[bci] == 0 || bciToPosMap[bci] == pos));
                        }
                    }
                }

                // Now build maps based on target instruction index
                indexToLocation = new MachineCodeLocation[instructionCount];
                indexToCodeStopKind = new CodeStopKind[instructionCount];
                indexToDebugInfoMap = new CiDebugInfo[instructionCount];
                indexToOpcode = new int[instructionCount];
                Arrays.fill(indexToOpcode, -1);
                indexToCallee = new RiMethod[instructionCount];

                // Also build list of instruction indices where there are labels
                final List<Integer> labels = new ArrayList<Integer>();

                int bci = 0; // position cursor in the original bytecode stream, used if we have a bytecode -> target code map
                for (int instructionIndex = 0; instructionIndex < instructionCount; instructionIndex++) {
                    final TargetCodeInstruction instruction = instructions.get(instructionIndex);
                    indexToLocation[instructionIndex] = codeManager().createMachineCodeLocation(instruction.address, "native target code instruction");
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

                    if (posToStopKindMap != null) {
                        final CodeStopKind codeStopKind = posToStopKindMap[pos];
                        if (codeStopKind != null) {
                            // We're at a stop
                            indexToCodeStopKind[instructionIndex] = codeStopKind;
                            CiDebugInfo info = indexToDebugInfoMap[instructionIndex];
                            final CiCodePos codePos = info == null ? null : info.codePos;
                            // TODO (mlvdv) only works for non-inlined calls
                            if (codePos != null && codePos.bci >= 0) {
                                ClassMethodActor method = (ClassMethodActor) codePos.method;
                                indexToCallee[instructionIndex] =  method.codeAttribute().calleeAt(codePos.bci);
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

        public boolean isStop(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructionCount) {
                throw new IllegalArgumentException();
            }
            return indexToCodeStopKind[index] != null;
        }

        public boolean isCall(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructionCount) {
                throw new IllegalArgumentException();
            }
            final CodeStopKind stopKind = indexToCodeStopKind[index];
            return stopKind != null && stopKind != CodeStopKind.SAFE;
        }

        public boolean isNativeCall(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructionCount) {
                throw new IllegalArgumentException();
            }
            final CodeStopKind stopKind = indexToCodeStopKind[index];
            return stopKind == CodeStopKind.NATIVE_CALL;
        }

        public boolean isBytecodeBoundary(int index) throws IllegalArgumentException {
            assert targetMethod != null;
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
            return targetMethod != null;
        }

        /**
         * Gets the debug info available for a given stop index.
         *
         * @param stopIndex a stop index
         * @return the debug info available for {@code stopIndex} or null if there is none
         * @see TargetMethod#debugInfoAt(int, FrameAccess)
         */
        private CiDebugInfo getDebugInfoAtStopIndex(final int stopIndex) {
            return TeleClassRegistry.usingTeleClassIDs(new Function<CiDebugInfo>() {
                @Override
                public CiDebugInfo call() throws Exception {
                    return targetMethod.debugInfoAt(stopIndex, null);
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
    public static List<TeleTargetMethod> get(TeleVM vm, MethodKey methodKey) {
        TeleClassActor teleClassActor = vm.classRegistry().findTeleClassActor(methodKey.holder());
        if (teleClassActor != null) {
            final List<TeleTargetMethod> result = new LinkedList<TeleTargetMethod>();
            for (TeleClassMethodActor teleClassMethodActor : teleClassActor.getTeleClassMethodActors()) {
                if (teleClassMethodActor.compilationCount() > 0) {
                    ClassMethodActor classMethodActor = teleClassMethodActor.classMethodActor();
                    if (classMethodActor.name.equals(methodKey.name()) && classMethodActor.descriptor.equals(methodKey.signature())) {
                        for (int i = 0; i < teleClassMethodActor.compilationCount(); ++i) {
                            TeleTargetMethod teleTargetMethod = teleClassMethodActor.getCompilation(i);
                            if (teleTargetMethod != null) {
                                result.add(teleTargetMethod);
                            }
                        }
                    }
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    private TeleClassMethodActor teleClassMethodActor = null;

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
     * Counter for the versions of the {@link TargetMethod} during its lifetime in the VM, starting with
     * its original version, which we call 1. Note that the initial (null) instance of the cache
     * is set to generation 0, which means that the cache begins life as not current.  This is important
     * so that we do perform expensive derivations on the code that are needed far less often than
     * simple meta information about the code.
     */
    private int vmCodeGenerationCount = 1;

    protected TeleTargetMethod(TeleVM vm, Reference targetMethodReference) {
        super(vm, targetMethodReference);

        // Delay initialization of classMethodActor because of the circularity that
        // the compilation history of the classMethodActor refers to this.

        // Register every method compilation, so that they can be located by code address.
        // Note that this depends on the basic location information already being read by
        // superclass constructors.
        vm.codeCache().register(this);
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
                final TargetMethod targetMethod = (TargetMethod) deepCopy();
                targetMethodCache = new TargetMethodCache(targetMethod, vmCodeGenerationCount, teleClassMethodActor);
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

    /**
     * @return whether the current cache is up to date with the state of the {@link TargetMethod}
     * in the VM.
     */
    private boolean isCurrent() {
        return !targetMethodCache.codeStart.isZero() && targetMethodCache.codeGenerationCount == vmCodeGenerationCount;
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
        if (teleClassMethodActor == null) {
            final Reference classMethodActorReference = vm().teleFields().TargetMethod_classMethodActor.readReference(reference());
            teleClassMethodActor = (TeleClassMethodActor) heap().makeTeleObject(classMethodActorReference);
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
            final Reference byteArrayReference = vm().teleFields().TargetMethod_code.readReference(reference());
            final TeleArrayObject teleByteArrayObject = (TeleArrayObject) heap().makeTeleObject(byteArrayReference);
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
    public final boolean isLoaded() {
        return targetMethodCache.isLoaded();
    }

    /**
     * @return count of the generation of the {@link TargetMethod} in the VM, as of the last
     * check.
     */
    public final int vmCodeGenerationCount() {
        return vmCodeGenerationCount;
    }

    /**
     * @return a local copy of the {@link TargetMethod} in the VM, the most recent generation.
     */
    public final TargetMethod targetMethod() {
        return targetMethodCache().targetMethod;
    }

    /**
     * @return surrogate for the {@link ClassMethodActor} in the VM for which this code was compiled.
     */
    public final TeleClassMethodActor getTeleClassMethodActor() {
        return teleClassMethodActor;
    }

    public final int compilationIndex() {
        return teleClassMethodActor == null ? 0 : teleClassMethodActor.compilationIndexOf(this);
    }

    public final InstructionMap getInstructionMap() {
        return targetMethodCache();
    }

    /**
     * Gets VM memory location of the first instruction in the method.
     *
     * @see TargetMethod#codeStart()
     */
    public final Pointer getCodeStart() {
        return targetMethodCache().codeStart;
    }

    /**
     * Gets the call entry memory location in the VM for this method.
     *
     * @return {@link Address#zero()} if this target method has not yet been compiled
     */
    public final Address callEntryPoint() {
        return targetMethodCache().callEntryPoint;
    }

    /**
     * Gets the local mirror of the class method actor associated with this target method. This may be null.
     */
    public final ClassMethodActor classMethodActor() {
        return teleClassMethodActor == null ? null : teleClassMethodActor.classMethodActor();
    }

    public int[] bciToPosMap() {
        return targetMethodCache().bciToPosMap;
    }

    /**
     * Gets the debug info available for a given stop index.
     *
     * @param stopIndex a stop index
     * @return the debug info available for {@code stopIndex} or null if there is none
     * @see TargetMethod#debugInfoAt(int, FrameAccess)
     */
    public CiDebugInfo getDebugInfoAtStopIndex(final int stopIndex) {
        return targetMethodCache().getDebugInfoAtStopIndex(stopIndex);
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

    /** {@inheritDoc}
     * <p>
   * Assume that a {@link TargetMethod} (method compilation) region does not move (see class comment).
     */
    @Override
    public boolean isRelocatable() {
        return false;
    }

    public void writeSummary(PrintStream printStream) {
        final IndentWriter writer = new IndentWriter(new OutputStreamWriter(printStream));
        writer.println("code for: " + classMethodActor().format("%H.%n(%p)"));
        writer.println("compilation: " + compilationIndex());
        targetMethod().traceBundle(writer);
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
