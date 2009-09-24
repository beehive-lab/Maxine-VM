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
package com.sun.max.vm.compiler.target;

import java.io.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.b.c.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.ir.observer.*;
import com.sun.max.vm.compiler.target.TargetBundleLayout.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.jit.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.JavaStackFrameLayout.*;

/**
 * Target method that saves for each catch block the ranges in the code that can
 * refer to them. Does not include the type of the caught exception.
 *
 * @author Doug Simon
 * @author Thomas Wuerthinger
 */
public abstract class CPSTargetMethod extends TargetMethod implements IrMethod {

    @INSPECTED
    private int[] catchRangePositions;

    @INSPECTED
    private int[] catchBlockPositions;

    @INSPECTED
    protected byte[] compressedJavaFrameDescriptors;

    /**
     * If non-null, this array encodes a serialized array of {@link InlineDataDescriptor} objects.
     */
    @INSPECTED
    protected byte[] encodedInlineDataDescriptors;

    protected byte[] referenceMaps;

    @INSPECTED
    protected int frameReferenceMapSize;

    public CPSTargetMethod(ClassMethodActor classMethodActor, RuntimeCompilerScheme compilerScheme) {
        super(classMethodActor, compilerScheme, null);
    }

    @Override
    public boolean contains(Builtin builtin, boolean defaultResult) {
        return defaultResult;
    }

    @Override
    public boolean isNative() {
        return classMethodActor().isNative();
    }

    @Override
    public final void cleanup() {
    }

    @Override
    public String name() {
        return description();
    }

    @Override
    public int count(Builtin builtin, int defaultResult) {
        return defaultResult;
    }

    @Override
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

    /**
     * Traces the exception handlers of the compiled code represented by this object.
     *
     * @param writer where the trace is written
     */
    public final void traceExceptionHandlers(IndentWriter writer) {
        if (catchRangePositions != null) {
            assert catchBlockPositions != null;
            writer.println("Catches:");
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

    public final void setGenerated(int[] catchRangePositions, int[] catchBlockPositions, int[] stopPositions, byte[] compressedJavaFrameDescriptors, Object[] directCallees, int numberOfIndirectCalls,
                    int numberOfSafepoints, byte[] referenceMaps, byte[] scalarLiterals, Object[] referenceLiterals, Object codeOrCodeBuffer, byte[] encodedInlineDataDescriptors, int frameSize,
                    int frameReferenceMapSize, TargetABI abi) {
        this.catchRangePositions = catchRangePositions;
        this.catchBlockPositions = catchBlockPositions;
        this.compressedJavaFrameDescriptors = compressedJavaFrameDescriptors;
        this.encodedInlineDataDescriptors = encodedInlineDataDescriptors;
        this.referenceMaps = referenceMaps;
        this.frameReferenceMapSize = frameReferenceMapSize;
        super.setABI(abi);
        super.setStopPositions(stopPositions, directCallees, numberOfIndirectCalls, numberOfSafepoints);
        super.setFrameSize(frameSize);
        super.setData(scalarLiterals, referenceLiterals, codeOrCodeBuffer);
    }

    public final TargetMethod duplicate() {
        final TargetGeneratorScheme targetGeneratorScheme = (TargetGeneratorScheme) compilerScheme;
        final CPSTargetMethod duplicate = targetGeneratorScheme.targetGenerator().createIrMethod(classMethodActor());
        final TargetBundleLayout targetBundleLayout = TargetBundleLayout.from(this);
        Code.allocate(targetBundleLayout, duplicate);
        duplicate.setGenerated(
            catchRangePositions(),
            catchBlockPositions(),
            stopPositions,
            compressedJavaFrameDescriptors,
            directCallees(),
            numberOfIndirectCalls(),
            numberOfSafepoints(),
            referenceMaps(),
            scalarLiterals(),
            referenceLiterals(),
            code(),
            encodedInlineDataDescriptors,
            frameSize(),
            frameReferenceMapSize(), super.abi()
        );
        return duplicate;
    }


    /**
     * Gets an object describing the layout of an activation frame created on the stack for a call to this target method.
     */
    public abstract JavaStackFrameLayout stackFrameLayout();

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

    /**
     * Gets the target code position for a machine code instruction address.
     *
     * @param instructionPointer
     *                an instruction pointer that may denote an instruction in this target method
     * @return the start position of the bytecode instruction that is implemented at the instruction pointer or
     *         -1 if {@code instructionPointer} denotes an instruction that does not correlate to any bytecode. This will
     *         be the case when {@code instructionPointer} is in the adapter frame stub code, prologue or epilogue.
     */
    public final int targetCodePositionFor(Pointer instructionPointer) {
        final int targetCodePosition = instructionPointer.minus(codeStart).toInt();
        if (targetCodePosition >= 0 && targetCodePosition < code.length) {
            return targetCodePosition;
        }
        return -1;
    }

    /**
     * Gets the position of the next call (direct or indirect) in this target method after a given position.
     *
     * @param targetCodePosition the position from which to start searching
     * @param nativeFunctionCall if {@code true}, then the search is refined to only consider
     *            {@linkplain #isNativeFunctionCall(int) native function calls}.
     *
     * @return -1 if the search fails
     */
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

    /**
     * Gets the index of a stop position within this target method derived from a given instruction pointer. If the
     * instruction pointer is equal to a safepoint position, then the index in {@link #stopPositions()} of that
     * safepoint is returned. Otherwise, the index of the highest stop position that is less than or equal to the
     * (possibly adjusted) target code position
     * denoted by the instruction pointer is returned.  That is, if {@code instructionPointer} does not exactly match a
     * stop position 'p' for a direct or indirect call, then the index of the highest stop position less than
     * 'p' is returned.
     *
     * @return -1 if no stop index can be found for {@code instructionPointer}
     * @see #stopPositions()
     */
    public int findClosestStopIndex(Pointer instructionPointer, boolean adjustPcForCall) {
        final int targetCodePosition = targetCodePositionFor(instructionPointer);
        if (stopPositions == null || targetCodePosition < 0 || targetCodePosition > code.length) {
            return -1;
        }

        // Direct calls come first, followed by indirect calls and safepoints in the stopPositions array.

        // Check for matching safepoints first
        for (int i = numberOfDirectCalls() + numberOfIndirectCalls(); i < numberOfStopPositions(); i++) {
            if (stopPosition(i) == targetCodePosition) {
                return i;
            }
        }

        // Since this is not a safepoint, it must be a call.

        final int adjustedTargetCodePosition;
        if (adjustPcForCall && compilerScheme.vmConfiguration().platform().processorKind.instructionSet.offsetToReturnPC == 0) {
            // targetCodePostion is the instruction after the call (which might be another call).
            // We need the find the call at which we actually stopped.
            adjustedTargetCodePosition = targetCodePosition - 1;
        } else {
            adjustedTargetCodePosition = targetCodePosition;
        }

        int stopIndexWithClosestPosition = -1;
        for (int i = numberOfDirectCalls() - 1; i >= 0; --i) {
            final int directCallPosition = stopPosition(i);
            if (directCallPosition <= adjustedTargetCodePosition) {
                if (directCallPosition == adjustedTargetCodePosition) {
                    return i; // perfect match; no further searching needed
                }
                stopIndexWithClosestPosition = i;
                break;
            }
        }

        // It is not enough that we find the first matching position, since there might be a direct as well as an indirect call before the instruction pointer
        // so we find the closest one. This can be avoided if we sort the stopPositions array first, but the runtime cost of this is unknown.
        for (int i = numberOfDirectCalls() + numberOfIndirectCalls() - 1; i >= numberOfDirectCalls(); i--) {
            final int indirectCallPosition = stopPosition(i);
            if (indirectCallPosition <= adjustedTargetCodePosition && (stopIndexWithClosestPosition < 0 || indirectCallPosition > stopPosition(stopIndexWithClosestPosition))) {
                stopIndexWithClosestPosition = i;
                break;
            }
        }

        return stopIndexWithClosestPosition;
    }


    /**
     * Prepares the reference map for a portion of memory that contains saved register state corresponding
     * to a safepoint triggered at a particular instruction. This method fetches the reference map information
     * for the specified method and then copies it over the reference map for this portion of memory.
     *
     * @param registerState a pointer to the saved register state
     * @param instructionPointer the instruction pointer at the safepoint trap
     */
    @Override
    public void prepareRegisterReferenceMap(Pointer registerState, Pointer instructionPointer, StackReferenceMapPreparer preparer) {
        preparer.tracePrepareReferenceMap(this, this.findClosestStopIndex(instructionPointer, true), Pointer.zero(), "registers");
        final int safepointIndex = this.findSafepointIndex(instructionPointer);
        if (safepointIndex < 0) {
            Log.print("Could not find safepoint index for instruction at position ");
            Log.print(instructionPointer.minus(codeStart()).toInt());
            Log.print(" in ");
            Log.printMethodActor(classMethodActor(), true);
            FatalError.unexpected("Could not find safepoint index");
        }

        // The register reference maps come after all the frame reference maps in referenceMap.
        int byteIndex = frameReferenceMapsSize() + (registerReferenceMapSize() * safepointIndex);

        final int registersSlotIndex = preparer.referenceMapBitIndex(registerState);
        for (int i = 0; i < registerReferenceMapSize(); i++) {
            final byte referenceMapByte = referenceMaps()[byteIndex];
            preparer.traceReferenceMapByteBefore(byteIndex, referenceMapByte, "Register");
            final int baseSlotIndex = registersSlotIndex + (i * Bytes.WIDTH);
            preparer.setBits(baseSlotIndex, referenceMapByte);
            preparer.traceReferenceMapByteAfter(Pointer.zero(), baseSlotIndex, referenceMapByte);
            byteIndex++;
        }
    }

    @Override
    public final boolean isGenerated() {
        return code != null;
    }

    @Override
    public final String traceToString() {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final IndentWriter writer = new IndentWriter(new OutputStreamWriter(byteArrayOutputStream));
        writer.println("target method: " + this);
        traceBundle(writer);
        writer.flush();
        if (MaxineVM.isPrototyping()) {
            Disassemble.disassemble(byteArrayOutputStream, this);
        }
        return byteArrayOutputStream.toString();
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
     * Gets a string representation of the reference map for each {@linkplain #stopPositions() stop} in this target method.
     */
    public String referenceMapsToString() {
        if (numberOfStopPositions() == 0) {
            return "";
        }
        final StringBuilder buf = new StringBuilder();
        final JavaStackFrameLayout layout = stackFrameLayout();
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
     * Traces the metadata of the compiled code represented by this object. In particular, the
     * {@linkplain #traceExceptionHandlers(IndentWriter) exception handlers}, the
     * {@linkplain #traceDirectCallees(IndentWriter) direct callees}, the #{@linkplain #traceScalarBytes(IndentWriter, TargetBundle) scalar data},
     * the {@linkplain #traceReferenceLiterals(IndentWriter, TargetBundle) reference literals} and the address of the
     * array containing the {@linkplain #code() compiled code}.
     *
     * @param writer where the trace is written
     */
    public final void traceBundle(IndentWriter writer) {
        final TargetBundleLayout targetBundleLayout = TargetBundleLayout.from(this);
        writer.println("Layout:");
        writer.println(Strings.indent(targetBundleLayout.toString(), writer.indentation()));
        traceExceptionHandlers(writer);
        traceDirectCallees(writer);
        traceScalarBytes(writer, targetBundleLayout);
        traceReferenceLiterals(writer, targetBundleLayout);
        traceFrameDescriptors(writer);
        traceReferenceMaps(writer);
        writer.println("Code cell: " + targetBundleLayout.cell(start(), ArrayField.code).toString());
    }

    /**
     * Traces the {@linkplain #directCallees() direct callees} of the compiled code represented by this object.
     *
     * @param writer where the trace is written
     */
    public final void traceDirectCallees(IndentWriter writer) {
        if (directCallees != null) {
            assert stopPositions != null && directCallees.length <= numberOfStopPositions();
            writer.println("Direct Calls: ");
            writer.indent();
            for (int i = 0; i < directCallees.length; i++) {
                writer.println(stopPosition(i) + " -> " + directCallees[i]);
            }
            writer.outdent();
        }
    }


    /**
     * Traces the {@linkplain #scalarLiterals() scalar data} addressed by the compiled code represented by this object.
     *
     * @param writer where the trace is written
     */
    public final void traceScalarBytes(IndentWriter writer, final TargetBundleLayout targetBundleLayout) {
        if (scalarLiterals != null) {
            writer.println("Scalars:");
            writer.indent();
            for (int i = 0; i < scalarLiterals.length; i++) {
                final Pointer pointer = targetBundleLayout.cell(start(), ArrayField.scalarLiterals).plus(ArrayField.scalarLiterals.arrayLayout.getElementOffsetInCell(i));
                writer.println("[" + pointer.toString() + "] 0x" + Integer.toHexString(scalarLiterals[i] & 0xFF) + "  " + scalarLiterals[i]);
            }
            writer.outdent();
        }
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
        int resultIndex = -1;
        int minDistance = Integer.MAX_VALUE;
        final int position = instructionPointer.minus(codeStart).toInt();
        for (int i = 0; i < numberOfStopPositions(); i++) {
            final int stopPosition = stopPosition(i);
            if (stopPosition <= position) {
                final int distance = position - stopPosition;
                if (distance < minDistance) {
                    minDistance = distance;
                    resultIndex = i;
                }
            }
        }
        if (resultIndex < 0) {
            return null;
        }
        return TargetJavaFrameDescriptor.get(this, resultIndex);
    }

    /**
     * Gets the bytecode locations for the inlining chain rooted at a given instruction pointer. The first bytecode
     * location in the returned sequence is the one at the closest position less or equal to the position denoted by
     * {@code instructionPointer}.
     *
     * @param instructionPointer a pointer to an instruction within this method
     * @return the bytecode locations for the inlining chain rooted at {@code instructionPointer}. This will be null if
     *         no bytecode location can be determined for {@code instructionPointer}.
     */
    @Override
    public Iterator<? extends BytecodeLocation> getBytecodeLocationsFor(Pointer instructionPointer) {
        final TargetJavaFrameDescriptor targetFrameDescriptor = getJavaFrameDescriptorFor(instructionPointer);
        if (targetFrameDescriptor != null) {
            return targetFrameDescriptor.inlinedFrames();
        }
        return null;
    }

    /**
     * @see TargetJavaFrameDescriptor#compress(com.sun.max.collect.Sequence)
     */
    public final byte[] compressedJavaFrameDescriptors() {
        return compressedJavaFrameDescriptors;
    }

    /**
     * Traces the {@linkplain #compressedJavaFrameDescriptors() frame descriptors} for the compiled code represented by this object.
     *
     * @param writer where the trace is written
     */
    public final void traceFrameDescriptors(IndentWriter writer) {
        if (compressedJavaFrameDescriptors != null) {
            writer.println("Frame Descriptors:");
            writer.indent();
            final IndexedSequence<TargetJavaFrameDescriptor> frameDescriptors = TargetJavaFrameDescriptor.inflate(compressedJavaFrameDescriptors);
            for (int stopIndex = 0; stopIndex < frameDescriptors.length(); ++stopIndex) {
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
    public final void traceReferenceMaps(IndentWriter writer) {
        final String refmaps = referenceMapsToString();
        if (!refmaps.isEmpty()) {
            writer.println("Reference Maps:");
            writer.println(Strings.indent(refmaps, writer.indentation()));
        }
    }

    public void prepareFrameReferenceMap(int stopIndex, Pointer refmapFramePointer, StackReferenceMapPreparer preparer) {
        preparer.tracePrepareReferenceMap(this, stopIndex, refmapFramePointer, "frame");
        int frameSlotIndex = preparer.referenceMapBitIndex(refmapFramePointer);
        int byteIndex = stopIndex * frameReferenceMapSize();
        for (int i = 0; i < frameReferenceMapSize(); i++) {
            final byte frameReferenceMapByte = referenceMaps()[byteIndex];
            preparer.traceReferenceMapByteBefore(byteIndex, frameReferenceMapByte, "Frame");
            preparer.setBits(frameSlotIndex, frameReferenceMapByte);
            preparer.traceReferenceMapByteAfter(refmapFramePointer, frameSlotIndex, frameReferenceMapByte);
            frameSlotIndex += Bytes.WIDTH;
            byteIndex++;
        }
    }

    @Override
    public void gatherCalls(AppendableSequence<MethodActor> directCalls, AppendableSequence<MethodActor> virtualCalls, AppendableSequence<MethodActor> interfaceCalls) {
        if (compilerScheme instanceof BcCompiler) {
            ((BcCompiler) compilerScheme).gatherCalls(this, directCalls, virtualCalls, interfaceCalls);
        } else if (compilerScheme instanceof JitCompiler) {
            ((JitCompiler) compilerScheme).gatherCalls(this, directCalls, virtualCalls, interfaceCalls);
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
     *     } inirectCallFrameMaps[numberOfIndirectCalls]
     *     {
     *         u1 map[frameReferenceMapSize];
     *     } safepointFrameMaps[numberOfSafepoints]
     *     {
     *         u1 map[registerReferenceMapSize];
     *     } safepointRegisterMaps[numberOfSafepoints];
     * }
     * </pre>
     */
    public final byte[] referenceMaps() {
        return referenceMaps;
    }


    /**
     * Gets the size of a single frame reference map encoded in this method's {@linkplain #referenceMaps() reference maps}.
     */
    public final int frameReferenceMapSize() {
        return frameReferenceMapSize;
    }
}
