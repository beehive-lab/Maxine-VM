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
import com.sun.max.tele.MaxMachineCode.InstructionMap;
import com.sun.max.tele.*;
import com.sun.max.tele.field.*;
import com.sun.max.tele.method.CodeLocation.MachineCodeLocation;
import com.sun.max.tele.method.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
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
     * Encapsulates a copied {@link TargeMethod} along with derived information.
     * The class is intended to be treated as immutable; a new one gets created for each
     * successive version of the TargetMethod. That means that any method can be called
     * without needing to obtain the VM lock.
     * <p>
     * Successive versions of a single {@link TargetMethod} in the VM can occur in two
     * ways; otherwise a {@link TargetMethod} in the VM is mostly immutable.
     * <ol>
     * <li>The initial state of the code starting location is zero, which gets assigned finally
     * when the code is generated and placed into the code cache.</li>
     * <li>The compiled code can be patched, for example when a method call is resolved.</li>
     * </ol>
     * <p>
     * There are three states for this cache:
     * <ul>
     * <li><i>Unloaded, Dirty</i>:  the initial state where {@code targetMethod = null && generation = 0}.</li>
     * <li><i>Loaded, Current</i>: {@code targetMethod != null && generation == TeleTargetMethod.codeGeneration}.
     * In this state, all derived information has been computed and is cached; it is available without reading
     * from the VM.</li>
     * <li><i>Loaded, Dirty</i>:  {@code targetMethod != null && generation < TeleTargetMethod.codeGeneration}.
     * In this state, derived cached information is presumed to be out of date and unreliable; every attempt
     * to use derived information should be preceded by an attempt to reload the cache and the old information
     * provided only if the attempt fails.</li>
     * </ul>
     * Once a {@link TargetMethod} has been loaded, it remains in that state, alternating between current and dirty
     * as the cached code is compared with the code in the VM during each update cycle.
     * <p>
     * This constructor requires access to the VM lock.
     *
     * @see TargetMethod
     * @see TeleVM#tryLock()
     *
     * @author Michael Van De Vanter
     */
    private final class TargetMethodCache implements InstructionMap {

        /**
         * A copy of the {@link TargetMethod} in the VM, reflecting its state at some point during
         * its lifetime.
         */
        private final TargetMethod targetMethod;

        /**
         * Counts the successive versions of this cache, with a new one presumed to be
         * created each time the code in the VM is discovered to have changed.  Note that this might
         * not agree with the actual number of times the code has changed, since it may have changed
         * more than once in a single VM execution cycle.
         */
        private final int codeGenerationCount;

        /**
         * The size of the compilation's machine code in bytes.
         */
        private final int codeLength;

        /**
         * Location of the first byte of the machine code for the compilation in VM memory.
         */
        private final Pointer codeStart;

        private final Address callEntryAddress;

        /**
         * Locations in compiled code (by byte offset from the start)
         * of stop positions:  calls and safepoints.
         *
         * @return the stop positions for the compiled code.
         * @see  StopPositions
         */
        private final StopPositions stopPositions;

        private final List<TargetCodeInstruction> instructions;

        /**
         * The number of disassembled instructions.
         */
        private final int instructionCount;

        private final List<TargetJavaFrameDescriptor> javaFrameDescriptors;

        private final int[] bytecodeToMachineCodePositionMap;

        private final MachineCodeLocation[] instructionLocations;

        /**
         * Map:  target code instruction index -> the kind of stop at the instruction, null if not a stop.
         */
        private final CodeStopKind[] codeStopKinds;

        /**
         * Map: target code instruction index -> bytecode that compiled into code starting at this instruction, if known; else empty.
         * The bytecode location may be in a different method that was inlined.
         */
        private final BytecodeLocation[] bytecodeLocations;

        /**
         * Map: target code instruction index -> the specific opcode implemented by the group of instructions starting
         * with this one, if known; else empty.
         */
        private final int[] opcodes;

        /**
         * Map: target code instruction index -> constant pool index of {@Link MethodRefConstant} if this is a call instruction; else -1.
         */
        private final int[] callees;

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
         * @param targetMethod a local copy of the {@link TargetMethod} in the VM.
         * @param generationCount number of this cache in the sequence of caches: 0 = no information; 1 VM's initial state.
         * @param teleClassMethodActor access to the {@link ClassMethodActor} in the VM of which this {@link TargetMethod}
         * is a compilation.
         * @see TeleVM#tryLock()
         */
        private TargetMethodCache(TargetMethod targetMethod, int generationCount, TeleClassMethodActor teleClassMethodActor) {
            assert (targetMethod == null && generationCount == 0) || (targetMethod != null && generationCount > 0);
            this.targetMethod = targetMethod;
            this.codeGenerationCount = generationCount;

            if (targetMethod == null) {
                this.codeLength = 0;
                this.codeStart = Pointer.zero();
                this.callEntryAddress = Address.zero();
                this.stopPositions = null;
                this.instructions = EMPTY_TARGET_INSTRUCTIONS;
                this.instructionCount = 0;
                this.javaFrameDescriptors = null;
                this.bytecodeToMachineCodePositionMap = null;
                this.instructionLocations = EMPTY_MACHINE_CODE_LOCATIONS;
                this.codeStopKinds = EMPTY_CODE_STOP_KINDS;
                this.bytecodeLocations = EMPTY_BYTECODE_LOCATIONS;
                this.opcodes = EMPTY_INT_ARRAY;
                this.callees = EMPTY_INT_ARRAY;
                this.labelIndexes = EMPTY_INTEGER_LIST;
            } else {
                codeLength = targetMethod.codeLength();
                codeStart = targetMethod.codeStart();
                if (codeStart.isZero()) {
                    callEntryAddress = Address.zero();
                } else {
                    callEntryAddress = codeStart.plus(targetMethod.callEntryPoint.offset());
                }

                if (targetMethod.stopPositions() != null) {
                    stopPositions = new StopPositions(targetMethod.stopPositions());
                } else {
                    stopPositions = null;
                }

                this.instructions = TeleDisassembler.decode(platform(), targetMethod.codeStart(), targetMethod.code(), targetMethod.encodedInlineDataDescriptors());
                this.instructionCount = this.instructions.size();

                byte[] bytecodes = null;
                if (teleClassMethodActor != null) {
                    final TeleCodeAttribute teleCodeAttribute = teleClassMethodActor.getTeleCodeAttribute();
                    if (teleCodeAttribute != null) {
                        bytecodes = teleCodeAttribute.readBytecodes();
                    }
                }

                this.javaFrameDescriptors = getJavaFrameDescriptors(targetMethod);

                // Non-null if we have a precise map between bytecode and machine code instructions
                this.bytecodeToMachineCodePositionMap = getBytecodeToMachineCodePositionMap(targetMethod);

                // First, gather general information from target method, indexed by
                // bytecode positions of instructions (byte offset from code start)

                /**
                 * Map:  machine code position (bytes offset from start) -> the kind of stop, null if not a stop.
                 */
                CodeStopKind[] positionToStopKindMap = null;
                if (stopPositions != null) {
                    positionToStopKindMap = new CodeStopKind[this.codeLength];

                    final int directCallCount = targetMethod.numberOfDirectCalls();
                    final int indirectCallCount = targetMethod.numberOfIndirectCalls();
                    final int safepointCount = targetMethod.numberOfSafepoints();
                    assert directCallCount + indirectCallCount + safepointCount == stopPositions.length();

                    for (int stopIndex = 0; stopIndex < directCallCount; stopIndex++) {
                        if (stopPositions.isNativeFunctionCall(stopIndex)) {
                            positionToStopKindMap[stopPositions.get(stopIndex)] = CodeStopKind.NATIVE_CALL;
                        } else {
                            positionToStopKindMap[stopPositions.get(stopIndex)] = CodeStopKind.DIRECT_CALL;
                        }
                    }
                    for (int stopIndex = directCallCount; stopIndex < directCallCount + indirectCallCount; stopIndex++) {
                        positionToStopKindMap[stopPositions.get(stopIndex)] = CodeStopKind.INDIRECT_CALL;
                    }
                    for (int stopIndex = directCallCount + indirectCallCount; stopIndex < stopPositions.length(); stopIndex++) {
                        positionToStopKindMap[stopPositions.get(stopIndex)] = CodeStopKind.SAFE;
                    }
                }

                final BytecodeLocation[] positionToBytecodeLocationMap = getPositionToBytecodeLocationMap(targetMethod, instructions, codeLength, stopPositions);

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
                    if (position < 0 || position >= codeLength) {
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
            return instructionLocations[index];
        }

        public boolean isStop(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructionCount) {
                throw new IllegalArgumentException();
            }
            return codeStopKinds[index] != null;
        }

        public boolean isCall(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructionCount) {
                throw new IllegalArgumentException();
            }
            final CodeStopKind stopKind = codeStopKinds[index];
            return stopKind != null && stopKind != CodeStopKind.SAFE;
        }

        public boolean isNativeCall(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructionCount) {
                throw new IllegalArgumentException();
            }
            final CodeStopKind stopKind = codeStopKinds[index];
            return stopKind == CodeStopKind.NATIVE_CALL;
        }

        public boolean isBytecodeBoundary(int index) throws IllegalArgumentException {
            assert targetMethod != null;
            if (index < 0 || index >= instructionCount) {
                throw new IllegalArgumentException();
            }
            return opcodes[index] >= 0;
        }

        public BytecodeLocation bytecodeLocation(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructionCount) {
                throw new IllegalArgumentException();
            }
            return bytecodeLocations[index];
        }

        public TargetJavaFrameDescriptor targetFrameDescriptor(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructionCount) {
                throw new IllegalArgumentException();
            }
            final TargetCodeInstruction instruction = instructions.get(index);
            if (javaFrameDescriptors != null) {
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

        public int opcode(int index) throws IllegalArgumentException {
            if (index < 0 || index >= instructionCount) {
                throw new IllegalArgumentException();
            }
            return opcodes[index];
        }

        public int calleeConstantPoolIndex(int index) {
            if (index < 0 || index >= instructionCount) {
                throw new IllegalArgumentException();
            }
            return callees[index];
        }

        public List<Integer> labelIndexes() {
            return labelIndexes;
        }

        public int[] bytecodeToMachineCodePositionMap() {
            return bytecodeToMachineCodePositionMap;
        }

        /**
         * @return whether the {@link TargetMethod} being cached has been copied from the VM yet.
         */
        private boolean isLoaded() {
            return targetMethod != null;
        }

        private byte[] code() {
            return targetMethod.code();
        }

        /**
         * @return address of the first instruction in the method compilation's code
         */
        private Pointer codeStart() {
            return codeStart;
        }

        private Address callEntryAddress() {
            return callEntryAddress;
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

    private final TeleClassMethodActor teleClassMethodActor;

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
    private TargetMethodCache targetMethodCache = new TargetMethodCache(null, 0, null);
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

        final Reference classMethodActorReference = vm().teleFields().TargetMethod_classMethodActor.readReference(targetMethodReference);
        teleClassMethodActor = (TeleClassMethodActor) heap().makeTeleObject(classMethodActorReference);

        // Register every method compilation, so that they can be located by code address.
        // Note that this depends on the basic location information already being read by
        // superclass constructors.
        vm.codeCache().register(this);
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
            if (!Arrays.equals(codeInVM, targetMethodCache.code())) {
                // The code in the VM is different than in the cache.
                // Set the VM generation count to one more than the cached copy.  This
                // makes the cache not "current", essentially marking it dirty.
                vmCodeGenerationCount = targetMethodCache.codeGenerationCount + 1;
                Trace.line(1, tracePrefix() + "TargetMethod patched for " + classMethodActor().name());
            }
        } catch (DataIOError dataIOError) {
            // If something goes wrong, delay the cache update until next time.
            return false;
        }
        return true;
    }

    @Override
    protected DeepCopier newDeepCopier() {
        return new ReducedDeepCopier();
    }

    /** {@inheritDoc}
     * <p>
     * Assume that the memory described by a {@link TargetMethod} (method compilation) object does not move.  That memory is
     * allocated in the code cache and contains the arrays produced by compilation.
     */
    @Override
    public boolean isRelocatable() {
        return false;
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
                if (teleClassMethodActor != null) {
                    Trace.line(TRACE_VALUE, "WARNING: failed to update TargetMethod for " + teleClassMethodActor.getName());
                } else {
                    Trace.line(TRACE_VALUE, "WARNING: failed to update class actor");
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
        return !targetMethodCache.codeStart().isZero() && targetMethodCache.codeGenerationCount == vmCodeGenerationCount;
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
        return targetMethodCache().codeStart();
    }

    // TODO (mlvdv) compute this in the constructor?  meta information in TeleTargetMethod,
    // consider ourselves dirty if this is 0?  Maybe only for the code start in parents,
    // if they're zero
    /**
     * Gets the call entry memory location in the VM for this method.
     *
     * @return {@link Address#zero()} if this target method has not yet been compiled
     */
    public final Address callEntryPoint() {
        return targetMethodCache().callEntryAddress();
    }


//    public final Address callEntryAddress() {
//        Address callEntryAddress = Address.zero();
//        final Pointer codeStart = codeStart();
//        if (!codeStart.isZero()) {
//            callEntryAddress = codeStart;
//            TeleObject teleCallEntryPoint = null;
//            if (vm().tryLock()) {
//                try {
//                    final Reference callEntryPointReference = vm().teleFields().TargetMethod_callEntryPoint.readReference(reference());
//                    teleCallEntryPoint = heap().makeTeleObject(callEntryPointReference);
//                    if (teleCallEntryPoint != null) {
//                        final CallEntryPoint callEntryPoint = (CallEntryPoint) teleCallEntryPoint.deepCopy();
//                        if (callEntryPoint != null) {
//                            callEntryAddress = codeStart.plus(callEntryPoint.offset());
//                        }
//                    }
//                } finally {
//                    vm().unlock();
//                }
//            }
//
//        }
//        return callEntryAddress;
//    }


    /**
     * Gets the local mirror of the class method actor associated with this target method. This may be null.
     */
    public final ClassMethodActor classMethodActor() {
        return teleClassMethodActor == null ? null : teleClassMethodActor.classMethodActor();
    }

    /**
     * Creates a map:  instruction position (bytes offset from start) -> bytecode location  (as much as can be determined).
     */
    protected BytecodeLocation[] getPositionToBytecodeLocationMap(TargetMethod targetMethod, List<TargetCodeInstruction> instructions, int codeLength, StopPositions stopPositions) {
        if (stopPositions != null) {
            final BytecodeLocation[] bytecodeLocations = new BytecodeLocation[codeLength];
            for (int stopIndex = 0; stopIndex < stopPositions.length(); ++stopIndex) {
                bytecodeLocations[stopPositions.get(stopIndex)] = targetMethod.getBytecodeLocationFor(stopIndex);
            }
            return bytecodeLocations;
        }
        return null;
    }

    protected int[] getBytecodeToMachineCodePositionMap(TargetMethod targetMethod) {
        return null;
    }

    protected List<TargetJavaFrameDescriptor> getJavaFrameDescriptors(TargetMethod targetMethod) {
        return null;
    }

    protected final List<TargetJavaFrameDescriptor> javaFrameDescriptors() {
        return targetMethodCache().javaFrameDescriptors;
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
        return targetMethodCache().targetMethod.getBytecodeLocationFor(stopIndex);
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

    /**
     * Disassembles this target method's code to a given writer.
     */
    protected void disassemble(IndentWriter writer) {
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

    public void writeSummary(PrintStream printStream) {
        final IndentWriter writer = new IndentWriter(new OutputStreamWriter(printStream));
        writer.println("code for: " + classMethodActor().format("%H.%n(%p)"));
        writer.println("compilation: " + compilationIndex());
        disassemble(writer);
        writer.flush();
        final Platform platform = platform();
        final InlineDataDecoder inlineDataDecoder =
            InlineDataDecoder.createFrom(targetMethodCache().targetMethod.encodedInlineDataDescriptors());
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
        com.sun.max.asm.dis.Disassembler.disassemble(printStream, targetMethodCache.code(), platform.isa, platform.wordWidth(), startAddress.toLong(), inlineDataDecoder, disassemblyPrinter);
    }

}
