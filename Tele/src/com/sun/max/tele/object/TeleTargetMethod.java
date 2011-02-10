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

import com.sun.cri.bytecode.*;
import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.io.*;
import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.MaxMachineCode.InstructionMap;
import com.sun.max.tele.field.*;
import com.sun.max.tele.method.CodeLocation.MachineCodeLocation;
import com.sun.max.tele.method.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.target.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for some flavor of {@link TargetMethod}, which is a compilation
 * of a Java {@link ClassMethod} in the VM.
 * <br>
 * When this description is first created, it examines only the location of the {@link TargetMehod} in the VM.
 * <br>
 * The contents of the {@link TargetMethod} in the VM are loaded, disassembled, and cached lazily.
 * The caches are flushed whenever
 * an update determines that the code has been changed (i.e. patched).
 * <br>
 * Content loading is performed by (restricted) deep copying the {@link TargetMethod}
 * from the VM, and caching the local instance.
 * <br>
 * <strong>Important</strong>: this implementation assumes that compilations in the VM, once created,
 * <strong>do not move in memory</strong>.
 *
 * @author Michael Van De Vanter
 */
public class TeleTargetMethod extends TeleRuntimeMemoryRegion implements TargetMethodAccess {

    private static final int TRACE_VALUE = 2;
    private static final List<TargetCodeInstruction> EMPTY_TARGET_INSTRUCTIONS =
        Collections.unmodifiableList(new ArrayList<TargetCodeInstruction>(0));
    private static final MachineCodeLocation[] EMPTY_MACHINE_CODE_LOCATIONS = new MachineCodeLocation[0];
    private static final CodeStopKind[] EMPTY_CODE_STOP_KINDS = new CodeStopKind[0];
    private static final BytecodeLocation[] EMPTY_BYTECODE_LOCATIONS = new BytecodeLocation[0];
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final ArrayList<Integer> EMPTY_INTEGER_LIST = new ArrayList<Integer>(0);

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
     * Summary information about a sequence of disassembled machine code instructions
     * that were compiled by the VM from a method, stub, adapter, or other routine.
     * <br>
     * Full initialization is lazy, so that it isn't done for all the methods discovered
     * in the VM, but only those for which we want all the details about the code.
     *
     * @author Michael Van De Vanter
     */
    public class CompiledCodeInstructionMap extends AbstractTeleVMHolder implements InstructionMap {

        private List<TargetCodeInstruction> instructions = EMPTY_TARGET_INSTRUCTIONS;
        private MachineCodeLocation[] instructionLocations = EMPTY_MACHINE_CODE_LOCATIONS;

        /**
         * Map:  target code instruction index -> the kind of stop at the instruction, null if not a stop.
         */
        private CodeStopKind[] codeStopKinds = EMPTY_CODE_STOP_KINDS;

        /**
         * Map: target code instruction index -> bytecode that compiled into code starting at this instruction, if known; else null.
         * The bytecode location may be in a different method that was inlined.
         */
        private BytecodeLocation[] bytecodeLocations = EMPTY_BYTECODE_LOCATIONS;

        /**
         * Map: target code instruction index -> the specific opcode implemented by the group of instructions starting
         * with this one, if known; else null.
         */
        private int[] opcodes = EMPTY_INT_ARRAY;

        /**
         * Map: target code instruction index -> constant pool index of {@Link MethodRefConstant} if this is a call instruction; else -1.
         */
        private int[] callees = EMPTY_INT_ARRAY;

        /**
         * Unmodifiable list of indexes for instructions that are labeled.
         */
        private List<Integer> labelIndexes = EMPTY_INTEGER_LIST;

        private final MethodRefIndexFinder methodRefIndexFinder = new MethodRefIndexFinder();

        public CompiledCodeInstructionMap(TeleVM teleVM) {
            super(teleVM);
        }

        private void initialize() {
            if (instructions.isEmpty()) {
                if (vm().tryLock()) {
                    try {
                        instructions = getInstructions();
                        final int instructionCount = instructions.size();

                        byte[] bytecodes = null;
                        final TeleClassMethodActor teleClassMethodActor = getTeleClassMethodActor();
                        if (teleClassMethodActor != null) {
                            final TeleCodeAttribute teleCodeAttribute = teleClassMethodActor.getTeleCodeAttribute();
                            if (teleCodeAttribute != null) {
                                bytecodes = teleCodeAttribute.readBytecodes();
                            }
                        }

                        // First, gather general information from target method, indexed by
                        // bytecode positions of instructions (byte offset from code start)
                        final int targetCodeLength = getCodeLength();
                        final CodeStopKind[] positionToStopKindMap = getPositionToStopKindMap();
                        final BytecodeLocation[] positionToBytecodeLocationMap = getPositionToBytecodeLocationMap();

                        // Non-null if we have a precise map between bytecode and machine code instructions
                        final int[] bytecodeToMachineCodePositionMap = getBytecodeToMachineCodePositionMap();

                        // Fill in maps indexed by instruction count
                        instructionLocations = new MachineCodeLocation[instructionCount];
                        codeStopKinds = new CodeStopKind[instructionCount];
                        bytecodeLocations = new BytecodeLocation[instructionCount];
                        opcodes = new int[instructionCount];
                        Arrays.fill(opcodes, -1);
                        callees = new int[instructionCount];
                        Arrays.fill(callees, -1);

                        // Fill in list of labels (index of instruction)
                        final List<Integer> labels = new ArrayList<Integer>();

                        int bytecodeIndex = 0; // position cursor in the original bytecode stream, used if we have a bytecode-> machine code map
                        for (int index = 0; index < instructionCount; index++) {
                            final TargetCodeInstruction instruction = instructions.get(index);
                            instructionLocations[index] = codeManager().createMachineCodeLocation(instruction.address, "native target code instruction");
                            if (instruction.label != null) {
                                labels.add(index);
                            }

                            // offset in bytes of this machine code instruction from beginning
                            final int position = instruction.position;

                            // Ensure that the reported instruction position is legitimate.
                            // The disassembler sometimes seems to report wild positions
                            // when disassembling random binary; this can happen when
                            // viewing some unknown native code whose length we must guess.
                            if (position < 0 || position >= targetCodeLength) {
                                continue;
                            }

                            if (positionToBytecodeLocationMap != null) {
                                bytecodeLocations[index] = positionToBytecodeLocationMap[position];
                            }

                            if (positionToStopKindMap != null) {
                                final CodeStopKind codeStopKind = positionToStopKindMap[position];
                                if (codeStopKind != null) {
                                    // We're at a stop
                                    codeStopKinds[index] = codeStopKind;
                                    final BytecodeLocation bytecodeLocation = bytecodeLocations[index];
                                    // TODO (mlvdv) only works for non-inlined calls
                                    if (bytecodeLocation != null && bytecodeLocation.classMethodActor.equals(classMethodActor()) && bytecodeLocation.bytecodePosition >= 0) {
                                        callees[index] = findCalleeIndex(bytecodes, bytecodeLocation.bytecodePosition);
                                    }
                                }
                            }
                            if (bytecodeToMachineCodePositionMap != null) {
                                // Add more information if we have a precise map from bytecode to machine code instructions
                                final int bytecodePosition = bytecodeIndex;
                                // To check if we're crossing a bytecode boundary in the JITed code, compare the offset of the instruction at the current row with the offset recorded by the JIT
                                // for the start of bytecode template.
                                if (bytecodePosition < bytecodeToMachineCodePositionMap.length &&
                                                position == bytecodeToMachineCodePositionMap[bytecodePosition]) {
                                    // This is the start of the machine code block implementing the next bytecode
                                    int opcode = Bytes.beU1(bytecodes, bytecodeIndex);
                                    if (opcode == Bytecodes.WIDE) {
                                        opcode = Bytes.beU1(bytecodes, bytecodeIndex + 1);
                                    }
                                    opcodes[index] = opcode;
                                    // Move bytecode position cursor to start of next instruction
                                    do {
                                        ++bytecodeIndex;
                                    } while (bytecodeIndex < bytecodeToMachineCodePositionMap.length &&
                                                    bytecodeToMachineCodePositionMap[bytecodeIndex] == 0);
                                }
                            }
                        }
                        labelIndexes = Collections.unmodifiableList(labels);
                    } finally {
                        vm().unlock();
                    }
                }
            }
        }

        /**
         * @param bytecodes
         * @param bytecodePosition byte offset into bytecodes
         * @return if a call instruction, the index into the constant pool of the called {@link MethodRefConstant}; else -1.
         */
        private int findCalleeIndex(byte[] bytecodes, int bytecodePosition) {
            if (bytecodes == null || bytecodePosition >= bytecodes.length) {
                return -1;
            }
            final BytecodeScanner bytecodeScanner = new BytecodeScanner(methodRefIndexFinder.reset());
            bytecodeScanner.scanInstruction(bytecodes, bytecodePosition);
            return methodRefIndexFinder.methodRefIndex();
        }

        public int length() {
            initialize();
            return instructions.size();
        }

        public TargetCodeInstruction instruction(int index) throws IllegalArgumentException {
            initialize();
            if (index < 0 || index >= instructions.size()) {
                throw new IllegalArgumentException();
            }
            return instructions.get(index);
        }

        public int findInstructionIndex(Address address) {
            initialize();
            final int length = instructions.size();
            if (address.greaterEqual(instructions.get(0).address)) {
                for (int index = 1; index < length; index++) {
                    instructions.get(index);
                    if (address.lessThan(instructions.get(index).address)) {
                        return index - 1;
                    }
                }
                final TargetCodeInstruction lastInstruction = instructions.get(instructions.size() - 1);
                if (address.lessThan(lastInstruction.address.plus(lastInstruction.bytes.length))) {
                    return length - 1;
                }
            }
            return -1;
        }
        public MachineCodeLocation instructionLocation(int index) {
            initialize();
            if (index < 0 || index >= instructions.size()) {
                throw new IllegalArgumentException();
            }
            return instructionLocations[index];
        }

        public boolean isStop(int index) throws IllegalArgumentException {
            initialize();
            if (index < 0 || index >= instructions.size()) {
                throw new IllegalArgumentException();
            }
            return codeStopKinds[index] != null;
        }

        public boolean isCall(int index) throws IllegalArgumentException {
            initialize();
            if (index < 0 || index >= instructions.size()) {
                throw new IllegalArgumentException();
            }
            final CodeStopKind stopKind = codeStopKinds[index];
            return stopKind != null && stopKind != CodeStopKind.SAFE;
        }

        public boolean isNativeCall(int index) throws IllegalArgumentException {
            initialize();
            if (index < 0 || index >= instructions.size()) {
                throw new IllegalArgumentException();
            }
            final CodeStopKind stopKind = codeStopKinds[index];
            return stopKind == CodeStopKind.NATIVE_CALL;
        }

        public boolean isBytecodeBoundary(int index) throws IllegalArgumentException {
            initialize();
            if (index < 0 || index >= instructions.size()) {
                throw new IllegalArgumentException();
            }
            return opcodes[index] >= 0;
        }

        public BytecodeLocation bytecodeLocation(int index) throws IllegalArgumentException {
            initialize();
            if (index < 0 || index >= instructions.size()) {
                throw new IllegalArgumentException();
            }
            return bytecodeLocations[index];
        }

        public TargetJavaFrameDescriptor targetFrameDescriptor(int index) throws IllegalArgumentException {
            initialize();
            if (index < 0 || index >= instructions.size()) {
                throw new IllegalArgumentException();
            }
            return getTargetFrameDescriptor(index);
        }

        public int opcode(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructions.size()) {
                throw new IllegalArgumentException();
            }
            return opcodes[index];
        }

        public int calleeConstantPoolIndex(int index) {
            initialize();
            if (index < 0 || index >= instructions.size()) {
                throw new IllegalArgumentException();
            }
            return callees[index];
        }

        public List<Integer> labelIndexes() {
            initialize();
            return labelIndexes;
        }

        public int[] bytecodeToMachineCodePositionMap() {
            return getBytecodeToMachineCodePositionMap();
        }

    }

    /**
     * A copier to be used for implementing {@link TeleTargetMethod#targetMethod()}.
     */
    class ReducedDeepCopier extends DeepCopier {
        public ReducedDeepCopier() {
            TeleFields teleFields = vm().teleFields();
            omit(teleFields.TargetMethod_scalarLiterals.fieldActor());
            omit(teleFields.TargetMethod_referenceLiterals.fieldActor());
            generator = teleFields.Adapter_generator.fieldActor();
        }
        private final FieldActor generator;

        @Override
        protected Object makeDeepCopy(FieldActor fieldActor, TeleObject teleObject) {
            if (fieldActor.equals(generator)) {
                return null;
            } else {
                return super.makeDeepCopy(fieldActor, teleObject);
            }
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
                if (teleClassMethodActor.hasTargetMethod()) {
                    ClassMethodActor classMethodActor = teleClassMethodActor.classMethodActor();
                    if (classMethodActor.name.equals(methodKey.name()) && classMethodActor.descriptor.equals(methodKey.signature())) {
                        for (int i = 0; i < teleClassMethodActor.numberOfCompilations(); ++i) {
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

    private static final Map<TeleObject, TargetABI> abiCache = new HashMap<TeleObject, TargetABI>();

    /**
     * Cached copy of the {@link TargetMethod} from the VM, replaced whenever the code is observed to have been patched.
     */
    private TargetMethod targetMethodCache;

    /**
     * Process epoch at the last time the target method in the VM was observed to have changed (been patched).
     */
    private long lastCodeChangeEpoch = -1L;

    /**
     * Cache of the code from the last time the local copy of the target method was made,
     * used for comparison to see if it has changed.
     */
    private byte[] codeCache = null;

    /**
     * Cache of the machine instructions disassembled from code the last time the code changed.
     */
    private List<TargetCodeInstruction> instructionCache;

    /**
     * Cache of the map built around disassembled machine code instructions.
     */
    private InstructionMap instructionMapCache;

    /**
     * @see  StopPositions
     * @see  TargetMethod#stopPositions()
     */
    protected StopPositions stopPositions;

    private TeleClassMethodActor teleClassMethodActor;

    protected TeleTargetMethod(TeleVM vm, Reference targetMethodReference) {
        super(vm, targetMethodReference);
        // Register every method compilation, so that they can be located by code address.
        // Note that this depends on the basic location information already being read by
        // superclass constructors.
        vm.codeCache().register(this);
    }

    /**
     * Creates and caches a local copy of the {@link TargetMethod} in the VM.
     */
    private void loadMethodCache() {
        targetMethodCache = (TargetMethod) deepCopy();
        codeCache = targetMethodCache.code();
        instructionCache = null;
        instructionMapCache = null;
    }

    /**
     * Clears all cached data from the {@link TargetMethod} in the VM that must
     * be recomputed if the machine code is discovered to have changed.
     */
    private void flushMethodCache() {
        targetMethodCache = null;
        codeCache = null;
        instructionCache = null;
        instructionMapCache = null;
    }

    /** {@inheritDoc}
     * <br>
     * Compiled machine code generally doesn't change, so the code and disassembled instructions are cached, but
     * this update checks for cases where the code does change (i.e. has been patched), in which case the caches
     * are flushed and the epoch of the change recorded.
     */
    @Override
    protected void updateObjectCache(long epoch, StatsPrinter statsPrinter) {
        super.updateObjectCache(epoch, statsPrinter);
        // Flush caches if the code in the VM is different than the cache (i.e. has been patched)
        if (isLoaded()) {
            if (vm().tryLock()) {
                try {
                    final Reference byteArrayReference = vm().teleFields().TargetMethod_code.readReference(reference());
                    final TeleArrayObject teleByteArrayObject = (TeleArrayObject) heap().makeTeleObject(byteArrayReference);
                    final byte[] newCode = (byte[]) teleByteArrayObject.shallowCopy();
                    if (!Arrays.equals(codeCache, newCode)) {
                        flushMethodCache();
                        lastCodeChangeEpoch = epoch;
                        Trace.line(1, tracePrefix() + "TargetMethod patched for " + getTeleClassMethodActor().classMethodActor().name());
                    }
                } catch (DataIOError dataIOError) {
                    // If something goes wrong, delay the cache flush until next time.
                } finally {
                    vm().unlock();
                }
            }
        }
    }

    /**
     * @return whether a copy of the {@link TargetMethod} in the VM has been created and cached.
     */
    public final boolean isLoaded() {
        return targetMethodCache != null;
    }

    /**
     * @return a lazily created local copy of the {@link TargetMethod} in the VM.
     */
    public final TargetMethod targetMethod() {
        if (targetMethodCache == null) {
            loadMethodCache();
        }
        return targetMethodCache;
    }

    public int compilationIndex() {
        // Lazily computed to avoid circularity during construction.
        final TeleClassMethodActor teleClassMethodActor = getTeleClassMethodActor();
        return teleClassMethodActor == null ? 0 : teleClassMethodActor.compilationIndexOf(this);
    }

    /**
     * @return disassembled target code instructions
     */
    protected final List<TargetCodeInstruction> getInstructions() {
        if (instructionCache == null) {
            final byte[] code = getCode();
            if (code != null) {
                instructionCache = TeleDisassembler.decode(platform(), getCodeStart(), code, targetMethod().encodedInlineDataDescriptors());
            }
        }
        return instructionCache;
    }

    public final InstructionMap getInstructionMap() {
        if (instructionMapCache == null) {
            instructionMapCache = new CompiledCodeInstructionMap(vm());
        }
        return instructionMapCache;
    }

    /**
     * Gets VM memory location of the first instruction in the method.
     *
     * @see TargetMethod#codeStart()
     */
    public final Pointer getCodeStart() {
        return targetMethod().codeStart();
    }

    /**
     * Gets the call entry memory location for this method.
     *
     * @return {@link Address#zero()} if this target method has not yet been compiled
     */
    public final Address callEntryPoint() {
        Address callEntryAddress = Address.zero();
        final Pointer codeStart = getCodeStart();
        if (!codeStart.isZero()) {
            callEntryAddress = codeStart;
            TeleObject teleCallEntryPoint = null;
            if (vm().tryLock()) {
                try {
                    final Reference callEntryPointReference = vm().teleFields().TargetMethod_callEntryPoint.readReference(reference());
                    teleCallEntryPoint = heap().makeTeleObject(callEntryPointReference);
                } finally {
                    vm().unlock();
                }
            }
            if (teleCallEntryPoint != null) {
                final CallEntryPoint callEntryPoint = (CallEntryPoint) teleCallEntryPoint.deepCopy();
                if (callEntryPoint != null) {
                    callEntryAddress = codeStart.plus(callEntryPoint.offset());
                }
            }
        }
        return callEntryAddress;
    }

    /**
     * Gets the method actor in the VM for this target method.
     *
     * @return {@code null} if the class method actor is null the target method
     */
    public final TeleClassMethodActor getTeleClassMethodActor() {
        if (teleClassMethodActor == null && vm().tryLock()) {
            try {
                final Reference classMethodActorReference = vm().teleFields().TargetMethod_classMethodActor.readReference(reference());
                teleClassMethodActor = (TeleClassMethodActor) heap().makeTeleObject(classMethodActorReference);
            } finally {
                vm().unlock();
            }
        }
        return teleClassMethodActor;
    }

    /**
     * Gets the locations in compiled code (by byte offset from the start)
     * of stop positions:  calls and safepoints.
     *
     * @return the stop positions for the compiled code.
     * @see TargetMethod#stopPositions()
     */
    private StopPositions getStopPositions() {
        if (stopPositions == null) {
            if (targetMethod().stopPositions() != null) {
                stopPositions = new StopPositions(targetMethod().stopPositions());
            }
        }
        return stopPositions;
    }

    /**
     * @return the process epoch at the last time when the code was observed to have changed.
     */
    public final long lastCodeChangeEpoch() {
        return lastCodeChangeEpoch;
    }

    /**
     * Gets the local mirror of the class method actor associated with this target method. This may be null.
     */
    public final ClassMethodActor classMethodActor() {
        final TeleClassMethodActor teleClassMethodActor = getTeleClassMethodActor();
        return teleClassMethodActor == null ? null : teleClassMethodActor.classMethodActor();
    }

    /**
     * Gets the byte array containing the target-specific machine code of this target method in the VM.
     * @see TargetMethod#code()
     */
    private byte[] getCode() {
        return targetMethod().code();
    }

    /**
     * Gets the length of the byte array containing the target-specific machine code of this target method in the VM.
     * @see TargetMethod#codeLength()
     */
    protected final int getCodeLength() {
        return targetMethod().codeLength();
    }

    /**
     * Creates a map:  instruction position (bytes offset from start) -> the kind of stop, null if not a stop.
     */
    private CodeStopKind[] getPositionToStopKindMap() {
        final StopPositions stopPositions = getStopPositions();
        if (stopPositions == null) {
            return null;
        }
        final CodeStopKind[] stopKinds = new CodeStopKind[getCodeLength()];

        final int directCallCount = targetMethod().numberOfDirectCalls();
        final int indirectCallCount = targetMethod().numberOfIndirectCalls();
        final int safepointCount = targetMethod().numberOfSafepoints();
        assert directCallCount + indirectCallCount + safepointCount == stopPositions.length();

        for (int stopIndex = 0; stopIndex < directCallCount; stopIndex++) {
            if (stopPositions.isNativeFunctionCall(stopIndex)) {
                stopKinds[stopPositions.get(stopIndex)] = CodeStopKind.NATIVE_CALL;
            } else {
                stopKinds[stopPositions.get(stopIndex)] = CodeStopKind.DIRECT_CALL;
            }
        }
        for (int stopIndex = directCallCount; stopIndex < directCallCount + indirectCallCount; stopIndex++) {
            stopKinds[stopPositions.get(stopIndex)] = CodeStopKind.INDIRECT_CALL;
        }
        for (int stopIndex = directCallCount + indirectCallCount; stopIndex < stopPositions.length(); stopIndex++) {
            stopKinds[stopPositions.get(stopIndex)] = CodeStopKind.SAFE;
        }
        return stopKinds;
    }

    /**
     * Creates a map:  instruction position (bytes offset from start) -> bytecode location  (as much as can be determined).
     */
    protected BytecodeLocation[] getPositionToBytecodeLocationMap() {
        final StopPositions stopPositions = getStopPositions();
        if (stopPositions == null) {
            return null;
        }
        BytecodeLocation[] bytecodeLocations = new BytecodeLocation[getCodeLength()];
        for (int stopIndex = 0; stopIndex < stopPositions.length(); ++stopIndex) {
            bytecodeLocations[stopPositions.get(stopIndex)] = getBytecodeLocation(stopIndex);
        }
        return bytecodeLocations;
    }

    protected int[] getBytecodeToMachineCodePositionMap() {
        return null;
    }

    protected List<TargetJavaFrameDescriptor> getJavaFrameDescriptors() {
        return null;
    }

    private TargetJavaFrameDescriptor getTargetFrameDescriptor(int instructionIndex) {
        final TargetCodeInstruction instruction = getInstructions().get(instructionIndex);
        final List<TargetJavaFrameDescriptor> javaFrameDescriptors = getJavaFrameDescriptors();
        if (javaFrameDescriptors != null) {
            final StopPositions stopPositions = getStopPositions();
            if (stopPositions != null) {
                for (int i = 0; i < stopPositions.length(); i++) {
                    if (stopPositions.get(i) == instruction.position) {
                        return javaFrameDescriptors.get(i);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets the Java frame descriptor corresponding to a given stop index.
     *
     * @param stopIndex a stop index
     * @return the Java frame descriptor corresponding to {@code stopIndex} or null if there is no Java frame descriptor
     *         for {@code stopIndex}
     * @see TargetMethod#getBytecodeLocationFor(int)
     */
    public BytecodeLocation getBytecodeLocation(int stopIndex) {
        return targetMethod().getBytecodeLocationFor(stopIndex);
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

    private byte[] encodedInlineDataDescriptors() {
        return targetMethod().encodedInlineDataDescriptors();
    }

    /**
     * Disassembles this target method's code to a given writer.
     */
    protected void disassemble(IndentWriter writer) {
    }

    // [tw] Warning: duplicated code!
    public MachineCodeInstructionArray getTargetCodeInstructions() {
        final List<TargetCodeInstruction> instructions = getInstructions();
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
    protected DeepCopier newDeepCopier() {
        return new ReducedDeepCopier();
    }

    @Override
    public ReferenceTypeProvider getReferenceType() {
        return vm().vmAccess().getReferenceType(getClass());
    }

    /** {@inheritDoc}
     * <br>
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
        disassemble(writer);
        writer.flush();
        final Platform platform = platform();
        final InlineDataDecoder inlineDataDecoder = InlineDataDecoder.createFrom(encodedInlineDataDescriptors());
        final Address startAddress = getCodeStart();
        final DisassemblyPrinter disassemblyPrinter = new DisassemblyPrinter(false) {
            @Override
            protected String disassembledObjectString(Disassembler disassembler, DisassembledObject disassembledObject) {
                final String string = super.disassembledObjectString(disassembler, disassembledObject);
                if (string.startsWith("call ")) {
                    final BytecodeLocation bytecodeLocation = null; //_teleTargetMethod.getBytecodeLocationFor(startAddress.plus(disassembledObject.startPosition()));
                    if (bytecodeLocation != null) {
                        final MethodRefConstant methodRef = bytecodeLocation.getCalleeMethodRef();
                        if (methodRef != null) {
                            final ConstantPool pool = bytecodeLocation.classMethodActor.codeAttribute().constantPool;
                            return string + " [" + methodRef.holder(pool).toJavaString(false) + "." + methodRef.name(pool) + methodRef.signature(pool).toJavaString(false, false) + "]";
                        }
                    }
                }
                return string;
            }
        };
        com.sun.max.asm.dis.Disassembler.disassemble(printStream, getCode(), platform.isa, platform.wordWidth(), startAddress.toLong(), inlineDataDecoder, disassemblyPrinter);
    }
}

