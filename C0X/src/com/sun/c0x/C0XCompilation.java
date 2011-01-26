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
package com.sun.c0x;

import static java.lang.reflect.Modifier.*;

import java.util.*;

import com.sun.c1x.util.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * The {@code C0XCompiler} class is a sketch of a new baseline compiler design which borrows
 * ideas, basic infrastructure, and the runtime interface from C1X. The design is very simple--
 * always compile a single method at a time, with essentially no optimizations (except register
 * allocation) and no inlining. The result is a quick, single-pass compiler that compiles
 * a basic block at a time in a single pass, producing a compiled method that corresponds closely
 * enough to the original bytecodes that it can be debugged at the bytecode level and never
 * needs to be deoptimized.
 *
 * @author Ben L. Titzer
 */
public class C0XCompilation {

    enum Condition {
        eql("=="),
        neq("!="),
        lss("<"),
        leq("<="),
        gtr(">"),
        geq(">=");

        public final String operator;

        private Condition(String operator) {
            this.operator = operator;
        }

    }

    static class BlockState {
        boolean generated;     // whether block has been compiled yet
        int codeOffset;        // offset of start of block in emitted code
        FrameState entryState; // state at (first) entry
    }

    abstract static class Location {

    }

    static class Register extends Location {
        final int num;
        final CiKind kind;

        Register(int num, CiKind kind) {
            this.num = num;
            this.kind = kind;
        }

        @Override
        public String toString() {
            return "R" + num + ":" + kind;
        }
    }

    class StackSlot extends Location {

        final int index;

        StackSlot(int index) {
            this.index = index;
        }

        @Override
        public String toString() {
            return index < maxLocals ? "L" + index : "S" + (index - maxLocals);
        }
    }

    /**
     * The {@code FrameState} class represents an abstraction of the Java frame state for a
     * method, including the state of all local variables and operand stack
     * slots. Each compiled method has space in its frame for storing all Java frame
     * state at known offsets from the stack pointer. Registers that cache Java locals and
     * stack slots are spilled into the appropriate stack locations across calls.
     * Exception handlers always start with nothing in registers; code to spill registers
     * and/or shuffle the stack contents before entering an exception handler is generated
     * at the end of the method.
     */
    class FrameState {
        final Location[] memory;  // stores what the current memory contents are
        final Location[] state;   // stores where the most recent values are
        int stackIndex;

        FrameState() {
            this.memory = new Location[maxLocals + maxStack];
            this.state = new Location[maxLocals + maxStack];
            stackIndex = maxLocals;
        }

        FrameState(FrameState s) {
            memory = s.memory.clone();
            state = s.state.clone();
            stackIndex = s.stackIndex;
        }

        final FrameState copy() {
            return new FrameState(this);
        }
    }

    abstract class RegisterAllocator {
        abstract Register allocate(CiKind kind);
        abstract Register allocate(int physNum, CiKind kind);
        abstract void release(Register r);
        abstract void spill();

        abstract void push(Location l);
        abstract Location pop(boolean release);
        abstract void push2(Location l);
        abstract Location pop2(boolean release);
    }

    final byte[] bytecode;
    byte[] blockMap;
    final RiExceptionHandler[] handlers;

    final RiRuntime runtime;
    final RiMethod method;
    final CiTarget target;
    final CodeGen codeGen;
    RiConstantPool constantPool;
    final BlockState[] blockState;
    final int maxLocals;
    final int maxStack;
    StackSlot[] stackSlots;

    CiBailout bailout;
    FrameState currentState;
    int[] blockQueue = new int[3];
    int blockQueuePos;
    int regNum;
    RiRegisterConfig registerConfig;

    public C0XCompilation(RiRuntime runtime, RiMethod method, CiTarget target) {
        this.runtime = runtime;
        this.method = method;
        this.target = target;
        this.bytecode = method.code();
        this.blockState = new BlockState[this.bytecode.length];
        RiExceptionHandler[] hlist = method.exceptionHandlers();
        this.handlers = hlist.length == 0 ? null : hlist;
        this.maxLocals = method.maxLocals();
        this.maxStack = method.maxStackSize();
        codeGen = new X86CodeGen(this, target); // TODO: make portable
    }

    void compile() {
        try {
            markBlocks();
            emitCode();
        } catch (Throwable t) {
            bailout = new CiBailout("Unexpected exception while compiling: " + method, t);
            throw bailout;
        }
        emitCode();
    }

    private void emitCode() {
        int bci;

        emitPrologue();

        // build the state for the initial block from incoming parameters
        enqueue(0, initialFrameState());
        BytecodeStream stream = new BytecodeStream(bytecode);

        // compile all the mainline blocks
        while ((bci = dequeue()) >= 0) {
            compileBlock(bci, stream);
        }

        // TODO: move exception handlers into main queue and compile them

        // TODO: emit exception adapter code

        // TODO: finalize code, data, and references
    }

    private void markBlocks() {
        BlockMarker marker = new BlockMarker(method);
        marker.markBlocks();
        this.blockMap = marker.blockMap;
    }

    private FrameState initialFrameState() {
        FrameState frameState = new FrameState();
        // TODO: should initialize frame state from calling convention
        int index = 0;
        if (!isStatic(method.accessFlags())) {
            frameState.state[index] = produce(CiKind.Object);
            index = 1;
        }
        RiSignature sig = method.signature();
        int max = sig.argumentCount(false);
        for (int i = 0; i < max; i++) {
            CiKind kind = sig.argumentKindAt(index).stackKind();
            frameState.state[index] = produce(kind);
            index += kind.sizeInSlots();
        }
        return frameState;
    }

    int dequeue() {
        if (blockQueuePos <= 0) {
            return -1;
        }
        return blockQueue[--blockQueuePos];
    }

    FrameState enqueue(int bci, FrameState state) {
        BlockState bs = blockState[bci];
        if (bs == null) {
            bs = new BlockState();
            blockState[bci] = bs;
            bs.entryState = state;
            enqueue(bci);
            return null;
        } else if (bs.entryState == null) {
            bs.entryState = state;
            enqueue(bci);
            return null;
        } else {
            // already on the queue or generated
            return bs.entryState;
        }
    }

    private void enqueue(int bci) {
        if (blockQueue.length <= blockQueuePos) {
            blockQueue = Arrays.copyOf(blockQueue, blockQueue.length * 2);
        }
        blockQueue[blockQueuePos++] = bci;
    }

    /**
     * Compile a basic block from beginning to end.
     * @param bci the bytecode index of the start of the basic block
     * @param stream the bytecode stream
     */
    void compileBlock(int bci, BytecodeStream stream) {
        stream.setBCI(bci);

        BlockState blockState = this.blockState[bci];
        currentState = blockState.entryState;

        emitBlockPrologue(bci);

        if (BlockMarker.isExceptionEntry(bci, blockMap)) {
            emitSafepoint(bci);
            emitExceptionLoad(bci);
            emitInstrumentation(bci);
        }

        if (BlockMarker.isBackwardBranchTarget(bci, blockMap)) {
            emitSafepoint(bci);
            emitInstrumentation(bci);
        }

        int endBCI = stream.endBCI();
    bytecodeLoop:
        while (bci < endBCI) {
            // read the opcode
            int opcode = stream.currentBC();

            // record the start of the bytecode in the map
            recordBytecodeStart(bci, opcode);

            // check whether the bytecode can cause an exception
            if (Bytecodes.canTrap(opcode) && handlers != null) {
                handleException(bci);
            }

            // Checkstyle: stop
            switch (opcode) {
                case Bytecodes.NOP            : /* nothing to do */ break;
                case Bytecodes.ACONST_NULL    : push1(emitObject(null)); break;
                case Bytecodes.ICONST_M1      : push1(emitInt(-1)); break;
                case Bytecodes.ICONST_0       : push1(emitInt(0)); break;
                case Bytecodes.ICONST_1       : push1(emitInt(1)); break;
                case Bytecodes.ICONST_2       : push1(emitInt(2)); break;
                case Bytecodes.ICONST_3       : push1(emitInt(3)); break;
                case Bytecodes.ICONST_4       : push1(emitInt(4)); break;
                case Bytecodes.ICONST_5       : push1(emitInt(5)); break;
                case Bytecodes.LCONST_0       : push2(emitLong(0)); break;
                case Bytecodes.LCONST_1       : push2(emitLong(1)); break;
                case Bytecodes.FCONST_0       : push1(emitFloat(0f)); break;
                case Bytecodes.FCONST_1       : push1(emitFloat(1f)); break;
                case Bytecodes.FCONST_2       : push1(emitFloat(2f)); break;
                case Bytecodes.DCONST_0       : push2(emitDouble(0d)); break;
                case Bytecodes.DCONST_1       : push2(emitDouble(1d)); break;
                case Bytecodes.BIPUSH         : push1(emitInt(stream.readByte())); break;
                case Bytecodes.SIPUSH         : push1(emitInt(stream.readShort())); break;
                case Bytecodes.LDC            : // fall through
                case Bytecodes.LDC_W          : // fall through
                case Bytecodes.LDC2_W         : doLoadConstant(stream.readCPI()); break;
                case Bytecodes.ILOAD          : load1(stream.readLocalIndex()); break;
                case Bytecodes.LLOAD          : load2(stream.readLocalIndex()); break;
                case Bytecodes.FLOAD          : load1(stream.readLocalIndex()); break;
                case Bytecodes.DLOAD          : load2(stream.readLocalIndex()); break;
                case Bytecodes.ALOAD          : load1(stream.readLocalIndex()); break;
                case Bytecodes.ILOAD_0        : load1(0); break;
                case Bytecodes.ILOAD_1        : load1(1); break;
                case Bytecodes.ILOAD_2        : load1(2); break;
                case Bytecodes.ILOAD_3        : load1(3); break;
                case Bytecodes.LLOAD_0        : load2(0); break;
                case Bytecodes.LLOAD_1        : load2(1); break;
                case Bytecodes.LLOAD_2        : load2(2); break;
                case Bytecodes.LLOAD_3        : load2(3); break;
                case Bytecodes.FLOAD_0        : load1(0); break;
                case Bytecodes.FLOAD_1        : load1(1); break;
                case Bytecodes.FLOAD_2        : load1(2); break;
                case Bytecodes.FLOAD_3        : load1(3); break;
                case Bytecodes.DLOAD_0        : load2(0); break;
                case Bytecodes.DLOAD_1        : load2(1); break;
                case Bytecodes.DLOAD_2        : load2(2); break;
                case Bytecodes.DLOAD_3        : load2(3); break;
                case Bytecodes.ALOAD_0        : load1(0); break;
                case Bytecodes.ALOAD_1        : load1(1); break;
                case Bytecodes.ALOAD_2        : load1(2); break;
                case Bytecodes.ALOAD_3        : load1(3); break;
                case Bytecodes.IALOAD         : doArrayLoad(CiKind.Int   ); break;
                case Bytecodes.LALOAD         : doArrayLoad(CiKind.Long  ); break;
                case Bytecodes.FALOAD         : doArrayLoad(CiKind.Float ); break;
                case Bytecodes.DALOAD         : doArrayLoad(CiKind.Double); break;
                case Bytecodes.AALOAD         : doArrayLoad(CiKind.Object); break;
                case Bytecodes.BALOAD         : doArrayLoad(CiKind.Byte  ); break;
                case Bytecodes.CALOAD         : doArrayLoad(CiKind.Char  ); break;
                case Bytecodes.SALOAD         : doArrayLoad(CiKind.Short ); break;
                case Bytecodes.ISTORE         : store1(stream.readLocalIndex()); break;
                case Bytecodes.LSTORE         : store2(stream.readLocalIndex()); break;
                case Bytecodes.FSTORE         : store1(stream.readLocalIndex()); break;
                case Bytecodes.DSTORE         : store2(stream.readLocalIndex()); break;
                case Bytecodes.ASTORE         : store1(stream.readLocalIndex()); break;
                case Bytecodes.ISTORE_0       : // fall through
                case Bytecodes.FSTORE_0       : // fall through
                case Bytecodes.ASTORE_0       : store1(0); break;
                case Bytecodes.ISTORE_1       : // fall through
                case Bytecodes.FSTORE_1       : // fall through
                case Bytecodes.ASTORE_1       : store1(1); break;
                case Bytecodes.ISTORE_2       : // fall through
                case Bytecodes.FSTORE_2       : // fall through
                case Bytecodes.ASTORE_2       : store1(2); break;
                case Bytecodes.ISTORE_3       : // fall through
                case Bytecodes.FSTORE_3       : // fall through
                case Bytecodes.ASTORE_3       : store1(3); break;
                case Bytecodes.LSTORE_0       : // fall through
                case Bytecodes.DSTORE_0       : store2(0); break;
                case Bytecodes.LSTORE_1       : // fall through
                case Bytecodes.DSTORE_1       : store2(1); break;
                case Bytecodes.LSTORE_2       : // fall through
                case Bytecodes.DSTORE_2       : store2(2); break;
                case Bytecodes.LSTORE_3       : // fall through
                case Bytecodes.DSTORE_3       : store2(3); break;
                case Bytecodes.IASTORE        : doArrayStore(CiKind.Int   ); break;
                case Bytecodes.LASTORE        : doArrayStore(CiKind.Long  ); break;
                case Bytecodes.FASTORE        : doArrayStore(CiKind.Float ); break;
                case Bytecodes.DASTORE        : doArrayStore(CiKind.Double); break;
                case Bytecodes.AASTORE        : doArrayStore(CiKind.Object); break;
                case Bytecodes.BASTORE        : doArrayStore(CiKind.Byte  ); break;
                case Bytecodes.CASTORE        : doArrayStore(CiKind.Char  ); break;
                case Bytecodes.SASTORE        : doArrayStore(CiKind.Short ); break;
                case Bytecodes.POP            : // fall through
                case Bytecodes.POP2           : // fall through
                case Bytecodes.DUP            : // fall through
                case Bytecodes.DUP_X1         : // fall through
                case Bytecodes.DUP_X2         : // fall through
                case Bytecodes.DUP2           : // fall through
                case Bytecodes.DUP2_X1        : // fall through
                case Bytecodes.DUP2_X2        : // fall through
                case Bytecodes.SWAP           : doStackOp(opcode); break;
                case Bytecodes.IADD           : // fall through
                case Bytecodes.ISUB           : // fall through
                case Bytecodes.IMUL           : // fall through
                case Bytecodes.IDIV           : // fall through
                case Bytecodes.IREM           : // fall through
                case Bytecodes.ISHL           : // fall through
                case Bytecodes.ISHR           : // fall through
                case Bytecodes.IUSHR          : // fall through
                case Bytecodes.IAND           : // fall through
                case Bytecodes.IOR            : // fall through
                case Bytecodes.IXOR           : doIntOp2(opcode); break;
                case Bytecodes.INEG           : doIntNeg(opcode); break;
                case Bytecodes.LADD           : // fall through
                case Bytecodes.LSUB           : // fall through
                case Bytecodes.LMUL           : // fall through
                case Bytecodes.LDIV           : // fall through
                case Bytecodes.LREM           : // fall through
                case Bytecodes.LAND           : // fall through
                case Bytecodes.LOR            : // fall through
                case Bytecodes.LXOR           : doLongOp2(opcode); break;
                case Bytecodes.LSHL           : // fall through
                case Bytecodes.LSHR           : // fall through
                case Bytecodes.LUSHR          : doLongShift(opcode); break;
                case Bytecodes.LNEG           : doLongNeg(opcode); break;
                case Bytecodes.FADD           : // fall through
                case Bytecodes.FSUB           : // fall through
                case Bytecodes.FMUL           : // fall through
                case Bytecodes.FDIV           : // fall through
                case Bytecodes.FREM           : doFloatOp2(opcode); break;
                case Bytecodes.FNEG           : doFloatNeg(opcode); break;
                case Bytecodes.DADD           : // fall through
                case Bytecodes.DSUB           : // fall through
                case Bytecodes.DMUL           : // fall through
                case Bytecodes.DDIV           : // fall through
                case Bytecodes.DREM           : doDoubleOp2(opcode); break;
                case Bytecodes.DNEG           : doDoubleNeg(opcode); break;
                case Bytecodes.IINC           : doIncrement(stream.readLocalIndex()); break;
                case Bytecodes.I2L            : doConvert(opcode, CiKind.Int   , CiKind.Long  ); break;
                case Bytecodes.I2F            : doConvert(opcode, CiKind.Int   , CiKind.Float ); break;
                case Bytecodes.I2D            : doConvert(opcode, CiKind.Int   , CiKind.Double); break;
                case Bytecodes.L2I            : doConvert(opcode, CiKind.Long  , CiKind.Int   ); break;
                case Bytecodes.L2F            : doConvert(opcode, CiKind.Long  , CiKind.Float ); break;
                case Bytecodes.L2D            : doConvert(opcode, CiKind.Long  , CiKind.Double); break;
                case Bytecodes.F2I            : doConvert(opcode, CiKind.Float , CiKind.Int   ); break;
                case Bytecodes.F2L            : doConvert(opcode, CiKind.Float , CiKind.Long  ); break;
                case Bytecodes.F2D            : doConvert(opcode, CiKind.Float , CiKind.Double); break;
                case Bytecodes.D2I            : doConvert(opcode, CiKind.Double, CiKind.Int   ); break;
                case Bytecodes.D2L            : doConvert(opcode, CiKind.Double, CiKind.Long  ); break;
                case Bytecodes.D2F            : doConvert(opcode, CiKind.Double, CiKind.Float ); break;
                case Bytecodes.I2B            : doConvert(opcode, CiKind.Int   , CiKind.Byte  ); break;
                case Bytecodes.I2C            : doConvert(opcode, CiKind.Int   , CiKind.Char  ); break;
                case Bytecodes.I2S            : doConvert(opcode, CiKind.Int   , CiKind.Short ); break;
                case Bytecodes.LCMP           : doCompareOp(CiKind.Long, opcode); break;
                case Bytecodes.FCMPL          : doCompareOp(CiKind.Float, opcode); break;
                case Bytecodes.FCMPG          : doCompareOp(CiKind.Float, opcode); break;
                case Bytecodes.DCMPL          : doCompareOp(CiKind.Double, opcode); break;
                case Bytecodes.DCMPG          : doCompareOp(CiKind.Double, opcode); break;
                case Bytecodes.IFEQ           : doIfZero(Condition.eql, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IFNE           : doIfZero(Condition.neq, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IFLT           : doIfZero(Condition.lss, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IFGE           : doIfZero(Condition.geq, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IFGT           : doIfZero(Condition.gtr, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IFLE           : doIfZero(Condition.leq, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IF_ICMPEQ      : doIfSame(CiKind.Int, Condition.eql, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IF_ICMPNE      : doIfSame(CiKind.Int, Condition.neq, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IF_ICMPLT      : doIfSame(CiKind.Int, Condition.lss, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IF_ICMPGE      : doIfSame(CiKind.Int, Condition.geq, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IF_ICMPGT      : doIfSame(CiKind.Int, Condition.gtr, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IF_ICMPLE      : doIfSame(CiKind.Int, Condition.leq, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IF_ACMPEQ      : doIfSame(CiKind.Object, Condition.eql, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IF_ACMPNE      : doIfSame(CiKind.Object, Condition.neq, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IFNULL         : doIfNull(Condition.eql, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IFNONNULL      : doIfNull(Condition.neq, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.GOTO           : doGoto(stream.currentBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.JSR            : doJsr(bci, stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.GOTO_W         : doGoto(stream.currentBCI(), stream.readFarBranchDest()); break bytecodeLoop;
                case Bytecodes.JSR_W          : doJsr(bci, stream.readFarBranchDest()); break;
                case Bytecodes.RET            : doRet(stream.readLocalIndex());  break bytecodeLoop;
                case Bytecodes.TABLESWITCH    : doTableswitch(new BytecodeTableSwitch(bytecode, bci)); break bytecodeLoop;
                case Bytecodes.LOOKUPSWITCH   : doLookupswitch(new BytecodeLookupSwitch(bytecode, bci)); break bytecodeLoop;
                case Bytecodes.IRETURN        : doReturn(CiKind.Int, pop1()); break bytecodeLoop;
                case Bytecodes.FRETURN        : doReturn(CiKind.Float, pop1()); break bytecodeLoop;
                case Bytecodes.ARETURN        : doReturn(CiKind.Object, pop1()); break bytecodeLoop;
                case Bytecodes.LRETURN        : doReturn(CiKind.Long, pop2()); break bytecodeLoop;
                case Bytecodes.DRETURN        : doReturn(CiKind.Double, pop2()); break bytecodeLoop;
                case Bytecodes.RETURN         : doReturn(CiKind.Void, null); break bytecodeLoop;
                case Bytecodes.ATHROW         : doThrow(bci); break bytecodeLoop;
                case Bytecodes.GETSTATIC      : doGetStatic(constantPool().lookupField(stream.readCPI(), opcode)); break;
                case Bytecodes.PUTSTATIC      : doPutStatic(constantPool().lookupField(stream.readCPI(), opcode)); break;
                case Bytecodes.GETFIELD       : doGetField(constantPool().lookupField(stream.readCPI(), opcode)); break;
                case Bytecodes.PUTFIELD       : doPutField(constantPool().lookupField(stream.readCPI(), opcode)); break;
                case Bytecodes.INVOKEVIRTUAL  : doInvokeVirtual(constantPool().lookupMethod(stream.readCPI(), opcode)); break;
                case Bytecodes.INVOKESPECIAL  : doInvokeSpecial(constantPool().lookupMethod(stream.readCPI(), opcode)); break;
                case Bytecodes.INVOKESTATIC   : doInvokeStatic(constantPool().lookupMethod(stream.readCPI(), opcode)); break;
                case Bytecodes.INVOKEINTERFACE: doInvokeInterface(constantPool().lookupMethod(stream.readCPI(), opcode)); break;
                case Bytecodes.NEW            : doNewInstance(stream.readCPI()); break;
                case Bytecodes.NEWARRAY       : doNewTypeArray(stream.readLocalIndex()); break;
                case Bytecodes.ANEWARRAY      : doNewObjectArray(stream.readCPI()); break;
                case Bytecodes.ARRAYLENGTH    : doArrayLength(); break;
                case Bytecodes.CHECKCAST      : doCheckCast(stream.readCPI()); break;
                case Bytecodes.INSTANCEOF     : doInstanceOf(stream.readCPI()); break;
                case Bytecodes.MONITORENTER   : doMonitorEnter(bci); break;
                case Bytecodes.MONITOREXIT    : doMonitorExit(bci); break;
                case Bytecodes.MULTIANEWARRAY : doNewMultiArray(stream.readCPI(), stream.readUByte(bci + 3)); break;
                case Bytecodes.BREAKPOINT     : doBreakpoint(bci); break;
                default                       : doUnknownBytecode(bci, opcode); break;
            }
            // Checkstyle: resume

            bci = stream.nextBCI();
            stream.next();
            if (BlockMarker.isBlockStart(bci, blockMap)) {
                // fell through to the next block
                enqueue(bci, currentState);
                break;
            }
        }

        blockState.generated = true;
    }

    void recordBytecodeStart(int bci, int opcode) {
        // printBytecodeStart(bci, opcode);
    }

    void emitInstrumentation(int bci) {
        // printString("block_counter @" + bci);
    }

    void emitExceptionLoad(int bci) {
        currentState.stackIndex = maxLocals; // clear the Java operand stack
        Location r = produce(CiKind.Object);
        // printString(r + " = exception_load()");
        push1(r);
    }

    void emitSafepoint(int bci) {
        // printString("safepoint @" + bci);
    }

    private void doUnknownBytecode(int bci, int opcode) {
        throw Util.shouldNotReachHere();
    }

    private void doBreakpoint(int bci) {
        codeGen.genBreakpoint(bci);
    }

    private void doNewMultiArray(char cpi, int rank) {
        RiType type = constantPool().lookupType(cpi, Bytecodes.MULTIANEWARRAY);
        Location[] lengths = popN(rank);
        Location r = codeGen.genNewMultiArray(type, lengths);
        push1(r);
    }

    private void doMonitorExit(int bci) {
        Location object = pop1();
        codeGen.genMonitorExit(object);
    }

    private void doMonitorEnter(int bci) {
        Location object = pop1();
        codeGen.genMonitorEnter(object);
    }

    private void doInstanceOf(char cpi) {
        RiType type = constantPool().lookupType(cpi, Bytecodes.INSTANCEOF);
        Location object = pop1();
        Location r = codeGen.genInstanceOf(type, object);
        push1(r);
    }

    private void doCheckCast(char cpi) {
        RiType type = constantPool().lookupType(cpi, Bytecodes.CHECKCAST);
        Location object = pop1();
        codeGen.genCheckCast(type, object);
        push1(object);
    }

    private void doArrayLength() {
        Location object = pop1();
        Location r = codeGen.genArrayLength(object);
        push1(r);
    }

    private void doNewObjectArray(char cpi) {
        RiType type = constantPool().lookupType(cpi, Bytecodes.ANEWARRAY);
        Location length = pop1();
        Location r = codeGen.genNewObjectArray(type, length);
        push1(r);
    }

    private void doNewTypeArray(int typeCode) {
        CiKind elemType = CiKind.fromArrayTypeCode(typeCode);
        Location length = pop1();
        Location r = codeGen.genNewTypeArray(elemType, length);
        push1(r);
    }

    private void doNewInstance(char cpi) {
        RiType type = constantPool().lookupType(cpi, Bytecodes.NEW);
        Location r = codeGen.genNewInstance(type);
        push1(r);
    }

    private void doInvokeInterface(RiMethod riMethod) {
        Location[] args = popN(riMethod.signature().argumentSlots(true));
        CiKind retType = riMethod.signature().returnKind();
        Location r = codeGen.genInvokeInterface(riMethod, args);
        pushZ(r, retType);
    }

    private void doInvokeStatic(RiMethod riMethod) {
        Location[] args = popN(riMethod.signature().argumentSlots(false));
        CiKind retType = riMethod.signature().returnKind();
        Location r = codeGen.genInvokeStatic(riMethod, args);
        pushZ(r, retType);
    }

    private void doInvokeSpecial(RiMethod riMethod) {
        Location[] args = popN(riMethod.signature().argumentSlots(true));
        CiKind retType = riMethod.signature().returnKind();
        Location r = codeGen.genInvokeSpecial(riMethod, args);
        pushZ(r, retType);
    }

    private void doInvokeVirtual(RiMethod riMethod) {
        Location[] args = popN(riMethod.signature().argumentSlots(true));
        CiKind retType = riMethod.signature().returnKind();
        Location r = codeGen.genInvokeVirtual(riMethod, args);
        pushZ(r, retType);
    }

    private void doPutField(RiField riField) {
        Location object = pop1();
        Location value = popX(riField.kind());
        codeGen.genPutField(riField, object, value);
    }

    private void doGetField(RiField riField) {
        Location object = pop1();
        Location r = codeGen.genGetField(riField, object);
        pushX(r, riField.kind());
    }

    private void doPutStatic(RiField riField) {
        Location value = popX(riField.kind());
        codeGen.getPutStatic(riField, value);
    }

    void doGetStatic(RiField riField) {
        Location r = codeGen.genGetStatic(riField);
        pushX(r, riField.kind());
    }

    private void doThrow(int i) {
        Location thrown = pop1();
        codeGen.genThrow(thrown);
    }

    private void doReturn(CiKind kind, Location value) {
        codeGen.genReturn(kind, value);
    }

    private void doTableswitch(BytecodeTableSwitch bytecodeTableSwitch) {
        Location key = pop1();
        codeGen.genTableswitch(bytecodeTableSwitch, key);
    }

    private void doLookupswitch(BytecodeLookupSwitch bytecodeLookupSwitch) {
        Location key = pop1();
        codeGen.genLookupswitch(bytecodeLookupSwitch, key);
    }

    private void doRet(int index) {
        Location r = currentState.state[index];
        codeGen.genRet(r);
    }

    private void doJsr(int bci, int targetBCI) {
        Location r = codeGen.genJsr(bci, targetBCI);
        push1(r);
        enqueue(targetBCI, currentState);
    }

    private void doGoto(int bci, int targetBCI) {
        codeGen.genGoto(bci, targetBCI);
        enqueue(targetBCI, currentState);
    }

    private void doIfNull(Condition cond, int nextBCI, int targetBCI) {
        Location obj = pop1();
        codeGen.genIfNull(cond, obj, nextBCI, targetBCI);
        enqueue(nextBCI, currentState);
        enqueue(targetBCI, currentState.copy());
    }

    private void doIfSame(CiKind kind, Condition cond, int nextBCI, int targetBCI) {
        Location y = popX(kind);
        Location x = popX(kind);
        codeGen.genIfSame(cond, x, y, nextBCI, targetBCI);
        enqueue(nextBCI, currentState);
        enqueue(targetBCI, currentState.copy());
    }

    private void doIfZero(Condition cond, int nextBCI, int targetBCI) {
        Location val = pop1();
        codeGen.genIfZero(cond, val, nextBCI, targetBCI);
        enqueue(nextBCI, currentState);
        enqueue(targetBCI, currentState.copy());
    }

    private void doIncrement(int index) {
        Location l = currentState.state[index];
        Location r = codeGen.genIncrement(l);
        currentState.state[index] = r;
    }

    private void doCompareOp(CiKind kind, int opcode) {
        Location y = popX(kind);
        Location x = popX(kind);
        Location r = codeGen.genCompareOp(kind, opcode, x, y);
        push1(r);
    }

    private void doConvert(int opcode, CiKind from, CiKind to) {
        Location value = popX(from);
        Location r = codeGen.genConvert(opcode, from, to, value);
        pushX(r, to);
    }

    private void doArrayLoad(CiKind kind) {
        Location index = pop1();
        Location array = pop1();
        Location r = codeGen.genArrayLoad(kind, array, index);
        pushX(r, kind);
    }

    private void doArrayStore(CiKind kind) {
        Location value = popX(kind);
        Location index = pop1();
        Location array = pop1();
        codeGen.genArrayStore(kind, array, index, value);
    }

    private void doIntOp2(int opcode) {
        Location y = pop1();
        Location x = pop1();
        Location r = codeGen.genIntOp2(opcode, x, y);
        push1(r);
    }

    private void doLongOp2(int opcode) {
        Location y = pop2();
        Location x = pop2();
        Location r = codeGen.genLongOp2(opcode, x, y);
        push2(r);
    }

    private void doLongShift(int opcode) {
        Location y = pop1();
        Location x = pop2();
        Location r = codeGen.genLongOp2(opcode, x, y);
        push2(r);
    }

    private void doFloatOp2(int opcode) {
        Location y = pop1();
        Location x = pop1();
        Location r = codeGen.genFloatOp2(opcode, x, y);
        push1(r);
    }

    private void doDoubleOp2(int opcode) {
        Location y = pop2();
        Location x = pop2();
        Location r = codeGen.genDoubleOp2(opcode, x, y);
        push2(r);
    }

    private void doIntNeg(int opcode) {
        Location x = pop1();
        Location r = codeGen.genIntNeg(opcode, x);
        push1(r);
    }

    private void doLongNeg(int opcode) {
        Location x = pop2();
        Location r = codeGen.genLongNeg(opcode, x);
        push2(r);
    }

    private void doFloatNeg(int opcode) {
        Location x = pop1();
        Location r = codeGen.genFloatNeg(opcode, x);
        push1(r);
    }

    private void doDoubleNeg(int opcode) {
        Location x = pop2();
        Location r = codeGen.genDoubleNeg(opcode, x);
        push2(r);
    }

    private void doLoadConstant(int bci) {
        Object con = constantPool().lookupConstant((char) bci);

        if (con instanceof RiType) {
            // this is a load of class constant which might be unresolved
            RiType ritype = (RiType) con;
            Location r;
            if (!ritype.isResolved()) {
                r = codeGen.genResolveClass(ritype);
            } else {
                r = codeGen.genObjectConstant(ritype.javaClass());
            }
            push1(r);
            return;
        } else if (con instanceof CiConstant) {
            CiConstant constant = (CiConstant) con;
            switch (constant.kind.stackKind()) {
                case Int:    push1(codeGen.genIntConstant(constant.asInt())); return;
                case Long:   push2(codeGen.genLongConstant(constant.asLong())); return;
                case Float:  push1(codeGen.genFloatConstant(constant.asFloat())); return;
                case Double: push2(codeGen.genDoubleConstant(constant.asDouble())); return;
                case Object: push1(codeGen.genObjectConstant(constant.asObject())); return;
            }
        }
        throw new Error("lookupConstant returned an object of incorrect type");
    }

    private void doStackOp(int opcode) {
        switch (opcode) {
            case Bytecodes.POP: {
                pop1();
                break;
            }
            case Bytecodes.POP2: {
                pop1();
                pop1();
                break;
            }
            case Bytecodes.DUP: {
                Location w = pop1();
                push1(w);
                push1(w);
                break;
            }
            case Bytecodes.DUP_X1: {
                Location w1 = pop1();
                Location w2 = pop1();
                push1(w1);
                push1(w2);
                push1(w1);
                break;
            }
            case Bytecodes.DUP_X2: {
                Location w1 = pop1();
                Location w2 = pop1();
                Location w3 = pop1();
                push1(w1);
                push1(w3);
                push1(w2);
                push1(w1);
                break;
            }
            case Bytecodes.DUP2: {
                Location w1 = pop1();
                Location w2 = pop1();
                push1(w2);
                push1(w1);
                push1(w2);
                push1(w1);
                break;
            }
            case Bytecodes.DUP2_X1: {
                Location w1 = pop1();
                Location w2 = pop1();
                Location w3 = pop1();
                push1(w2);
                push1(w1);
                push1(w3);
                push1(w2);
                push1(w1);
                break;
            }
            case Bytecodes.DUP2_X2: {
                Location w1 = pop1();
                Location w2 = pop1();
                Location w3 = pop1();
                Location w4 = pop1();
                push1(w2);
                push1(w1);
                push1(w4);
                push1(w3);
                push1(w2);
                push1(w1);
                break;
            }
            case Bytecodes.SWAP: {
                Location w1 = pop1();
                Location w2 = pop1();
                push1(w1);
                push1(w2);
                break;
            }
            default:
                throw Util.shouldNotReachHere();
        }
    }

    void load1(int index) {
        currentState.state[currentState.stackIndex++] = currentState.state[index];
    }

    void store1(int index) {
        currentState.state[index] = currentState.state[--currentState.stackIndex];
    }

    void load2(int index) {
        currentState.state[currentState.stackIndex++] = currentState.state[index];
        currentState.state[currentState.stackIndex++] = null;
    }

    void store2(int index) {
        currentState.state[index] = currentState.state[--currentState.stackIndex];
        currentState.state[index] = currentState.state[--currentState.stackIndex];
    }

    void push1(Location val) {
        currentState.state[currentState.stackIndex++] = val;
    }

    void pushX(Location val, CiKind kind) {
        if (kind.isDoubleWord()) {
            push2(val);
        } else {
            push1(val);
        }
    }

    Location pushZ(CiKind retType) {
        Location r = null;
        if (retType != CiKind.Void) {
            pushX(r = produce(retType), retType);
        }
        return r;
    }

    Location pushZ(Location r, CiKind retType) {
        if (retType != CiKind.Void) {
            pushX(r, retType);
        }
        return r;
    }

    Location pop1() {
        return currentState.state[--currentState.stackIndex];
    }

    Location[] popN(int count) {
        // pop multiple arguments
        Location[] result = new Location[count];
        for (int i = 0; i < count; i++) {
            result[i] = currentState.state[i + (currentState.stackIndex - count)];
        }
        currentState.stackIndex -= count;
        return result;
    }

    Location popX(CiKind kind) {
        return kind.isDoubleWord() ? pop2() : pop1();
    }

    void push2(Location val) {
        currentState.state[currentState.stackIndex++] = val;
        currentState.state[currentState.stackIndex++] = null;
    }

    Location pop2() {
        --currentState.stackIndex;
        return currentState.state[--currentState.stackIndex];
    }

    Location produce(CiKind kind) {
        return new Register(regNum++, kind);
    }

    RiConstantPool constantPool() {
        if (constantPool == null) {
            constantPool = runtime.getConstantPool(method);
        }
        return constantPool;
    }

    void handleException(int bci) {
        FrameState state = null;
        for (RiExceptionHandler h : handlers) {
            // XXX: could be sped up if handlers are sorted by startBCI
            if (h.startBCI() <= bci && bci < h.endBCI()) {
                if (state == null) {
                    state = currentState.copy();
                    deferExceptionAdapter(state, bci, h);
                }
            }
        }
    }

    void deferExceptionAdapter(FrameState state, int bci, RiExceptionHandler h) {
        Util.nonFatalUnimplemented("defer the exception adapter state");
    }

    void spillLocals(FrameState state, boolean kill) {
        spillSome(state, maxLocals, kill); // spill all local variables into frame
    }

    void spillAll(FrameState state, boolean kill) {
        spillSome(state, state.stackIndex, kill); // spill all values into frame
    }

    void spillSome(FrameState state, int max, boolean kill) {
        for (int i = 0; i < max; i++) {
            Location cur = state.state[i];
            if (cur != null && cur != state.memory[i]) {
                spill(state, i, kill);
            }
        }
    }

    void spill(FrameState state, int index, boolean kill) {
        // generate code to move the value from its current location into the stack
        Location spillLocation = stackSlot(index);
        emitMove(state.state[index], spillLocation);
        if (kill) {
            // if we are killing values, then the new current location is on the stack
            state.memory[index] = state.state[index] = spillLocation;
        }
    }

    void emitMove(Location from, Location to) {
        // printString(to + " = " + from);
    }

    private void emitPrologue() {
        // printPrologue(method);
    }

    private void emitBlockPrologue(int bci) {
        // printBlockPrologue(this, bci);
    }

    Location emitResolveClass(RiType ritype) {
        Location r = produce(CiKind.Object);
        // printOp(r, "resolve_class:" + ritype);
        return r;
    }

    Location emitInt(int val) {
        return codeGen.genIntConstant(val);
    }

    Location emitLong(long val) {
        return codeGen.genLongConstant(val);
    }

    Location emitFloat(float val) {
        return codeGen.genFloatConstant(val);
    }

    Location emitDouble(double val) {
        return codeGen.genDoubleConstant(val);
    }

    Location emitObject(Object val) {
        return codeGen.genObjectConstant(val);
    }

    StackSlot stackSlot(int index) {
        if (stackSlots == null) {
            // initialize the stack slot array lazily
            stackSlots = new StackSlot[maxLocals + maxStack];
            for (int k = 0; k < stackSlots.length; k++) {
                stackSlots[k] = new StackSlot(k);
            }
        }
        return stackSlots[index];
    }
}
