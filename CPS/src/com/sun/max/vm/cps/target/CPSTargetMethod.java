/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.target;

import static com.sun.max.platform.Platform.*;

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.cri.ci.CiDebugInfo.Frame;
import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.cps.ir.observer.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.CompiledStackFrameLayout.Slots;

/**
 * Target method that saves for each catch block the ranges in the code that can
 * refer to them. Does not include the type of the caught exception.
 *
 * @author Doug Simon
 * @author Thomas Wuerthinger
 */
public abstract class CPSTargetMethod extends TargetMethod implements IrMethod {

    /**
     * @see #catchRangePositions()
     */
    @INSPECTED
    private int[] catchRangePositions;

    /**
     * @see #catchBlockPositions()
     */
    @INSPECTED
    private int[] catchBlockPositions;

    /**
     * @see #compressedJavaFrameDescriptors()
     */
    @INSPECTED
    protected byte[] compressedJavaFrameDescriptors;

    /**
     * If non-null, this array encodes a serialized array of {@link InlineDataDescriptor} objects.
     */
    @INSPECTED
    protected byte[] encodedInlineDataDescriptors;

    /**
     * @see #referenceMaps()
     */
    protected byte[] referenceMaps;

    @INSPECTED
    protected int frameReferenceMapSize;

    public CPSTargetMethod(ClassMethodActor classMethodActor, CallEntryPoint callEntryPoint) {
        super(classMethodActor, callEntryPoint);
    }

    public void cleanup() {
    }

    public boolean contains(Builtin builtin, boolean defaultResult) {
        return defaultResult;
    }

    public boolean isNative() {
        return classMethodActor().isNative();
    }

    public int count(Builtin builtin, int defaultResult) {
        return defaultResult;
    }

    public Class<? extends IrTraceObserver> irTraceObserverType() {
        return IrTraceObserver.class;
    }

    /**
     * Computes the number of bytes required for the reference maps of a target method.
     *
     * @param numberOfDirectCalls
     * @param numberOfIndirectCalls
     * @param numberOfSafepoints
     * @param frameReferenceMapSize
     * @param registerReferenceMapSize
     * @return
     */
    public static int computeReferenceMapsSize(int numberOfDirectCalls, int numberOfIndirectCalls, int numberOfSafepoints, int frameReferenceMapSize, int registerReferenceMapSize) {
        final int numberOfStopPositions = numberOfDirectCalls + numberOfIndirectCalls + numberOfSafepoints;
        if (numberOfStopPositions != 0) {
            // NOTE: number of safepoints is counted twice due to the need for a register map.
            return (numberOfStopPositions * frameReferenceMapSize) + (numberOfSafepoints * registerReferenceMapSize);
        }
        return 0;
    }

    /**
     * Gets the array of positions denoting which ranges of code are covered by an
     * exception dispatcher. The {@code n}th range includes positions
     * {@code [catchRangePositions()[n] .. catchRangePositions()[n + 1])} unless {@code n == catchRangePositions().length - 1}
     * in which case it includes positions {@code [catchRangePositions()[n] .. codeLength())}. Note that these range
     * specifications exclude the last position.
     * <p>
     * The address of the dispatcher for range {@code n} is {@code catchBlockPositions()[n]}. If
     * {@code catchBlockPositions()[n] == 0}, then {@code n} denotes a code range not covered by an exception dispatcher.
     * <p>
     * In the example below, any exception that occurs while the instruction pointer corresponds to position 3, 4, 5 or 6 will
     * be handled by the dispatch code at position 7. An exception that occurs at any other position will be propagated to
     * the caller.
     * <pre>{@code
     *
     * catch range positions: [0, 3, 7]
     * catch block positions: [0, 7, 0]
     *
     *             caller          +------+      caller
     *                ^            |      |        ^
     *                |            |      V        |
     *          +-----|-----+------|------+--------|---------+
     *     code | exception |  exception  |    exception     |
     *          +-----------+-------------+------------------+
     *           0        2  3           6 7               12
     * }</pre>
     *
     *
     * @return positions of exception dispatcher ranges in the machine code, matched with the
     *         {@linkplain #catchBlockPositions() catch block positions}
     */
    public final int[] catchRangePositions() {
        return catchRangePositions;
    }

    /**
     * @see #catchRangePositions()
     */
    public final int numberOfCatchRanges() {
        return (catchRangePositions == null) ? 0 : catchRangePositions.length;
    }

    /**
     * @see #catchRangePositions()
     */
    public final int[] catchBlockPositions() {
        return catchBlockPositions;
    }

    boolean fatalIfNotSorted(int[] catchRangePositions) {
        if (catchRangePositions != null) {
            int last = Integer.MIN_VALUE;
            for (int i = 0; i < catchRangePositions.length; ++i) {
                if (catchRangePositions[i] < last) {
                    FatalError.unexpected(classMethodActor().format("%H.%n(%p)") + ": Bad catchRangePositions: element " + i + " is less than element " + (i - 1) + ": " + catchRangePositions[i] + " < " + catchRangePositions[i - 1]);
                }
                last = catchRangePositions[i];
            }
        }
        return true;
    }

    public final void setGenerated(
                    int[] catchRangePositions,
                    int[] catchBlockPositions,
                    int[] stopPositions,
                    byte[] compressedJavaFrameDescriptors,
                    Object[] directCallees,
                    int numberOfIndirectCalls,
                    int numberOfSafepoints,
                    byte[] referenceMaps,
                    byte[] scalarLiterals,
                    Object[] referenceLiterals,
                    byte[] codeBuffer,
                    byte[] encodedInlineDataDescriptors,
                    int frameSize,
                    int frameReferenceMapSize) {
        assert fatalIfNotSorted(catchRangePositions);
        this.catchRangePositions = catchRangePositions;
        this.catchBlockPositions = catchBlockPositions;
        this.compressedJavaFrameDescriptors = compressedJavaFrameDescriptors;
        this.encodedInlineDataDescriptors = encodedInlineDataDescriptors;
        this.referenceMaps = referenceMaps;
        this.frameReferenceMapSize = frameReferenceMapSize;
        super.setStopPositions(stopPositions, directCallees, numberOfIndirectCalls, numberOfSafepoints);
        super.setFrameSize(frameSize);
        super.setData(scalarLiterals, referenceLiterals, codeBuffer);
    }

    /**
     * Creates a copy of this target method.
     */
    protected abstract CPSTargetMethod createDuplicate();

    /**
     * Gets the size of the reference map covering the registers.
     */
    public abstract int registerReferenceMapSize();

    /**
     * Overwrite this method if the instruction pointer for a throw must be adjusted when it
     * is in a frame that has made a call (i.e. not hte top frame).
     * @return the value that should be added to the instruction pointer
     */
    public int callerInstructionPointerAdjustment() {
        return 0;
    }

    @Override
    public final Address throwAddressToCatchAddress(boolean isTopFrame, Address throwAddress, Class<? extends Throwable> throwableClass) {
        if (catchRangePositions != null) {

            int throwOffset = throwAddress.minus(codeStart).toInt();

            if (!isTopFrame) {
                throwOffset += callerInstructionPointerAdjustment();
            }

            for (int i = catchRangePositions.length - 1; i >= 0; i--) {
                if (throwOffset >= catchRangePositions[i]) {
                    final int catchBlockPosition = catchBlockPositions[i];
                    if (catchBlockPosition <= 0) {
                        return Address.zero();
                    }
                    return codeStart.plus(catchBlockPosition);
                }
            }
        }
        return Address.zero();
    }

    /**
     * @return the safepoint index for the given instruction pointer or -1 if not found
     */
    public int findSafepointIndex(Pointer instructionPointer) {
        final Pointer firstInstructionPointer = codeStart();
        int safepointIndex = 0;
        for (int stopIndex = numberOfDirectCalls() + numberOfIndirectCalls(); stopIndex < numberOfStopPositions(); ++stopIndex) {
            if (firstInstructionPointer.plus(stopPosition(stopIndex)).equals(instructionPointer)) {
                return safepointIndex;
            }
            ++safepointIndex;
        }
        return -1;
    }

    @Override
    public int findNextCall(int targetCodePosition, boolean nativeFunctionCall) {
        if (stopPositions == null || targetCodePosition < 0 || targetCodePosition > code.length) {
            return -1;
        }

        int closestCallPosition = Integer.MAX_VALUE;
        final int numberOfCalls = numberOfDirectCalls() + numberOfIndirectCalls();
        for (int stopIndex = 0; stopIndex < numberOfCalls; stopIndex++) {
            final int callPosition = stopPosition(stopIndex);
            if (callPosition > targetCodePosition && callPosition < closestCallPosition && (!nativeFunctionCall || StopPositions.isNativeFunctionCall(stopPositions, stopIndex))) {
                closestCallPosition = callPosition;
            }
        }
        if (closestCallPosition != Integer.MAX_VALUE) {
            return closestCallPosition;
        }
        return -1;
    }

    @Override
    public final boolean isGenerated() {
        return code != null;
    }

    /**
     * Gets the {@linkplain #referenceMaps() frame reference map} for the {@code n}th stop in this target method of a
     * given type.
     *
     * Note that unless the reference maps are finalized, the value returned by this method may differ
     * from the value returned by a subsequent call to this method.
     *
     * @param stopType
     *                specifies the requested stop type
     * @param n
     *                denotes an value in the range {@code [0 .. numberOfStopPositions(stopType))}
     * @return the bit map denoting which frame slots contain object references at the specified stop in this target method
     * @throws IllegalArgumentException if {@code n < 0 || n>= numberOfStopPositions(stopType)}
     */
    public final ByteArrayBitMap frameReferenceMapFor(StopType stopType, int n) throws IllegalArgumentException {
        return frameReferenceMapFor(stopType.stopPositionIndex(this, n));
    }

    /**
     * Gets the {@linkplain #referenceMaps() register reference map} for a given safepoint.
     *
     * Note that unless the reference maps are finalized, the value returned by this method may differ
     * from the value returned by a subsequent call to this method.
     *
     * @param safepointIndex a value between {@code [0 .. numberOfSafepoints())} denoting a safepoint
     * @return the bit map specifying which registers contain object references at the given safepoint position
     */
    public final ByteArrayBitMap registerReferenceMapFor(int safepointIndex) {
        final int registerReferenceMapSize = registerReferenceMapSize();
        // The register reference maps come after all the frame reference maps in _referenceMaps.
        final int start = frameReferenceMapsSize() + (registerReferenceMapSize * safepointIndex);
        return new ByteArrayBitMap(referenceMaps, start, registerReferenceMapSize);
    }

    /**
     * Gets the number of leading bytes in {@link #referenceMaps()} encoding the frame reference maps for each {@linkplain #stopPositions() stop position}.
     * The remainder of the bytes in {@link #referenceMaps()} encodes the register reference maps for the safepoints.
     */
    public int frameReferenceMapsSize() {
        return frameReferenceMapSize * numberOfStopPositions();
    }

    /**
     * Gets an object describing the layout of an activation frame created on the stack for a call to this target method.
     * @return an object that represents the layout of this stack frame
     */
    public abstract CompiledStackFrameLayout stackFrameLayout();

    public String referenceMapsToString() {
        if (numberOfStopPositions() == 0) {
            return "";
        }
        final StringBuilder buf = new StringBuilder();
        final CompiledStackFrameLayout layout = stackFrameLayout();
        final Slots slots = layout.slots();
        int safepointIndex = 0;
        final int firstSafepointStopIndex = numberOfDirectCalls() + numberOfIndirectCalls();
        for (int stopIndex = 0; stopIndex < numberOfStopPositions(); ++stopIndex) {
            final int stopPosition = stopPosition(stopIndex);
            buf.append(String.format("stop: index=%d, position=%d, type=%s%n", stopIndex, stopPosition,
                            stopIndex < numberOfDirectCalls() ? "direct call" :
                           (stopIndex < firstSafepointStopIndex ? "indirect call" : "safepoint")));
            int byteIndex = stopIndex * frameReferenceMapSize();
            final ByteArrayBitMap referenceMap = new ByteArrayBitMap(referenceMaps(), byteIndex, frameReferenceMapSize());
            buf.append(slots.toString(referenceMap));
            if (stopIndex >= firstSafepointStopIndex && registerReferenceMapSize() != 0) {
                // The register reference maps come after all the frame reference maps in _referenceMaps.
                byteIndex = frameReferenceMapsSize() + (registerReferenceMapSize() * safepointIndex);
                String referenceRegisters = "";
                buf.append("  register map:");
                for (int i = 0; i < registerReferenceMapSize(); i++) {
                    final byte refMapByte = referenceMaps[byteIndex];
                    buf.append(String.format(" 0x%x", refMapByte & 0xff));
                    if (refMapByte != 0) {
                        for (int bitIndex = 0; bitIndex < Bytes.WIDTH; bitIndex++) {
                            if (((refMapByte >>> bitIndex) & 1) != 0) {
                                referenceRegisters += "reg" + (bitIndex + (i * Bytes.WIDTH)) + " ";
                            }
                        }
                    }
                    byteIndex++;
                }
                if (!referenceRegisters.isEmpty()) {
                    buf.append(" { " + referenceRegisters + "}");
                }
                buf.append(String.format("%n"));
                ++safepointIndex;
            }
        }

        return buf.toString();
    }

    /**
     * Gets the {@linkplain InlineDataDescriptor inline data descriptors} associated with this target method's code
     * encoded as a byte array in the format described {@linkplain InlineDataDescriptor here}.
     *
     * @return null if there are no inline data descriptors associated with this target method's code
     */
    @Override
    public byte[] encodedInlineDataDescriptors() {
        return encodedInlineDataDescriptors;
    }

    /**
     * Gets the {@linkplain #referenceMaps() frame reference map} for a stop position denoted by a given index into
     * {@link #stopPositions()}.
     *
     * Note that unless the reference maps are finalized, the value returned by this method may differ
     * from the value returned by a subsequent call to this method.
     *
     * @param stopIndex
     *                a valid index into {@link #stopPositions()}
     * @return the bit map denoting which frame slots contain object references at the specified stop in this target method
     * @throws IllegalArgumentException if {@code stopIndex < 0 || stopIndex >= numberOfStopPositions()}
     */
    public final ByteArrayBitMap frameReferenceMapFor(int stopIndex) {
        if (stopIndex < 0 || stopIndex >= numberOfStopPositions()) {
            throw new IllegalArgumentException();
        }
        return new ByteArrayBitMap(referenceMaps, stopIndex * frameReferenceMapSize, frameReferenceMapSize);
    }

    /**
     * Gets the frame descriptor at or before a given instruction pointer. The returned frame descriptor is the one at
     * the closest position less or equal to the position denoted by {@code instructionPointer}.
     *
     * @param instructionPointer a pointer to an instruction within this method
     * @return the frame descriptor closest to the position denoted by {@code instructionPointer} or null if not frame
     *         descriptor can be determined for {@code instructionPointer}
     */
    private TargetJavaFrameDescriptor getJavaFrameDescriptorFor(Pointer instructionPointer) {
        if (stopPositions == null || compressedJavaFrameDescriptors == null) {
            return null;
        }
        int stopIndex = findStopIndex(instructionPointer);
        if (stopIndex < 0) {
            return null;
        }
        return TargetJavaFrameDescriptor.get(this, stopIndex);
    }

    protected int findStopIndex(Pointer instructionPointer) {
        int stopIndex = -1;
        int minDistance = Integer.MAX_VALUE;
        final int position = instructionPointer.minus(codeStart).toInt();
        for (int i = 0; i < numberOfStopPositions(); i++) {
            final int stopPosition = stopPosition(i);
            if (stopPosition <= position) {
                final int distance = position - stopPosition;
                if (distance < minDistance) {
                    minDistance = distance;
                    stopIndex = i;
                }
            }
        }
        return stopIndex;
    }

    @Override
    public BytecodeLocation getBytecodeLocationFor(Pointer ip, boolean ipIsReturnAddress) {
        if (ipIsReturnAddress && platform().isa.offsetToReturnPC == 0) {
            ip = ip.minus(1);
        }
        return getJavaFrameDescriptorFor(ip);
    }

    @Override
    public BytecodeLocation getBytecodeLocationFor(int stopIndex) {
        return TargetJavaFrameDescriptor.get(this, stopIndex);
    }

    /**
     * @see TargetJavaFrameDescriptor#compress(java.util.List)
     */
    public final byte[] compressedJavaFrameDescriptors() {
        return compressedJavaFrameDescriptors;
    }

    @Override
    public CiDebugInfo getDebugInfo(Pointer instructionPointer, boolean implicitExceptionPoint) {
        if (implicitExceptionPoint) {
            // CPS target methods don't have Java frame descriptors at implicit throw points.
            return null;
        }
        if (Platform.platform().isa.offsetToReturnPC == 0) {
            instructionPointer = instructionPointer.minus(1);
        }

        if (stopPositions == null || compressedJavaFrameDescriptors == null) {
            return null;
        }
        int stopIndex = findStopIndex(instructionPointer);
        if (stopIndex < 0) {
            return null;
        }
        TargetJavaFrameDescriptor jfd = TargetJavaFrameDescriptor.get(this, stopIndex);
        Frame frame = jfd.toFrame(null);

        CiBitMap regRefMap = null;
        CiBitMap frameRefMap = new CiBitMap(referenceMaps, stopIndex * frameReferenceMapSize, frameReferenceMapSize);
        if (stopIndex >= numberOfDirectCalls() + numberOfIndirectCalls()) {
            int regRefMapSize = registerReferenceMapSize();
            regRefMap = new CiBitMap(referenceMaps, frameReferenceMapsSize() + (regRefMapSize * stopIndex), regRefMapSize);
        }

        return new CiDebugInfo(frame, regRefMap, frameRefMap);
    }

    @HOSTED_ONLY
    @Override
    public void gatherCalls(Set<MethodActor> directCalls, Set<MethodActor> virtualCalls, Set<MethodActor> interfaceCalls, Set<MethodActor> inlinedMethods) {
        if (directCallees != null) {
            for (Object o : directCallees) {
                if (o instanceof MethodActor) {
                    directCalls.add((MethodActor) o);
                }
            }
        }

        if (!classMethodActor.isTemplate()) {
            final List<TargetJavaFrameDescriptor> frameDescriptors = TargetJavaFrameDescriptor.inflate(compressedJavaFrameDescriptors);
            final int numberOfCalls = numberOfDirectCalls() + numberOfIndirectCalls();
            for (int stopIndex = numberOfDirectCalls(); stopIndex < numberOfCalls; ++stopIndex) {
                BytecodeLocation location = frameDescriptors.get(stopIndex);
                if (location != null) {
                    final InvokedMethodRecorder invokedMethodRecorder = new InvokedMethodRecorder(location.classMethodActor, directCalls, virtualCalls, interfaceCalls);
                    final BytecodeScanner bytecodeScanner = new BytecodeScanner(invokedMethodRecorder);
                    final byte[] bytecode = location.classMethodActor.codeAttribute().code();
                    if (bytecode != null && location.bytecodePosition < bytecode.length) {
                        bytecodeScanner.scanInstruction(bytecode, location.bytecodePosition);
                    }
                    inlinedMethods.add(location.classMethodActor);
                    location = location.parent();
                    while (location != null) {
                        inlinedMethods.add(location.classMethodActor);
                        location = location.parent();
                    }
                }
            }
        }
    }

    /**
     * Gets the frame and register reference maps for this target method.
     *
     * Note that unless the reference maps are finalized, the value returned by this method may differ
     * from the value returned by a subsequent call to this method.
     *
     * The format of the returned byte array is described by the following pseudo C declaration:
     * <p>
     *
     * <pre>
     * referenceMaps {
     *     {
     *         u1 map[frameReferenceMapSize];
     *     } directCallFrameMaps[numberOfDirectCalls]
     *     {
     *         u1 map[frameReferenceMapSize];
     *     } indirectCallFrameMaps[numberOfIndirectCalls]
     *     {
     *         u1 map[frameReferenceMapSize];
     *     } safepointFrameMaps[numberOfSafepoints]
     *     {
     *         u1 map[registerReferenceMapSize];
     *     } safepointRegisterMaps[numberOfSafepoints];
     * }
     * </pre>
     */
    @Override
    public final byte[] referenceMaps() {
        return referenceMaps;
    }

    /**
     * Gets the size of a single frame reference map encoded in this method's {@linkplain #referenceMaps() reference maps}.
     */
    public final int frameReferenceMapSize() {
        return frameReferenceMapSize;
    }

    /**
     * Traces the exception handlers of the compiled code represented by this object.
     *
     * @param writer where the trace is written
     */
    @Override
    public final void traceExceptionHandlers(IndentWriter writer) {
        if (catchRangePositions != null) {
            assert catchBlockPositions != null;
            writer.println("Exception handlers:");
            writer.indent();
            for (int i = 0; i < catchRangePositions.length; i++) {
                if (catchBlockPositions[i] != 0) {
                    final int catchRangeEnd = (i == catchRangePositions.length - 1) ? code.length : catchRangePositions[i + 1];
                    final int catchRangeStart = catchRangePositions[i];
                    writer.println("[" + catchRangeStart + " .. " + catchRangeEnd + ") -> " + catchBlockPositions[i]);
                }
            }
            writer.outdent();
        }
    }

    /**
     * Traces the {@linkplain #compressedJavaFrameDescriptors() frame descriptors} for the compiled code represented by this object.
     *
     * @param writer where the trace is written
     */
    @Override
    public final void traceDebugInfo(IndentWriter writer) {
        if (compressedJavaFrameDescriptors != null) {
            writer.println("Frame Descriptors:");
            writer.indent();
            final List<TargetJavaFrameDescriptor> frameDescriptors = TargetJavaFrameDescriptor.inflate(compressedJavaFrameDescriptors);
            for (int stopIndex = 0; stopIndex < frameDescriptors.size(); ++stopIndex) {
                final TargetJavaFrameDescriptor frameDescriptor = frameDescriptors.get(stopIndex);
                final int stopPosition = stopPosition(stopIndex);
                writer.println(stopPosition + ": " + frameDescriptor);
            }
            writer.outdent();
        }
    }

    /**
     * Traces the {@linkplain #referenceMaps() reference maps} for the stops in the compiled code represented by this object.
     *
     * @param writer where the trace is written
     */
    public void traceReferenceMaps(IndentWriter writer) {
        final String refmaps = referenceMapsToString();
        if (!refmaps.isEmpty()) {
            writer.println("Reference Maps:");
            writer.println(Strings.indent(refmaps, writer.indentation()));
        }
    }

    @Override
    public void traceBundle(IndentWriter writer) {
        super.traceBundle(writer);
        traceReferenceMaps(writer);
    }
}
