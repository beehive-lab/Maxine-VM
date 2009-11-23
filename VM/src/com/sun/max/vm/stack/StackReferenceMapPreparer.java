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
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.bytecode.refmaps.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.trampoline.*;
import com.sun.max.vm.type.*;

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
 * @author Paul Caprioli
 */
public final class StackReferenceMapPreparer implements ReferenceMapCallback {

    /**
     * An array of the VM thread locals that are GC roots.
     */
    private static VmThreadLocal[] vmThreadLocalGCRoots;

    private final VmThread owner;
    private final Timer timer = new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK);
    private Pointer triggeredVmThreadLocals;
    private Pointer enabledVmThreadLocals;
    private Pointer disabledVmThreadLocals;
    private Pointer referenceMap;
    private Pointer lowestStackSlot;
    private boolean completingReferenceMap;
    private long preparationTime;

    /**
     * This is used to skip preparation of the reference map for the top frame on a stack.  This is
     * used when a GC thread prepares its own stack reference map as the frame initiating the
     * preparation will be dead once the GC actual starts.
     *
     * @see VmThreadLocal#prepareCurrentStackReferenceMap()
     */
    private boolean ignoreCurrentFrame;

    private TargetMethod trampolineTargetMethod;
    private Pointer trampolineRefmapPointer;

    @HOSTED_ONLY
    public static void setVmThreadLocalGCRoots(VmThreadLocal[] vmThreadLocals) {
        assert vmThreadLocalGCRoots == null : "Cannot overwrite vmThreadLocalGCRoots";
        for (VmThreadLocal tl : vmThreadLocals) {
            assert tl.isReference;
        }
        vmThreadLocalGCRoots = vmThreadLocals;
    }

    private static Pointer slotAddress(int slotIndex, Pointer vmThreadLocals) {
        return LOWEST_STACK_SLOT_ADDRESS.getConstantWord(vmThreadLocals).asPointer().plusWords(slotIndex);
    }

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
        final int lowestRefMapBytePreservedBits = ~Ints.highBitsSet(lowestBitIndex % Bytes.WIDTH);
        final int highestRefMapBytePreservedBits = ~Ints.lowBitsSet(highestBitIndex % Bytes.WIDTH);
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
        if (Heap.traceRootScanning()) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Cleared refmap indexes [");
            Log.print(lowestBitIndex);
            Log.print(" .. ");
            Log.print(highestBitIndex);
            Log.println("]");
            Log.unlock(lockDisabledSafepoints);
        }
    }

    /**
     * Scan references in the stack in the specified interval [lowestSlot, highestSlot].
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
            for (int bitIndex = startBit; bitIndex < endBit; bitIndex++) {
                if (((refMapByte >>> bitIndex) & 1) != 0) {
                    final int stackWordIndex = baseIndex + bitIndex;
                    if (Heap.traceRootScanning()) {
                        Log.print("    Slot: ");
                        printSlot(stackWordIndex, vmThreadLocals, Pointer.zero());
                        Log.println();
                    }
                    wordPointerIndexVisitor.visit(lowestStackSlot, stackWordIndex);
                }
            }
        }
    }

    @INLINE
    private static int referenceMapByteIndex(final Pointer lowestStackSlot, Pointer slot) {
        return Unsigned.idiv(referenceMapBitIndex(lowestStackSlot, slot), Bytes.WIDTH);
    }

    @INLINE
    private static int referenceMapBitIndex(final Pointer lowestStackSlot, Pointer slot) {
        return Unsigned.idiv(slot.minus(lowestStackSlot).toInt(), Word.size());
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
        Log.print("index=");
        Log.print(slotIndex);
        if (!framePointer.isZero()) {
            final int offset = slotAddress.minus(framePointer).toInt();
            if (offset >= 0) {
                Log.print(", fp+");
            } else {
                Log.print(", fp");
            }
            Log.print(offset);
        }
        Log.print(", address=");
        Log.print(slotAddress);
        Log.print(", value=");
        Log.print(slotAddress.readWord(0));
        if (slotAddress.lessThan(referenceMap)) {
            final Pointer enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
            final Pointer disabledVmThreadLocals = SAFEPOINTS_DISABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
            final Pointer triggeredVmThreadLocals = SAFEPOINTS_TRIGGERED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
            if (slotAddress.greaterEqual(disabledVmThreadLocals)) {
                Log.print(", name=");
                final int vmThreadLocalIndex = slotAddress.minus(disabledVmThreadLocals).dividedBy(Word.size()).toInt();
                Log.print(values().get(vmThreadLocalIndex).name);
            } else if (slotAddress.greaterEqual(enabledVmThreadLocals)) {
                Log.print(", name=");
                final int vmThreadLocalIndex = slotAddress.minus(enabledVmThreadLocals).dividedBy(Word.size()).toInt();
                Log.print(values().get(vmThreadLocalIndex).name);
            } else if (slotAddress.greaterEqual(triggeredVmThreadLocals)) {
                Log.print(", name=");
                final int vmThreadLocalIndex = slotAddress.minus(triggeredVmThreadLocals).dividedBy(Word.size()).toInt();
                Log.print(values().get(vmThreadLocalIndex).name);
            }
        }
    }

    public StackReferenceMapPreparer(VmThread owner) {
        this.owner = owner;
    }

    /**
     * Gets the time taken for the last call to {@link #prepareStackReferenceMap(Pointer, Pointer, Pointer, Pointer, boolean)}.
     * If there was an interleaving call to {@link #completeStackReferenceMap(Pointer, Pointer, Pointer, Pointer)}, then that
     * time is included as well. That is, this method gives the amount of time spent preparing the stack
     * reference map for the associated thread during the last/current GC.
     *
     * @return a time in the resolution specified by {@link HeapScheme#GC_TIMING_CLOCK}
     */
    public long preparationTime() {
        return preparationTime;
    }

    /**
     * Prepares a reference map for the entire stack of a VM thread
     * while the GC has not changed anything yet.
     *
     * Later on, the GC can quickly scan the prepared stack reference map
     * without allocation and without using any object references (other than the ones subject to root scanning).
     *
     * @param vmThreadLocals
     * @param instructionPointer
     * @param stackPointer
     * @param framePointer
     * @param ignoreTopFrame specifies if the top frame is to be ignored
     * @return the amount of time (in the resolution specified by {@link HeapScheme#GC_TIMING_CLOCK}) taken to prepare the reference map
     */
    public long prepareStackReferenceMap(Pointer vmThreadLocals, Pointer instructionPointer, Pointer stackPointer, Pointer framePointer, boolean ignoreTopFrame) {
        timer.start();
        ignoreCurrentFrame = ignoreTopFrame;
        enabledVmThreadLocals = SAFEPOINTS_ENABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
        disabledVmThreadLocals = SAFEPOINTS_DISABLED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
        triggeredVmThreadLocals = SAFEPOINTS_TRIGGERED_THREAD_LOCALS.getConstantWord(vmThreadLocals).asPointer();
        referenceMap = STACK_REFERENCE_MAP.getConstantWord(vmThreadLocals).asPointer();
        lowestStackSlot = LOWEST_STACK_SLOT_ADDRESS.getConstantWord(vmThreadLocals).asPointer();
        final Pointer highestStackSlot = HIGHEST_STACK_SLOT_ADDRESS.getConstantWord(vmThreadLocals).asPointer();

        // Inform subsequent reference map scanning (see VmThreadLocal.scanReferences()) of the stack range covered:
        LOWEST_ACTIVE_STACK_SLOT_ADDRESS.setVariableWord(vmThreadLocals, stackPointer);

        final VmThread vmThread = VmThread.fromVmThreadLocals(vmThreadLocals);
        if (this != vmThread.stackReferenceMapPreparer()) {
            FatalError.unexpected("Cannot use stack reference map preparer of another thread");
        }

        // clear the reference map covering the stack contents and the VM thread locals
        clearReferenceMapRange(vmThreadLocals, stackPointer, highestStackSlot);
        clearReferenceMapRange(vmThreadLocals, lowestStackSlot, vmThreadLocalsEnd(vmThreadLocals));

        boolean lockDisabledSafepoints = false;
        if (Heap.traceRootScanning()) {
            lockDisabledSafepoints = Log.lock(); // Note: This lock basically serializes stack reference map preparation
            Log.print("Preparing stack reference map for thread ");
            Log.printThread(vmThread, false);
            Log.println(":");
            Log.print("  Highest slot: ");
            Log.print(highestStackSlot);
            Log.print(" [index=");
            Log.print(referenceMapBitIndex(highestStackSlot));
            Log.println("]");
            Log.print("  Lowest active slot: ");
            Log.print(stackPointer);
            Log.print(" [index=");
            Log.print(referenceMapBitIndex(stackPointer));
            Log.println("]");
            Log.print("  Lowest slot: ");
            Log.print(lowestStackSlot);
            Log.print(" [index=");
            Log.print(referenceMapBitIndex(lowestStackSlot));
            Log.println("]");
            Log.print("  Current thread is ");
            Log.printCurrentThread(true);
        }

        // prepare references for each of the vm thread locals copies
        prepareVmThreadLocalsReferenceMap(enabledVmThreadLocals);
        prepareVmThreadLocalsReferenceMap(disabledVmThreadLocals);
        prepareVmThreadLocalsReferenceMap(triggeredVmThreadLocals);

        // walk the stack and prepare references for each stack frame
        final StackFrameWalker stackFrameWalker = vmThread.unwindingOrReferenceMapPreparingStackFrameWalker();
        stackFrameWalker.prepareReferenceMap(instructionPointer, stackPointer, framePointer, this);

        if (Heap.traceRootScanning()) {
            Log.unlock(lockDisabledSafepoints);
        }
        timer.stop();
        final long time = timer.getLastElapsedTime();
        preparationTime = time;
        return time;
    }

    /**
     * Completes the stack reference map for a thread that was suspended by a safepoint while executing Java code. The
     * reference map covering the stack between the frame in which the safepoint trap occurred and the JNI stub that
     * enters into the native code for blocking on the global {@linkplain VmThreadMap#ACTIVE thread lock} is not yet
     * prepared. This method completes this part of the threads stack reference map.
     *
     * @param vmThreadLocals the VM thread locals for the thread whose stack reference map is to be completed
     */
    public void completeStackReferenceMap(Pointer vmThreadLocals) {
        FatalError.check(!ignoreCurrentFrame, "All frames should be scanned when competing a stack reference map");
        Pointer anchor = LAST_JAVA_FRAME_ANCHOR.getVariableWord(vmThreadLocals).asPointer();
        Pointer instructionPointer = JavaFrameAnchor.PC.get(anchor);
        Pointer stackPointer = JavaFrameAnchor.SP.get(anchor);
        Pointer framePointer = JavaFrameAnchor.FP.get(anchor);
        if (instructionPointer.isZero()) {
            FatalError.unexpected("A mutator thread in Java at safepoint should be blocked on a monitor");
        }
        timer.start();
        final Pointer highestSlot = LOWEST_ACTIVE_STACK_SLOT_ADDRESS.getVariableWord(vmThreadLocals).asPointer();

        // Inform subsequent reference map scanning (see VmThreadLocal.scanReferences()) of the stack range covered:
        LOWEST_ACTIVE_STACK_SLOT_ADDRESS.setVariableWord(vmThreadLocals, stackPointer);

        final VmThread vmThread = VmThread.fromVmThreadLocals(vmThreadLocals);
        if (this != vmThread.stackReferenceMapPreparer()) {
            FatalError.unexpected("Cannot use stack reference map preparer of another thread");
        }

        // clear the reference map covering the as-yet-unprepared stack contents
        clearReferenceMapRange(vmThreadLocals, stackPointer, highestSlot.minus(Word.size()));

        boolean lockDisabledSafepoints = false;
        if (Heap.traceRootScanning()) {
            lockDisabledSafepoints = Log.lock(); // Note: This lock basically serializes stack reference map preparation
            Log.print("Completing preparation of stack reference map for thread ");
            Log.printThread(vmThread, false);
            Log.println(":");
            Log.print("  Highest slot: ");
            Log.print(highestSlot);
            Log.print(" [index=");
            Log.print(referenceMapBitIndex(highestSlot));
            Log.println("]");
            Log.print("  Lowest active slot: ");
            Log.print(stackPointer);
            Log.print(" [index=");
            Log.print(referenceMapBitIndex(stackPointer));
            Log.println("]");
            Log.print("  Lowest slot: ");
            Log.print(lowestStackSlot);
            Log.print(" [index=");
            Log.print(referenceMapBitIndex(lowestStackSlot));
            Log.println("]");
            Log.print("  Current thread is ");
            Log.printCurrentThread(true);
        }

        // walk the stack and prepare references for each stack frame
        final StackFrameWalker stackFrameWalker = vmThread.unwindingOrReferenceMapPreparingStackFrameWalker();
        completingReferenceMap = true;
        stackFrameWalker.prepareReferenceMap(instructionPointer, stackPointer, framePointer, this);
        completingReferenceMap = false;

        if (Heap.traceRootScanning()) {
            Log.unlock(lockDisabledSafepoints);
        }
        timer.stop();
        preparationTime += timer.getLastElapsedTime();
    }

    private void prepareVmThreadLocalsReferenceMap(Pointer vmThreadLocals) {
        for (VmThreadLocal local : vmThreadLocalGCRoots) {
            setReferenceMapBit(local.pointer(vmThreadLocals));
        }
    }

    public void setReferenceMapBit(Pointer slotAddress) {
        referenceMap.setBit(referenceMapBitIndex(lowestStackSlot, slotAddress));
    }

    private void printSlot(int slotIndex, Pointer framePointer) {
        printSlot(slotIndex, triggeredVmThreadLocals, framePointer);
    }

    /**
     * Prepares a reference map for the entire stack of a VM thread executing or blocked in native code.
     *
     * @param vmThreadLocals a pointer to the VM thread locals denoting the thread stack whose reference map is to be prepared
     */
    public void prepareStackReferenceMap(Pointer vmThreadLocals) {
        Pointer anchor = LAST_JAVA_FRAME_ANCHOR.getVariableWord(vmThreadLocals).asPointer();
        Pointer instructionPointer = JavaFrameAnchor.PC.get(anchor);
        Pointer stackPointer = JavaFrameAnchor.SP.get(anchor);
        Pointer framePointer = JavaFrameAnchor.FP.get(anchor);
        if (instructionPointer.isZero()) {
            FatalError.unexpected("Thread is not stopped");
        }
        prepareStackReferenceMap(vmThreadLocals, instructionPointer, stackPointer, framePointer, false);
    }

    /**
     * Prepares a reference map for the stack of a VM thread that was stopped by a safepoint. This method
     * prepares the reference map for all the frames starting with the one in which the trap occurred and
     * ending with the frame for {@link VmThread#run}.
     *
     * @param vmThreadLocals a pointer to the VM thread locals denoting the thread stack whose reference map is to be prepared
     * @param trapState the trap state
     */
    public void prepareStackReferenceMapFromTrap(Pointer vmThreadLocals, Pointer trapState) {
        final TrapStateAccess trapStateAccess = TrapStateAccess.instance();
        final Pointer instructionPointer = trapStateAccess.getInstructionPointer(trapState);
        final TargetMethod targetMethod = Code.codePointerToTargetMethod(instructionPointer);
        final Pointer stackPointer = trapStateAccess.getStackPointer(trapState, targetMethod);
        final Pointer framePointer = trapStateAccess.getFramePointer(trapState, targetMethod);
        prepareStackReferenceMap(vmThreadLocals, instructionPointer, stackPointer, framePointer, false);
    }

    /**
     * Gets the reference-map index of a given stack slot (i.e. which bit in the reference map is correlated with the slot).
     *
     * @param slotAddress an address within the range of stack addresses covered by the reference map
     * @return the index of the bit for {@code slotAddress} in the reference map
     */
    public int referenceMapBitIndex(Address slotAddress) {
        return referenceMapBitIndex(lowestStackSlot, slotAddress.asPointer());
    }

    /**
     * Gets the address of a stack slot given a slot index.
     *
     * @param slotIndex the slot index for which the address is requested
     * @return the address of the stack slot denoted by {@code slotIndex}
     */
    private Pointer slotAddress(int slotIndex) {
        return lowestStackSlot.plusWords(slotIndex);
    }

    public void tracePrepareReferenceMap(TargetMethod targetMethod, int stopIndex, Pointer refmapFramePointer, String label) {
        if (Heap.traceRootScanning()) {
            Log.print("  Preparing reference map for ");
            Log.print(label);
            Log.print(" of ");
            if (targetMethod instanceof JitTargetMethod) {
                Log.print("JitTargetMethod ");
            }
            Log.printMethod(targetMethod.classMethodActor(), false);
            Log.print(" +");
            Log.println(targetMethod.stopPosition(stopIndex));
            Log.print("    Stop index: ");
            Log.println(stopIndex);
            if (!refmapFramePointer.isZero()) {
                Log.print("    Frame pointer: ");
                printSlot(referenceMapBitIndex(refmapFramePointer), Pointer.zero());
                Log.println();
            }
        }
    }

    /**
     * If {@linkplain Heap#traceRootScanning() GC tracing} is enabled, then this method traces one byte's worth
     * of a frame/register reference map.
     *
     * @param byteIndex the index of the reference map byte
     * @param referenceMapByte the value of the reference map byte
     * @param referenceMapLabel a label indicating whether this reference map is for a frame or for the registers
     */
    public void traceReferenceMapByteBefore(int byteIndex, final byte referenceMapByte, String referenceMapLabel) {
        if (Heap.traceRootScanning()) {
            Log.print("    ");
            Log.print(referenceMapLabel);
            Log.print(" map byte index: ");
            Log.println(byteIndex);
            Log.print("    ");
            Log.print(referenceMapLabel);
            Log.print(" map byte:       ");
            Log.println(Address.fromInt(referenceMapByte & 0xff));
        }
    }

    /**
     * If {@linkplain Heap#traceRootScanning() GC tracing} is enabled, then this method traces the stack slots corresponding to a
     * frame or set of set of saved registers that are determined to contain references by a reference map.
     *
     * @param framePointer the frame pointer. This value should be {@link Pointer#zero()} if the reference map is for a
     *            set of saved registers.
     * @param baseSlotIndex the index of the slot corresponding to bit 0 of {@code referenceMapByte}
     * @param referenceMapByte a the reference map byte
     */
    public void traceReferenceMapByteAfter(Pointer framePointer, int baseSlotIndex, final byte referenceMapByte) {
        if (Heap.traceRootScanning()) {
            for (int bitIndex = 0; bitIndex < Bytes.WIDTH; bitIndex++) {
                if (((referenceMapByte >>> bitIndex) & 1) != 0) {
                    final int slotIndex = baseSlotIndex + bitIndex;
                    Log.print("      Slot: ");
                    printSlot(slotIndex, framePointer);
                    Log.println();
                }
            }
        }
    }

    /**
     * Prepares the reference map to cover the reference parameters on the stack at a call from a JIT compiled method
     * into a trampoline. These slots are normally ignored when computing the reference maps for a JIT'ed method as they
     * are covered by a reference map in the callee if necessary. They <b>cannot</b> be covered by a reference map in
     * the JIT'ed method as these slots are seen as local variables in a JIT callee and as such can be overwritten with
     * non-reference values.
     *
     * However, in the case where a JIT'ed method calls into a trampoline, the reference parameters of the call are not
     * covered by any reference map. In this situation, we need to analyze the invokeXXX bytecode at the call site to
     * derive the signature of the callee which in turn allows us to mark the parameter stack slots that contain
     * references.
     *
     * @param caller the JIT compiled method that that made the call into the trampoline frame
     * @param instructionPointer the execution address in {@code caller}. This will be at the site of the call to the trampoline.
     * @param refmapFramePointer the address in the frame of {@code caller} to which the reference map for {@code caller} is relative
     * @param operandStackPointer pointer to the top value on the operand stack in the frame of {@code caller}
     */
    private void prepareTrampolineFrameForJITCaller(JitTargetMethod caller, Pointer instructionPointer, Pointer refmapFramePointer, Pointer operandStackPointer) {
        final int bytecodePosition = caller.bytecodePositionForCallSite(instructionPointer);
        final CodeAttribute codeAttribute = caller.classMethodActor().codeAttribute();
        final ConstantPool constantPool = codeAttribute.constantPool;
        final byte[] code = codeAttribute.code();
        final MethodRefConstant methodConstant = constantPool.methodAt(getInvokeConstantPoolIndexOperand(code, bytecodePosition));
        final boolean isInvokestatic = (code[bytecodePosition] & 0xFF) == Bytecode.INVOKESTATIC.ordinal();
        final SignatureDescriptor signature = methodConstant.signature(constantPool);

        final int numberOfSlots = signature.computeNumberOfSlots() + (isInvokestatic ? 0 : 1);

        if (Heap.traceRootScanning()) {
            Log.print("        Frame pointer: ");
            printSlot(referenceMapBitIndex(refmapFramePointer), Pointer.zero());
            Log.println();
            Log.print("    Bytecode position: ");
            Log.println(bytecodePosition);
            Log.print("               Callee: ");
            Log.print(methodConstant.name(constantPool).string);
            Log.print(methodConstant.signature(constantPool).string);
            Log.print(" in ");
            Log.println(methodConstant.holder(constantPool).string);
            Log.print("     Is invokestatic?: ");
            Log.println(isInvokestatic);
        }

        if (numberOfSlots != 0) {
            final int fpSlotIndex = referenceMapBitIndex(refmapFramePointer);
            final JitStackFrameLayout stackFrameLayout = caller.stackFrameLayout();
            final Pointer operandStackSlot0Pointer = refmapFramePointer.plus(stackFrameLayout.sizeOfOperandStack());
            final Pointer lastParameterPointer = operandStackPointer;
            final Pointer firstParameterPointer = lastParameterPointer.plus(numberOfSlots * JitStackFrameLayout.JIT_SLOT_SIZE);
            int parameterWordIndex = operandStackSlot0Pointer.minus(firstParameterPointer).dividedBy(JitStackFrameLayout.JIT_SLOT_SIZE).toInt();

            // First deal with the receiver (if any)
            if (!isInvokestatic) {
                final int fpRelativeIndex = stackFrameLayout.operandStackReferenceMapIndex(parameterWordIndex);
                final int slotIndex = fpSlotIndex + fpRelativeIndex;
                traceReceiver(refmapFramePointer, slotIndex);
                // Mark the slot for the receiver as it is not covered by the method signature:
                referenceMap.setBit(slotIndex);
                parameterWordIndex++;
            }

            // Now process the other parameters
            for (int i = 0; i < signature.numberOfParameters(); ++i) {
                final TypeDescriptor parameter = signature.parameterDescriptorAt(i);
                final Kind parameterKind = parameter.toKind();
                final int fpRelativeIndex = stackFrameLayout.operandStackReferenceMapIndex(parameterKind.isCategory2() ? parameterWordIndex + 1 : parameterWordIndex);
                final int slotIndex = fpSlotIndex + fpRelativeIndex;
                if (parameterKind == Kind.REFERENCE) {
                    traceTypeDescriptor(parameter, slotIndex, parameterWordIndex);
                    referenceMap.setBit(slotIndex);
                } else {
                    traceTypeDescriptor(parameter);
                }
                parameterWordIndex += parameterKind.isCategory2() ? 2 : 1;
            }
        }
    }

    private void traceReceiver(Pointer stackPointer, final int slotIndex) {
        if (Heap.traceRootScanning()) {
            Log.print("    Parameter: param index=");
            Log.print(0);
            Log.print(", ");
            printSlot(slotIndex, Pointer.zero());
            Log.println(", receiver");
        }
    }

    private void traceTypeDescriptor(TypeDescriptor parameterTypeDescriptor) {
        if (Heap.traceRootScanning()) {
            Log.print("    Parameter: type=");
            Log.println(parameterTypeDescriptor.string);
        }
    }

    private void traceTypeDescriptor(TypeDescriptor parameterTypeDescriptor, int slotIndex, int paramIndex) {
        if (Heap.traceRootScanning()) {
            Log.print("    Parameter: param index=");
            Log.print(paramIndex);
            Log.print(", ");
            printSlot(slotIndex, Pointer.zero());
            Log.print(", type=");
            Log.println(parameterTypeDescriptor.string);
        }
    }

    private void setTrampolineStackSlotBitForRegister(int framePointerSlotIndex, int parameterRegisterIndex) {
        referenceMap.setBit(framePointerSlotIndex + parameterRegisterIndex);
        if (Heap.traceRootScanning()) {
            Log.print("    Parameter: register index=");
            Log.print(parameterRegisterIndex);
            Log.print(", ");
            printSlot(framePointerSlotIndex + parameterRegisterIndex, slotAddress(framePointerSlotIndex));
            Log.println();
        }
    }

    /**
     * Prepares the reference map for the frame of a call to a trampoline from an opto compiled method.
     *
     * An opto-compiled caller may pass some arguments in registers.  The trampoline is polymorphic, i.e. it does not have any
     * helpful maps regarding the actual callee.  It does store all potential parameter registers on its stack, though,
     * and recovers them before returning.  We mark those that contain references.
     */
    private void prepareTrampolineFrameForOptimizedCaller(TargetMethod caller, int callerStopIndex, int offsetToFirstParameter) {
        final ClassMethodActor callee;
        final ClassMethodActor trampolineMethodActor = trampolineTargetMethod.classMethodActor();
        if (trampolineMethodActor.isStaticTrampoline()) {
            callee = (ClassMethodActor) caller.directCallees()[callerStopIndex];
        } else {
            final Object receiver = trampolineRefmapPointer.plus(offsetToFirstParameter).getReference().toJava();
            final ClassActor classActor = ObjectAccess.readClassActor(receiver);
            assert trampolineTargetMethod.referenceLiterals().length == 1;
            final DynamicTrampoline dynamicTrampoline = (DynamicTrampoline) trampolineTargetMethod.referenceLiterals()[0];
            if (trampolineMethodActor.isVirtualTrampoline()) {
                callee = classActor.getVirtualMethodActorByVTableIndex(dynamicTrampoline.dispatchTableIndex());
            } else {
                callee = classActor.getVirtualMethodActorByIIndex(dynamicTrampoline.dispatchTableIndex());
            }
        }

        if (Heap.traceRootScanning()) {
            Log.print("    Frame pointer: ");
            printSlot(referenceMapBitIndex(trampolineRefmapPointer), Pointer.zero());
            Log.println();
            Log.print("    Callee: ");
            Log.printMethod(callee, true);
        }

        final int framePointerSlotIndex = referenceMapBitIndex(trampolineRefmapPointer.plus(offsetToFirstParameter));
        int parameterRegisterIndex = 0;
        if (!callee.isStatic()) {
            setTrampolineStackSlotBitForRegister(framePointerSlotIndex, 0);
            parameterRegisterIndex = 1;
        }

        for (int i = 0; i < callee.descriptor().numberOfParameters(); ++i) {
            final TypeDescriptor parameter = callee.descriptor().parameterDescriptorAt(i);
            final Kind parameterKind = parameter.toKind();
            if (parameterKind == Kind.REFERENCE) {
                setTrampolineStackSlotBitForRegister(framePointerSlotIndex, parameterRegisterIndex);
            }
            if (trampolineTargetMethod.abi().putIntoIntegerRegister(parameterKind)) {
                parameterRegisterIndex++;
                if (parameterRegisterIndex >= trampolineTargetMethod.abi().integerIncomingParameterRegisters().length()) {
                    // done since all subsequent parameters are known to be passed on the stack
                    return;
                }
            }
        }
    }

    public boolean checkIgnoreCurrentFrame() {
        if (ignoreCurrentFrame) {
            // Skipping the top frame
            ignoreCurrentFrame = false;
            return true;
        }
        return false;
    }

    /**
     * Prepares the part of the reference map corresponding to a single stack frame of a VM thread.
     *
     * @param targetMethod the method being executed in the frame
     * @param instructionPointer the current execution point in {@code targetMethod}
     * @param refmapFramePointer the frame pointer of the frame. The reference map entries for the frame are relative to this address.
     * @param operandStackPointer the CPU defined stack pointer (e.g. RSP on AMD64)
     * @param callee the target method whose frame is below this frame being walked (i.e. the frame of {@code
     *            targetMethod}'s callee). This will be null if {@code targetMethod} called a native function.
     * @return false to communicate to the enclosing stack walker that this is the last frame to be walked; true if the
     *         stack walker should continue to the next frame
     */
    public boolean prepareFrameReferenceMap(TargetMethod targetMethod, Pointer instructionPointer, Pointer refmapFramePointer, Pointer operandStackPointer, int offsetToFirstParameter, TargetMethod callee) {

        if (targetMethod.classMethodActor() != null && targetMethod.classMethodActor().isTrampoline()) {
            // Since trampolines are reused for different callees with different parameter signatures,
            // they do not carry enough reference map information for incoming parameters.
            // We need to find out what the actual callee is before preparing the trampoline frame reference map.
            // Rather than starting a stack walk from here,
            // we simply delay processing of the trampoline frame (see below)
            // until we meet the caller frame during the stack walk we are already in.
            // For this purpose, we remember this information about the trampoline frame:
            trampolineTargetMethod = targetMethod;
            trampolineRefmapPointer = refmapFramePointer;
        } else {
            final int stopIndex = targetMethod.findClosestStopIndex(instructionPointer);
            if (stopIndex < 0) {
                Log.print("Could not find stop position for instruction at position ");
                Log.print(instructionPointer.minus(targetMethod.codeStart()).toInt());
                Log.print(" in ");
                Log.printMethod(targetMethod.classMethodActor(), true);
                throw FatalError.unexpected("Could not find stop position in target method");
            }

            if (trampolineTargetMethod != null) {
                if (Heap.traceRootScanning()) {
                    Log.print("  Preparing reference map for trampoline frame called by ");
                    Log.print((targetMethod instanceof JitTargetMethod) ? "JIT'ed" : "optimized");
                    Log.print(" caller ");
                    Log.printMethod(targetMethod.classMethodActor(), true);
                }
                if (targetMethod instanceof JitTargetMethod) {
                    // This is a call from a JIT target method to a trampoline.
                    prepareTrampolineFrameForJITCaller((JitTargetMethod) targetMethod, instructionPointer, refmapFramePointer, operandStackPointer);
                } else {
                    // This is a call from an optimized target method to a trampoline.
                    prepareTrampolineFrameForOptimizedCaller(targetMethod, stopIndex, offsetToFirstParameter);
                }
                // Done processing this trampoline frame:
                trampolineTargetMethod = null;
                trampolineRefmapPointer = Pointer.zero();
            }
            targetMethod.prepareFrameReferenceMap(stopIndex, refmapFramePointer, this, callee);
        }

        // If the stack reference map is being completed, then the stack walk stops after the first trap stub
        if (completingReferenceMap && targetMethod.classMethodActor().isTrapStub()) {
            return false;
        }
        return true;
    }

    public void setBits(int baseSlotIndex, byte referenceMapByte) {
        referenceMap.setBits(baseSlotIndex, referenceMapByte);
    }
}
