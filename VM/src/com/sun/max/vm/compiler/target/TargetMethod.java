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

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.io.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.target.TargetBundleLayout.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
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

    /**
     * The compiler scheme that produced this target method.
     */
    @INSPECTED
    public final RuntimeCompilerScheme compilerScheme;

    @INSPECTED
    private final ClassMethodActor classMethodActor;

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
    private Object[] referenceLiterals;

    @INSPECTED
    protected byte[] code;

    @INSPECTED
    protected Pointer codeStart = Pointer.zero();

    private int frameSize = -1;

    private int registerRestoreEpilogueOffset = -1;

    @INSPECTED
    private TargetABI abi;

    public TargetMethod(String description, RuntimeCompilerScheme compilerScheme, TargetABI abi) {
        this.compilerScheme = compilerScheme;
        this.classMethodActor = null;
        this.abi = abi;
        setDescription(description);
    }

    public TargetMethod(ClassMethodActor classMethodActor, RuntimeCompilerScheme compilerScheme, TargetABI abi) {
        this.classMethodActor = classMethodActor;
        this.compilerScheme = compilerScheme;
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
        return abi().callEntryPoint();
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

        // copy the arrays into the target bundle
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

    public Word getEntryPoint(CallEntryPoint callEntryPoint) {
        return callEntryPoint.in(this);
    }

    public abstract void patchCallSite(int callOffset, Word callEntryPoint);

    public abstract void forwardTo(TargetMethod newTargetMethod);

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

    @PROTOTYPE_ONLY
    protected boolean isDirectCalleeInPrologue(int directCalleeIndex) {
        return false;
    }

    public boolean isCalleeSaved() {
        return false;
    }

    public void prepareReferenceMap(TargetMethod caller, Pointer callerInstructionPointer, boolean isTopFrame, Pointer instructionPointer, Pointer stackPointer, Pointer framePointer, ReferenceMapCallback result) {

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
     * Analyzes the target method that this compiler produced to build a call graph. This method appends the direct
     * calls (i.e. static and special calls), the virtual calls, and the interface calls to the appendable sequences
     * supplied.
     *
     * @param directCalls a sequence of the direct calls to which this method should append
     * @param virtualCalls a sequence of virtual calls to which this method should append
     * @param interfaceCalls a sequence of interface calls to which this method should append
     */
    @PROTOTYPE_ONLY
    public abstract void gatherCalls(AppendableSequence<MethodActor> directCalls, AppendableSequence<MethodActor> virtualCalls, AppendableSequence<MethodActor> interfaceCalls);

    @Override
    public final String toString() {
        return (classMethodActor == null) ? description() : classMethodActor.format("%H.%n(%p)");
    }


    public void prepareRegisterReferenceMap(Pointer registerState, Pointer instructionPointer, StackReferenceMapPreparer preparer) {

    }

    protected final void setABI(TargetABI abi) {
        this.abi = abi;
    }
}
