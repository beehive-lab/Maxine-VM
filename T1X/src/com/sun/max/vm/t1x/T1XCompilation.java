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
package com.sun.max.vm.t1x;

import static com.sun.c1x.target.amd64.AMD64.*;
import static com.sun.cri.ci.CiRegister.*;
import static com.sun.max.platform.Platform.*;
import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.classfile.ErrorContext.*;
import static com.sun.max.vm.t1x.T1XTemplateTag.*;

import java.util.*;

import com.sun.c1x.*;
import com.sun.c1x.asm.*;
import com.sun.c1x.target.amd64.*;
import com.sun.c1x.target.amd64.AMD64Assembler.ConditionFlag;
import com.sun.cri.bytecode.*;
import com.sun.cri.bytecode.Bytecodes.MemoryBarriers;
import com.sun.cri.ci.*;
import com.sun.cri.ci.CiAddress.Scale;
import com.sun.cri.ci.CiCallingConvention.Type;
import com.sun.cri.ci.CiRegister.RegisterFlag;
import com.sun.cri.ci.CiTargetMethod.CodeAnnotation;
import com.sun.cri.ci.CiTargetMethod.JumpTable;
import com.sun.cri.ci.CiTargetMethod.LookupTable;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.debug.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.profile.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.amd64.*;
import com.sun.max.vm.t1x.T1XCompilation.PatchInfo;
import com.sun.max.vm.t1x.T1XTemplate.StopsBuilder;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.*;

/**
 * T1X per-compilation information.
 * <p>
 * This class is designed such that a single instance can be re-used for
 * separate compilations.
 *
 * @author Doug Simon
 */
public final class T1XCompilation {

    // Static info

    static final AdapterGenerator adapterGenerator = AdapterGenerator.forCallee(null, CallEntryPoint.JIT_ENTRY_POINT);
    static final CiRegister[] cpuRegParams = vm().registerConfigs.standard.getCallingConventionRegisters(Type.JavaCall, RegisterFlag.CPU);
    static final CiRegister[] fpuRegParams = vm().registerConfigs.standard.getCallingConventionRegisters(Type.JavaCall, RegisterFlag.FPU);
    static final CiRegister scratch = vm().registerConfigs.standard.getScratchRegister();
    static final Object EQ;
    static final Object NE;
    static final Object GE;
    static final Object GT;
    static final Object LE;
    static final Object LT;
    static {
        if (isAMD64()) {
            EQ = ConditionFlag.equal;
            NE = ConditionFlag.notEqual;
            GE = ConditionFlag.greaterEqual;
            GT = ConditionFlag.greater;
            LE = ConditionFlag.lessEqual;
            LT = ConditionFlag.less;
        } else {
            throw unimplISA();
        }
    }

    /**
     * The minimum value to which {@link T1XOptions#TraceBytecodeParserLevel} must be set to trace
     * the bytecode instructions as they are parsed.
     */
    public static final int TRACELEVEL_INSTRUCTIONS = 1;

    // Fields holding info/data structures reused across all compilations

    /**
     * The machine code assembler.
     */
    final AbstractAssembler asm;

    /**
     * The buffer to which the assembler writes its output.
     */
    final Buffer buf;

    /**
     * The compiler context.
     */
    final T1X compiler;

    /**
     * Object used to aggregate all the stops for the compiled code.
     */
    final StopsBuilder stops = new StopsBuilder();

    /**
     * Locations in the code buffer that need to be patched.
     */
    final PatchInfo patchInfo;

    /**
     * The set of reference literals.
     */
    final ArrayList<Object> referenceLiterals;

    /**
     * Code annotations for disassembly jump tables (lazily initialized).
     */
    ArrayList<CodeAnnotation> codeAnnotations;

    // Fields holding per-compilation info

    /**
     * The method being compiled.
     */
    ClassMethodActor method;

    CodeAttribute codeAttribute;

    /**
     * The last bytecode compiled.
     */
    int prevOpcode;

    /**
     * Access to the bytecode being compiled.
     */
    BytecodeStream stream;

    /**
     * Frame info for the method being compiled.
     */
    JVMSFrameLayout frame;

    /**
     * The local variable index for a copy of the receiver object locked by a non-static synchronized method.
     */
    int syncMethodReceiverCopy = -1;

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
    ConstantPool cp;

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

    MethodProfile.Builder methodProfileBuilder;

    /**
     * Map of BCIs to target code positions. Entries in the table corresponding to the start of a bytecode instruction
     * hold the position in the code buffer where the first byte of the template was emitted. This map
     * includes an entry for the BCI one byte past the end of the bytecode array. This is useful
     * for determining the end of the code emitted for the last bytecode instruction. That is, the value at
     * {@code bciToPos[bciToPos.length - 1]} is the target code position at which the epilogue starts
     * (0 denotes absence of an epilogue).
     */
    int[] bciToPos;

    Adapter adapter;

    /**
     * Determines if the target ISA is AMD64.
     */
    @FOLD
    static boolean isAMD64() {
        return platform().isa == ISA.AMD64;
    }

    /**
     * Called to denote some functionality is not yet implemented for the target ISA.
     */
    @NEVER_INLINE
    static FatalError unimplISA() {
        throw FatalError.unexpected("Unimplemented platform: " + platform().isa);
    }

    /**
     * Creates a compilation object.
     */
    public T1XCompilation(T1X compiler) {
        this.compiler = compiler;
        AbstractAssembler asm = null;
        if (isAMD64()) {
            asm = new AMD64Assembler(target(), null);
            patchInfo = new PatchInfoAMD64();
        } else {
            throw unimplISA();
        }
        this.asm = asm;
        this.buf = asm.codeBuffer;
        this.referenceLiterals = new ArrayList<Object>();
    }

    /**
     * Initializes all the per-compilation fields.
     *
     * @param method the method about to be compiled
     */
    void initCompile(ClassMethodActor method, CodeAttribute codeAttribute) {
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

    void initFrame(ClassMethodActor method, CodeAttribute codeAttribute) {
        if (isAMD64()) {
            int maxLocals = codeAttribute.maxLocals;
            int maxStack = codeAttribute.maxStack;
            int maxParams = method.numberOfParameterSlots();
            if (method.isSynchronized() && !method.isStatic()) {
                syncMethodReceiverCopy = maxLocals++;
            }
            frame = new AMD64JVMSFrameLayout(maxLocals, maxStack, maxParams, compiler.templateSlots);
        } else {
            unimplISA();
        }
    }

    /**
     * Cleans up all the per-compilation fields.
     */
    void cleanup() {
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
        syncMethodReceiverCopy = -1;
        syncMethodHandlerPos = -1;
        cp = null;
        buf.reset();
        referenceLiterals.clear();
        if (codeAnnotations != null) {
            codeAnnotations.clear();
        }
        patchInfo.reset();
        adapter = null;
        stops.reset(false);
        if (methodProfileBuilder != null) {
            methodProfileBuilder.finish();
            methodProfileBuilder = null;
        }
    }

    void emitProfileMethodEntry() {
        if (methodProfileBuilder != null) {
            methodProfileBuilder.addEntryCounter(MethodInstrumentation.initialEntryCount);
            assignReferenceLiteralTemplateArgument(0, methodProfileBuilder.methodProfileObject());
            T1XTemplate template;
            if (method.isStatic()) {
                template = getTemplate(PROFILE_STATIC_METHOD_ENTRY);
            } else {
                template = getTemplate(PROFILE_NONSTATIC_METHOD_ENTRY);
                assignLocalDisplacementTemplateArgument(1, 0, Kind.REFERENCE);
            }
            emitAndRecordStops(template);
        }
    }

    void emitMethodTraceEntry() {
        if (T1XOptions.TraceMethods) {
            T1XTemplate template = getTemplate(TRACE_METHOD_ENTRY);
            assignReferenceLiteralTemplateArgument(0, method.toString());
            emitAndRecordStops(template);
        }
    }

    void emitSynchronizedMethodAcquire() {
        if (method.isSynchronized()) {
            if (method.isStatic()) {
                T1XTemplate template = getTemplate(LOCK_CLASS);
                assignReferenceLiteralTemplateArgument(0, method.holder().javaClass());
                emitAndRecordStops(template);
            } else {
                T1XTemplate template = getTemplate(LOCK_RECEIVER);
                assignLocalDisplacementTemplateArgument(0, 0, Kind.REFERENCE);
                assignLocalDisplacementTemplateArgument(1, syncMethodReceiverCopy, Kind.REFERENCE);
                emitAndRecordStops(template);
            }
            syncMethodStartPos = buf.position();
        }
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
            fixup();
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
        emitPrologue();

        emitProfileMethodEntry();

        emitMethodTraceEntry();

        emitSynchronizedMethodAcquire();

        int bci = 0;
        int endBCI = stream.endBCI();
        while (bci < endBCI) {
            int opcode = stream.currentBC();
            processBytecode(opcode);
            stream.next();
            bci = stream.currentBCI();
        }

        int epiloguePos = buf.position();

        emitSynchronizedMethodHandler(method, endBCI);

        if (epiloguePos != buf.position()) {
            bciToPos[endBCI] = epiloguePos;
        }
    }

    void emitSynchronizedMethodHandler(ClassMethodActor method, int endBCI) {
        if (method.isSynchronized()) {
            syncMethodHandlerPos = buf.position();
            if (method.isStatic()) {
                T1XTemplate template = getTemplate(UNLOCK_CLASS);
                assignReferenceLiteralTemplateArgument(0, method.holder().javaClass());
                emitAndRecordStops(template);
            } else {
                T1XTemplate template = getTemplate(UNLOCK_RECEIVER);
                assignLocalDisplacementTemplateArgument(0, syncMethodReceiverCopy, Kind.REFERENCE);
                emitAndRecordStops(template);
            }
            syncMethodEndPos = buf.position();
            emitAndRecordStops(getTemplate(RETHROW_EXCEPTION));
        }
    }

    private void startBlock(int bci) {
        if (!blockBCIs[bci]) {
            numberOfBlocks++;
            blockBCIs[bci] = true;
        }
    }

    /**
     * Copies the code from a given template into the code buffer and updates the set of stops for the method being
     * translated with those derived from the template.
     *
     * @param template the compiled code to emit
     */
    private void emitAndRecordStops(T1XTemplate template) {
        if (template.numberOfStops != 0) {
            int bci = stream.currentBCI();
            stops.add(template, buf.position(), bci == stream.endBCI() ? -1 : bci, null);
        }
        buf.emitBytes(template.code, 0, template.code.length);
    }

    private void beginBytecode(int representativeOpcode) {
        int bci = stream.currentBCI();
        int pos = buf.position();

        bciToPos[bci] = pos;

        if (Bytecodes.isBlockEnd(prevOpcode)) {
            startBlock(bci);
            if (handlerBCIs != null) {
                if (handlerBCIs[bci]) {
                    emitAndRecordStops(getTemplate(LOAD_EXCEPTION));
                }

            }
        }
        prevOpcode = representativeOpcode;
    }

    private T1XTemplate getTemplate(T1XTemplateTag tag) {
        assert tag != null;
        assert compiler.templates[tag.ordinal()] != null;
        return compiler.templates[tag.ordinal()];
    }

    private Adapter emitPrologue() {
        if (adapterGenerator != null) {
            adapter = adapterGenerator.adapt(method, asm);
        }
        if (isAMD64()) {
            AMD64Assembler asm = (AMD64Assembler) this.asm;

            int frameSize = frame.frameSize();
            asm.enter(frameSize - Word.size(), 0);
            asm.subq(rbp, framePointerAdjustment());
            if (Trap.STACK_BANGING) {
                int pageSize = platform().pageSize;
                int framePages = frameSize / pageSize;
                // emit multiple stack bangs for methods with frames larger than a page
                for (int i = 0; i <= framePages; i++) {
                    int offset = (i + C1XOptions.StackShadowPages) * pageSize;
                    // Deduct 'frameSize' to handle frames larger than (C1XOptions.StackShadowPages * pageSize)
                    offset = offset - frameSize;
                    asm.movq(new CiAddress(CiKind.Word, RSP, (-offset)), rax);
                }
            }
            return adapter;
        } else {
            throw unimplISA();
        }
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
     * Emit template for bytecode instruction with no operands. These bytecode have no dependencies, so emitting the
     * template just consists of copying the target instruction into the code buffer.
     */
    private void emit(T1XTemplateTag tag) {
        T1XTemplate template = getTemplate(tag);
        beginBytecode(tag.opcode);
        emitAndRecordStops(template);
    }

    private void emitInt(T1XTemplateTag tag, int value) {
        T1XTemplate template = getTemplate(tag);
        beginBytecode(tag.opcode);
        assignIntTemplateArgument(0, value);
        emitAndRecordStops(template);
    }

    private void emitDouble(T1XTemplateTag tag, double value) {
        T1XTemplate template = getTemplate(tag);
        beginBytecode(tag.opcode);
        assignDoubleTemplateArgument(0, value);
        emitAndRecordStops(template);
    }

    private void emitFloat(T1XTemplateTag tag, float value) {
        T1XTemplate template = getTemplate(tag);
        beginBytecode(tag.opcode);
        assignFloatTemplateArgument(0, value);
        emitAndRecordStops(template);
    }

    private void emitLong(T1XTemplateTag tag, long value) {
        T1XTemplate template = getTemplate(tag);
        beginBytecode(tag.opcode);
        assignLongTemplateArgument(0, value);
        emitAndRecordStops(template);
    }

    /**
     * Emits a template for a bytecode operating on a local variable (operand is an index to a local variable). The
     * template is customized so that the emitted code uses a specific local variable index.
     *
     * @param opcode One of iload, istore, dload, dstore, fload, fstore, lload, lstore
     * @param localIndex the local variable index to customize the template with.
     * @param kind the kind of the value in the local
     */
    private void emitVarAccess(T1XTemplateTag tag, int localIndex, Kind kind) {
        T1XTemplate template = getTemplate(tag);
        beginBytecode(tag.opcode);
        assignLocalDisplacementTemplateArgument(0, localIndex, kind);
        emitAndRecordStops(template);
    }

    /**
     * Emit template for a bytecode operating on a (static or dynamic) field.
     * @param index Index to the field ref constant.
     * @param template one of getfield, putfield, getstatic, putstatic
     */
    private void emitFieldAccess(EnumMap<KindEnum, T1XTemplateTag> tags, int index) {
        FieldRefConstant fieldRefConstant = cp.fieldAt(index);
        Kind fieldKind = fieldRefConstant.type(cp).toKind();
        T1XTemplateTag tag = tags.get(fieldKind.asEnum);
        beginBytecode(tag.opcode);

        if (fieldRefConstant.isResolvableWithoutClassLoading(cp)) {
            try {
                FieldActor fieldActor = fieldRefConstant.resolve(cp, index);
                emitPreVolatileFieldAccess(tag, fieldActor);
                if (fieldActor.isStatic()) {
                    if (fieldActor.holder().isInitialized()) {
                        T1XTemplate template = getTemplate(tag.initialized);
                        assignReferenceLiteralTemplateArgument(0, fieldActor.holder().staticTuple());
                        assignIntTemplateArgument(1, fieldActor.offset());
                        emitAndRecordStops(template);
                        emitPostVolatileFieldAccess(tag, fieldActor);
                        return;
                    }
                } else {
                    T1XTemplate template = getTemplate(tag.resolved);
                    assignIntTemplateArgument(0, fieldActor.offset());
                    emitAndRecordStops(template);
                    emitPostVolatileFieldAccess(tag, fieldActor);
                    return;
                }
            } catch (LinkageError e) {
                // This should not happen since the field ref constant is resolvable without class loading (i.e., it
                // has already been resolved). If it were to happen, the safe thing to do is to fall off to the
                // "no assumption" case, where a template for an unresolved class is taken instead. So do nothing here.
            }
        }
        T1XTemplate template = getTemplate(tag);
        assignReferenceLiteralTemplateArgument(0, cp.makeResolutionGuard(index));
        // Emit the template unmodified now. It will be modified in the end once all labels to literals are fixed.
        emitAndRecordStops(template);
    }

    void emitPreVolatileFieldAccess(T1XTemplateTag tag, FieldActor fieldActor) {
        if (fieldActor.isVolatile()) {
            boolean isWrite = tag.opcode == Bytecodes.PUTFIELD || tag.opcode == Bytecodes.PUTSTATIC;
            if (isAMD64()) {
                AMD64Assembler asm = (AMD64Assembler) this.asm;
                asm.membar(isWrite ? MemoryBarriers.JMM_PRE_VOLATILE_WRITE : MemoryBarriers.JMM_PRE_VOLATILE_READ);
            } else {
                unimplISA();
            }
        }
    }

    void emitPostVolatileFieldAccess(T1XTemplateTag tag, FieldActor fieldActor) {
        if (fieldActor.isVolatile()) {
            boolean isWrite = tag.opcode == Bytecodes.PUTFIELD || tag.opcode == Bytecodes.PUTSTATIC;
            if (isAMD64()) {
                AMD64Assembler asm = (AMD64Assembler) this.asm;
                asm.membar(isWrite ? MemoryBarriers.JMM_POST_VOLATILE_WRITE : MemoryBarriers.JMM_POST_VOLATILE_READ);
            } else {
                unimplISA();
            }
        }
    }

    /**
     * Emits code for an instruction that references a {@link ClassConstant}.
     *
     * @param template the instruction template
     * @param index a constant pool index
     * @return
     */
    private void emitTemplateWithClassConstant(T1XTemplateTag tag, int index) {
        ClassConstant classConstant = cp.classAt(index);
        boolean isArray = tag == ANEWARRAY;
        T1XTemplate template;
        if (classConstant.isResolvableWithoutClassLoading(cp)) {
            template = getTemplate(tag.resolved);
            beginBytecode(tag.opcode);
            ClassActor resolvedClassActor = classConstant.resolve(cp, index);
            if (isArray) {
                resolvedClassActor = ArrayClassActor.forComponentClassActor(resolvedClassActor);
            }
            assignReferenceLiteralTemplateArgument(0, resolvedClassActor);
        } else {
            template = getTemplate(tag);
            beginBytecode(tag.opcode);
            assignReferenceLiteralTemplateArgument(0, cp.makeResolutionGuard(index));
        }
        emitAndRecordStops(template);
    }

    private void emitIinc(int index, int increment) {
        beginBytecode(IINC.opcode);
        final T1XTemplate template = getTemplate(IINC);
        assignLocalDisplacementTemplateArgument(0, index, Kind.INT);
        assignIntTemplateArgument(1, increment);
        emitAndRecordStops(template);
    }

    @PLATFORM(cpu = "amd64")
    private int framePointerAdjustment() {
        final int enterSize = frame.frameSize() - Word.size();
        return enterSize - frame.sizeOfNonParameterLocals();
    }

    private void emitReturn(T1XTemplateTag tag, T1XTemplateTag tagUnlockClass, T1XTemplateTag tagUnlockReceiver) {
        beginBytecode(tag.opcode);
        if (method.isSynchronized()) {
            if (method.isStatic()) {
                T1XTemplate template = getTemplate(tagUnlockClass);
                assignReferenceLiteralTemplateArgument(0, method.holder().javaClass());
                emitAndRecordStops(template);
            } else {
                T1XTemplate template = getTemplate(tagUnlockReceiver);
                assignLocalDisplacementTemplateArgument(0, syncMethodReceiverCopy, Kind.REFERENCE);
                emitAndRecordStops(template);
            }
        } else {
            T1XTemplate template = getTemplate(tag);
            emitAndRecordStops(template);
        }

        if (isAMD64()) {
            AMD64Assembler asm = (AMD64Assembler) this.asm;
            asm.addq(rbp, framePointerAdjustment());
            asm.leave();
            // when returning, retract from the caller stack by the space used for the arguments.
            final short stackAmountInBytes = (short) frame.sizeOfParameters();
            asm.ret(stackAmountInBytes);
        } else {
            unimplISA();
        }
    }

    private void emitBranch(Object ccObj, T1XTemplateTag tag, int targetBCI) {
        int bci = stream.currentBCI();
        startBlock(targetBCI);
        beginBytecode(tag.opcode);
        if (ccObj != null) {
            T1XTemplate template = getTemplate(tag);
            emitAndRecordStops(template);
        }

        int pos = buf.position();
        if (isAMD64()) {
            AMD64Assembler asm = (AMD64Assembler) this.asm;
            PatchInfoAMD64 patchInfo = (PatchInfoAMD64) this.patchInfo;
            ConditionFlag cc = (ConditionFlag) ccObj;
            if (bci < targetBCI) {
                if (cc != null) {
                    patchInfo.addJCC(cc, pos, targetBCI);
                    asm.jcc(cc, 0, true);
                } else {
                    // Unconditional jump
                    patchInfo.addJMP(pos, targetBCI);
                    asm.jmp(0, true);
                }
                assert bciToPos[targetBCI] == 0;
            } else {
                // Ideally, we'd like to emit a safepoint at the target of a backward branch.
                // However, that would require at least one extra pass to determine where
                // the backward branches are. Instead, we simply emit a safepoint at the source of
                // a backward branch. This means the cost of the safepoint is taken even if
                // the backward branch is not taken but that cost should not be noticeable.
                byte[] safepointCode = vm().safepoint.code;
                buf.emitBytes(safepointCode, 0, safepointCode.length);
                stops.addBytecodeBackwardBranch(bci, pos);

                // Compute relative offset.
                final int target = bciToPos[targetBCI];
                if (cc == null) {
                    asm.jmp(target, false);
                } else {
                    asm.jcc(cc, target, false);
                }
            }
        } else {
            unimplISA();
        }
    }

    /**
     * Gets the kind used to select an INVOKE... bytecode template.
     */
    private Kind invokeKind(SignatureDescriptor signature) {
        Kind resultKind = signature.resultKind();
        if (resultKind.isWord || resultKind.isReference || resultKind.stackKind == Kind.INT) {
            return Kind.WORD;
        }
        return resultKind;
    }

    private static int receiverStackIndex(SignatureDescriptor signatureDescriptor) {
        int index = 0;
        for (int i = 0; i < signatureDescriptor.numberOfParameters(); i++) {
            Kind kind = signatureDescriptor.parameterDescriptorAt(i).toKind();
            index += kind.stackSlots;
        }
        return index;
    }

    private void emitInvokevirtual(int index) {
        ClassMethodRefConstant classMethodRef = cp.classMethodAt(index);
        SignatureDescriptor signature = classMethodRef.signature(cp);
        Kind kind = invokeKind(signature);
        T1XTemplateTag tag = INVOKEVIRTUALS.get(kind.asEnum);
        try {
            if (classMethodRef.isResolvableWithoutClassLoading(cp)) {
                try {
                    VirtualMethodActor virtualMethodActor = classMethodRef.resolveVirtual(cp, index);
                    if (virtualMethodActor.isPrivate() || virtualMethodActor.isFinal()) {
                        // this is an invokevirtual to a private or final method, treat it like invokespecial
                        tag = INVOKESPECIALS.get(kind.asEnum).resolved;
                        T1XTemplate template = getTemplate(tag);
                        beginBytecode(tag.opcode);
                        assignIntTemplateArgument(0, receiverStackIndex(signature));
                        recordDirectBytecodeCall(template, virtualMethodActor);
                        return;
//                    } else if (shouldProfileMethodCall(virtualMethodActor)) {
//                        // emit a profiled call
//                        T1XTemplate template = getTemplate(tag.instrumented);
//                        beginBytecode(tag.opcode);
//                        int vtableIndex = virtualMethodActor.vTableIndex();
//                        assignIntTemplateArgument(0, vtableIndex);
//                        assignIntTemplateArgument(1, receiverStackIndex(signature));
//                        assignReferenceLiteralTemplateArgument(2, methodProfileBuilder.methodProfileObject());
//                        assignIntTemplateArgument(3, methodProfileBuilder.addMethodProfile(index, MethodInstrumentation.DEFAULT_RECEIVER_METHOD_PROFILE_ENTRIES));
//                        emitAndRecordStops(template);
                    } else {
                        // emit an unprofiled virtual dispatch
                        T1XTemplate template = getTemplate(tag.resolved);
                        beginBytecode(tag.opcode);
                        assignIntTemplateArgument(0, virtualMethodActor.vTableIndex());
                        assignIntTemplateArgument(1, receiverStackIndex(signature));
                        emitAndRecordStops(template);
                    }
                    return;
                } catch (LinkageError e) {
                    // fall through
                }
            }
        } catch (LinkageError error) {
            // Fall back on unresolved template that will cause the error to be rethrown at runtime.
        }
        T1XTemplate template = getTemplate(tag);
        beginBytecode(tag.opcode);
        assignReferenceLiteralTemplateArgument(0, cp.makeResolutionGuard(index));
        assignIntTemplateArgument(1, receiverStackIndex(signature));
        emitAndRecordStops(template);
    }

    private void emitInvokeinterface(int index) {
        InterfaceMethodRefConstant interfaceMethodRef = cp.interfaceMethodAt(index);
        SignatureDescriptor signature = interfaceMethodRef.signature(cp);
        Kind kind = invokeKind(signature);
        T1XTemplateTag tag = INVOKEINTERFACES.get(kind.asEnum);
        try {
            if (interfaceMethodRef.isResolvableWithoutClassLoading(cp)) {
                try {
                    InterfaceMethodActor interfaceMethodActor = (InterfaceMethodActor) interfaceMethodRef.resolve(cp, index);
//                    if (shouldProfileMethodCall(interfaceMethodActor)) {
//                        T1XTemplate template = getTemplate(template.instrumented);
//                        beginBytecode(template.opcode);
//                        assignReferenceLiteralTemplateArgument(0, interfaceMethodActor);
//                        assignIntTemplateArgument(1, receiverStackIndex(signature));
//                        assignReferenceLiteralTemplateArgument(2, methodProfileBuilder.methodProfileObject());
//                        assignIntTemplateArgument(3, methodProfileBuilder.addMethodProfile(index, MethodInstrumentation.DEFAULT_RECEIVER_METHOD_PROFILE_ENTRIES));
//                        emitAndRecordStops(template);
//                    } else {
                    T1XTemplate template = getTemplate(tag.resolved);
                    beginBytecode(tag.opcode);
                    assignReferenceLiteralTemplateArgument(0, interfaceMethodActor);
                    assignIntTemplateArgument(1, receiverStackIndex(signature));
                    emitAndRecordStops(template);
//                    }
                    return;
                } catch (LinkageError e) {
                    // fall through
                }
            }
        } catch (LinkageError error) {
            // Fall back on unresolved template that will cause the error to be rethrown at runtime.
        }
        T1XTemplate template = getTemplate(tag);
        beginBytecode(tag.opcode);
        assignReferenceLiteralTemplateArgument(0, cp.makeResolutionGuard(index));
        assignIntTemplateArgument(1, receiverStackIndex(signature));
        emitAndRecordStops(template);
    }

    private void emitInvokespecial(int index) {
        ClassMethodRefConstant classMethodRef = cp.classMethodAt(index);
        Kind kind = invokeKind(classMethodRef.signature(cp));
        SignatureDescriptor signature = classMethodRef.signature(cp);
        T1XTemplateTag tag = INVOKESPECIALS.get(kind.asEnum);
        try {
            if (classMethodRef.isResolvableWithoutClassLoading(cp)) {
                VirtualMethodActor virtualMethodActor = classMethodRef.resolveVirtual(cp, index);
                T1XTemplate template = getTemplate(tag.resolved);
                beginBytecode(tag.opcode);
                assignIntTemplateArgument(0, receiverStackIndex(signature));
                recordDirectBytecodeCall(template, virtualMethodActor);
                return;
            }
        } catch (LinkageError error) {
            // Fall back on unresolved template that will cause the error to be rethrown at runtime.
        }
        T1XTemplate template = getTemplate(tag);
        beginBytecode(tag.opcode);
        assignReferenceLiteralTemplateArgument(0, cp.makeResolutionGuard(index));
        assignIntTemplateArgument(1, receiverStackIndex(signature));
        emitAndRecordStops(template);
    }

    private void emitInvokestatic(int index) {
        ClassMethodRefConstant classMethodRef = cp.classMethodAt(index);
        Kind kind = invokeKind(classMethodRef.signature(cp));
        T1XTemplateTag tag = INVOKESTATICS.get(kind.asEnum);
        try {
            if (classMethodRef.isResolvableWithoutClassLoading(cp)) {
                StaticMethodActor staticMethodActor = classMethodRef.resolveStatic(cp, index);
                if (staticMethodActor.holder().isInitialized()) {
                    T1XTemplate template = getTemplate(tag.initialized);
                    beginBytecode(tag.opcode);
                    recordDirectBytecodeCall(template, staticMethodActor);
                    return;
                }
            }
        } catch (LinkageError error) {
            // Fall back on unresolved template that will cause the error to be rethrown at runtime.
        }
        T1XTemplate template = getTemplate(tag);
        beginBytecode(tag.opcode);
        assignReferenceLiteralTemplateArgument(0, cp.makeResolutionGuard(index));
        emitAndRecordStops(template);
    }

    private void recordDirectBytecodeCall(T1XTemplate template, ClassMethodActor callee) {
        if (isAMD64()) {
            AMD64Assembler asm = (AMD64Assembler) this.asm;
            if (callee.currentTargetMethod() == null) {
                // Align unlinked bytecode call site for MT safe patching
                final int alignment = 7;
                final int callSitePosition = buf.position() + template.bytecodeCall.pos;
                final int roundDownMask = ~alignment;
                final int directCallInstructionLength = 5; // [0xE8] disp32
                final int endOfCallSite = callSitePosition + (directCallInstructionLength - 1);
                if ((callSitePosition & roundDownMask) != (endOfCallSite & roundDownMask)) {
                    // Emit nops to align up to next 8-byte boundary
                    asm.nop(8 - (callSitePosition & alignment));
                }
            }
        } else {
            unimplISA();
        }
        stops.add(template, buf.position(), stream.currentBCI(), callee);
        buf.emitBytes(template.code, 0, template.code.length);
    }

    private void emitConstant(int index) {
        PoolConstant constant = cp.at(index);
        int bytecode = Bytecodes.LDC;
        switch (constant.tag()) {
            case CLASS: {
                ClassConstant classConstant = (ClassConstant) constant;
                if (classConstant.isResolvableWithoutClassLoading(cp)) {
                    T1XTemplate template = getTemplate(LDC$reference$resolved);
                    beginBytecode(bytecode);
                    Object mirror = ((ClassActor) classConstant.value(cp, index).asObject()).javaClass();
                    assignReferenceLiteralTemplateArgument(0, mirror);
                    emitAndRecordStops(template);
                } else {
                    T1XTemplate template = getTemplate(LDC$reference);
                    beginBytecode(bytecode);
                    assignReferenceLiteralTemplateArgument(0, cp.makeResolutionGuard(index));
                    emitAndRecordStops(template);
                }
                break;
            }
            case INTEGER: {
                T1XTemplate template = getTemplate(LDC$int);
                beginBytecode(bytecode);
                IntegerConstant integerConstant = (IntegerConstant) constant;
                assignIntTemplateArgument(0, integerConstant.value());
                emitAndRecordStops(template);
                break;
            }
            case LONG: {
                T1XTemplate template = getTemplate(LDC$long);
                beginBytecode(bytecode);
                LongConstant longConstant = (LongConstant) constant;
                assignLongTemplateArgument(0, longConstant.value());
                emitAndRecordStops(template);
                break;
            }
            case FLOAT: {
                T1XTemplate template = getTemplate(LDC$float);
                beginBytecode(bytecode);
                FloatConstant floatConstant = (FloatConstant) constant;
                assignFloatTemplateArgument(0, floatConstant.value());
                emitAndRecordStops(template);
                break;
            }
            case DOUBLE: {
                T1XTemplate template = getTemplate(LDC$double);
                beginBytecode(bytecode);
                DoubleConstant doubleConstant = (DoubleConstant) constant;
                assignDoubleTemplateArgument(0, doubleConstant.value());
                emitAndRecordStops(template);
                break;
            }
            case STRING: {
                T1XTemplate template = getTemplate(LDC$reference$resolved);
                beginBytecode(bytecode);
                StringConstant stringConstant = (StringConstant) constant;
                assignReferenceLiteralTemplateArgument(0, stringConstant.value);
                emitAndRecordStops(template);
                break;
            }
            default: {
                assert false : "ldc for unexpected constant tag: " + constant.tag();
                break;
            }
        }
    }

    private void emitMultianewarray(int index, int numberOfDimensions) {
        ClassConstant classRef = cp.classAt(index);
        if (classRef.isResolvableWithoutClassLoading(cp)) {
            T1XTemplate template = getTemplate(MULTIANEWARRAY$resolved);
            beginBytecode(Bytecodes.MULTIANEWARRAY);
            ClassActor arrayClassActor = classRef.resolve(cp, index);
            assert arrayClassActor.isArrayClass();
            assert arrayClassActor.numberOfDimensions() >= numberOfDimensions : "dimensionality of array class constant smaller that dimension operand";
            assignReferenceLiteralTemplateArgument(0, arrayClassActor);
            assignReferenceLiteralTemplateArgument(1, new int[numberOfDimensions]);
            // Emit the template
            emitAndRecordStops(template);
            return; // we're done.
        }
        // Unresolved case
        T1XTemplate template = getTemplate(MULTIANEWARRAY);
        beginBytecode(Bytecodes.MULTIANEWARRAY);
        assignReferenceLiteralTemplateArgument(0, cp.makeResolutionGuard(index));
        assignReferenceLiteralTemplateArgument(1, new int[numberOfDimensions]);
        emitAndRecordStops(template);
    }

    private void emitNew(int index) {
        ClassConstant classRef = cp.classAt(index);
        if (classRef.isResolvableWithoutClassLoading(cp)) {
            ClassActor classActor = classRef.resolve(cp, index);
            if (classActor.isInitialized()) {
                T1XTemplate template = getTemplate(NEW$init);
                beginBytecode(Bytecodes.NEW);
                assignReferenceLiteralTemplateArgument(0, classActor);
                emitAndRecordStops(template);
                return;
            }
        }
        T1XTemplate template = getTemplate(NEW);
        beginBytecode(Bytecodes.NEW);
        assignReferenceLiteralTemplateArgument(0, cp.makeResolutionGuard(index));
        emitAndRecordStops(template);
    }

    private void emitNewarray(int tag) {
        T1XTemplate template = getTemplate(NEWARRAY);
        beginBytecode(Bytecodes.NEWARRAY);
        Kind arrayElementKind = Kind.fromNewArrayTag(tag);
        assignReferenceLiteralTemplateArgument(0, arrayElementKind);
        emitAndRecordStops(template);
    }

    private void emitTableswitch() {
        int bci = stream.currentBCI();
        BytecodeTableSwitch ts = new BytecodeTableSwitch(stream, bci);
        int lowMatch = ts.lowKey();
        int highMatch = ts.highKey();
        if (lowMatch > highMatch) {
            throw verifyError("Low must be less than or equal to high in TABLESWITCH");
        }
        beginBytecode(Bytecodes.TABLESWITCH);

        if (isAMD64()) {
            AMD64Assembler asm = (AMD64Assembler) this.asm;
            PatchInfoAMD64 patchInfo = (PatchInfoAMD64) this.patchInfo;

            // Pop index from stack into rax
            asm.movl(rax, new CiAddress(CiKind.Int, rsp.asValue()));
            asm.addq(rsp, JVMSFrameLayout.JVMS_SLOT_SIZE);

            // Compare index against jump table bounds
            if (lowMatch != 0) {
                // subtract the low value from the switch index
                asm.subl(rax, lowMatch);
                asm.cmpl(rax, highMatch - lowMatch);
            } else {
                asm.cmpl(rax, highMatch);
            }

            // Jump to default target if index is not within the jump table
            startBlock(ts.defaultTarget());
            int pos = buf.position();
            patchInfo.addJCC(ConditionFlag.above, pos, ts.defaultTarget());
            asm.jcc(ConditionFlag.above, 0, true);

            // Set r15 to address of jump table
            int leaPos = buf.position();
            buf.mark();
            asm.leaq(r15, new CiAddress(CiKind.Word, InstructionRelative.asValue(), 0));

            // Load jump table entry into r15 and jump to it
            asm.movslq(rax, new CiAddress(CiKind.Int, r15.asValue(), rax.asValue(), Scale.Times4, 0));
            asm.addq(r15, rax);
            asm.jmp(r15);

            // Inserting padding so that jump table address is 4-byte aligned
            if ((buf.position() & 0x3) != 0) {
                asm.nop(4 - (buf.position() & 0x3));
            }

            // Patch LEA instruction above now that we know the position of the jump table
            int jumpTablePos = buf.position();
            buf.setPosition(leaPos);
            buf.mark();
            asm.leaq(r15, new CiAddress(CiKind.Word, InstructionRelative.asValue(), jumpTablePos - leaPos));
            buf.setPosition(jumpTablePos);

            // Emit jump table entries
            for (int i = 0; i < ts.numberOfCases(); i++) {
                int targetBCI = ts.targetAt(i);
                startBlock(targetBCI);
                pos = buf.position();
                patchInfo.addJumpTableEntry(pos, jumpTablePos, targetBCI);
                buf.emitInt(0);
            }

            if (codeAnnotations == null) {
                codeAnnotations = new ArrayList<CiTargetMethod.CodeAnnotation>();
            }
            codeAnnotations.add(new JumpTable(jumpTablePos, ts.lowKey(), ts.highKey(), 4));

        } else {
            unimplISA();
        }
    }

    private void emitLookupswitch() {
        int bci = stream.currentBCI();
        BytecodeLookupSwitch ls = new BytecodeLookupSwitch(stream, bci);
        beginBytecode(Bytecodes.LOOKUPSWITCH);
        if (isAMD64()) {
            AMD64Assembler asm = (AMD64Assembler) this.asm;
            PatchInfoAMD64 patchInfo = (PatchInfoAMD64) this.patchInfo;
            if (ls.numberOfCases() == 0) {
                // Pop the key
                T1XTemplate template = getTemplate(POP);
                emitAndRecordStops(template);

                int targetBCI = ls.defaultTarget();
                startBlock(targetBCI);
                if (stream.nextBCI() == targetBCI) {
                    // Skip completely if default target is next instruction
                } else if (targetBCI <= bci) {
                    int target = bciToPos[targetBCI];
                    assert target != 0;
                    asm.jmp(target, false);
                } else {
                    patchInfo.addJMP(buf.position(), targetBCI);
                    asm.jmp(0, true);
                }
            } else {
                // Pop key from stack into rax
                asm.movl(rax, new CiAddress(CiKind.Int, rsp.asValue()));
                asm.addq(rsp, JVMSFrameLayout.JVMS_SLOT_SIZE);

                // Set rbx to address of lookup table
                int leaPos = buf.position();
                buf.mark();
                asm.leaq(rbx, new CiAddress(CiKind.Word, InstructionRelative.asValue(), 0));

                // Initialize rcx to index of last entry
                asm.movl(rcx, (ls.numberOfCases() - 1) * 2);

                int loopPos = buf.position();

                // Compare the value against the key
                asm.cmpl(rax, new CiAddress(CiKind.Int, rbx.asValue(), rcx.asValue(), Scale.Times4, 0));

                // If equal, exit loop
                int matchTestPos = buf.position();
                final int placeholderForShortJumpDisp = matchTestPos + 2;
                asm.jcc(ConditionFlag.equal, placeholderForShortJumpDisp, false);
                assert buf.position() - matchTestPos == 2;

                // Decrement loop var (r15) and jump to top of loop if it did not go below zero (i.e. carry flag was not set)
                asm.subl(rcx, 2);
                asm.jcc(ConditionFlag.carryClear, loopPos, false);

                // Jump to default target
                startBlock(ls.defaultTarget());
                patchInfo.addJMP(buf.position(), ls.defaultTarget());
                asm.jmp(0, true);

                // Patch the first conditional branch instruction above now that we know where's it's going
                int matchPos = buf.position();
                buf.setPosition(matchTestPos);
                asm.jcc(ConditionFlag.equal, matchPos, false);
                buf.setPosition(matchPos);

                // Load jump table entry into r15 and jump to it
                asm.movslq(rcx, new CiAddress(CiKind.Int, rbx.asValue(), rcx.asValue(), Scale.Times4, 4));
                asm.addq(rbx, rcx);
                asm.jmp(rbx);

                // Inserting padding so that lookup table address is 4-byte aligned
                while ((buf.position() & 0x3) != 0) {
                    asm.nop();
                }

                // Patch the LEA instruction above now that we know the position of the lookup table
                int lookupTablePos = buf.position();
                buf.setPosition(leaPos);
                buf.mark();
                asm.leaq(rbx, new CiAddress(CiKind.Word, InstructionRelative.asValue(), lookupTablePos - leaPos));
                buf.setPosition(lookupTablePos);

                // Emit lookup table entries
                for (int i = 0; i < ls.numberOfCases(); i++) {
                    int key = ls.keyAt(i);
                    int targetBCI = ls.targetAt(i);
                    startBlock(targetBCI);
                    patchInfo.addLookupTableEntry(buf.position(), key, lookupTablePos, targetBCI);
                    buf.emitInt(key);
                    buf.emitInt(0);
                }
                if (codeAnnotations == null) {
                    codeAnnotations = new ArrayList<CiTargetMethod.CodeAnnotation>();
                }
                codeAnnotations.add(new LookupTable(lookupTablePos, ls.numberOfCases(), 4, 4));
            }
        } else {
            unimplISA();
        }
    }

    private void fixup() {
        if (patchInfo.size == 0) {
            return;
        }

        int endPos = buf.position();
        if (isAMD64()) {
            PatchInfoAMD64 patchInfo = (PatchInfoAMD64) this.patchInfo;
            AMD64Assembler asm = (AMD64Assembler) this.asm;
            int i = 0;
            while (i < patchInfo.size) {
                int tag = patchInfo.at(i++);
                if (tag == PatchInfoAMD64.JCC) {
                    ConditionFlag cc = ConditionFlag.values[patchInfo.at(i++)];
                    int pos = patchInfo.at(i++);
                    int targetBCI = patchInfo.at(i++);
                    int target = bciToPos[targetBCI];
                    assert target != 0;
                    buf.setPosition(pos);
                    asm.jcc(cc, target, true);
                } else if (tag == PatchInfoAMD64.JMP) {
                    int pos = patchInfo.at(i++);
                    int targetBCI = patchInfo.at(i++);
                    int target = bciToPos[targetBCI];
                    assert target != 0;
                    buf.setPosition(pos);
                    asm.jmp(target, true);
                } else if (tag == PatchInfoAMD64.JUMP_TABLE_ENTRY) {
                    int pos = patchInfo.at(i++);
                    int jumpTablePos = patchInfo.at(i++);
                    int targetBCI = patchInfo.at(i++);
                    int target = bciToPos[targetBCI];
                    assert target != 0;
                    int disp = target - jumpTablePos;
                    buf.setPosition(pos);
                    buf.emitInt(disp);
                } else if (tag == PatchInfoAMD64.LOOKUP_TABLE_ENTRY) {
                    int pos = patchInfo.at(i++);
                    int key = patchInfo.at(i++);
                    int lookupTablePos = patchInfo.at(i++);
                    int targetBCI = patchInfo.at(i++);
                    int target = bciToPos[targetBCI];
                    assert target != 0;
                    int disp = target - lookupTablePos;
                    buf.setPosition(pos);
                    buf.emitInt(key);
                    buf.emitInt(disp);
                } else {
                    throw FatalError.unexpected(String.valueOf(tag));
                }
            }

        } else {
            unimplISA();
        }
        buf.setPosition(endPos);
    }

    /**
     * Compute the offset to a literal reference being created. The current position in the code buffer must be
     * that of the instruction loading the literal.
     *
     * @param numReferenceLiterals number of created reference literals (including the one being created).
     * @return an offset, in byte, to the base used to load literal.
     */
    private int computeReferenceLiteralOffset(int numReferenceLiterals) {
        if (isAMD64()) {
            // Remember: in the target bundle, the reference literal cell is directly adjacent to the code cell.
            return numReferenceLiterals * Word.size() + Layout.byteArrayLayout().getElementOffsetInCell(buf.position()).toInt();
        } else {
            throw unimplISA();
        }
    }

    /**
     * Return the relative offset of the literal to the current code buffer position. (negative number since literals
     * are placed before code in the bundle)
     *
     * @param literal the object
     * @return the offset of the literal relative to the current code position
     */
    private int createReferenceLiteral(Object literal) {
        int literalOffset = computeReferenceLiteralOffset(1 + referenceLiterals.size());
        referenceLiterals.add(literal);
        if (DebugHeap.isTagging()) {
            // Account for the DebugHeap tag in front of the code object:
            literalOffset += Word.size();
        }
        return -literalOffset;
    }

    protected void assignReferenceLiteralTemplateArgument(int parameterIndex, Object argument) {
        if (isAMD64()) {
            AMD64Assembler asm = (AMD64Assembler) this.asm;
            final CiRegister dst = cpuRegParams[parameterIndex];
            buf.mark();
            CiAddress src = new CiAddress(CiKind.Word, InstructionRelative.asValue(), createReferenceLiteral(argument));
            asm.movq(dst, src);
        } else {
            unimplISA();
        }
    }

    private void assignIntTemplateArgument(int parameterIndex, int argument) {
        if (isAMD64()) {
            AMD64Assembler asm = (AMD64Assembler) this.asm;
            final CiRegister dst = cpuRegParams[parameterIndex];
            asm.movl(dst, argument);
        } else {
            unimplISA();
        }
    }

    private void assignFloatTemplateArgument(int parameterIndex, float argument) {
        if (isAMD64()) {
            AMD64Assembler asm = (AMD64Assembler) this.asm;
            final CiRegister dst = fpuRegParams[parameterIndex];
            asm.movl(scratch, Intrinsics.floatToInt(argument));
            asm.movdl(dst, scratch);
        } else {
            unimplISA();
        }
    }

    private void assignLongTemplateArgument(int parameterIndex, long argument) {
        if (isAMD64()) {
            AMD64Assembler asm = (AMD64Assembler) this.asm;
            final CiRegister dst = cpuRegParams[parameterIndex];
            asm.movq(dst, argument);
        } else {
            unimplISA();
        }
    }

    private void assignDoubleTemplateArgument(int parameterIndex, double argument) {
        if (isAMD64()) {
            AMD64Assembler asm = (AMD64Assembler) this.asm;
            final CiRegister dst = fpuRegParams[parameterIndex];
            asm.movq(scratch, Intrinsics.doubleToLong(argument));
            asm.movdq(dst, scratch);
        } else {
            unimplISA();
        }
    }

    private void assignLocalDisplacementTemplateArgument(int parameterIndex, int localIndex, Kind kind) {
        // Long locals (ones that use two slots in the locals area) take two slots,
        // as required by the JVM spec. The value of the long local is stored in
        // the second slot so that it can be loaded/stored without further adjustments
        // to the stack/base pointer offsets.
        int slotIndex = !kind.isCategory1 ? (localIndex + 1) : localIndex;
        int slotOffset = frame.localVariableOffset(slotIndex) + JVMSFrameLayout.offsetInStackSlot(kind);
        assignIntTemplateArgument(parameterIndex, slotOffset);
    }

    private void processBytecode(int opcode) throws InternalError {
        switch (opcode) {
            // Checkstyle: stop

            case Bytecodes.NOP                : bciToPos[stream.currentBCI()] = buf.position(); break;
            case Bytecodes.AALOAD             : emit(AALOAD); break;
            case Bytecodes.AASTORE            : emit(AASTORE); break;
            case Bytecodes.ACONST_NULL        : emit(ACONST_NULL); break;
            case Bytecodes.ARRAYLENGTH        : emit(ARRAYLENGTH); break;
            case Bytecodes.ATHROW             : emit(ATHROW); break;
            case Bytecodes.BALOAD             : emit(BALOAD); break;
            case Bytecodes.BASTORE            : emit(BASTORE); break;
            case Bytecodes.CALOAD             : emit(CALOAD); break;
            case Bytecodes.CASTORE            : emit(CASTORE); break;
            case Bytecodes.D2F                : emit(D2F); break;
            case Bytecodes.D2I                : emit(D2I); break;
            case Bytecodes.D2L                : emit(D2L); break;
            case Bytecodes.DADD               : emit(DADD); break;
            case Bytecodes.DALOAD             : emit(DALOAD); break;
            case Bytecodes.DASTORE            : emit(DASTORE); break;
            case Bytecodes.DCMPG              : emit(DCMPG); break;
            case Bytecodes.DCMPL              : emit(DCMPL); break;
            case Bytecodes.DDIV               : emit(DDIV); break;
            case Bytecodes.DMUL               : emit(DMUL); break;
            case Bytecodes.DREM               : emit(DREM); break;
            case Bytecodes.DSUB               : emit(DSUB); break;
            case Bytecodes.DUP                : emit(DUP); break;
            case Bytecodes.DUP2               : emit(DUP2); break;
            case Bytecodes.DUP2_X1            : emit(DUP2_X1); break;
            case Bytecodes.DUP2_X2            : emit(DUP2_X2); break;
            case Bytecodes.DUP_X1             : emit(DUP_X1); break;
            case Bytecodes.DUP_X2             : emit(DUP_X2); break;
            case Bytecodes.F2D                : emit(F2D); break;
            case Bytecodes.F2I                : emit(F2I); break;
            case Bytecodes.F2L                : emit(F2L); break;
            case Bytecodes.FADD               : emit(FADD); break;
            case Bytecodes.FALOAD             : emit(FALOAD); break;
            case Bytecodes.FASTORE            : emit(FASTORE); break;
            case Bytecodes.FCMPG              : emit(FCMPG); break;
            case Bytecodes.FCMPL              : emit(FCMPL); break;
            case Bytecodes.FDIV               : emit(FDIV); break;
            case Bytecodes.FMUL               : emit(FMUL); break;
            case Bytecodes.FREM               : emit(FREM); break;
            case Bytecodes.FSUB               : emit(FSUB); break;
            case Bytecodes.I2B                : emit(I2B); break;
            case Bytecodes.I2C                : emit(I2C); break;
            case Bytecodes.I2D                : emit(I2D); break;
            case Bytecodes.I2F                : emit(I2F); break;
            case Bytecodes.I2L                : emit(I2L); break;
            case Bytecodes.I2S                : emit(I2S); break;
            case Bytecodes.IADD               : emit(IADD); break;
            case Bytecodes.IALOAD             : emit(IALOAD); break;
            case Bytecodes.IAND               : emit(IAND); break;
            case Bytecodes.IASTORE            : emit(IASTORE); break;
            case Bytecodes.ICONST_0           : emit(ICONST_0); break;
            case Bytecodes.ICONST_1           : emit(ICONST_1); break;
            case Bytecodes.ICONST_2           : emit(ICONST_2); break;
            case Bytecodes.ICONST_3           : emit(ICONST_3); break;
            case Bytecodes.ICONST_4           : emit(ICONST_4); break;
            case Bytecodes.ICONST_5           : emit(ICONST_5); break;
            case Bytecodes.ICONST_M1          : emit(ICONST_M1); break;
            case Bytecodes.IDIV               : emit(IDIV); break;
            case Bytecodes.IMUL               : emit(IMUL); break;
            case Bytecodes.INEG               : emit(INEG); break;
            case Bytecodes.IOR                : emit(IOR); break;
            case Bytecodes.IREM               : emit(IREM); break;
            case Bytecodes.ISHL               : emit(ISHL); break;
            case Bytecodes.ISHR               : emit(ISHR); break;
            case Bytecodes.ISUB               : emit(ISUB); break;
            case Bytecodes.IUSHR              : emit(IUSHR); break;
            case Bytecodes.IXOR               : emit(IXOR); break;
            case Bytecodes.L2D                : emit(L2D); break;
            case Bytecodes.L2F                : emit(L2F); break;
            case Bytecodes.L2I                : emit(L2I); break;
            case Bytecodes.LADD               : emit(LADD); break;
            case Bytecodes.LALOAD             : emit(LALOAD); break;
            case Bytecodes.LAND               : emit(LAND); break;
            case Bytecodes.LASTORE            : emit(LASTORE); break;
            case Bytecodes.LCMP               : emit(LCMP); break;
            case Bytecodes.LDIV               : emit(LDIV); break;
            case Bytecodes.LMUL               : emit(LMUL); break;
            case Bytecodes.LNEG               : emit(LNEG); break;
            case Bytecodes.LOR                : emit(LOR); break;
            case Bytecodes.LREM               : emit(LREM); break;
            case Bytecodes.LSHL               : emit(LSHL); break;
            case Bytecodes.LSHR               : emit(LSHR); break;
            case Bytecodes.LSUB               : emit(LSUB); break;
            case Bytecodes.LUSHR              : emit(LUSHR); break;
            case Bytecodes.LXOR               : emit(LXOR); break;
            case Bytecodes.MONITORENTER       : emit(MONITORENTER); break;
            case Bytecodes.MONITOREXIT        : emit(MONITOREXIT); break;
            case Bytecodes.POP                : emit(POP); break;
            case Bytecodes.POP2               : emit(POP2); break;
            case Bytecodes.SALOAD             : emit(SALOAD); break;
            case Bytecodes.SASTORE            : emit(SASTORE); break;
            case Bytecodes.SWAP               : emit(SWAP); break;
            case Bytecodes.LCONST_0           : emitLong(LCONST, 0L); break;
            case Bytecodes.LCONST_1           : emitLong(LCONST, 1L); break;
            case Bytecodes.DCONST_0           : emitDouble(DCONST, 0D); break;
            case Bytecodes.DCONST_1           : emitDouble(DCONST, 1D); break;
            case Bytecodes.DNEG               : emitDouble(DNEG, 0D); break;
            case Bytecodes.FCONST_0           : emitFloat(FCONST, 0F); break;
            case Bytecodes.FCONST_1           : emitFloat(FCONST, 1F); break;
            case Bytecodes.FCONST_2           : emitFloat(FCONST, 2F); break;
            case Bytecodes.FNEG               : emitFloat(FNEG, 0F); break;
            case Bytecodes.ARETURN            : emitReturn(ARETURN, ARETURN$unlockClass, ARETURN$unlockReceiver); break;
            case Bytecodes.DRETURN            : emitReturn(DRETURN, DRETURN$unlockClass, DRETURN$unlockReceiver); break;
            case Bytecodes.FRETURN            : emitReturn(FRETURN, FRETURN$unlockClass, FRETURN$unlockReceiver); break;
            case Bytecodes.IRETURN            : emitReturn(IRETURN, IRETURN$unlockClass, IRETURN$unlockReceiver); break;
            case Bytecodes.LRETURN            : emitReturn(LRETURN, LRETURN$unlockClass, LRETURN$unlockReceiver); break;
            case Bytecodes.RETURN             : emitReturn(RETURN, RETURN$unlockClass, RETURN$unlockReceiver); break;
            case Bytecodes.ALOAD              : emitVarAccess(ALOAD, stream.readLocalIndex(), Kind.REFERENCE); break;
            case Bytecodes.ALOAD_0            : emitVarAccess(ALOAD, 0, Kind.REFERENCE); break;
            case Bytecodes.ALOAD_1            : emitVarAccess(ALOAD, 1, Kind.REFERENCE); break;
            case Bytecodes.ALOAD_2            : emitVarAccess(ALOAD, 2, Kind.REFERENCE); break;
            case Bytecodes.ALOAD_3            : emitVarAccess(ALOAD, 3, Kind.REFERENCE); break;
            case Bytecodes.ASTORE             : emitVarAccess(ASTORE, stream.readLocalIndex(), Kind.REFERENCE); break;
            case Bytecodes.ASTORE_0           : emitVarAccess(ASTORE, 0, Kind.REFERENCE); break;
            case Bytecodes.ASTORE_1           : emitVarAccess(ASTORE, 1, Kind.REFERENCE); break;
            case Bytecodes.ASTORE_2           : emitVarAccess(ASTORE, 2, Kind.REFERENCE); break;
            case Bytecodes.ASTORE_3           : emitVarAccess(ASTORE, 3, Kind.REFERENCE); break;
            case Bytecodes.DLOAD              : emitVarAccess(DLOAD, stream.readLocalIndex(), Kind.DOUBLE); break;
            case Bytecodes.DLOAD_0            : emitVarAccess(DLOAD, 0, Kind.DOUBLE); break;
            case Bytecodes.DLOAD_1            : emitVarAccess(DLOAD, 1, Kind.DOUBLE); break;
            case Bytecodes.DLOAD_2            : emitVarAccess(DLOAD, 2, Kind.DOUBLE); break;
            case Bytecodes.DLOAD_3            : emitVarAccess(DLOAD, 3, Kind.DOUBLE); break;
            case Bytecodes.DSTORE             : emitVarAccess(DSTORE, stream.readLocalIndex(), Kind.DOUBLE); break;
            case Bytecodes.DSTORE_0           : emitVarAccess(DSTORE, 0, Kind.DOUBLE); break;
            case Bytecodes.DSTORE_1           : emitVarAccess(DSTORE, 1, Kind.DOUBLE); break;
            case Bytecodes.DSTORE_2           : emitVarAccess(DSTORE, 2, Kind.DOUBLE); break;
            case Bytecodes.DSTORE_3           : emitVarAccess(DSTORE, 3, Kind.DOUBLE); break;
            case Bytecodes.FLOAD              : emitVarAccess(FLOAD, stream.readLocalIndex(), Kind.FLOAT); break;
            case Bytecodes.FLOAD_0            : emitVarAccess(FLOAD, 0, Kind.FLOAT); break;
            case Bytecodes.FLOAD_1            : emitVarAccess(FLOAD, 1, Kind.FLOAT); break;
            case Bytecodes.FLOAD_2            : emitVarAccess(FLOAD, 2, Kind.FLOAT); break;
            case Bytecodes.FLOAD_3            : emitVarAccess(FLOAD, 3, Kind.FLOAT); break;
            case Bytecodes.FSTORE             : emitVarAccess(FSTORE, stream.readLocalIndex(), Kind.FLOAT); break;
            case Bytecodes.FSTORE_0           : emitVarAccess(FSTORE, 0, Kind.FLOAT); break;
            case Bytecodes.FSTORE_1           : emitVarAccess(FSTORE, 1, Kind.FLOAT); break;
            case Bytecodes.FSTORE_2           : emitVarAccess(FSTORE, 2, Kind.FLOAT); break;
            case Bytecodes.FSTORE_3           : emitVarAccess(FSTORE, 3, Kind.FLOAT); break;
            case Bytecodes.ILOAD              : emitVarAccess(ILOAD, stream.readLocalIndex(), Kind.INT); break;
            case Bytecodes.ILOAD_0            : emitVarAccess(ILOAD, 0, Kind.INT); break;
            case Bytecodes.ILOAD_1            : emitVarAccess(ILOAD, 1, Kind.INT); break;
            case Bytecodes.ILOAD_2            : emitVarAccess(ILOAD, 2, Kind.INT); break;
            case Bytecodes.ILOAD_3            : emitVarAccess(ILOAD, 3, Kind.INT); break;
            case Bytecodes.ISTORE             : emitVarAccess(ISTORE, stream.readLocalIndex(), Kind.INT); break;
            case Bytecodes.ISTORE_0           : emitVarAccess(ISTORE, 0, Kind.INT); break;
            case Bytecodes.ISTORE_1           : emitVarAccess(ISTORE, 1, Kind.INT); break;
            case Bytecodes.ISTORE_2           : emitVarAccess(ISTORE, 2, Kind.INT); break;
            case Bytecodes.ISTORE_3           : emitVarAccess(ISTORE, 3, Kind.INT); break;
            case Bytecodes.LSTORE             : emitVarAccess(LSTORE, stream.readLocalIndex(), Kind.LONG); break;
            case Bytecodes.LLOAD              : emitVarAccess(LLOAD, stream.readLocalIndex(), Kind.LONG); break;
            case Bytecodes.LLOAD_0            : emitVarAccess(LLOAD, 0, Kind.LONG); break;
            case Bytecodes.LLOAD_1            : emitVarAccess(LLOAD, 1, Kind.LONG); break;
            case Bytecodes.LLOAD_2            : emitVarAccess(LLOAD, 2, Kind.LONG); break;
            case Bytecodes.LLOAD_3            : emitVarAccess(LLOAD, 3, Kind.LONG); break;
            case Bytecodes.LSTORE_0           : emitVarAccess(LSTORE, 0, Kind.LONG); break;
            case Bytecodes.LSTORE_1           : emitVarAccess(LSTORE, 1, Kind.LONG); break;
            case Bytecodes.LSTORE_2           : emitVarAccess(LSTORE, 2, Kind.LONG); break;
            case Bytecodes.LSTORE_3           : emitVarAccess(LSTORE, 3, Kind.LONG); break;
            case Bytecodes.IFEQ               : emitBranch(EQ, IFEQ, stream.readBranchDest()); break;
            case Bytecodes.IFNE               : emitBranch(NE, IFNE, stream.readBranchDest()); break;
            case Bytecodes.IFLE               : emitBranch(LE, IFLE, stream.readBranchDest()); break;
            case Bytecodes.IFLT               : emitBranch(LT, IFLT, stream.readBranchDest()); break;
            case Bytecodes.IFGE               : emitBranch(GE, IFGE, stream.readBranchDest()); break;
            case Bytecodes.IFGT               : emitBranch(GT, IFGT, stream.readBranchDest()); break;
            case Bytecodes.IF_ICMPEQ          : emitBranch(EQ, IF_ICMPEQ, stream.readBranchDest()); break;
            case Bytecodes.IF_ICMPNE          : emitBranch(NE, IF_ICMPNE, stream.readBranchDest()); break;
            case Bytecodes.IF_ICMPGE          : emitBranch(GE, IF_ICMPGE, stream.readBranchDest()); break;
            case Bytecodes.IF_ICMPGT          : emitBranch(GT, IF_ICMPGT, stream.readBranchDest()); break;
            case Bytecodes.IF_ICMPLE          : emitBranch(LE, IF_ICMPLE, stream.readBranchDest()); break;
            case Bytecodes.IF_ICMPLT          : emitBranch(LT, IF_ICMPLT, stream.readBranchDest()); break;
            case Bytecodes.IF_ACMPEQ          : emitBranch(EQ, IF_ACMPEQ, stream.readBranchDest()); break;
            case Bytecodes.IF_ACMPNE          : emitBranch(NE, IF_ACMPNE, stream.readBranchDest()); break;
            case Bytecodes.IFNULL             : emitBranch(EQ, IFNULL,    stream.readBranchDest()); break;
            case Bytecodes.IFNONNULL          : emitBranch(NE, IFNONNULL, stream.readBranchDest()); break;
            case Bytecodes.GOTO               : emitBranch(null, GOTO, stream.readBranchDest()); break;
            case Bytecodes.GOTO_W             : emitBranch(null, GOTO_W, stream.readFarBranchDest()); break;
            case Bytecodes.GETFIELD           : emitFieldAccess(GETFIELDS, stream.readCPI()); break;
            case Bytecodes.GETSTATIC          : emitFieldAccess(GETSTATICS, stream.readCPI()); break;
            case Bytecodes.PUTFIELD           : emitFieldAccess(PUTFIELDS, stream.readCPI()); break;
            case Bytecodes.PUTSTATIC          : emitFieldAccess(PUTSTATICS, stream.readCPI()); break;
            case Bytecodes.ANEWARRAY          : emitTemplateWithClassConstant(ANEWARRAY, stream.readCPI()); break;
            case Bytecodes.CHECKCAST          : emitTemplateWithClassConstant(CHECKCAST, stream.readCPI()); break;
            case Bytecodes.INSTANCEOF         : emitTemplateWithClassConstant(INSTANCEOF, stream.readCPI()); break;
            case Bytecodes.BIPUSH             : emitInt(BIPUSH, stream.readByte()); break;
            case Bytecodes.SIPUSH             : emitInt(SIPUSH, stream.readShort()); break;
            case Bytecodes.NEW                : emitNew(stream.readCPI()); break;
            case Bytecodes.INVOKESPECIAL      : emitInvokespecial(stream.readCPI()); break;
            case Bytecodes.INVOKESTATIC       : emitInvokestatic(stream.readCPI()); break;
            case Bytecodes.INVOKEVIRTUAL      : emitInvokevirtual(stream.readCPI()); break;
            case Bytecodes.INVOKEINTERFACE    : emitInvokeinterface(stream.readCPI()); break;
            case Bytecodes.NEWARRAY           : emitNewarray(stream.readLocalIndex()); break;
            case Bytecodes.LDC                : emitConstant(stream.readCPI()); break;
            case Bytecodes.LDC_W              : emitConstant(stream.readCPI()); break;
            case Bytecodes.LDC2_W             : emitConstant(stream.readCPI()); break;
            case Bytecodes.TABLESWITCH        : emitTableswitch(); break;
            case Bytecodes.LOOKUPSWITCH       : emitLookupswitch(); break;
            case Bytecodes.IINC               : emitIinc(stream.readLocalIndex(), stream.readIncrement()); break;
            case Bytecodes.MULTIANEWARRAY     : emitMultianewarray(stream.readCPI(), stream.readUByte(stream.currentBCI() + 3)); break;


            case Bytecodes.UNSAFE_CAST        : break;
            case Bytecodes.WLOAD              : emitVarAccess(WLOAD, stream.readLocalIndex(), Kind.WORD); break;
            case Bytecodes.WLOAD_0            : emitVarAccess(WLOAD, 0, Kind.WORD); break;
            case Bytecodes.WLOAD_1            : emitVarAccess(WLOAD, 1, Kind.WORD); break;
            case Bytecodes.WLOAD_2            : emitVarAccess(WLOAD, 2, Kind.WORD); break;
            case Bytecodes.WLOAD_3            : emitVarAccess(WLOAD, 3, Kind.WORD); break;
            case Bytecodes.WSTORE             : emitVarAccess(WSTORE, stream.readLocalIndex(), Kind.WORD); break;
            case Bytecodes.WSTORE_0           : emitVarAccess(WSTORE, 0, Kind.WORD); break;
            case Bytecodes.WSTORE_1           : emitVarAccess(WSTORE, 1, Kind.WORD); break;
            case Bytecodes.WSTORE_2           : emitVarAccess(WSTORE, 2, Kind.WORD); break;
            case Bytecodes.WSTORE_3           : emitVarAccess(WSTORE, 3, Kind.WORD); break;

            case Bytecodes.WCONST_0           : emit(WCONST_0); break;
            case Bytecodes.WDIV               : emit(WDIV); break;
            case Bytecodes.WDIVI              : emit(WDIVI); break;
            case Bytecodes.WREM               : emit(WREM); break;
            case Bytecodes.WREMI              : emit(WREMI); break;

            case Bytecodes.MEMBAR:
            case Bytecodes.PCMPSWP:
            case Bytecodes.PGET:
            case Bytecodes.PSET:
            case Bytecodes.PREAD:
            case Bytecodes.PWRITE: {
                opcode = opcode | (stream.readCPI() << 8);
                switch (opcode) {
                    case Bytecodes.PREAD_BYTE         : emit(PREAD_BYTE); break;
                    case Bytecodes.PREAD_CHAR         : emit(PREAD_CHAR); break;
                    case Bytecodes.PREAD_SHORT        : emit(PREAD_SHORT); break;
                    case Bytecodes.PREAD_INT          : emit(PREAD_INT); break;
                    case Bytecodes.PREAD_LONG         : emit(PREAD_LONG); break;
                    case Bytecodes.PREAD_FLOAT        : emit(PREAD_FLOAT); break;
                    case Bytecodes.PREAD_DOUBLE       : emit(PREAD_DOUBLE); break;
                    case Bytecodes.PREAD_WORD         : emit(PREAD_WORD); break;
                    case Bytecodes.PREAD_REFERENCE    : emit(PREAD_REFERENCE); break;

                    case Bytecodes.PREAD_BYTE_I       : emit(PREAD_BYTE_I); break;
                    case Bytecodes.PREAD_CHAR_I       : emit(PREAD_CHAR_I); break;
                    case Bytecodes.PREAD_SHORT_I      : emit(PREAD_SHORT_I); break;
                    case Bytecodes.PREAD_INT_I        : emit(PREAD_INT_I); break;
                    case Bytecodes.PREAD_LONG_I       : emit(PREAD_LONG_I); break;
                    case Bytecodes.PREAD_FLOAT_I      : emit(PREAD_FLOAT_I); break;
                    case Bytecodes.PREAD_DOUBLE_I     : emit(PREAD_DOUBLE_I); break;
                    case Bytecodes.PREAD_WORD_I       : emit(PREAD_WORD_I); break;
                    case Bytecodes.PREAD_REFERENCE_I  : emit(PREAD_REFERENCE_I); break;

                    case Bytecodes.PWRITE_BYTE        : emit(PWRITE_BYTE); break;
                    case Bytecodes.PWRITE_SHORT       : emit(PWRITE_SHORT); break;
                    case Bytecodes.PWRITE_INT         : emit(PWRITE_INT); break;
                    case Bytecodes.PWRITE_LONG        : emit(PWRITE_LONG); break;
                    case Bytecodes.PWRITE_FLOAT       : emit(PWRITE_FLOAT); break;
                    case Bytecodes.PWRITE_DOUBLE      : emit(PWRITE_DOUBLE); break;
                    case Bytecodes.PWRITE_WORD        : emit(PWRITE_WORD); break;
                    case Bytecodes.PWRITE_REFERENCE   : emit(PWRITE_REFERENCE); break;

                    case Bytecodes.PWRITE_BYTE_I      : emit(PWRITE_BYTE_I); break;
                    case Bytecodes.PWRITE_SHORT_I     : emit(PWRITE_SHORT_I); break;
                    case Bytecodes.PWRITE_INT_I       : emit(PWRITE_INT_I); break;
                    case Bytecodes.PWRITE_LONG_I      : emit(PWRITE_LONG_I); break;
                    case Bytecodes.PWRITE_FLOAT_I     : emit(PWRITE_FLOAT_I); break;
                    case Bytecodes.PWRITE_DOUBLE_I    : emit(PWRITE_DOUBLE_I); break;
                    case Bytecodes.PWRITE_WORD_I      : emit(PWRITE_WORD_I); break;
                    case Bytecodes.PWRITE_REFERENCE_I : emit(PWRITE_REFERENCE_I); break;

                    case Bytecodes.PGET_BYTE          : emit(PGET_BYTE); break;
                    case Bytecodes.PGET_CHAR          : emit(PGET_CHAR); break;
                    case Bytecodes.PGET_SHORT         : emit(PGET_SHORT); break;
                    case Bytecodes.PGET_INT           : emit(PGET_INT); break;
                    case Bytecodes.PGET_LONG          : emit(PGET_LONG); break;
                    case Bytecodes.PGET_FLOAT         : emit(PGET_FLOAT); break;
                    case Bytecodes.PGET_DOUBLE        : emit(PGET_DOUBLE); break;
                    case Bytecodes.PGET_WORD          : emit(PGET_WORD); break;
                    case Bytecodes.PGET_REFERENCE     : emit(PGET_REFERENCE); break;

                    case Bytecodes.PSET_BYTE          : emit(PSET_BYTE); break;
                    case Bytecodes.PSET_SHORT         : emit(PSET_SHORT); break;
                    case Bytecodes.PSET_INT           : emit(PSET_INT); break;
                    case Bytecodes.PSET_LONG          : emit(PSET_LONG); break;
                    case Bytecodes.PSET_FLOAT         : emit(PSET_FLOAT); break;
                    case Bytecodes.PSET_DOUBLE        : emit(PSET_DOUBLE); break;
                    case Bytecodes.PSET_WORD          : emit(PSET_WORD); break;
                    case Bytecodes.PSET_REFERENCE     : emit(PSET_REFERENCE); break;

                    case Bytecodes.PCMPSWP_INT        : emit(PCMPSWP_INT); break;
                    case Bytecodes.PCMPSWP_WORD       : emit(PCMPSWP_WORD); break;
                    case Bytecodes.PCMPSWP_REFERENCE  : emit(PCMPSWP_REFERENCE); break;
                    case Bytecodes.PCMPSWP_INT_I      : emit(PCMPSWP_INT_I); break;
                    case Bytecodes.PCMPSWP_WORD_I     : emit(PCMPSWP_WORD_I); break;
                    case Bytecodes.PCMPSWP_REFERENCE_I: emit(PCMPSWP_REFERENCE_I); break;

                    case Bytecodes.MEMBAR_LOAD_LOAD   : emit(MEMBAR_LOAD_LOAD); break;
                    case Bytecodes.MEMBAR_LOAD_STORE  : emit(MEMBAR_LOAD_STORE); break;
                    case Bytecodes.MEMBAR_STORE_LOAD  : emit(MEMBAR_STORE_LOAD); break;
                    case Bytecodes.MEMBAR_STORE_STORE : emit(MEMBAR_STORE_STORE); break;

                    default                           : throw new CiBailout("Unsupported opcode" + errorSuffix());
                }
                break;
            }

            case Bytecodes.MOV_I2F            : emit(MOV_I2F); break;
            case Bytecodes.MOV_F2I            : emit(MOV_F2I); break;
            case Bytecodes.MOV_L2D            : emit(MOV_L2D); break;
            case Bytecodes.MOV_D2L            : emit(MOV_D2L); break;


            case Bytecodes.WRETURN            : emitReturn(WRETURN, WRETURN$unlockClass, WRETURN$unlockReceiver); break;
            case Bytecodes.PAUSE              : emit(PAUSE); break;
            case Bytecodes.LSB                : emit(LSB); break;
            case Bytecodes.MSB                : emit(MSB); break;

            case Bytecodes.RET                :
            case Bytecodes.JSR_W              :
            case Bytecodes.JSR                : throw new UnsupportedSubroutineException(opcode, stream.currentBCI());

            case Bytecodes.READREG            :
            case Bytecodes.WRITEREG           :
            case Bytecodes.ADD_SP             :
            case Bytecodes.INFOPOINT          :
            case Bytecodes.FLUSHW             :
            case Bytecodes.ALLOCA             :
            case Bytecodes.STACKHANDLE        :
            case Bytecodes.JNICALL            :
            case Bytecodes.TEMPLATE_CALL      :
            case Bytecodes.ICMP               :
            case Bytecodes.WCMP               :
            default                           : throw new CiBailout("Unsupported opcode" + errorSuffix());
            // Checkstyle: resume
        }
    }

    private String errorSuffix() {
        int opcode = stream.currentBC();
        String name = Bytecodes.nameOf(opcode);
        return " [bci=" + stream.currentBCI() + ", opcode=" + opcode + "(" + name + ")]";
    }

    /**
     * Records locations in the code buffer that need to be subsequently patched.
     */
    static class PatchInfo {
        /**
         * Encoded patch data. The encoding format is defined in the platform specific subclasses.
         */
        int[] data = new int[10];

        /**
         * The length of valid data in {@code data} (which may be less that {@code data.length}).
         */
        int size;

        void ensureCapacity(int minCapacity) {
            if (minCapacity > data.length) {
                int newCapacity = (size * 3) / 2 + 1;
                if (newCapacity < minCapacity) {
                    newCapacity = minCapacity;
                }
                data = Arrays.copyOf(data, newCapacity);
            }
        }

        int at(int i) {
            return data[i];
        }

        void reset() {
            size = 0;
        }
    }
}

@PLATFORM(cpu = "amd64")
class PatchInfoAMD64 extends PatchInfo {

    /**
     * Denotes a conditonal jump patch.
     * Encoding: {@code cc, pos, targetBCI}.
     */
    static final int JCC = 0;

    /**
     * Denotes an unconditonal jump patch.
     * Encoding: {@code pos, targetBCI}.
     */
    static final int JMP = 1;

    /**
     * Denotes a signed int jump table entry.
     * Encoding: {@code pos, jumpTablePos, targetBCI}.
     */
    static final int JUMP_TABLE_ENTRY = 2;

    /**
     * Denotes a signed int jump table entry.
     * Encoding: {@code pos, key, lookupTablePos, targetBCI}.
     */
    static final int LOOKUP_TABLE_ENTRY = 3;

    void addJCC(ConditionFlag cc, int pos, int targetBCI) {
        ensureCapacity(size + 4);
        data[size++] = JCC;
        data[size++] = cc.ordinal();
        data[size++] = pos;
        data[size++] = targetBCI;

    }

    void addJMP(int pos, int targetBCI) {
        ensureCapacity(size + 3);
        data[size++] = JMP;
        data[size++] = pos;
        data[size++] = targetBCI;
    }

    void addJumpTableEntry(int pos, int jumpTablePos, int targetBCI) {
        ensureCapacity(size + 4);
        data[size++] = JUMP_TABLE_ENTRY;
        data[size++] = pos;
        data[size++] = jumpTablePos;
        data[size++] = targetBCI;
    }

    void addLookupTableEntry(int pos, int key, int lookupTablePos, int targetBCI) {
        ensureCapacity(size + 5);
        data[size++] = LOOKUP_TABLE_ENTRY;
        data[size++] = pos;
        data[size++] = key;
        data[size++] = lookupTablePos;
        data[size++] = targetBCI;
    }
}
