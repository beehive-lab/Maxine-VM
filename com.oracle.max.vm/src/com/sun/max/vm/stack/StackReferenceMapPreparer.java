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
package com.sun.max.vm.stack;

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.intrinsics.Infopoints.*;
import static com.sun.max.vm.runtime.VMRegister.*;
import static com.sun.max.vm.thread.VmThreadLocal.*;
import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;
import java.util.concurrent.atomic.*;

import com.oracle.max.cri.intrinsics.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.timer.*;
import com.sun.max.vm.*;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.*;
import com.sun.max.vm.log.VMLogger.Interval;
import com.sun.max.vm.log.hosted.*;
import com.sun.max.vm.bytecode.refmaps.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;
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
 * which fills in stack reference maps for target methods compiled by the baseline compiler as needed
 * was carefully crafted to comply with this requirement.
 */
public final class StackReferenceMapPreparer extends FrameReferenceMapVisitor {

    /**
     * Disables stack root scanning if greater than 0.
     */
    private static int LogSRSSuppressionCount;

    /**
     * A counter used purely for {@link #LogSRSSuppressionCount}.
     */
    private static AtomicInteger SRSCount = new AtomicInteger();

    static {
        VMOptions.addFieldOption("-XX:", "LogSRSSuppressionCount", "Disable logging of the first n stack root scans.");
    }

    /**
     * Determines if stack root scanning should be logged.
     */
    public static boolean logStackRootScanning() {
        return Heap.logRootScanning() || (stackRootScanLogger.enabled() && LogSRSSuppressionCount <= 0);
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
        if (logStackRootScanning()) {
            StackReferenceMapPreparer.stackRootScanLogger.logClearedRefMapIndexes(lowestBitIndex, highestBitIndex);
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
            scanReferenceMapByte(lowestRefMapByteIndex, lowestStackSlot, referenceMap, lowestBitIndex % Bytes.WIDTH, (highestBitIndex % Bytes.WIDTH) + 1, tla, wordPointerIndexVisitor);
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
                    if (logStackRootScanning()) {
                        stackRootScanLogger.logStackSlot(stackWordIndex, tla, Pointer.zero(), true);
                    }
                    if (!Reference.fromOrigin(slotAddress(stackWordIndex, tla).readWord(0).asPointer()).isTagged()) {
                        // only visit non-tagged references
                        wordPointerIndexVisitor.visit(lowestStackSlot, stackWordIndex);
                    }
                }
            }
        }
    }

    @INLINE
    private static int referenceMapByteIndex(final Pointer lowestStackSlot, Pointer slot) {
        return UnsignedMath.divide(referenceMapBitIndex(lowestStackSlot, slot), Bytes.WIDTH);
    }

    @INLINE
    private static int referenceMapBitIndex(final Pointer lowestStackSlot, Pointer slot) {
        return UnsignedMath.divide(slot.minus(lowestStackSlot).toInt(), Word.size());
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
     * Gets the time taken for the last call to {@link #prepareStackReferenceMap(Pointer)}.
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
    public long prepareStackReferenceMap(Pointer tla, CodePointer instructionPointer, Pointer stackPointer, Pointer framePointer, boolean ignoreTopFrame) {
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

        boolean lockDisabledSafepoints = logStackRootScanStart(stackPointer, highestStackSlot, vmThread);

        // walk the stack and prepare references for each stack frame
        StackFrameWalker sfw = vmThread.referenceMapPreparingStackFrameWalker();
        sfw.prepareReferenceMap(instructionPointer.toPointer(), stackPointer, framePointer, this);

        logStackRootScanEnd(lockDisabledSafepoints);

        timer.stop();
        preparationTime = timer.getLastElapsedTime();
        return preparationTime;
    }

    private void logStackRootScanEnd(boolean lockDisabledSafepoints) {
        if (logStackRootScanning()) {
            stackRootScanLogger.unlock(lockDisabledSafepoints);
        }
    }

    private boolean logStackRootScanStart(Pointer stackPointer, Pointer highestStackSlot, VmThread vmThread) {
        // Ideally this test and decrement should be atomic but it's ok
        // for LogSRSSuppressionCount to be approximate
        if (LogSRSSuppressionCount > 0) {
            LogSRSSuppressionCount--;
        }
        SRSCount.incrementAndGet();
        if (logStackRootScanning()) {
            boolean lockDisabledSafepoints = stackRootScanLogger.lock();
            stackRootScanLogger.logStart(
                            SRSCount.get(),
                            prepare,
                            stackPointer,
                            highestStackSlot,
                            lowestStackSlot,
                            vmThread,
                            referenceMapBitIndex(highestStackSlot),
                            referenceMapBitIndex(stackPointer),
                            referenceMapBitIndex(lowestStackSlot));
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
        CodePointer instructionPointer = CodePointer.from(JavaFrameAnchor.PC.get(anchor));
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
        if (logStackRootScanning()) {
            stackRootScanLogger.logComplete(
                            vmThread,
                            highestSlot,
                            referenceMapBitIndex(stackPointer),
                            stackPointer,
                            referenceMapBitIndex(stackPointer),
                            lowestStackSlot,
                            referenceMapBitIndex(lowestStackSlot));
        }

        // walk the stack and prepare references for each stack frame
        StackFrameWalker sfw = vmThread.referenceMapPreparingStackFrameWalker();
        completingReferenceMapLimit = highestSlot;
        sfw.prepareReferenceMap(instructionPointer.toPointer(), stackPointer, framePointer, this);
        completingReferenceMapLimit = Pointer.zero();

        logStackRootScanEnd(lockDisabledSafepoints);
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
            if (logStackRootScanning()) {
                StackReferenceMapPreparer.stackRootScanLogger.logEmptyMap(VmThread.fromTLA(tla));
            }
            return;
        }
        CodePointer instructionPointer = CodePointer.from(JavaFrameAnchor.PC.get(anchor));
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
     * @param trapFrame the trap state
     */
    public void prepareStackReferenceMapFromTrap(Pointer tla, Pointer trapFrame) {
        final TrapFrameAccess tfa = vm().trapFrameAccess;
        final Pointer instructionPointer = tfa.getPC(trapFrame);
        final Pointer stackPointer = tfa.getSP(trapFrame);
        final Pointer framePointer = tfa.getFP(trapFrame);
        prepareStackReferenceMap(tla, CodePointer.from(instructionPointer), stackPointer, framePointer, false);
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

    @Override
    public void logPrepareReferenceMap(TargetMethod targetMethod, int safepointIndex, Pointer refmapFramePointer, String label) {
        if (logStackRootScanning()) {
            stackRootScanLogger.logPrepare(prepare, targetMethod, safepointIndex, refmapFramePointer, label,
                            referenceMapBitIndex(refmapFramePointer), ttla);
        }
    }

    /**
     * If {@linkplain Heap#logRootScanning() GC tracing} is enabled, then this method logs one byte's worth
     * of a frame/register reference map.
     *
     * @param byteIndex the index of the reference map byte
     * @param referenceMapByte the value of the reference map byte
     * @param referenceMapLabel a label indicating whether this reference map is for a frame or for the registers
     */
    @Override
    public void logReferenceMapByteBefore(int byteIndex, byte referenceMapByte, String referenceMapLabel) {
        if (logStackRootScanning()) {
            stackRootScanLogger.logMapByteBefore(byteIndex, referenceMapByte, referenceMapLabel);
        }
    }

    /**
     * If {@linkplain Heap#logRootScanning() GC tracing} is enabled, then this method logs the stack slots corresponding to a
     * frame or set of set of saved registers that are determined to contain references by a reference map.
     *
     * @param framePointer the frame pointer. This value should be {@link Pointer#zero()} if the reference map is for a
     *            set of saved registers.
     * @param baseSlotIndex the index of the slot corresponding to bit 0 of {@code referenceMapByte}
     * @param referenceMapByte a the reference map byte
     */
    @Override
    public void logReferenceMapByteAfter(Pointer framePointer, int baseSlotIndex, final byte referenceMapByte) {
        if (logStackRootScanning()) {
            for (int bitIndex = 0; bitIndex < Bytes.WIDTH; bitIndex++) {
                if (((referenceMapByte >>> bitIndex) & 1) != 0) {
                    final int slotIndex = baseSlotIndex + bitIndex;
                    stackRootScanLogger.logStackSlot(slotIndex, ttla, framePointer, false);
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

    @Override
    public void setBits(int baseSlotIndex, byte referenceMapByte) {
        referenceMap.setBits(baseSlotIndex, referenceMapByte);
    }

    @Override
    public int referenceMapBitIndex(Address slotAddress) {
        return referenceMapBitIndex(lowestStackSlot, slotAddress.asPointer());
    }

    /**
     * Updates the reference map bits for a range of slots within a frame.
     *
     * @param cursor the cursor corresponding to the frame for which the bits are being filled in
     * @param slotPointer the pointer to the slot that corresponds to bit 0 in the reference map
     * @param refMap an integer containing up to 32 reference map bits for up to 32 successive slots in the frame
     * @param numBits the number of bits in the reference map
     */
    @Override
    public void visitReferenceMapBits(StackFrameCursor cursor, Pointer slotPointer, int refMap, int numBits) {
        if (logStackRootScanning()) {
            stackRootScanLogger.logSetReferenceMapBits(cursor.sp(), cursor.fp(), slotPointer, refMap, numBits, cursor.targetMethod().regionName());
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
                        if (logStackRootScanning()) {
                            stackRootScanLogger.logPrintRef(
                                            slotPointer.plusWords(i),
                                            slotPointer.plusWords(i).minus(cursor.sp()).toInt(),
                                            ref.toOrigin(),
                                            ref.isTagged());
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
                int byteIndex = UnsignedMath.divide(slotIndex, Bytes.WIDTH);

                byte prev = referenceMap.getByte(byteIndex);
                prev |= mapBits << rest;
                referenceMap.setByte(byteIndex, prev);

                slotIndex += bits;
                mapBits >>>= bits;
            }
        }

    }

    @NEVER_INLINE
    private void invalidRef(Reference ref, StackFrameCursor cursor, Pointer slotPointer, int slotIndex) {
        StackRootScanLogger.printRef(slotPointer.plusWords(slotIndex), slotPointer.plusWords(slotIndex).minus(cursor.sp()).toInt(), ref.toOrigin(), ref.isTagged(), false);
        Log.print("invalid ref ### [SRSCount: ");
        Log.print(SRSCount.get());
        Log.print("] ");
        final Pointer ip = cursor.ipAsPointer();
        Throw.logFrame(null, cursor.targetMethod(), ip);
        throw FatalError.unexpected("invalid ref");
    }

    private boolean inThisStack(Pointer pointer) {
        return pointer.greaterEqual(lowestStackSlot);
    }

    /**
     * Walks the stack of the current thread from the current
     * instruction pointer, stack pointer, and frame pointer, verifying the reference map
     * for each stack frame by using the {@link Heap#isValidRef(Reference)}
     * heuristic.
     */
    public static void verifyReferenceMapsForThisThread() {
        VmThread current = VmThread.current();
        current.stackReferenceMapVerifier().verifyReferenceMaps0(current, CodePointer.from(here()), getCpuStackPointer(), getCpuFramePointer());
    }

    /**
     * Walks the stack of a given thread from a specified frame, verifying the reference map
     * for each stack frame by using the {@link Heap#isValidRef(Reference)}
     * heuristic.
     */
    public static void verifyReferenceMaps(VmThread thread, CodePointer ip, Pointer sp, Pointer fp) {
        thread.stackReferenceMapVerifier().verifyReferenceMaps0(thread, ip, sp, fp);
    }

    private void verifyReferenceMaps0(VmThread thread, CodePointer ip, Pointer sp, Pointer fp) {
        Pointer tla = thread.tla();
        initRefMapFields(tla);
        boolean lockDisabledSafepoints = logStackRootScanStart(sp, HIGHEST_STACK_SLOT_ADDRESS.load(tla), thread);
        thread.stackDumpStackFrameWalker().verifyReferenceMap(ip.toPointer(), sp, fp, this);
        logStackRootScanEnd(lockDisabledSafepoints);
    }

    /*
     * Logging.
     */

    /**
     * Stack root scan logger instance.
     */
    public static final StackRootScanLogger stackRootScanLogger = new StackRootScanLogger();

    /**
     * Stack root scan logger.
     * Encapsulates all the loggable operations related to stack root scanning. (and there are a lot).
     * This is by far the most complex logger in the system and was the most difficult to convert
     * from the previous trace-only mode. It was necessary to store several {@link Reference} valued
     * objects in the log in order to accurately preserve the tracing.
     */
    @HOSTED_ONLY
    @VMLoggerInterface
    private interface StackRootScanLoggerInterface {
        // pack 8/9th args to stay in 8 arg limit
        void start(
                        @VMLogParam(name = "count") int count,
                        @VMLogParam(name = "prepare") boolean prepare,
                        @VMLogParam(name = "stackPointer") Pointer stackPointer,
                        @VMLogParam(name = "highestStackSlot") Pointer highestStackSlot,
                        @VMLogParam(name = "lowestStackSlot") Pointer lowestStackSlot,
                        @VMLogParam(name = "vmThread") VmThread vmThread,
                        @VMLogParam(name = "highestStackSlotReferenceMapBitIndex") int highestStackSlotReferenceMapBitIndex,
//                        @VMLogParam(name = "stackPointerReferenceMapBitIndex") int stackPointerReferenceMapBitIndex,
//                        @VMLogParam(name = "lowestStackSlotReferenceMapBitIndex") int lowestStackSlotReferenceMapBitIndex);
                        @VMLogParam(name = "stackAndLowestStackSlotReferenceMapBitIndex") long stackAndLowestStackSlotReferenceMapBitIndex);
        void startThreadLocals();
        void scanThread(
                        @VMLogParam(name = "vmThread") VmThread vmThread);
        void referenceThreadLocal(
                        @VMLogParam(name = "index") int index,
                        @VMLogParam(name = "address") Pointer address,
                        @VMLogParam(name = "value") Word value,
                        @VMLogParam(name = "name") String name,
                        @VMLogParam(name = "categorySuffix") String categorySuffix);
        void threadSlotRange(
                        @VMLogParam(name = "highestSlot") Pointer highestSlot,
                        @VMLogParam(name = "lowestActiveSlot") Pointer lowestActiveSlot,
                        @VMLogParam(name = "lowestSlot") Pointer lowestSlot);
        void prepare(
                        @VMLogParam(name = "prepare") boolean prepare,
                        @VMLogParam(name = "targetMethod") TargetMethod targetMethod,
                        @VMLogParam(name = "safepointIndex") int safepointIndex,
                        @VMLogParam(name = "refmapFramePointer") Pointer refmapFramePointer,
                        @VMLogParam(name = "label") String label,
                        @VMLogParam(name = "refmapFramePointerBitIndex") int refmapFramePointerBitIndex,
                        @VMLogParam(name = "ttla") Pointer ttla);
        void printRef(
                        @VMLogParam(name = "refPointer") Pointer refPointer,
                        @VMLogParam(name = "spOffset") int spOffset,
                        @VMLogParam(name = "refOrigin") Pointer refOrigin,
                        @VMLogParam(name = "isTagged") boolean isTagged);
        void registerState(
                        @VMLogParam(name = "reg") CiRegister reg);
        void parameter(
                        @VMLogParam(name = "index") int index,
                        @VMLogParam(name = "parameter") TypeDescriptor parameter);
        void receiver(
                        @VMLogParam(name = "receiver") TypeDescriptor receiver);
        void stackSlot(
                        @VMLogParam(name = "slotIndex") int slotIndex,
                        @VMLogParam(name = "tla") Pointer tla,
                        @VMLogParam(name = "framePointer") Pointer framePointer,
                        @VMLogParam(name = "checkTagging") boolean checkTagging);
        void clearedRefMapIndexes(
                        @VMLogParam(name = "lowestBitIndex") int lowestBitIndex,
                        @VMLogParam(name = "highestBitIndex") int highestBitIndex);
        void complete(
                        @VMLogParam(name = "vmThread") VmThread vmThread,
                        @VMLogParam(name = "highestSlot") Pointer highestSlot,
                        @VMLogParam(name = "highestSlotBitIndex") int highestSlotBitIndex,
                        @VMLogParam(name = "stackPointer") Pointer stackPointer,
                        @VMLogParam(name = "stackPointerBitIndex") int stackPointerSlotBitIndex,
                        @VMLogParam(name = "lowestSlot") Pointer lowestSlot,
                        @VMLogParam(name = "lowestSlotBitIndex") int lowestSlotBitIndex);
        void emptyMap(
                        @VMLogParam(name = "vmThread") VmThread vmThread);
        void finalizeMaps(
                        @VMLogParam(name = "interval") Interval interval,
                        @VMLogParam(name = "helper") ReferenceMapEditorLogHelper helper);
        void safepoint(
                        @VMLogParam(name = "helper") ReferenceMapEditorLogHelper helper,
                        @VMLogParam(name = "interpreter") ReferenceMapInterpreter interpreter,
                        @VMLogParam(name = "bci") int bci,
                        @VMLogParam(name = "safePointIndex") int safePointIndex);
        void mapByteBefore(
                        @VMLogParam(name = "byteIndex") int byteIndex,
                        @VMLogParam(name = "referenceMapByte") byte referenceMapByte,
                        @VMLogParam(name = "referenceMapLabel") String referenceMapLabel);
        void setReferenceMapBits(
                        @VMLogParam(name = "sp") Pointer sp,
                        @VMLogParam(name = "fp") Pointer fp,
                        @VMLogParam(name = "slotPointer") Pointer slotPointer,
                        @VMLogParam(name = "refMapBits") int refMapBits,
                        @VMLogParam(name = "numBits") int numBits,
                        @VMLogParam(name = "description") String description);
    }

    /*
     * Methods to access typed objects stored in untyped form in the log (at present).
     */


    @INTRINSIC(UNSAFE_CAST)
    private static native TypeDescriptor asTypeDescriptor(Object arg);

    private static TypeDescriptor toTypeDescriptor(Record r, int argNum) {
        return asTypeDescriptor(VMLogger.toObject(r, argNum));
    }

    // This could be stored with as its "number" save that it is currently hard to convert back.
    @INTRINSIC(UNSAFE_CAST)
    private static native CiRegister asCiRegister(Object arg);

    private static CiRegister toCiRegister(Record r, int argNum) {
        return asCiRegister(VMLogger.toObject(r, argNum));
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native ReferenceMapEditorLogHelper asReferenceMapEditorLogHelper(Object arg);

    private static ReferenceMapEditorLogHelper toReferenceMapEditorLogHelper(Record r, int argNum) {
        return asReferenceMapEditorLogHelper(VMLogger.toObject(r, argNum));
    }

    @INTRINSIC(UNSAFE_CAST)
    private static native ReferenceMapInterpreter asReferenceMapInterpreter(Object arg);

    private static ReferenceMapInterpreter toReferenceMapInterpreter(Record r, int argNum) {
        return asReferenceMapInterpreter(VMLogger.toObject(r, argNum));
    }

    public static final class StackRootScanLogger extends StackRootScanLoggerAuto {
        StackRootScanLogger() {
            super("SRS", "stack root scanning.");
        }

        @INLINE
        void logStart(int count, boolean prepare, Pointer stackPointer, Pointer highestStackSlot, Pointer lowestStackSlot, VmThread vmThread,
                        int highestStackSlotReferenceMapBitIndex, int stackPointerReferenceMapBitIndex, int lowestStackSlotReferenceMapBitIndex) {
            // would exceed 8 arg limit without packing.
            log(Operation.Start.ordinal(), intArg(count), booleanArg(prepare), stackPointer, highestStackSlot, lowestStackSlot,
                            vmThreadArg(vmThread),
                            intArg(highestStackSlotReferenceMapBitIndex),
                            twoIntArgs(stackPointerReferenceMapBitIndex, lowestStackSlotReferenceMapBitIndex));
        }

        @Override
        public void checkOptions() {
            super.checkOptions();
            checkDominantLoggerOptions(Heap.rootScanLogger);
        }

        @Override
        protected void traceStart(int count, boolean prepare, Pointer stackPointer, Pointer highestStackSlot,
                        Pointer lowestStackSlot, VmThread vmThread, int highestStackSlotReferenceMapBitIndex,
                        long stackAndLowestStackSlotReferenceMapBitIndex) {
            int stackPointerReferenceMapBitIndex = toIntArg1(stackAndLowestStackSlotReferenceMapBitIndex);
            int lowestStackSlotReferenceMapBitIndex = toIntArg2(stackAndLowestStackSlotReferenceMapBitIndex);
            Log.print('[');
            Log.print(count);
            Log.print(prepare ? "] Preparing" : "] Verifying");
            Log.print(" stack reference map for thread ");
            Log.printThread(vmThread, false);
            Log.println(":");
            Log.print("  Highest slot: ");
            Log.print(highestStackSlot);
            Log.print(" [index=");
            Log.print(highestStackSlotReferenceMapBitIndex);
            Log.println("]");
            Log.print("  Lowest active slot: ");
            Log.print(stackPointer);
            Log.print(" [index=");
            Log.print(stackPointerReferenceMapBitIndex);
            Log.println("]");
            Log.print("  Lowest slot: ");
            Log.print(lowestStackSlot);
            Log.print(" [index=");
            Log.print(lowestStackSlotReferenceMapBitIndex);
            Log.println("]");
            Log.print("  Current thread is ");
            Log.printCurrentThread(true);
        }

        @Override
        protected void traceEmptyMap(VmThread vmThread) {
            Log.print("Empty stack reference map for thread ");
            Log.printThread(vmThread, true);
        }

        @Override
        protected void traceComplete(VmThread vmThread, Pointer highestSlot, int highestSlotBitIndex, Pointer stackPointer,
                        int stackPointerBitIndex, Pointer lowestSlot, int lowestSlotBitIndex) {
            Log.print("Completing preparation of stack reference map for thread ");
            Log.printThread(vmThread, false);
            Log.println(":");
            Log.print("  Highest slot: ");
            Log.print(highestSlot);
            Log.print(" [index=");
            Log.print(highestSlotBitIndex);
            Log.println("]");
            Log.print("  Lowest active slot: ");
            Log.print(stackPointer);
            Log.print(" [index=");
            Log.print(stackPointerBitIndex);
            Log.println("]");
            Log.print("  Lowest slot: ");
            Log.print(lowestSlot);
            Log.print(" [index=");
            Log.print(lowestSlotBitIndex);
            Log.println("]");
            Log.print("  Current thread is ");
            Log.printCurrentThread(true);
        }

        @Override
        protected void traceParameter(int index, TypeDescriptor parameter) {
            Log.print("    parameter ");
            Log.print(index);
            Log.print(", type: ");
            Log.println(parameter.string);
        }

        @Override
        protected void traceSafepoint(ReferenceMapEditorLogHelper helper, ReferenceMapInterpreter interpreter, int bci, int safePointIndex) {
            // Tracing this is complex and compiler specific, so we call back
            helper.traceSafepoint(interpreter, bci, safePointIndex);
        }

        @Override
        protected void tracePrintRef(Pointer refPointer, int spOffset, Pointer refOrigin, boolean isTagged) {
            printRef(refPointer, spOffset, refOrigin, isTagged, true);
        }

        @Override
        protected void tracePrepare(boolean prepare, TargetMethod targetMethod, int safepointIndex, Pointer refmapFramePointer,
                        String label, int refmapFramePointerBitIndex, Pointer ttla) {
            Log.print(prepare ? "  Preparing" : "  Verifying");
            Log.print(" reference map for ");
            Log.print(label);
            Log.print(" of ");
            Log.printMethod(targetMethod, false);
            Log.print(" +");
            Log.println(targetMethod);
            Log.print("    Stop index: ");
            Log.println(safepointIndex);
            if (!refmapFramePointer.isZero()) {
                Log.print("    Frame pointer: ");
                printSlot(refmapFramePointerBitIndex, ttla, Pointer.zero(), false);
                Log.println();
            }
        }

        @Override
        protected void traceStartThreadLocals() {
            Log.println("  Thread locals:");
        }

        @Override
        protected void traceScanThread(VmThread vmThread) {
            Log.print("Scanning thread locals and stack for thread ");
            Log.printThread(vmThread, false);
            Log.print(":");
        }

        @Override
        protected void traceReferenceThreadLocal(int index, Pointer address, Word value, String name, String categorySuffix) {
            Log.print("    index=");
            Log.print(index);
            Log.print(", address=");
            Log.print(address);
            Log.print(", value=");
            Log.print(value);
            Log.print(", name=");
            Log.print(name);
            Log.println(categorySuffix);
        }

        @Override
        protected void traceThreadSlotRange(Pointer highestSlot, Pointer lowestActiveSlot, Pointer lowestSlot) {
            if (highestSlot.isZero()) {
                Log.print("No Java stack frames");
            } else {
                Log.print("  Highest slot: ");
                Log.println(highestSlot);
                Log.print("  Lowest active slot: ");
                Log.println(lowestActiveSlot);
                Log.print("  Lowest slot: ");
                Log.println(lowestSlot);
            }
        }

        @Override
        protected void traceRegisterState(CiRegister reg) {
            Log.print("    register: ");
            Log.println(reg.name);
        }

        @Override
        protected void traceReceiver(TypeDescriptor receiver) {
            Log.print("    receiver, type: ");
            Log.println(receiver.string);
        }

        @Override
        protected void traceStackSlot(int slotIndex, Pointer tla, Pointer framePointer, boolean checkTagging) {
            Log.print("    Slot: ");
            printSlot(slotIndex, tla, framePointer, checkTagging);
            Log.println();
        }

        @Override
        protected void traceClearedRefMapIndexes(int lowestBitIndex, int highestBitIndex) {
            Log.print("Cleared refmap indexes [");
            Log.print(lowestBitIndex);
            Log.print(" .. ");
            Log.print(highestBitIndex);
            Log.println("]");
        }

        @Override
        protected void traceFinalizeMaps(Interval interval, ReferenceMapEditorLogHelper helper) {
            Log.printCurrentThread(false);
            Log.print(": ");
            Log.print(interval == Interval.BEGIN ? "Finalizing " : "Finalized ");
            Log.print(helper.compilerName()); Log.print(" reference maps for ");
            Log.printMethod(helper.targetMethod().classMethodActor, true);
        }

        @Override
        protected void traceMapByteBefore(int byteIndex, byte referenceMapByte, String referenceMapLabel) {
            Log.print("    ");
            Log.print(referenceMapLabel);
            Log.print(" map byte index: ");
            Log.println(byteIndex);
            Log.print("    ");
            Log.print(referenceMapLabel);
            Log.print(" map byte:       ");
            Log.println(referenceMapByte);
        }

        @Override
        protected void traceSetReferenceMapBits(Pointer sp, Pointer fp, Pointer slotPointer, int refMapBits, int numBits, String description) {
            Log.print("    setReferenceMapBits: sp = ");
            Log.print(sp);
            Log.print(" fp = ");
            Log.print(fp);
            Log.print(", slots @ ");
            Log.print(slotPointer);
            Log.print(", bits = ");
            for (int i = 0; i < numBits; i++) {
                Log.print((refMapBits >>> i) & 1);
            }
            Log.print(", description = ");
            Log.println(description);
        }

        private static void printRef(Pointer refStackPointer, int spOffset, Pointer refOrigin, boolean tagged, boolean valid) {
            Log.print("    ref @ ");
            Log.print(refStackPointer);
            Log.print(" [sp + ");
            Log.print(spOffset);
            Log.print("] = ");
            Log.print(refOrigin);
            if (tagged) {
                Log.print(" tagged");
            }
            Log.print(valid ? " ok\n" : " (invalid)\n");
        }


        private static void printSlot(int slotIndex, Pointer tla, Pointer framePointer, boolean checkTagging) {
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
            final Word value = slotAddress.readWord(0);
            Log.print(value);
            if (checkTagging && Reference.fromOrigin(value.asPointer()).isTagged()) {
                Log.print(" (tagged)");
            }
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

    }

// START GENERATED CODE
    private static abstract class StackRootScanLoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            ClearedRefMapIndexes, Complete, EmptyMap,
            FinalizeMaps, MapByteBefore, Parameter, Prepare,
            PrintRef, Receiver, ReferenceThreadLocal, RegisterState,
            Safepoint, ScanThread, SetReferenceMapBits, StackSlot,
            Start, StartThreadLocals, ThreadSlotRange;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = new int[] {0x0, 0x0, 0x0, 0x2, 0x4, 0x2, 0x12, 0x0, 0x1, 0x18, 0x1, 0x3, 0x0, 0x20,
            0x0, 0x0, 0x0, 0x0};

        protected StackRootScanLoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription, REFMAPS);
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logClearedRefMapIndexes(int lowestBitIndex, int highestBitIndex) {
            log(Operation.ClearedRefMapIndexes.ordinal(), intArg(lowestBitIndex), intArg(highestBitIndex));
        }
        protected abstract void traceClearedRefMapIndexes(int lowestBitIndex, int highestBitIndex);

        @INLINE
        public final void logComplete(VmThread vmThread, Pointer highestSlot, int highestSlotBitIndex, Pointer stackPointer, int stackPointerBitIndex,
                Pointer lowestSlot, int lowestSlotBitIndex) {
            log(Operation.Complete.ordinal(), vmThreadArg(vmThread), highestSlot, intArg(highestSlotBitIndex), stackPointer, intArg(stackPointerBitIndex),
                lowestSlot, intArg(lowestSlotBitIndex));
        }
        protected abstract void traceComplete(VmThread vmThread, Pointer highestSlot, int highestSlotBitIndex, Pointer stackPointer, int stackPointerBitIndex,
                Pointer lowestSlot, int lowestSlotBitIndex);

        @INLINE
        public final void logEmptyMap(VmThread vmThread) {
            log(Operation.EmptyMap.ordinal(), vmThreadArg(vmThread));
        }
        protected abstract void traceEmptyMap(VmThread vmThread);

        @INLINE
        public final void logFinalizeMaps(Interval interval, ReferenceMapEditorLogHelper helper) {
            log(Operation.FinalizeMaps.ordinal(), intervalArg(interval), objectArg(helper));
        }
        protected abstract void traceFinalizeMaps(Interval interval, ReferenceMapEditorLogHelper helper);

        @INLINE
        public final void logMapByteBefore(int byteIndex, byte referenceMapByte, String referenceMapLabel) {
            log(Operation.MapByteBefore.ordinal(), intArg(byteIndex), objectArg(referenceMapByte), objectArg(referenceMapLabel));
        }
        protected abstract void traceMapByteBefore(int byteIndex, byte referenceMapByte, String referenceMapLabel);

        @INLINE
        public final void logParameter(int index, TypeDescriptor parameter) {
            log(Operation.Parameter.ordinal(), intArg(index), objectArg(parameter));
        }
        protected abstract void traceParameter(int index, TypeDescriptor parameter);

        @INLINE
        public final void logPrepare(boolean prepare, TargetMethod targetMethod, int safepointIndex, Pointer refmapFramePointer, String label,
                int refmapFramePointerBitIndex, Pointer ttla) {
            log(Operation.Prepare.ordinal(), booleanArg(prepare), objectArg(targetMethod), intArg(safepointIndex), refmapFramePointer, objectArg(label),
                intArg(refmapFramePointerBitIndex), ttla);
        }
        protected abstract void tracePrepare(boolean prepare, TargetMethod targetMethod, int safepointIndex, Pointer refmapFramePointer, String label,
                int refmapFramePointerBitIndex, Pointer ttla);

        @INLINE
        public final void logPrintRef(Pointer refPointer, int spOffset, Pointer refOrigin, boolean isTagged) {
            log(Operation.PrintRef.ordinal(), refPointer, intArg(spOffset), refOrigin, booleanArg(isTagged));
        }
        protected abstract void tracePrintRef(Pointer refPointer, int spOffset, Pointer refOrigin, boolean isTagged);

        @INLINE
        public final void logReceiver(TypeDescriptor receiver) {
            log(Operation.Receiver.ordinal(), objectArg(receiver));
        }
        protected abstract void traceReceiver(TypeDescriptor receiver);

        @INLINE
        public final void logReferenceThreadLocal(int index, Pointer address, Word value, String name, String categorySuffix) {
            log(Operation.ReferenceThreadLocal.ordinal(), intArg(index), address, value, objectArg(name), objectArg(categorySuffix));
        }
        protected abstract void traceReferenceThreadLocal(int index, Pointer address, Word value, String name, String categorySuffix);

        @INLINE
        public final void logRegisterState(CiRegister reg) {
            log(Operation.RegisterState.ordinal(), objectArg(reg));
        }
        protected abstract void traceRegisterState(CiRegister reg);

        @INLINE
        public final void logSafepoint(ReferenceMapEditorLogHelper helper, ReferenceMapInterpreter interpreter, int bci, int safePointIndex) {
            log(Operation.Safepoint.ordinal(), objectArg(helper), objectArg(interpreter), intArg(bci), intArg(safePointIndex));
        }
        protected abstract void traceSafepoint(ReferenceMapEditorLogHelper helper, ReferenceMapInterpreter interpreter, int bci, int safePointIndex);

        @INLINE
        public final void logScanThread(VmThread vmThread) {
            log(Operation.ScanThread.ordinal(), vmThreadArg(vmThread));
        }
        protected abstract void traceScanThread(VmThread vmThread);

        @INLINE
        public final void logSetReferenceMapBits(Pointer sp, Pointer fp, Pointer slotPointer, int refMapBits, int numBits,
                String description) {
            log(Operation.SetReferenceMapBits.ordinal(), sp, fp, slotPointer, intArg(refMapBits), intArg(numBits),
                objectArg(description));
        }
        protected abstract void traceSetReferenceMapBits(Pointer sp, Pointer fp, Pointer slotPointer, int refMapBits, int numBits,
                String description);

        @INLINE
        public final void logStackSlot(int slotIndex, Pointer tla, Pointer framePointer, boolean checkTagging) {
            log(Operation.StackSlot.ordinal(), intArg(slotIndex), tla, framePointer, booleanArg(checkTagging));
        }
        protected abstract void traceStackSlot(int slotIndex, Pointer tla, Pointer framePointer, boolean checkTagging);

        @INLINE
        public final void logStart(int count, boolean prepare, Pointer stackPointer, Pointer highestStackSlot, Pointer lowestStackSlot,
                VmThread vmThread, int highestStackSlotReferenceMapBitIndex, long stackAndLowestStackSlotReferenceMapBitIndex) {
            log(Operation.Start.ordinal(), intArg(count), booleanArg(prepare), stackPointer, highestStackSlot, lowestStackSlot,
                vmThreadArg(vmThread), intArg(highestStackSlotReferenceMapBitIndex), longArg(stackAndLowestStackSlotReferenceMapBitIndex));
        }
        protected abstract void traceStart(int count, boolean prepare, Pointer stackPointer, Pointer highestStackSlot, Pointer lowestStackSlot,
                VmThread vmThread, int highestStackSlotReferenceMapBitIndex, long stackAndLowestStackSlotReferenceMapBitIndex);

        @INLINE
        public final void logStartThreadLocals() {
            log(Operation.StartThreadLocals.ordinal());
        }
        protected abstract void traceStartThreadLocals();

        @INLINE
        public final void logThreadSlotRange(Pointer highestSlot, Pointer lowestActiveSlot, Pointer lowestSlot) {
            log(Operation.ThreadSlotRange.ordinal(), highestSlot, lowestActiveSlot, lowestSlot);
        }
        protected abstract void traceThreadSlotRange(Pointer highestSlot, Pointer lowestActiveSlot, Pointer lowestSlot);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //ClearedRefMapIndexes
                    traceClearedRefMapIndexes(toInt(r, 1), toInt(r, 2));
                    break;
                }
                case 1: { //Complete
                    traceComplete(toVmThread(r, 1), toPointer(r, 2), toInt(r, 3), toPointer(r, 4), toInt(r, 5), toPointer(r, 6), toInt(r, 7));
                    break;
                }
                case 2: { //EmptyMap
                    traceEmptyMap(toVmThread(r, 1));
                    break;
                }
                case 3: { //FinalizeMaps
                    traceFinalizeMaps(toInterval(r, 1), toReferenceMapEditorLogHelper(r, 2));
                    break;
                }
                case 4: { //MapByteBefore
                    traceMapByteBefore(toInt(r, 1), toByte(r, 2), toString(r, 3));
                    break;
                }
                case 5: { //Parameter
                    traceParameter(toInt(r, 1), toTypeDescriptor(r, 2));
                    break;
                }
                case 6: { //Prepare
                    tracePrepare(toBoolean(r, 1), toTargetMethod(r, 2), toInt(r, 3), toPointer(r, 4), toString(r, 5), toInt(r, 6), toPointer(r, 7));
                    break;
                }
                case 7: { //PrintRef
                    tracePrintRef(toPointer(r, 1), toInt(r, 2), toPointer(r, 3), toBoolean(r, 4));
                    break;
                }
                case 8: { //Receiver
                    traceReceiver(toTypeDescriptor(r, 1));
                    break;
                }
                case 9: { //ReferenceThreadLocal
                    traceReferenceThreadLocal(toInt(r, 1), toPointer(r, 2), toWord(r, 3), toString(r, 4), toString(r, 5));
                    break;
                }
                case 10: { //RegisterState
                    traceRegisterState(toCiRegister(r, 1));
                    break;
                }
                case 11: { //Safepoint
                    traceSafepoint(toReferenceMapEditorLogHelper(r, 1), toReferenceMapInterpreter(r, 2), toInt(r, 3), toInt(r, 4));
                    break;
                }
                case 12: { //ScanThread
                    traceScanThread(toVmThread(r, 1));
                    break;
                }
                case 13: { //SetReferenceMapBits
                    traceSetReferenceMapBits(toPointer(r, 1), toPointer(r, 2), toPointer(r, 3), toInt(r, 4), toInt(r, 5), toString(r, 6));
                    break;
                }
                case 14: { //StackSlot
                    traceStackSlot(toInt(r, 1), toPointer(r, 2), toPointer(r, 3), toBoolean(r, 4));
                    break;
                }
                case 15: { //Start
                    traceStart(toInt(r, 1), toBoolean(r, 2), toPointer(r, 3), toPointer(r, 4), toPointer(r, 5), toVmThread(r, 6), toInt(r, 7), toLong(r, 8));
                    break;
                }
                case 16: { //StartThreadLocals
                    traceStartThreadLocals();
                    break;
                }
                case 17: { //ThreadSlotRange
                    traceThreadSlotRange(toPointer(r, 1), toPointer(r, 2), toPointer(r, 3));
                    break;
                }
            }
        }
    }

// END GENERATED CODE

}
