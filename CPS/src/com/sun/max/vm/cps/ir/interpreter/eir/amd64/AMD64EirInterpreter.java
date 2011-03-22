/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.cps.ir.interpreter.eir.amd64;

import static com.sun.max.vm.cps.eir.amd64.AMD64EirRegister.General.*;
import static com.sun.max.vm.cps.ir.interpreter.eir.amd64.AMD64EirCPU.ConditionFlag.*;

import java.lang.reflect.*;
import java.math.*;

import com.sun.max.asm.amd64.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.EirStackSlot.*;
import com.sun.max.vm.cps.eir.amd64.*;
import com.sun.max.vm.cps.eir.amd64.AMD64EirInstruction.*;
import com.sun.max.vm.cps.ir.interpreter.*;
import com.sun.max.vm.cps.ir.interpreter.eir.*;
import com.sun.max.vm.cps.ir.interpreter.eir.amd64.AMD64EirCPU.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * AMD64 EIR instruction visitors for interpretation.
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
 * Description: Includes CPU state {@linkplain AMD64EirCPU#dump(java.io.PrintStream) dumps} after each instruction traced.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class AMD64EirInterpreter extends EirInterpreter implements AMD64EirInstructionVisitor {

    public AMD64EirInterpreter(AMD64EirGenerator eirGenerator) {
        super(eirGenerator);
    }

    private AMD64EirCPU cpu = new AMD64EirCPU(this);

    @Override
    protected AMD64EirCPU cpu() {
        return cpu;
    }

    @Override
    protected Value interpret(EirMethod eirMethod, Value... arguments) throws InvocationTargetException {
        final AMD64EirCPU savedCPU = cpu.save();
        try {
            return super.interpret(eirMethod, arguments);
        } finally {
            cpu = savedCPU;
        }
    }

    public void visit(EirPrologue prologue) {
        final int frameSize = prologue.eirMethod().frameSize();
        if (frameSize != 0) {
            cpu.writeFramePointer(cpu.readFramePointer().minus(frameSize));
        }
    }

    public void visit(EirEpilogue epilogue) {
        final int frameSize = epilogue.eirMethod().frameSize();
        if (frameSize != 0) {
            cpu.writeFramePointer(cpu.readFramePointer().plus(frameSize));
        }
    }

    public void visit(EirAssignment assignment) {
        switch (assignment.kind().asEnum) {
            case INT: {
                final int value = cpu.readInt(assignment.sourceOperand().location());
                cpu.writeInt(assignment.destinationOperand().location(), value);
                break;
            }
            case LONG:
            case WORD:
            case REFERENCE: {
                final Value value = cpu.read(assignment.sourceOperand().location());
                cpu.write(assignment.destinationOperand().location(), adjustToAssignmentType(value));
                break;
            }
            case FLOAT: {
                final float value = cpu.readFloat(assignment.sourceOperand().location());
                cpu.writeFloat(assignment.destinationOperand().location(), value);
                break;
            }
            case DOUBLE: {
                final double value = cpu.readDouble(assignment.sourceOperand().location());
                cpu.writeDouble(assignment.destinationOperand().location(), value);
                break;
            }
            default: {
                ProgramError.unknownCase();
                break;
            }
        }
    }

    public void visit(AMD64EirLoad load) {
        final Value value;
        final Value pointer = cpu.read(load.pointerOperand().location());
        if (pointer.kind().isWord) {
            // This must be a load from the stack
            assert load.indexOperand() == null;
            final int offset = load.offsetOperand() != null ? cpu.read(load.offsetOperand().location()).asInt() : 0;
            value = cpu.stack().read(pointer.asWord().asAddress().plus(offset));
        } else {
            final Value[] arguments = (load.indexOperand() != null) ? new Value[3] : new Value[2];
            arguments[0] = pointer;
            if (load.offsetOperand() != null) {
                arguments[1] = IntValue.from(cpu.readInt(load.offsetOperand().location()));
            } else {
                arguments[1] = IntValue.from(0);
            }
            if (load.indexOperand() != null) {
                arguments[2] = IntValue.from(cpu.readInt(load.indexOperand().location()));
            }
            value = pointerLoad(load.kind(), arguments);
        }
        switch (load.kind().asEnum) {
            case BYTE: {
                cpu.writeWord(load.destinationOperand().location(), Address.fromInt(value.toInt()));
                break;
            }
            case SHORT: {
                cpu.writeWord(load.destinationOperand().location(), Address.fromInt(value.unsignedToShort()));
                break;
            }
            case BOOLEAN:
            case CHAR:
            case INT: {
                cpu.writeWord(load.destinationOperand().location(), Address.fromInt(value.unsignedToInt()));
                break;
            }
            case LONG: {
                cpu.writeLong(load.destinationOperand().location(), value.asLong());
                break;
            }
            case FLOAT: {
                cpu.writeFloat(load.destinationOperand().location(), value.asFloat());
                break;
            }
            case DOUBLE: {
                cpu.writeDouble(load.destinationOperand().location(), value.asDouble());
                break;
            }
            case WORD:
            case REFERENCE: {
                cpu.write(load.destinationOperand().location(), value);
                break;
            }
            case VOID: {
                ProgramError.unexpected();
            }
        }
    }

    public void visit(AMD64EirStore store) {
        final Value pointer = cpu.read(store.pointerOperand().location());
        if (pointer.kind().isWord) {
            // This must be a store to the stack: don't type check the load if it's Word-type store
            final Value value = store.kind().isWord ?
                            cpu.read(store.valueOperand().location()) :
                            cpu.read(store.kind(), store.valueOperand().location());
            assert store.indexOperand() == null;
            final int offset = store.offsetOperand() != null ? cpu.read(store.offsetOperand().location()).asInt() : 0;
            cpu.stack().write(pointer.asWord().asAddress().plus(offset), value);
        } else {
            final Value value = cpu.read(store.kind(), store.valueOperand().location());
            final Value[] arguments = (store.indexOperand() != null) ? new Value[4] : new Value[3];
            arguments[0] = pointer;
            if (store.offsetOperand() != null) {
                arguments[1] = IntValue.from(cpu.readInt(store.offsetOperand().location()));
            } else {
                arguments[1] = IntValue.from(0);
            }
            if (store.indexOperand() != null) {
                arguments[2] = IntValue.from(cpu.readInt(store.indexOperand().location()));
                arguments[3] = value;
            } else {
                arguments[2] = value;
            }
            pointerStore(store.kind(), arguments);
        }
    }

    public void visit(AMD64EirCompareAndSwap compareAndSwap) {
        FatalError.unimplemented();
    }

    public void visit(AMD64EirInstruction.ADD_I32 instruction) {
        final int a = cpu.readInt(instruction.destinationOperand().location());
        final int b = cpu.readInt(instruction.sourceOperand().location());
        cpu.writeInt(instruction.destinationOperand().location(), a + b);
    }

    public void visit(ADD_I64 instruction) {
        final long a = cpu.readLong(instruction.destinationOperand().location());
        final long b = cpu.readLong(instruction.sourceOperand().location());
        cpu.writeLong(instruction.destinationOperand().location(), a + b);
    }

    public void visit(ADDSD instruction) {
        final double a = cpu.readDouble(instruction.destinationOperand().location());
        final double b = cpu.readDouble(instruction.sourceOperand().location());
        cpu.writeDouble(instruction.destinationOperand().location(), a + b);
    }

    public void visit(ADDSS instruction) {
        final float a = cpu.readFloat(instruction.destinationOperand().location());
        final float b = cpu.readFloat(instruction.sourceOperand().location());
        cpu.writeFloat(instruction.destinationOperand().location(), a + b);
    }

    public void visit(AND_I32 instruction) {
        final int a = cpu.readInt(instruction.destinationOperand().location());
        final int b = cpu.readInt(instruction.sourceOperand().location());
        cpu.writeInt(instruction.destinationOperand().location(), a & b);
    }

    public void visit(AND_I64 instruction) {
        final long a = cpu.readLong(instruction.destinationOperand().location());
        final long b = cpu.readLong(instruction.sourceOperand().location());
        cpu.writeLong(instruction.destinationOperand().location(), a & b);
    }

    public void visit(CDQ instruction) {
        assert instruction.destinationOperand().location() == RDX;
        assert instruction.sourceOperand().location() == RAX;

        final int eax = cpu.readInt(instruction.sourceOperand().location());
        final int edx = (eax < 0) ? -1 : 0;
        cpu.writeInt(instruction.destinationOperand().location(), edx);
    }

    public void visit(CMOVA_I32 instruction) {
        if (!cpu.test(CF) && !cpu.test(ZF)) {
            final int i = cpu.readInt(instruction.sourceOperand().location());
            cpu.writeInt(instruction.destinationOperand().location(), i);
        }
    }

    public void visit(CMOVB_I32 instruction) {
        if (cpu.test(CF)) {
            final int i = cpu.readInt(instruction.sourceOperand().location());
            cpu.writeInt(instruction.destinationOperand().location(), i);
        }
    }

    public void visit(CMOVE_I32 instruction) {
        if (cpu.test(ZF)) {
            final int i = cpu.readInt(instruction.sourceOperand().location());
            cpu.writeInt(instruction.destinationOperand().location(), i);
        }
    }

    public void visit(CMOVE_I64 instruction) {
        if (cpu.test(ZF)) {
            Word w = cpu.readWord(instruction.sourceOperand().location());
            cpu.writeWord(instruction.destinationOperand().location(), w);
        }
    }

    public void visit(CMOVAE_I32 instruction) {
        if (!cpu.test(CF)) {
            final int i = cpu.readInt(instruction.sourceOperand().location());
            cpu.writeInt(instruction.destinationOperand().location(), i);
        }
    }

    public void visit(CMOVL_I32 instruction) {
        if (cpu.test(OF) != cpu.test(SF)) {
            final int i = cpu.readInt(instruction.sourceOperand().location());
            cpu.writeInt(instruction.destinationOperand().location(), i);
        }
    }

    public void visit(CMOVBE_I32 instruction) {
        if (cpu.test(CF) || cpu.test(ZF)) {
            final int i = cpu.readInt(instruction.sourceOperand().location());
            cpu.writeInt(instruction.destinationOperand().location(), i);
        }
    }

    public void visit(CMOVP_I32 instruction) {
        if (cpu.test(PF)) {
            final int i = cpu.readInt(instruction.sourceOperand().location());
            cpu.writeInt(instruction.destinationOperand().location(), i);
        }
    }

    public void visit(CMP_I32 instruction) {
        final int a = cpu.readInt(instruction.destinationOperand().location());
        final int b = cpu.readInt(instruction.sourceOperand().location());

        cpu.set(ZF, a == b);
        cpu.set(CF, Address.fromInt(a).lessThan(Address.fromInt(b)));

        cpu.set(OF, ((a < 0) && (b >= 0) && (a - b >= 0)) || ((a >= 0) && (b < 0) && (a - b < 0)));
        cpu.set(SF, a >= b ? cpu.test(OF) : !cpu.test(OF));
    }

    public void visit(CMP_I64 instruction) {
        Value valueA = cpu.read(instruction.destinationOperand().location());
        Value valueB = cpu.read(instruction.sourceOperand().location());
        final long a;
        final long b;
        if (valueA.kind().isReference || valueB.kind().isReference) {
            if (valueA.kind() != Kind.REFERENCE) {
                ProgramError.check(valueA.toLong() == 0L);
                valueA = ReferenceValue.NULL;
            }
            if (valueB.kind() != Kind.REFERENCE) {
                ProgramError.check(valueB.toLong() == 0L);
                valueB = ReferenceValue.NULL;
            }
            if (valueA.asObject() == valueB.asObject()) {
                cpu.set(ZF, true);
                // It does not matter what the actual values here are as long as they are the same:
                a = 0L;
                b = 0L;
            } else {
                cpu.set(ZF, false);
                // It does not matter what the actual values here are as long as they differ:
                a = valueA.hashCode();
                b = ~a;
            }
        } else {
            a = valueA.toLong();
            b = valueB.toLong();
            cpu.set(ZF, a == b);
        }
        cpu.set(CF, Address.fromLong(a).lessThan(Address.fromLong(b)));

        cpu.set(OF, ((a < 0) && (b >= 0) && (a - b >= 0)) || ((a >= 0) && (b < 0) && (a - b < 0)));
        cpu.set(SF, a >= b ? cpu.test(OF) : !cpu.test(OF));
    }

    private boolean doubleCompare(AMD64XMMComparison comparison, double a, double b) {
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return comparison == AMD64XMMComparison.UNORDERED;
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
        final double a = cpu.readDouble(instruction.destinationOperand().location());
        final double b = cpu.readDouble(instruction.sourceOperand().location());

        final double result = doubleCompare(instruction.comparison(), a, b) ? SpecialBuiltin.longToDouble(-1L) : SpecialBuiltin.longToDouble(0L);
        cpu.writeDouble(instruction.destinationOperand().location(), result);
    }

    private boolean floatCompare(AMD64XMMComparison comparison, float a, float b) {
        if (Float.isNaN(a) || Float.isNaN(b)) {
            return comparison == AMD64XMMComparison.UNORDERED;
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
        final float a = cpu.readFloat(instruction.destinationOperand().location());
        final float b = cpu.readFloat(instruction.sourceOperand().location());

        final float result = floatCompare(instruction.comparison(), a, b) ? SpecialBuiltin.intToFloat(-1) : SpecialBuiltin.intToFloat(0);
        cpu.writeFloat(instruction.destinationOperand().location(), result);
    }

    public void visit(COMISD instruction) {
        final double a = cpu.readDouble(instruction.destinationOperand().location());
        final double b = cpu.readDouble(instruction.sourceOperand().location());

        if (Double.isNaN(a) || Double.isNaN(b)) {
            cpu.set(PF, true);
            cpu.set(ZF, true);
            cpu.set(CF, true);

        } else {
            cpu.set(PF, false);
            cpu.set(ZF, a == b);
            cpu.set(CF, a < b);
        }
        cpu.set(OF, false);
        cpu.set(SF, false);
    }

    public void visit(COMISS instruction) {
        final float a = cpu.readFloat(instruction.destinationOperand().location());
        final float b = cpu.readFloat(instruction.sourceOperand().location());

        if (Float.isNaN(a) || Float.isNaN(b)) {
            cpu.set(PF, true);
            cpu.set(ZF, true);
            cpu.set(CF, true);

        } else {
            cpu.set(PF, false);
            cpu.set(ZF, a == b);
            cpu.set(CF, a < b);
        }
        cpu.set(OF, false);
        cpu.set(SF, false);
    }

    public void visit(CQO instruction) {
        assert instruction.destinationOperand().location() == RDX;
        assert instruction.sourceOperand().location() == RAX;

        final long rax = cpu.readLong(instruction.sourceOperand().location());
        final long rdx = (rax < 0L) ? -1L : 0L;
        cpu.writeLong(instruction.destinationOperand().location(), rdx);
    }

    public void visit(CVTTSD2SI_I32 instruction) {
        final double d = cpu.readDouble(instruction.sourceOperand().location());
        cpu.writeInt(instruction.destinationOperand().location(), (int) d);
    }

    public void visit(CVTTSD2SI_I64 instruction) {
        final double d = cpu.readDouble(instruction.sourceOperand().location());
        cpu.writeLong(instruction.destinationOperand().location(), (long) d);
    }

    public void visit(CVTSD2SS instruction) {
        final double d = cpu.readDouble(instruction.sourceOperand().location());
        cpu.writeFloat(instruction.destinationOperand().location(), (float) d);
    }

    public void visit(CVTSI2SD_I32 instruction) {
        final int n = cpu.readInt(instruction.sourceOperand().location());
        cpu.writeDouble(instruction.destinationOperand().location(), n);
    }

    public void visit(CVTSI2SD_I64 instruction) {
        final long n = cpu.readInt(instruction.sourceOperand().location());
        cpu.writeDouble(instruction.destinationOperand().location(), n);
    }

    public void visit(CVTSI2SS_I32 instruction) {
        final int n = cpu.readInt(instruction.sourceOperand().location());
        cpu.writeFloat(instruction.destinationOperand().location(), n);
    }

    public void visit(CVTSI2SS_I64 instruction) {
        final long n = cpu.readInt(instruction.sourceOperand().location());
        cpu.writeFloat(instruction.destinationOperand().location(), n);
    }

    public void visit(CVTSS2SD instruction) {
        final float f = cpu.readFloat(instruction.sourceOperand().location());
        cpu.writeDouble(instruction.destinationOperand().location(), f);
    }

    public void visit(CVTTSS2SI_I32 instruction) {
        final float f = cpu.readFloat(instruction.sourceOperand().location());
        cpu.writeInt(instruction.destinationOperand().location(), (int) f);
    }

    public void visit(CVTTSS2SI_I64 instruction) {
        final float f = cpu.readFloat(instruction.sourceOperand().location());
        cpu.writeLong(instruction.destinationOperand().location(), (long) f);
    }

    public void visit(DEC_I64 instruction) {
        long n = cpu.readLong(instruction.operand().location());
        n--;
        cpu.writeLong(instruction.operand().location(), n);
    }

    private static final BigInteger MAX_ULONG64 = new BigInteger("FFFFFFFFFFFFFFFF", 16);

    public void visit(DIV_I64 instruction) {
        final long rd = cpu.readLong(RDX);
        final long dividend = cpu.readLong(RAX);
        if (((rd == 0L) != (dividend >= 0)) || ((rd == -1L) != (dividend < 0))) {
            ProgramError.unexpected("IDIV_I64: junk in RD");
        }
        final int divisor = cpu.readInt(instruction.divisor().location());
        if (dividend < 0) {
            final BigInteger unsignedDividend = BigInteger.valueOf(dividend).and(MAX_ULONG64);
            final BigInteger[] quotientAndRemainder = unsignedDividend.divideAndRemainder(BigInteger.valueOf(divisor));
            cpu.writeLong(RAX, quotientAndRemainder[0].longValue());
            cpu.writeLong(RDX, quotientAndRemainder[1].longValue());
        } else {
            cpu.writeLong(RAX, dividend / divisor);
            cpu.writeLong(RDX, dividend % divisor);
        }
    }

    public void visit(DIVSD instruction) {
        final double dividend = cpu.readDouble(instruction.destinationOperand().location());
        final double divisor = cpu.readDouble(instruction.sourceOperand().location());
        cpu.writeDouble(instruction.destinationOperand().location(), dividend / divisor);
    }

    public void visit(DIVSS instruction) {
        final float dividend = cpu.readFloat(instruction.destinationOperand().location());
        final float divisor = cpu.readFloat(instruction.sourceOperand().location());
        cpu.writeFloat(instruction.destinationOperand().location(), dividend / divisor);
    }

    public void visit(IDIV_I32 instruction) {
        final int divisor = cpu.readInt(instruction.divisor().location());
        long dividend = cpu.readInt(RDX);
        dividend <<= 32;
        dividend |= cpu.readInt(RAX);
        cpu.writeInt(RAX, (int) (dividend / divisor));
        cpu.writeInt(RDX, (int) (dividend % divisor));
    }

    public void visit(IDIV_I64 instruction) {
        final long rd = cpu.readLong(RDX);
        final long dividend = cpu.readLong(RAX);
        if (((rd == 0L) != (dividend >= 0)) || ((rd == -1L) != (dividend < 0))) {
            ProgramError.unexpected("IDIV_I64: junk in RD");
        }
        final int divisor = cpu.readInt(instruction.divisor().location());
        cpu.writeLong(RAX, dividend / divisor);
        cpu.writeLong(RDX, dividend % divisor);
    }

    public void visit(IMUL_I32 instruction) {
        final int a = cpu.readInt(instruction.destinationOperand().location());
        final int b = cpu.readInt(instruction.sourceOperand().location());
        cpu.writeInt(instruction.destinationOperand().location(), a * b);
    }

    public void visit(IMUL_I64 instruction) {
        final long a = cpu.readLong(instruction.destinationOperand().location());
        final long b = cpu.readLong(instruction.sourceOperand().location());
        cpu.writeLong(instruction.destinationOperand().location(), a * b);
    }

    public void visit(JMP instruction) {
        cpu.gotoBlock(instruction.target());
    }

    public void visit(JMP_indirect instruction) {
        final int serial = cpu.readWord(instruction.operand().location()).asAddress().toInt();
        for (EirBlock block : instruction.block().method().blocks()) {
            if (block.serial() == serial) {
                cpu.gotoBlock(block);
                return;
            }
        }
        ProgramError.unexpected("target block for JMP_indirect not found");
    }

    private void conditionalBranch(AMD64EirConditionalBranch instruction, boolean condition) {
        if (condition) {
            cpu.gotoBlock(instruction.target());
        } else {
            cpu.gotoBlock(instruction.next());
        }
    }

    public void visit(JA instruction) {
        conditionalBranch(instruction, !cpu.test(CF) && !cpu.test(ZF));
    }

    public void visit(JAE instruction) {
        conditionalBranch(instruction, !cpu.test(CF));
    }

    public void visit(JB instruction) {
        conditionalBranch(instruction, cpu.test(CF));
    }

    public void visit(JBE instruction) {
        conditionalBranch(instruction, cpu.test(CF) || cpu.test(ZF));
    }

    public void visit(JG instruction) {
        conditionalBranch(instruction, !cpu.test(ZF) && cpu.test(SF) == cpu.test(OF));
    }

    public void visit(JGE instruction) {
        conditionalBranch(instruction, cpu.test(SF) == cpu.test(OF));
    }

    public void visit(JL instruction) {
        conditionalBranch(instruction, cpu.test(SF) != cpu.test(OF));
    }

    public void visit(JLE instruction) {
        conditionalBranch(instruction, cpu.test(ZF) || (cpu.test(SF) != cpu.test(OF)));
    }

    public void visit(JNZ instruction) {
        conditionalBranch(instruction, !cpu.test(ZF));
    }

    public void visit(JZ instruction) {
        conditionalBranch(instruction, cpu.test(ZF));
    }

    public void visit(LEA_STACK_ADDRESS instruction) {
        final int sourceOffset = cpu.offset(instruction.sourceOperand().location().asStackSlot());
        cpu.write(instruction.destinationOperand().location(), new WordValue(cpu.readFramePointer().plus(sourceOffset)));
    }

    public void visit(STACK_ALLOCATE instruction) {
        int offset = frame().method().frameSize() - instruction.offset;
        EirStackSlot stackSlot = new EirStackSlot(Purpose.BLOCK, offset);
        final int sourceOffset = cpu.offset(stackSlot);
        cpu.write(instruction.operand().location(), new WordValue(cpu.readFramePointer().plus(sourceOffset)));
    }

    public void visit(LFENCE instruction) {
    }

    public void visit(MFENCE instruction) {
    }

    public void visit(PAUSE instruction) {
    }

    public void visit(MOVD_I32_F32 instruction) {
        final float f = cpu.readFloat(instruction.sourceOperand().location());
        cpu.writeInt(instruction.destinationOperand().location(), SpecialBuiltin.floatToInt(f));
    }

    public void visit(MOVD_I64_F64 instruction) {
        final double d = cpu.readDouble(instruction.sourceOperand().location());
        cpu.writeLong(instruction.destinationOperand().location(), SpecialBuiltin.doubleToLong(d));
    }

    public void visit(MOVD_F32_I32 instruction) {
        final int f = cpu.readInt(instruction.sourceOperand().location());
        cpu.writeFloat(instruction.destinationOperand().location(), SpecialBuiltin.intToFloat(f));
    }

    public void visit(MOVD_F64_I64 instruction) {
        final long d = cpu.readLong(instruction.sourceOperand().location());
        cpu.writeDouble(instruction.destinationOperand().location(), SpecialBuiltin.longToDouble(d));
    }

    public void visit(MOVSX_I8 instruction) {
        final byte b = cpu.readByte(instruction.sourceOperand().location());
        cpu.writeInt(instruction.destinationOperand().location(), b);
    }

    public void visit(MOVSX_I16 instruction) {
        final short s = cpu.readShort(instruction.sourceOperand().location());
        cpu.writeInt(instruction.destinationOperand().location(), s);
    }

    public void visit(MOVSXD instruction) {
        final int i = cpu.readInt(instruction.sourceOperand().location());
        cpu.writeLong(instruction.destinationOperand().location(), i);
    }

    public void visit(MOVZX_I16 instruction) {
        final short s = cpu.readShort(instruction.sourceOperand().location());
        cpu.writeInt(instruction.destinationOperand().location(), s & 0xffff);
    }

    public void visit(MOVZXD instruction) {
        final int i = cpu.readInt(instruction.sourceOperand().location());
        cpu.writeLong(instruction.destinationOperand().location(), i & 0xffffffffL);
    }

    public void visit(MULSD instruction) {
        final double a = cpu.readDouble(instruction.destinationOperand().location());
        final double b = cpu.readDouble(instruction.sourceOperand().location());
        cpu.writeDouble(instruction.destinationOperand().location(), a * b);
    }

    public void visit(MULSS instruction) {
        final float a = cpu.readFloat(instruction.destinationOperand().location());
        final float b = cpu.readFloat(instruction.sourceOperand().location());
        cpu.writeFloat(instruction.destinationOperand().location(), a * b);
    }

    public void visit(NEG_I32 instruction) {
        final int operand = cpu.readInt(instruction.operand().location());
        cpu.writeInt(instruction.operand().location(), -operand);
    }

    public void visit(NEG_I64 instruction) {
        final long operand = cpu.readLong(instruction.operand().location());
        cpu.writeLong(instruction.operand().location(), -operand);
    }

    public void visit(NOT_I32 instruction) {
        final int operand = cpu.readInt(instruction.operand().location());
        cpu.writeInt(instruction.operand().location(), ~operand);
    }

    public void visit(NOT_I64 instruction) {
        final long operand = cpu.readLong(instruction.operand().location());
        cpu.writeLong(instruction.operand().location(), ~operand);
    }

    public void visit(OR_I32 instruction) {
        final int a = cpu.readInt(instruction.destinationOperand().location());
        final int b = cpu.readInt(instruction.sourceOperand().location());
        cpu.writeInt(instruction.destinationOperand().location(), a | b);
    }

    public void visit(OR_I64 instruction) {
        final long a = cpu.readLong(instruction.destinationOperand().location());
        final long b = cpu.readLong(instruction.sourceOperand().location());
        cpu.writeLong(instruction.destinationOperand().location(), a | b);
    }

    public void visit(POP instruction) {
        final Value result = cpu.pop();
        cpu.write(instruction.destinationOperand().location(), result);
    }

    public void visit(PUSH instruction) {
        final Value value = cpu.read(instruction.sourceLocation());
        cpu.push(value);
    }

    public void visit(RET instruction) {
        ret();
    }

    public void visit(SAL_I32 instruction) {
        assert !(instruction.sourceOperand().location() instanceof AMD64EirRegister.General) || (instruction.sourceOperand().location() == RCX);
        final int number = cpu.readInt(instruction.destinationOperand().location());
        final int shift = cpu.readByte(instruction.sourceOperand().location());
        cpu.writeInt(instruction.destinationOperand().location(), number << shift);
    }

    public void visit(SAL_I64 instruction) {
        assert !(instruction.sourceOperand().location() instanceof AMD64EirRegister.General) || (instruction.sourceOperand().location() == RCX);
        final long number = cpu.readInt(instruction.destinationOperand().location());
        final int shift = cpu.readByte(instruction.sourceOperand().location());
        cpu.writeLong(instruction.destinationOperand().location(), number << shift);
    }

    public void visit(SAR_I32 instruction) {
        assert !(instruction.sourceOperand().location() instanceof AMD64EirRegister.General) || (instruction.sourceOperand().location() == RCX);
        final int number = cpu.readInt(instruction.destinationOperand().location());
        final int shift = cpu.readByte(instruction.sourceOperand().location());
        cpu.writeInt(instruction.destinationOperand().location(), number >> shift);
    }

    public void visit(SAR_I64 instruction) {
        assert !(instruction.sourceOperand().location() instanceof AMD64EirRegister.General) || (instruction.sourceOperand().location() == RCX);
        final long number = cpu.readLong(instruction.destinationOperand().location());
        final int shift = cpu.readByte(instruction.sourceOperand().location());
        cpu.writeLong(instruction.destinationOperand().location(), number >> shift);
    }

    private void setConditionally(AMD64EirUnaryOperation instruction, boolean condition) {
        cpu.writeLong(instruction.operand().location(), condition ? 1L : 0L);
    }

    public void visit(SETB instruction) {
        setConditionally(instruction, cpu.test(CF));
    }

    public void visit(SETBE instruction) {
        setConditionally(instruction, cpu.test(CF) || cpu.test(ZF));
    }

    public void visit(SETL instruction) {
        setConditionally(instruction, cpu.test(SF) != cpu.test(OF));
    }

    public void visit(SETNB instruction) {
        setConditionally(instruction, !cpu.test(CF));
    }

    public void visit(SETNBE instruction) {
        setConditionally(instruction, !cpu.test(CF) && !cpu.test(ZF));
    }

    public void visit(SETNP instruction) {
        setConditionally(instruction, !cpu.test(PF));
    }

    public void visit(SETP instruction) {
        setConditionally(instruction, cpu.test(PF));
    }

    public void visit(SETNLE instruction) {
        if (!cpu.test(ZF) && (cpu.test(SF) == cpu.test(OF))) {
            long n = cpu.readLong(instruction.operand().location());
            n |= 1;
            cpu.writeLong(instruction.operand().location(), n);
        }
    }

    public void visit(SFENCE instruction) {
    }

    public void visit(SHR_I32 instruction) {
        assert !(instruction.sourceOperand().location() instanceof AMD64EirRegister.General) || (instruction.sourceOperand().location() == RCX);
        final int number = cpu.readInt(instruction.destinationOperand().location());
        final int shift = cpu.readByte(instruction.sourceOperand().location());
        cpu.writeInt(instruction.destinationOperand().location(), number >>> shift);
    }

    public void visit(SHR_I64 instruction) {
        assert !(instruction.sourceOperand().location() instanceof AMD64EirRegister.General) || (instruction.sourceOperand().location() == RCX);
        final long number = cpu.readLong(instruction.destinationOperand().location());
        final int shift = cpu.readByte(instruction.sourceOperand().location());
        cpu.writeLong(instruction.destinationOperand().location(), number >>> shift);
    }

    public void visit(SUB_I32 instruction) {
        final int a = cpu.readInt(instruction.destinationOperand().location());
        final int b = cpu.readInt(instruction.sourceOperand().location());
        cpu.writeInt(instruction.destinationOperand().location(), a - b);
    }

    public void visit(SUB_I64 instruction) {
        final long a = cpu.readLong(instruction.destinationOperand().location());
        final long b = cpu.readLong(instruction.sourceOperand().location());
        cpu.writeLong(instruction.destinationOperand().location(), a - b);
    }

    public void visit(SUBSD instruction) {
        final double a = cpu.readDouble(instruction.destinationOperand().location());
        final double b = cpu.readDouble(instruction.sourceOperand().location());
        cpu.writeDouble(instruction.destinationOperand().location(), a - b);
    }

    public void visit(SUBSS instruction) {
        final float a = cpu.readFloat(instruction.destinationOperand().location());
        final float b = cpu.readFloat(instruction.sourceOperand().location());
        cpu.writeFloat(instruction.destinationOperand().location(), a - b);
    }

    public void visit(SWITCH_I32 instruction) {
        final int a = cpu.readInt(instruction.tag().location());

        for (int i = 0; i < instruction.matches().length; i++) {
            if (a == instruction.matches()[i].value().asInt()) {
                cpu.gotoBlock(instruction.targets[i]);
                return;
            }
        }

        cpu.gotoBlock(instruction.defaultTarget());
    }

    public void visit(XCHG instruction) {
        final Word a = cpu.readWord(instruction.destinationOperand().location());
        final Word b = cpu.readWord(instruction.sourceOperand().location());
        cpu.writeWord(instruction.destinationOperand().location(), b);
        cpu.writeWord(instruction.sourceOperand().location(), a);
    }

    public void visit(XOR_I32 instruction) {
        if (instruction.destinationOperand().location().equals(instruction.sourceOperand().location())) {
            // This is the x86 idiom for zeroing a location. It needs to be special-cased
            // in case the location is currently holding a reference as reading a
            // long from such a location will not work.
            cpu.writeInt(instruction.destinationOperand().location(), 0);
            return;
        }
        final int a = cpu.readInt(instruction.destinationOperand().location());
        final int b = cpu.readInt(instruction.sourceOperand().location());
        cpu.writeInt(instruction.destinationOperand().location(), a ^ b);
    }

    public void visit(XOR_I64 instruction) {
        if (instruction.destinationOperand().location().equals(instruction.sourceOperand().location())) {
            // This is the x86 idiom for zeroing a location. It needs to be special-cased
            // in case the location is currently holding a reference as reading a
            // long from such a location will not work.
            cpu.writeLong(instruction.destinationOperand().location(), 0L);
            return;
        }
        final long a = cpu.readLong(instruction.destinationOperand().location());
        final long b = cpu.readLong(instruction.sourceOperand().location());
        cpu.writeLong(instruction.destinationOperand().location(), a ^ b);
    }

    public void visit(XORPD instruction) {
        if (instruction.destinationOperand().location().equals(instruction.sourceOperand().location())) {
            // This is the x86 idiom for zeroing a location. It needs to be special-cased
            // in case the location is currently holding a reference as reading a
            // long from such a location will not work.
            cpu.writeDouble(instruction.destinationOperand().location(), 0D);
            return;
        }
        final long a = SpecialBuiltin.doubleToLong(cpu.readDouble(instruction.destinationOperand().location()));
        final long b = SpecialBuiltin.doubleToLong(cpu.readDouble(instruction.sourceOperand().location()));
        cpu.writeDouble(instruction.destinationOperand().location(), SpecialBuiltin.longToDouble(a ^ b));
    }

    public void visit(XORPS instruction) {
        if (instruction.destinationOperand().location().equals(instruction.sourceOperand().location())) {
            // This is the x86 idiom for zeroing a location. It needs to be special-cased
            // in case the location is currently holding a reference as reading a
            // long from such a location will not work.
            cpu.writeFloat(instruction.destinationOperand().location(), 0F);
            return;
        }
        final int a = SpecialBuiltin.floatToInt(cpu.readFloat(instruction.destinationOperand().location()));
        final int b = SpecialBuiltin.floatToInt(cpu.readFloat(instruction.sourceOperand().location()));
        cpu.writeFloat(instruction.destinationOperand().location(), SpecialBuiltin.intToFloat(a ^ b));
    }

    public void visit(ZERO instruction) {
        switch (instruction.kind().asEnum) {
            case INT:
            case LONG:
            case WORD:
                cpu.writeLong(instruction.operand().location(), 0L);
                break;
            case REFERENCE:
                cpu.write(instruction.operand().location(), ReferenceValue.NULL);
                break;
            case FLOAT:
                cpu.writeFloat(instruction.operand().location(), 0F);
                break;
            case DOUBLE:
                cpu.writeDouble(instruction.operand().location(), 0D);
                break;
            default:
                ProgramError.unknownCase();
                break;
        }
    }

    @Override
    public void visit(BSF_I64 instruction) {
        final long a = cpu.readWord(instruction.sourceOperand().location()).asAddress().toLong();
        if (a == 0) {
            cpu.set(ConditionFlag.ZF, true);
            return;
        }
        cpu.writeInt(instruction.destinationLocation(),  (int) Long.lowestOneBit(a));
        cpu.set(ConditionFlag.ZF, false);
    }

    @Override
    public void visit(BSR_I64 instruction) {
        long a = cpu.readWord(instruction.sourceOperand().location()).asAddress().toLong();
        if (a == 0) {
            cpu.set(ConditionFlag.ZF, true);
            return;
        }
        cpu.writeInt(instruction.destinationLocation(), (int) Long.highestOneBit(a));
        cpu.set(ConditionFlag.ZF, false);
    }
}
