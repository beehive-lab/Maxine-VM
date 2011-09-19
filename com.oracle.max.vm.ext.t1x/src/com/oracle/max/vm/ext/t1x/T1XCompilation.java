/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.t1x;

import static com.oracle.max.vm.ext.t1x.T1XTemplateTag.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.stack.JVMSFrameLayout.*;

import java.util.*;

import com.oracle.max.asm.*;
import com.oracle.max.vm.ext.t1x.T1XTemplate.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiTargetMethod.CodeAnnotation;
import com.sun.cri.ri.*;
import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.profile.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.*;

/**
 * T1X per-compilation information.
 * <p>
 * This class is designed such that a single instance can be re-used for
 * separate compilations.
 */
public abstract class T1XCompilation {

    // Static info

    protected static final AdapterGenerator adapterGenerator = AdapterGenerator.forCallee(null, CallEntryPoint.BASELINE_ENTRY_POINT);

    protected static final CiRegister scratch = vm().registerConfigs.standard.getScratchRegister();
    protected static final CiRegister scratch2 = vm().registerConfigs.standard.getReturnRegister(CiKind.Word);

    protected static final CiRegister sp = vm().registerConfigs.bytecodeTemplate.getRegisterForRole(VMRegister.ABI_SP);
    protected static final CiRegister fp = vm().registerConfigs.bytecodeTemplate.getRegisterForRole(VMRegister.ABI_FP);

    protected static final CiValue SP = sp.asValue();
    protected static final CiValue FP = fp.asValue();

    private static final int WORDS_PER_SLOT = JVMS_SLOT_SIZE / Word.size();
    protected static final int HALFWORD_OFFSET_IN_WORD = JVMSFrameLayout.offsetWithinWord(Kind.INT);

    protected static final CiAddress[] SP_WORD_ADDRESSES_CACHE = new CiAddress[4];
    protected static final CiAddress[] SP_INT_ADDRESSES_CACHE = new CiAddress[4];

    protected static final int FP_SLOTS_CACHE_START_OFFSET = -(20 * JVMS_SLOT_SIZE);
    protected static final int FP_SLOTS_CACHE_END_OFFSET = 40 * JVMS_SLOT_SIZE;
    protected static final CiAddress[] FP_SLOTS_CACHE = new CiAddress[(FP_SLOTS_CACHE_END_OFFSET - FP_SLOTS_CACHE_START_OFFSET) / JVMS_SLOT_SIZE];

    static {
        for (int i = 0; i < SP_WORD_ADDRESSES_CACHE.length; i++) {
            SP_WORD_ADDRESSES_CACHE[i] = new CiAddress(CiKind.Word, SP, i * JVMS_SLOT_SIZE);
        }
        for (int i = 0; i < SP_INT_ADDRESSES_CACHE.length; i++) {
            SP_INT_ADDRESSES_CACHE[i] = new CiAddress(CiKind.Word, SP, (i * JVMS_SLOT_SIZE) + HALFWORD_OFFSET_IN_WORD);
        }
        int offset = FP_SLOTS_CACHE_START_OFFSET;
        for (int i = 0; i < FP_SLOTS_CACHE.length; i++) {
            FP_SLOTS_CACHE[i] = new CiAddress(CiKind.Word, FP, offset);
            offset += JVMS_SLOT_SIZE;
        }
    }

    /**
     * Gets the effective address of a word-sized operand stack slot.
     *
     * @param index an operand stack index where 0 is the top slot, 1 is the slot below it etc
     * @return the effective address of the operand stack slot at index {@code index} from the top stack slot. This
     *         value can be used for a word-sized access to the operand stack.
     */
    protected static CiAddress spWord(int index) {
        assert index >= 0;
        if (index < SP_WORD_ADDRESSES_CACHE.length) {
            return SP_WORD_ADDRESSES_CACHE[index];
        }
        return new CiAddress(CiKind.Word, SP, index * JVMS_SLOT_SIZE);
    }

    /**
     * Gets the effective address of an int-sized operand stack slot.
     *
     * @param index an operand stack index where 0 is the top slot, 1 is the slot below it etc
     * @return the effective address of the operand stack slot at index {@code index} from the top stack slot. This
     *         value can be used for a int-sized access to the operand stack.
     */
    protected static CiAddress spInt(int index) {
        assert index >= 0;
        if (index < SP_INT_ADDRESSES_CACHE.length) {
            return SP_INT_ADDRESSES_CACHE[index];
        }
        return new CiAddress(CiKind.Word, SP, (index * JVMS_SLOT_SIZE) + HALFWORD_OFFSET_IN_WORD);
    }

    /**
     * Gets the effective address of a long-sized operand stack slot.
     *
     * @param index an operand stack index where 0 is the top slot, 1 is the slot below it etc
     * @return the effective address of the operand stack slot at index {@code index} from the top stack slot. This
     *         value can be used for a long-sized access to the operand stack.
     */
    protected static CiAddress spLong(int index) {
        assert Word.size() == 8 : "32-bit not yet supported";
        return spWord(index);
    }

    protected static CiAddress localSlot(int offset) {
        assert offset % JVMS_SLOT_SIZE == 0;
        int cacheIndex = (offset - FP_SLOTS_CACHE_START_OFFSET) / JVMS_SLOT_SIZE;
        if (cacheIndex >= 0 && cacheIndex < FP_SLOTS_CACHE.length) {
            return FP_SLOTS_CACHE[cacheIndex];
        }
        return new CiAddress(CiKind.Word, FP, offset);
    }

    /**
     * The minimum value to which {@link T1XOptions#TraceBytecodeParserLevel} must be set to trace
     * the bytecode instructions as they are parsed.
     */
    public static final int TRACELEVEL_INSTRUCTIONS = 1;

    // Fields holding info/data structures reused across all compilations

    /**
     * The buffer to which the assembler writes its output.
     */
    protected Buffer buf;

    /**
     * The compiler context.
     */
    final T1X compiler;

    /**
     * Object used to aggregate all the stops for the compiled code.
     */
    final SafepointsBuilder safepointsBuilder = new SafepointsBuilder();

    /**
     * The set of reference literals.
     */
    final ArrayList<Object> referenceLiterals;

    /**
     * Code annotations for disassembly jump tables (lazily initialized).
     */
    protected ArrayList<CodeAnnotation> codeAnnotations;

    // Fields holding per-compilation info

    /**
     * The method being compiled.
     */
    protected ClassMethodActor method;

    CodeAttribute codeAttribute;

    /**
     * The last bytecode compiled.
     */
    int prevOpcode;

    /**
     * Access to the bytecode being compiled.
     */
    protected BytecodeStream stream;

    /**
     * Frame info for the method being compiled.
     */
    protected JVMSFrameLayout frame;

    /**
     * The local variable index for a copy of the receiver object locked by a non-static synchronized method.
     */
    protected int synchronizedReceiver = -1;

    /**
     * The first code position in the range covered by the exception handler synthesized for a synchronized method.
     */
    int syncMethodStartPos = -1;

    /**
     * The code position one past the range covered by the exception handler synthesized for a synchronized method.
     */
    int syncMethodEndPos = -1;

    /**
     * The code position of the exception handler synthesized for a synchronized method.
     */
    int syncMethodHandlerPos = -1;

    /**
     * Constant pool of the method being compiled.
     */
    protected ConstantPool cp;

    /**
     * The bytecode indexes of the basic blocks.
     */
    boolean[] blockBCIs;

    /**
     * The number of blocks in the method.
     */
    int numberOfBlocks;

    /**
     * The BCIs of the exception handlers.
     */
    boolean[] handlerBCIs;

    /**
     * The exception handlers.
     */
    CiExceptionHandler[] handlers;

    protected MethodProfile.Builder methodProfileBuilder;

    /**
     * The template currently being emitted.
     */
    protected T1XTemplate template;

    /**
     * Bit map of which arguments for the current template have been initialized.
     */
    private int initializedArgs;

    /**
     * Map of BCIs to target code positions. Entries in the table corresponding to the start of a bytecode instruction
     * hold the position in the code buffer where the first byte of the template was emitted. This map
     * includes an entry for the BCI one byte past the end of the bytecode array. This is useful
     * for determining the end of the code emitted for the last bytecode instruction. That is, the value at
     * {@code bciToPos[bciToPos.length - 1]} is the target code position at which the epilogue starts
     * (0 denotes absence of an epilogue).
     */
    protected int[] bciToPos;

    Adapter adapter;

    /**
     * Creates a compilation object.
     */
    public T1XCompilation(T1X compiler) {
        this.compiler = compiler;
        this.referenceLiterals = new ArrayList<Object>();
    }

    /**
     * Initializes all the per-compilation fields.
     *
     * @param method the method about to be compiled
     */
    protected void initCompile(ClassMethodActor method, CodeAttribute codeAttribute) {
        assert this.method == null;
        assert buf.position() == 0;
        assert referenceLiterals.isEmpty();
        this.method = method;
        this.codeAttribute = codeAttribute;
        cp = codeAttribute.cp;
        byte[] code = codeAttribute.code();
        stream = new BytecodeStream(code);

        bciToPos = new int[code.length + 1];
        blockBCIs = new boolean[code.length];
        methodProfileBuilder = MethodInstrumentation.createMethodProfile(method);

        startBlock(0);

        initFrame(method, codeAttribute);

        initHandlers(method, code);
    }

    static void startTimer(T1XTimer timer) {
        if (T1XOptions.PrintTimers) {
            timer.start();
        }
    }

    static void stopTimer(T1XTimer timer) {
        if (T1XOptions.PrintTimers) {
            timer.stop();
        }
    }

    void initHandlers(ClassMethodActor method, byte[] code) {
        handlers = codeAttribute.exceptionHandlers();
        if (handlers.length != 0) {
            handlerBCIs = new boolean[code.length];
            for (CiExceptionHandler handler : handlers) {
                handlerBCIs[handler.handlerBCI()] = true;
            }
        } else {
            handlerBCIs = null;
        }
    }

    /**
     * Initializes {@link #frame} and {@link #synchronizedReceiver}.
     */
    protected abstract void initFrame(ClassMethodActor method, CodeAttribute codeAttribute);

    /**
     * Cleans up all the per-compilation fields.
     */
    protected void cleanup() {
        method = null;
        codeAttribute = null;
        frame = null;
        bciToPos = null;
        blockBCIs = null;
        numberOfBlocks = 0;
        stream = null;
        handlerBCIs = null;
        handlers = null;
        syncMethodStartPos = -1;
        syncMethodEndPos = -1;
        synchronizedReceiver = -1;
        syncMethodHandlerPos = -1;
        cp = null;
        buf.reset();
        referenceLiterals.clear();
        if (codeAnnotations != null) {
            codeAnnotations.clear();
        }
        adapter = null;
        safepointsBuilder.reset(false);
        methodProfileBuilder = null;

        template = null;
        initializedArgs = 0;
    }

    /**
     * Thrown to indicate that {@link Bytecodes#JSR} or {@link Bytecodes#RET}
     * was encountered when compiling a method.
     */
    @SuppressWarnings("serial")
    class UnsupportedSubroutineException extends RuntimeException {
        final int bci;
        final int opcode;
        public UnsupportedSubroutineException(int opcode, int bci) {
            super(Bytecodes.nameOf(opcode) + "@" + bci + " in " + method);
            this.bci = bci;
            this.opcode = opcode;
        }
    }

    /**
     * Translates the bytecode of a given method into a {@link T1XTargetMethod}.
     */
    public T1XTargetMethod compile(ClassMethodActor method, boolean install) {
        try {
            return compile1(method, method.codeAttribute(), install);
        } catch (UnsupportedSubroutineException e) {
            T1XMetrics.MethodsWithSubroutines++;
            if (T1XOptions.PrintJsrRetRewrites) {
                Log.println("T1X rewriting bytecode of " + method + " to inline subroutine indicated by " + Bytecodes.nameOf(e.opcode) + " at bci " + e.bci);
            }
            TypeInferencingVerifier verifier = new TypeInferencingVerifier(method.holder());
            CodeAttribute codeAttribute = verifier.verify(method, this.codeAttribute);
            cleanup();
            return compile1(method, codeAttribute, install);
        }
    }

    public T1XTargetMethod compile1(ClassMethodActor method, CodeAttribute codeAttribute, boolean install) {
        startTimer(T1XTimer.PRE_COMPILE);
        try {
            initCompile(method, codeAttribute);
        } finally {
            stopTimer(T1XTimer.PRE_COMPILE);
        }

        startTimer(T1XTimer.COMPILE);
        try {
            compile2(method);
        } finally {
            stopTimer(T1XTimer.COMPILE);
        }

        startTimer(T1XTimer.FIXUP);
        try {
            int endPos = buf.position();
            fixup();
            buf.setPosition(endPos);
        } finally {
            stopTimer(T1XTimer.FIXUP);
        }

        startTimer(T1XTimer.INSTALL);
        try {
            return new T1XTargetMethod(this, install);
        } finally {
            stopTimer(T1XTimer.INSTALL);
        }
    }

    void compile2(ClassMethodActor method) throws InternalError {
        adapter = emitPrologue();

        do_profileMethodEntry();

        do_methodTraceEntry();

        do_synchronizedMethodAcquire();

        int bci = 0;
        int endBCI = stream.endBCI();
        while (bci < endBCI) {
            int opcode = stream.currentBC();
            processBytecode(opcode);
            stream.next();
            bci = stream.currentBCI();
        }

        int epiloguePos = buf.position();

        do_synchronizedMethodHandler(method, endBCI);

        if (epiloguePos != buf.position()) {
            bciToPos[endBCI] = epiloguePos;
        }
    }

    protected void startBlock(int bci) {
        if (!blockBCIs[bci]) {
            numberOfBlocks++;
            blockBCIs[bci] = true;
        }
    }

    protected void beginBytecode(int representativeOpcode) {
        int bci = stream.currentBCI();
        int pos = buf.position();

        bciToPos[bci] = pos;

        if (Bytecodes.isBlockEnd(prevOpcode)) {
            startBlock(bci);
            if (handlerBCIs != null) {
                if (handlerBCIs[bci]) {
                    emit(LOAD_EXCEPTION);
                }

            }
        }

        prevOpcode = representativeOpcode;
    }

    /**
     * Starts the process of emitting a template.
     * This includes emitting code to copy any arguments from the stack to the
     * relevant parameter locations.
     * <p>
     * A call to this method must be matched with a call to {@link #finish()} or {@link #finish(ClassMethodActor, boolean)}
     * once the code for initializing the non-stack-based template parameters has been emitted.
     *
     * @param tag denotes the template to emit
     */
    protected void start(T1XTemplateTag tag) {
        assert template == null;
        this.template = getTemplate(tag);
        assert template != null : "template for tag " + tag + " is null";

        initializedArgs = 0;
        Sig sig = template.sig;
        if (sig.stackArgs != 0) {
            for (int i = 0; i < sig.in.length; i++) {
                Arg a = sig.in[i];
                if (a.isStack()) {
                    initializedArgs |= 1 << i;
                    switch (a.kind) {
                        case Int:
                            peekInt(a.reg, a.slot);
                            break;
                        case Word:
                            peekWord(a.reg, a.slot);
                            break;
                        case Float:
                            peekFloat(a.reg, a.slot);
                            break;
                        case Long:
                            peekLong(a.reg, a.slot);
                            break;
                        case Double:
                            peekDouble(a.reg, a.slot);
                            break;
                        case Object:
                            peekObject(a.reg, a.slot);
                            break;
                        default:
                            assert false;
                    }
                }
            }
        }
    }

    /**
     * Returns the template to use for {@code tag}.
     * By default, returns the template in the associated {@link #compiler compiler}
     * templates array, but may be overridden by a subclass to make the behavior
     * more context sensitive.
     * @param tag
     * @return
     */
    protected T1XTemplate getTemplate(T1XTemplateTag tag) {
        return  compiler.templates[tag.ordinal()];
    }

    /**
     * Completes the process of emitting the current template.
     */
    protected void finish() {
        assert template != null;
        assert assertArgsAreInitialized();

        emitAndRecordSafepoints(template);

        // Adjust the stack to model the net effect of the template including
        // the slot for the value pushed (if any) by the template.
        Sig sig = template.sig;
        if (sig.stackDelta < 0) {
            decStack(-sig.stackDelta);
        } else if (sig.stackDelta > 0) {
            incStack(sig.stackDelta);
        }

        // The stack parameters to an invoke are popped by the callee so they should not also be
        // popped as part of the stack adjustment above.
        assert sig.stackArgs == 0 || !Bytecodes.isInvoke(template.tag.opcode) : template + ": invoke templates should not use @" + Slot.class.getSimpleName() + " annotation";

        // Push the result of the template (if any)
        if (sig.out.isStack()) {
            Arg out = sig.out;
            switch (out.kind) {
                case Int:
                    pokeInt(out.reg, out.slot);
                    break;
                case Word:
                    pokeWord(out.reg, out.slot);
                    break;
                case Float:
                    pokeFloat(out.reg, out.slot);
                    break;
                case Long:
                    pokeLong(out.reg, out.slot);
                    break;
                case Double:
                    pokeDouble(out.reg, out.slot);
                    break;
                case Object:
                    pokeObject(out.reg, out.slot);
                    break;
                default:
                    assert false : out.kind;
            }
        }
        template = null;
        initializedArgs = 0;
    }

    /**
     * Asserts that a given argument of the current template has not yet been initialized
     * and then records the fact that it is now initialized.
     */
    private boolean assertInitializeArg(Arg a, int n) {
        int argBit = 1 << n;
        if ((initializedArgs & argBit) != 0) {
            throw new AssertionError(template + ": parameter " + n + " (\"" + a.name + "\") is already initialized");
        }
        initializedArgs |= argBit;
        return true;
    }

    /**
     * Asserts that all the arguments of the current template have been initialized.
     */
    private boolean assertArgsAreInitialized() {
        int allArgs = (1 << template.sig.in.length) - 1;
        int initArgs = initializedArgs;
        if (initArgs != allArgs) {
            StringBuilder sb = new StringBuilder(template + ": uninitialized arguments: ");
            String sep = "";
            for (int i = 0; i < 32 && allArgs != 0; i++) {
                if ((initArgs & 1) == 0) {
                    sb.append(sep).append(template.sig.in[i].name);
                    sep = ", ";
                }
                allArgs >>= 1;
                initArgs >>= 1;
            }
            throw new InternalError(sb.toString());
        }
        return true;
    }

    /**
     * Emits a template including the setup of stack-based parameters and pushing of the result (if any).
     */
    protected void emit(T1XTemplateTag tag) {
        start(tag);
        finish();
    }

    @Override
    public String toString() {
        String s = getClass().getSimpleName() + "[" + Thread.currentThread().getName() + "]";
        ClassMethodActor method = this.method;
        if (method != null) {
            s += ": " + method;
        }
        return s;
    }

    /**
     * Copies the code from a given template into the code buffer and updates the set of safepoints for the method being
     * translated with those derived from the template.
     *
     * @param template the compiled code to emit
     */
    protected void emitAndRecordSafepoints(T1XTemplate template) {
        if (template.safepoints.length != 0) {
            int bci = stream.currentBCI();
            safepointsBuilder.add(template, buf.position(), bci == stream.endBCI() ? -1 : bci);
        }
        buf.emitBytes(template.code, 0, template.code.length);
    }

    protected abstract Adapter emitPrologue();

    protected abstract void emitEpilogue();

    protected int localSlotOffset(int localIndex, CiKind kind) {
        // Long and double locals use two slots in the locals area,
        // as required by the JVM spec. The value of the long or double local is stored in
        // the second slot.
        int slotIndex = localIndex + kind.jvmSlots - 1;
        int slotOffset = frame.localVariableOffset(slotIndex) + JVMSFrameLayout.offsetInStackSlot(kind);
        return slotOffset;
    }


    protected abstract void loadInt(CiRegister dst, int index);
    protected abstract void loadLong(CiRegister dst, int index);
    protected abstract void loadWord(CiRegister dst, int index);
    protected abstract void loadObject(CiRegister dst, int index);

    protected abstract void storeInt(CiRegister src, int index);
    protected abstract void storeLong(CiRegister src, int index);
    protected abstract void storeWord(CiRegister src, int index);
    protected abstract void storeObject(CiRegister src, int index);

    /**
     * Emits code for a branch.
     *
     * @param opcode
     * @param targetBCI
     * @param bci
     */
    protected abstract void branch(int opcode, int targetBCI, int bci);

    /**
     * Gets the kind used to select an INVOKE... bytecode template.
     */
    protected abstract CiKind invokeKind(RiSignature signature);

    /**
     * Gets the index of the {@link Slot slot} containing the receiver for
     * a non-static call.
     *
     * @param signature the signature of the call
     */
    protected static int receiverStackIndex(SignatureDescriptor signature) {
        int index = 0;
        for (int i = 0; i < signature.numberOfParameters(); i++) {
            Kind kind = signature.parameterDescriptorAt(i).toKind();
            index += kind.stackSlots;
        }
        return index;
    }

    /**
     * Fixes up the code locations that need to be patched.
     */
    protected abstract void fixup();

    /**
     * Computes the offset to a literal reference being created. The current position in the code buffer must be
     * that of the instruction loading the literal.
     *
     * @param numReferenceLiterals number of created reference literals (including the one being created)
     * @return an offset, in bytes, to the base used to load the literal
     */
    protected abstract int computeReferenceLiteralOffset(int numReferenceLiterals);

    /**
     * Return the relative offset of the literal to the current code buffer position. (negative number since literals
     * are placed before code in the bundle)
     *
     * @param literal the object
     * @return the offset of the literal relative to the current code position
     */
    protected int createReferenceLiteral(Object literal) {
        int literalOffset = computeReferenceLiteralOffset(1 + referenceLiterals.size());
        referenceLiterals.add(literal);
        if (DebugHeap.isTagging()) {
            // Account for the DebugHeap tag in front of the code object:
            literalOffset += Word.size();
        }
        return -literalOffset;
    }

    /**
     * Emits code to assign the value in {@code src} to {@code dst}.
     */
    protected abstract void assignObjectReg(CiRegister dst, CiRegister src);

    /**
     * Emits code to assign the value in {@code src} to {@code dst}.
     */
    protected abstract void assignWordReg(CiRegister dst, CiRegister src);

    /**
     * Emits code to assign {@code value} to {@code dst}.
     */
    protected abstract void assignInt(CiRegister dst, int value);

    /**
     * Emits code to assign {@code value} to {@code dst}.
     */
    protected abstract void assignLong(CiRegister dst, long value);

    /**
     * Emits code to assign {@code value} to {@code dst}.
     */
    protected abstract void assignObject(CiRegister dst, Object value);

    /**
     * Emits code to assign {@code value} to {@code dst}.
     */
    protected abstract void assignFloat(CiRegister dst, float value);

    /**
     * Emits code to assign {@code value} to {@code dst}.
     */
    protected abstract void assignDouble(CiRegister dst, double value);

    /**
     * Emits code to decrement the operand stack pointer by {@code n} slots.
     */
    protected abstract void decStack(int n);

    /**
     * Emits code to increment the operand stack pointer by {@code n} slots.
     */
    protected abstract void incStack(int n);

    /**
     * Emits code to adjust the value in {@code reg} by {@code delta}.
     */
    protected abstract void adjustReg(CiRegister reg, int delta);

    /**
     * Emits code to copy the value in the {@code i}'th {@linkplain Slot slot} to register {@code dst}.
     */
    protected abstract void peekObject(CiRegister dst, int i);

    /**
     * Emits code to copy the value in register {@code src} to the {@code i}'th {@linkplain Slot slot}.
     */
    protected abstract void pokeObject(CiRegister src, int i);

    /**
     * Emits code to copy the value in the {@code i}'th {@linkplain Slot slot} to register {@code dst}.
     */
    protected abstract void peekWord(CiRegister dst, int i);

    /**
     * Emits code to copy the value in register {@code src} to the {@code i}'th {@linkplain Slot slot}.
     */
    protected abstract void pokeWord(CiRegister src, int i);

    /**
     * Emits code to copy the value in the {@code i}'th {@linkplain Slot slot} to register {@code dst}.
     */
    protected abstract void peekInt(CiRegister dst, int i);

    /**
     * Emits code to copy the value in register {@code src} to the {@code i}'th {@linkplain Slot slot}.
     */
    protected abstract void pokeInt(CiRegister src, int i);

    /**
     * Emits code to copy the value in the {@code i}'th {@linkplain Slot slot} to register {@code dst}.
     */
    protected abstract void peekLong(CiRegister dst, int i);

    /**
     * Emits code to copy the value in register {@code src} to the {@code i}'th {@linkplain Slot slot}.
     */
    protected abstract void pokeLong(CiRegister src, int i);

    /**
     * Emits code to copy the value in the {@code i}'th {@linkplain Slot slot} to register {@code dst}.
     */
    protected abstract void peekDouble(CiRegister dst, int i);

    /**
     * Emits code to copy the value in register {@code src} to the {@code i}'th {@linkplain Slot slot}.
     */
    protected abstract void pokeDouble(CiRegister src, int i);

    /**
     * Emits code to copy the value in the {@code i}'th {@linkplain Slot slot} to register {@code dst}.
     */
    protected abstract void peekFloat(CiRegister dst, int i);

    /**
     * Emits code to copy the value in register {@code src} to the {@code i}'th {@linkplain Slot slot}.
     */
    protected abstract void pokeFloat(CiRegister src, int i);

    /**
     * Emits code to trap if the value in register {@code src} is 0.
     */
    protected abstract void nullCheck(CiRegister src);

    /**
     * Emits a direct call instruction whose immediate operand (denoting the absolute or relative offset to the target) will be patched later.
     *
     * @return the {@linkplain Safepoints safepoint} for the call
     */
    protected abstract int callDirect();

    /**
     * Emits an indirect call instruction.
     *
     * @param target the register holding the address of the call target
     * @param receiverStackIndex the index of the receiver which must be copied from the stack to the receiver register
     *            used by the optimizing compiler. This is required so that dynamic trampolines can find the receiver in
     *            the expected register. If {@code receiverStackIndex == -1}, then the copy is not emitted as
     *            {@code target} is guaranteed to not be the address of a trampoline.
     * @return the {@linkplain Safepoints safepoint} for the call
     */
    protected abstract int callIndirect(CiRegister target, int receiverStackIndex);

    /**
     * Gets the register for a given parameter of the current template.
     * When assertions are enabled, this method also checks the index, name and kind
     * of the parameter and ensures it is not being initialized more than once.
     *
     * @param n the index of the parameter
     * @param name the expected name of the parameter
     * @param kind the expected kind of the parameter
     * @return the register for parameter {@code n} of the current template.
     */
    @INLINE
    protected final CiRegister reg(int n, String name, CiKind kind) {
        assert n >= 0 && n < template.sig.in.length : template + ": parameter " + n + " is out of bounds";
        Arg a = template.sig.in[n];
        assert assertInitializeArg(a, n);
        assert a.kind == kind : template + ": expected " + a.kind + " value for parameter " + n + " (\"" + a.name + "\"), not " + kind;
        assert a.name.equals(name) : template + ": expected name of parameter " + n + " to be \"" + a.name + "\", not \"" + name + "\"";
        return a.reg;
    }

    /**
     * Emits code to copy the value in {@code src} to parameter {@code n} of the current template.
     *
     * @param name the expected name of the parameter
     */
    final void assignObjectReg(int n, String name, CiRegister src) {
        assignObjectReg(reg(n, name, CiKind.Object), src);
    }

    /**
     * Emits code to copy the value in {@code src} to parameter {@code n} of the current template.
     *
     * @param name the expected name of the parameter
     */
    final void assignWordReg(int n, String name, CiRegister src) {
        assignWordReg(reg(n, name, CiKind.Word), src);
    }

    /**
     * Emits code to assign {@code value} to parameter {@code n} of the current template.
     *
     * @param name the expected name of the parameter
     */
    final protected void assignObject(int n, String name, Object value) {
        assignObject(reg(n, name, CiKind.Object), value);
    }

    /**
     * Emits code to assign {@code value} to parameter {@code n} of the current template.
     *
     * @param name the expected name of the parameter
     */
    final protected void assignInt(int n, String name, int value) {
        assignInt(reg(n, name, CiKind.Int), value);
    }

    /**
     * Emits code to assign {@code value} to parameter {@code n} of the current template.
     *
     * @param name the expected name of the parameter
     */
    final protected void assignFloat(int n, String name, float value) {
        assignFloat(reg(n, name, CiKind.Float), value);
    }

    /**
     * Emits code to assign {@code value} to parameter {@code n} of the current template.
     *
     * @param name the expected name of the parameter
     */
    final protected void assignLong(int n, String name, long value) {
        assignLong(reg(n, name, CiKind.Long), value);
    }

    /**
     * Emits code to assign {@code value} to parameter {@code n} of the current template.
     *
     * @param name the expected name of the parameter
     */
    final protected void assignDouble(int n, String name, double value) {
        assignDouble(reg(n, name, CiKind.Double), value);
    }

    /**
     * Emits code to copy the value from index {@code i} of the local variables array
     * to parameter {@code n} of the current template.
     *
     * @param name the expected name of the parameter
     */
    final protected void loadInt(int n, String name, int i) {
        loadInt(reg(n, name, CiKind.Int), i);
    }

    /**
     * Emits code to copy the value from index {@code i} of the local variables array
     * to parameter {@code n} of the current template.
     *
     * @param name the expected name of the parameter
     */
    final protected void loadObject(int n, String name, int i) {
        loadObject(reg(n, name, CiKind.Object), i);
    }

    /**
     * Emits code to copy the value in the {@code i}'th {@linkplain Slot slot} of the operand stack
     * to parameter {@code n} of the current template.
     *
     * @param name the expected name of the parameter
     */
    protected final void peekObject(int n, String name, int i) {
        peekObject(reg(n, name, CiKind.Object), i);
    }

    /**
     * Emits code to copy the value in the {@code i}'th {@linkplain Slot slot} of the operand stack
     * to parameter {@code n} of the current template.
     *
     * @param name the expected name of the parameter
     */
    final void peekInt(int n, String name, int i) {
        peekInt(reg(n, name, CiKind.Int), i);
    }

    /**
     * Emits code to copy the value in the {@code i}'th {@linkplain Slot slot} of the operand stack
     * to parameter {@code n} of the current template.
     *
     * @param name the expected name of the parameter
     */
    final void peekFloat(int n, String name, int i) {
        peekFloat(reg(n, name, CiKind.Float), i);
    }

    /**
     * Emits code to copy the value in the {@code i}'th {@linkplain Slot slot} of the operand stack
     * to parameter {@code n} of the current template.
     *
     * @param name the expected name of the parameter
     */
    final void peekDouble(int n, String name, int i) {
        peekDouble(reg(n, name, CiKind.Double), i);
    }

    /**
     * Emits code to copy the value in the {@code i}'th {@linkplain Slot slot} of the operand stack
     * to parameter {@code n} of the current template.
     *
     * @param name the expected name of the parameter
     */
    final void peekLong(int n, String name, int i) {
        peekLong(reg(n, name, CiKind.Long), i);
    }

    private String errorSuffix() {
        int opcode = stream.currentBC();
        String name = Bytecodes.nameOf(opcode);
        return " [bci=" + stream.currentBCI() + ", opcode=" + opcode + "(" + name + ")]";
    }

    protected void processBytecode(int opcode) throws InternalError {
        beginBytecode(opcode);
        switch (opcode) {
            // Checkstyle: stop

            case Bytecodes.NOP                : bciToPos[stream.currentBCI()] = buf.position(); break;
            case Bytecodes.AALOAD             : do_aaload(); break;
            case Bytecodes.AASTORE            : do_aastore(); break;
            case Bytecodes.ACONST_NULL        : do_oconst(null); break;
            case Bytecodes.ARRAYLENGTH        : do_arraylength(); break;
            case Bytecodes.ATHROW             : do_athrow(); break;
            case Bytecodes.BALOAD             : do_baload(); break;
            case Bytecodes.BASTORE            : do_bastore(); break;
            case Bytecodes.CALOAD             : do_caload(); break;
            case Bytecodes.CASTORE            : do_castore(); break;
            case Bytecodes.D2F                : do_d2f(); break;
            case Bytecodes.D2I                : do_d2i(); break;
            case Bytecodes.D2L                : do_d2l(); break;
            case Bytecodes.DADD               : do_dadd(); break;
            case Bytecodes.DALOAD             : do_daload(); break;
            case Bytecodes.DASTORE            : do_dastore(); break;
            case Bytecodes.DCMPG              : do_dcmpg(); break;
            case Bytecodes.DCMPL              : do_dcmpl(); break;
            case Bytecodes.DDIV               : do_ddiv(); break;
            case Bytecodes.DMUL               : do_dmul(); break;
            case Bytecodes.DREM               : do_drem(); break;
            case Bytecodes.DSUB               : do_dsub(); break;
            case Bytecodes.DUP                : do_dup(); break;
            case Bytecodes.DUP2               : do_dup2(); break;
            case Bytecodes.DUP2_X1            : do_dup2_x1(); break;
            case Bytecodes.DUP2_X2            : do_dup2_x2(); break;
            case Bytecodes.DUP_X1             : do_dup_x1(); break;
            case Bytecodes.DUP_X2             : do_dup_x2(); break;
            case Bytecodes.F2D                : do_f2d(); break;
            case Bytecodes.F2I                : do_f2i(); break;
            case Bytecodes.F2L                : do_f2l(); break;
            case Bytecodes.FADD               : do_fadd(); break;
            case Bytecodes.FALOAD             : do_faload(); break;
            case Bytecodes.FASTORE            : do_fastore(); break;
            case Bytecodes.FCMPG              : do_fcmpg(); break;
            case Bytecodes.FCMPL              : do_fcmpl(); break;
            case Bytecodes.FDIV               : do_fdiv(); break;
            case Bytecodes.FMUL               : do_fmul(); break;
            case Bytecodes.FREM               : do_frem(); break;
            case Bytecodes.FSUB               : do_fsub(); break;
            case Bytecodes.I2B                : do_i2b(); break;
            case Bytecodes.I2C                : do_i2c(); break;
            case Bytecodes.I2D                : do_i2d(); break;
            case Bytecodes.I2F                : do_i2f(); break;
            case Bytecodes.I2L                : do_i2l(); break;
            case Bytecodes.I2S                : do_i2s(); break;
            case Bytecodes.IADD               : do_iadd(); break;
            case Bytecodes.IALOAD             : do_iaload(); break;
            case Bytecodes.IAND               : do_iand(); break;
            case Bytecodes.IASTORE            : do_iastore(); break;
            case Bytecodes.ICONST_0           : do_iconst(0); break;
            case Bytecodes.ICONST_1           : do_iconst(1); break;
            case Bytecodes.ICONST_2           : do_iconst(2); break;
            case Bytecodes.ICONST_3           : do_iconst(3); break;
            case Bytecodes.ICONST_4           : do_iconst(4); break;
            case Bytecodes.ICONST_5           : do_iconst(5); break;
            case Bytecodes.ICONST_M1          : do_iconst(-1); break;
            case Bytecodes.IDIV               : do_idiv(); break;
            case Bytecodes.IMUL               : do_imul(); break;
            case Bytecodes.INEG               : do_ineg(); break;
            case Bytecodes.IOR                : do_ior(); break;
            case Bytecodes.IREM               : do_irem(); break;
            case Bytecodes.ISHL               : do_ishl(); break;
            case Bytecodes.ISHR               : do_ishr(); break;
            case Bytecodes.ISUB               : do_isub(); break;
            case Bytecodes.IUSHR              : do_iushr(); break;
            case Bytecodes.IXOR               : do_ixor(); break;
            case Bytecodes.L2D                : do_l2d(); break;
            case Bytecodes.L2F                : do_l2f(); break;
            case Bytecodes.L2I                : do_l2i(); break;
            case Bytecodes.LADD               : do_ladd(); break;
            case Bytecodes.LALOAD             : do_laload(); break;
            case Bytecodes.LAND               : do_land(); break;
            case Bytecodes.LASTORE            : do_lastore(); break;
            case Bytecodes.LCMP               : do_lcmp(); break;
            case Bytecodes.LDIV               : do_ldiv(); break;
            case Bytecodes.LMUL               : do_lmul(); break;
            case Bytecodes.LNEG               : do_lneg(); break;
            case Bytecodes.LOR                : do_lor(); break;
            case Bytecodes.LREM               : do_lrem(); break;
            case Bytecodes.LSHL               : do_lshl(); break;
            case Bytecodes.LSHR               : do_lshr(); break;
            case Bytecodes.LSUB               : do_lsub(); break;
            case Bytecodes.LUSHR              : do_lushr(); break;
            case Bytecodes.LXOR               : do_lxor(); break;
            case Bytecodes.MONITORENTER       : do_monitorenter(); break;
            case Bytecodes.MONITOREXIT        : do_monitorexit(); break;
            case Bytecodes.POP                : do_pop(); break;
            case Bytecodes.POP2               : do_pop2(); break;
            case Bytecodes.SALOAD             : do_saload(); break;
            case Bytecodes.SASTORE            : do_sastore(); break;
            case Bytecodes.SWAP               : do_swap(); break;
            case Bytecodes.LCONST_0           : do_lconst(0L); break;
            case Bytecodes.LCONST_1           : do_lconst(1L); break;
            case Bytecodes.DCONST_0           : do_dconst(0D); break;
            case Bytecodes.DCONST_1           : do_dconst(1D); break;
            case Bytecodes.DNEG               : do_dneg(); break;
            case Bytecodes.FCONST_0           : do_fconst(0F); break;
            case Bytecodes.FCONST_1           : do_fconst(1F); break;
            case Bytecodes.FCONST_2           : do_fconst(2F); break;
            case Bytecodes.FNEG               : do_fneg(); break;
            case Bytecodes.ARETURN            : do_return(ARETURN, ARETURN$unlock); break;
            case Bytecodes.DRETURN            : do_return(DRETURN, DRETURN$unlock); break;
            case Bytecodes.FRETURN            : do_return(FRETURN, FRETURN$unlock); break;
            case Bytecodes.IRETURN            : do_return(IRETURN, IRETURN$unlock); break;
            case Bytecodes.LRETURN            : do_return(LRETURN, LRETURN$unlock); break;
            case Bytecodes.RETURN             : do_return(RETURN, RETURN$unlock); break;
            case Bytecodes.ALOAD              : do_load(stream.readLocalIndex(), CiKind.Object); break;
            case Bytecodes.ALOAD_0            : do_load(0, CiKind.Object); break;
            case Bytecodes.ALOAD_1            : do_load(1, CiKind.Object); break;
            case Bytecodes.ALOAD_2            : do_load(2, CiKind.Object); break;
            case Bytecodes.ALOAD_3            : do_load(3, CiKind.Object); break;
            case Bytecodes.ASTORE             : do_store(stream.readLocalIndex(), CiKind.Object); break;
            case Bytecodes.ASTORE_0           : do_store(0, CiKind.Object); break;
            case Bytecodes.ASTORE_1           : do_store(1, CiKind.Object); break;
            case Bytecodes.ASTORE_2           : do_store(2, CiKind.Object); break;
            case Bytecodes.ASTORE_3           : do_store(3, CiKind.Object); break;
            case Bytecodes.DLOAD              : do_load(stream.readLocalIndex(), CiKind.Double); break;
            case Bytecodes.DLOAD_0            : do_load(0, CiKind.Double); break;
            case Bytecodes.DLOAD_1            : do_load(1, CiKind.Double); break;
            case Bytecodes.DLOAD_2            : do_load(2, CiKind.Double); break;
            case Bytecodes.DLOAD_3            : do_load(3, CiKind.Double); break;
            case Bytecodes.DSTORE             : do_store(stream.readLocalIndex(), CiKind.Double); break;
            case Bytecodes.DSTORE_0           : do_store(0, CiKind.Double); break;
            case Bytecodes.DSTORE_1           : do_store(1, CiKind.Double); break;
            case Bytecodes.DSTORE_2           : do_store(2, CiKind.Double); break;
            case Bytecodes.DSTORE_3           : do_store(3, CiKind.Double); break;
            case Bytecodes.FLOAD              : do_load(stream.readLocalIndex(), CiKind.Float); break;
            case Bytecodes.FLOAD_0            : do_load(0, CiKind.Float); break;
            case Bytecodes.FLOAD_1            : do_load(1, CiKind.Float); break;
            case Bytecodes.FLOAD_2            : do_load(2, CiKind.Float); break;
            case Bytecodes.FLOAD_3            : do_load(3, CiKind.Float); break;
            case Bytecodes.FSTORE             : do_store(stream.readLocalIndex(), CiKind.Float); break;
            case Bytecodes.FSTORE_0           : do_store(0, CiKind.Float); break;
            case Bytecodes.FSTORE_1           : do_store(1, CiKind.Float); break;
            case Bytecodes.FSTORE_2           : do_store(2, CiKind.Float); break;
            case Bytecodes.FSTORE_3           : do_store(3, CiKind.Float); break;
            case Bytecodes.ILOAD              : do_load(stream.readLocalIndex(), CiKind.Int); break;
            case Bytecodes.ILOAD_0            : do_load(0, CiKind.Int); break;
            case Bytecodes.ILOAD_1            : do_load(1, CiKind.Int); break;
            case Bytecodes.ILOAD_2            : do_load(2, CiKind.Int); break;
            case Bytecodes.ILOAD_3            : do_load(3, CiKind.Int); break;
            case Bytecodes.ISTORE             : do_store(stream.readLocalIndex(), CiKind.Int); break;
            case Bytecodes.ISTORE_0           : do_store(0, CiKind.Int); break;
            case Bytecodes.ISTORE_1           : do_store(1, CiKind.Int); break;
            case Bytecodes.ISTORE_2           : do_store(2, CiKind.Int); break;
            case Bytecodes.ISTORE_3           : do_store(3, CiKind.Int); break;
            case Bytecodes.LLOAD              : do_load(stream.readLocalIndex(), CiKind.Long); break;
            case Bytecodes.LLOAD_0            : do_load(0, CiKind.Long); break;
            case Bytecodes.LLOAD_1            : do_load(1, CiKind.Long); break;
            case Bytecodes.LLOAD_2            : do_load(2, CiKind.Long); break;
            case Bytecodes.LLOAD_3            : do_load(3, CiKind.Long); break;
            case Bytecodes.LSTORE             : do_store(stream.readLocalIndex(), CiKind.Long); break;
            case Bytecodes.LSTORE_0           : do_store(0, CiKind.Long); break;
            case Bytecodes.LSTORE_1           : do_store(1, CiKind.Long); break;
            case Bytecodes.LSTORE_2           : do_store(2, CiKind.Long); break;
            case Bytecodes.LSTORE_3           : do_store(3, CiKind.Long); break;
            case Bytecodes.IFEQ               : do_branch(Bytecodes.IFEQ, stream.readBranchDest()); break;
            case Bytecodes.IFNE               : do_branch(Bytecodes.IFNE, stream.readBranchDest()); break;
            case Bytecodes.IFLE               : do_branch(Bytecodes.IFLE, stream.readBranchDest()); break;
            case Bytecodes.IFLT               : do_branch(Bytecodes.IFLT, stream.readBranchDest()); break;
            case Bytecodes.IFGE               : do_branch(Bytecodes.IFGE, stream.readBranchDest()); break;
            case Bytecodes.IFGT               : do_branch(Bytecodes.IFGT, stream.readBranchDest()); break;
            case Bytecodes.IF_ICMPEQ          : do_branch(Bytecodes.IF_ICMPEQ, stream.readBranchDest()); break;
            case Bytecodes.IF_ICMPNE          : do_branch(Bytecodes.IF_ICMPNE, stream.readBranchDest()); break;
            case Bytecodes.IF_ICMPGE          : do_branch(Bytecodes.IF_ICMPGE, stream.readBranchDest()); break;
            case Bytecodes.IF_ICMPGT          : do_branch(Bytecodes.IF_ICMPGT, stream.readBranchDest()); break;
            case Bytecodes.IF_ICMPLE          : do_branch(Bytecodes.IF_ICMPLE, stream.readBranchDest()); break;
            case Bytecodes.IF_ICMPLT          : do_branch(Bytecodes.IF_ICMPLT, stream.readBranchDest()); break;
            case Bytecodes.IF_ACMPEQ          : do_branch(Bytecodes.IF_ACMPEQ, stream.readBranchDest()); break;
            case Bytecodes.IF_ACMPNE          : do_branch(Bytecodes.IF_ACMPNE, stream.readBranchDest()); break;
            case Bytecodes.IFNULL             : do_branch(Bytecodes.IFNULL, stream.readBranchDest()); break;
            case Bytecodes.IFNONNULL          : do_branch(Bytecodes.IFNONNULL, stream.readBranchDest()); break;
            case Bytecodes.GOTO               : do_branch(Bytecodes.GOTO, stream.readBranchDest()); break;
            case Bytecodes.GOTO_W             : do_branch(Bytecodes.GOTO_W, stream.readFarBranchDest()); break;
            case Bytecodes.GETFIELD           : do_fieldAccess(GETFIELDS, stream.readCPI()); break;
            case Bytecodes.GETSTATIC          : do_fieldAccess(GETSTATICS, stream.readCPI()); break;
            case Bytecodes.PUTFIELD           : do_fieldAccess(PUTFIELDS, stream.readCPI()); break;
            case Bytecodes.PUTSTATIC          : do_fieldAccess(PUTSTATICS, stream.readCPI()); break;
            case Bytecodes.ANEWARRAY          : do_anewarray(stream.readCPI()); break;
            case Bytecodes.CHECKCAST          : do_checkcast(stream.readCPI()); break;
            case Bytecodes.INSTANCEOF         : do_instanceof(stream.readCPI()); break;
            case Bytecodes.BIPUSH             : do_iconst(stream.readByte()); break;
            case Bytecodes.SIPUSH             : do_iconst(stream.readShort()); break;
            case Bytecodes.NEW                : do_new(stream.readCPI()); break;
            case Bytecodes.INVOKESPECIAL      : do_invokespecial(stream.readCPI()); break;
            case Bytecodes.INVOKESTATIC       : do_invokestatic(stream.readCPI()); break;
            case Bytecodes.INVOKEVIRTUAL      : do_invokevirtual(stream.readCPI()); break;
            case Bytecodes.INVOKEINTERFACE    : do_invokeinterface(stream.readCPI()); break;
            case Bytecodes.NEWARRAY           : do_newarray(stream.readLocalIndex()); break;
            case Bytecodes.LDC                : do_ldc(stream.readCPI()); break;
            case Bytecodes.LDC_W              : do_ldc(stream.readCPI()); break;
            case Bytecodes.LDC2_W             : do_ldc(stream.readCPI()); break;
            case Bytecodes.TABLESWITCH        : do_tableswitch(); break;
            case Bytecodes.LOOKUPSWITCH       : do_lookupswitch(); break;
            case Bytecodes.IINC               : do_iinc(stream.readLocalIndex(), stream.readIncrement()); break;
            case Bytecodes.MULTIANEWARRAY     : do_multianewarray(stream.readCPI(), stream.readUByte(stream.currentBCI() + 3)); break;


            case Bytecodes.UNSAFE_CAST        : break;
            case Bytecodes.WLOAD              : do_load(stream.readLocalIndex(), CiKind.Word); break;
            case Bytecodes.WLOAD_0            : do_load(0, CiKind.Word); break;
            case Bytecodes.WLOAD_1            : do_load(1, CiKind.Word); break;
            case Bytecodes.WLOAD_2            : do_load(2, CiKind.Word); break;
            case Bytecodes.WLOAD_3            : do_load(3, CiKind.Word); break;
            case Bytecodes.WSTORE             : do_store(stream.readLocalIndex(), CiKind.Word); break;
            case Bytecodes.WSTORE_0           : do_store(0, CiKind.Word); break;
            case Bytecodes.WSTORE_1           : do_store(1, CiKind.Word); break;
            case Bytecodes.WSTORE_2           : do_store(2, CiKind.Word); break;
            case Bytecodes.WSTORE_3           : do_store(3, CiKind.Word); break;

            case Bytecodes.WCONST_0           : if (Word.size() == 8) do_lconst(0); else do_iconst(0); break;
            case Bytecodes.WDIV               : do_wdiv(); break;
            case Bytecodes.WDIVI              : do_wdivi(); break;
            case Bytecodes.WREM               : do_wrem(); break;
            case Bytecodes.WREMI              : do_wremi(); break;

            case Bytecodes.INFOPOINT: {
                opcode = opcode | (stream.readUByte(stream.currentBCI() + 1) << 16);
                if (opcode == Bytecodes.UNCOMMON_TRAP) {
                    do_uncommonTrap();
                    break;
                } else if (opcode == Bytecodes.HERE) {
                    do_here();
                    break;
                } else {
                    throw new CiBailout("Unsupported opcode" + errorSuffix());
                }
            }

            case Bytecodes.MEMBAR:
            case Bytecodes.PCMPSWP:
            case Bytecodes.PGET:
            case Bytecodes.PSET:
            case Bytecodes.PREAD:
            case Bytecodes.PWRITE: {
                opcode = opcode | (stream.readCPI() << 8);
                switch (opcode) {
                    case Bytecodes.PREAD_BYTE         : do_pread_byte(); break;
                    case Bytecodes.PREAD_CHAR         : do_pread_char(); break;
                    case Bytecodes.PREAD_SHORT        : do_pread_short(); break;
                    case Bytecodes.PREAD_INT          : do_pread_int(); break;
                    case Bytecodes.PREAD_LONG         : do_pread_long(); break;
                    case Bytecodes.PREAD_FLOAT        : do_pread_float(); break;
                    case Bytecodes.PREAD_DOUBLE       : do_pread_double(); break;
                    case Bytecodes.PREAD_WORD         : do_pread_word(); break;
                    case Bytecodes.PREAD_REFERENCE    : do_pread_reference(); break;

                    case Bytecodes.PREAD_BYTE_I       : do_pread_byte_i(); break;
                    case Bytecodes.PREAD_CHAR_I       : do_pread_char_i(); break;
                    case Bytecodes.PREAD_SHORT_I      : do_pread_short_i(); break;
                    case Bytecodes.PREAD_INT_I        : do_pread_int_i(); break;
                    case Bytecodes.PREAD_LONG_I       : do_pread_long_i(); break;
                    case Bytecodes.PREAD_FLOAT_I      : do_pread_float_i(); break;
                    case Bytecodes.PREAD_DOUBLE_I     : do_pread_double_i(); break;
                    case Bytecodes.PREAD_WORD_I       : do_pread_word_i(); break;
                    case Bytecodes.PREAD_REFERENCE_I  : do_pread_reference_i(); break;

                    case Bytecodes.PWRITE_BYTE        : do_pwrite_byte(); break;
                    case Bytecodes.PWRITE_SHORT       : do_pwrite_short(); break;
                    case Bytecodes.PWRITE_INT         : do_pwrite_int(); break;
                    case Bytecodes.PWRITE_LONG        : do_pwrite_long(); break;
                    case Bytecodes.PWRITE_FLOAT       : do_pwrite_float(); break;
                    case Bytecodes.PWRITE_DOUBLE      : do_pwrite_double(); break;
                    case Bytecodes.PWRITE_WORD        : do_pwrite_word(); break;
                    case Bytecodes.PWRITE_REFERENCE   : do_pwrite_reference(); break;

                    case Bytecodes.PWRITE_BYTE_I      : do_pwrite_byte_i(); break;
                    case Bytecodes.PWRITE_SHORT_I     : do_pwrite_short_i(); break;
                    case Bytecodes.PWRITE_INT_I       : do_pwrite_int_i(); break;
                    case Bytecodes.PWRITE_LONG_I      : do_pwrite_long_i(); break;
                    case Bytecodes.PWRITE_FLOAT_I     : do_pwrite_float_i(); break;
                    case Bytecodes.PWRITE_DOUBLE_I    : do_pwrite_double_i(); break;
                    case Bytecodes.PWRITE_WORD_I      : do_pwrite_word_i(); break;
                    case Bytecodes.PWRITE_REFERENCE_I : do_pwrite_reference_i(); break;

                    case Bytecodes.PGET_BYTE          : do_pget_byte(); break;
                    case Bytecodes.PGET_CHAR          : do_pget_char(); break;
                    case Bytecodes.PGET_SHORT         : do_pget_short(); break;
                    case Bytecodes.PGET_INT           : do_pget_int(); break;
                    case Bytecodes.PGET_LONG          : do_pget_long(); break;
                    case Bytecodes.PGET_FLOAT         : do_pget_float(); break;
                    case Bytecodes.PGET_DOUBLE        : do_pget_double(); break;
                    case Bytecodes.PGET_WORD          : do_pget_word(); break;
                    case Bytecodes.PGET_REFERENCE     : do_pget_reference(); break;

                    case Bytecodes.PSET_BYTE          : do_pset_byte(); break;
                    case Bytecodes.PSET_SHORT         : do_pset_short(); break;
                    case Bytecodes.PSET_INT           : do_pset_int(); break;
                    case Bytecodes.PSET_LONG          : do_pset_long(); break;
                    case Bytecodes.PSET_FLOAT         : do_pset_float(); break;
                    case Bytecodes.PSET_DOUBLE        : do_pset_double(); break;
                    case Bytecodes.PSET_WORD          : do_pset_word(); break;
                    case Bytecodes.PSET_REFERENCE     : do_pset_reference(); break;

                    case Bytecodes.PCMPSWP_INT        : do_pcmpswp_int(); break;
                    case Bytecodes.PCMPSWP_WORD       : do_pcmpswp_word(); break;
                    case Bytecodes.PCMPSWP_REFERENCE  : do_pcmpswp_reference(); break;
                    case Bytecodes.PCMPSWP_INT_I      : do_pcmpswp_int_i(); break;
                    case Bytecodes.PCMPSWP_WORD_I     : do_pcmpswp_word_i(); break;
                    case Bytecodes.PCMPSWP_REFERENCE_I: do_pcmpswp_reference_i(); break;

                    case Bytecodes.MEMBAR_LOAD_LOAD   : do_membar_load_load(); break;
                    case Bytecodes.MEMBAR_LOAD_STORE  : do_membar_load_store(); break;
                    case Bytecodes.MEMBAR_STORE_LOAD  : do_membar_store_load(); break;
                    case Bytecodes.MEMBAR_STORE_STORE : do_membar_store_store(); break;

                    default                           : throw new CiBailout("Unsupported opcode" + errorSuffix());
                }
                break;
            }

            case Bytecodes.MOV_I2F            : do_mov_i2f(); break;
            case Bytecodes.MOV_F2I            : do_mov_f2i(); break;
            case Bytecodes.MOV_L2D            : do_mov_l2d(); break;
            case Bytecodes.MOV_D2L            : do_mov_d2l(); break;


            case Bytecodes.WRETURN            : do_return(WRETURN, WRETURN$unlock); break;
            case Bytecodes.PAUSE              : do_pause(); break;
            case Bytecodes.LSB                : do_lsb(); break;
            case Bytecodes.MSB                : do_msb(); break;

            case Bytecodes.RET                :
            case Bytecodes.JSR_W              :
            case Bytecodes.JSR                : throw new UnsupportedSubroutineException(opcode, stream.currentBCI());

            case Bytecodes.READREG            :
            case Bytecodes.WRITEREG           :
            case Bytecodes.FLUSHW             :
            case Bytecodes.ALLOCA             :
            case Bytecodes.STACKHANDLE        :
            case Bytecodes.JNICALL            :
            default                           : throw new CiBailout("Unsupported opcode" + errorSuffix());
            // Checkstyle: resume
        }
    }

    protected void do_uncommonTrap() {
        // Use a safepoint so that this position is not overlapping
        // with the subsequent instruction. In all other senses,
        // an uncommon trap is essentially a nop in baseline code.
        int pos = buf.position();
        byte[] safepointCode = vm().safepointPoll.code;
        buf.emitBytes(safepointCode, 0, safepointCode.length);
        safepointsBuilder.addSafepoint(stream.currentBCI(), Safepoints.make(pos), null);
    }

    protected void do_here() {
        emit(HERE);
    }

    void do_profileMethodEntry() {
        if (methodProfileBuilder != null) {
            methodProfileBuilder.addEntryCounter(MethodInstrumentation.initialEntryCount);
            if (method.isStatic()) {
                start(PROFILE_STATIC_METHOD_ENTRY);
                assignObject(0, "mpo", methodProfileBuilder.methodProfileObject());
                finish();
            } else {
                start(PROFILE_NONSTATIC_METHOD_ENTRY);
                assignObject(0, "mpo", methodProfileBuilder.methodProfileObject());
                loadObject(1, "rcvr", 0);
                finish();
            }
        }
    }

    protected void do_methodTraceEntry() {
        if (T1XOptions.TraceMethods) {
            start(TRACE_METHOD_ENTRY);
            assignObject(0, "method", method.toString());
            finish();
        }
    }

    void do_synchronizedMethodAcquire() {
        if (method.isSynchronized()) {
            start(LOCK);
            if (method.isStatic()) {
                assignObject(0, "object", method.holder().javaClass());
            } else {
                loadObject(0, "object", 0);
                storeObject(template.sig.in[0].reg, synchronizedReceiver);
            }
            finish();
            syncMethodStartPos = buf.position();
        }
    }

    void do_synchronizedMethodHandler(ClassMethodActor method, int endBCI) {
        if (method.isSynchronized()) {
            syncMethodHandlerPos = buf.position();
            start(UNLOCK);
            if (method.isStatic()) {
                assignObject(0, "object", method.holder().javaClass());
            } else {
                loadObject(0, "object", synchronizedReceiver);
            }
            finish();

            syncMethodEndPos = buf.position();
            emit(RETHROW_EXCEPTION);
        }
    }

    protected void do_oconst(Object value) {
        assignObject(scratch, value);
        incStack(1);
        pokeObject(scratch, 0);
    }

    protected void do_iconst(int value) {
        assignInt(scratch, value);
        incStack(1);
        pokeInt(scratch, 0);
    }

    protected void do_dconst(double value) {
        assignLong(scratch, Intrinsics.doubleToLong(value));
        incStack(2);
        pokeLong(scratch, 0);
    }

    protected void do_fconst(float value) {
        assignInt(scratch, Intrinsics.floatToInt(value));
        incStack(1);
        pokeInt(scratch, 0);
    }

    protected void do_lconst(long value) {
        assignLong(scratch, value);
        incStack(2);
        pokeLong(scratch, 0);
    }

    protected void do_load(int index, CiKind kind) {
        switch(kind) {
            case Int:
            case Float:
                loadInt(scratch, index);
                incStack(1);
                pokeInt(scratch, 0);
                break;
            case Word:
            case Object:
                loadWord(scratch, index);
                incStack(1);
                pokeWord(scratch, 0);
                break;
            case Long:
            case Double:
                loadLong(scratch, index);
                incStack(2);
                pokeLong(scratch, 0);
                break;
            default:
                throw new InternalError("Unexpected kind: " + kind);
        }
    }

    protected void do_store(int index, CiKind kind) {
        switch(kind) {
            case Int:
            case Float:
                peekInt(scratch, 0);
                decStack(1);
                storeInt(scratch, index);
                break;
            case Word:
            case Object:
                peekWord(scratch, 0);
                decStack(1);
                storeWord(scratch, index);
                break;
            case Long:
            case Double:
                peekLong(scratch, 0);
                decStack(2);
                storeLong(scratch, index);
                break;
            default:
                throw new InternalError("Unexpected kind: " + kind);
        }
    }

    /**
     * Emit template for a bytecode operating on a (static or dynamic) field.
     * @param index Index to the field ref constant.
     * @param template one of getfield, putfield, getstatic, putstatic
     */
    protected void do_fieldAccess(EnumMap<CiKind, T1XTemplateTag> tags, int index) {
        FieldRefConstant fieldRefConstant = cp.fieldAt(index);
        CiKind fieldKind = fieldRefConstant.type(cp).toKind().ciKind;
        T1XTemplateTag tag = tags.get(fieldKind);
        if (fieldRefConstant.isResolvableWithoutClassLoading(cp)) {
            try {
                FieldActor fieldActor = fieldRefConstant.resolve(cp, index);
                do_preVolatileFieldAccess(tag, fieldActor);
                if (fieldActor.isStatic()) {
                    if (fieldActor.holder().isInitialized()) {
                        start(tag.initialized);
                        assignObject(0, "staticTuple", fieldActor.holder().staticTuple());
                        assignInt(1, "offset", fieldActor.offset());
                        finish();
                        do_postVolatileFieldAccess(tag, fieldActor);
                        return;
                    }
                } else {
                    start(tag.resolved);
                    assignInt(1, "offset", fieldActor.offset());
                    finish();
                    do_postVolatileFieldAccess(tag, fieldActor);
                    return;
                }
            } catch (LinkageError e) {
                // This should not happen since the field ref constant is resolvable without class loading (i.e., it
                // has already been resolved). If it were to happen, the safe thing to do is to fall off to the
                // unresolved case.
            }
        }
        start(tag);
        assignObject(0, "guard", cp.makeResolutionGuard(index));
        finish();
    }

    protected void do_preVolatileFieldAccess(T1XTemplateTag tag, FieldActor fieldActor) {
    }

    protected void do_postVolatileFieldAccess(T1XTemplateTag tag, FieldActor fieldActor) {
    }

    protected void do_anewarray(int index) {
        T1XTemplateTag tag;
        ClassConstant classConstant = cp.classAt(index);
        Object arrayType;
        if (classConstant.isResolvableWithoutClassLoading(cp)) {
            tag = ANEWARRAY$resolved;
            ClassActor resolvedClassActor = classConstant.resolve(cp, index);
            arrayType = ArrayClassActor.forComponentClassActor(resolvedClassActor);
        } else {
            tag = ANEWARRAY;
            arrayType = cp.makeResolutionGuard(index);
        }
        start(tag);
        assignObject(0, "arrayType", arrayType);
        finish();
    }

    protected void do_iinc(int index, int increment) {
        loadInt(scratch, index);
        adjustReg(scratch, increment);
        storeInt(scratch, index);
    }

    protected void do_return(T1XTemplateTag tag, T1XTemplateTag tagUnlock) {
        if (method.holder() == ClassRegistry.OBJECT) {
            start(RETURN$registerFinalizer);
            loadObject(0, "object", 0);
            finish();
        } else if (method.isSynchronized()) {
            start(tagUnlock);
            if (method.isStatic()) {
                assignObject(0, "object", method.holder().javaClass());
            } else {
                loadObject(0, "object", synchronizedReceiver);
            }
            finish();
        } else {
            emit(tag);
        }

        emitEpilogue();
    }

    protected void do_branch(int opcode, int targetBCI) {
        int bci = stream.currentBCI();
        startBlock(targetBCI);

        if (bci >= targetBCI && methodProfileBuilder != null) {
            // Profiling of backward branches.
            // Ideally, we'd like to emit a safepoint at the target of a backward branch.
            // However, that would require at least one extra pass to determine where
            // the backward branches are. Instead, we simply emit a safepoint at the source of
            // a backward branch. This means the cost of the safepoint is taken even if
            // the backward branch is not taken but that cost should not be noticeable.
            start(PROFILE_BACKWARD_BRANCH);
            assignObject(0, "mpo", methodProfileBuilder.methodProfileObject());
            finish();

            // Note also that the safepoint must be placed before conditional
            // test instruction(s) to avoid affecting the condition flags
            // with the safepoint poll instruction(s).
            int pos = buf.position();
            byte[] safepointCode = vm().safepointPoll.code;
            buf.emitBytes(safepointCode, 0, safepointCode.length);
            safepointsBuilder.addSafepoint(bci, Safepoints.make(pos), null);
        }

        branch(opcode, targetBCI, bci);
    }

    protected void finishCall(T1XTemplateTag tag, CiKind returnKind, int safepoint, ClassMethodActor directCallee) {
        safepointsBuilder.addSafepoint(stream.currentBCI(), safepoint, directCallee);

        if (!returnKind.isVoid()) {
            incStack(returnKind.jvmSlots);
            CiRegister reg = vm().registerConfigs.standard.getReturnRegister(returnKind);
            switch (returnKind) {
                case Float:
                    pokeFloat(reg, 0);
                    break;
                case Long:
                    pokeLong(reg, 0);
                    break;
                case Double:
                    pokeDouble(reg, 0);
                    break;
                case Word:
                    pokeWord(reg, 0);
                    break;
                case Object:
                    pokeObject(reg, 0);
                    break;
                default:
                    throw new InternalError("Unexpected return kind: " + returnKind);
            }
        }

    }

    /*
     * The following three methods exist to be overridden by the VMA extension.
     * They permit flexibility in the form of the templates for the INVOKE bytecodes
     * without hard-wiring in additional forms. In particular, they allow access to the
     * associated {@link MethodActor} in all situations.
     */

    protected void assignInvokeVirtualTemplateParameters(VirtualMethodActor virtualMethodActor, int receiverStackIndex) {
        assignInt(0, "vTableIndex", virtualMethodActor.vTableIndex());
        peekObject(1, "receiver", receiverStackIndex);
    }

    protected void do_invokespecial_resolved(T1XTemplateTag tag, VirtualMethodActor virtualMethodActor, int receiverStackIndex) {
        peekObject(scratch, receiverStackIndex);
        nullCheck(scratch);
    }

    protected void do_invokestatic_resolved(T1XTemplateTag tag, StaticMethodActor staticMethodActor) {
    }

    protected void do_invokevirtual(int index) {
        ClassMethodRefConstant classMethodRef = cp.classMethodAt(index);
        SignatureDescriptor signature = classMethodRef.signature(cp);
        CiKind kind = invokeKind(signature);
        T1XTemplateTag tag = INVOKEVIRTUALS.get(kind);
        int receiverStackIndex = receiverStackIndex(signature);
        try {
            if (classMethodRef.isResolvableWithoutClassLoading(cp)) {
                try {
                    VirtualMethodActor virtualMethodActor = classMethodRef.resolveVirtual(cp, index);
                    if (virtualMethodActor.isPrivate() || virtualMethodActor.isFinal()) {
                        // this is an invokevirtual to a private or final method, treat it like invokespecial
                        do_invokespecial_resolved(tag, virtualMethodActor, receiverStackIndex);

                        int safepoint = callDirect();
                        finishCall(tag, kind, safepoint, virtualMethodActor);
                        return;
                    } else {
                        // emit an unprofiled virtual dispatch
                        start(tag.resolved);
                        CiRegister target = template.sig.out.reg;
                        assignInvokeVirtualTemplateParameters(virtualMethodActor, receiverStackIndex);
                        finish();
                        int safepoint = callIndirect(target, receiverStackIndex);
                        finishCall(tag, kind, safepoint, null);
                    }
                    return;
                } catch (LinkageError e) {
                    // fall through
                }
            }
        } catch (LinkageError error) {
            // Fall back on unresolved template that will cause the error to be rethrown at runtime.
        }
        start(tag);
        CiRegister target = template.sig.out.reg;
        assignObject(0, "guard", cp.makeResolutionGuard(index));
        peekObject(1, "receiver", receiverStackIndex);
        finish();

        int safepoint = callIndirect(target, receiverStackIndex);
        finishCall(tag, kind, safepoint, null);
    }

    protected void do_invokeinterface(int index) {
        InterfaceMethodRefConstant interfaceMethodRef = cp.interfaceMethodAt(index);
        SignatureDescriptor signature = interfaceMethodRef.signature(cp);
        CiKind kind = invokeKind(signature);
        T1XTemplateTag tag = INVOKEINTERFACES.get(kind);
        int receiverStackIndex = receiverStackIndex(signature);
        try {
            if (interfaceMethodRef.isResolvableWithoutClassLoading(cp)) {
                try {
                    MethodActor interfaceMethod = interfaceMethodRef.resolve(cp, index);
                    start(tag.resolved);
                    CiRegister target = template.sig.out.reg;
                    assignObject(0, "methodActor", interfaceMethod);
                    peekObject(1, "receiver", receiverStackIndex);
                    finish();

                    int safepoint = callIndirect(target, receiverStackIndex);
                    finishCall(tag, kind, safepoint, null);
                    return;
                } catch (LinkageError e) {
                    // fall through
                }
            }
        } catch (LinkageError error) {
            // Fall back on unresolved template that will cause the error to be rethrown at runtime.
        }
        start(tag);
        CiRegister target = template.sig.out.reg;
        assignObject(0, "guard", cp.makeResolutionGuard(index));
        peekObject(1, "receiver", receiverStackIndex);
        finish();

        int safepoint = callIndirect(target, receiverStackIndex);
        finishCall(tag, kind, safepoint, null);
    }

    protected void do_invokespecial(int index) {
        ClassMethodRefConstant classMethodRef = cp.classMethodAt(index);
        CiKind kind = invokeKind(classMethodRef.signature(cp));
        SignatureDescriptor signature = classMethodRef.signature(cp);
        T1XTemplateTag tag = INVOKESPECIALS.get(kind);
        int receiverStackIndex = receiverStackIndex(signature);
        try {
            if (classMethodRef.isResolvableWithoutClassLoading(cp)) {
                VirtualMethodActor virtualMethodActor = classMethodRef.resolveVirtual(cp, index);
                do_invokespecial_resolved(tag, virtualMethodActor, receiverStackIndex);

                int safepoint = callDirect();
                finishCall(tag, kind, safepoint, virtualMethodActor);
                return;
            }
        } catch (LinkageError error) {
            // Fall back on unresolved template that will cause the error to be rethrown at runtime.
        }
        start(tag);
        CiRegister target = template.sig.out.reg;
        assignObject(0, "guard", cp.makeResolutionGuard(index));
        peekObject(1, "receiver", receiverStackIndex);
        finish();

        int safepoint = callIndirect(target, receiverStackIndex);
        finishCall(tag, kind, safepoint, null);
    }

    protected void do_invokestatic(int index) {
        ClassMethodRefConstant classMethodRef = cp.classMethodAt(index);
        CiKind kind = invokeKind(classMethodRef.signature(cp));
        T1XTemplateTag tag = INVOKESTATICS.get(kind);
        try {
            if (classMethodRef.isResolvableWithoutClassLoading(cp)) {
                StaticMethodActor staticMethodActor = classMethodRef.resolveStatic(cp, index);
                if (staticMethodActor.holder().isInitialized()) {
                    do_invokestatic_resolved(tag, staticMethodActor);

                    int safepoint = callDirect();
                    finishCall(tag, kind, safepoint, staticMethodActor);
                    return;
                }
            }
        } catch (LinkageError error) {
            // Fall back on unresolved template that will cause the error to be rethrown at runtime.
        }
        start(tag);
        CiRegister target = template.sig.out.reg;
        assignObject(0, "guard", cp.makeResolutionGuard(index));
        finish();

        int safepoint = callIndirect(target, -1);
        finishCall(tag, kind, safepoint, null);
    }

    protected void do_instanceof(int cpi) {
        ClassConstant classConstant = cp.classAt(cpi);
        if (classConstant.isResolvableWithoutClassLoading(cp)) {
            start(INSTANCEOF$resolved);
            ClassActor resolvedClassActor = classConstant.resolve(cp, cpi);
            assignObject(0, "classActor", resolvedClassActor);
            finish();
        } else {
            start(INSTANCEOF);
            assignObject(0, "guard", cp.makeResolutionGuard(cpi));
            finish();
        }
    }

    protected void do_checkcast(int cpi) {
        ClassConstant classConstant = cp.classAt(cpi);
        if (classConstant.isResolvableWithoutClassLoading(cp)) {
            start(CHECKCAST$resolved);
            assignObject(0, "classActor", classConstant.resolve(cp, cpi));
            finish();
        } else {
            start(CHECKCAST);
            assignObject(0, "guard", cp.makeResolutionGuard(cpi));
            finish();
        }
    }

    protected void do_multianewarray(int index, int numberOfDimensions) {
        CiRegister lengths;
        {
            start(CREATE_MULTIANEWARRAY_DIMENSIONS);
            assignWordReg(0, "sp", sp);
            assignInt(1, "n", numberOfDimensions);
            lengths = template.sig.out.reg;
            finish();
            decStack(numberOfDimensions);
        }
        ClassConstant classRef = cp.classAt(index);
        if (classRef.isResolvableWithoutClassLoading(cp)) {
            start(MULTIANEWARRAY$resolved);
            ClassActor arrayClassActor = classRef.resolve(cp, index);
            assert arrayClassActor.isArrayClass();
            assert arrayClassActor.numberOfDimensions() >= numberOfDimensions : "dimensionality of array class constant smaller that dimension operand";
            assignObject(0, "arrayClassActor", arrayClassActor);
            assignObjectReg(1, "lengths", lengths);
            finish();
        } else {
            // Unresolved case
            start(MULTIANEWARRAY);
            assignObject(0, "guard", cp.makeResolutionGuard(index));
            assignObjectReg(1, "lengths", lengths);
            finish();
        }
    }

    protected void do_pop() {
        decStack(1);
    }

    protected void do_pop2() {
        decStack(2);
    }

    protected void do_dup() {
        incStack(1);
        peekWord(scratch, 1);
        pokeWord(scratch, 0);
    }

    protected void do_dup_x1() {
        incStack(1);
        // value1
        peekWord(scratch, 1);
        pokeWord(scratch, 0);

        // value2
        peekWord(scratch, 2);
        pokeWord(scratch, 1);

        // value1
        peekWord(scratch, 0);
        pokeWord(scratch, 2);
    }

    protected void do_dup_x2() {
        incStack(1);
        // value1
        peekWord(scratch, 1);
        pokeWord(scratch, 0);

        // value2
        peekWord(scratch, 2);
        pokeWord(scratch, 1);

        // value3
        peekWord(scratch, 3);
        pokeWord(scratch, 2);

        // value1
        peekWord(scratch, 0);
        pokeWord(scratch, 3);
    }

    protected void do_dup2() {
        incStack(2);
        peekWord(scratch, 3);
        pokeWord(scratch, 1);
        peekWord(scratch, 2);
        pokeWord(scratch, 0);
    }

    protected void do_dup2_x1() {
        incStack(2);
        // value1
        peekWord(scratch, 2);
        pokeWord(scratch, 0);

        // value2
        peekWord(scratch, 3);
        pokeWord(scratch, 1);

        // value3
        peekWord(scratch, 4);
        pokeWord(scratch, 2);

        // value1
        peekWord(scratch, 0);
        pokeWord(scratch, 3);

        // value2
        peekWord(scratch, 1);
        pokeWord(scratch, 4);
    }

    protected void do_dup2_x2() {
        incStack(2);
        // value1
        peekWord(scratch, 2);
        pokeWord(scratch, 0);

        // value2
        peekWord(scratch, 3);
        pokeWord(scratch, 1);

        // value3
        peekWord(scratch, 4);
        pokeWord(scratch, 2);

        // value4
        peekWord(scratch, 5);
        pokeWord(scratch, 3);

        // value1
        peekWord(scratch, 0);
        pokeWord(scratch, 4);

        // value2
        peekWord(scratch, 1);
        pokeWord(scratch, 5);
    }

    protected void do_swap() {
        peekWord(scratch, 0);
        peekWord(scratch2, 1);
        pokeWord(scratch, 1);
        pokeWord(scratch2, 0);
    }

    protected void do_ineg() {
        start(INEG);
        assignInt(1, "zero", 0);
        finish();
    }

    protected void do_lneg() {
        start(LNEG);
        assignLong(1, "zero", 0L);
        finish();
    }

    protected void do_dneg() {
        start(DNEG);
        assignDouble(1, "zero", 0d);
        finish();
    }

    protected void do_fneg() {
        start(FNEG);
        assignFloat(1, "zero", 0f);
        finish();
    }

    protected void do_ldc(int index) {
        PoolConstant constant = cp.at(index);
        switch (constant.tag()) {
            case CLASS: {
                ClassConstant classConstant = (ClassConstant) constant;
                if (classConstant.isResolvableWithoutClassLoading(cp)) {
                    Object mirror = ((ClassActor) classConstant.value(cp, index).asObject()).javaClass();
                    incStack(1);
                    assignObject(scratch, mirror);
                    pokeObject(scratch, 0);
                } else {
                    start(LDC$reference);
                    assignObject(0, "guard", cp.makeResolutionGuard(index));
                    finish();
                }
                break;
            }
            case INTEGER: {
                IntegerConstant integerConstant = (IntegerConstant) constant;
                do_iconst(integerConstant.value());
                break;
            }
            case LONG: {
                LongConstant longConstant = (LongConstant) constant;
                do_lconst(longConstant.value());
                break;
            }
            case FLOAT: {
                FloatConstant floatConstant = (FloatConstant) constant;
                do_fconst(floatConstant.value());
                break;
            }
            case DOUBLE: {
                DoubleConstant doubleConstant = (DoubleConstant) constant;
                do_dconst(doubleConstant.value());
                break;
            }
            case STRING: {
                StringConstant stringConstant = (StringConstant) constant;
                do_oconst(stringConstant.value);
                break;
            }
            default: {
                assert false : "ldc for unexpected constant tag: " + constant.tag();
                break;
            }
        }
    }

    protected void do_new(int index) {
        ClassConstant classRef = cp.classAt(index);
        if (classRef.isResolvableWithoutClassLoading(cp)) {
            ClassActor classActor = classRef.resolve(cp, index);
            if (classActor.isInitialized()) {
                start(NEW$init);
                assignObject(0, "classActor", classActor);
                finish();
                return;
            }
        }
        start(NEW);
        assignObject(0, "guard", cp.makeResolutionGuard(index));
        finish();
    }

    protected void do_newarray(int tag) {
        start(NEWARRAY);
        Kind arrayElementKind = Kind.fromNewArrayTag(tag);
        assignObject(0, "kind", arrayElementKind);
        finish();
    }

    protected abstract void do_tableswitch();

    protected abstract void do_lookupswitch();

    protected void do_ddiv() {
        emit(DDIV);
    }

    protected void do_dcmpl() {
        emit(DCMPL);
    }

    protected void do_dcmpg() {
        emit(DCMPG);
    }

    protected void do_dastore() {
        emit(DASTORE);
    }

    protected void do_daload() {
        emit(DALOAD);
    }

    protected void do_dadd() {
        emit(DADD);
    }

    protected void do_d2l() {
        emit(D2L);
    }

    protected void do_d2i() {
        emit(D2I);
    }

    protected void do_d2f() {
        emit(D2F);
    }

    protected void do_castore() {
        emit(CASTORE);
    }

    protected void do_caload() {
        emit(CALOAD);
    }

    protected void do_bastore() {
        emit(BASTORE);
    }

    protected void do_baload() {
        emit(BALOAD);
    }

    protected void do_athrow() {
        emit(ATHROW);
    }

    protected void do_arraylength() {
        emit(ARRAYLENGTH);
    }

    protected void do_aastore() {
        emit(AASTORE);
    }

    protected void do_aaload() {
        emit(AALOAD);
    }

    protected void do_dmul() {
        emit(DMUL);
    }

    protected void do_drem() {
        emit(DREM);
    }

    protected void do_dsub() {
        emit(DSUB);
    }

    protected void do_f2d() {
        emit(F2D);
    }

    protected void do_f2i() {
        emit(F2I);
    }

    protected void do_f2l() {
        emit(F2L);
    }

    protected void do_fadd() {
        emit(FADD);
    }

    protected void do_faload() {
        emit(FALOAD);
    }

    protected void do_fastore() {
        emit(FASTORE);
    }

    protected void do_fcmpg() {
        emit(FCMPG);
    }

    protected void do_fcmpl() {
        emit(FCMPL);
    }

    protected void do_fdiv() {
        emit(FDIV);
    }

    protected void do_fmul() {
        emit(FMUL);
    }

    protected void do_frem() {
        emit(FREM);
    }

    protected void do_fsub() {
        emit(FSUB);
    }

    protected void do_i2b() {
        emit(I2B);
    }

    protected void do_i2c() {
        emit(I2C);
    }

    protected void do_i2d() {
        emit(I2D);
    }

    protected void do_i2f() {
        emit(I2F);
    }

    protected void do_i2l() {
        emit(I2L);
    }

    protected void do_i2s() {
        emit(I2S);
    }

    protected void do_iadd() {
        emit(IADD);
    }

    protected void do_iaload() {
        emit(IALOAD);
    }

    protected void do_iand() {
        emit(IAND);
    }

    protected void do_iastore() {
        emit(IASTORE);
    }

    protected void do_idiv() {
        emit(IDIV);
    }

    protected void do_imul() {
        emit(IMUL);
    }

    protected void do_ior() {
        emit(IOR);
    }

    protected void do_irem() {
        emit(IREM);
    }

    protected void do_ishl() {
        emit(ISHL);
    }

    protected void do_ishr() {
        emit(ISHR);
    }

    protected void do_isub() {
        emit(ISUB);
    }

    protected void do_iushr() {
        emit(IUSHR);
    }

    protected void do_ixor() {
        emit(IXOR);
    }

    protected void do_l2d() {
        emit(L2D);
    }

    protected void do_l2f() {
        emit(L2F);
    }

    protected void do_l2i() {
        emit(L2I);
    }

    protected void do_ladd() {
        emit(LADD);
    }

    protected void do_laload() {
        emit(LALOAD);
    }

    protected void do_land() {
        emit(LAND);
    }

    protected void do_lastore() {
        emit(LASTORE);
    }

    protected void do_lcmp() {
        emit(LCMP);
    }

    protected void do_ldiv() {
        emit(LDIV);
    }

    protected void do_lmul() {
        emit(LMUL);
    }

    protected void do_lor() {
        emit(LOR);
    }

    protected void do_lrem() {
        emit(LREM);
    }

    protected void do_lshl() {
        emit(LSHL);
    }

    protected void do_lshr() {
        emit(LSHR);
    }

    protected void do_lsub() {
        emit(LSUB);
    }

    protected void do_lushr() {
        emit(LUSHR);
    }

    protected void do_lxor() {
        emit(LXOR);
    }

    protected void do_monitorenter() {
        emit(MONITORENTER);
    }

    protected void do_monitorexit() {
        emit(MONITOREXIT);
    }

    protected void do_saload() {
        emit(SALOAD);
    }

    protected void do_sastore() {
        emit(SASTORE);
    }

    protected void do_wdiv() {
        emit(WDIV);
    }

    protected void do_wdivi() {
        emit(WDIVI);
    }

    protected void do_wrem() {
        emit(WREM);
    }

    protected void do_wremi() {
        emit(WREMI);
    }

    protected void do_pread_byte() {
        emit(PREAD_BYTE);
    }

    protected void do_pread_char() {
        emit(PREAD_CHAR);
    }

    protected void do_pread_short() {
        emit(PREAD_SHORT);
    }

    protected void do_pread_int() {
        emit(PREAD_INT);
    }

    protected void do_pread_long() {
        emit(PREAD_LONG);
    }

    protected void do_pread_float() {
        emit(PREAD_FLOAT);
    }

    protected void do_pread_double() {
        emit(PREAD_DOUBLE);
    }

    protected void do_pread_word() {
        emit(PREAD_WORD);
    }

    protected void do_pread_reference() {
        emit(PREAD_REFERENCE);
    }

    protected void do_pread_byte_i() {
        emit(PREAD_BYTE_I);
    }

    protected void do_pread_char_i() {
        emit(PREAD_CHAR_I);
    }

    protected void do_pread_short_i() {
        emit(PREAD_SHORT_I);
    }

    protected void do_pread_int_i() {
        emit(PREAD_INT_I);
    }

    protected void do_pread_long_i() {
        emit(PREAD_LONG_I);
    }

    protected void do_pread_float_i() {
        emit(PREAD_FLOAT_I);
    }

    protected void do_pread_double_i() {
        emit(PREAD_DOUBLE_I);
    }

    protected void do_pread_word_i() {
        emit(PREAD_WORD_I);
    }

    protected void do_pread_reference_i() {
        emit(PREAD_REFERENCE_I);
    }

    protected void do_pwrite_byte() {
        emit(PWRITE_BYTE);
    }

    protected void do_pwrite_short() {
        emit(PWRITE_SHORT);
    }

    protected void do_pwrite_int() {
        emit(PWRITE_INT);
    }

    protected void do_pwrite_long() {
        emit(PWRITE_LONG);
    }

    protected void do_pwrite_float() {
        emit(PWRITE_FLOAT);
    }

    protected void do_pwrite_double() {
        emit(PWRITE_DOUBLE);
    }

    protected void do_pwrite_word() {
        emit(PWRITE_WORD);
    }

    protected void do_pwrite_reference() {
        emit(PWRITE_REFERENCE);
    }

    protected void do_pwrite_byte_i() {
        emit(PWRITE_BYTE_I);
    }

    protected void do_pwrite_short_i() {
        emit(PWRITE_SHORT_I);
    }

    protected void do_pwrite_int_i() {
        emit(PWRITE_INT_I);
    }

    protected void do_pwrite_long_i() {
        emit(PWRITE_LONG_I);
    }

    protected void do_pwrite_float_i() {
        emit(PWRITE_FLOAT_I);
    }

    protected void do_pwrite_double_i() {
        emit(PWRITE_DOUBLE_I);
    }

    protected void do_pwrite_word_i() {
        emit(PWRITE_WORD_I);
    }

    protected void do_pwrite_reference_i() {
        emit(PWRITE_REFERENCE_I);
    }

    protected void do_pget_byte() {
        emit(PGET_BYTE);
    }

    protected void do_pget_char() {
        emit(PGET_CHAR);
    }

    protected void do_pget_short() {
        emit(PGET_SHORT);
    }

    protected void do_pget_int() {
        emit(PGET_INT);
    }

    protected void do_pget_long() {
        emit(PGET_LONG);
    }

    protected void do_pget_float() {
        emit(PGET_FLOAT);
    }

    protected void do_pget_double() {
        emit(PGET_DOUBLE);
    }

    protected void do_pget_word() {
        emit(PGET_WORD);
    }

    protected void do_pget_reference() {
        emit(PGET_REFERENCE);
    }

    protected void do_pset_byte() {
        emit(PSET_BYTE);
    }

    protected void do_pset_short() {
        emit(PSET_SHORT);
    }

    protected void do_pset_int() {
        emit(PSET_INT);
    }

    protected void do_pset_long() {
        emit(PSET_LONG);
    }

    protected void do_pset_float() {
        emit(PSET_FLOAT);
    }

    protected void do_pset_double() {
        emit(PSET_DOUBLE);
    }

    protected void do_pset_word() {
        emit(PSET_WORD);
    }

    protected void do_pset_reference() {
        emit(PSET_REFERENCE);
    }

    protected void do_pcmpswp_int() {
        emit(PCMPSWP_INT);
    }

    protected void do_pcmpswp_word() {
        emit(PCMPSWP_WORD);
    }

    protected void do_pcmpswp_reference() {
        emit(PCMPSWP_REFERENCE);
    }

    protected void do_pcmpswp_int_i() {
        emit(PCMPSWP_INT_I);
    }

    protected void do_pcmpswp_word_i() {
        emit(PCMPSWP_WORD_I);
    }

    protected void do_pcmpswp_reference_i() {
        emit(PCMPSWP_REFERENCE_I);
    }

    protected void do_membar_load_load() {
        emit(MEMBAR_LOAD_LOAD);
    }

    protected void do_membar_load_store() {
        emit(MEMBAR_LOAD_STORE);
    }

    protected void do_membar_store_load() {
        emit(MEMBAR_STORE_LOAD);
    }

    protected void do_membar_store_store() {
        emit(MEMBAR_STORE_STORE);
    }

    protected void do_mov_i2f() {
        emit(MOV_I2F);
    }

    protected void do_mov_f2i() {
        emit(MOV_F2I);
    }

    protected void do_mov_l2d() {
        emit(MOV_L2D);
    }

    protected void do_mov_d2l() {
        emit(MOV_D2L);
    }

    protected void do_pause() {
        emit(PAUSE);
    }

    protected void do_lsb() {
        emit(LSB);
    }

    protected void do_msb() {
        emit(MSB);
    }
}
