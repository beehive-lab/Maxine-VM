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
package com.sun.max.vm.compiler.target;

import static com.sun.cri.bytecode.Bytecodes.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.compiler.target.TargetMethod.Flavor.*;

import java.io.*;
import java.util.*;

import com.sun.cri.bytecode.*;
import com.sun.cri.bytecode.Bytes;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.asm.*;
import com.sun.max.asm.dis.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.platform.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.TargetBundleLayout.ArrayField;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.profile.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.Cursor;

/**
 * A collection of objects that represent the compiled target code
 * and its auxiliary data structures for a Java method.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Thomas Wuerthinger
 */
public abstract class TargetMethod extends MemoryRegion {

    protected static String PrintTargetMethods;
    static {
        VMOptions.addFieldOption("-XX:", "PrintTargetMethods", "Print compiled target methods whose fully qualified name contains <value>.");
    }

    static final boolean COMPILED = true;

    /**
     * Categorization of target methods.
     */
    public enum Flavor {

        /**
         * A compiled method.
         */
        Standard(COMPILED),

        /**
         * A piece of compiled machine code representing the implementation of a single bytecode instruction.
         */
        BytecodeTemplate(COMPILED),

        /**
         * Trampoline for virtual method dispatch (i.e. translation of {@link Bytecodes#INVOKEVIRTUAL}).
         */
        VirtualTrampoline(!COMPILED),

        /**
         * Trampoline for interface method dispatch (i.e. translation of {@link Bytecodes#INVOKEINTERFACE}).
         */
        InterfaceTrampoline(!COMPILED),

        /**
         * Trampoline for static method call (i.e. translation of {@link Bytecodes#INVOKESPECIAL} or {@link Bytecodes#INVOKESTATIC}).
         */
        StaticTrampoline(!COMPILED),

        /**
         * A {@linkplain com.sun.c1x.globalstub.GlobalStub global stub}.
         */
        GlobalStub(!COMPILED),

        /**
         * An {@linkplain Adapter adapter}.
         */
        Adapter(!COMPILED),

        /**
         * The trap stub.
         */
        TrapStub(!COMPILED);

        /**
         * Determines if a target method of this flavor represents compiled code.
         * A target method has a non-null {@link TargetMethod#classMethodActor})
         * iff is it compiled.
         */
        public final boolean compiled;

        Flavor(boolean compiled) {
            this.compiled = compiled;
        }
    }

    public final Flavor flavor;

    /**
     * Determines if this method is of a given flavor.
     */
    public final boolean is(Flavor flavor) {
        return this.flavor == flavor;
    }

    /**
     * Determines if this target method represents compiled code.
     * A target method has a non-null {@link TargetMethod#classMethodActor})
     * iff is it compiled.
     */
    public final boolean isCompiled() {
        return flavor.compiled;
    }

    /**
     * Determines if this method is a trampoline.
     */
    public final boolean isTrampoline() {
        return this.flavor == VirtualTrampoline || this.flavor == InterfaceTrampoline || flavor == StaticTrampoline;
    }

    @INSPECTED
    public final ClassMethodActor classMethodActor;

    /**
     * The stop positions are encoded in the lower 31 bits of each element.
     *
     * @see #stopPositions()
     * @see StopPositions
     */
    protected int[] stopPositions;

    protected Object[] directCallees;

    private int numberOfIndirectCalls;

    private int numberOfSafepoints;

    protected byte[] scalarLiterals;

    @INSPECTED
    protected Object[] referenceLiterals;

    @INSPECTED
    protected byte[] code;

    @INSPECTED
    protected Pointer codeStart = Pointer.zero();

    private int frameSize = -1;

    private int registerRestoreEpilogueOffset = -1;

    public TargetMethod(Flavor flavor, String description, CallEntryPoint callEntryPoint) {
        this.classMethodActor = null;
        this.callEntryPoint = callEntryPoint;
        this.flavor = flavor;
        setRegionName(description);
        assert isCompiled() == (classMethodActor != null);
    }

    public TargetMethod(ClassMethodActor classMethodActor, CallEntryPoint callEntryPoint) {
        this.classMethodActor = classMethodActor;
        this.callEntryPoint = callEntryPoint;
        setRegionName(classMethodActor.name.toString());

        if (classMethodActor.isTemplate()) {
            flavor = BytecodeTemplate;
        } else {
            flavor = Standard;
        }
        assert isCompiled() == (classMethodActor != null);
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
     * {@code ip}.
     *
     * @param targetMethod the target method to process
     * @param ip a pointer to an instruction within this method
     * @param ipIsReturnAddress
     * @return the bytecode locations for the inlining chain rooted at {@code ip}. This will be null if
     *         no bytecode location can be determined for {@code ip}.
     */
    public static CiCodePos getCodePos(TargetMethod targetMethod, Pointer ip, boolean ipIsReturnAddress) {
        class Caller {
            final ClassMethodActor method;
            final int bci;
            final Caller next;
            Caller(ClassMethodActor method, int bci, Caller next) {
                this.method = method;
                this.bci = bci;
                this.next = next;
            }
            CiCodePos toCiCodePos(CiCodePos caller) {
                CiCodePos pos = new CiCodePos(caller, method, bci);
                if (next != null) {
                    return next.toCiCodePos(pos);
                }
                return pos;
            }
        }
        final Caller[] head = {null};
        CodePosClosure cpc = new CodePosClosure() {
            public boolean doCodePos(ClassMethodActor method, int bci) {
                head[0] = new Caller(method, bci, head[0]);
                return true;
            }
        };
        targetMethod.forEachCodePos(cpc, ip, ipIsReturnAddress);
        if (head[0] == null) {
            return null;
        }
        return head[0].toCiCodePos(null);
    }

    /**
     * Iterates over the bytecode locations for the inlining chain rooted at a given instruction pointer.
     *
     * @param cpc a closure called for each bytecode location in the inlining chain rooted at {@code ip} (inner most
     *            callee first)
     * @param ip a pointer to an instruction within this method
     * @param ipIsReturnAddress
     * @return the number of bytecode locations iterated over (i.e. the number of times
     *         {@link CodePosClosure#doCodePos(ClassMethodActor, int)} was called
     */
    public int forEachCodePos(CodePosClosure cpc, Pointer ip, boolean ipIsReturnAddress) {
        return 0;
    }

    /**
     * Gets the bytecode frame(s) for a given stop.
     *
     * @param stopIndex an index of a stop within this method
     * @return the bytecode frame(s) at the denoted stop
     */
    public CiFrame getBytecodeFrames(int stopIndex) {
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
        return callEntryPoint;
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

    /**
     * The entry point used for <i>standard</i> calls in this target method to JVM compiled/interpreted code.
     * Non-standard calls are those to external native code and calls to the runtime inserted by the
     * compiler. The former type of calls directly use the native address supplied by the {@linkplain DynamicLinker linker}
     * and the latter always uses {@link CallEntryPoint#OPTIMIZED_ENTRY_POINT}.
     */
    @INSPECTED
    public final CallEntryPoint callEntryPoint;

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
     * @param codeBuffer the buffer containing the compiled code. The compiled code is in the first {@code this.code.length} bytes of {@code codeBuffer}.
     */
    protected final void setData(byte[] scalarLiterals, Object[] referenceLiterals, byte[] codeBuffer) {

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

        // now copy the code
        System.arraycopy(codeBuffer, 0, this.code, 0, this.code.length);
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
        final Object[] directCallees = directCallees();
        if (directCallees != null) {
            for (int i = 0; i < directCallees.length; i++) {
                final int offset = getCallEntryOffset(directCallees[i], i);
                Object currentDirectCallee = directCallees[i];
                if (currentDirectCallee == null) {
                    // template call
                    assert classMethodActor.isTemplate();
                } else {
                    final TargetMethod callee = getTargetMethod(currentDirectCallee);
                    if (callee == null) {
                        if (MaxineVM.isHosted() && classMethodActor.isTemplate()) {
                            assert currentDirectCallee == classMethodActor : "unlinkable call in a template must be a template call";
                            // leave call site unpatched
                        } else {
                            linkedAll = false;
                            final int pos = stopPosition(i);
                            final Address callSite = codeStart.plus(pos);
                            if (!isPatchableCallSite(callSite)) {
                                FatalError.unexpected(classMethodActor + ": call site calling static trampoline must be patchable: 0x" + callSite.toHexString() +
                                                " [0x" + codeStart.toHexString() + "+" + pos + "]");
                            }
                            fixupCallSite(pos, vm().stubs.staticTrampoline().codeStart.plus(offset));
                        }
                    } else {
                        fixupCallSite(stopPosition(i), callee.codeStart().plus(offset));
                    }
                }
            }
        }

        if (adapter != null) {
            adapter.generator.linkAdapterCallInPrologue(this, adapter);
        }
        return linkedAll;
    }

    public final TargetMethod getTargetMethod(Object o) {
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

    @HOSTED_ONLY
    protected boolean isDirectCalleeInPrologue(int directCalleeIndex) {
        return false;
    }

    public final boolean isCalleeSaved() {
        return registerRestoreEpilogueOffset >= 0;
    }

    /**
     * Gets an object to help decode inline data in this target method's code.
     */
    public InlineDataDecoder inlineDataDecoder() {
        return null;
    }

    /**
     * Gets the array recording the positions of the stops in this target method.
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
     * @param ip
     *                an instruction pointer that may denote an instruction in this target method
     * @return the start position of the bytecode instruction that is implemented at the instruction pointer or
     *         -1 if {@code ip} denotes an instruction that does not correlate to any bytecode. This will
     *         be the case when {@code ip} is in the adapter frame stub code, prologue or epilogue.
     */
    public final int posFor(Address ip) {
        final int pos = ip.minus(codeStart).toInt();
        if (pos >= 0 && pos <= code.length) {
            return pos;
        }
        return -1;
    }

    /**
     * Gets a mapping from bytecode positions to target code positions. A non-zero value
     * {@code val} at index {@code i} in the array encodes that there is a bytecode instruction whose opcode is at index
     * {@code i} in the bytecode array and whose target code position is {@code val}. Unless {@code i} is equal to the
     * length of the bytecode array in which case {@code val} denotes the target code position one byte past the
     * last target code byte emitted for the last bytecode instruction.
     *
     * @return {@code null} if there is no such mapping available
     */
    public int[] bciToPosMap() {
        return null;
    }

    /**
     * Gets the position of the next call (direct or indirect) in this target method after a given position.
     *
     * @param pos the position from which to start searching
     * @param nativeFunctionCall if {@code true}, then the search is refined to only consider
     *            {@linkplain #isNativeFunctionCall(int) native function calls}.
     *
     * @return -1 if the search fails
     */
    public int findNextCall(int pos, boolean nativeFunctionCall) {
        if (stopPositions == null || pos < 0 || pos > code.length) {
            return -1;
        }

        int closestCallPos = Integer.MAX_VALUE;
        final int numberOfCalls = numberOfDirectCalls() + numberOfIndirectCalls();
        for (int stopIndex = 0; stopIndex < numberOfCalls; stopIndex++) {
            final int callPosition = stopPosition(stopIndex);
            if (callPosition > pos && callPosition < closestCallPos && (!nativeFunctionCall || StopPositions.isNativeFunctionCall(stopPositions, stopIndex))) {
                closestCallPos = callPosition;
            }
        }
        if (closestCallPos != Integer.MAX_VALUE) {
            return closestCallPos;
        }
        return -1;
    }


    /**
     * Gets the index of a stop position within this target method derived from a given instruction pointer. If the
     * instruction pointer is equal to a safepoint position, then the index in {@link #stopPositions()} of that
     * safepoint is returned. Otherwise, the index of the highest stop position that is less than or equal to the
     * (possibly adjusted) target code position
     * denoted by the instruction pointer is returned.  That is, if {@code ip} does not exactly match a
     * stop position 'p' for a direct or indirect call, then the index of the highest stop position less than
     * 'p' is returned.
     *
     * @return -1 if no stop index can be found for {@code ip}
     * @see #stopPositions()
     */
    public int findClosestStopIndex(Pointer ip) {
        final int pos = posFor(ip);
        if (stopPositions == null || pos < 0 || pos > code.length) {
            return -1;
        }

        // Direct calls come first, followed by indirect calls and safepoints in the stopPositions array.

        // Check for matching safepoints first
        int numberOfCalls = numberOfDirectCalls() + numberOfIndirectCalls();
        for (int i = numberOfCalls; i < numberOfStopPositions(); i++) {
            if (stopPosition(i) == pos) {
                return i;
            }
        }

        // Check for native calls
        for (int i = 0; i < numberOfCalls; i++) {
            if (stopPosition(i) == pos && StopPositions.isNativeFunctionCall(stopPositions, i)) {
                return i;
            }
        }

        // Since this is not a safepoint, it must be a call.
        final int adjustedPos;
        if (platform().isa.offsetToReturnPC == 0) {
            // targetCodePostion is the instruction after the call (which might be another call).
            // We need to the find the call at which we actually stopped.
            adjustedPos = pos - 1;
        } else {
            adjustedPos = pos;
        }

        int stopIndexWithClosestPos = -1;
        for (int i = numberOfDirectCalls() - 1; i >= 0; --i) {
            final int directCallPosition = stopPosition(i);
            if (directCallPosition <= adjustedPos) {
                if (directCallPosition == adjustedPos) {
                    return i; // perfect match; no further searching needed
                }
                stopIndexWithClosestPos = i;
                break;
            }
        }

        // It is not enough that we find the first matching position, since there might be a direct as well as an indirect call before the instruction pointer
        // so we find the closest one. This can be avoided if we sort the stopPositions array first, but the runtime cost of this is unknown.
        for (int i = numberOfCalls - 1; i >= numberOfDirectCalls(); i--) {
            final int indirectCallPosition = stopPosition(i);
            if (indirectCallPosition <= adjustedPos && (stopIndexWithClosestPos < 0 || indirectCallPosition > stopPosition(stopIndexWithClosestPos))) {
                stopIndexWithClosestPos = i;
                break;
            }
        }

        return stopIndexWithClosestPos;
    }

    @Override
    public final String toString() {
        return (classMethodActor == null) ? regionName() : classMethodActor.format("%H.%n(%p)");
    }

    public String name() {
        return regionName();
    }

    public final String traceToString() {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final IndentWriter writer = new IndentWriter(new OutputStreamWriter(byteArrayOutputStream));
        writer.println("target method: " + this);
        traceBundle(writer);
        writer.flush();
        if (MaxineVM.isHosted()) {
            disassemble(byteArrayOutputStream);
        }
        return byteArrayOutputStream.toString();
    }


    /**
     * Prints a textual disassembly the code in a target method.
     *
     * @param out where to print the disassembly
     * @param targetMethod the target method whose code is to be disassembled
     */
    @HOSTED_ONLY
    public void disassemble(OutputStream out) {
        final Platform platform = Platform.platform();
        final InlineDataDecoder inlineDataDecoder = inlineDataDecoder();
        final Pointer startAddress = codeStart();
        final DisassemblyPrinter disassemblyPrinter = new DisassemblyPrinter(false) {
            @Override
            protected String disassembledObjectString(Disassembler disassembler, DisassembledObject disassembledObject) {
                String string = super.disassembledObjectString(disassembler, disassembledObject);
                if (string.startsWith("call ")) {
                    final Pointer ip = startAddress.plus(disassembledObject.startPosition());
                    final CiCodePos[] result = {null};
                    CodePosClosure cpc = new CodePosClosure() {
                        public boolean doCodePos(ClassMethodActor method, int bci) {
                            result[0] = new CiCodePos(null, method, bci);
                            return false;
                        }
                    };
                    forEachCodePos(cpc, ip, true);
                    CiCodePos codePos = result[0];
                    if (codePos != null) {
                        byte[] code = codePos.method.code();
                        int bci = codePos.bci;
                        byte opcode = code[bci];
                        if (opcode == INVOKEINTERFACE || opcode == INVOKESPECIAL || opcode == INVOKESTATIC || opcode == INVOKEVIRTUAL) {
                            int cpi = Bytes.beU2(code, bci + 1);
                            RiMethod callee = vm().runtime.getConstantPool(codePos.method).lookupMethod(cpi, opcode);
                            string += " [" + callee + "]";
                        }
                    }

                    if (StopPositions.isNativeFunctionCallPosition(stopPositions(), disassembledObject.startPosition())) {
                        string += " <native function call>";
                    }
                }
                return string;
            }
        };
        Disassembler.disassemble(out, code(), platform.isa, platform.wordWidth(), startAddress.toLong(), inlineDataDecoder, disassemblyPrinter);
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
    public void traceBundle(IndentWriter writer) {
        final TargetBundleLayout targetBundleLayout = TargetBundleLayout.from(this);
        writer.println("Layout:");
        writer.println(Strings.indent(targetBundleLayout.toString(), writer.indentation()));
        traceExceptionHandlers(writer);
        traceDirectCallees(writer);
        traceScalarBytes(writer, targetBundleLayout);
        traceReferenceLiterals(writer, targetBundleLayout);
        traceDebugInfo(writer);
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
     * Analyzes the target method that this compiler produced to build a call graph. This method gathers the
     * methods called directly or indirectly by this target method as well as the methods it inlined.
     *
     * @param directCalls the set of direct calls to which this method should append
     * @param virtualCalls the set of virtual calls to which this method should append
     * @param interfaceCalls the set of interface calls to which this method should append
     * @param inlinedMethods the set of inlined methods to which this method should append
     */
    @HOSTED_ONLY
    public abstract void gatherCalls(Set<MethodActor> directCalls, Set<MethodActor> virtualCalls, Set<MethodActor> interfaceCalls, Set<MethodActor> inlinedMethods);

    public abstract Address throwAddressToCatchAddress(boolean isTopFrame, Address throwAddress, Class<? extends Throwable> throwableClass);

    /**
     * Modifies the call site at the specified offset to use the new specified entry point.
     * The modification must tolerate the execution of the target method by concurrently running threads.
     *
     * @param callSite offset to a call site relative to the start of the code of this target method
     * @param callEntryPoint entry point the call site should call after patching
     */
    public abstract void patchCallSite(int callOffset, Address callEntryPoint);

    /**
     * Fixup of call site in the method. This differs from the above in that the call site is updated before
     * any thread can see it. Thus there isn't any concurrency between modifying the call site and threads
     * trying to run it.
     *
     * @param callOffset offset to a call site relative to the start of the code of this target method
     * @param callEntryPoint entry point the call site should call after fixup
     */
    public abstract void fixupCallSite(int callOffset, Address callEntryPoint);

    /**
     * Indicates whether a call site can be patched safely when multiple threads may execute this target method concurrently.
     * @param callSite offset to a call site relative to the start of the code of this target method.
     * @return true if mt-safe patching is possible on the specified call site.
     */
    public abstract boolean isPatchableCallSite(Address callSite);

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

    /**
     * Determines if this method has been compiled under the invariant that the
     * register state upon entry to a local exception handler for an implicit
     * exception is the same as at the implicit exception point.
     */
    public boolean preserveRegistersForLocalExceptionHandler() {
        return true;
    }

    /**
     * Gets the register configuration used to compile this method.
     */
    public final RiRegisterConfig getRegisterConfig() {
        RegisterConfigs configs = vm().registerConfigs;
        switch (flavor) {
            case Adapter:
                return null;
            case GlobalStub:
                return configs.globalStub;
            case VirtualTrampoline:
            case StaticTrampoline:
            case InterfaceTrampoline:
                return configs.trampoline;
            case BytecodeTemplate:
                return configs.bytecodeTemplate;
            case Standard:
                assert classMethodActor != null : "cannot determine register configuration for " + this;
                return configs.getRegisterConfig(classMethodActor);
            case TrapStub:
                return configs.trapStub;
            default:
                throw FatalError.unexpected(flavor.toString());
        }
    }

    /**
     * Gets the profile data gathered during execution of this method.
     *
     * @return {@code null} if this method has no profiling info
     */
    public MethodProfile profile() {
        return null;
    }
}
