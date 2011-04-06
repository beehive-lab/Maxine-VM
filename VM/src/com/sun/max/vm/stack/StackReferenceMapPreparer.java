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
package com.sun.max.vm.stack;

import static com.sun.cri.bytecode.Bytecodes.Infopoints.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.runtime.VMRegister.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;

import java.util.concurrent.atomic.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.*;
import com.sun.max.vm.bytecode.refmaps.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.StackFrameWalker.Cursor;
import com.sun.max.vm.thread.*;

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
 * which fills in stack reference maps for target methods compiled by the baseline compiler as needed
 * was carefully crafted to comply with this requirement.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Ben L. Titzer
 * @author Paul Caprioli
 */
public final class StackReferenceMapPreparer {

    /**
     * Flag controlling tracing of stack root scanning (SRS).
     */
    public static boolean TraceSRS;

    /**
     * Disables -XX:+TraceStackRootScanning if greater than 0.
     */
    private static int TraceSRSSuppressionCount;

    /**
     * A counter used purely for {@link #TraceSRSSuppressionCount}.
     */
    private static AtomicInteger SRSCount = new AtomicInteger();

    static {
        VMOptions.addFieldOption("-XX:", "TraceSRS", "Trace stack root scanning.");
        VMOptions.addFieldOption("-XX:", "TraceSRSSuppressionCount", "Disable tracing of the first n stack root scans.");
    }

    /**
     * Determines if stack root scanning should be traced.
     */
    public static boolean traceStackRootScanning() {
        return Heap.traceRootScanning() || (TraceSRS && TraceSRSSuppressionCount <= 0);
    }

    public static boolean VerifyRefMaps;
    static {
        VMOptions.addFieldOption("-XX:", "VerifyRefMaps", StackReferenceMapPreparer.class,
            "Verify reference maps by performing a stack walk and checking plausibility of reference roots in " +
            "the stack--as often as possible.", MaxineVM.Phase.PRISTINE);
    }

    private final Timer timer = new SingleUseTimer(HeapScheme.GC_TIMING_CLOCK);
    private Pointer ttla;
    private Pointer referenceMap;
    private Pointer lowestStackSlot;
    private Pointer completingReferenceMapLimit;
    private final boolean verify;
    private final boolean prepare;
    private long preparationTime;

    /**
     * This is used to skip preparation of the reference map for the top frame on a stack.  This is
     * used when a GC thread prepares its own stack reference map as the frame initiating the
     * preparation will be dead once the GC actual starts.
     *
     * @see VmThreadLocal#prepareCurrentStackReferenceMap()
     */
    private boolean ignoreCurrentFrame;

    public StackReferenceMapPreparer(boolean verify, boolean prepare) {
        this.verify = verify;
        this.prepare = prepare;
    }

    private static Pointer slotAddress(int slotIndex, Pointer tla) {
        return LOWEST_STACK_SLOT_ADDRESS.load(tla).plusWords(slotIndex);
    }

    /**
     * Clear a range of bits in the reference map. The reference map bits at indices in the inclusive range
     * {@code [lowestSlotIndex .. highestSlotIndex]} are zeroed. No other bits in the reference map are modified.
     *
     * @param tla a pointer to the VM thread locals corresponding to the stack to scan
     * @param lowestSlot the address of the lowest slot to clear
     * @param highestSlot the address of the highest slot to clear
     */
    public static void clearReferenceMapRange(Pointer tla, Pointer lowestSlot, Pointer highestSlot) {
        checkValidReferenceMapRange(tla, lowestSlot, highestSlot);
        Pointer lowestStackSlot = LOWEST_STACK_SLOT_ADDRESS.load(tla);
        Pointer referenceMap = STACK_REFERENCE_MAP.load(tla);
        int highestRefMapByteIndex = referenceMapByteIndex(lowestStackSlot, highestSlot);
        int lowestRefMapByteIndex = referenceMapByteIndex(lowestStackSlot, lowestSlot);

        // Handle the lowest and highest reference map bytes separately as they may contain bits
        // for slot addresses lower than 'lowestSlot' and higher than 'highestSlot' respectively.
        // These bits must be preserved.
        int lowestBitIndex = referenceMapBitIndex(lowestStackSlot, lowestSlot);
        int highestBitIndex = referenceMapBitIndex(lowestStackSlot, highestSlot);
        int lowestRefMapBytePreservedBits = ~Ints.highBitsSet(lowestBitIndex % Bytes.WIDTH);
        int highestRefMapBytePreservedBits = ~Ints.lowBitsSet(highestBitIndex % Bytes.WIDTH);
        if (lowestRefMapByteIndex == highestRefMapByteIndex) {
            byte singleRefMapByte = referenceMap.readByte(lowestRefMapByteIndex);
            int singleRefMapBytePreservedBits = lowestRefMapBytePreservedBits | highestRefMapBytePreservedBits;
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
        if (traceStackRootScanning()) {
            boolean lockDisabledSafepoints = Log.lock();
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
     * @param tla a pointer to the VM thread locals corresponding to the stack to scan
     * @param lowestSlot the address of the lowest slot to scan
     * @param highestSlot the address of the highest slot to scan
     * @param wordPointerIndexVisitor the visitor to apply to each slot that is a reference
     */
    public static void scanReferenceMapRange(Pointer tla, Pointer lowestSlot, Pointer highestSlot, PointerIndexVisitor wordPointerIndexVisitor) {
        checkValidReferenceMapRange(tla, lowestSlot, highestSlot);
        Pointer lowestStackSlot = LOWEST_STACK_SLOT_ADDRESS.load(tla);
        Pointer referenceMap = STACK_REFERENCE_MAP.load(tla);
        int highestRefMapByteIndex = referenceMapByteIndex(lowestStackSlot, highestSlot);
        int lowestRefMapByteIndex = referenceMapByteIndex(lowestStackSlot, lowestSlot);

        // Handle the lowest reference map byte separately as it may contain bits
        // for slot addresses lower than 'lowestSlot'. These bits must be ignored:
        int lowestBitIndex = referenceMapBitIndex(lowestStackSlot, lowestSlot);
        int highestBitIndex = referenceMapBitIndex(lowestStackSlot, highestSlot);
        if (highestRefMapByteIndex == lowestRefMapByteIndex) {
            scanReferenceMapByte(lowestRefMapByteIndex, lowestStackSlot, referenceMap, lowestBitIndex % Bytes.WIDTH, highestBitIndex % Bytes.WIDTH, tla, wordPointerIndexVisitor);
        } else {
            scanReferenceMapByte(lowestRefMapByteIndex, lowestStackSlot, referenceMap, lowestBitIndex % Bytes.WIDTH, Bytes.WIDTH, tla, wordPointerIndexVisitor);
            scanReferenceMapByte(highestRefMapByteIndex, lowestStackSlot, referenceMap, 0, (highestBitIndex % Bytes.WIDTH) + 1, tla, wordPointerIndexVisitor);

            for (int refMapByteIndex = lowestRefMapByteIndex + 1; refMapByteIndex < highestRefMapByteIndex; refMapByteIndex++) {
                scanReferenceMapByte(refMapByteIndex, lowestStackSlot, referenceMap, 0, Bytes.WIDTH, tla, wordPointerIndexVisitor);
            }
        }
    }

    private static void scanReferenceMapByte(int refMapByteIndex, Pointer lowestStackSlot, Pointer referenceMap, int startBit, int endBit, Pointer tla, PointerIndexVisitor wordPointerIndexVisitor) {
        int refMapByte = referenceMap.getByte(refMapByteIndex);
        if (refMapByte != 0) {
            int baseIndex = refMapByteIndex * Bytes.WIDTH;
            for (int bitIndex = startBit; bitIndex < endBit; bitIndex++) {
                if (((refMapByte >>> bitIndex) & 1) != 0) {
                    int stackWordIndex = baseIndex + bitIndex;
                    if (traceStackRootScanning()) {
                        Log.print("    Slot: ");
                        printSlot(stackWordIndex, tla, Pointer.zero());
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

    private static void checkValidReferenceMapRange(Pointer tla, Pointer lowestSlot, Pointer highestSlot) {
        Pointer lowestStackSlot = LOWEST_STACK_SLOT_ADDRESS.load(tla);
        Pointer highestStackSlot = HIGHEST_STACK_SLOT_ADDRESS.load(tla);
        String error = null;
        if (highestSlot.lessThan(lowestSlot)) {
            error = "invalid reference map range: highest slot is less than lowest slot";
        } else if (highestSlot.greaterThan(highestStackSlot)) {
            error = "invalid reference map range: highest slot is greater than highest stack slot";
        } else if (lowestSlot.lessThan(lowestStackSlot)) {
            error = "invalid reference map range: lowest slot is less than lowest stack slot";
        }
        if (error != null) {
            Log.print("Error building reference map for stack of thread ");
            Log.printThread(VmThread.fromTLA(tla), false);
            Log.print(": ");
            Log.println(error);
            FatalError.unexpected(error);
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
     * @param tla a pointer to the VM thread locals corresponding to the stack containing the slot
     * @param framePointer the address of the frame pointer if known, zero otherwise
     */
    private static void printSlot(int slotIndex, Pointer tla, Pointer framePointer) {
        Pointer slotAddress = slotAddress(slotIndex, tla);
        Pointer referenceMap = STACK_REFERENCE_MAP.load(tla);
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
            Pointer etla = ETLA.load(tla);
            Pointer dtla = DTLA.load(tla);
            Pointer ttla = TTLA.load(tla);
            if (slotAddress.greaterEqual(dtla)) {
                Log.print(", name=");
                int vmThreadLocalIndex = slotAddress.minus(dtla).dividedBy(Word.size()).toInt();
                Log.print(values().get(vmThreadLocalIndex).name);
            } else if (slotAddress.greaterEqual(etla)) {
                Log.print(", name=");
                int vmThreadLocalIndex = slotAddress.minus(etla).dividedBy(Word.size()).toInt();
                Log.print(values().get(vmThreadLocalIndex).name);
            } else if (slotAddress.greaterEqual(ttla)) {
                Log.print(", name=");
                int vmThreadLocalIndex = slotAddress.minus(ttla).dividedBy(Word.size()).toInt();
                Log.print(values().get(vmThreadLocalIndex).name);
            }
        }
    }

    /**
     * Gets the time taken for the last call to {@link #prepareStackReferenceMap(Pointer, Pointer, Pointer, Pointer, boolean)}.
     * If there was an interleaving call to {@link #completeStackReferenceMap(com.sun.max.unsafe.Pointer)} completeStackReferenceMap(Pointer, Pointer, Pointer, Pointer)}, then that
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
     * @param tla
     * @param instructionPointer
     * @param stackPointer
     * @param framePointer
     * @param ignoreTopFrame specifies if the top frame is to be ignored
     * @return the amount of time (in the resolution specified by {@link HeapScheme#GC_TIMING_CLOCK}) taken to prepare the reference map
     */
    public long prepareStackReferenceMap(Pointer tla, Pointer instructionPointer, Pointer stackPointer, Pointer framePointer, boolean ignoreTopFrame) {
        timer.start();
        ignoreCurrentFrame = ignoreTopFrame;
        initRefMapFields(tla);
        Pointer highestStackSlot = HIGHEST_STACK_SLOT_ADDRESS.load(tla);

        // Inform subsequent reference map scanning (see VmThreadLocal.scanReferences()) of the stack range covered:
        LOWEST_ACTIVE_STACK_SLOT_ADDRESS.store3(tla, stackPointer);

        VmThread vmThread = VmThread.fromTLA(tla);
        if (this != vmThread.stackReferenceMapPreparer()) {
            FatalError.unexpected("Cannot use stack reference map preparer of another thread");
        }

        // clear the reference map covering the stack contents
        clearReferenceMapRange(tla, stackPointer, highestStackSlot);

        boolean lockDisabledSafepoints = traceStackRootScanStart(stackPointer, highestStackSlot, vmThread);

        // walk the stack and prepare references for each stack frame
        StackFrameWalker sfw = vmThread.referenceMapPreparingStackFrameWalker();
        sfw.prepareReferenceMap(instructionPointer, stackPointer, framePointer, this);

        traceStackRootScanEnd(lockDisabledSafepoints);

        timer.stop();
        preparationTime = timer.getLastElapsedTime();
        return preparationTime;
    }

    private void traceStackRootScanEnd(boolean lockDisabledSafepoints) {
        if (traceStackRootScanning()) {
            Log.unlock(lockDisabledSafepoints);
        }
    }

    private boolean traceStackRootScanStart(Pointer stackPointer, Pointer highestStackSlot, VmThread vmThread) {
        // Ideally this test and decrement should be atomic but it's ok
        // for TraceSRSSuppressionCount to be approximate
        if (TraceSRSSuppressionCount > 0) {
            TraceSRSSuppressionCount--;
        }
        SRSCount.incrementAndGet();
        if (traceStackRootScanning()) {
            boolean lockDisabledSafepoints = Log.lock(); // Note: This lock serializes stack reference map preparation
            Log.print('[');
            Log.print(SRSCount.get());
            Log.print(prepare ? "] Preparing" : "] Verifying");
            Log.print(" stack reference map for thread ");
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
            return lockDisabledSafepoints;
        }
        return false;
    }

    private void initRefMapFields(Pointer tla) {
        ttla = TTLA.load(tla);
        referenceMap = STACK_REFERENCE_MAP.load(tla);
        lowestStackSlot = LOWEST_STACK_SLOT_ADDRESS.load(tla);
    }

    /**
     * Completes the stack reference map for a thread that was suspended by a safepoint while executing Java code. The
     * reference map covering the stack between the frame in which the safepoint trap occurred and the JNI stub that
     * enters into the native code for blocking on the global {@linkplain VmThreadMap#THREAD_LOCK thread lock} is not yet
     * prepared. This method completes this part of the threads stack reference map.
     *
     * @param tla the VM thread locals for the thread whose stack reference map is to be completed
     */
    public void completeStackReferenceMap(Pointer tla) {
        timer.start();
        FatalError.check(!ignoreCurrentFrame, "All frames should be scanned when competing a stack reference map");
        Pointer etla = ETLA.load(tla);
        Pointer anchor = LAST_JAVA_FRAME_ANCHOR.load(etla);
        Pointer instructionPointer = JavaFrameAnchor.PC.get(anchor);
        Pointer stackPointer = JavaFrameAnchor.SP.get(anchor);
        Pointer framePointer = JavaFrameAnchor.FP.get(anchor);
        if (instructionPointer.isZero()) {
            FatalError.unexpected("A mutator thread in Java at safepoint should be blocked on a monitor");
        }
        Pointer highestSlot = LOWEST_ACTIVE_STACK_SLOT_ADDRESS.load(tla);

        // Inform subsequent reference map scanning (see VmThreadLocal.scanReferences()) of the stack range covered:
        LOWEST_ACTIVE_STACK_SLOT_ADDRESS.store3(tla, stackPointer);

        VmThread vmThread = VmThread.fromTLA(tla);
        if (this != vmThread.stackReferenceMapPreparer()) {
            FatalError.unexpected("Cannot use stack reference map preparer of another thread");
        }

        // clear the reference map covering the as-yet-unprepared stack contents
        clearReferenceMapRange(tla, stackPointer, highestSlot.minus(Word.size()));

        boolean lockDisabledSafepoints = false;
        if (traceStackRootScanning()) {
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
        StackFrameWalker sfw = vmThread.referenceMapPreparingStackFrameWalker();
        completingReferenceMapLimit = highestSlot;
        sfw.prepareReferenceMap(instructionPointer, stackPointer, framePointer, this);
        completingReferenceMapLimit = Pointer.zero();

        traceStackRootScanEnd(lockDisabledSafepoints);
        timer.stop();
        preparationTime += timer.getLastElapsedTime();
    }

    /**
     * Gets the lowest stack address for which a stack map has already been completed.
     * A zero return value indicates that this preparer is not currently in a call to {@link #completeStackReferenceMap(Pointer)}.
     */
    public Pointer completingReferenceMapLimit() {
        return completingReferenceMapLimit;
    }

    public void setReferenceMapBit(Pointer slotAddress) {
        referenceMap.setBit(referenceMapBitIndex(lowestStackSlot, slotAddress));
    }

    private void printSlot(int slotIndex, Pointer framePointer) {
        printSlot(slotIndex, ttla, framePointer);
    }

    /**
     * Prepares a reference map for the entire stack of a VM thread executing or blocked in native code.
     *
     * @param tla a pointer to the VM thread locals denoting the thread stack whose reference map is to be prepared
     */
    public void prepareStackReferenceMap(Pointer tla) {
        Pointer etla = ETLA.load(tla);
        Pointer anchor = LAST_JAVA_FRAME_ANCHOR.load(etla);
        if (anchor.isZero()) {
            // This is a thread that has returned from VmThread.run() but has not
            // yet been terminated via a call to VmThread.detach(). In this state,
            // it has no Java stack frames that need scanning.
            if (traceStackRootScanning()) {
                boolean lockDisabledSafepoints = Log.lock();
                Log.print("Empty stack reference map for thread ");
                Log.printThread(VmThread.fromTLA(tla), true);
                Log.unlock(lockDisabledSafepoints);
            }
            return;
        }
        Pointer instructionPointer = JavaFrameAnchor.PC.get(anchor);
        Pointer stackPointer = JavaFrameAnchor.SP.get(anchor);
        Pointer framePointer = JavaFrameAnchor.FP.get(anchor);
        if (instructionPointer.isZero()) {
            FatalError.unexpected("Thread is not stopped");
        }
        prepareStackReferenceMap(tla, instructionPointer, stackPointer, framePointer, false);
    }

    /**
     * Prepares a reference map for the stack of a VM thread that was stopped by a safepoint. This method
     * prepares the reference map for all the frames starting with the one in which the trap occurred and
     * ending with the frame for {@link VmThread#run}.
     *
     * @param tla a pointer to the VM thread locals denoting the thread stack whose reference map is to be prepared
     * @param trapState the trap state
     */
    public void prepareStackReferenceMapFromTrap(Pointer tla, Pointer trapState) {
        final TrapStateAccess trapStateAccess = vm().trapStateAccess;
        final Pointer instructionPointer = trapStateAccess.getPC(trapState);
        final Pointer stackPointer = trapStateAccess.getSP(trapState);
        final Pointer framePointer = trapStateAccess.getFP(trapState);
        prepareStackReferenceMap(tla, instructionPointer, stackPointer, framePointer, false);
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
        if (traceStackRootScanning()) {
            Log.print(prepare ? "  Preparing" : "  Verifying");
            Log.print(" reference map for ");
            Log.print(label);
            Log.print(" of ");
            Log.printMethod(targetMethod, false);
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
    public void traceReferenceMapByteBefore(int byteIndex, byte referenceMapByte, String referenceMapLabel) {
        if (traceStackRootScanning()) {
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
        if (traceStackRootScanning()) {
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

    public boolean checkIgnoreCurrentFrame() {
        if (ignoreCurrentFrame) {
            // Skipping the top frame
            ignoreCurrentFrame = false;
            return true;
        }
        return false;
    }

    public void setBits(int baseSlotIndex, byte referenceMapByte) {
        referenceMap.setBits(baseSlotIndex, referenceMapByte);
    }

    /**
     * Updates the reference map bits for a range of slots within a frame.
     *
     * @param cursor the cursor corresponding to the frame for which the bits are being filled in
     * @param slotPointer the pointer to the slot that corresponds to bit 0 in the reference map
     * @param refMap an integer containing up to 32 reference map bits for up to 32 successive slots in the frame
     * @param numBits the number of bits in the reference map
     */
    public void setReferenceMapBits(Cursor cursor, Pointer slotPointer, int refMap, int numBits) {
        if (traceStackRootScanning()) {
            boolean lockDisabledSafepoints = Log.lock();
            Log.print("    setReferenceMapBits: sp = ");
            Log.print(cursor.sp());
            Log.print(" fp = ");
            Log.print(cursor.fp());
            Log.print(", slots @ ");
            Log.print(slotPointer);
            Log.print(", bits = ");
            for (int i = 0; i < numBits; i++) {
                Log.print((refMap >>> i) & 1);
            }
            Log.print(", description = ");
            Log.println(cursor.targetMethod().regionName());
            Log.unlock(lockDisabledSafepoints);
        }
        if (!inThisStack(cursor.sp())) {
            throw FatalError.unexpected("sp not in this stack");
        }
        if (!inThisStack(slotPointer)) {
            throw FatalError.unexpected("slots not in this stack");
        }
        if (refMap == 0) {
            // nothing to do.
            return;
        }
        if ((refMap & (-1 << numBits)) != 0) {
            throw FatalError.unexpected("reference map has extraneous high order bits set");
        }
        if (verify) {
            // look at the contents of the stack and check they are valid refs
            for (int i = 0; i < numBits; i++) {
                if (((refMap >> i) & 1) == 1) {
                    Reference ref = slotPointer.getReference(i);
                    if (Heap.isValidRef(ref)) {
                        if (traceStackRootScanning()) {
                            printRef(ref, cursor, slotPointer, i, true);
                        }
                    } else {
                        invalidRef(ref, cursor, slotPointer, i);
                    }
                }
            }
        }
        if (prepare) {
            // copy the bits into the complete reference map for the stack
            int mapBits = refMap;
            int slotIndex = referenceMapBitIndex(slotPointer);
            int slotEnd = referenceMapBitIndex(slotPointer.plusWords(numBits));

            while (slotIndex < slotEnd) {
                int rest = slotIndex % Bytes.WIDTH; // number of bits to shift mapBits over
                int bits = Bytes.WIDTH - rest;      // number of bits from mapBits to use
                int byteIndex = Unsigned.idiv(slotIndex, Bytes.WIDTH);

                byte prev = referenceMap.getByte(byteIndex);
                prev |= mapBits << rest;
                referenceMap.setByte(byteIndex, prev);

                slotIndex += bits;
                mapBits >>>= bits;
            }
        }

    }

    private void printRef(Reference ref, Cursor cursor, Pointer slotPointer, int slotIndex, boolean valid) {
        Log.print("    ref @ ");
        Log.print(slotPointer.plusWords(slotIndex));
        Log.print(" [sp + ");
        Log.print(slotPointer.plusWords(slotIndex).minus(cursor.sp()).toInt());
        Log.print("] = ");
        Log.print(ref.toOrigin());
        Log.print(valid ? " ok\n" : " (invalid)\n");
    }

    @NEVER_INLINE
    private void invalidRef(Reference ref, Cursor cursor, Pointer slotPointer, int slotIndex) {
        printRef(ref, cursor, slotPointer, slotIndex, false);
        Log.print("invalid ref ### [SRSCount: ");
        Log.print(SRSCount.get());
        Log.print("] ");
        Throw.logFrame(null, cursor.targetMethod(), cursor.ip());
        throw FatalError.unexpected("invalid ref");
    }

    private boolean inThisStack(Pointer pointer) {
        return pointer.greaterEqual(lowestStackSlot);
    }

    /**
     * This method can be called to walk the stack of the current thread from the current
     * instruction pointer, stack pointer, and frame pointer, verifying the reference map
     * for each stack frame by using the {@link Heap#isValidRef(com.sun.max.vm.ref.Reference)}
     * heuristic.
     */
    public static void verifyReferenceMapsForThisThread() {
        if (VerifyRefMaps) {
            VmThread current = VmThread.current();
            current.stackReferenceMapVerifier().verifyReferenceMaps(current);
        }
    }

    private void verifyReferenceMaps(VmThread current) {
        Pointer tla = current.tla();
        initRefMapFields(tla);
        Pointer sp = getCpuStackPointer();
        Pointer fp = getCpuFramePointer();
        boolean lockDisabledSafepoints = traceStackRootScanStart(sp, HIGHEST_STACK_SLOT_ADDRESS.load(tla), VmThread.current());
        current.stackDumpStackFrameWalker().verifyReferenceMap(Pointer.fromLong(here()), sp, fp, this);
        traceStackRootScanEnd(lockDisabledSafepoints);
    }
}
