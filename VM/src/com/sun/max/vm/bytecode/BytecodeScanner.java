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
package com.sun.max.vm.bytecode;

import static com.sun.max.vm.classfile.ErrorContext.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;

/**
 * Visitor dispatch over byte codes.
 *
 * @author Bernd Mathiske
 */
public final class BytecodeScanner {

    final BytecodeVisitor _bytecodeVisitor;

    public BytecodeScanner(BytecodeVisitor bytecodeVisitor) {
        _bytecodeVisitor = bytecodeVisitor;
        bytecodeVisitor.setBytecodeScanner(this);
    }

    protected BytecodeBlock _bytecodeBlock;

    public BytecodeBlock bytecodeBlock() {
        return _bytecodeBlock;
    }

    private int _currentBytePosition;

    public int currentBytePosition() {
        return _currentBytePosition;
    }

    protected boolean _stopped;

    /**
     * Stop the scanning.
     */
    public void stop() {
        _stopped = true;
    }

    public boolean wasStopped() {
        return _stopped;
    }

    protected Bytecode _currentOpcode;

    /**
     * Gets the most recently scanned opcode. Note that if this is called while in
     * {@link BytecodeVisitor#opcodeDecoded()} and the current instruction is widened then the {@link Bytecode#WIDE}
     * opcode will be returned. If it's called while in the visitor method for the widened instruction then the widened
     * opcode is returned and {@link #isCurrentOpcodeWidened()} will return true. If it's called while in
     * {@link BytecodeVisitor#instructionDecoded()} then the opcode is returned and {@link #isCurrentOpcodeWidened()}
     * will return false.
     */
    public Bytecode currentOpcode() {
        return _currentOpcode;
    }

    protected int _currentOpcodePosition;

    @INLINE
    public int currentOpcodePosition() {
        return _currentOpcodePosition;
    }

    protected boolean _currentOpcodeWidened;

    /**
     * @see #currentOpcode()
     */
    public boolean isCurrentOpcodeWidened() {
        return _currentOpcodeWidened;
    }

    /**
     * Gets a decription of the current scan location in a string that resembles a line in a standard stack trace.
     *
     * @param classMethodActor the context of the bytecode being scanned
     */
    public String getCurrentLocationAsString(ClassMethodActor classMethodActor) {
        final int lineNumber = classMethodActor.codeAttribute().lineNumberTable().findLineNumber(_currentOpcodePosition);
        final StringBuilder buf = new StringBuilder();
        if (lineNumber != -1) {
            final ClassActor holder = classMethodActor.holder();
            String sourceFileName = holder.sourceFileName();
            if (sourceFileName == null) {
                sourceFileName = "<unknown>";
            }
            buf.append(classMethodActor.format("%H.%n(" + sourceFileName + ":" + lineNumber + ")"));
        } else {
            buf.append(classMethodActor.format("%H.%n(%p)"));
        }
        return buf.append(" [bytecode index=" + _currentOpcodePosition + ", opcode=" + _currentOpcode + "]").toString();
    }

    private byte readByte() {
        return _bytecodeBlock.code()[_currentBytePosition++];
    }

    private int readUnsigned1() {
        return readByte() & 0xff;
    }

    private int readSigned1() {
        return readByte();
    }

    private int readUnsigned2() {
        final int high = readByte() & 0xff;
        final int low = readByte() & 0xff;
        return (high << 8) | low;
    }

    private int readSigned2() {
        final int high = readByte();
        final int low = readByte() & 0xff;
        return (high << 8) | low;
    }

    private int readSigned4() {
        final int b3 = readByte() << 24;
        final int b2 = (readByte() & 0xff) << 16;
        final int b1 = (readByte() & 0xff) << 8;
        final int b0 = readByte() & 0xff;
        return b3 | b2 | b1 | b0;
    }

    public int readSwitchCase() {
        return readSigned4();
    }

    public int readSwitchOffset() {
        return readSigned4();
    }

    private void alignAddress() {
        final int remainder = _currentBytePosition % 4;
        if (remainder != 0) {
            _currentBytePosition += 4 - remainder;
        }
    }

    private void wide() {
        _currentOpcodeWidened = true;
        _currentOpcode = Bytecode.from(readUnsigned1());
        final int index = readUnsigned2();
        switch (_currentOpcode) {
            case ILOAD: {
                _bytecodeVisitor.iload(index);
                break;
            }
            case LLOAD: {
                _bytecodeVisitor.lload(index);
                break;
            }
            case FLOAD: {
                _bytecodeVisitor.fload(index);
                break;
            }
            case DLOAD: {
                _bytecodeVisitor.dload(index);
                break;
            }
            case ALOAD: {
                _bytecodeVisitor.aload(index);
                break;
            }
            case ISTORE: {
                _bytecodeVisitor.istore(index);
                break;
            }
            case LSTORE: {
                _bytecodeVisitor.lstore(index);
                break;
            }
            case FSTORE: {
                _bytecodeVisitor.fstore(index);
                break;
            }
            case DSTORE: {
                _bytecodeVisitor.dstore(index);
                break;
            }
            case ASTORE: {
                _bytecodeVisitor.astore(index);
                break;
            }
            case IINC: {
                final int addend = readSigned2();
                _bytecodeVisitor.iinc(index, addend);
                break;
            }
            case RET: {
                _bytecodeVisitor.ret(index);
                break;
            }
            default: {
                throw verifyError("Invalid application of WIDE prefix to " + _currentOpcode);
            }
        }
        _currentOpcodeWidened = false;
    }

    protected void scanInstruction() {
        _currentOpcode = Bytecode.from(readUnsigned1());
        _bytecodeVisitor.opcodeDecoded();
        if (_stopped) {
            return;
        }
        switch (_currentOpcode) {
            case NOP: {
                _bytecodeVisitor.nop();
                break;
            }
            case ACONST_NULL: {
                _bytecodeVisitor.aconst_null();
                break;
            }
            case ICONST_M1: {
                _bytecodeVisitor.iconst_m1();
                break;
            }
            case ICONST_0: {
                _bytecodeVisitor.iconst_0();
                break;
            }
            case ICONST_1: {
                _bytecodeVisitor.iconst_1();
                break;
            }
            case ICONST_2: {
                _bytecodeVisitor.iconst_2();
                break;
            }
            case ICONST_3: {
                _bytecodeVisitor.iconst_3();
                break;
            }
            case ICONST_4: {
                _bytecodeVisitor.iconst_4();
                break;
            }
            case ICONST_5: {
                _bytecodeVisitor.iconst_5();
                break;
            }
            case LCONST_0: {
                _bytecodeVisitor.lconst_0();
                break;
            }
            case LCONST_1: {
                _bytecodeVisitor.lconst_1();
                break;
            }
            case FCONST_0: {
                _bytecodeVisitor.fconst_0();
                break;
            }
            case FCONST_1: {
                _bytecodeVisitor.fconst_1();
                break;
            }
            case FCONST_2: {
                _bytecodeVisitor.fconst_2();
                break;
            }
            case DCONST_0: {
                _bytecodeVisitor.dconst_0();
                break;
            }
            case DCONST_1: {
                _bytecodeVisitor.dconst_1();
                break;
            }
            case BIPUSH: {
                final int operand = readSigned1();
                _bytecodeVisitor.bipush(operand);
                break;
            }
            case SIPUSH: {
                final int operand = readSigned2();
                _bytecodeVisitor.sipush(operand);
                break;
            }
            case LDC: {
                final int index = readUnsigned1();
                _bytecodeVisitor.ldc(index);
                break;
            }
            case LDC_W: {
                final int index = readUnsigned2();
                _bytecodeVisitor.ldc_w(index);
                break;
            }
            case LDC2_W: {
                final int index = readUnsigned2();
                _bytecodeVisitor.ldc2_w(index);
                break;
            }
            case ILOAD: {
                final int index = readUnsigned1();
                _bytecodeVisitor.iload(index);
                break;
            }
            case LLOAD: {
                final int index = readUnsigned1();
                _bytecodeVisitor.lload(index);
                break;
            }
            case FLOAD: {
                final int index = readUnsigned1();
                _bytecodeVisitor.fload(index);
                break;
            }
            case DLOAD: {
                final int index = readUnsigned1();
                _bytecodeVisitor.dload(index);
                break;
            }
            case ALOAD: {
                final int index = readUnsigned1();
                _bytecodeVisitor.aload(index);
                break;
            }
            case ILOAD_0: {
                _bytecodeVisitor.iload_0();
                break;
            }
            case ILOAD_1: {
                _bytecodeVisitor.iload_1();
                break;
            }
            case ILOAD_2: {
                _bytecodeVisitor.iload_2();
                break;
            }
            case ILOAD_3: {
                _bytecodeVisitor.iload_3();
                break;
            }
            case LLOAD_0: {
                _bytecodeVisitor.lload_0();
                break;
            }
            case LLOAD_1: {
                _bytecodeVisitor.lload_1();
                break;
            }
            case LLOAD_2: {
                _bytecodeVisitor.lload_2();
                break;
            }
            case LLOAD_3: {
                _bytecodeVisitor.lload_3();
                break;
            }
            case FLOAD_0: {
                _bytecodeVisitor.fload_0();
                break;
            }
            case FLOAD_1: {
                _bytecodeVisitor.fload_1();
                break;
            }
            case FLOAD_2: {
                _bytecodeVisitor.fload_2();
                break;
            }
            case FLOAD_3: {
                _bytecodeVisitor.fload_3();
                break;
            }
            case DLOAD_0: {
                _bytecodeVisitor.dload_0();
                break;
            }
            case DLOAD_1: {
                _bytecodeVisitor.dload_1();
                break;
            }
            case DLOAD_2: {
                _bytecodeVisitor.dload_2();
                break;
            }
            case DLOAD_3: {
                _bytecodeVisitor.dload_3();
                break;
            }
            case ALOAD_0: {
                _bytecodeVisitor.aload_0();
                break;
            }
            case ALOAD_1: {
                _bytecodeVisitor.aload_1();
                break;
            }
            case ALOAD_2: {
                _bytecodeVisitor.aload_2();
                break;
            }
            case ALOAD_3: {
                _bytecodeVisitor.aload_3();
                break;
            }
            case IALOAD: {
                _bytecodeVisitor.iaload();
                break;
            }
            case LALOAD: {
                _bytecodeVisitor.laload();
                break;
            }
            case FALOAD: {
                _bytecodeVisitor.faload();
                break;
            }
            case DALOAD: {
                _bytecodeVisitor.daload();
                break;
            }
            case AALOAD: {
                _bytecodeVisitor.aaload();
                break;
            }
            case BALOAD: {
                _bytecodeVisitor.baload();
                break;
            }
            case CALOAD: {
                _bytecodeVisitor.caload();
                break;
            }
            case SALOAD: {
                _bytecodeVisitor.saload();
                break;
            }
            case ISTORE: {
                final int index = readUnsigned1();
                _bytecodeVisitor.istore(index);
                break;
            }
            case LSTORE: {
                final int index = readUnsigned1();
                _bytecodeVisitor.lstore(index);
                break;
            }
            case FSTORE: {
                final int index = readUnsigned1();
                _bytecodeVisitor.fstore(index);
                break;
            }
            case DSTORE: {
                final int index = readUnsigned1();
                _bytecodeVisitor.dstore(index);
                break;
            }
            case ASTORE: {
                final int index = readUnsigned1();
                _bytecodeVisitor.astore(index);
                break;
            }
            case ISTORE_0: {
                _bytecodeVisitor.istore_0();
                break;
            }
            case ISTORE_1: {
                _bytecodeVisitor.istore_1();
                break;
            }
            case ISTORE_2: {
                _bytecodeVisitor.istore_2();
                break;
            }
            case ISTORE_3: {
                _bytecodeVisitor.istore_3();
                break;
            }
            case LSTORE_0: {
                _bytecodeVisitor.lstore_0();
                break;
            }
            case LSTORE_1: {
                _bytecodeVisitor.lstore_1();
                break;
            }
            case LSTORE_2: {
                _bytecodeVisitor.lstore_2();
                break;
            }
            case LSTORE_3: {
                _bytecodeVisitor.lstore_3();
                break;
            }
            case FSTORE_0: {
                _bytecodeVisitor.fstore_0();
                break;
            }
            case FSTORE_1: {
                _bytecodeVisitor.fstore_1();
                break;
            }
            case FSTORE_2: {
                _bytecodeVisitor.fstore_2();
                break;
            }
            case FSTORE_3: {
                _bytecodeVisitor.fstore_3();
                break;
            }
            case DSTORE_0: {
                _bytecodeVisitor.dstore_0();
                break;
            }
            case DSTORE_1: {
                _bytecodeVisitor.dstore_1();
                break;
            }
            case DSTORE_2: {
                _bytecodeVisitor.dstore_2();
                break;
            }
            case DSTORE_3: {
                _bytecodeVisitor.dstore_3();
                break;
            }
            case ASTORE_0: {
                _bytecodeVisitor.astore_0();
                break;
            }
            case ASTORE_1: {
                _bytecodeVisitor.astore_1();
                break;
            }
            case ASTORE_2: {
                _bytecodeVisitor.astore_2();
                break;
            }
            case ASTORE_3: {
                _bytecodeVisitor.astore_3();
                break;
            }
            case IASTORE: {
                _bytecodeVisitor.iastore();
                break;
            }
            case LASTORE: {
                _bytecodeVisitor.lastore();
                break;
            }
            case FASTORE: {
                _bytecodeVisitor.fastore();
                break;
            }
            case DASTORE: {
                _bytecodeVisitor.dastore();
                break;
            }
            case AASTORE: {
                _bytecodeVisitor.aastore();
                break;
            }
            case BASTORE: {
                _bytecodeVisitor.bastore();
                break;
            }
            case CASTORE: {
                _bytecodeVisitor.castore();
                break;
            }
            case SASTORE: {
                _bytecodeVisitor.sastore();
                break;
            }
            case POP: {
                _bytecodeVisitor.pop();
                break;
            }
            case POP2: {
                _bytecodeVisitor.pop2();
                break;
            }
            case DUP: {
                _bytecodeVisitor.dup();
                break;
            }
            case DUP_X1: {
                _bytecodeVisitor.dup_x1();
                break;
            }
            case DUP_X2: {
                _bytecodeVisitor.dup_x2();
                break;
            }
            case DUP2: {
                _bytecodeVisitor.dup2();
                break;
            }
            case DUP2_X1: {
                _bytecodeVisitor.dup2_x1();
                break;
            }
            case DUP2_X2: {
                _bytecodeVisitor.dup2_x2();
                break;
            }
            case SWAP: {
                _bytecodeVisitor.swap();
                break;
            }
            case IADD: {
                _bytecodeVisitor.iadd();
                break;
            }
            case LADD: {
                _bytecodeVisitor.ladd();
                break;
            }
            case FADD: {
                _bytecodeVisitor.fadd();
                break;
            }
            case DADD: {
                _bytecodeVisitor.dadd();
                break;
            }
            case ISUB: {
                _bytecodeVisitor.isub();
                break;
            }
            case LSUB: {
                _bytecodeVisitor.lsub();
                break;
            }
            case FSUB: {
                _bytecodeVisitor.fsub();
                break;
            }
            case DSUB: {
                _bytecodeVisitor.dsub();
                break;
            }
            case IMUL: {
                _bytecodeVisitor.imul();
                break;
            }
            case LMUL: {
                _bytecodeVisitor.lmul();
                break;
            }
            case FMUL: {
                _bytecodeVisitor.fmul();
                break;
            }
            case DMUL: {
                _bytecodeVisitor.dmul();
                break;
            }
            case IDIV: {
                _bytecodeVisitor.idiv();
                break;
            }
            case LDIV: {
                _bytecodeVisitor.ldiv();
                break;
            }
            case FDIV: {
                _bytecodeVisitor.fdiv();
                break;
            }
            case DDIV: {
                _bytecodeVisitor.ddiv();
                break;
            }
            case IREM: {
                _bytecodeVisitor.irem();
                break;
            }
            case LREM: {
                _bytecodeVisitor.lrem();
                break;
            }
            case FREM: {
                _bytecodeVisitor.frem();
                break;
            }
            case DREM: {
                _bytecodeVisitor.drem();
                break;
            }
            case INEG: {
                _bytecodeVisitor.ineg();
                break;
            }
            case LNEG: {
                _bytecodeVisitor.lneg();
                break;
            }
            case FNEG: {
                _bytecodeVisitor.fneg();
                break;
            }
            case DNEG: {
                _bytecodeVisitor.dneg();
                break;
            }
            case ISHL: {
                _bytecodeVisitor.ishl();
                break;
            }
            case LSHL: {
                _bytecodeVisitor.lshl();
                break;
            }
            case ISHR: {
                _bytecodeVisitor.ishr();
                break;
            }
            case LSHR: {
                _bytecodeVisitor.lshr();
                break;
            }
            case IUSHR: {
                _bytecodeVisitor.iushr();
                break;
            }
            case LUSHR: {
                _bytecodeVisitor.lushr();
                break;
            }
            case IAND: {
                _bytecodeVisitor.iand();
                break;
            }
            case LAND: {
                _bytecodeVisitor.land();
                break;
            }
            case IOR: {
                _bytecodeVisitor.ior();
                break;
            }
            case LOR: {
                _bytecodeVisitor.lor();
                break;
            }
            case IXOR: {
                _bytecodeVisitor.ixor();
                break;
            }
            case LXOR: {
                _bytecodeVisitor.lxor();
                break;
            }
            case IINC: {
                final int index = readUnsigned1();
                final int addend = readSigned1();
                _bytecodeVisitor.iinc(index, addend);
                break;
            }
            case I2L: {
                _bytecodeVisitor.i2l();
                break;
            }
            case I2F: {
                _bytecodeVisitor.i2f();
                break;
            }
            case I2D: {
                _bytecodeVisitor.i2d();
                break;
            }
            case L2I: {
                _bytecodeVisitor.l2i();
                break;
            }
            case L2F: {
                _bytecodeVisitor.l2f();
                break;
            }
            case L2D: {
                _bytecodeVisitor.l2d();
                break;
            }
            case F2I: {
                _bytecodeVisitor.f2i();
                break;
            }
            case F2L: {
                _bytecodeVisitor.f2l();
                break;
            }
            case F2D: {
                _bytecodeVisitor.f2d();
                break;
            }
            case D2I: {
                _bytecodeVisitor.d2i();
                break;
            }
            case D2L: {
                _bytecodeVisitor.d2l();
                break;
            }
            case D2F: {
                _bytecodeVisitor.d2f();
                break;
            }
            case I2B: {
                _bytecodeVisitor.i2b();
                break;
            }
            case I2C: {
                _bytecodeVisitor.i2c();
                break;
            }
            case I2S: {
                _bytecodeVisitor.i2s();
                break;
            }
            case LCMP: {
                _bytecodeVisitor.lcmp();
                break;
            }
            case FCMPL: {
                _bytecodeVisitor.fcmpl();
                break;
            }
            case FCMPG: {
                _bytecodeVisitor.fcmpg();
                break;
            }
            case DCMPL: {
                _bytecodeVisitor.dcmpl();
                break;
            }
            case DCMPG: {
                _bytecodeVisitor.dcmpg();
                break;
            }
            case IFEQ: {
                final int offset = readSigned2();
                _bytecodeVisitor.ifeq(offset);
                break;
            }
            case IFNE: {
                final int offset = readSigned2();
                _bytecodeVisitor.ifne(offset);
                break;
            }
            case IFLT: {
                final int offset = readSigned2();
                _bytecodeVisitor.iflt(offset);
                break;
            }
            case IFGE: {
                final int offset = readSigned2();
                _bytecodeVisitor.ifge(offset);
                break;
            }
            case IFGT: {
                final int offset = readSigned2();
                _bytecodeVisitor.ifgt(offset);
                break;
            }
            case IFLE: {
                final int offset = readSigned2();
                _bytecodeVisitor.ifle(offset);
                break;
            }
            case IF_ICMPEQ: {
                final int offset = readSigned2();
                _bytecodeVisitor.if_icmpeq(offset);
                break;
            }
            case IF_ICMPNE: {
                final int offset = readSigned2();
                _bytecodeVisitor.if_icmpne(offset);
                break;
            }
            case IF_ICMPLT: {
                final int offset = readSigned2();
                _bytecodeVisitor.if_icmplt(offset);
                break;
            }
            case IF_ICMPGE: {
                final int offset = readSigned2();
                _bytecodeVisitor.if_icmpge(offset);
                break;
            }
            case IF_ICMPGT: {
                final int offset = readSigned2();
                _bytecodeVisitor.if_icmpgt(offset);
                break;
            }
            case IF_ICMPLE: {
                final int offset = readSigned2();
                _bytecodeVisitor.if_icmple(offset);
                break;
            }
            case IF_ACMPEQ: {
                final int offset = readSigned2();
                _bytecodeVisitor.if_acmpeq(offset);
                break;
            }
            case IF_ACMPNE: {
                final int offset = readSigned2();
                _bytecodeVisitor.if_acmpne(offset);
                break;
            }
            case GOTO: {
                final int offset = readSigned2();
                _bytecodeVisitor.goto_(offset);
                break;
            }
            case JSR: {
                final int offset = readSigned2();
                _bytecodeVisitor.jsr(offset);
                break;
            }
            case RET: {
                final int index = readUnsigned1();
                _bytecodeVisitor.ret(index);
                break;
            }
            case TABLESWITCH: {
                alignAddress();
                final int defaultOffset = readSigned4();
                final int lowMatch = readSigned4();
                final int highMatch = readSigned4();
                if (lowMatch > highMatch) {
                    throw verifyError("Low must be less than or equal to high in TABLESWITCH");
                }
                final int numberOfCases = highMatch - lowMatch + 1;
                final int start = _currentBytePosition;
                _bytecodeVisitor.tableswitch(defaultOffset, lowMatch, highMatch, numberOfCases);
                final int caseBytesRead = _currentBytePosition - start;
                if ((caseBytesRead % 4) != 0 || (caseBytesRead / 4) != numberOfCases) {
                    ProgramError.unexpected("Bytecode visitor did not consume exactly the offset operands of the tableswitch instruction at " + _currentOpcodePosition);
                }
                break;
            }
            case LOOKUPSWITCH: {
                alignAddress();
                final int defaultOffset = readSigned4();
                final int numberOfCases = readSigned4();
                if (numberOfCases < 0) {
                    throw verifyError("Number of keys in LOOKUPSWITCH less than 0");
                }
                final int start = _currentBytePosition;
                _bytecodeVisitor.lookupswitch(defaultOffset, numberOfCases);
                final int caseBytesRead = _currentBytePosition - start;
                if ((caseBytesRead % 8) != 0 || (caseBytesRead / 8) != numberOfCases) {
                    ProgramError.unexpected("Bytecode visitor did not consume exactly the offset operands of the tableswitch instruction at " + _currentOpcodePosition);
                }
                break;
            }
            case IRETURN: {
                _bytecodeVisitor.ireturn();
                break;
            }
            case LRETURN: {
                _bytecodeVisitor.lreturn();
                break;
            }
            case FRETURN: {
                _bytecodeVisitor.freturn();
                break;
            }
            case DRETURN: {
                _bytecodeVisitor.dreturn();
                break;
            }
            case ARETURN: {
                _bytecodeVisitor.areturn();
                break;
            }
            case RETURN: {
                _bytecodeVisitor.vreturn();
                break;
            }
            case GETSTATIC: {
                final int index = readUnsigned2();
                _bytecodeVisitor.getstatic(index);
                break;
            }
            case PUTSTATIC: {
                final int index = readUnsigned2();
                _bytecodeVisitor.putstatic(index);
                break;
            }
            case GETFIELD: {
                final int index = readUnsigned2();
                _bytecodeVisitor.getfield(index);
                break;
            }
            case PUTFIELD: {
                final int index = readUnsigned2();
                _bytecodeVisitor.putfield(index);
                break;
            }
            case INVOKEVIRTUAL: {
                final int index = readUnsigned2();
                _bytecodeVisitor.invokevirtual(index);
                break;
            }
            case INVOKESPECIAL: {
                final int index = readUnsigned2();
                _bytecodeVisitor.invokespecial(index);
                break;
            }
            case INVOKESTATIC: {
                final int index = readUnsigned2();
                _bytecodeVisitor.invokestatic(index);
                break;
            }
            case INVOKEINTERFACE: {
                final int index = readUnsigned2();
                final int countUnused = readUnsigned1();
                final byte zero = readByte();
                if (zero != 0) {
                    throw verifyError("Fourth operand byte of invokeinterface must be zero");
                }
                _bytecodeVisitor.invokeinterface(index, countUnused);
                break;
            }
            case NEW: {
                final int index = readUnsigned2();
                _bytecodeVisitor.new_(index);
                break;
            }
            case NEWARRAY: {
                final int tag = readByte();
                _bytecodeVisitor.newarray(tag);
                break;
            }
            case ANEWARRAY: {
                final int index = readUnsigned2();
                _bytecodeVisitor.anewarray(index);
                break;
            }
            case ARRAYLENGTH: {
                _bytecodeVisitor.arraylength();
                break;
            }
            case ATHROW: {
                _bytecodeVisitor.athrow();
                break;
            }
            case CHECKCAST: {
                final int index = readUnsigned2();
                _bytecodeVisitor.checkcast(index);
                break;
            }
            case INSTANCEOF: {
                final int index = readUnsigned2();
                _bytecodeVisitor.instanceof_(index);
                break;
            }
            case MONITORENTER: {
                _bytecodeVisitor.monitorenter();
                break;
            }
            case MONITOREXIT: {
                _bytecodeVisitor.monitorexit();
                break;
            }
            case WIDE: {
                _bytecodeVisitor.wide();
                wide();
                break;
            }
            case MULTIANEWARRAY: {
                final int index = readUnsigned2();
                final int nDimensions = readUnsigned1();
                _bytecodeVisitor.multianewarray(index, nDimensions);
                break;
            }
            case IFNULL: {
                final int offset = readSigned2();
                _bytecodeVisitor.ifnull(offset);
                break;
            }
            case IFNONNULL: {
                final int offset = readSigned2();
                _bytecodeVisitor.ifnonnull(offset);
                break;
            }
            case GOTO_W: {
                final int offset = readSigned4();
                _bytecodeVisitor.goto_w(offset);
                break;
            }
            case JSR_W: {
                final int offset = readSigned4();
                _bytecodeVisitor.jsr_w(offset);
                break;
            }
            case BREAKPOINT: {
                _bytecodeVisitor.breakpoint();
                break;
            }
            case CALLNATIVE: {
                final int nativeFunctionDescriptorIndex = readUnsigned2();
                _bytecodeVisitor.callnative(nativeFunctionDescriptorIndex);
                break;
            }
            default: {
                throw verifyError("Unsupported bytecode: " + _currentOpcode);
            }
        }
        _bytecodeVisitor.instructionDecoded();
    }

    public int scanInstruction(BytecodeBlock bytecodeBlock) {
        _bytecodeBlock = bytecodeBlock;
        try {
            _currentBytePosition = _bytecodeBlock.start();
            _currentOpcodePosition = _currentBytePosition;
            scanInstruction();
            return _currentBytePosition;
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
            if (_currentBytePosition > bytecodeBlock.end()) {
                throw verifyError("Ran off end of code");
            }
            throw arrayIndexOutOfBoundsException;
        }
    }

    public int scanInstruction(byte[] bytecode, int startAddress) {
        return scanInstruction(new BytecodeBlock(bytecode, startAddress, bytecode.length));
    }

    public void scan(BytecodeBlock bytecodeBlock) {
        _bytecodeBlock = bytecodeBlock;
        try {
            _currentBytePosition = _bytecodeBlock.start();
            _currentOpcodePosition = _currentBytePosition;
            _bytecodeVisitor.prologue();
            while (!_stopped && _currentBytePosition <= _bytecodeBlock.end()) {
                _currentOpcodePosition = _currentBytePosition;
                scanInstruction();
            }
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
            if (_currentBytePosition > bytecodeBlock.end()) {
                throw verifyError("Ran off end of code");
            }
            throw arrayIndexOutOfBoundsException;
        }
    }

    public void scan(ClassMethodActor classMethodActor) {
        scan(new BytecodeBlock(classMethodActor.codeAttribute().code()));
    }

    public void skipBytes(int numBytes) {
        _currentBytePosition += numBytes;
    }


}
