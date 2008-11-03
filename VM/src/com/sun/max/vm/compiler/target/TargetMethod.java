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

import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.profile.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.compiler.ir.observer.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.target.TargetBundleLayout.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.object.host.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.JavaStackFrameLayout.*;
import com.sun.max.vm.template.*;

/**
 * A collection of objects that represent the compiled target code
 * and its auxiliary data structures for a Java method.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public abstract class TargetMethod extends RuntimeMemoryRegion implements IrMethod {

    public static enum TargetMethodType {
        JIT,
        JIT_TRACED,
        OPTIMIZED
    }

    @INSPECTED
    private final ClassMethodActor _classMethodActor;

    public final ClassMethodActor classMethodActor() {
        return _classMethodActor;
    }

    public abstract InstructionSet instructionSet();

    public TargetMethod(ClassMethodActor classMethodActor) {
        _classMethodActor = classMethodActor;
    }

    public final String name() {
        return _classMethodActor.name().toString();
    }

    public final boolean isNative() {
        return classMethodActor().compilee().isNative();
    }

    public final void cleanup() {
    }

    /**
     * @see #catchRangePositions()
     */
    private int[] _catchRangePositions;

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
        return _catchRangePositions;
    }

    /**
     * @see #catchRangePositions()
     */
    public final int numberOfCatchRanges() {
        return (_catchRangePositions == null) ? 0 : _catchRangePositions.length;
    }

    /**
     * @see #catchRangePositions()
     */
    private int[] _catchBlockPositions;

    /**
     * @see #catchRangePositions()
     */
    public final int[] catchBlockPositions() {
        return _catchBlockPositions;
    }

    /**
     * The stop positions are encoded in the lower 31 bits of each element.
     * The high bit indicates whether the stop is a call that returns a Reference.
     *
     * @see #stopPosition()
     */
    @INSPECTED
    private int[] _stopPositions;

    public final int[] stopPositions() {
        return _stopPositions;
    }

    public final int numberOfStopPositions() {
        return _stopPositions == null ? 0 : _stopPositions.length;
    }

    /**
     * Gets the array recording the positions of the {@link StopType stops} in this target method.
     * <p>
     * This array is composed of four contiguous segments. The first segment contains the positions of the direct call
     * stops and the indexes in this segment match the entries of the {@link #_directCallees} array). The second segment
     * and third segments contain the positions of the register indirect call and safepoint stops. The fourth contains
     * the positions of guardpoint stops.
     * <p>
     *
     * <pre>
     *   +-----------------------------+-------------------------------+----------------------------+-----------------------------+
     *   |          direct calls       |           indirect calls      |          safepoints        |          guardpoints        |
     *   +-----------------------------+-------------------------------+----------------------------+-----------------------------+
     *    <-- numberOfDirectCalls() --> <-- numberOfIndirectCalls() --> <-- numberOfSafepoints() --> <-- numberOfGuardpoints() -->
     *
     *   +-----------------------------+
     *   |       directCallees()       |
     *   +-----------------------------+
     *
     * </pre>
     *
     * @see StopType
     */
    public final int stopPosition(int stopIndex) {
        return _stopPositions[stopIndex] & ~REFERENCE_RETURN_FLAG;
    }

    public static final int REFERENCE_RETURN_FLAG = 0x80000000;

    public final boolean isReferenceCall(int stopIndex) {
        return (_stopPositions[stopIndex] & REFERENCE_RETURN_FLAG) != 0;
    }

    /**
     * Gets the {@linkplain #referenceMaps() frame reference map} for a stop position denoted by a given index into
     * {@link #stopPositions()}.
     *
     * Note that unless {@link #areReferenceMapsFinalized()} returns true, the value returned by this method may differ
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
        return new ByteArrayBitMap(_referenceMaps, stopIndex * _frameReferenceMapSize, _frameReferenceMapSize);
    }

    /**
     * Gets the {@linkplain #referenceMaps() frame reference map} for the {@code n}th stop in this target method of a
     * given type.
     *
     * Note that unless {@link #areReferenceMapsFinalized()} returns true, the value returned by this method may differ
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
     * Gets the number of leading bytes in {@link #referenceMaps()} encoding the frame reference maps for each {@linkplain #stopPositions() stop position}.
     * The remainder of the bytes in {@link #referenceMaps()} encodes the register reference maps for the safepoints.
     */
    public int frameReferenceMapsSize() {
        return _frameReferenceMapSize * numberOfStopPositions();
    }

    /**
     * Gets the {@linkplain #referenceMaps() register reference map} for a given safepoint.
     *
     * Note that unless {@link #areReferenceMapsFinalized()} returns true, the value returned by this method may differ
     * from the value returned by a subsequent call to this method.
     *
     * @param safepointIndex
     *                a value between {@code [0 .. numberOfSafepoints())} denoting a safepoint
     * @return the bit map specifying which registers contain object references at the given safepoint position
     */
    public final ByteArrayBitMap registerReferenceMapFor(int safepointIndex) {
        final int registerReferenceMapSize = registerReferenceMapSize();
        // The register reference maps come after all the frame reference maps in _referenceMaps.
        final int start = frameReferenceMapsSize() + (registerReferenceMapSize * safepointIndex);
        return new ByteArrayBitMap(_referenceMaps, start, registerReferenceMapSize);
    }

    @INSPECTED
    private byte[] _compressedJavaFrameDescriptors;

    /**
     * If non-null, this array encodes a serialized array of {@link InlineDataDescriptor} objects.
     */
    @INSPECTED
    private byte[] _encodedInlineDataDescriptors;

    /**
     * Gets the {@linkplain InlineDataDescriptor inline data descriptors} associated with this target method's code
     * encoded as a byte array in the format described {@linkplain InlineDataDescriptor here}.
     *
     * @return null if there are no inline data descriptors associated with this target method's code
     */
    public byte[] encodedInlineDataDescriptors() {
        return _encodedInlineDataDescriptors;
    }

    /**
     * @see TargetJavaFrameDescriptor#compress(com.sun.max.collect.Sequence)
     */
    public final byte[] compressedJavaFrameDescriptors() {
        return _compressedJavaFrameDescriptors;
    }

    public TargetJavaFrameDescriptor getJavaFrameDescriptor(int index) {
        return TargetJavaFrameDescriptor.get(this, index);
    }

    public TargetJavaFrameDescriptor getPrecedingJavaFrameDescriptor(Address instructionPointer) {
        if (_stopPositions == null) {
            return null;
        }
        int resultIndex = -1;
        int minDistance = Integer.MAX_VALUE;
        final int position = instructionPointer.minus(_codeStart).toInt();
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
        return getJavaFrameDescriptor(resultIndex);
    }

    public final int numberOfDirectCalls() {
        return (_directCallees == null) ? 0 : _directCallees.length;
    }

    private ClassMethodActor[] _directCallees;

    /**
     * @return class method actors referenced by direct call instructions, matched to the stop positions array above by array index
     */
    public final ClassMethodActor[] directCallees() {
        return _directCallees;
    }

    /**
     * Gets the call entry point to be used for a direct call from this target method. By default, the
     * call entry point will be the one specified by the {@linkplain #abi() ABI} of this target method.
     * This models a direct call to another target method compiled with the same compiler as this target method.
     *
     * @param directCallIndex an index into the {@linkplain #directCallees() direct callees} of this target method
     */
    protected CallEntryPoint callEntryPointForDirectCall(int directCallIndex) {
        return abi().callEntryPoint();
    }

    @INSPECTED
    private int _numberOfIndirectCalls;

    public final int numberOfIndirectCalls() {
        return _numberOfIndirectCalls;
    }

    @INSPECTED
    private int _numberOfSafepoints;

    public final int numberOfSafepoints() {
        return _numberOfSafepoints;
    }

    private int _numberOfGuardpoints;

    public int numberOfGuardpoints() {
        return _numberOfGuardpoints;
    }

    /**
     * @see #referenceMaps()
     */
    private byte[] _referenceMaps;

    /**
     * Gets the frame and register reference maps for this target method.
     *
     * Note that unless {@link #areReferenceMapsFinalized()} returns true, the value returned by this method may differ
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
        return _referenceMaps;
    }

    /**
     * Determines if the {@linkplain #referenceMaps() reference maps} for this target method have been finalized. Only
     * finalized reference maps are guaranteed to never change for the remaining lifetime of this target method.
     */
    public abstract boolean areReferenceMapsFinalized();

    /**
     * Ensures that the {@linkplain #referenceMaps() reference maps} for this target method are finalized. Only
     * finalized reference maps are guaranteed to never change for the remaining lifetime of this target method.
     */
    public abstract void finalizeReferenceMaps();

    @INSPECTED
    private byte[] _scalarLiteralBytes;

    /**
     * @return non-object data referenced by the machine code
     */
    public final byte[] scalarLiteralBytes() {
        return _scalarLiteralBytes;
    }

    public final int numberOfScalarLiteralBytes() {
        return (_scalarLiteralBytes == null) ? 0 : _scalarLiteralBytes.length;
    }

    @INSPECTED
    private Object[] _referenceLiterals;

    /**
     * @return object references referenced by the machine code
     */
    public final Object[] referenceLiterals() {
        return _referenceLiterals;
    }

    public final int numberOfReferenceLiterals() {
        return (_referenceLiterals == null) ? 0 : _referenceLiterals.length;
    }

    @INSPECTED
    private byte[] _code;

    /**
     * Gets the byte array containing the target-specific machine code of this target method.
     */
    public final byte[] code() {
        return _code;
    }

    public final int codeLength() {
        return (_code == null) ? 0 : _code.length;
    }

    /**
     * Gets the address of the first instruction in this target method's {@linkplain #code() compiled code array}.
     * <p>
     * Needs {@linkplain DataPrototype#assignRelocationFlags() relocation}.
     */
    @INSPECTED
    private Pointer _codeStart = Pointer.zero();

    public final Pointer codeStart() {
        return _codeStart;
    }

    public final void setCodeStart(Pointer codeStart) {
        _codeStart = codeStart;
    }

    private int _frameSize;

    /**
     * Gets the size (in bytes) of the stack frame used for the local variables in
     * the method represented by this object. The stack pointer is decremented
     * by this amount when entering a method and is correspondingly incremented
     * by this amount when exiting a method.
     */
    public final int frameSize() {
        return _frameSize;
    }

    private int _frameReferenceMapSize;

    /**
     * Gets the size of a single frame reference map encoded in this method's {@linkplain #referenceMaps() reference maps}.
     */
    public final int frameReferenceMapSize() {
        return _frameReferenceMapSize;
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
                buf.append("Register map:");
                for (int i = 0; i < registerReferenceMapSize(); i++) {
                    final byte refMapByte = _referenceMaps[byteIndex];
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
                ++safepointIndex;
            }
        }

        return buf.toString();
    }

    /**
     * Gets an object describing the layout of an activation frame created on the stack for a call to this target method.
     */
    public abstract JavaStackFrameLayout stackFrameLayout();

    @INSPECTED
    private TargetABI _abi;

    public final TargetABI abi() {
        return _abi;
    }

    private int _markerPosition = -1;

    public int markerPosition() {
        return _markerPosition;
    }

    /**
     * Gets the size of the reference map covering the registers.
     */
    public abstract int registerReferenceMapSize();

    /**
     * Completes the definition of this target method as the result of compilation.
     *
     * @param targetBundle an object describing the address of the objects allocated in a {@link CodeRegion} referenced
     *            by this target method's fields
     * @param catchRangePositions describes the {@linkplain #catchRangePositions() code ranges} covered by exception
     *            dispatchers
     * @param catchBlockPositions the positions of the {@linkplain #catchBlockPositions() exception dispatchers}
     * @param stopPositions the positions in this target method at which the locations of object references are
     *            precisely known
     * @param directCallees the positions in this target method of direct calls (e.g. calls to methods for which a
     *            compiled version available)
     * @param numberOfIndirectCalls the positions in this target method of register indirect calls (e.g. late binding
     *            calls, virtual/interface calls)
     * @param numberOfSafepoints the number of {@linkplain Safepoint safepoint} positions in this target method
     * @param referenceMaps the set of bits maps, one per stop position, describing the locations of object references.
     *            The format requirements of this data structure are explained {@linkplain #referenceMaps() here}.
     * @param scalarLiteralBytes a byte array encoding the scalar data accessed by this target via code relative offsets
     * @param referenceLiterals an object array encoding the object references accessed by this target via code relative
     *            offsets
     * @param codeOrCodeBuffer the compiled code, either as a byte array, or as a <code>CodeBuffer</code> object
     * @param frameSize the amount of stack allocated for an activation frame during a call to this target method
     * @param abi
     */
    public final void setGenerated(TargetBundle targetBundle,
                            int[] catchRangePositions,
                            int[] catchBlockPositions,
                            int[] stopPositions,
                            byte[] compressedJavaFrameDescriptors,
                            ClassMethodActor[] directCallees,
                            int numberOfIndirectCalls,
                            int numberOfSafepoints,
                            int numberOfGuardpoints,
                            byte[] referenceMaps,
                            byte[] scalarLiteralBytes,
                            Object[] referenceLiterals,
                            Object codeOrCodeBuffer,
                            byte[] encodedInlineDataDescriptors,
                            int frameSize,
                            int frameReferenceMapSize,
                            TargetABI abi,
                            int markerPosition) {
        _codeStart = targetBundle.firstElementPointer(ArrayField.code);
        _frameSize = frameSize;
        _frameReferenceMapSize = frameReferenceMapSize;
        _numberOfIndirectCalls = numberOfIndirectCalls;
        _numberOfSafepoints = numberOfSafepoints;
        _numberOfGuardpoints = numberOfGuardpoints;
        _abi = abi;
        _compressedJavaFrameDescriptors = compressedJavaFrameDescriptors;
        _encodedInlineDataDescriptors = encodedInlineDataDescriptors;
        _markerPosition = markerPosition;

        assert checkReferenceMapSize(stopPositions, numberOfSafepoints, referenceMaps, frameReferenceMapSize);

        // copy the arrays into the target bundle
        _catchRangePositions = copyObjectIntoArrayCell(catchRangePositions, targetBundle, ArrayField.catchRangePositions);
        _catchBlockPositions = copyObjectIntoArrayCell(catchBlockPositions, targetBundle, ArrayField.catchBlockPositions);
        _stopPositions = copyObjectIntoArrayCell(stopPositions, targetBundle, ArrayField.stopPositions);
        _directCallees = copyObjectIntoArrayCell(directCallees, targetBundle, ArrayField.directCallees);
        _referenceMaps = copyObjectIntoArrayCell(referenceMaps, targetBundle, ArrayField.referenceMaps);
        _scalarLiteralBytes = copyObjectIntoArrayCell(scalarLiteralBytes, targetBundle, ArrayField.scalarLiteralBytes);
        _referenceLiterals = copyObjectIntoArrayCell(referenceLiterals, targetBundle, ArrayField.referenceLiterals);

        // now copy the code (or a code buffer) into the cell for the byte[]
        if (codeOrCodeBuffer instanceof byte[]) {
            _code = copyObjectIntoArrayCell((byte[]) codeOrCodeBuffer, targetBundle, ArrayField.code);
        } else if (codeOrCodeBuffer instanceof CodeBuffer) {
            final CodeBuffer codeBuffer = (CodeBuffer) codeOrCodeBuffer;
            if (MaxineVM.isPrototyping()) {
                _code = new byte[codeBuffer.currentPosition()];
            } else {
                _code = UnsafeLoophole.cast(Cell.plantArray(targetBundle.cell(ArrayField.code), PrimitiveClassActor.BYTE_ARRAY_CLASS_ACTOR.dynamicHub(), codeBuffer.currentPosition()));
            }
            codeBuffer.copyTo(_code);
        } else {
            throw ProgramError.unexpected("byte[] or CodeBuffer required in TargetMethod.setGenerated()");
        }

        // collect metrics about the size of target methods' constituent arrays
        if (MaxineVM.isPrototyping()) {
            for (ArrayField field : ArrayField.VALUES) {
                Metrics.accumulate("TargetMethod." + field, targetBundle.layout().cellSize(field).toInt());
            }
            if (_compressedJavaFrameDescriptors != null) {
                Metrics.accumulate("TargetMethod.compressedFrameDescriptors", HostObjectAccess.getSize(_compressedJavaFrameDescriptors).toInt());
            }
        }
    }

    private boolean checkReferenceMapSize(int[] stopPositions, int numberOfSafepoints, byte[] referenceMaps, int frameReferenceMapSize) {
        return referenceMaps == null || referenceMaps.length > 0 && (referenceMaps.length - (frameReferenceMapSize * stopPositions.length) == numberOfSafepoints * registerReferenceMapSize());
    }

    private <Object_Type> Object_Type copyObjectIntoArrayCell(Object_Type object, TargetBundle targetBundle, ArrayField field) {
        if (MaxineVM.isPrototyping() || object == null) {
            return object;
        }
        return Cell.plantClone(targetBundle.cell(field), object);
    }

    public final boolean isGenerated() {
        return _code != null;
    }

    public final ClassMethodActor callSiteToCallee(Address callSite) {
        final int callOffset = callSite.minus(_codeStart).toInt();
        for (int i = 0; i < numberOfStopPositions(); i++) {
            if (stopPosition(i) == callOffset) {
                return _directCallees[i];
            }
        }
        throw ProgramError.unexpected("could not find callee for call site: " + callSite.toHexString());
    }

    public final Address throwAddressToCatchAddress(Address throwAddress) {
        if (_catchRangePositions != null) {
            final int throwOffset = throwAddress.minus(_codeStart).toInt();
            for (int i = _catchRangePositions.length - 1; i >= 0; i--) {
                if (throwOffset >= _catchRangePositions[i]) {
                    final int catchBlockPosition = _catchBlockPositions[i];
                    if (catchBlockPosition <= 0) {
                        return Address.zero();
                    }
                    return _codeStart.plus(catchBlockPosition);
                }
            }
        }
        return Address.zero();
    }

    /**
     * Traces the metadata of the compiled code represented by this object. In particular, the
     * {@linkplain #traceExceptionHandlers(IndentWriter) exception handlers}, the
     * {@linkplain #traceDirectCallees(IndentWriter) direct callees}, the #{@linkplain #traceScalarBytes(IndentWriter, TargetBundle) scalar data},
     * the {@linkplain #traceReferenceLiterals(IndentWriter, TargetBundle) reference literals} and the address of the
     * array contain {@linkplain #code() compiled code}.
     *
     * @param writer where the trace is written
     */
    public final void traceBundle(IndentWriter writer) {
        final TargetBundle targetBundle = TargetBundle.from(this);
        writer.println("Layout: ");
        writer.println(Strings.indent(targetBundle.layout().toString(), writer.indentation()));
        traceExceptionHandlers(writer);
        traceDirectCallees(writer);
        traceScalarBytes(writer, targetBundle);
        traceReferenceLiterals(writer, targetBundle);
        writer.println("Code cell: " + targetBundle.cell(ArrayField.code).toString());
    }

    /**
     * Traces the exception handlers of the compiled code represented by this object.
     *
     * @param writer where the trace is written
     */
    public final void traceExceptionHandlers(IndentWriter writer) {
        if (_catchRangePositions != null) {
            assert _catchBlockPositions != null;
            writer.println("Catches: ");
            writer.indent();
            for (int i = 0; i < _catchRangePositions.length; i++) {
                if (_catchBlockPositions[i] != 0) {
                    final int catchRangeEnd = (i == _catchRangePositions.length - 1) ? _code.length : _catchRangePositions[i + 1];
                    final int catchRangeStart = _catchRangePositions[i];
                    writer.println("[" + catchRangeStart + " .. " + catchRangeEnd + ") -> " + _catchBlockPositions[i]);
                }
            }
            writer.outdent();
        }
    }

    /**
     * Traces the {@linkplain #directCallees() direct callees} of the compiled code represented by this object.
     *
     * @param writer where the trace is written
     */
    public final void traceDirectCallees(IndentWriter writer) {
        if (_directCallees != null) {
            assert _stopPositions != null && _directCallees.length <= numberOfStopPositions();
            writer.println("Direct Calls: ");
            writer.indent();
            for (int i = 0; i < _directCallees.length; i++) {
                writer.println(stopPosition(i) + " -> " + _directCallees[i]);
            }
            writer.outdent();
        }
    }

    /**
     * Traces the {@linkplain #scalarLiteralBytes() scalar data} addressed by the compiled code represented by this object.
     *
     * @param writer where the trace is written
     */
    public final void traceScalarBytes(IndentWriter writer, final TargetBundle targetBundle) {
        if (_scalarLiteralBytes != null) {
            writer.println("Scalars: ");
            writer.indent();
            for (int i = 0; i < _scalarLiteralBytes.length; i++) {
                final Pointer pointer = targetBundle.cell(ArrayField.scalarLiteralBytes).plus(ArrayField.scalarLiteralBytes.layout().getElementOffsetInCell(i));
                writer.println("[" + pointer.toString() + "] 0x" + Integer.toHexString(_scalarLiteralBytes[i] & 0xFF) + "  " + _scalarLiteralBytes[i]);
            }
            writer.outdent();
        }
    }

    /**
     * Traces the {@linkplain #referenceLiterals() reference literals} addressed by the compiled code represented by this object.
     *
     * @param writer where the trace is written
     */
    public final void traceReferenceLiterals(IndentWriter writer, final TargetBundle targetBundle) {
        if (_referenceLiterals != null) {
            writer.println("References: ");
            writer.indent();
            for (int i = 0; i < _referenceLiterals.length; i++) {
                final Pointer pointer = targetBundle.cell(ArrayField.referenceLiterals).plus(ArrayField.referenceLiterals.layout().getElementOffsetInCell(i));
                writer.println("[" + pointer.toString() + "] " + _referenceLiterals[i]);
            }
            writer.outdent();
        }
    }

    @Override
    public final String toString() {
        return _classMethodActor.name().toString();
    }

    public final void trace(int level) {
        if (Trace.hasLevel(level)) {
            Trace.line(level, this.traceToString());
        }
    }

    public final String traceToString() {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final IndentWriter writer = new IndentWriter(new OutputStreamWriter(byteArrayOutputStream));
        writer.println("target method: " + this);
        traceBundle(writer);
        if (MaxineVM.isPrototyping()) {
            Disassemble.disassemble(byteArrayOutputStream, this);
        }
        return byteArrayOutputStream.toString();
    }

    public Class<? extends IrTraceObserver> irTraceObserverType() {
        return IrTraceObserver.class;
    }

    public final boolean contains(final Builtin builtin, boolean defaultResult) {
        return defaultResult;
    }

    public final int count(final Builtin builtin, int defaultResult) {
        return defaultResult;
    }

    public Word getEntryPoint(CallEntryPoint callEntryPoint) {
        return callEntryPoint.in(this);
    }

    public Pointer homogeneousCallEntryPoint() {
        return abi().callEntryPoint().in(this);
    }

    public abstract void patchCallSite(int callOffset, Word callEntryPoint);

    public abstract void forwardTo(TargetMethod newTargetMethod);

    public final boolean isSafepointAt(Pointer instructionPointer) {
        final int position = instructionPointer.minus(codeStart()).toInt();
        for (int i = 0; i < numberOfSafepoints(); i++) {
            if (StopType.SAFEPOINT.stopPosition(this, i) == position) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return the compiler scheme that produces this target method.
     * @return
     */
    public DynamicCompilerScheme compilerScheme() {
        return MaxineVM.hostOrTarget().configuration().compilerScheme();
    }

    public final Pointer[] referenceLiteralPointers() {
        final Object[] literals = referenceLiterals();
        if (literals == null) {
            return new Pointer[0];
        }
        final Pointer[] pointers = new Pointer[literals.length];
        final TargetBundle targetBundle = TargetBundle.from(this);
        for (int i = 0; i < literals.length; i++) {
            WordArray.set(pointers, i, targetBundle.cell(ArrayField.referenceLiterals).plus(ArrayField.referenceLiterals.layout().getElementOffsetInCell(i)));
        }
        return pointers;
    }

    public final TargetMethod duplicate() {
        final TargetGeneratorScheme targetGeneratorScheme = (TargetGeneratorScheme) compilerScheme();
        final TargetMethod duplicate = targetGeneratorScheme.targetGenerator().createIrMethod(classMethodActor());
        final TargetBundleLayout targetBundleLayout = TargetBundleLayout.from(this);
        duplicate.setSize(targetBundleLayout.bundleSize());
        Code.allocate(duplicate);
        final TargetBundle targetBundle = new TargetBundle(targetBundleLayout, duplicate.start());
        if (MaxineVM.isPrototyping()) {
            // setGenerated below will not create a clone of the elements of the target method when in prototyping mode.
            ClassMethodActor[] duplicatedDirectCallees = directCallees();
            if (duplicatedDirectCallees != null) {
                duplicatedDirectCallees = new ClassMethodActor[duplicatedDirectCallees.length];
                System.arraycopy(directCallees(), 0, duplicatedDirectCallees, 0, duplicatedDirectCallees.length);
            }
            Object[] duplicatedReferenceLiterals = referenceLiterals();
            if (duplicatedReferenceLiterals != null) {
                duplicatedReferenceLiterals = new Object[duplicatedReferenceLiterals.length];
                System.arraycopy(referenceLiterals(), 0, duplicatedReferenceLiterals, 0, duplicatedReferenceLiterals.length);
            }
            duplicate.setGenerated(targetBundle,
                        catchRangePositions() == null ? null : catchRangePositions().clone(),
                        catchBlockPositions() == null ? null : catchBlockPositions().clone(),
                        _stopPositions == null ? null : _stopPositions.clone(),
                        _compressedJavaFrameDescriptors,
                        duplicatedDirectCallees,
                        numberOfIndirectCalls(),
                        numberOfSafepoints(),
                        numberOfGuardpoints(),
                        referenceMaps() == null ? null : referenceMaps().clone(),
                        scalarLiteralBytes() == null ? null : scalarLiteralBytes().clone(),
                        duplicatedReferenceLiterals,
                        code().clone(),
                        _encodedInlineDataDescriptors,
                        frameSize(),
                        frameReferenceMapSize(),
                        abi(),
                        _markerPosition);
        } else {
            duplicate.setGenerated(targetBundle,
                            catchRangePositions(),
                            catchBlockPositions(),
                            _stopPositions,
                            _compressedJavaFrameDescriptors,
                            directCallees(),
                            numberOfIndirectCalls(),
                            numberOfSafepoints(),
                            numberOfGuardpoints(),
                            referenceMaps(),
                            scalarLiteralBytes(),
                            referenceLiterals(),
                            code(),
                            _encodedInlineDataDescriptors,
                            frameSize(),
                            frameReferenceMapSize(),
                            abi(),
                            _markerPosition);
        }
        return duplicate;
    }

    /**
     * Links all the calls from this target method to other methods for which the exact method actor is known. Linking a
     * call means patching the operand of a call instruction that specifies the address of the target code to call. In
     * the case of a callee for which there is no target code available (i.e. it has not yet been compiled or it has
     * been evicted from the code cache), the address of a {@linkplain StaticTrampoline static trampoline} is patched
     * into the call instruction.
     *
     * @return true if target code was available for all the direct callees
     */
    public final boolean linkDirectCalls() {
        boolean linkedAll = true;
        final ClassMethodActor[] directCallees = directCallees();
        if (directCallees != null) {
            for (int i = 0; i < directCallees.length; i++) {
                final CallEntryPoint callEntryPoint = callEntryPointForDirectCall(i);
                final int offset = callEntryPoint.offsetFromCalleeCodeStart();
                final TargetMethod callee = CompilationScheme.Static.getCurrentTargetMethod(directCallees[i]);
                if (callee == null) {
                    linkedAll = false;
                    patchCallSite(stopPosition(i), StaticTrampoline.codeStart().plus(offset));
                } else {
                    patchCallSite(stopPosition(i), callee.codeStart().plus(offset));
                }
            }
        }
        return linkedAll;
    }

    @PROTOTYPE_ONLY
    protected boolean isDirectCalleeInPrologue(int directCalleeIndex) {
        return false;
    }

    /**
     * Links all the direct calls in this target method. Only calls within this target method's prologue are linked to
     * the target callee (if it's
     * {@linkplain CompilationScheme.Static#getCurrentTargetMethod(ClassMethodActor) available}). All other direct
     * calls are linked to a {@linkplain StaticTrampoline static trampoline}.
     *
     * @return true if all the direct callees in this target method's prologue were linked to a resolved target method
     */
    @PROTOTYPE_ONLY
    public final boolean linkDirectCallsInPrologue() {
        boolean linkedAll = true;
        final ClassMethodActor[] directCallees = directCallees();
        if (directCallees != null) {
            for (int i = 0; i < directCallees.length; i++) {
                final CallEntryPoint callEntryPoint = callEntryPointForDirectCall(i);
                final int offset = callEntryPoint.offsetFromCalleeCodeStart();
                final TargetMethod callee = CompilationScheme.Static.getCurrentTargetMethod(directCallees[i]);
                if (!isDirectCalleeInPrologue(i)) {
                    patchCallSite(stopPosition(i), StaticTrampoline.codeStart().plus(offset));
                } else if (callee == null) {
                    linkedAll = false;
                    patchCallSite(stopPosition(i), StaticTrampoline.codeStart().plus(offset));
                } else {
                    patchCallSite(stopPosition(i), callee.codeStart().plus(offset));
                }
            }
        }
        return linkedAll;
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

    public boolean isAtSafepoint(Pointer instructionPointer) {
        return findSafepointIndex(instructionPointer) >= 0;
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
    public int targetCodePositionFor(Pointer instructionPointer) {
        final int targetCodePosition = instructionPointer.minus(_codeStart).toInt();
        if (targetCodePosition >= 0 && targetCodePosition < _code.length) {
            return targetCodePosition;
        }
        return -1;
    }

    /**
     * Gets the index of a stop position within this target method derived from a given instruction pointer. If the
     * instruction pointer is equal to a safepoint position, then the index in {@link #stopPositions()} of that
     * safepoint is returned. Otherwise, the index of the highest stop position that is less than the target code
     * position denoted by the instruction pointer is returned. That is, if {@code instructionPointer} exactly matches a
     * stop position 'pos' for a direct or indirect call, then the index of the highest stop position <b>less than</b>
     * 'pos' is returned.
     *
     * @return -1 if no stop index can be found for {@code instructionPointer}
     * @see #stopPositions()
     */
    public int findClosestStopIndex(Pointer instructionPointer) {
        final int targetCodePosition = targetCodePositionFor(instructionPointer);
        if (_stopPositions == null || targetCodePosition < 0 || targetCodePosition > _code.length) {
            return -1;
        }

        // Direct calls come first, followed by indirect calls and safepoints in the _stopPositions array.
        // For direct and indirect calls, the instruction pointer will point to the instruction immediately
        // after the call instruction (_stopPositions correspond to calls and safepoints).

        // Check for matching safepoints first
        for (int i = numberOfDirectCalls() + numberOfIndirectCalls(); i < numberOfStopPositions(); i++) {
            if (stopPosition(i) == targetCodePosition) {
                return i;
            }
        }

        // Since this is not a safepoint as it seems, it must be a call.
        // In that case, the instruction pointer is already one instruction after the call.
        // We compare with "less than" from now on to ensure that we do not accidentally pick up the next stop instruction, which might be "equal".

        int stopIndexWithClosestPosition = -1;
        for (int i = numberOfDirectCalls() - 1; i >= 0; --i) {
            final int directCallPosition = stopPosition(i);
            if (directCallPosition < targetCodePosition) {
                stopIndexWithClosestPosition = i;
                break;
            }
        }

        // It is not enough that we find the first matching position, since there might be a direct as well as an indirect call before the instruction pointer
        // so we find the closest one. This can be avoided if we sort the _stopPositions array first, but the runtime cost of this is unknown.
        for (int i = numberOfDirectCalls() + numberOfIndirectCalls() - 1; i >= numberOfDirectCalls(); i--) {
            final int indirectCallPosition = stopPosition(i);
            if (indirectCallPosition < targetCodePosition && (stopIndexWithClosestPosition < 0 || indirectCallPosition >  stopPosition(stopIndexWithClosestPosition))) {
                stopIndexWithClosestPosition = i;
                break;
            }
        }

        if (stopIndexWithClosestPosition >= 0) {
            return stopIndexWithClosestPosition;
        }
        return -1;
    }

    /**
     * Gets the index of a stop position within the target method derived from a given guardpoint index.
     */
    public int findGuardpointIndex(int index) {
        return numberOfDirectCalls() + numberOfIndirectCalls() + numberOfSafepoints() + index;
    }

    /**
     *
     * @param stackReferenceMapPreparer
     * @param instructionPointer
     * @param stackPointer
     * @param framePointer
     * @return
     */
    public boolean prepareFrameReferenceMap(StackReferenceMapPreparer stackReferenceMapPreparer, Pointer instructionPointer, Pointer stackPointer, Pointer framePointer) {
        return stackReferenceMapPreparer.prepareFrameReferenceMap(this, instructionPointer, stackPointer, framePointer);
    }
}
