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
/*VCSID=f677134e-3c3e-41b4-8ed2-e54071616ad5*/
package com.sun.max.vm.interpreter.eir.ia32;

import static com.sun.max.vm.compiler.eir.ia32.IA32EirRegister.General.*;
import static com.sun.max.vm.interpreter.eir.ia32.IA32EirCPU.ConditionFlag.*;

import java.math.*;

import com.sun.max.asm.ia32.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.ia32.*;
import com.sun.max.vm.compiler.eir.ia32.IA32EirInstruction.*;
import com.sun.max.vm.interpreter.*;
import com.sun.max.vm.interpreter.eir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;


/**
 * IA32 EIR instruction visitors for interpretation.
 * 
 * Instances of this interpreter are configured by the following properties where the
 * value of a property is retrieved by calling {@link System#getProperty(String)} with
 * a key composed of the property name prefixed by {@link IrInterpreter#PROPERTY_PREFIX}.
 * 
 * <p>
 * Property: {@code "jit"} <br />
 * Default: {@code false} <br />
 * Description: Enables {@linkplain #jitEnabled() JIT compilation}. This option is useful for
 *              testing the correctness of compiled code with respect to calling conventions.
 * <p>
 * Property: {@code "trace.level"} <br />
 * Default: {@code 3} <br />
 * Description: Specifies the {@linkplain Trace#level() level} at which the interpreter will emit tracing while interpreting.
 * <p>
 * Property: {@code "trace.cpu"} <br />
 * Default: {@code false} <br />
 * Description: Includes CPU state {@linkplain IA32EirCPU#dump(java.io.PrintStream) dumps} after each instruction traced.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 * @author Laurent Daynes
 */
public class IA32EirInterpreter extends EirInterpreter implements IA32EirInstructionVisitor {

    public IA32EirInterpreter(IA32EirGenerator eirGenerator) {
        super(eirGenerator);
    }

    private IA32EirCPU _cpu = new IA32EirCPU(this);

    @Override
    protected IA32EirCPU cpu() {
        return _cpu;
    }

    public void visit(EirPrologue prologue) {
        final int frameSize = prologue.eirMethod().frameSize();
        if (frameSize != 0) {
            _cpu.writeFramePointer(_cpu.readFramePointer().minus(frameSize));
        }
    }

    public void visit(EirEpilogue epilogue) {
        final int frameSize = epilogue.eirMethod().frameSize();
        if (frameSize != 0) {
            _cpu.writeFramePointer(_cpu.readFramePointer().plus(frameSize));
        }
    }

    public void visit(EirAssignment assignment) {
        switch (assignment.kind().asEnum()) {
            case INT: {
                final int value = _cpu.readInt(assignment.sourceOperand().location());
                _cpu.writeInt(assignment.destinationOperand().location(), value);
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                final Value value = _cpu.read(assignment.sourceOperand().location());
                _cpu.write(assignment.destinationOperand().location(), adjustToAssignmentType(value));
                break;
            }
            case FLOAT: {
                final float value = _cpu.readFloat(assignment.sourceOperand().location());
                _cpu.writeFloat(assignment.destinationOperand().location(), value);
                break;
            }
            case DOUBLE: {
                final double value = _cpu.readDouble(assignment.sourceOperand().location());
                _cpu.writeDouble(assignment.destinationOperand().location(), value);
                break;
            }
            default: {
                ProgramError.unknownCase();
                break;
            }
        }
    }

    public void visit(IA32EirLoad load) {
        final Value value;
        final Value pointer = _cpu.read(load.pointerOperand().location());
        if (pointer.kind() == Kind.WORD) {
            // This must be a load from the stack
            assert load.indexOperand() == null;
            final int offset = load.offsetOperand() != null ? _cpu.read(load.offsetOperand().location()).asInt() : 0;
            value = _cpu.stack().read(pointer.asWord().asAddress().plus(offset));
        } else {
            final Value[] arguments = (load.indexOperand() != null) ? new Value[3] : new Value[2];
            arguments[0] = pointer;
            if (load.offsetOperand() != null) {
                arguments[1] = IntValue.from(_cpu.readInt(load.offsetOperand().location()));
            } else {
                arguments[1] = IntValue.from(0);
            }
            if (load.indexOperand() != null) {
                arguments[2] = IntValue.from(_cpu.readInt(load.indexOperand().location()));
            }
            value = pointerLoad(load.kind(), arguments);
        }
        switch (load.kind().asEnum()) {
            case BYTE: {
                _cpu.writeWord(load.destinationOperand().location(), Address.fromInt(value.toInt()));
                break;
            }
            case SHORT: {
                _cpu.writeWord(load.destinationOperand().location(), Address.fromInt(value.unsignedToShort()));
                break;
            }
            case BOOLEAN:
            case CHAR:
            case INT: {
                _cpu.writeWord(load.destinationOperand().location(), Address.fromInt(value.unsignedToInt()));
                break;
            }
            case LONG: {
                _cpu.writeLong(load.destinationOperand().location(), value.asLong());
                break;
            }
            case FLOAT: {
                _cpu.writeFloat(load.destinationOperand().location(), value.asFloat());
                break;
            }
            case DOUBLE: {
                _cpu.writeDouble(load.destinationOperand().location(), value.asDouble());
                break;
            }
            case WORD:
            case REFERENCE: {
                _cpu.write(load.destinationOperand().location(), value);
                break;
            }
            case VOID: {
                ProgramError.unexpected();
            }
        }
    }

    public void visit(IA32EirStore store) {
        final Value pointer = _cpu.read(store.pointerOperand().location());
        final Value value = _cpu.read(store.kind(), store.valueOperand().location());
        if (pointer.kind() == Kind.WORD) {
            // This must be a store to the stack
            assert store.indexOperand() == null;
            final int offset = store.offsetOperand() != null ? _cpu.read(store.offsetOperand().location()).asInt() : 0;
            _cpu.stack().write(pointer.asWord().asAddress().plus(offset), value);
        } else {
            final Value[] arguments = (store.indexOperand() != null) ? new Value[4] : new Value[3];
            arguments[0] = pointer;
            if (store.offsetOperand() != null) {
                arguments[1] = IntValue.from(_cpu.readInt(store.offsetOperand().location()));
            } else {
                arguments[1] = IntValue.from(0);
            }
            if (store.indexOperand() != null) {
                arguments[2] = IntValue.from(_cpu.readInt(store.indexOperand().location()));
                arguments[3] = value;
            } else {
                arguments[2] = value;
            }
            pointerStore(store.kind(), arguments);
        }
    }

    public void visit(IA32EirCompareAndSwap compareAndSwap) {
        Problem.unimplemented();
    }

    public void visit(IA32EirInstruction.ADD instruction) {
        final int a = _cpu.readInt(instruction.destinationOperand().location());
        final int b = _cpu.readInt(instruction.sourceOperand().location());
        _cpu.writeInt(instruction.destinationOperand().location(), a + b);
    }

    public void visit(ADDSD instruction) {
        final double a = _cpu.readDouble(instruction.destinationOperand().location());
        final double b = _cpu.readDouble(instruction.sourceOperand().location());
        _cpu.writeDouble(instruction.destinationOperand().location(), a + b);
    }

    public void visit(ADDSS instruction) {
        final float a = _cpu.readFloat(instruction.destinationOperand().location());
        final float b = _cpu.readFloat(instruction.sourceOperand().location());
        _cpu.writeFloat(instruction.destinationOperand().location(), a + b);
    }

    public void visit(AND instruction) {
        final int a = _cpu.readInt(instruction.destinationOperand().location());
        final int b = _cpu.readInt(instruction.sourceOperand().location());
        _cpu.writeInt(instruction.destinationOperand().location(), a & b);
    }

    public void visit(CDQ instruction) {
        assert instruction.destinationOperand().location() == EDX;
        assert instruction.sourceOperand().location() == EAX;

        final int eax = _cpu.readInt(instruction.sourceOperand().location());
        final int edx = (eax < 0) ? -1 : 0;
        _cpu.writeInt(instruction.destinationOperand().location(), edx);
    }

    public void visit(CMOVA_I32 instruction) {
        if (!_cpu.test(CF) && !_cpu.test(ZF)) {
            final int i = _cpu.readInt(instruction.sourceOperand().location());
            _cpu.writeInt(instruction.destinationOperand().location(), i);
        }
    }

    public void visit(CMOVB_I32 instruction) {
        if (_cpu.test(CF)) {
            final int i = _cpu.readInt(instruction.sourceOperand().location());
            _cpu.writeInt(instruction.destinationOperand().location(), i);
        }
    }

    public void visit(CMOVE_I32 instruction) {
        if (_cpu.test(ZF)) {
            final int i = _cpu.readInt(instruction.sourceOperand().location());
            _cpu.writeInt(instruction.destinationOperand().location(), i);
        }
    }

    public void visit(CMOVG_I32 instruction) {
        if (!_cpu.test(ZF) && !_cpu.test(SF)) {
            final int i = _cpu.readInt(instruction.sourceOperand().location());
            _cpu.writeInt(instruction.destinationOperand().location(), i);
        }
    }

    public void visit(CMOVGE_I32 instruction) {
        if (_cpu.test(SF) == _cpu.test(OF)) {
            final int i = _cpu.readInt(instruction.sourceOperand().location());
            _cpu.writeInt(instruction.destinationOperand().location(), i);
        }
    }

    public void visit(CMOVL_I32 instruction) {
        if (_cpu.test(OF) != _cpu.test(SF)) {
            final int i = _cpu.readInt(instruction.sourceOperand().location());
            _cpu.writeInt(instruction.destinationOperand().location(), i);
        }
    }

    public void visit(CMOVLE_I32 instruction) {
        if (_cpu.test(ZF) || (_cpu.test(SF) != _cpu.test(OF))) {
            final int i = _cpu.readInt(instruction.sourceOperand().location());
            _cpu.writeInt(instruction.destinationOperand().location(), i);
        }
    }

    public void visit(CMOVP_I32 instruction) {
        if (_cpu.test(PF)) {
            final int i = _cpu.readInt(instruction.sourceOperand().location());
            _cpu.writeInt(instruction.destinationOperand().location(), i);
        }
    }

    public void visit(CMP_I32 instruction) {
        final int a = _cpu.readInt(instruction.destinationOperand().location());
        final int b = _cpu.readInt(instruction.sourceOperand().location());

        _cpu.set(ZF, a == b);
        _cpu.set(CF, Address.fromInt(a).lessThan(Address.fromInt(b)));

        _cpu.set(OF, ((a < 0) && (b >= 0) && (a - b >= 0)) || ((a >= 0) && (b < 0) && (a - b < 0)));
        _cpu.set(SF, a >= b ? _cpu.test(OF) : !_cpu.test(OF));
    }

    public void visit(CMP_I64 instruction) {
        final Value valueA = _cpu.read(instruction.destinationOperand().location());
        final Value valueB = _cpu.read(instruction.sourceOperand().location());
        assert (valueA.kind() == Kind.REFERENCE) == (valueB.kind() == Kind.REFERENCE);
        final long a;
        final long b;
        if (valueA.kind() == Kind.REFERENCE) {
            if (valueA.asObject() == valueB.asObject()) {
                _cpu.set(ZF, true);
                // It does not matter what the actual values here are as long as they are the same:
                a = 0L;
                b = 0L;
            } else {
                _cpu.set(ZF, false);
                // It does not matter what the actual values here are as long as they differ:
                a = valueA.hashCode();
                b = ~a;
            }
        } else {
            a = valueA.toLong();
            b = valueB.toLong();
            _cpu.set(ZF, a == b);
        }
        _cpu.set(CF, Address.fromLong(a).lessThan(Address.fromLong(b)));

        _cpu.set(OF, ((a < 0) && (b >= 0) && (a - b >= 0)) || ((a >= 0) && (b < 0) && (a - b < 0)));
        _cpu.set(SF, a >= b ? _cpu.test(OF) : !_cpu.test(OF));
    }

    private boolean isNaN(double d) {
        // TODO
        return false;
    }

    private boolean doubleCompare(IA32XMMComparison comparison, double a, double b) {
        if (isNaN(a) || isNaN(b)) {
            return comparison == IA32XMMComparison.UNORDERED;
        }
        switch (comparison) {
            case ORDERED:
                return true;
            case UNORDERED:
                return false;
            case EQUAL:
                return a == b;
            case LESS_THAN:
                return a < b;
            case GREATER_THAN:
                return a > b;
            case LESS_THAN_OR_EQUAL:
                return a <= b;
            case NOT_EQUAL:
                return a != b;
            case NOT_LESS_THAN:
                return !(a < b);
            case NOT_LESS_THAN_OR_EQUAL:
                return !(a <= b);
        }
        ProgramError.unknownCase();
        return false;
    }

    public void visit(CMPSD instruction) {
        final double a = _cpu.readDouble(instruction.destinationOperand().location());
        final double b = _cpu.readDouble(instruction.sourceOperand().location());

        final double result = doubleCompare(instruction.comparison(), a, b) ? UnsafeLoophole.longToDouble(-1L) : UnsafeLoophole.longToDouble(0L);
        _cpu.writeDouble(instruction.destinationOperand().location(), result);
    }

    private boolean isNaN(float f) {
        // TODO
        return false;
    }

    private boolean floatCompare(IA32XMMComparison comparison, float a, float b) {
        if (isNaN(a) || isNaN(b)) {
            return comparison == IA32XMMComparison.UNORDERED;
        }
        switch (comparison) {
            case ORDERED:
                return true;
            case UNORDERED:
                return false;
            case EQUAL:
                return a == b;
            case LESS_THAN:
                return a < b;
            case GREATER_THAN:
                return a > b;
            case LESS_THAN_OR_EQUAL:
                return a <= b;
            case NOT_EQUAL:
                return a != b;
            case NOT_LESS_THAN:
                return !(a < b);
            case NOT_LESS_THAN_OR_EQUAL:
                return !(a <= b);
        }
        ProgramError.unknownCase();
        return false;
    }

    public void visit(CMPSS instruction) {
        final float a = _cpu.readFloat(instruction.destinationOperand().location());
        final float b = _cpu.readFloat(instruction.sourceOperand().location());

        final float result = floatCompare(instruction.comparison(), a, b) ? UnsafeLoophole.intToFloat(-1) : UnsafeLoophole.intToFloat(0);
        _cpu.writeFloat(instruction.destinationOperand().location(), result);
    }

    public void visit(COMISD instruction) {
        final double a = _cpu.readDouble(instruction.destinationOperand().location());
        final double b = _cpu.readDouble(instruction.sourceOperand().location());

        if (isNaN(a) || isNaN(b)) {
            _cpu.set(PF, true);
            _cpu.set(ZF, true);
            _cpu.set(CF, true);

        } else {
            _cpu.set(PF, false);
            _cpu.set(ZF, a == b);
            _cpu.set(CF, a < b);
        }
        _cpu.set(OF, false);
        _cpu.set(SF, false);
    }

    public void visit(COMISS instruction) {
        final float a = _cpu.readFloat(instruction.destinationOperand().location());
        final float b = _cpu.readFloat(instruction.sourceOperand().location());

        if (isNaN(a) || isNaN(b)) {
            _cpu.set(PF, true);
            _cpu.set(ZF, true);
            _cpu.set(CF, true);

        } else {
            _cpu.set(PF, false);
            _cpu.set(ZF, a == b);
            _cpu.set(CF, a < b);
        }
        _cpu.set(OF, false);
        _cpu.set(SF, false);
    }

    public void visit(CVTSD2SI_I32 instruction) {
        final double d = _cpu.readDouble(instruction.sourceOperand().location());
        _cpu.writeInt(instruction.destinationOperand().location(), (int) d);
    }

    public void visit(CVTSD2SI_I64 instruction) {
        final double d = _cpu.readDouble(instruction.sourceOperand().location());
        _cpu.writeLong(instruction.destinationOperand().location(), (long) d);
    }

    public void visit(CVTSD2SS instruction) {
        final double d = _cpu.readDouble(instruction.sourceOperand().location());
        _cpu.writeFloat(instruction.destinationOperand().location(), (float) d);
    }

    public void visit(CVTSI2SD instruction) {
        final int n = _cpu.readInt(instruction.sourceOperand().location());
        _cpu.writeDouble(instruction.destinationOperand().location(), n);
    }

    public void visit(CVTSI2SS_I32 instruction) {
        final int n = _cpu.readInt(instruction.sourceOperand().location());
        _cpu.writeFloat(instruction.destinationOperand().location(), n);
    }

    public void visit(CVTSS2SD instruction) {
        final float f = _cpu.readFloat(instruction.sourceOperand().location());
        _cpu.writeDouble(instruction.destinationOperand().location(), f);
    }

    public void visit(CVTSS2SI instruction) {
        final float f = _cpu.readFloat(instruction.sourceOperand().location());
        _cpu.writeInt(instruction.destinationOperand().location(), (int) f);
    }

    private static final BigInteger MAX_ULONG64 = new BigInteger("FFFFFFFFFFFFFFFF", 16);

    public void visit(DIV_I64 instruction) {
        final long rd = _cpu.readLong(EDX);
        final long dividend = _cpu.readLong(EAX);
        if (((rd == 0L) != (dividend >= 0)) || ((rd == -1L) != (dividend < 0))) {
            ProgramError.unexpected("IDIV_I64: junk in RD");
        }
        final int divisor = _cpu.readInt(instruction.divisor().location());
        if (dividend < 0) {
            final BigInteger unsignedDividend = BigInteger.valueOf(dividend).and(MAX_ULONG64);
            final BigInteger[] quotientAndRemainder = unsignedDividend.divideAndRemainder(BigInteger.valueOf(divisor));
            _cpu.writeLong(EAX, quotientAndRemainder[0].longValue());
            _cpu.writeLong(EDX, quotientAndRemainder[1].longValue());
        } else {
            _cpu.writeLong(EAX, dividend / divisor);
            _cpu.writeLong(EDX, dividend % divisor);
        }
    }

    public void visit(DIVSD instruction) {
        final double dividend = _cpu.readDouble(instruction.destinationOperand().location());
        final double divisor = _cpu.readDouble(instruction.sourceOperand().location());
        _cpu.writeDouble(instruction.destinationOperand().location(), dividend / divisor);
    }

    public void visit(DIVSS instruction) {
        final float dividend = _cpu.readFloat(instruction.destinationOperand().location());
        final float divisor = _cpu.readFloat(instruction.sourceOperand().location());
        _cpu.writeFloat(instruction.destinationOperand().location(), dividend / divisor);
    }

    public void visit(IDIV instruction) {
        final int divisor = _cpu.readInt(instruction.divisor().location());
        long dividend = _cpu.readInt(EDX);
        dividend <<= 32;
        dividend |= _cpu.readInt(EAX);
        _cpu.writeInt(EAX, (int) (dividend / divisor));
        _cpu.writeInt(EDX, (int) (dividend % divisor));
    }

    public void visit(IMUL instruction) {
        final int a = _cpu.readInt(instruction.destinationOperand().location());
        final int b = _cpu.readInt(instruction.sourceOperand().location());
        _cpu.writeInt(instruction.destinationOperand().location(), a * b);
    }

    public void visit(JMP instruction) {
        _cpu.gotoBlock(instruction.target());
    }

    public void visit(JMP_indirect instruction) {
        ProgramError.unexpected("indirect jump not implemented at EIR level - it should only occur during exception dispatching at target level");
    }

    private void conditionalBranch(IA32EirConditionalBranch instruction, boolean condition) {
        if (condition) {
            _cpu.gotoBlock(instruction.target());
        } else {
            _cpu.gotoBlock(instruction.next());
        }
    }

    public void visit(JA instruction) {
        conditionalBranch(instruction, !_cpu.test(CF) && !_cpu.test(ZF));
    }

    public void visit(JAE instruction) {
        conditionalBranch(instruction, !_cpu.test(CF));
    }

    public void visit(JB instruction) {
        conditionalBranch(instruction, _cpu.test(CF));
    }

    public void visit(JBE instruction) {
        conditionalBranch(instruction, _cpu.test(CF) || _cpu.test(ZF));
    }

    public void visit(JG instruction) {
        conditionalBranch(instruction, !_cpu.test(ZF) && !_cpu.test(SF));
    }

    public void visit(JGE instruction) {
        conditionalBranch(instruction, _cpu.test(SF) == _cpu.test(OF));
    }

    public void visit(JL instruction) {
        conditionalBranch(instruction, _cpu.test(SF) != _cpu.test(OF));
    }

    public void visit(JLE instruction) {
        conditionalBranch(instruction, _cpu.test(ZF) || (_cpu.test(SF) != _cpu.test(OF)));
    }

    public void visit(JNZ instruction) {
        conditionalBranch(instruction, !_cpu.test(ZF));
    }

    public void visit(JZ instruction) {
        conditionalBranch(instruction, _cpu.test(ZF));
    }

    public void visit(LEA_PC instruction) {
        _cpu.write(instruction.operand().location(), new WordValue(Address.fromLong(_cpu.currentInstructionAddress().index())));
    }

    public void visit(LEA_STACK_ADDRESS instruction) {
        final int sourceOffset = _cpu.offset(instruction.sourceOperand().location().asStackSlot());
        _cpu.write(instruction.destinationOperand().location(), new WordValue(_cpu.readFramePointer().plus(sourceOffset)));
    }

    public void visit(LFENCE instruction) {
    }

    public void visit(MFENCE instruction) {
    }

    public void visit(MOVD instruction) {
        final float f = _cpu.readFloat(instruction.sourceOperand().location());
        _cpu.writeInt(instruction.destinationOperand().location(), UnsafeLoophole.floatToInt(f));
    }

    public void visit(MOVSD instruction) {
        final long n = _cpu.readLong(instruction.sourceOperand().location());
        _cpu.writeLong(instruction.destinationOperand().location(), n);
    }

    public void visit(MOVSX_I8 instruction) {
        final byte b = _cpu.readByte(instruction.sourceOperand().location());
        _cpu.writeInt(instruction.destinationOperand().location(), b);
    }

    public void visit(MOVSX_I16 instruction) {
        final short s = _cpu.readShort(instruction.sourceOperand().location());
        _cpu.writeInt(instruction.destinationOperand().location(), s);
    }

    public void visit(MOVZX_I16 instruction) {
        final short s = _cpu.readShort(instruction.sourceOperand().location());
        _cpu.writeInt(instruction.destinationOperand().location(), s & 0xffff);
    }

    public void visit(MULSD instruction) {
        final double a = _cpu.readDouble(instruction.destinationOperand().location());
        final double b = _cpu.readDouble(instruction.sourceOperand().location());
        _cpu.writeDouble(instruction.destinationOperand().location(), a * b);
    }

    public void visit(MULSS instruction) {
        final float a = _cpu.readFloat(instruction.destinationOperand().location());
        final float b = _cpu.readFloat(instruction.sourceOperand().location());
        _cpu.writeFloat(instruction.destinationOperand().location(), a * b);
    }

    public void visit(NEG instruction) {
        final int operand = _cpu.readInt(instruction.operand().location());
        _cpu.writeInt(instruction.operand().location(), -operand);
    }

    public void visit(NOT instruction) {
        final int operand = _cpu.readInt(instruction.operand().location());
        _cpu.writeInt(instruction.operand().location(), ~operand);
    }

    public void visit(OR instruction) {
        final int a = _cpu.readInt(instruction.destinationOperand().location());
        final int b = _cpu.readInt(instruction.sourceOperand().location());
        _cpu.writeInt(instruction.destinationOperand().location(), a | b);
    }

    public void visit(PADDQ instruction) {
        final long a = _cpu.readLong(instruction.destinationOperand().location());
        final long b = _cpu.readLong(instruction.sourceOperand().location());
        _cpu.writeLong(instruction.destinationOperand().location(), a + b);
    }

    public void visit(PAND instruction) {
        final long a = _cpu.readLong(instruction.destinationOperand().location());
        final long b = _cpu.readLong(instruction.sourceOperand().location());
        _cpu.writeLong(instruction.destinationOperand().location(), a & b);
    }

    public void visit(PANDN instruction) {
        final long a = _cpu.readLong(instruction.destinationOperand().location());
        final long b = _cpu.readLong(instruction.sourceOperand().location());
        _cpu.writeLong(instruction.destinationOperand().location(), ~a & b);
    }

    public void visit(PCMPEQD instruction) {
        final long a = _cpu.readLong(instruction.operand().location());
        final long b = _cpu.readLong(instruction.operand().location());
        long result = 0L;
        if ((a >>> 32) == (b >>> 32)) {
            result |= 0xffffffff00000000L;
        }
        if ((a & 0xffffffffL) == (b & 0xffffffffL)) {
            result |= 0xffffffffL;
        }
        _cpu.writeLong(instruction.operand().location(), result);
    }

    public void visit(POR instruction) {
        final long a = _cpu.readLong(instruction.destinationOperand().location());
        final long b = _cpu.readLong(instruction.sourceOperand().location());
        _cpu.writeLong(instruction.destinationOperand().location(), a | b);
    }

    public void visit(PSLLQ instruction) {
        final long number = _cpu.readInt(instruction.destinationOperand().location());
        final int shift = _cpu.readByte(instruction.sourceOperand().location());
        _cpu.writeLong(instruction.destinationOperand().location(), number << shift);
    }

    public void visit(PSRLQ instruction) {
        final long number = _cpu.readInt(instruction.destinationOperand().location());
        final int shift = _cpu.readByte(instruction.sourceOperand().location());
        _cpu.writeLong(instruction.destinationOperand().location(), number >>> shift);
    }

    public void visit(PSUBQ instruction) {
        final long a = _cpu.readLong(instruction.destinationOperand().location());
        final long b = _cpu.readLong(instruction.sourceOperand().location());
        _cpu.writeLong(instruction.destinationOperand().location(), a - b);
    }

    public void visit(PXOR instruction) {
        if (instruction.destinationOperand().location().equals(instruction.sourceOperand().location())) {
            // This is the x86 idiom for zeroing a location. It needs to be special-cased
            // in case the location is currently holding a reference as reading a
            // long from such a location will not work.
            _cpu.writeLong(instruction.destinationOperand().location(), 0L);
            return;
        }
        final long a = _cpu.readLong(instruction.destinationOperand().location());
        final long b = _cpu.readLong(instruction.sourceOperand().location());
        _cpu.writeLong(instruction.destinationOperand().location(), a ^ b);
    }

    public void visit(RET instruction) {
        ret();
    }

    public void visit(SAL instruction) {
        assert !(instruction.sourceOperand().location() instanceof IA32EirRegister.General) || (instruction.sourceOperand().location() == ECX);
        final int number = _cpu.readInt(instruction.destinationOperand().location());
        final int shift = _cpu.readByte(instruction.sourceOperand().location());
        _cpu.writeInt(instruction.destinationOperand().location(), number << shift);
    }

    public void visit(SAR instruction) {
        assert !(instruction.sourceOperand().location() instanceof IA32EirRegister.General) || (instruction.sourceOperand().location() == ECX);
        final int number = _cpu.readInt(instruction.destinationOperand().location());
        final int shift = _cpu.readByte(instruction.sourceOperand().location());
        _cpu.writeInt(instruction.destinationOperand().location(), number >> shift);
    }

    public void visit(SFENCE instruction) {
    }

    public void visit(SHR instruction) {
        assert !(instruction.sourceOperand().location() instanceof IA32EirRegister.General) || (instruction.sourceOperand().location() == ECX);
        final int number = _cpu.readInt(instruction.destinationOperand().location());
        final int shift = _cpu.readByte(instruction.sourceOperand().location());
        _cpu.writeInt(instruction.destinationOperand().location(), number >>> shift);
    }

    public void visit(STORE_HIGH instruction) {
        final int i = _cpu.readInt(instruction.sourceOperand().location());
        long n = _cpu.readInt(instruction.destinationOperand().location());
        n &= 0xffffffff;
        n |= ((long) i) << 32;
        _cpu.writeLong(instruction.destinationOperand().location(), n);
    }

    public void visit(STORE_LOW instruction) {
        final int i = _cpu.readInt(instruction.sourceOperand().location());
        long n = _cpu.readInt(instruction.destinationOperand().location());
        n &= 0xffffffff00000000L;
        n |= i & 0xffffffffL;
        _cpu.writeLong(instruction.destinationOperand().location(), n);
    }

    public void visit(SUB instruction) {
        final int a = _cpu.readInt(instruction.destinationOperand().location());
        final int b = _cpu.readInt(instruction.sourceOperand().location());
        _cpu.writeInt(instruction.destinationOperand().location(), a - b);
    }

    public void visit(SUBSD instruction) {
        final double a = _cpu.readDouble(instruction.destinationOperand().location());
        final double b = _cpu.readDouble(instruction.sourceOperand().location());
        _cpu.writeDouble(instruction.destinationOperand().location(), a - b);
    }

    public void visit(SUBSS instruction) {
        final float a = _cpu.readFloat(instruction.destinationOperand().location());
        final float b = _cpu.readFloat(instruction.sourceOperand().location());
        _cpu.writeFloat(instruction.destinationOperand().location(), a - b);
    }

    public void visit(SWITCH_I32 instruction) {
        final int a = _cpu.readInt(instruction.tag().location());

        for (int i = 0; i < instruction.matches().length; i++) {
            if (a == instruction.matches() [i].value().asInt()) {
                _cpu.gotoBlock(instruction.targets()[i]);
                return;
            }
        }

        _cpu.gotoBlock(instruction.defaultTarget());
    }

    public void visit(XOR instruction) {
        if (instruction.destinationOperand().location().equals(instruction.sourceOperand().location())) {
            // This is the x86 idiom for zeroing a location. It needs to be special-cased
            // in case the location is currently holding a reference as reading a
            // long from such a location will not work.
            _cpu.writeInt(instruction.destinationOperand().location(), 0);
            return;
        }
        final int a = _cpu.readInt(instruction.destinationOperand().location());
        final int b = _cpu.readInt(instruction.sourceOperand().location());
        _cpu.writeInt(instruction.destinationOperand().location(), a ^ b);
    }

    public void visit(XORPD instruction) {
        if (instruction.destinationOperand().location().equals(instruction.sourceOperand().location())) {
            // This is the x86 idiom for zeroing a location. It needs to be special-cased
            // in case the location is currently holding a reference as reading a
            // long from such a location will not work.
            _cpu.writeDouble(instruction.destinationOperand().location(), 0D);
            return;
        }
        final long a = UnsafeLoophole.doubleToLong(_cpu.readDouble(instruction.destinationOperand().location()));
        final long b = UnsafeLoophole.doubleToLong(_cpu.readDouble(instruction.sourceOperand().location()));
        _cpu.writeDouble(instruction.destinationOperand().location(), UnsafeLoophole.longToDouble(a ^ b));
    }

    public void visit(XORPS instruction) {
        if (instruction.destinationOperand().location().equals(instruction.sourceOperand().location())) {
            // This is the x86 idiom for zeroing a location. It needs to be special-cased
            // in case the location is currently holding a reference as reading a
            // long from such a location will not work.
            _cpu.writeFloat(instruction.destinationOperand().location(), 0F);
            return;
        }
        final int a = UnsafeLoophole.floatToInt(_cpu.readFloat(instruction.destinationOperand().location()));
        final int b = UnsafeLoophole.floatToInt(_cpu.readFloat(instruction.sourceOperand().location()));
        _cpu.writeFloat(instruction.destinationOperand().location(), UnsafeLoophole.intToFloat(a ^ b));
    }
}
