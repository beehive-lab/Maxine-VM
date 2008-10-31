/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.stack;

import static com.sun.max.vm.thread.VmThreadLocal.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.bytecode.refmaps.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.heap.util.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.trampoline.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.type.SignatureDescriptor.*;

/**
 * GC support: prepares the object reference map of a thread's stack.
 * Sets one bit in the map for each slot on the stack that contains an object reference.
 * The map is located on the stack above the safepoints-triggered VM thread locals.
 * The map covers two address ranges: the first is the safepoints-triggered VM thread local and the
 * second is the portion of the stack "in use" which extends from the current stack pointer (regarded as "stack top")
 * to the end of the safepoints-enabled VM thread locals (regarded as "stack bottom").
 * The latter region extends from the stack pointer up to the end of the safepoints-enabled VM thread locals.
 * The diagram {@linkplain VmThread here} depicts where the reference map is on the stack as well as the regions
 * it covers.
 * <p>
 * The lowest bit in the map corresponds to the first word of the safepoints-triggered VM thread locals.
 * The {@code n}th bit corresponds to the stack top where {@code n == NUMBER_OF_REFERENCE_MAP_SLOTS_FOR_TRIGGERED_VM_THREAD_LOCALS}.
 * The highest bit in the map corresponds to the stack bottom.
 * <p>
 * The GC's root scanning can then simply iterate over the bits in the map to find all object references on the stack.
 * Thus the GC does not need to allocate any auxiliary data during root scanning,
 * nor does it need to traverse any objects.
 * This provides a lot of flexibility in GC implementation.
 *
 * ATTENTION: the algorithm below must not allocate any objects from the GC heap,
 * since it is running at a GC safepoint when the global GC lock may already be taken.
 * Especially the {@linkplain ReferenceMapInterpreter reference map interpreter},
 * which fills in stack reference maps for JIT target methods as needed
 * was carefully crafted to comply with this requirement.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Ben L. Titzer
 */
public final class StackReferenceMapPreparer {

    public StackReferenceMapPreparer(VmThread owner) {
        _owner = owner;
    }

    private final VmThread _owner;
    private Pointer _triggeredVmThreadLocals;
    private Pointer _enabledVmThreadLocals;
    private Pointer _disabledVmThreadLocals;
    private Pointer _referenceMap;
    private Pointer _lowestStackSlot;

    /**
     * Prepares a reference map for the entire stack of a VM thread
     * while the GC has not changed anything yet.
     *
     * Later on, the GC can quickly scan the prepared stack reference map
     * without allocation and without using any object references (other than the ones subject to root scanning).
     *
     * @return the index of the lowest slot in the VM thread locals storage
     */
    public void prepareStackReferenceMap(Pointer vmThreadLocals, Pointer instructionPointer, Pointer stackPointer, Pointer framePointer) {
        _enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
        _disabledVmThreadLocals = SAFEPOINTS_DISABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
        _triggeredVmThreadLocals = SAFEPOINTS_TRIGGERED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
        _referenceMap = STACK_REFERENCE_MAP.getConstantWord(vmThreadLocals).asPointer();
        _lowestStackSlot = LOWEST_STACK_SLOT_ADDRESS.getConstantWord(vmThreadLocals).asPointer();
        final Pointer highestSlot = HIGHEST_STACK_SLOT_ADDRESS.getConstantWord(vmThreadLocals).asPointer();

        // Inform subsequent reference map scanning (see VmThreadLocal.scanReferences()) of the stack range covered:
        LOWEST_ACTIVE_STACK_SLOT_ADDRESS.setVariableWord(vmThreadLocals, stackPointer);

        final VmThread vmThread = UnsafeLoophole.cast(VmThread.class, VM_THREAD.getConstantReference(vmThreadLocals));
        if (this != VmThread.current().stackReferenceMapPreparer()) {
            FatalError.unexpected("Cannot use stack reference map preparer of another thread");
        }

        // clear the reference map covering the stack contents and the vm thread locals
        clearReferenceMapRange(vmThreadLocals, stackPointer, highestSlot);
        clearReferenceMapRange(vmThreadLocals, _lowestStackSlot, vmThreadLocalsEnd(vmThreadLocals));

        boolean lockDisabledSafepoints = false;
        if (Heap.traceGC()) {
            lockDisabledSafepoints = Debug.lock(); // Note: This lock basically serializes stack reference map preparation
            Debug.print("Preparing stack reference map for thread ");
            Debug.printVmThread(vmThread, false);
            Debug.println(":");
            Debug.print("  Highest slot: ");
            Debug.println(highestSlot);
            Debug.print("  Lowest active slot: ");
            Debug.println(stackPointer);
            Debug.print("  Lowest slot: ");
            Debug.println(_lowestStackSlot);
            Debug.print("  Current thread is ");
            Debug.printVmThread(VmThread.current(), true);
        }

        // prepare references for each of the vm thread locals copies
        prepareVmThreadLocalsReferenceMap(_enabledVmThreadLocals);
        prepareVmThreadLocalsReferenceMap(_disabledVmThreadLocals);
        prepareVmThreadLocalsReferenceMap(_triggeredVmThreadLocals);

        // walk the stack and prepare references for each stack frame
        final StackFrameWalker stackFrameWalker = vmThread.stackFrameWalker();
        stackFrameWalker.prepareReferenceMap(instructionPointer, stackPointer, framePointer, this);

        if (Heap.traceGC()) {
            Debug.unlock(lockDisabledSafepoints);
        }
    }

    public void completeStackReferenceMap(Pointer vmThreadLocals, Pointer instructionPointer, Pointer stackPointer, Pointer framePointer) {
        final Pointer highestSlot = LOWEST_ACTIVE_STACK_SLOT_ADDRESS.getVariableWord(vmThreadLocals).asPointer();

        // Inform subsequent reference map scanning (see VmThreadLocal.scanReferences()) of the stack range covered:
        LOWEST_ACTIVE_STACK_SLOT_ADDRESS.setVariableWord(vmThreadLocals, stackPointer);

        final VmThread vmThread = UnsafeLoophole.cast(VmThread.class, VM_THREAD.getConstantReference(vmThreadLocals));

        if (this != vmThread.stackReferenceMapPreparer()) {
            FatalError.unexpected("Cannot use stack reference map preparer of another thread");
        }

        // clear the reference map covering the as-yet-unprepared stack contents
        clearReferenceMapRange(vmThreadLocals, stackPointer, highestSlot.minus(Word.size()));

        boolean lockDisabledSafepoints = false;
        if (Heap.traceGC()) {
            lockDisabledSafepoints = Debug.lock(); // Note: This lock basically serializes stack reference map preparation
            Debug.print("Completing preparation of stack reference map for thread ");
            Debug.printVmThread(vmThread, false);
            Debug.println(":");
            Debug.print("  Highest slot: ");
            Debug.println(highestSlot);
            Debug.print("  Lowest active slot: ");
            Debug.println(stackPointer);
            Debug.print("  Lowest slot: ");
            Debug.println(_lowestStackSlot);
            Debug.print("  Current thread is ");
            Debug.printVmThread(VmThread.current(), true);
        }

        // walk the stack and prepare references for each stack frame
        final StackFrameWalker stackFrameWalker = vmThread.stackFrameWalker();
        stackFrameWalker.prepareReferenceMap(instructionPointer, stackPointer, framePointer, this);

        if (Heap.traceGC()) {
            Debug.unlock(lockDisabledSafepoints);
        }
    }

    /**
     * An array of the VM thread locals that are GC roots.
     */
    private static final VmThreadLocal[] _vmThreadLocalGCRoots;
    static {
        final AppendableSequence<VmThreadLocal> referenceLocals = new ArrayListSequence<VmThreadLocal>();
        for (VmThreadLocal local : VALUES) {
            if (local.kind() == Kind.REFERENCE) {
                referenceLocals.append(local);
            }
        }
        _vmThreadLocalGCRoots = Sequence.Static.toArray(referenceLocals, VmThreadLocal.class);
    }

    private void prepareVmThreadLocalsReferenceMap(Pointer vmThreadLocals) {
        for (VmThreadLocal vmThreadLocal : _vmThreadLocalGCRoots) {
            setReferenceMapBit(vmThreadLocal.pointer(vmThreadLocals));
        }
    }

    @INLINE
    private void setReferenceMapBit(Pointer slotAddress) {
        _referenceMap.setBit(referenceMapBitIndex(_lowestStackSlot, slotAddress));
    }

    private void printSlot(int slotIndex, Pointer framePointer) {
        printSlot(slotIndex, _triggeredVmThreadLocals, framePointer);
    }

    /**
     * Prints the details for a stack slot. In particular the index, frame offset (if known), address, value and name (if available) of the slot are
     * printed. Examples of the output for a slot are show below:
     *
     * <pre>
     *     index=6, address=0xfffffc7ffecfe028, value=0xfffffc8002526828, name=VM_THREAD
     *     index=94, frame offset=16, address=0xfffffc7ffecfbb00, value=0xfffffc7ffecfbb30
     * </pre>
     * @param slotIndex the index of the slot to be printed
     * @param vmThreadLocals a pointer to the VM thread locals corresponding to the stack containing the slot
     * @param framePointer the address of the frame pointer if known, zero otherwise
     */
    private static void printSlot(int slotIndex, Pointer vmThreadLocals, Pointer framePointer) {
        final Pointer slotAddress = slotAddress(slotIndex, vmThreadLocals);
        final Pointer referenceMap = VmThreadLocal.STACK_REFERENCE_MAP.getConstantWord(vmThreadLocals).asPointer();
        Debug.print("index=");
        Debug.print(slotIndex);
        if (!framePointer.isZero()) {
            final int offset = slotAddress.minus(framePointer).toInt();
            if (offset >= 0) {
                Debug.print(", fp+");
            } else {
                Debug.print(", fp");
            }
            Debug.print(offset);
        }
        Debug.print(", address=");
        Debug.print(slotAddress);
        Debug.print(", value=");
        Debug.print(slotAddress.readWord(0));
        if (slotAddress.lessThan(referenceMap)) {
            final Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
            final Pointer disabledVmThreadLocals = SAFEPOINTS_DISABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
            final Pointer triggeredVmThreadLocals = SAFEPOINTS_TRIGGERED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
            if (slotAddress.greaterEqual(disabledVmThreadLocals)) {
                Debug.print(", name=");
                final int vmThreadLocalIndex = slotAddress.minus(disabledVmThreadLocals).dividedBy(Word.size()).toInt();
                Debug.print(NAMES.get(vmThreadLocalIndex));
            } else if (slotAddress.greaterEqual(enabledVmThreadLocals)) {
                Debug.print(", name=");
                final int vmThreadLocalIndex = slotAddress.minus(enabledVmThreadLocals).dividedBy(Word.size()).toInt();
                Debug.print(NAMES.get(vmThreadLocalIndex));
            } else if (slotAddress.greaterEqual(triggeredVmThreadLocals)) {
                Debug.print(", name=");
                final int vmThreadLocalIndex = slotAddress.minus(triggeredVmThreadLocals).dividedBy(Word.size()).toInt();
                Debug.print(NAMES.get(vmThreadLocalIndex));
            }
        }
    }

    /**
     * Prepares a reference map for the entire stack of a VM thread including all of the VM thread locals (which include
     * saved register values iff at a Java safepoint) while the GC has not changed anything yet.
     *
     * Later on, the GC can quickly scan the prepared stack reference map without allocation and without using any
     * object references (other than the ones subject to root scanning).
     *
     * @param vmThreadLocals a pointer to the VM thread locals denoting the thread stack whose reference map is to be prepared
     */
    public void prepareStackReferenceMap(Pointer vmThreadLocals) {
        final Pointer instructionPointer = LAST_JAVA_CALLER_INSTRUCTION_POINTER.getVariableWord(vmThreadLocals).asPointer();
        if (instructionPointer.isZero()) {
            FatalError.unexpected("Thread is not stopped");
/*            instructionPointer = TRAP_INSTRUCTION_POINTER.getVariableWord(vmThreadLocals).asPointer();
            final Pointer stackPointer = TRAP_STACK_POINTER.getVariableWord(vmThreadLocals).asPointer();
            final Pointer framePointer = TRAP_FRAME_POINTER.getVariableWord(vmThreadLocals).asPointer();
            prepareStackReferenceMap(vmThreadLocals, instructionPointer, stackPointer, framePointer);
*/
        }

        // the GC is calling this on behalf of a thread in native code
        final Pointer stackPointer = LAST_JAVA_CALLER_STACK_POINTER.getVariableWord(vmThreadLocals).asPointer();
        final Pointer framePointer = LAST_JAVA_CALLER_FRAME_POINTER.getVariableWord(vmThreadLocals).asPointer();
        prepareStackReferenceMap(vmThreadLocals, instructionPointer, stackPointer, framePointer);
    }

    /**
     * Prepares a reference map for the entire stack of a VM thread including all of the VM thread locals (which include
     * saved register values iff at a Java safepoint) while the GC has not changed anything yet.
     *
     * Later on, the GC can quickly scan the prepared stack reference map without allocation and without using any
     * object references (other than the ones subject to root scanning).
     *
     * @param vmThreadLocals a pointer to the VM thread locals denoting the thread stack whose reference map is to be prepared
     * @param trapState the trap state
     */
    public void prepareStackReferenceMapFromTrap(Pointer vmThreadLocals, Pointer trapState) {
        final Safepoint safepoint = VMConfiguration.hostOrTarget().safepoint();
        final Pointer instructionPointer = safepoint.getInstructionPointer(trapState);
        final TargetMethod targetMethod = Code.codePointerToTargetMethod(instructionPointer);
        final Pointer stackPointer = safepoint.getStackPointer(trapState, targetMethod);
        final Pointer framePointer = safepoint.getFramePointer(trapState, targetMethod);
        prepareStackReferenceMap(vmThreadLocals, instructionPointer, stackPointer, framePointer);
    }

    /**
     * Prepares the reference map for a portion of memory that contains saved register state corresponding
     * to a safepoint triggered at a particular instruction. This method fetches the reference map information
     * for the specified method and then copies it over the reference map for this portion of memory.
     *
     * @param trapState a pointer to the saved register state
     * @param instructionPointer the instruction pointer at the safepoint trap
     */
    public void prepareRegisterReferenceMap(Pointer registerState, Pointer instructionPointer) {
        final TargetMethod targetMethod = Code.codePointerToTargetMethod(instructionPointer);
        if (targetMethod != null) {
            tracePrepareReferenceMap(targetMethod, targetMethod.findClosestStopIndex(instructionPointer), "registers");
            final int safepointIndex = targetMethod.findSafepointIndex(instructionPointer);
            if (safepointIndex < 0) {
                Debug.print("Could not find safepoint index for instruction at position ");
                Debug.print(instructionPointer.minus(targetMethod.codeStart()).toInt());
                Debug.print(" in ");
                Debug.printMethodActor(targetMethod.classMethodActor(), true);
                FatalError.unexpected("Could not find safepoint index");
            }

            // The register reference maps come after all the frame reference maps in _referenceMaps.
            int byteIndex = targetMethod.frameReferenceMapsSize() + (targetMethod.registerReferenceMapSize() * safepointIndex);

            final int registersSlotIndex = referenceMapBitIndex(registerState);
            for (int i = 0; i < targetMethod.registerReferenceMapSize(); i++) {
                final byte referenceMapByte = targetMethod.referenceMaps()[byteIndex];
                traceReferenceMapByteBefore(byteIndex, referenceMapByte, "Register");
                final int baseSlotIndex = registersSlotIndex + (i * Bytes.WIDTH);
                _referenceMap.setBits(baseSlotIndex, referenceMapByte);
                traceReferenceMapByteAfter(Pointer.zero(), baseSlotIndex, referenceMapByte);
                byteIndex++;
            }
        }
    }

    /**
     * Gets the reference-map index of a given stack slot (i.e. which bit in the reference map is correlated with the slot).
     *
     * @param slotAddress an address within the range of stack addresses covered by the reference map
     * @return the index of the bit for {@code slotAddress} in the reference map
     */
    private int referenceMapBitIndex(Address slotAddress) {
        return referenceMapBitIndex(_lowestStackSlot, slotAddress.asPointer());
    }

    /**
     * Gets the address of a stack slot given a slot index.
     *
     * @param slotIndex the slot index for which the address is requested
     * @return the address of the stack slot denoted by {@code slotIndex}
     */
    private Pointer slotAddress(int slotIndex) {
        return _lowestStackSlot.plusWords(slotIndex);
    }

    private static Pointer slotAddress(int slotIndex, Pointer vmThreadLocals) {
        return LOWEST_STACK_SLOT_ADDRESS.getConstantWord(vmThreadLocals).asPointer().plusWords(slotIndex);
    }

    private void prepareFrameReferenceMap(TargetMethod targetMethod, int stopIndex, Pointer framePointer) {
        tracePrepareReferenceMap(targetMethod, stopIndex, "frame");
        int frameSlotIndex = referenceMapBitIndex(framePointer);
        int byteIndex = stopIndex * targetMethod.frameReferenceMapSize();
        for (int i = 0; i < targetMethod.frameReferenceMapSize(); i++) {
            final byte frameReferenceMapByte = targetMethod.referenceMaps()[byteIndex];
            traceReferenceMapByteBefore(byteIndex, frameReferenceMapByte, "Frame");
            _referenceMap.setBits(frameSlotIndex, frameReferenceMapByte);
            traceReferenceMapByteAfter(framePointer, frameSlotIndex, frameReferenceMapByte);
            frameSlotIndex += Bytes.WIDTH;
            byteIndex++;
        }
    }

    private void tracePrepareReferenceMap(TargetMethod targetMethod, int stopIndex, String label) {
        if (Heap.traceGC()) {
            Debug.print("  Preparing reference map for ");
            Debug.print(label);
            Debug.print(" of ");
            Debug.printMethodActor(targetMethod.classMethodActor(), false);
            Debug.print(" +");
            Debug.println(targetMethod.stopPosition(stopIndex));
            Debug.print("    Stop index: ");
            Debug.println(stopIndex);
        }
    }

    /**
     * If {@linkplain Heap#traceGC() GC tracing} is enabled, then this method traces one byte's worth
     * of a frame/register reference map.
     *
     * @param byteIndex the index of the reference map byte
     * @param referenceMapByte the value of the reference map byte
     * @param referenceMapLabel a label indicating whether this reference map is for a frame or for the registers
     */
    private void traceReferenceMapByteBefore(int byteIndex, final byte referenceMapByte, String referenceMapLabel) {
        if (Heap.traceGC()) {
            Debug.print("    ");
            Debug.print(referenceMapLabel);
            Debug.print(" map byte index: ");
            Debug.println(byteIndex);
            Debug.print("    ");
            Debug.print(referenceMapLabel);
            Debug.print(" map byte:       ");
            Debug.println(Address.fromInt(referenceMapByte & 0xff));
        }
    }

    /**
     * If {@linkplain Heap#traceGC() GC tracing} is enabled, then this method traces the stack slots corresponding to a
     * frame or set of set of saved registers that are determined to contain references by a reference map.
     *
     * @param framePointer the frame pointer. This value should be {@link Pointer#zero()} if the reference map is for a
     *            set of saved registers.
     * @param baseSlotIndex the index of the slot corresponding to bit 0 of {@code referenceMapByte}
     * @param referenceMapByte a the reference map byte
     */
    private void traceReferenceMapByteAfter(Pointer framePointer, int baseSlotIndex, final byte referenceMapByte) {
        if (Heap.traceGC()) {
            for (int bitIndex = 0; bitIndex < Bytes.WIDTH; bitIndex++) {
                if (((referenceMapByte >>> bitIndex) & 1) != 0) {
                    final int slotIndex = baseSlotIndex + bitIndex;
                    Debug.print("      Slot: ");
                    printSlot(slotIndex, framePointer);
                    Debug.println();
                }
            }
        }
    }

    private TargetMethod _trampolineTargetMethod;
    private Pointer _trampolineFramePointer;

    /**
     * Gets the constant pool index that is the operand of an invoke bytecode.
     *
     * @param code a method's bytecode
     * @param invokeOpcodePosition the bytecode position of an invokeXXX instruction in the method
     * @return the constant pool index operand directly following the opcode (in 2 bytes, big endian, unsigned, see JVM spec)
     */
    private static int getInvokeConstantPoolIndexOperand(byte[] code, int invokeOpcodePosition) {
        return ((code[invokeOpcodePosition + 1] & 0xff) << 8) | (code[invokeOpcodePosition + 2] & 0xff);
    }

    /**
     * A helper class for preparing the reference map for the frame of a call to a trampoline from a JIT compiled
     * method. The primary reason for this functionality being modeled with a helper class instead of a method is
     * to use the mechanism for {@linkplain SignatureDescriptor#visitParameterDescriptors(ParameterVisitor, boolean) traversing}
     * the parameter types in a method signature.
     *
     * The JIT normally does not consider outgoing parameters, because these are treated by the callee. Here, we do not
     * have a callee yet, but a polymorphic trampoline instead. In this situation we need to preserve the references in
     * outgoing parameters on the JIT stack with a special effort. Analyzing the invokeXXX bytecode at the call site, we
     * find out what the expected callee signature is and mark those parameter stack slots that contain references.
     */
    final class TrampolineFrameForJITCallerReferenceMapPreparer implements ParameterVisitor {
        /**
         * The slot index corresponding to the frame pointer.
         */
        private int _framePointerSlotIndex;
        private int _parameterIndex;

        /**
         * Prepares the reference map for the frame of a call to a trampoline from a JIT compiled method.
         */
        void run(JitTargetMethod caller, Pointer instructionPointer, Pointer stackPointer) {
            FatalError.check(_parameterIndex == 0, "parameterSlotIndex != 0");

            _framePointerSlotIndex = referenceMapBitIndex(stackPointer);

            // The instruction pointer is now just beyond the call machine instruction.
            // Just in case the call happens to be the last machine instruction for the invoke bytecode we are interested in, we subtract one byte.
            // Thus we always look up what bytecode we were in during the call,
            final int bytecodePosition = caller.bytecodePositionFor(instructionPointer.minus(1));

            final CodeAttribute codeAttribute = caller.classMethodActor().codeAttribute();
            final ConstantPool constantPool = codeAttribute.constantPool();
            final byte[] code = codeAttribute.code();
            final MethodRefConstant methodConstant = constantPool.methodAt(getInvokeConstantPoolIndexOperand(code, bytecodePosition));
            final boolean isInvokestatic = (code[bytecodePosition] & 0xFF) == Bytecode.INVOKESTATIC.ordinal();

            if (Heap.traceGC()) {
                Debug.print("    Bytecode position: ");
                Debug.println(bytecodePosition);
                Debug.print("           Slot index: ");
                Debug.println(_framePointerSlotIndex);
                Debug.print("     Is invokestatic?: ");
                Debug.println(isInvokestatic);
            }
            // Process the parameters first
            methodConstant.signature(constantPool).visitParameterDescriptors(this, true);

            // Now deal with the receiver (if any)
            if (!isInvokestatic) {
                final int slotIndex = _framePointerSlotIndex + _parameterIndex;
                traceReceiver(stackPointer, slotIndex);
                // Mark the slot for the receiver, which is not covered by the method signature:
                _referenceMap.setBit(slotIndex);
            }
            _parameterIndex = 0;
        }

        public void visit(TypeDescriptor parameterTypeDescriptor) {
            final Kind parameterKind = parameterTypeDescriptor.toKind();
            if (parameterKind == Kind.REFERENCE) {
                final int slotIndex = _framePointerSlotIndex + _parameterIndex;
                traceTypeDescriptor(parameterTypeDescriptor, slotIndex);
                // Mark the slot for the receiver, which is not covered by the method signature:
                _referenceMap.setBit(slotIndex);
                _referenceMap.setBit(slotIndex);
            } else {
                traceTypeDescriptor(parameterTypeDescriptor);
            }
            _parameterIndex++;
            if (parameterKind.isCategory2()) {
                _parameterIndex++;
            }
        }

        private void traceReceiver(Pointer stackPointer, final int slotIndex) {
            if (Heap.traceGC()) {
                Debug.print("    Parameter: param index=");
                Debug.print(_parameterIndex);
                printSlot(slotIndex, stackPointer);
                Debug.println(", receiver");
            }
        }

        private void traceTypeDescriptor(TypeDescriptor parameterTypeDescriptor) {
            if (Heap.traceGC()) {
                Debug.print("    Parameter: type=");
                Debug.println(parameterTypeDescriptor.string());
            }
        }

        private void traceTypeDescriptor(TypeDescriptor parameterTypeDescriptor, final int slotIndex) {
            if (Heap.traceGC()) {
                Debug.print("    Parameter: param index=");
                Debug.print(_parameterIndex);
                printSlot(slotIndex, slotAddress(_framePointerSlotIndex));
                Debug.print(", type=");
                Debug.println(parameterTypeDescriptor.string());
            }
        }
    }

    private final TrampolineFrameForJITCallerReferenceMapPreparer _trampolineFrameForJITCallerReferenceMapPreparer = new TrampolineFrameForJITCallerReferenceMapPreparer();

    private void setTrampolineStackSlotBitForRegister(int framePointerSlotIndex, int parameterRegisterIndex) {
        _referenceMap.setBit(framePointerSlotIndex + parameterRegisterIndex);
        if (Heap.traceGC()) {
            Debug.print("    Parameter: register index=");
            Debug.print(parameterRegisterIndex);
            printSlot(framePointerSlotIndex + parameterRegisterIndex, slotAddress(framePointerSlotIndex));
            Debug.println();
        }
    }

    /**
     * A helper class for preparing the reference map for the frame of a call to a trampoline from an opto compiled
     * method. The primary reason for this functionality being modeled with a helper class instead of a method is to use
     * the mechanism for {@linkplain SignatureDescriptor#visitParameterDescriptors(ParameterVisitor, boolean) traversing}
     * the parameter types in a method signature.
     *
     * An opto-compiled caller may pass some arguments in registers. The trampoline is polymorphic, i.e. it does not have any
     * helpful maps regarding the actual callee. It does store all potential parameter registers on its stack, though,
     * and recovers them before returning. We mark those that contain references.
     */
    final class TrampolineFrameForOptimizedCallerReferenceMapPreparer implements ParameterVisitor {
        private int _framePointerSlotIndex;
        private int _parameterRegisterIndex;

        /**
         * Prepares the reference map for the frame of a call to a trampoline from an opto compiled method.
         */
        void run(TargetMethod caller, int callerStopIndex) {
            _framePointerSlotIndex = referenceMapBitIndex(_trampolineFramePointer);

            _parameterRegisterIndex = 0;

            ClassMethodActor callee;
            final TrampolineMethodActor trampolineMethodActor = (TrampolineMethodActor) _trampolineTargetMethod.classMethodActor();
            if (trampolineMethodActor.invocation() == TRAMPOLINE.Invocation.STATIC) {
                callee = caller.directCallees()[callerStopIndex];
            } else {
                final Object receiver = _trampolineFramePointer.getReference().toJava();
                final ClassActor classActor = ObjectAccess.readClassActor(receiver);

                assert _trampolineTargetMethod.referenceLiterals().length == 1;
                final DynamicTrampoline dynamicTrampoline = (DynamicTrampoline) _trampolineTargetMethod.referenceLiterals()[0];

                if (trampolineMethodActor.invocation() == TRAMPOLINE.Invocation.VIRTUAL) {
                    callee = classActor.getVirtualMethodActorByVTableIndex(dynamicTrampoline.dispatchTableIndex());
                } else {
                    callee = classActor.getVirtualMethodActorByIIndex(dynamicTrampoline.dispatchTableIndex());
                }
            }
            if (Heap.traceGC()) {
                Debug.print("    Callee: ");
                Debug.printMethodActor(callee, true);
            }
            if (!callee.isStatic()) {
                setTrampolineStackSlotBitForRegister(_framePointerSlotIndex, 0);
                _parameterRegisterIndex++;
            }

            callee.descriptor().visitParameterDescriptors(this, false);
        }

        public void visit(TypeDescriptor parameterTypeDescriptor) {
            final Kind parameterKind = parameterTypeDescriptor.toKind();
            if (parameterKind == Kind.REFERENCE) {
                setTrampolineStackSlotBitForRegister(_framePointerSlotIndex, _parameterRegisterIndex);
            }
            // TODO: ask the abi whether this kind of parameter may use an integer register or not
            if (_trampolineTargetMethod.abi().putIntoIntegerRegister(parameterKind)) {
                _parameterRegisterIndex++;
                if (_parameterRegisterIndex >= _trampolineTargetMethod.abi().integerIncomingParameterRegisters().length()) {
                    // done since all subsequent parameters are known to be passed on the stack
                    return;
                }
            }
        }
    }

    private final TrampolineFrameForOptimizedCallerReferenceMapPreparer _trampolineFrameForOptimizedCallerReferenceMapPreparer = new TrampolineFrameForOptimizedCallerReferenceMapPreparer();

    /**
     * Prepares the part of the reference map corresponding to a single stack frame of a VM thread.
     *
     * @param targetMethod the method being executed in the frame
     * @param instructionPointer the current execution point in {@code targetMethod}
     * @param stackPointer the stack pointer of the frame
     * @param framePointer the stack pointer of the frame
     * @return false to communicate to the enclosing stack walker that this is the last frame to be walked; true if the
     *         stack walker should continue to the next frame
     */
    public boolean prepareFrameReferenceMap(TargetMethod targetMethod, Pointer instructionPointer, Pointer stackPointer, Pointer framePointer) {
        if (targetMethod.classMethodActor() instanceof TrampolineMethodActor) {
            // Since trampolines are reused for different callees with different parameter signatures,
            // they do not carry enough reference map information for incoming parameters.
            // We need to find out what the actual callee is before preparing the trampoline frame reference map.
            // Rather than starting a stack walk from here,
            // we simply delay processing of the trampoline frame (see below)
            // until we meet the caller frame during the stack walk we are already in.
            // For this purpose, we remember this information about the trampoline frame:
            _trampolineTargetMethod = targetMethod;
            _trampolineFramePointer = framePointer;
        } else {
            final int stopIndex = targetMethod.findClosestStopIndex(instructionPointer);
            if (stopIndex < 0) {
                throw FatalError.unexpected("Could not find stop position in target method");
            }

            if (_trampolineTargetMethod != null) {
                if (Heap.traceGC()) {
                    Debug.print("  Preparing trampoline frame reference map called by ");
                    Debug.print((targetMethod instanceof JitTargetMethod) ? "JIT'ed" : "optimized");
                    Debug.print(" caller ");
                    Debug.printMethodActor(targetMethod.classMethodActor(), true);
                }
                if (targetMethod instanceof JitTargetMethod) {
                    // This is a call from a JIT target method to a trampoline.
                    _trampolineFrameForJITCallerReferenceMapPreparer.run((JitTargetMethod) targetMethod, instructionPointer, stackPointer);
                } else {
                    // This is a call from an optimized target method to a trampoline.
                    _trampolineFrameForOptimizedCallerReferenceMapPreparer.run(targetMethod, stopIndex);
                }
                // Done processing this trampoline frame:
                _trampolineTargetMethod = null;
                _trampolineFramePointer = Pointer.zero();
            }
            prepareFrameReferenceMap(targetMethod, stopIndex, framePointer);
        }
        return true;
    }

    /**
     * Clear a range of bits in the reference map. The reference map bits at indices in the inclusive range
     * {@code [lowestSlotIndex .. highestSlotIndex]} are zeroed. No other bits in the reference map are modified.
     *
     * @param vmThreadLocals a pointer to the VM thread locals corresponding to the stack to scan
     * @param lowestSlot the address of the lowest slot to clear
     * @param highestSlot the address of the highest slot to clear
     */
    public static void clearReferenceMapRange(Pointer vmThreadLocals, Pointer lowestSlot, Pointer highestSlot) {
        checkValidReferenceMapRange(vmThreadLocals, lowestSlot, highestSlot);
        final Pointer lowestStackSlot = VmThreadLocal.LOWEST_STACK_SLOT_ADDRESS.getConstantWord(vmThreadLocals).asPointer();
        final Pointer referenceMap = VmThreadLocal.STACK_REFERENCE_MAP.getConstantWord(vmThreadLocals).asPointer();
        final int highestRefMapByteIndex = referenceMapByteIndex(lowestStackSlot, highestSlot);
        final int lowestRefMapByteIndex = referenceMapByteIndex(lowestStackSlot, lowestSlot);

        // Handle the lowest and highest reference map bytes separately as they may contain bits
        // for slot addresses lower than 'lowestSlot' and higher than 'highestSlot' respectively.
        // These bits must be preserved.
        final int lowestBitIndex = referenceMapBitIndex(lowestStackSlot, lowestSlot);
        final int highestBitIndex = referenceMapBitIndex(lowestStackSlot, highestSlot);
        final int lowestRefMapBytePreservedBits = (1 << (lowestBitIndex % Bytes.WIDTH)) - 1;
        final int highestRefMapBytePreservedBits = ~((1 << ((highestBitIndex + 1) % Bytes.WIDTH)) - 1);
        if (lowestRefMapByteIndex == highestRefMapByteIndex) {
            final byte singleRefMapByte = referenceMap.readByte(lowestRefMapByteIndex);
            final int singleRefMapBytePreservedBits = lowestRefMapBytePreservedBits | highestRefMapBytePreservedBits;
            referenceMap.writeByte(lowestRefMapByteIndex, (byte) (singleRefMapByte & singleRefMapBytePreservedBits));
        } else {
            byte lowestRefMapByte = referenceMap.readByte(lowestRefMapByteIndex);
            byte highestRefMapByte = referenceMap.readByte(highestRefMapByteIndex);
            lowestRefMapByte = (byte) (lowestRefMapByte & lowestRefMapBytePreservedBits);
            highestRefMapByte = (byte) (highestRefMapByte & highestRefMapBytePreservedBits);
            referenceMap.writeByte(lowestRefMapByteIndex, lowestRefMapByte);
            referenceMap.writeByte(highestRefMapByteIndex, highestRefMapByte);

            for (int refMapByteIndex = lowestRefMapByteIndex + 1; refMapByteIndex < highestRefMapByteIndex; refMapByteIndex++) {
                referenceMap.writeByte(refMapByteIndex, (byte) 0);
            }
        }
    }

    /**
     * Scan references in the stack in the specified interval [lowestSlot, highestSlot]. Note that this method
     * always inspects complete reference map bytes, and thus assumes that bits corresponding to these extra roundoff
     * slots at the beginning and end of the interval are zero.
     *
     * @param vmThreadLocals a pointer to the VM thread locals corresponding to the stack to scan
     * @param lowestSlot the address of the lowest slot to scan
     * @param highestSlot the address of the highest slot to scan
     * @param wordPointerIndexVisitor the visitor to apply to each slot that is a reference
     */
    public static void scanReferenceMapRange(Pointer vmThreadLocals, Pointer lowestSlot, Pointer highestSlot, PointerIndexVisitor wordPointerIndexVisitor) {
        checkValidReferenceMapRange(vmThreadLocals, lowestSlot, highestSlot);
        final Pointer lowestStackSlot = VmThreadLocal.LOWEST_STACK_SLOT_ADDRESS.getConstantWord(vmThreadLocals).asPointer();
        final Pointer referenceMap = VmThreadLocal.STACK_REFERENCE_MAP.getConstantWord(vmThreadLocals).asPointer();
        final int highestRefMapByteIndex = referenceMapByteIndex(lowestStackSlot, highestSlot);
        final int lowestRefMapByteIndex = referenceMapByteIndex(lowestStackSlot, lowestSlot);

        // Handle the lowest reference map byte separately as it may contain bits
        // for slot addresses lower than 'lowestSlot'. These bits must be ignored:
        final int lowestBitIndex = referenceMapBitIndex(lowestStackSlot, lowestSlot);
        final int highestBitIndex = referenceMapBitIndex(lowestStackSlot, highestSlot);
        if (highestRefMapByteIndex == lowestRefMapByteIndex) {
            scanReferenceMapByte(lowestRefMapByteIndex, lowestStackSlot, referenceMap, lowestBitIndex % Bytes.WIDTH, highestBitIndex % Bytes.WIDTH, vmThreadLocals, wordPointerIndexVisitor);
        } else {
            scanReferenceMapByte(lowestRefMapByteIndex, lowestStackSlot, referenceMap, lowestBitIndex % Bytes.WIDTH, Bytes.WIDTH, vmThreadLocals, wordPointerIndexVisitor);
            scanReferenceMapByte(highestRefMapByteIndex, lowestStackSlot, referenceMap, 0, (highestBitIndex % Bytes.WIDTH) + 1, vmThreadLocals, wordPointerIndexVisitor);

            for (int refMapByteIndex = lowestRefMapByteIndex + 1; refMapByteIndex < highestRefMapByteIndex; refMapByteIndex++) {
                scanReferenceMapByte(refMapByteIndex, lowestStackSlot, referenceMap, 0, Bytes.WIDTH, vmThreadLocals, wordPointerIndexVisitor);
            }
        }
    }

    private static void scanReferenceMapByte(int refMapByteIndex, Pointer lowestStackSlot, Pointer referenceMap, int startBit, int endBit, Pointer vmThreadLocals, PointerIndexVisitor wordPointerIndexVisitor) {
        final int refMapByte = referenceMap.getByte(refMapByteIndex);
        if (refMapByte != 0) {
            final int baseIndex = refMapByteIndex * Bytes.WIDTH;
            final Pointer slot = lowestStackSlot.plus(baseIndex * Word.size());
            for (int bitIndex = startBit; bitIndex < endBit; bitIndex++) {
                if (((refMapByte >>> bitIndex) & 1) != 0) {
                    if (Heap.traceGC()) {
                        Debug.print("    Slot: ");
                        printSlot(baseIndex + bitIndex, vmThreadLocals, Pointer.zero());
                        Debug.println();
                    }
                    wordPointerIndexVisitor.visitPointerIndex(slot, bitIndex);
                }
            }
        }
    }

    @INLINE
    private static int referenceMapByteIndex(final Pointer lowestStackSlot, Pointer slot) {
        return (slot.minus(lowestStackSlot).toInt() / Word.size()) / Bytes.WIDTH;
    }

    @INLINE
    private static int referenceMapWordIndex(final Pointer lowestStackSlot, Pointer slot) {
        return (slot.minus(lowestStackSlot).toInt() / Word.size()) / Word.numberOfBits();
    }

    @INLINE
    private static int referenceMapBitIndex(final Pointer lowestStackSlot, Pointer slot) {
        return slot.minus(lowestStackSlot).toInt() / Word.size();
    }

    private static void checkValidReferenceMapRange(Pointer vmThreadLocals, Pointer lowestSlot, Pointer highestSlot) {
        final Pointer lowestStackSlot = VmThreadLocal.LOWEST_STACK_SLOT_ADDRESS.getConstantWord(vmThreadLocals).asPointer();
        final Pointer highestStackSlot = VmThreadLocal.HIGHEST_STACK_SLOT_ADDRESS.getConstantWord(vmThreadLocals).asPointer();
        if (highestSlot.lessThan(lowestSlot)) {
            FatalError.unexpected("invalid reference map range: highest slot is less than lowest slot");
        }
        if (highestSlot.greaterThan(highestStackSlot)) {
            FatalError.unexpected("invalid reference map range: highest slot is greater than highest stack slot");
        }
        if (lowestSlot.lessThan(lowestStackSlot)) {
            FatalError.unexpected("invalid reference map range: lowest slot is less than lowest stack slot");
        }
    }
}
