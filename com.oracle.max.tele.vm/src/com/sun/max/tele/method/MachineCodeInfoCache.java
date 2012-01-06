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
package com.sun.max.tele.method;

import static com.sun.max.platform.Platform.*;

import java.io.*;
import java.util.*;

import com.oracle.max.hcfdis.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.bytecode.Bytes;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiTargetMethod.CodeAnnotation;
import com.sun.cri.ri.*;
import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.data.*;
import com.sun.max.tele.method.CodeLocation.MachineCodeLocation;
import com.sun.max.tele.object.*;
import com.sun.max.tele.type.*;
import com.sun.max.tele.util.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.compiler.target.TargetMethod.FrameAccess;
import com.sun.max.vm.reference.*;

/**
 * A cache that encapsulates a {@link TargetMethod} instance copied from the VM, together with derived information that
 * is needed when examining the machine code.
 * <p>
 * The machine code in a {@link TargetMethod} can change in the VM at runtime. There are several cases where the state
 * of the {@link TargetMethod} in the VM changes:
 * <ol>
 * <li>When a method compilation in the VM begins, the {@link TeleTargetMethod}'s {@code code} field is initially null
 * and the starting location is {@link Address#zero()}. When the compilation completes, code and related data are copied
 * into the appropriate place in the code cache, the {@link TargetMethod}'s {@code code} and other fields are set
 * (references into the code cache allocation) and the starting location is assigned.</li>
 * <li>The compiled code can be patched in place, for example when a method call is resolved.</li>
 * <li>The compiled code may be relocated, for example by eviction in a managed code cache region.</li>
 * </ol>
 * <p>
 * This cache object is effectively immutable, so every method on it is thread-safe. A new cache must be created each
 * time the {@link TeleTargetMethod} object in the VM is discovered to have changed. This is done lazily.
 * <p>
 * There are four states for this cache:
 * <ol>
 * <li><i>Unloaded, Dirty</i>: the initial state where
 * {@code codeVersion == 0 && targetMethod == null && isDirty == true}. This represents the most common case, where a
 * {@link TargetMethod} in the code cache is known and registered, but there has been no request to examine the
 * details of the compiled code itself.</li>
 * <li><i>Loaded, not Dirty</i>: {@code codeVersion > 0 && targetMethod != null && isDirty == false}. In this state, all
 * derived information has been computed and is cached; it is available without need to read from VM memory.</li>
 * <li><i>Loaded, Dirty</i>: {@code codeVersion > 0 && targetMethod != null &&  && isDirty == true}. In this state, derived
 * cached information is presumed to be out of date (the code in the VM has been patched, relocated or changed in some
 * other way). Every attempt to use the cached information should be preceded by an attempt to reload the cache; if the
 * reload fails then the old information can be used until the cache successfully refreshes.</li>
 * <li><i>Dead</i>: {@code codeVersion > 0 && targetMethod == null}.  In this state, the compilation has been declared
 * dead, presumably evicted from the VM's code cache.
 * </ol>
 * Once a {@link TargetMethod} has been loaded, it remains in the "loaded" state, alternating between "not dirty" and
 * "dirty" as the cached code is compared with the code in the VM during each update cycle.
 * <p>
 * This constructor requires locked access to the VM.
 *
 * @see TargetMethod
 * @see TeleVM#tryLock()
 *
 */
public final class MachineCodeInfoCache extends AbstractVmHolder {

    private static final int TRACE_VALUE = 2;
    private static final List<TargetCodeInstruction> EMPTY_TARGET_INSTRUCTIONS =  Collections.emptyList();
    private static final MachineCodeLocation[] EMPTY_MACHINE_CODE_LOCATIONS = {};
    private static final Integer[] EMPTY_SAFEPOINTS = {};
    private static final CiDebugInfo[] EMPTY_DEBUG_INFO_MAP = {};
    private static final int[] EMPTY_INT_ARRAY = {};
    private static final RiMethod[] EMPTY_METHOD_ARRAY = {};
    private static final List<Integer> EMPTY_INTEGER_LIST = Collections.emptyList();

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

    public final class TargetMethodMachineCodeInfo implements MaxMachineCodeInfo {

        /**
         * A limited deep copy of the {@link TargetMethod} in the VM, reflecting its state at some point during
         * its lifetime.
         */
        private final TargetMethod targetMethodCopy;


        /**
         * The version number of this cache, with a new one presumed to be created each time the code in the VM is
         * discovered to have patched or relocated. Note that this might not agree with the actual number of times the
         * code has changed, since it may have changed more than once in a single VM execution cycle.
         * <p>
         * Version 0 corresponds to the initial state, where the data has not been loaded yet.
         * <p>
         * Version -1 corresponds to the "evicted" state, where the code has been removed from the code cache and the
         * compilation is no longer available for use.
         */
        private final int codeVersion;

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
         * @param codeVersion number of this cache in the sequence of caches: 0 = no information; 1 VM's initial state.
         * @param teleClassMethodActor access to the {@link ClassMethodActor} in the VM of which this {@link TargetMethod}
         * is a compilation.
         */
        private TargetMethodMachineCodeInfo(TargetMethod targetMethodCopy, int codeVersion, TeleClassMethodActor teleClassMethodActor) {
            this.targetMethodCopy = targetMethodCopy;
            this.codeVersion = codeVersion;

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
                        final Reference callEntryPointReference = fields().TargetMethod_callEntryPoint.readReference(teleTargetMethod.reference());
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
                CodeAttribute codeAttribute = targetMethodCopy.codeAttribute();
                if (codeAttribute != null) {
                    bytecodes = codeAttribute.code();
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
                            CiFrame frame = new CiFrame(null, teleTargetMethod.classMethodActor(), bci, false, new CiValue[0], 0, 0, 0);
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
                try {
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
                } catch (InvalidCodeAddressException e) {
                    TeleError.unexpected("TargetMethod cache loading failed @" + e.getAddressString() + ":  " + e.getMessage());
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

        public int codeVersion() {
            return codeVersion;
        }

        public TargetMethod targetMethod() {
            return targetMethodCopy;
        }

        public Pointer codeStart() {
            return codeStart;
        }

        public Address callEntryPoint() {
            return callEntryPoint;
        }

        public int[] bciToPosMap() {
            return bciToPosMap;
        }

        public List<TargetCodeInstruction> instructions() {
            return instructions;
        }

        public byte[] code() {
            return code;
        }

        /**
         * Gets the debug info available for a given safepoint.
         *
         * @param safepointIndex a safepoint index
         * @return the debug info available for {@code safepointIndex} or null if there is none
         * @see TargetMethod#debugInfoAt(int, FrameAccess)
         */
        public CiDebugInfo getDebugInfoAtSafepointIndex(final int safepointIndex) {
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

    private final TeleTargetMethod teleTargetMethod;

    private volatile TargetMethodMachineCodeInfo machineCodeInfo;

    private boolean isDirty;

    public MachineCodeInfoCache(TeleVM vm, TeleTargetMethod teleTargetMethod) {
        super(vm);
        this.teleTargetMethod = teleTargetMethod;
        this.machineCodeInfo = new TargetMethodMachineCodeInfo(null, 0, null);
        this.isDirty = true;
    }

    public void update(TargetMethod targetMethodCopy, TeleClassMethodActor teleClassMethodActor) {
        machineCodeInfo = new TargetMethodMachineCodeInfo(targetMethodCopy, machineCodeInfo.codeVersion + 1, teleClassMethodActor);
        if (machineCodeInfo.codeStart.isNotZero()) {
            isDirty = false;
        }
    }

    /**
     * Returns an immutable, consistent summary of information about the compilation, attempting
     * to update the summary first if it is known to be out of date.
     *
     * @return the most recent cache of the information that we can get
     */
    public TargetMethodMachineCodeInfo machineCodeInfo() {
        if (isDirty && vm().tryLock()) {
            TeleClassMethodActor teleClassMethodActor = null;
            try {
                final TargetMethod targetMethodCopy = (TargetMethod) teleTargetMethod.deepCopy();
                teleClassMethodActor = teleTargetMethod.getTeleClassMethodActor();
                update(targetMethodCopy, teleClassMethodActor);
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
        return machineCodeInfo;
    }

    /**
     * @return whether the {@link TargetMethod} being cached has been copied from the VM yet.
     */
    public boolean isLoaded() {
        return machineCodeInfo.targetMethodCopy != null;
    }

    public void markDirty() {
        isDirty = true;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void writeSummary(PrintStream printStream) {
        final IndentWriter writer = new IndentWriter(new OutputStreamWriter(printStream));
        writer.println("code for: " + teleTargetMethod.classMethodActor().format("%H.%n(%p)"));
        writer.println("compilation: " + (teleTargetMethod.longDesignator()));
        writer.flush();
        final Platform platform = platform();
        final TargetMethodMachineCodeInfo tmCache = machineCodeInfo;
        final InlineDataDecoder inlineDataDecoder = tmCache.inlineDataDecoder;
        final Address startAddress = tmCache.codeStart;
        final byte[] code = tmCache.code;
        final DisassemblyPrinter disassemblyPrinter = new DisassemblyPrinter(false);
        com.sun.max.asm.dis.Disassembler.disassemble(printStream, code, platform.isa, platform.wordWidth(), startAddress.toLong(), inlineDataDecoder, disassemblyPrinter);
    }

}
