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
import com.sun.c1x.bytecode.Bytecodes;
import com.sun.c1x.bytecode.BytecodeStream;
import com.sun.c1x.value.BasicType;
import com.sun.c1x.ir.*;
import com.sun.c1x.util.Util;

import java.util.List;

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

    class Location {

    }

    class Register extends Location {

    }

    class StackSlot extends Location {

        final int index;

        StackSlot(int index) {
            this.index = index;
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
        final Location[] state;  // stores where the most recent values are
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
    final int maxLocals;
    final int maxStack;
    final boolean hasHandler;
    final List<CiExceptionHandler> handlers;
    FrameState currentState;

    public C0XCompiler(CiRuntime runtime, CiMethod method, CiBytecodeExtension extension) {
        this.runtime = runtime;
        this.method = method;
        this.extension = extension;
        this.bytecode = method.code();
        this.handlers = method.exceptionHandlers();
        this.maxLocals = method.maxLocals();
        this.maxStack = method.maxStackSize();
        this.hasHandler = method.isSynchronized() || handlers != null && handlers.size() > 0;
    }

    /**
     * Compile a basic block from beginning to end.
     * @param bci the bytecode index of the start of the basic block
     * @param stream the bytecode stream
     * @param state the frame state at the beginning of the basic block
     */
    void compileBlock(int bci, BytecodeStream stream, Location[] state) {
        stream.setBCI(bci);

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

            // check whether the bytecode can cause an exception
            if (Bytecodes.canTrap(opcode) && hasHandler) {
                handleException(bci);
            }

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
                case Bytecodes.IINC           : increment(); break;
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
                case Bytecodes.IFEQ           : doIfZero(Condition.eql); break bytecodeLoop;
                case Bytecodes.IFNE           : doIfZero(Condition.neq); break bytecodeLoop;
                case Bytecodes.IFLT           : doIfZero(Condition.lss); break bytecodeLoop;
                case Bytecodes.IFGE           : doIfZero(Condition.geq); break bytecodeLoop;
                case Bytecodes.IFGT           : doIfZero(Condition.gtr); break bytecodeLoop;
                case Bytecodes.IFLE           : doIfZero(Condition.leq); break bytecodeLoop;
                case Bytecodes.IF_ICMPEQ      : doIfSame(BasicType.Int, Condition.eql); break bytecodeLoop;
                case Bytecodes.IF_ICMPNE      : doIfSame(BasicType.Int, Condition.neq); break bytecodeLoop;
                case Bytecodes.IF_ICMPLT      : doIfSame(BasicType.Int, Condition.lss); break bytecodeLoop;
                case Bytecodes.IF_ICMPGE      : doIfSame(BasicType.Int, Condition.geq); break bytecodeLoop;
                case Bytecodes.IF_ICMPGT      : doIfSame(BasicType.Int, Condition.gtr); break bytecodeLoop;
                case Bytecodes.IF_ICMPLE      : doIfSame(BasicType.Int, Condition.leq); break bytecodeLoop;
                case Bytecodes.IF_ACMPEQ      : doIfSame(BasicType.Object, Condition.eql); break bytecodeLoop;
                case Bytecodes.IF_ACMPNE      : doIfSame(BasicType.Object, Condition.neq); break bytecodeLoop;
                case Bytecodes.IFNULL         : doIfNull(Condition.eql); break bytecodeLoop;
                case Bytecodes.IFNONNULL      : doIfNull(Condition.neq); break bytecodeLoop;
                case Bytecodes.GOTO           : doGoto(stream.currentBCI(), stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.JSR            : doJsr(stream.readBranchDest()); break bytecodeLoop;
                case Bytecodes.GOTO_W         : doGoto(stream.currentBCI(), stream.readFarBranchDest()); break bytecodeLoop;
                case Bytecodes.JSR_W          : doJsr(stream.readFarBranchDest()); break;
                case Bytecodes.RET            : doRet(stream.readLocalIndex());  break bytecodeLoop;
                case Bytecodes.TABLESWITCH    : doTableswitch(); break bytecodeLoop;
                case Bytecodes.LOOKUPSWITCH   : doLookupswitch(); break bytecodeLoop;
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
                case Bytecodes.INVOKESPECIAL  : invokeSpecial(constantPool().lookupInvokeSpecial(stream.readCPI()), null); break;
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

            bci = stream.nextBCI();
            stream.next();
            if (isBlockStart(bci)) {
                // fell through to the next block
                break;
            }
        }
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
        pushZ(extcode.signatureType().returnBasicType());
        Util.nonFatalUnimplemented(args);
    }

    void breakpoint(int bci) {
        Util.nonFatalUnimplemented("emit breakpoint instruction @ " + bci);
    }

    void emitInstrumentation(int bci) {
        Util.nonFatalUnimplemented("emit instrumentation for block @ " + bci);
    }

    void emitExceptionLoad(int bci) {
        currentState.stackIndex = maxLocals; // clear the Java operand stack
        push1(produce(BasicType.Object));
        Util.nonFatalUnimplemented("emit load of exception object @ " + bci);
    }

    void emitSafepoint(int bci) {
        Util.nonFatalUnimplemented("emit safepoint code @ " + bci);
    }

    void newMultiArray(char cpi, int rank) {
        push1(produce(BasicType.Object));
        Util.nonFatalUnimplemented("emit multianewarray");
    }

    void monitorExit(int bci) {
        Location object = pop1();
        Util.nonFatalUnimplemented("emit monitor exit code @ " + bci);
    }

    void monitorEnter(int bci) {
        Location object = pop1();
        Util.nonFatalUnimplemented("emit monitor enter code @ " + bci);
    }

    void instanceOf(char cpi) {
        CiType type = constantPool().lookupType(cpi);
        Location object = pop1();
        push1(produce(BasicType.Boolean));
        Util.nonFatalUnimplemented("emit instanceof code");
    }

    void checkCast(char cpi) {
        CiType type = constantPool().lookupType(cpi);
        Location object = pop1();
        push1(object);
        Util.nonFatalUnimplemented("emit checkcast code");
    }

    void arrayLength() {
        Location object = pop1();
        push1(object);
        Util.nonFatalUnimplemented("emit array length code");
    }

    void newObjectArray(char cpi) {
        CiType type = constantPool().lookupType(cpi);
        Location length = pop1();
        push1(produce(BasicType.Object));
        Util.nonFatalUnimplemented("emit new object array");
    }

    void newTypeArray(int typeCode) {
        BasicType elemType = BasicType.fromArrayTypeCode(typeCode);
        pop1();
        push1(produce(BasicType.Object));
        Util.nonFatalUnimplemented("emit new type array");
    }

    void newInstance(char cpi) {
        CiType type = constantPool().lookupType(cpi);
        push1(produce(BasicType.Object));
        Util.nonFatalUnimplemented("emit new instance");
    }

    void invokeInterface(CiMethod ciMethod) {
        Location[] args = popN(ciMethod.signatureType().argumentCount(true));
        pushZ(ciMethod.signatureType().returnBasicType());
        Util.nonFatalUnimplemented("emit invoke interface");
    }

    void invokeStatic(CiMethod ciMethod) {
        Location[] args = popN(ciMethod.signatureType().argumentCount(false));
        pushZ(ciMethod.signatureType().returnBasicType());
        Util.nonFatalUnimplemented("emit invoke static");
    }

    void invokeSpecial(CiMethod ciMethod, Object o) {
        Location[] args = popN(ciMethod.signatureType().argumentCount(true));
        pushZ(ciMethod.signatureType().returnBasicType());
        Util.nonFatalUnimplemented("emit invoke special");
    }

    void invokeVirtual(CiMethod ciMethod) {
        Location[] args = popN(ciMethod.signatureType().argumentCount(true));
        pushZ(ciMethod.signatureType().returnBasicType());
        Util.nonFatalUnimplemented("emit invoke virtual");
    }

    void putField(CiField ciField) {
        Location object = pop1();
        Location value = popX(ciField.basicType());
        Util.nonFatalUnimplemented("emit put field");
    }

    void getField(CiField ciField) {
        Location object = pop1();
        pushX(produce(ciField.basicType()), ciField.basicType());
        Util.nonFatalUnimplemented("emit get field");
    }

    void putStatic(CiField ciField) {
        Location value = popX(ciField.basicType());
        Util.nonFatalUnimplemented("emit put static");
    }

    void getStatic(CiField ciField) {
        pushX(produce(ciField.basicType()), ciField.basicType());
        Util.nonFatalUnimplemented("emit get static");
    }

    void doThrow(int i) {
        Location thrown = pop1();
        Util.nonFatalUnimplemented("emit throw");
    }

    void doIfNull(Condition cond) {
        Location obj = pop1();
        Util.nonFatalUnimplemented("emit if null");
    }

    void doReturn(Location value) {
        Util.nonFatalUnimplemented("emit return");
    }

    void doTableswitch() {
        Location key = pop1();
        Util.nonFatalUnimplemented("emit table switch");
    }

    void doLookupswitch() {
        Location key = pop1();
        Util.nonFatalUnimplemented("emit lookup switch");
    }

    void doRet(int targetBCI) {
        Util.nonFatalUnimplemented("emit ret -> " + targetBCI);
    }

    void doJsr(int targetBCI) {
        Util.nonFatalUnimplemented("emit jsr -> " + targetBCI);
    }

    void doGoto(int bci, int targetBCI) {
        Util.nonFatalUnimplemented("emit goto @ " + bci + " -> " + targetBCI);
    }

    void doIfSame(BasicType basicType, Condition condition) {
        Location r = pop1();
        Location l = pop1();
        Util.nonFatalUnimplemented("emit if same");
    }

    void doIfZero(Condition condition) {
        Location r = pop1();
        Util.nonFatalUnimplemented("emit if zero");
    }

    void increment() {
        Util.nonFatalUnimplemented("emit increment");
    }

    void compareOp(BasicType basicType, int opcode) {
        Location r = pop1();
        Location l = pop1();
        push1(produce(BasicType.Int));
        Util.nonFatalUnimplemented("emit compare op");
    }

    void convert(int opcode, BasicType from, BasicType to) {
        Location value = popX(from);
        pushX(produce(to), to);
        Util.nonFatalUnimplemented("emit convert");
    }

    void arrayLoad(BasicType basicType) {
        Location index = pop1();
        Location array = pop1();
        push1(produce(basicType));
        Util.nonFatalUnimplemented("emit array load");
    }

    void arrayStore(BasicType basicType) {
        Location value = popX(basicType);
        Location index = pop1();
        Location array = pop1();
        Util.nonFatalUnimplemented("emit array store");
    }

    void intOp2(int opcode) {
        Location r = pop1();
        Location l = pop1();
        push1(produce(BasicType.Float));
        Util.nonFatalUnimplemented("emit integer operation");
    }

    void longOp2(int opcode) {
        Location r = pop2();
        Location l = pop2();
        push2(produce(BasicType.Long));
        Util.nonFatalUnimplemented("emit long operation");
    }

    void floatOp2(int opcode) {
        Location r = pop1();
        Location l = pop1();
        push1(produce(BasicType.Float));
        Util.nonFatalUnimplemented("emit float operation");
    }

    void doubleOp2(int opcode) {
        Location r = pop2();
        Location l = pop2();
        push2(produce(BasicType.Double));
        Util.nonFatalUnimplemented("emit double operation");
    }

    void intNeg(int opcode) {
        Location r = pop1();
        push1(produce(BasicType.Int));
        Util.nonFatalUnimplemented("integer negation");
    }

    void longNeg(int opcode) {
        Location r = pop2();
        push2(produce(BasicType.Long));
        Util.nonFatalUnimplemented("long negation");
    }

    void floatNeg(int opcode) {
        Location r = pop1();
        push1(produce(BasicType.Float));
        Util.nonFatalUnimplemented("float negation");
    }

    void doubleNeg(int opcode) {
        Location r = pop2();
        push2(produce(BasicType.Double));
        Util.nonFatalUnimplemented("double negation");
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

    void pushZ(BasicType retType) {
        if (retType != BasicType.Void) {
            pushX(produce(retType), retType);
        }
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
        return Util.nonFatalUnimplemented(false);
    }

    boolean isBackwardBranchTarget(int bci) {
        return Util.nonFatalUnimplemented(false);
    }

    boolean isExceptionEntry(int bci) {
        return Util.nonFatalUnimplemented(false);
    }

    Location produce(BasicType basicType) {
        return Util.nonFatalUnimplemented(null);
    }

    void release(Location value) {
        Util.nonFatalUnimplemented("release a location");
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
        Util.nonFatalUnimplemented("emit move");
    }

    Location emitInt(int val) {
        Util.nonFatalUnimplemented("emit code to load the int");
        return produce(BasicType.Int);
    }

    Location emitLong(long val) {
        Util.nonFatalUnimplemented("emit code to load the long");
        return produce(BasicType.Long);
    }

    Location emitFloat(float val) {
        Util.nonFatalUnimplemented("emit code to load the float");
        return produce(BasicType.Float);
    }

    Location emitDouble(double val) {
        Util.nonFatalUnimplemented("emit code to load the double");
        return produce(BasicType.Double);
    }

    Location emitObject(Object val) {
        Util.nonFatalUnimplemented("emit code to load the object");
        return produce(BasicType.Object);
    }
}
