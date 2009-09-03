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
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.ir.observer.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.target.TargetBundleLayout.*;
import com.sun.max.vm.debug.*;
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
public abstract class TargetMethod extends RuntimeMemoryRegion {

    @PROTOTYPE_ONLY
    public static boolean COLLECT_TARGET_METHOD_STATS;

    /**
     * The compiler scheme that produced this target method.
     */
    @INSPECTED
    public final RuntimeCompilerScheme compilerScheme;

    @INSPECTED
    private final ClassMethodActor classMethodActor;

    @INSPECTED
    protected byte[] compressedJavaFrameDescriptors;

    /**
     * If non-null, this array encodes a serialized array of {@link InlineDataDescriptor} objects.
     */
    @INSPECTED
    protected byte[] encodedInlineDataDescriptors;

    /**
     * The stop positions are encoded in the lower 31 bits of each element.
     * The high bit indicates whether the stop is a call that returns a Reference.
     *
     * @see #stopPositions()
     */
    @INSPECTED
    protected int[] stopPositions;

    @INSPECTED
    private Object[] directCallees;

    @INSPECTED
    private int numberOfIndirectCalls;

    @INSPECTED
    private int numberOfSafepoints;

    @INSPECTED
    private byte[] scalarLiterals;

    @INSPECTED
    private Object[] referenceLiterals;

    @INSPECTED
    protected byte[] code;

    @INSPECTED
    protected Pointer codeStart = Pointer.zero();

    private int frameSize;

    private int registerRestoreEpilogueOffset = -1;

    @INSPECTED
    private int frameReferenceMapSize;

    private byte[] referenceMaps;

    @INSPECTED
    private TargetABI abi;

    public TargetMethod(String description, RuntimeCompilerScheme compilerScheme) {
        this((ClassMethodActor) null, compilerScheme);
    }

    public TargetMethod(ClassMethodActor classMethodActor, RuntimeCompilerScheme compilerScheme) {
        this.classMethodActor = classMethodActor;
        this.compilerScheme = compilerScheme;
        setDescription("Target-" + name());
    }

    public int registerRestoreEpilogueOffset() {
        return registerRestoreEpilogueOffset;
    }

    protected void setRegisterRestoreEpilogueOffset(int x) {
        registerRestoreEpilogueOffset = x;
    }

    public final ClassMethodActor classMethodActor() {
        return classMethodActor;
    }

    public abstract InstructionSet instructionSet();

    public final String name() {
        return (classMethodActor == null) ? description() : classMethodActor.name.toString();
    }

    public final boolean isNative() {
        return (classMethodActor == null) ? false : classMethodActor.compilee().isNative();
    }

    public final void cleanup() {
    }


    /**
     * Gets the array recording the positions of the {@link StopType stops} in this target method.
     * <p>
     * This array is composed of three contiguous segments. The first segment contains the positions of the direct call
     * stops and the indexes in this segment match the entries of the {@link #directCallees} array). The second segment
     * and third segments contain the positions of the register indirect call and safepoint stops.
     * <p>
     *
     * <pre>
     *   +-----------------------------+-------------------------------+----------------------------+
     *   |          direct calls       |           indirect calls      |          safepoints        |
     *   +-----------------------------+-------------------------------+----------------------------+
     *    <-- numberOfDirectCalls() --> <-- numberOfIndirectCalls() --> <-- numberOfSafepoints() -->
     *
     * </pre>
     * The methods and constants defined in {@link StopPositions} should be used to decode the entries of this array.
     *
     * @see StopType
     * @see StopPositions
     */
    public final int[] stopPositions() {
        return stopPositions;
    }

    public final int numberOfStopPositions() {
        return stopPositions == null ? 0 : stopPositions.length;
    }

    /**
     * Gets the position of a given stop in this target method.
     *
     * @param stopIndex an index into the {@link #stopPositions()} array
     * @return
     */
    public final int stopPosition(int stopIndex) {
        return StopPositions.get(stopPositions, stopIndex);
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
     * Gets the number of leading bytes in {@link #referenceMaps()} encoding the frame reference maps for each {@linkplain #stopPositions() stop position}.
     * The remainder of the bytes in {@link #referenceMaps()} encodes the register reference maps for the safepoints.
     */
    public int frameReferenceMapsSize() {
        return frameReferenceMapSize * numberOfStopPositions();
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
     * Gets the {@linkplain InlineDataDescriptor inline data descriptors} associated with this target method's code
     * encoded as a byte array in the format described {@linkplain InlineDataDescriptor here}.
     *
     * @return null if there are no inline data descriptors associated with this target method's code
     */
    public byte[] encodedInlineDataDescriptors() {
        return encodedInlineDataDescriptors;
    }

    /**
     * @see TargetJavaFrameDescriptor#compress(com.sun.max.collect.Sequence)
     */
    public final byte[] compressedJavaFrameDescriptors() {
        return compressedJavaFrameDescriptors;
    }

    /**
     * Gets the frame descriptor corresponding to a given stop index.
     *
     * @param stopIndex a value between 0 and {@link #numberOfStopPositions()}
     * @return the frame descriptor at {@code stopIndex} or null if there is no frame descriptors in this target method
     */
    public TargetJavaFrameDescriptor getJavaFrameDescriptor(int stopIndex) {
        return TargetJavaFrameDescriptor.get(this, stopIndex);
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
    public Iterator<? extends BytecodeLocation> getBytecodeLocationsFor(Pointer instructionPointer) {
        final TargetJavaFrameDescriptor targetFrameDescriptor = getJavaFrameDescriptorFor(instructionPointer);
        if (targetFrameDescriptor != null) {
            return targetFrameDescriptor.inlinedFrames();
        }
        return null;
    }

    /**
     * Gets the bytecode location for a given instruction pointer.
     *
     * @param instructionPointer a pointer to an instruction within this method
     * @return the bytecode location {@code instructionPointer}. This will be null if no bytecode location can be
     *         determined for {@code instructionPointer}.
     */
    public BytecodeLocation getBytecodeLocationFor(Pointer instructionPointer) {
        final Iterator<? extends BytecodeLocation> bytecodeLocationsFor = getBytecodeLocationsFor(instructionPointer);
        if (bytecodeLocationsFor != null && bytecodeLocationsFor.hasNext()) {
            return bytecodeLocationsFor.next();
        }
        return null;
    }

    /**
     * Gets the frame descriptor at or before a given instruction pointer. The returned frame descriptor is the one at
     * the closest position less or equal to the position denoted by {@code instructionPointer}.
     *
     * @param instructionPointer a pointer to an instruction within this method
     * @return the frame descriptor closest to the position denoted by {@code instructionPointer} or null if not frame
     *         descriptor can be determined for {@code instructionPointer}
     */
    public TargetJavaFrameDescriptor getJavaFrameDescriptorFor(Pointer instructionPointer) {
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
        return getJavaFrameDescriptor(resultIndex);
    }

    public final int numberOfDirectCalls() {
        return (directCallees == null) ? 0 : directCallees.length;
    }

    /**
     * @return class method actors referenced by direct call instructions, matched to the stop positions array above by array index
     */
    public final Object[] directCallees() {
        return directCallees;
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

    public final int numberOfIndirectCalls() {
        return numberOfIndirectCalls;
    }

    public final int numberOfSafepoints() {
        return numberOfSafepoints;
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
     * @return non-object data referenced by the machine code
     */
    public final byte[] scalarLiterals() {
        return scalarLiterals;
    }

    public final int numberOfScalarLiteralBytes() {
        return (scalarLiterals == null) ? 0 : scalarLiterals.length;
    }

    /**
     * @return object references referenced by the machine code
     */
    public final Object[] referenceLiterals() {
        return referenceLiterals;
    }

    public final int numberOfReferenceLiterals() {
        return (referenceLiterals == null) ? 0 : referenceLiterals.length;
    }

    /**
     * Gets the byte array containing the target-specific machine code of this target method.
     */
    public final byte[] code() {
        return code;
    }

    public final int codeLength() {
        return (code == null) ? 0 : code.length;
    }

    /**
     * Gets the address of the first instruction in this target method's {@linkplain #code() compiled code array}.
     * <p>
     * Needs {@linkplain DataPrototype#assignRelocationFlags() relocation}.
     */
    public final Pointer codeStart() {
        return codeStart;
    }

    @PROTOTYPE_ONLY
    public final void setCodeStart(Pointer codeStart) {
        this.codeStart = codeStart;
    }

    /**
     * Gets the size (in bytes) of the stack frame used for the local variables in
     * the method represented by this object. The stack pointer is decremented
     * by this amount when entering a method and is correspondingly incremented
     * by this amount when exiting a method.
     */
    public final int frameSize() {
        return frameSize;
    }

    /**
     * Gets the size of a single frame reference map encoded in this method's {@linkplain #referenceMaps() reference maps}.
     */
    public final int frameReferenceMapSize() {
        return frameReferenceMapSize;
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
     * Gets an object describing the layout of an activation frame created on the stack for a call to this target method.
     */
    public abstract JavaStackFrameLayout stackFrameLayout();

    public final TargetABI abi() {
        return abi;
    }

    /**
     * Gets the size of the reference map covering the registers.
     */
    public abstract int registerReferenceMapSize();

    /**
     * Assigns the arrays co-located in a {@linkplain CodeRegion code region} containing the machine code and related data.
     *
     * @param code the code
     * @param codeStart the address of the first element of {@code code}
     * @param scalarLiterals the scalar data referenced from {@code code}
     * @param referenceLiterals the reference data referenced from {@code code}
     */
    public final void setCodeArrays(byte[] code, Pointer codeStart, byte[] scalarLiterals, Object[] referenceLiterals) {
        this.scalarLiterals = scalarLiterals;
        this.referenceLiterals = referenceLiterals;
        this.code = code;
        this.codeStart = codeStart;
    }

    /**
     * Completes the definition of this target method as the result of compilation.
     *
     * @param catchRangePositions describes the {@linkplain #catchRangePositions() code ranges} covered by exception
     *            dispatchers
     * @param catchBlockPositions the positions of the {@linkplain #catchBlockPositions() exception dispatchers}
     * @param stopPositions the positions in this target method at which the locations of object references are
     *            precisely known
     * @param directCallees the positions in this target method of direct calls (e.g. calls to methods for which a
     *            compiled version available)
     * @param numberOfIndirectCalls the positions in this target method of register indirect calls (e.g. late binding
     *            calls, virtual/interface calls)
     * @param numberOfSafepoints the number of {@linkplain com.sun.max.vm.runtime.Safepoint safepoint} positions in this
     *            target method
     * @param referenceMaps the set of bits maps, one per stop position, describing the locations of object references.
     *            The format requirements of this data structure are explained {@linkplain #referenceMaps() here}.
     * @param scalarLiterals a byte array encoding the scalar data accessed by this target via code relative offsets
     * @param referenceLiterals an object array encoding the object references accessed by this target via code relative
     *            offsets
     * @param codeOrCodeBuffer the compiled code, either as a byte array, or as a {@code CodeBuffer} object
     * @param frameSize the amount of stack allocated for an activation frame during a call to this target method
     * @param abi the target ABI
     *
     * TODO: move ABI initialization to constructor
     */
    protected final void setGenerated(int[] stopPositions,
                                   byte[] compressedJavaFrameDescriptors,
                                   Object[] directCallees,
                                   int numberOfIndirectCalls,
                                   int numberOfSafepoints,
                                   byte[] referenceMaps,
                                   byte[] scalarLiterals,
                                   Object[] referenceLiterals,
                                   Object codeOrCodeBuffer,
                                   byte[] encodedInlineDataDescriptors,
                                   int frameSize,
                                   int frameReferenceMapSize,
                                   TargetABI abi) {

        assert !codeStart.isZero() : "Must call setCodeArrays() first";

        this.frameSize = frameSize;
        this.frameReferenceMapSize = frameReferenceMapSize;
        this.numberOfIndirectCalls = numberOfIndirectCalls;
        this.numberOfSafepoints = numberOfSafepoints;
        this.abi = abi;
        this.compressedJavaFrameDescriptors = compressedJavaFrameDescriptors;
        this.encodedInlineDataDescriptors = encodedInlineDataDescriptors;

        // TODO: (tw) reenable the assertion
//        assert checkReferenceMapSize(stopPositions, numberOfSafepoints, referenceMaps, frameReferenceMapSize);

        // copy the arrays into the target bundle
        this.stopPositions = stopPositions;
        this.directCallees = directCallees;
        this.referenceMaps = referenceMaps;
        if (scalarLiterals != null) {
            assert scalarLiterals.length != 0;
            System.arraycopy(scalarLiterals, 0, this.scalarLiterals, 0, this.scalarLiterals.length);
        } else {
            assert this.scalarLiterals == null;
        }
        if (referenceLiterals != null && referenceLiterals.length > 0) {
            System.arraycopy(referenceLiterals, 0, this.referenceLiterals, 0, this.referenceLiterals.length);
        }

        // now copy the code (or a code buffer) into the cell for the byte[]
        if (codeOrCodeBuffer instanceof byte[]) {
            System.arraycopy(codeOrCodeBuffer, 0, code, 0, code.length);
        } else if (codeOrCodeBuffer instanceof CodeBuffer) {
            final CodeBuffer codeBuffer = (CodeBuffer) codeOrCodeBuffer;
            codeBuffer.copyTo(code);
        } else {
            throw ProgramError.unexpected("byte[] or CodeBuffer required in TargetMethod.setGenerated()");
        }
    }

    private boolean checkReferenceMapSize(int[] stopPositions, int numberOfSafepoints, byte[] referenceMaps, int frameReferenceMapSize) {
        return referenceMaps == null || referenceMaps.length > 0 && (referenceMaps.length - (frameReferenceMapSize * stopPositions.length) == numberOfSafepoints * registerReferenceMapSize());
    }

    public final boolean isGenerated() {
        return code != null;
    }

    public final ClassMethodActor callSiteToCallee(Address callSite) {
        final int callOffset = callSite.minus(codeStart).toInt();
        for (int i = 0; i < numberOfStopPositions(); i++) {
            if (stopPosition(i) == callOffset && directCallees[i] instanceof ClassMethodActor) {
                return (ClassMethodActor) directCallees[i];
            }
        }
        throw FatalError.unexpected("could not find callee for call site: " + callSite.toHexString());
    }

    public abstract Address throwAddressToCatchAddress(boolean isTopFrame, Address throwAddress, Class<? extends Throwable> throwableClass);

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

    public abstract void traceExceptionHandlers(IndentWriter writer);

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
     * Traces the {@linkplain #referenceLiterals() reference literals} addressed by the compiled code represented by this object.
     *
     * @param writer where the trace is written
     */
    public final void traceReferenceLiterals(IndentWriter writer, final TargetBundleLayout targetBundleLayout) {
        if (referenceLiterals != null) {
            writer.println("References: ");
            writer.indent();
            for (int i = 0; i < referenceLiterals.length; i++) {
                final Pointer pointer = targetBundleLayout.cell(start(), ArrayField.referenceLiterals).plus(ArrayField.referenceLiterals.arrayLayout.getElementOffsetInCell(i));
                writer.println("[" + pointer.toString() + "] " + referenceLiterals[i]);
            }
            writer.outdent();
        }
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

    @Override
    public final String toString() {
        return (classMethodActor == null) ? description() : classMethodActor.format("%H.%n(%p)");
    }

    public final void trace(int level) {
        if (Trace.hasLevel(level)) {
            Trace.line(level, traceToString());
        }
    }

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

    public Class<? extends IrTraceObserver> irTraceObserverType() {
        return IrTraceObserver.class;
    }

    public Word getEntryPoint(CallEntryPoint callEntryPoint) {
        return callEntryPoint.in(this);
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
        final Object[] directCallees = directCallees();
        if (directCallees != null) {
            for (int i = 0; i < directCallees.length; i++) {
                final int offset = getCallEntryOffset(directCallees[i], i);
                Object currentDirectCallee = directCallees[i];
                final TargetMethod callee = getTargetMethod(currentDirectCallee);
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

    private TargetMethod getTargetMethod(Object o) {
        TargetMethod result = null;
        if (o instanceof ClassMethodActor) {
            result = CompilationScheme.Static.getCurrentTargetMethod((ClassMethodActor) o);
        } else if (o instanceof TargetMethod) {
            result = (TargetMethod) o;
        }
        return result;
    }

    private int getCallEntryOffset(Object callee, int index) {
        final CallEntryPoint callEntryPoint = callEntryPointForDirectCall(index);
        return callEntryPoint.offsetFromCalleeCodeStart();
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
        final Object[] directCallees = directCallees();
        if (directCallees != null) {
            for (int i = 0; i < directCallees.length; i++) {
                final int offset = getCallEntryOffset(directCallees[i], i);
                final TargetMethod callee = getTargetMethod(directCallees[i]);
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

    public boolean prepareFrameReferenceMap(StackReferenceMapPreparer stackReferenceMapPreparer, Pointer instructionPointer, Pointer refmapFramePointer, Pointer operandStackPointer, int offsetToFirstParameter) {
        return stackReferenceMapPreparer.prepareFrameReferenceMap(this, instructionPointer, refmapFramePointer, operandStackPointer, offsetToFirstParameter);
    }
}
