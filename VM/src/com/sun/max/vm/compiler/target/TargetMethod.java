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
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.collect.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.target.TargetBundleLayout.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.*;
import com.sun.max.vm.template.*;

/**
 * A collection of objects that represent the compiled target code
 * and its auxiliary data structures for a Java method.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Thomas Wuerthinger
 */
public abstract class TargetMethod extends RuntimeMemoryRegion {

    public static final VMStringOption printTargetMethods = VMOptions.register(new VMStringOption("-XX:PrintTargetMethods=", false, null,
        "Print compiled target methods whose fully qualified name matches <value>."), MaxineVM.Phase.STARTING);

    @INSPECTED
    public final ClassMethodActor classMethodActor;

    /**
     * The stop positions are encoded in the lower 31 bits of each element.
     * The high bit indicates whether the stop is a call that returns a Reference.
     *
     * @see #stopPositions()
     */
    @INSPECTED
    protected int[] stopPositions;

    @INSPECTED
    protected Object[] directCallees;

    @INSPECTED
    private int numberOfIndirectCalls;

    @INSPECTED
    private int numberOfSafepoints;

    @INSPECTED
    protected byte[] scalarLiterals;

    @INSPECTED
    protected Object[] referenceLiterals;

    @INSPECTED
    protected byte[] code;

    @INSPECTED
    protected Pointer codeStart = Pointer.zero();

    private int frameSize = -1;

    private int registerRestoreEpilogueOffset = -1;

    @INSPECTED
    private TargetABI abi;

    public TargetMethod(String description, TargetABI abi) {
        this.classMethodActor = null;
        this.abi = abi;
        setDescription(description);
    }

    public TargetMethod(ClassMethodActor classMethodActor, TargetABI abi) {
        this.classMethodActor = classMethodActor;
        this.abi = abi;
        setDescription(classMethodActor.name.toString());
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

    public abstract byte[] referenceMaps();

    /**
     * Gets the bytecode locations for the inlining chain rooted at a given instruction pointer. The first bytecode
     * location in the returned sequence is the one at the closest position less or equal to the position denoted by
     * {@code instructionPointer}.
     *
     * @param instructionPointer a pointer to an instruction within this method
     * @param implicitExceptionPoint {@code true} if the instruction pointer corresponds to an implicit exception point
     * @return the bytecode locations for the inlining chain rooted at {@code instructionPointer}. This will be null if
     *         no bytecode location can be determined for {@code instructionPointer}.
     */
    public BytecodeLocation getBytecodeLocationFor(Pointer instructionPointer, boolean implicitExceptionPoint) {
        return null;
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
        return abi().callEntryPoint;
    }

    public final int numberOfIndirectCalls() {
        return numberOfIndirectCalls;
    }

    public final int numberOfSafepoints() {
        return numberOfSafepoints;
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

    @HOSTED_ONLY
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
        assert frameSize != -1 : "frame size not yet initialized";
        return frameSize;
    }

    public final TargetABI abi() {
        return abi;
    }

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

    protected final void setStopPositions(int[] stopPositions, Object[] directCallees, int numberOfIndirectCalls, int numberOfSafepoints) {
        this.stopPositions = stopPositions;
        this.directCallees = directCallees;
        this.numberOfIndirectCalls = numberOfIndirectCalls;
        this.numberOfSafepoints = numberOfSafepoints;
    }

    protected final void setFrameSize(int frameSize) {
        assert frameSize != -1 : "invalid frame size!";
        this.frameSize = frameSize;
    }

    /**
     * Completes the definition of this target method as the result of compilation.
     *
     * @param scalarLiterals a byte array encoding the scalar data accessed by this target via code relative offsets
     * @param referenceLiterals an object array encoding the object references accessed by this target via code relative
     *            offsets
     * @param codeOrCodeBuffer the compiled code, either as a byte array, or as a {@code CodeBuffer} object
     */
    protected final void setData(byte[] scalarLiterals, Object[] referenceLiterals, Object codeOrCodeBuffer) {

        assert !codeStart.isZero() : "Must call setCodeArrays() first";

        // Copy scalar literals
        if (scalarLiterals != null && scalarLiterals.length > 0) {
            assert scalarLiterals.length != 0;
            System.arraycopy(scalarLiterals, 0, this.scalarLiterals, 0, this.scalarLiterals.length);
        }

        // Copy reference literals
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

    public final ClassMethodActor callSiteToCallee(Address callSite) {
        final int callOffset = callSite.minus(codeStart).toInt();
        for (int i = 0; i < numberOfStopPositions(); i++) {
            if (stopPosition(i) == callOffset && directCallees[i] instanceof ClassMethodActor) {
                return (ClassMethodActor) directCallees[i];
            }
        }
        throw FatalError.unexpected("could not find callee for call site: " + callSite.toHexString());
    }

    public Word getEntryPoint(CallEntryPoint callEntryPoint) {
        return callEntryPoint.in(this);
    }

    /**
     * Links all the calls from this target method to other methods for which the exact method actor is known. Linking a
     * call means patching the operand of a call instruction that specifies the address of the target code to call. In
     * the case of a callee for which there is no target code available (i.e. it has not yet been compiled or it has
     * been evicted from the code cache), the address of a {@linkplain StaticTrampoline static trampoline} is patched
     * into the call instruction.
     *
     * @param adapter the adapter called by the prologue of this method. This will be {@code null} if this method does
     *            not have an adapter prologue.
     * @return true if target code was available for all the direct callees
     */
    public final boolean linkDirectCalls(Adapter adapter) {
        boolean linkedAll = true;
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

        if (adapter != null) {
            adapter.generator.linkAdapterCallInPrologue(this, adapter);
        }
        return linkedAll;
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
        return callEntryPoint.offsetInCallee();
    }

    /**
     * Links all the direct calls in this target method. Only calls within this target method's prologue are linked to
     * the target callee (if it's
     * {@linkplain CompilationScheme.Static#getCurrentTargetMethod(ClassMethodActor) available}). All other direct
     * calls are linked to a {@linkplain StaticTrampoline static trampoline}.
     *
     * @return true if all the direct callees in this target method's prologue were linked to a resolved target method
     */
    @HOSTED_ONLY
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

    @HOSTED_ONLY
    protected boolean isDirectCalleeInPrologue(int directCalleeIndex) {
        return false;
    }

    public boolean isCalleeSaved() {
        return registerRestoreEpilogueOffset >= 0;
    }

    public byte[] encodedInlineDataDescriptors() {
        return null;
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
    public int findClosestStopIndex(Pointer instructionPointer) {
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
        if (Platform.target().processorKind.instructionSet.offsetToReturnPC == 0) {
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

    @Override
    public final String toString() {
        return (classMethodActor == null) ? description() : classMethodActor.format("%H.%n(%p)");
    }

    protected final void setABI(TargetABI abi) {
        this.abi = abi;
    }

    public void cleanup() {
    }

    public boolean contains(Builtin builtin, boolean defaultResult) {
        return false;
    }

    public int count(Builtin builtin, int defaultResult) {
        return 0;
    }

    public boolean isGenerated() {
        return false;
    }

    public boolean isNative() {
        return false;
    }

    public String name() {
        return description();
    }

    public final String traceToString() {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final IndentWriter writer = new IndentWriter(new OutputStreamWriter(byteArrayOutputStream));
        writer.println("target method: " + this);
        traceBundle(writer);
        writer.flush();
        if (MaxineVM.isHosted()) {
            Disassemble.disassemble(byteArrayOutputStream, this);
        }
        return byteArrayOutputStream.toString();
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
        traceDebugInfo(writer);
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

    public boolean isTrapStub() {
        return classMethodActor != null && classMethodActor.isTrapStub();
    }

    public final boolean isTrampoline() {
        return classMethodActor != null && classMethodActor.isTrampoline();
    }

    /**
     * Analyzes the target method that this compiler produced to build a call graph. This method appends the direct
     * calls (i.e. static and special calls), the virtual calls, and the interface calls to the appendable sequences
     * supplied.
     *
     * @param directCalls a sequence of the direct calls to which this method should append
     * @param virtualCalls a sequence of virtual calls to which this method should append
     * @param interfaceCalls a sequence of interface calls to which this method should append
     */
    @HOSTED_ONLY
    public abstract void gatherCalls(Set<MethodActor> directCalls, Set<MethodActor> virtualCalls, Set<MethodActor> interfaceCalls);

    public abstract Address throwAddressToCatchAddress(boolean isTopFrame, Address throwAddress, Class<? extends Throwable> throwableClass);

    public abstract void patchCallSite(int callOffset, Word callEntryPoint);

    public abstract void forwardTo(TargetMethod newTargetMethod);

    /**
     * Traces the debug info for the compiled code represented by this object.
     * @param writer where the trace is written
     */
    public abstract void traceDebugInfo(IndentWriter writer);

    /**
     * @param writer where the trace is written
     */
    public abstract void traceExceptionHandlers(IndentWriter writer);

    /**
     * Gets a string representation of the reference map for each stop in this target method.
     * @return a string representation of the reference map
     */
    public abstract String referenceMapsToString();

    public ByteArrayBitMap registerReferenceMapFor(int index) {
        throw FatalError.unimplemented();
    }

    public ByteArrayBitMap frameReferenceMapFor(StopType type, int index) {
        throw FatalError.unimplemented();
    }

    /**
     * Determines if this method was compiled with the template JIT compiler.
     */
    public boolean isJitCompiled() {
        return false;
    }

    /**
     * Gets an object describing the layout of an activation frame created on the stack for a call to this target method.
     * @return an object that represents the layout of this stack frame
     */
    public CompiledStackFrameLayout stackFrameLayout() {
        throw FatalError.unimplemented();
    }

    /**
     * Gets the bytecode position for a machine code call site address.
     *
     * @param returnInstructionPointer an instruction pointer that denotes a call site in this target method. The pointer
     *        is passed as was written to the platform-specific link register.  E.g. on SPARC, the instructionPointer is
     *        the PC of the call itself.  On AMD64, the instructionPointer is the PC of the instruction following the call.
     * @return the start position of the bytecode instruction that is implemented at the instruction pointer or -1 if
     *         {@code instructionPointer} denotes an instruction that does not correlate to any bytecode. This will be
     *         the case when {@code instructionPointer} is not in this target method or is in the adapter frame stub
     *         code, prologue or epilogue.
     */
    public int bytecodePositionForCallSite(Pointer returnInstructionPointer) {
        throw FatalError.unimplemented();
    }

    /**
     * Creates an duplicate of this target method.
     * @return a new instance of this target method
     */
    public TargetMethod duplicate() {
        throw FatalError.unimplemented();
    }

    /**
     * Prepares the reference map for the current frame (and potentially for registers stored in a callee frame).
     *
     * @param current the current stack frame
     * @param callee the callee stack frame (ignoring any interposing {@linkplain Adapter adapter} frame)
     * @param preparer the reference map preparer which receives the reference map
     */
    public abstract void prepareReferenceMap(Cursor current, Cursor callee, StackReferenceMapPreparer preparer);

    /**
     * Attempts to catch an exception thrown by this method or a callee method. This method should not return
     * if this method catches the exception, but instead should unwind the stack and resume execution at the handler.
     * @param current the current stack frame
     * @param callee the callee stack frame (ignoring any interposing {@linkplain Adapter adapter} frame)
     * @param throwable the exception thrown
     */
    public abstract void catchException(Cursor current, Cursor callee, Throwable throwable);

    /**
     * Accepts a visitor for this stack frame.
     * @param current the current stack frame
     * @param visitor the visitor which will visit the frame
     * @return {@code true} if the visitor indicates the stack walk should continue
     */
    public abstract boolean acceptStackFrameVisitor(Cursor current, StackFrameVisitor visitor);

    /**
     * Advances the stack frame cursor from this frame to the next frame.
     * @param current the current stack frame cursor
     */
    public abstract void advance(Cursor current);
}
