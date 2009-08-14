/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c0x;

import com.sun.c1x.ci.*;
import com.sun.c1x.bytecode.*;
import com.sun.c1x.value.BasicType;
import com.sun.c1x.util.Util;

import java.util.List;
import java.io.PrintStream;

/**
 * The <code>C0XCompiler</code> class is a sketch of a new baseline compiler design which borrows
 * ideas, basic infrastructure, and the runtime interface from C1X. The design is very simple--
 * always compile a single method at a time, with essentially no optimizations (except register
 * allocation) and no inlining. The result is a quick, single-pass compiler that compiles
 * a basic block at a time in a single pass, producing a compiled method that corresponds closely
 * enough to the original bytecodes that it can be debugged at the bytecode level and never
 * needs to be deoptimized.
 *
 * @author Ben L. Titzer
 */
public class C0XCompiler {

    private static final int BLOCK_START = 1;
    private static final int BLOCK_BACKWARD_TARGET = 2;
    private static final int BLOCK_EXCEPTION_ENTRY = 4;

    private static final PrintStream out = System.out;

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

    abstract class Location {

    }

    class Register extends Location {
        final int num;
        final BasicType type;
        Register(int num, BasicType type) {
            this.num = num;
            this.type = type;
        }

        @Override
        public String toString() {
            return "r" + num + ": " + type;
        }
    }

    class StackSlot extends Location {

        final int index;

        StackSlot(int index) {
            this.index = index;
        }

        @Override
        public String toString() {
            return "s" + index;
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

    final CiRuntime runtime;
    final CiMethod method;
    final CiBytecodeExtension extension;
    CiConstantPool constantPool;
    final byte[] bytecode;
    final byte[] blockMap;
    final BlockState[] blockState;
    final int maxLocals;
    final int maxStack;
    final boolean hasHandler;
    final List<CiExceptionHandler> handlers;

    FrameState currentState;
    int[] queue = new int[3];
    int queuePos;
    int regNum;

    public C0XCompiler(CiRuntime runtime, CiMethod method, CiBytecodeExtension extension) {
        this.runtime = runtime;
        this.method = method;
        this.extension = extension;
        this.bytecode = method.code();
        this.blockMap = new byte[this.bytecode.length]; // TODO: get block map
        this.blockState = new BlockState[this.bytecode.length];
        this.handlers = method.exceptionHandlers();
        this.maxLocals = method.maxLocals();
        this.maxStack = method.maxStackSize();
        this.hasHandler = method.isSynchronized() || handlers != null && handlers.size() > 0;
    }

    void compile() {
        // build the state for the initial block (incoming parameters)
        int bci;

        enqueue(0, initialFrameState());
        BytecodeStream stream = new BytecodeStream(bytecode);

        // compile all the mainline blocks
        while ((bci = dequeue()) >= 0) {
            compileBlock(bci, stream);
        }

        // move exception handlers into main queue and compile them
        while ((bci = dequeue()) >= 0) {
            compileBlock(bci, stream);
        }

        // emit exception adapter code

        // finalize code, data, and references
    }

    private FrameState initialFrameState() {
        FrameState frameState = new FrameState();
        // TODO: for testing purposes, just make everything on the stack
        // (should initialize frame state from calling convention)
        for (int i = 0; i < maxLocals; i++) {
            frameState.state[i] = frameState.memory[i] = new StackSlot(i);
        }
        return frameState;
    }

    int dequeue() {
        if (queuePos <= 0) {
            return -1;
        }
        return queue[--queuePos];
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
        if (queue.length <= queuePos) {
            int[] nqueue = new int[queue.length * 2];
            for (int i = 0; i < queue.length; i++) {
                nqueue[i] = queue[i];
            }
            queue = nqueue;
        }
        queue[queuePos++] = bci;
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

        if (isExceptionEntry(bci)) {
            emitSafepoint(bci);
            emitExceptionLoad(bci);
            emitInstrumentation(bci);
        }

        if (isBackwardBranchTarget(bci)) {
            emitSafepoint(bci);
            emitInstrumentation(bci);
        }

        int endBCI = stream.endBCI();
    bytecodeLoop:
        while (bci < endBCI) {
            // read the opcode
            int opcode = stream.currentBC();

            // record the start of the bytecode in the map

            // check whether the bytecode can cause an exception
            if (Bytecodes.canTrap(opcode) && hasHandler) {
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
                case Bytecodes.LDC2_W         : loadConstant(bci); break;
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
                case Bytecodes.IALOAD         : arrayLoad(BasicType.Int   ); break;
                case Bytecodes.LALOAD         : arrayLoad(BasicType.Long  ); break;
                case Bytecodes.FALOAD         : arrayLoad(BasicType.Float ); break;
                case Bytecodes.DALOAD         : arrayLoad(BasicType.Double); break;
                case Bytecodes.AALOAD         : arrayLoad(BasicType.Object); break;
                case Bytecodes.BALOAD         : arrayLoad(BasicType.Byte  ); break;
                case Bytecodes.CALOAD         : arrayLoad(BasicType.Char  ); break;
                case Bytecodes.SALOAD         : arrayLoad(BasicType.Short ); break;
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
                case Bytecodes.IASTORE        : arrayStore(BasicType.Int   ); break;
                case Bytecodes.LASTORE        : arrayStore(BasicType.Long  ); break;
                case Bytecodes.FASTORE        : arrayStore(BasicType.Float ); break;
                case Bytecodes.DASTORE        : arrayStore(BasicType.Double); break;
                case Bytecodes.AASTORE        : arrayStore(BasicType.Object); break;
                case Bytecodes.BASTORE        : arrayStore(BasicType.Byte  ); break;
                case Bytecodes.CASTORE        : arrayStore(BasicType.Char  ); break;
                case Bytecodes.SASTORE        : arrayStore(BasicType.Short ); break;
                case Bytecodes.POP            : // fall through
                case Bytecodes.POP2           : // fall through
                case Bytecodes.DUP            : // fall through
                case Bytecodes.DUP_X1         : // fall through
                case Bytecodes.DUP_X2         : // fall through
                case Bytecodes.DUP2           : // fall through
                case Bytecodes.DUP2_X1        : // fall through
                case Bytecodes.DUP2_X2        : // fall through
                case Bytecodes.SWAP           : stackOp(opcode); break;
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
                case Bytecodes.IXOR           : intOp2(opcode); break;
                case Bytecodes.INEG           : intNeg(opcode); break;
                case Bytecodes.LADD           : // fall through
                case Bytecodes.LSUB           : // fall through
                case Bytecodes.LMUL           : // fall through
                case Bytecodes.LDIV           : // fall through
                case Bytecodes.LREM           : // fall through
                case Bytecodes.LSHL           : // fall through
                case Bytecodes.LSHR           : // fall through
                case Bytecodes.LUSHR          : // fall through
                case Bytecodes.LAND           : // fall through
                case Bytecodes.LOR            : // fall through
                case Bytecodes.LXOR           : longOp2(opcode); break;
                case Bytecodes.LNEG           : longNeg(opcode); break;
                case Bytecodes.FADD           : // fall through
                case Bytecodes.FSUB           : // fall through
                case Bytecodes.FMUL           : // fall through
                case Bytecodes.FDIV           : // fall through
                case Bytecodes.FREM           : floatOp2(opcode); break;
                case Bytecodes.FNEG           : floatNeg(opcode); break;
                case Bytecodes.DADD           : // fall through
                case Bytecodes.DSUB           : // fall through
                case Bytecodes.DMUL           : // fall through
                case Bytecodes.DDIV           : // fall through
                case Bytecodes.DREM           : doubleNeg(opcode); break;
                case Bytecodes.DNEG           : doubleOp2(opcode); break;
                case Bytecodes.IINC           : increment(stream.readLocalIndex()); break;
                case Bytecodes.I2L            : convert(opcode, BasicType.Int   , BasicType.Long  ); break;
                case Bytecodes.I2F            : convert(opcode, BasicType.Int   , BasicType.Float ); break;
                case Bytecodes.I2D            : convert(opcode, BasicType.Int   , BasicType.Double); break;
                case Bytecodes.L2I            : convert(opcode, BasicType.Long  , BasicType.Int   ); break;
                case Bytecodes.L2F            : convert(opcode, BasicType.Long  , BasicType.Float ); break;
                case Bytecodes.L2D            : convert(opcode, BasicType.Long  , BasicType.Double); break;
                case Bytecodes.F2I            : convert(opcode, BasicType.Float , BasicType.Int   ); break;
                case Bytecodes.F2L            : convert(opcode, BasicType.Float , BasicType.Long  ); break;
                case Bytecodes.F2D            : convert(opcode, BasicType.Float , BasicType.Double); break;
                case Bytecodes.D2I            : convert(opcode, BasicType.Double, BasicType.Int   ); break;
                case Bytecodes.D2L            : convert(opcode, BasicType.Double, BasicType.Long  ); break;
                case Bytecodes.D2F            : convert(opcode, BasicType.Double, BasicType.Float ); break;
                case Bytecodes.I2B            : convert(opcode, BasicType.Int   , BasicType.Byte  ); break;
                case Bytecodes.I2C            : convert(opcode, BasicType.Int   , BasicType.Char  ); break;
                case Bytecodes.I2S            : convert(opcode, BasicType.Int   , BasicType.Short ); break;
                case Bytecodes.LCMP           : compareOp(BasicType.Long, opcode); break;
                case Bytecodes.FCMPL          : compareOp(BasicType.Float, opcode); break;
                case Bytecodes.FCMPG          : compareOp(BasicType.Float, opcode); break;
                case Bytecodes.DCMPL          : compareOp(BasicType.Double, opcode); break;
                case Bytecodes.DCMPG          : compareOp(BasicType.Double, opcode); break;
                case Bytecodes.IFEQ           : doIfZero(Condition.eql, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IFNE           : doIfZero(Condition.neq, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IFLT           : doIfZero(Condition.lss, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IFGE           : doIfZero(Condition.geq, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IFGT           : doIfZero(Condition.gtr, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IFLE           : doIfZero(Condition.leq, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IF_ICMPEQ      : doIfSame(BasicType.Int, Condition.eql, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IF_ICMPNE      : doIfSame(BasicType.Int, Condition.neq, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IF_ICMPLT      : doIfSame(BasicType.Int, Condition.lss, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IF_ICMPGE      : doIfSame(BasicType.Int, Condition.geq, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IF_ICMPGT      : doIfSame(BasicType.Int, Condition.gtr, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IF_ICMPLE      : doIfSame(BasicType.Int, Condition.leq, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IF_ACMPEQ      : doIfSame(BasicType.Object, Condition.eql, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IF_ACMPNE      : doIfSame(BasicType.Object, Condition.neq, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IFNULL         : doIfNull(Condition.eql, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.IFNONNULL      : doIfNull(Condition.neq, stream.nextBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.GOTO           : doGoto(stream.currentBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.JSR            : doJsr(stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.GOTO_W         : doGoto(stream.currentBCI(), stream.readFarBranchDest()); break bytecodeLoop;
                case Bytecodes.JSR_W          : doJsr(stream.readFarBranchDest()); break;
                case Bytecodes.RET            : doRet(stream.readLocalIndex());  break bytecodeLoop;
                case Bytecodes.TABLESWITCH    : doTableswitch(new BytecodeTableSwitch(bytecode, bci)); break bytecodeLoop;
                case Bytecodes.LOOKUPSWITCH   : doLookupswitch(new BytecodeLookupSwitch(bytecode, bci)); break bytecodeLoop;
                case Bytecodes.IRETURN        : // fall through
                case Bytecodes.FRETURN        : // fall through
                case Bytecodes.ARETURN        : doReturn(pop1()); break bytecodeLoop;
                case Bytecodes.LRETURN        : // fall through
                case Bytecodes.DRETURN        : doReturn(pop2()); break bytecodeLoop;
                case Bytecodes.RETURN         : doReturn(null  ); break bytecodeLoop;
                case Bytecodes.ATHROW         : doThrow(bci); break bytecodeLoop;
                case Bytecodes.GETSTATIC      : getStatic(constantPool().lookupGetStatic(stream.readCPI())); break;
                case Bytecodes.PUTSTATIC      : putStatic(constantPool().lookupPutStatic(stream.readCPI())); break;
                case Bytecodes.GETFIELD       : getField(constantPool().lookupGetField(stream.readCPI())); break;
                case Bytecodes.PUTFIELD       : putField(constantPool().lookupPutField(stream.readCPI())); break;
                case Bytecodes.INVOKEVIRTUAL  : invokeVirtual(constantPool().lookupInvokeVirtual(stream.readCPI())); break;
                case Bytecodes.INVOKESPECIAL  : invokeSpecial(constantPool().lookupInvokeSpecial(stream.readCPI())); break;
                case Bytecodes.INVOKESTATIC   : invokeStatic(constantPool().lookupInvokeStatic(stream.readCPI())); break;
                case Bytecodes.INVOKEINTERFACE: invokeInterface(constantPool().lookupInvokeInterface(stream.readCPI())); break;
                case Bytecodes.NEW            : newInstance(stream.readCPI()); break;
                case Bytecodes.NEWARRAY       : newTypeArray(stream.readLocalIndex()); break;
                case Bytecodes.ANEWARRAY      : newObjectArray(stream.readCPI()); break;
                case Bytecodes.ARRAYLENGTH    : arrayLength(); break;
                case Bytecodes.CHECKCAST      : checkCast(stream.readCPI()); break;
                case Bytecodes.INSTANCEOF     : instanceOf(stream.readCPI()); break;
                case Bytecodes.MONITORENTER   : monitorEnter(bci); break;
                case Bytecodes.MONITOREXIT    : monitorExit(bci); break;
                case Bytecodes.MULTIANEWARRAY : newMultiArray(stream.readCPI(), stream.readUByte(bci + 3)); break;
                case Bytecodes.BREAKPOINT     : breakpoint(bci); break;
                default                       : doUnknownBytecode(bci, opcode); break;
            }
            // Checkstyle: resume

            bci = stream.nextBCI();
            stream.next();
            if (isBlockStart(bci)) {
                // fell through to the next block
                enqueue(bci, currentState);
                break;
            }
        }

        blockState.generated = true;
    }

    private void doUnknownBytecode(int bci, int opcode) {
        if (extension != null) {
            CiBytecodeExtension.Bytecode extcode = extension.getBytecode(opcode, bci, bytecode);
            if (extcode != null) {
                doExtendedBytecode(extcode);
                return;
            }
        }
        throw Util.shouldNotReachHere();
    }

    private void doExtendedBytecode(CiBytecodeExtension.Bytecode extcode) {
        Location[] args = popN(extcode.signatureType().argumentCount(false));
        BasicType retType = extcode.signatureType().returnBasicType();
        if (retType != BasicType.Void) {
            pushX(produce(retType), retType);
        }
        emitString("extended " + extcode);
    }

    void breakpoint(int bci) {
        emitString("breakpoint @" + bci);
    }

    void emitInstrumentation(int bci) {
        emitString("block_counter @" + bci);
    }

    void emitExceptionLoad(int bci) {
        currentState.stackIndex = maxLocals; // clear the Java operand stack
        Location r = produce(BasicType.Object);
        emitString(r + " = exception_load()");
        push1(r);
    }

    void emitSafepoint(int bci) {
        emitString("safepoint @" + bci);
    }

    void newMultiArray(char cpi, int rank) {
        CiType type = constantPool().lookupType(cpi);
        Location[] lengths = popN(rank);
        Location r = produce(BasicType.Object);
        emitOp(r, "multianewarray:" + type + rank, lengths);
        push1(r);
    }

    void monitorExit(int bci) {
        Location object = pop1();
        emitOp(null, "monitorexit", object);
    }

    void monitorEnter(int bci) {
        Location object = pop1();
        emitOp(null, "monitorenter", object);
    }

    void instanceOf(char cpi) {
        CiType type = constantPool().lookupType(cpi);
        Location object = pop1();
        Location r = produce(BasicType.Boolean);
        emitOp(r, "instanceof:" + type, object);
        push1(r);
    }

    void checkCast(char cpi) {
        CiType type = constantPool().lookupType(cpi);
        Location object = pop1();
        emitOp(object, "checkcast:" + type, object);
        push1(object);
    }

    void arrayLength() {
        Location object = pop1();
        Location r = produce(BasicType.Int);
        emitOp(r, "arraylength", object);
        push1(r);
    }

    void newObjectArray(char cpi) {
        CiType type = constantPool().lookupType(cpi);
        Location length = pop1();
        Location r = produce(BasicType.Object);
        emitOp(r, "anewarray:" + type, length);
        push1(r);
    }

    void newTypeArray(int typeCode) {
        BasicType elemType = BasicType.fromArrayTypeCode(typeCode);
        Location length = pop1();
        Location r = produce(BasicType.Object);
        emitOp(r, "newarray:" + elemType, length);
        push1(r);
    }

    void newInstance(char cpi) {
        CiType type = constantPool().lookupType(cpi);
        Location r = produce(BasicType.Object);
        emitOp(r, "new:" + type);
        push1(r);
    }

    void invokeInterface(CiMethod ciMethod) {
        Location[] args = popN(ciMethod.signatureType().argumentCount(true));
        BasicType retType = ciMethod.signatureType().returnBasicType();
        Location r = pushZ(retType);
        emitOp(r, "invokeinterface:" + ciMethod, args);
    }

    void invokeStatic(CiMethod ciMethod) {
        Location[] args = popN(ciMethod.signatureType().argumentCount(false));
        BasicType retType = ciMethod.signatureType().returnBasicType();
        Location r = pushZ(retType);
        emitOp(r, "invokestatic:" + ciMethod, args);
    }

    void invokeSpecial(CiMethod ciMethod) {
        Location[] args = popN(ciMethod.signatureType().argumentCount(true));
        BasicType retType = ciMethod.signatureType().returnBasicType();
        Location r = pushZ(retType);
        emitOp(r, "invokespecial:" + ciMethod, args);
    }

    void invokeVirtual(CiMethod ciMethod) {
        Location[] args = popN(ciMethod.signatureType().argumentCount(true));
        BasicType retType = ciMethod.signatureType().returnBasicType();
        Location r = pushZ(retType);
        emitOp(r, "invokevirtual:" + ciMethod, args);
    }

    void putField(CiField ciField) {
        Location object = pop1();
        Location value = popX(ciField.basicType());
        emitOp(null, "putfield:" + ciField, object, value);
    }

    void getField(CiField ciField) {
        Location object = pop1();
        Location r = produce(ciField.basicType());
        pushX(r, ciField.basicType());
        emitOp(r, "getfield:" + ciField, object);
    }

    void putStatic(CiField ciField) {
        Location value = popX(ciField.basicType());
        emitOp(null, "putstatic:" + ciField, value);
    }

    void getStatic(CiField ciField) {
        Location r = produce(ciField.basicType());
        emitOp(r, "getstatic:" + ciField);
        pushX(r, ciField.basicType());
    }

    void doThrow(int i) {
        Location thrown = pop1();
        emitString("throw " + thrown);
    }

    void doReturn(Location value) {
        emitString("return " + (value == null ? "" : value.toString()));
    }

    void doTableswitch(BytecodeTableSwitch bytecodeTableSwitch) {
        Location key = pop1();
        emitString("tableswitch " + key);
    }

    void doLookupswitch(BytecodeLookupSwitch bytecodeLookupSwitch) {
        Location key = pop1();
        emitString("lookupswitch " + key);
    }

    void doRet(int index) {
        Location r = currentState.state[index];
        emitString("ret " + r);
    }

    void doJsr(int targetBCI) {
        push1(emitCurrentPC()); // TODO: current BC is not quite correct (need PC after this block)
        emitString("jsr -> " + targetBCI);
        enqueue(targetBCI, currentState);
    }

    void doGoto(int bci, int targetBCI) {
        emitString("goto -> " + targetBCI);
        enqueue(targetBCI, currentState);
    }

    void doIfNull(Condition cond, int nextBCI, int targetBCI) {
        Location obj = pop1();
        emitString("if(" + obj + " " + cond + " null) -> " + targetBCI + " else -> " + nextBCI);
        FrameState nstate = enqueue(nextBCI, currentState);
        FrameState tstate = enqueue(targetBCI, currentState.copy());
    }

    void doIfSame(BasicType basicType, Condition condition, int nextBCI, int targetBCI) {
        Location y = pop1();
        Location x = pop1();
        emitString("if:" + basicType + "(" + x + "  " + condition + "  " + y + ") -> " + targetBCI + " else -> " + nextBCI);
        FrameState nstate = enqueue(nextBCI, currentState);
        FrameState tstate = enqueue(targetBCI, currentState.copy());
    }

    void doIfZero(Condition condition, int nextBCI, int targetBCI) {
        Location r = pop1();
        emitString("if(" + r + " " + condition.operator + " 0) -> " + targetBCI + " else -> " + nextBCI);
        FrameState nstate = enqueue(nextBCI, currentState);
        FrameState tstate = enqueue(targetBCI, currentState.copy());
    }

    void increment(int index) {
        Location l = currentState.state[index];
        Location r = produce(BasicType.Int);
        emitOp(r, "inc", l);
    }

    void compareOp(BasicType basicType, int opcode) {
        Location y = pop1();
        Location x = pop1();
        Location r = produce(BasicType.Int);
        emitOp(r, "compare:" + basicType, x, y);
        push1(r);
    }

    void convert(int opcode, BasicType from, BasicType to) {
        Location value = popX(from);
        Location r = produce(to);
        emitOp(r, "convert:" + from + ":" + to, value);
        pushX(r, to);
    }

    void arrayLoad(BasicType basicType) {
        Location index = pop1();
        Location array = pop1();
        Location r = produce(basicType);
        emitOp(r, "array_load:" + basicType, array, index);
        push1(r);
    }

    void arrayStore(BasicType basicType) {
        Location value = popX(basicType);
        Location index = pop1();
        Location array = pop1();
        emitOp(null, "array_store:" + basicType, array, index, value);
    }

    void intOp2(int opcode) {
        Location y = pop1();
        Location x = pop1();
        Location r = produce(BasicType.Int);
        emitOp(r, "int." + Bytecodes.operator(opcode), x, y);
        push1(r);
    }

    void longOp2(int opcode) {
        Location y = pop2();
        Location x = pop2();
        Location r = produce(BasicType.Long);
        emitOp(r, "long." + Bytecodes.operator(opcode), x, y);
        push2(r);
    }

    void floatOp2(int opcode) {
        Location y = pop1();
        Location x = pop1();
        Location r = produce(BasicType.Float);
        emitOp(r, "float." + Bytecodes.operator(opcode), x, y);
        push1(r);
    }

    void doubleOp2(int opcode) {
        Location y = pop2();
        Location x = pop2();
        Location r = produce(BasicType.Double);
        emitOp(r, "double." + Bytecodes.operator(opcode), x, y);
        push2(r);
    }

    void intNeg(int opcode) {
        Location x = pop1();
        Location r = produce(BasicType.Int);
        emitOp(r, "int.neg", x);
        push1(r);
    }

    void longNeg(int opcode) {
        Location x = pop2();
        Location r = produce(BasicType.Long);
        emitOp(r, "long.neg", x);
        push2(r);
    }

    void floatNeg(int opcode) {
        Location x = pop1();
        Location r = produce(BasicType.Float);
        emitOp(r, "float.neg", x);
        push1(r);
    }

    void doubleNeg(int opcode) {
        Location x = pop2();
        Location r = produce(BasicType.Double);
        emitOp(r, "double.neg", x);
        push2(r);
    }

    void loadConstant(int bci) {
        Object con = constantPool().lookupConstant((char) bci);

        if (con instanceof CiType) {
            // this is a load of class constant which might be unresolved
            CiType citype = (CiType) con;
            if (!citype.isLoaded()) {
                Util.nonFatalUnimplemented("load of unresolved class");
            } else {
                push1(emitObject(citype.javaClass()));
            }
            return;
        } else if (con instanceof CiConstant) {
            CiConstant constant = (CiConstant) con;
            switch (constant.basicType.stackType()) {
                case Int:    push1(emitInt(constant.asInt())); return;
                case Long:   push2(emitLong(constant.asLong())); return;
                case Float:  push1(emitFloat(constant.asFloat())); return;
                case Double: push2(emitDouble(constant.asDouble())); return;
                case Object: push1(emitObject(constant.asObject())); return;
            }
        }
        throw new Error("lookupConstant returned an object of incorrect type");
    }

    void stackOp(int opcode) {
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

    void pushX(Location val, BasicType basicType) {
        if (basicType.isDoubleWord()) {
            push2(val);
        } else {
            push1(val);
        }
    }

    Location pushZ(BasicType retType) {
        Location r = null;
        if (retType != BasicType.Void) {
            pushX(r = produce(retType), retType);
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
        return result;
    }

    Location popX(BasicType basicType) {
        return basicType.isDoubleWord() ? pop2() : pop1();
    }

    void push2(Location val) {
        currentState.state[currentState.stackIndex++] = val;
        currentState.state[currentState.stackIndex++] = null;
    }

    Location pop2() {
        --currentState.stackIndex;
        return currentState.state[--currentState.stackIndex];
    }

    boolean isBlockStart(int bci) {
        return blockMap[bci] != 0;
    }

    boolean isBackwardBranchTarget(int bci) {
        return (blockMap[bci] & BLOCK_BACKWARD_TARGET) != 0;
    }

    boolean isExceptionEntry(int bci) {
        return (blockMap[bci] & BLOCK_EXCEPTION_ENTRY) != 0;
    }

    Location produce(BasicType basicType) {
        return new Register(regNum++, basicType);
    }

    CiConstantPool constantPool() {
        if (constantPool == null) {
            constantPool = runtime.getConstantPool(method);
        }
        return constantPool;
    }

    void handleException(int bci) {
        FrameState state = null;
        for (CiExceptionHandler h : handlers) {
            // XXX: could be sped up if handlers are sorted by startBCI
            if (h.startBCI() <= bci && bci < h.endBCI()) {
                if (state == null) {
                    state = currentState.copy();
                    deferExceptionAdapter(state, bci, h);
                }
            }
        }
    }

    void deferExceptionAdapter(FrameState state, int bci, CiExceptionHandler h) {
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
        Location spillLocation = new StackSlot(index);
        emitMove(state.state[index], spillLocation);
        if (kill) {
            // if we are killing values, then the new current location is on the stack
            state.memory[index] = state.state[index] = spillLocation;
        }
    }

    void emitMove(Location from, Location to) {
        emitString(to + " = " + from);
    }

    void emitOp(Location r, String op, Location... locs) {
        StringBuilder b = new StringBuilder();
        if (r != null) {
            b.append(r).append(" = ");
        }
        b.append(op).append("(");
        for (int i = 0; i < locs.length; i++) {
            if (i > 0) {
                b.append(", ");
                b.append(locs[i]);
            }
        }
        b.append(")");
        emitString(b.toString());
    }

    Location emitCurrentPC() {
        Location r = produce(BasicType.Word);
        emitOp(r, "block_end_pc");
        return r;
    }

    Location emitInt(int val) {
        Location r = produce(BasicType.Int);
        emitString(r + " = int: " + val);
        return r;
    }

    Location emitLong(long val) {
        Location r = produce(BasicType.Long);
        emitString(r + " = long: " + val);
        return r;
    }

    Location emitFloat(float val) {
        Location r = produce(BasicType.Float);
        emitString(r + " = float: " + val);
        return r;
    }

    Location emitDouble(double val) {
        Location r = produce(BasicType.Double);
        emitString(r + " = double: " + val);
        return r;
    }

    Location emitObject(Object val) {
        Location r = produce(BasicType.Object);
        emitString(r + " = object: " + val);
        return r;
    }

    void unimplemented(String str, Object... params) {
        Util.nonFatalUnimplemented(params);
    }

    void unimplemented(String str, int param) {
        Util.nonFatalUnimplemented(param);
    }

    void emitString(String str) {
        out.println("    " + str);
    }
}
