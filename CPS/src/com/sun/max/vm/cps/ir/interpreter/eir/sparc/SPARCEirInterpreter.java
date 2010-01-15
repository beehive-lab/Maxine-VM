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
package com.sun.max.vm.cps.ir.interpreter.eir.sparc;
import static com.sun.max.vm.cps.ir.interpreter.eir.sparc.SPARCEirCPU.IntegerConditionFlag.*;

import com.sun.max.asm.sparc.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.EirStackSlot.*;
import com.sun.max.vm.cps.eir.sparc.*;
import com.sun.max.vm.cps.eir.sparc.SPARCEirInstruction.*;
import com.sun.max.vm.cps.ir.interpreter.eir.*;
import com.sun.max.vm.cps.ir.interpreter.eir.EirCPU.*;
import com.sun.max.vm.cps.ir.interpreter.eir.sparc.SPARCEirCPU.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 *  An interpreter for SPARC EIR representations of methods.
 *
 *
 * @author Laurent Daynes
 */
public class SPARCEirInterpreter extends EirInterpreter implements SPARCEirInstructionVisitor {

    class SPARCEirFrame extends EirFrame {
        final SPARCEirCPU.RegisterWindow registerWindow;

        SPARCEirFrame(EirFrame caller, EirMethod method) {
            super(caller, method);
            registerWindow = new SPARCEirCPU.RegisterWindow();
        }

        SPARCEirCPU.RegisterWindow registerWindow() {
            return registerWindow;
        }
    }

    class SPARCInitialEirFrame extends SPARCEirFrame {
        SPARCInitialEirFrame() {
            super(null, null);
        }

        @Override
        public EirABI abi() {
            return eirGenerator().eirABIsScheme().nativeABI;
        }
    }

    @Override
    protected EirFrame initialEirFrame() {
        return new SPARCInitialEirFrame();
    }

    private SPARCEirCPU cpu;

    @Override
    protected SPARCEirCPU cpu() {
        return cpu;
    }

    public SPARCEirInterpreter(EirGenerator eirGenerator) {
        super(eirGenerator);
        cpu = new SPARCEirCPU(this);
    }

    @Override
    protected EirLocation [] argumentLocations(EirMethod eirMethod) {
        if (!cpu().usesRegisterWindow()) {
            return eirMethod.parameterLocations();
        }

        final EirLocation [] argumentLocations = new EirLocation[eirMethod.parameterLocations().length];
        final int i0 = SPARCEirRegisters.GeneralPurpose.I0.ordinal;
        int index = 0;
        for (EirLocation location : eirMethod.parameterLocations()) {
            if (location instanceof SPARCEirRegisters.GeneralPurpose) {
                final SPARCEirRegisters.GeneralPurpose inRegister = (SPARCEirRegisters.GeneralPurpose) location;
                argumentLocations[index] = SPARCEirRegisters.GeneralPurpose.OUT_REGISTERS.get(inRegister.ordinal - i0);
            } else {
                argumentLocations[index] = location;
            }
            index++;
        }

        return argumentLocations;
    }

    /**
     * Returns the location of the receiver at the caller for the specified method.
     * Always O0 on SPARC.
     *
     * @param eirMethod
     * @return
     */
    @Override
    protected EirLocation receiverLocation(EirMethod eirMethod) {
        return SPARCEirRegisters.GeneralPurpose.O0;
    }

    @Override
    protected EirLocation returnedResultLocation(EirMethod eirMethod) {
        if (eirMethod.resultLocation() == SPARCEirRegisters.GeneralPurpose.I0) {
            return SPARCEirRegisters.GeneralPurpose.O0;
        }
        return eirMethod.resultLocation();
    }

    @Override
    protected InstructionAddress callAndLink(EirMethod eirMethod) {
        final InstructionAddress returnAddress = cpu().nextInstructionAddress();
        cpu().writeRegister(SPARCEirRegisters.GeneralPurpose.O7, ReferenceValue.from(returnAddress));
        return returnAddress;
    }

    public void visit(EirPrologue instruction) {
        if (!instruction.eirMethod().isTemplate()) {
            if (cpu().usesRegisterWindow()) {
                final int frameSize = instruction.eirMethod().frameSize();
                final Address fp = cpu().readStackPointer();
                final Address sp = fp.minus(frameSize);
                // Stack must be 16 byte align. FIXME: this is sparc v9
                assert sp.aligned(16).equals(sp);
                final SPARCEirFrame currentFrame = (SPARCEirFrame) frame();
                currentFrame.registerWindow().save(cpu());
                pushFrame(new SPARCEirFrame(currentFrame, instruction.eirMethod()));
                cpu().writeStackPointer(sp);
                assert cpu().readFramePointer().equals(fp);
            } else {
                FatalError.unimplemented();
            }
        }
    }

    public void visit(EirEpilogue instruction) {
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

    /**
     * Helper method for load conversion.
     * SPARC doesn't have floating-point to integer register (and vice-versa) moving instructions.
     * Instead, one must transit via memory and performs the integer/floating-point conversion in floating-point register.
     * The compiler uses temporary storage on the stack for moving data. This cannot be emulated in the EIR interpreter without some explicit
     * type conversion when loading from the stack. The conversion is only between types of equals width.
     *
     * @param value
     * @param loadKind
     * @return
     */
    private static Value toLoadKind(Value value, Kind loadKind) {
        if (loadKind == value.kind()) {
            return value;
        }
        switch (loadKind.asEnum) {
            case INT: {
                if (value.kind() == Kind.FLOAT) {
                    return IntValue.from((int) value.asFloat());
                }
                break;
            }
            case LONG: {
                if (value.kind() == Kind.DOUBLE) {
                    return LongValue.from((long) value.asDouble());
                }
                break;
            }
            case FLOAT: {
                if (value.kind() == Kind.INT) {
                    return FloatValue.from(value.asInt());
                }
                break;
            }
            case DOUBLE: {
                if (value.kind() == Kind.LONG) {
                    return DoubleValue.from(value.asLong());
                }
                break;
            }
            case BYTE:
            case SHORT:
            case BOOLEAN:
            case CHAR:
            case WORD:
            case REFERENCE:
            case VOID: {
                break;
            }
        }
        ProgramError.unexpected();
        return null;
    }

    public void visit(SPARCEirLoad load) {
        final Value value;
        final Value pointer = cpu.read(load.pointerOperand().location());
        if (pointer.kind() == Kind.WORD) {
            // This must be a load from the stack
            assert load.indexOperand() == null;
            final int offset = load.offsetOperand() != null ? cpu.read(load.offsetOperand().location()).asInt() : 0;
            value = toLoadKind(cpu.stack().read(pointer.asWord().asAddress().plus(offset)), load.kind);
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
            value = pointerLoad(load.kind, arguments);
        }
        switch (load.kind.asEnum) {
            case BYTE: {
                cpu.writeWord(load.destinationLocation(), Address.fromInt(value.toInt()));
                break;
            }
            case SHORT: {
                cpu.writeWord(load.destinationLocation(), Address.fromInt(value.unsignedToShort()));
                break;
            }
            case BOOLEAN:
            case CHAR:
            case INT: {
                cpu.writeWord(load.destinationLocation(), Address.fromInt(value.unsignedToInt()));
                break;
            }
            case LONG: {
                cpu.writeLong(load.destinationLocation(), value.asLong());
                break;
            }
            case FLOAT: {
                cpu.writeFloat(load.destinationLocation(), value.asFloat());
                break;
            }
            case DOUBLE: {
                cpu.writeDouble(load.destinationLocation(), value.asDouble());
                break;
            }
            case WORD:
            case REFERENCE: {
                cpu.write(load.destinationLocation(), value);
                break;
            }
            case VOID: {
                ProgramError.unexpected();
            }
        }
    }

    public void visit(SPARCEirStore store) {
        final Value pointer = cpu.read(store.pointerOperand().location());
        if (pointer.kind() == Kind.WORD) {
            // This must be a store to the stack: don't type check the load
            final Value value = store.kind == Kind.WORD ?
                            cpu.read(store.valueOperand().location()) :
                            cpu.read(store.kind, store.valueOperand().location());
            assert store.indexOperand() == null;
            final int offset = store.offsetOperand() != null ? cpu.read(store.offsetOperand().location()).asInt() : 0;
            cpu.stack().write(pointer.asWord().asAddress().plus(offset), value);
        } else {
            final Value value = cpu.read(store.kind, store.valueOperand().location());
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
            pointerStore(store.kind, arguments);
        }
    }

    public void visit(SPARCEirCompareAndSwap instruction) {
        FatalError.unimplemented();
    }

    public void visit(ADD_I32 instruction) {
        final int a = cpu.readInt(instruction.leftLocation());
        final int b = cpu.readInt(instruction.rightLocation());
        cpu.writeInt(instruction.destinationLocation(), a + b);
    }

    public void visit(ADD_I64 instruction) {
        final long a = cpu.readLong(instruction.leftLocation());
        final long b = cpu.readLong(instruction.rightLocation());
        cpu.writeLong(instruction.destinationLocation(), a + b);
    }

    public void visit(AND_I32 instruction) {
        final int a = cpu.readInt(instruction.leftLocation());
        final int b = cpu.readInt(instruction.rightLocation());
        cpu.writeInt(instruction.destinationLocation(), a & b);
    }

    public void visit(AND_I64 instruction) {
        final long a = cpu.readLong(instruction.leftLocation());
        final long b = cpu.readLong(instruction.rightLocation());
        cpu.writeLong(instruction.destinationLocation(), a & b);
    }

    private boolean testE(ICCOperand cc) {
        return cpu.test(Z, cc);
    }
    private boolean testNE(ICCOperand cc) {
        return !cpu.test(Z, cc);
    }

    private boolean testL(ICCOperand cc) {
        return  cpu.test(N, cc) ^ cpu.test(V, cc);
    }

    private boolean testLE(ICCOperand cc) {
        return testE(cc)  || testL(cc);
    }

    private boolean testGE(ICCOperand cc) {
        return !testL(cc);
    }

    private boolean testG(ICCOperand cc) {
        return !testLE(cc);
    }

    private boolean testLU(ICCOperand cc) {
        return cpu.test(C, cc);
    }

    private boolean testLEU(ICCOperand cc) {
        return cpu.test(C, cc) || cpu.test(Z, cc);
    }

    private boolean testGEU(ICCOperand cc) {
        return !cpu.test(C, cc);
    }

    private boolean testGU(ICCOperand cc) {
        return !testLEU(cc);
    }

    private boolean testE(FCCOperand fcc) {
        return cpu.get(fcc) == FCCValue.E;
    }

    private boolean testNE(FCCOperand fcc) {
        final FCCValue value = cpu.get(fcc);
        return value == FCCValue.L ||  value == FCCValue.G ||  value == FCCValue.U;
    }

    private boolean testG(FCCOperand fcc) {
        return cpu.get(fcc) == FCCValue.G;
    }

    private boolean testGE(FCCOperand fcc) {
        final FCCValue value = cpu.get(fcc);
        return value == FCCValue.G ||  value == FCCValue.E;
    }

    private boolean testL(FCCOperand fcc) {
        return cpu.get(fcc) == FCCValue.L;
    }

    private boolean testLE(FCCOperand fcc) {
        final FCCValue value = cpu.get(fcc);
        return value == FCCValue.L ||  value == FCCValue.E;
    }

    public void visit(MOVNE instruction) {
        final ConditionCodeRegister cc = instruction.testedConditionCode();

        if (!testNE((ICCOperand) cc)) {
            return;
        }

        final int i = cpu.readInt(instruction.sourceLocation());
        cpu.writeInt(instruction.destinationLocation(), i);
    }

    public void visit(MOVFNE instruction) {
        final ConditionCodeRegister cc = instruction.testedConditionCode();
        assert cc instanceof FCCOperand;
        if (!testNE((FCCOperand) cc)) {
            return;

        }
        final int i = cpu.readInt(instruction.sourceLocation());
        cpu.writeInt(instruction.destinationLocation(), i);
    }

    public void visit(MOVE instruction) {
        final ConditionCodeRegister cc = instruction.testedConditionCode();
        if (!testE((ICCOperand) cc)) {
            return;
        }

        final int i = cpu.readInt(instruction.sourceLocation());
        cpu.writeInt(instruction.destinationLocation(), i);
    }

    public void visit(MOVFE instruction) {
        final ConditionCodeRegister cc = instruction.testedConditionCode();
        assert cc instanceof FCCOperand;
        if (!testE((FCCOperand) cc)) {
            return;
        }

        final int i = cpu.readInt(instruction.sourceLocation());
        cpu.writeInt(instruction.destinationLocation(), i);
        // TODO Auto-generated method stub
    }

    public void visit(MOVG instruction) {
        final ConditionCodeRegister cc = instruction.testedConditionCode();
        if (!testG((ICCOperand) cc)) {
            return;
        }
        final int i = cpu.readInt(instruction.sourceLocation());
        cpu.writeInt(instruction.destinationLocation(), i);
    }

    public void visit(MOVCC instruction) {
        final ConditionCodeRegister cc = instruction.testedConditionCode();
        //test for unsigned result
        if (!testGEU((ICCOperand) cc)) {
            return;
        }
        final int i = cpu.readInt(instruction.sourceLocation());
        cpu.writeInt(instruction.destinationLocation(), i);
    }

    public void visit(MOVGU instruction) {
        final ConditionCodeRegister cc = instruction.testedConditionCode();
        //test for unsigned result
        if (!testGU((ICCOperand) cc)) {
            return;
        }
        final int i = cpu.readInt(instruction.sourceLocation());
        cpu.writeInt(instruction.destinationLocation(), i);

    }

    public void visit(MOVL instruction) {
        final ConditionCodeRegister cc = instruction.testedConditionCode();
        if (!testL((ICCOperand) cc)) {
            return;
        }

        final int i = cpu.readInt(instruction.sourceLocation());
        cpu.writeInt(instruction.destinationLocation(), i);
    }

    public void visit(MOVFG instruction) {
        final ConditionCodeRegister cc = instruction.testedConditionCode();
        assert cc instanceof FCCOperand;
        if (!testG((FCCOperand) cc)) {
            return;
        }
        final int i = cpu.readInt(instruction.sourceLocation());
        cpu.writeInt(instruction.destinationLocation(), i);
    }

    public void visit(MOVCS instruction) {
        final ConditionCodeRegister cc = instruction.testedConditionCode();
        //tests for unsigned result
        if (!testLU((ICCOperand) cc)) {
            return;
        }
        final int i = cpu.readInt(instruction.sourceLocation());
        cpu.writeInt(instruction.destinationLocation(), i);

    }

    public void visit(MOVLEU instruction) {
        final ConditionCodeRegister cc = instruction.testedConditionCode();
        if (!testLEU((ICCOperand) cc)) {
            return;
        }
        final int i = cpu.readInt(instruction.sourceLocation());
        cpu.writeInt(instruction.destinationLocation(), i);

    }

    public void visit(MOVFL instruction) {
        final ConditionCodeRegister cc = instruction.testedConditionCode();
        assert cc instanceof FCCOperand;
        if (!testL((FCCOperand) cc)) {
            return;
        }
        final int i = cpu.readInt(instruction.sourceLocation());
        cpu.writeInt(instruction.destinationLocation(), i);

    }

    public void visit(CMP_I32 instruction) {

        final int a = cpu.readInt(instruction.leftLocation());
        final int b = cpu.readInt(instruction.rightLocation());

        cpu.set(C, ICCOperand.ICC, false);
        cpu.set(Z, ICCOperand.ICC, false);
        cpu.set(V, ICCOperand.ICC, false);
        cpu.set(N, ICCOperand.ICC, false);

        final int result = a - b;
        cpu.set(Z, ICCOperand.ICC, a == b);
        cpu.set(N, ICCOperand.ICC, result < 0);
        cpu.set(C, ICCOperand.ICC, Address.fromInt(a).lessThan(Address.fromInt(b)));

        final boolean operandsDifferInSign = (a >= 0 && b < 0) || (a < 0 && b >= 0);
        final boolean firstOperandDiffersInSignFromResult = (a >= 0 && result < 0) || (a < 0 && result > 0);
        cpu.set(V, ICCOperand.ICC, operandsDifferInSign && firstOperandDiffersInSignFromResult);
    }

    public void visit(CMP_I64 instruction) {
        Value valueA = cpu.read(instruction.leftLocation());
        Value valueB = cpu.read(instruction.rightLocation());
        final long a;
        final long b;
        if (valueA.kind() == Kind.REFERENCE || valueB.kind() == Kind.REFERENCE) {
            if (valueA.kind() != Kind.REFERENCE) {
                ProgramError.check(valueA.toLong() == 0L);
                valueA = ReferenceValue.NULL;
            }
            if (valueB.kind() != Kind.REFERENCE) {
                ProgramError.check(valueB.toLong() == 0L);
                valueB = ReferenceValue.NULL;
            }
            // Use arbitrary value for reference a.
            a = valueA.hashCode();

            if (valueA.asObject() == valueB.asObject()) {
                b = a;
            } else {
                b = ~a;
            }
        } else {
            a = valueA.toLong();
            b = valueB.toLong();
        }
        cpu.set(Z, ICCOperand.XCC, a == b);
        cpu.set(V, ICCOperand.XCC, ((a < 0) && (b >= 0) && (a - b >= 0)) || ((a >= 0) && (b < 0) && (a - b < 0)));
        cpu.set(C, ICCOperand.XCC, Address.fromLong(a).lessThan(Address.fromLong(b)));
        cpu.set(N, ICCOperand.XCC, (a - b) < 0);
    }

    public void visit(DIV_I32 instruction) {
        final int a = cpu.readInt(instruction.leftLocation());
        final int b = cpu.readInt(instruction.rightLocation());
        cpu.writeInt(instruction.destinationLocation(), a / b);
    }

    public void visit(DIV_I64 instruction) {
        final long a = cpu.readLong(instruction.leftLocation());
        final long b = cpu.readLong(instruction.rightLocation());
        cpu.writeLong(instruction.destinationLocation(), a / b);
    }

    public void visit(BA instruction) {
        cpu.gotoBlock(instruction.target());
    }

    private void conditionalBranch(SPARCEirConditionalBranch instruction, boolean condition) {
        if (condition) {
            cpu.gotoBlock(instruction.target());
        } else {
            cpu.gotoBlock(instruction.next());
        }
    }

    public void visit(BRZ instruction) {
        conditionalBranch(instruction, cpu.read(instruction.testedOperandLocation()).isZero());
    }

    public void visit(BRNZ instruction) {
        conditionalBranch(instruction, !cpu.read(instruction.testedOperandLocation()).isZero());
    }

    public void visit(BRLZ instruction) {
        conditionalBranch(instruction, cpu.read(instruction.testedOperandLocation()).asLong() < 0);
    }

    public void visit(BRLEZ instruction) {
        conditionalBranch(instruction, cpu.read(instruction.testedOperandLocation()).asLong() <= 0);
    }

    public void visit(BRGEZ instruction) {
        conditionalBranch(instruction, cpu.read(instruction.testedOperandLocation()).asLong() >= 0);
    }

    public void visit(BRGZ instruction) {
        conditionalBranch(instruction, cpu.read(instruction.testedOperandLocation()).asLong() > 0);
    }

    public void visit(BE instruction) {
        conditionalBranch(instruction, testE(instruction.conditionCode()));
    }

    public void visit(BNE instruction) {
        conditionalBranch(instruction, testNE(instruction.conditionCode()));
    }

    public void visit(BL instruction) {
        conditionalBranch(instruction, testL(instruction.conditionCode()));
    }

    public void visit(BLE instruction) {
        conditionalBranch(instruction, testLE(instruction.conditionCode()));
    }

    public void visit(BGE instruction) {
        conditionalBranch(instruction, testGE(instruction.conditionCode()));
    }

    public void visit(BG instruction) {
        conditionalBranch(instruction, testG(instruction.conditionCode()));
    }

    public void visit(BLU instruction) {
        conditionalBranch(instruction,  testLU(instruction.conditionCode()));
    }

    public void visit(BLEU instruction) {
        conditionalBranch(instruction, testLEU(instruction.conditionCode()));
    }

    public void visit(BGEU instruction) {
        conditionalBranch(instruction, testGEU(instruction.conditionCode()));
    }

    public void visit(BGU instruction) {
        conditionalBranch(instruction, testGU(instruction.conditionCode()));
    }

    public void visit(FLAT_RETURN instruction) {
        // TODO Auto-generated method stub

    }

    public void visit(FADD_S instruction) {
        final float a = cpu.readFloat(instruction.leftLocation());
        final float b = cpu.readFloat(instruction.rightLocation());
        cpu.writeFloat(instruction.destinationLocation(), a + b);
    }

    public void visit(FADD_D instruction) {
        final double a = cpu.readDouble(instruction.leftLocation());
        final double b = cpu.readDouble(instruction.rightLocation());
        cpu.writeDouble(instruction.destinationLocation(), a + b);
    }

    public void visit(FCMP_S instruction) {
        final float a = cpu.readFloat(instruction.leftLocation());
        final float b = cpu.readFloat(instruction.rightLocation());
        final FCCOperand fcc = instruction.selectedConditionCode();
        if (a == b) {
            cpu.set(fcc, FCCValue.E);
        } else if (a < b) {
            cpu.set(fcc, FCCValue.L);
        } else if (a > b) {
            cpu.set(fcc, FCCValue.G);
        } else {
            assert Float.isNaN(a) || Float.isNaN(b);
            cpu.set(fcc, FCCValue.U);
        }
    }

    public void visit(FCMP_D instruction) {
        final double a = cpu.readDouble(instruction.leftLocation());
        final double b = cpu.readDouble(instruction.rightLocation());
        final FCCOperand fcc = instruction.selectedConditionCode();
        if (a == b) {
            cpu.set(fcc, FCCValue.E);
        } else if (a < b) {
            cpu.set(fcc, FCCValue.L);
        } else if (a > b) {
            cpu.set(fcc, FCCValue.G);
        } else {
            assert Double.isNaN(a) || Double.isNaN(b);
            cpu.set(fcc, FCCValue.U);
        }
    }

    public void visit(FDIV_S instruction) {
        final float a = cpu.readFloat(instruction.leftLocation());
        final float b = cpu.readFloat(instruction.rightLocation());
        cpu.writeFloat(instruction.destinationLocation(), a / b);
    }

    public void visit(FDIV_D instruction) {
        final double a = cpu.readDouble(instruction.leftLocation());
        final double b = cpu.readDouble(instruction.rightLocation());
        cpu.writeDouble(instruction.destinationLocation(), a / b);
    }

    public void visit(FMUL_S instruction) {
        final float a = cpu.readFloat(instruction.leftLocation());
        final float b = cpu.readFloat(instruction.rightLocation());
        cpu.writeFloat(instruction.destinationLocation(), a * b);
    }

    public void visit(FMUL_D instruction) {
        final double a = cpu.readDouble(instruction.leftLocation());
        final double b = cpu.readDouble(instruction.rightLocation());
        cpu.writeDouble(instruction.destinationLocation(), a * b);
    }

    public void visit(FNEG_S instruction) {
        final float a = cpu.readFloat(instruction.operandLocation());
        cpu.writeFloat(instruction.operandLocation(), -a);

    }

    public void visit(FNEG_D instruction) {
        final double a = cpu.readDouble(instruction.operandLocation());
        cpu.writeDouble(instruction.operandLocation(), -a);
    }

    public void visit(FSUB_S instruction) {
        final float a = cpu.readFloat(instruction.leftLocation());
        final float b = cpu.readFloat(instruction.rightLocation());
        cpu.writeFloat(instruction.destinationLocation(), a - b);
    }

    public void visit(FSUB_D instruction) {
        final double a = cpu.readDouble(instruction.leftLocation());
        final double b = cpu.readDouble(instruction.rightLocation());
        cpu.writeDouble(instruction.destinationLocation(), a - b);
    }

    public void visit(FSTOD instruction) {
        final float f = cpu.readFloat(instruction.sourceLocation());
        final double d = f;
        cpu.writeDouble(instruction.destinationLocation(), d);
    }

    public void visit(FDTOS instruction) {
        final double d = cpu.readDouble(instruction.sourceLocation());
        final float f = (float) d;
        cpu.writeFloat(instruction.destinationLocation(), f);
    }

    public void visit(FITOS instruction) {
        final int i = cpu.readInt(instruction.sourceLocation());
        final float f = i;
        cpu.writeFloat(instruction.destinationLocation(), f);
    }

    public void visit(FITOD instruction) {
        final int i = cpu.readInt(instruction.sourceLocation());
        final double d = i;
        cpu.writeDouble(instruction.destinationLocation(), d);
    }

    public void visit(FXTOS instruction) {
        final long l = cpu.readLong(instruction.sourceLocation());
        final float f = l;
        cpu.writeFloat(instruction.destinationLocation(), f);
    }

    public void visit(FXTOD instruction) {
        final long l = cpu.readLong(instruction.sourceLocation());
        final double d = l;
        cpu.writeDouble(instruction.destinationLocation(), d);
    }

    public void visit(FSTOI instruction) {
        // Float-value is converted into an integer value and stored in a float register.
        final float f = cpu.readFloat(instruction.sourceLocation());
        final int i = (int) f;
        cpu.writeFloat(instruction.destinationLocation(), i);
    }

    public void visit(FDTOI instruction) {
        // Double-value is converted into an integer value and stored in a float register.
        final double d = cpu.readDouble(instruction.sourceLocation());
        final int i = (int) d;
        cpu.writeFloat(instruction.destinationLocation(), i);
    }

    public void visit(FSTOX instruction) {
        // Float-value is converted first into an long value and stored in a double-precision float register.
        final float f = cpu.readFloat(instruction.sourceLocation());
        final long l = (long) f;
        cpu.writeDouble(instruction.destinationLocation(), l);
    }

    public void visit(FDTOX instruction) {
        // Double-value is converted first into an long value and stored in a double-precision float register.
        final double d = cpu.readDouble(instruction.sourceLocation());
        final long l = (long) d;
        cpu.writeDouble(instruction.destinationLocation(), l);
    }

    public void visit(FLUSHW instruction) {
    }

    public void visit(JMP_indirect instruction) {
        ProgramError.unexpected("indirect jump not implemented at EIR level - it should only occur during exception dispatching at target level");
    }

    public void visit(MEMBAR instruction) {
    }

    public void visit(SET_STACK_ADDRESS instruction) {
        final int sourceOffset = cpu.offset(instruction.sourceOperand().location().asStackSlot());
        cpu.write(instruction.destinationOperand().location(), new WordValue(cpu.readFramePointer().plus(sourceOffset)));
    }

    public void visit(STACK_ALLOCATE instruction) {
        EirStackSlot stackSlot = new EirStackSlot(Purpose.BLOCK, instruction.offset);
        final int sourceOffset = cpu.offset(stackSlot);
        cpu.write(instruction.operand().location(), new WordValue(cpu.readFramePointer().plus(sourceOffset)));
    }

    public void visit(MOV_I32 instruction) {
        final int a = cpu.readInt(instruction.sourceLocation());
        cpu.writeInt(instruction.destinationLocation(), a);
    }

    public void visit(MOV_I64 instruction) {
        final long a = cpu.readInt(instruction.sourceLocation());
        cpu.writeLong(instruction.destinationLocation(), a);
    }

    public void visit(MUL_I32 instruction) {
        final int a = cpu.readInt(instruction.leftLocation());
        final int b = cpu.readInt(instruction.rightLocation());
        cpu.writeInt(instruction.destinationLocation(), a * b);
    }

    public void visit(MUL_I64 instruction) {
        final long a = cpu.readLong(instruction.leftLocation());
        final long b = cpu.readLong(instruction.rightLocation());
        cpu.writeLong(instruction.destinationLocation(), a * b);
    }

    public void visit(NEG_I32 instruction) {
        final int a = cpu.readInt(instruction.operandLocation());
        cpu.writeInt(instruction.operandLocation(), -a);
    }

    public void visit(NEG_I64 instruction) {
        final long a = cpu.readLong(instruction.operandLocation());
        cpu.writeLong(instruction.operandLocation(), -a);
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
        final int a = cpu.readInt(instruction.leftLocation());
        final int b = cpu.readInt(instruction.rightLocation());
        cpu.writeInt(instruction.destinationLocation(), a | b);
    }

    public void visit(OR_I64 instruction) {
        final long a = cpu.readLong(instruction.leftLocation());
        final long b = cpu.readLong(instruction.rightLocation());
        cpu.writeLong(instruction.destinationLocation(), a | b);
    }

    public void visit(RDPC instruction) {
        cpu.write(instruction.operand().location(), new WordValue(Address.fromLong(cpu.currentInstructionAddress().index())));
    }

    public void visit(RET instruction) {
        if (cpu().usesRegisterWindow()) {
            ret((InstructionAddress) cpu().read(SPARCEirRegisters.GeneralPurpose.I7).asObject());
            popFrame();
            final SPARCEirFrame currentFrame = (SPARCEirFrame) frame();
            currentFrame.registerWindow().restore(cpu());
        } else {
            FatalError.unimplemented();
        }
    }

    public void visit(SET_I32 instruction) {
        assert instruction.immediateOperand().isConstant();
        final int a = instruction.immediateOperand().value().asInt();
        cpu.writeInt(instruction.operandLocation(), a);
    }

    public void visit(SLL_I32 instruction) {
        final int number = cpu.readInt(instruction.leftLocation());
        final int shift = cpu.readByte(instruction.rightLocation());
        cpu.writeInt(instruction.destinationLocation(), number << shift);
    }

    public void visit(SLL_I64 instruction) {
        final long number = cpu.readInt(instruction.leftLocation());
        final int shift = cpu.readByte(instruction.rightLocation());
        cpu.writeLong(instruction.destinationLocation(), number << shift);
    }

    public void visit(SRA_I32 instruction) {
        final int number = cpu.readInt(instruction.leftLocation());
        final int shift = cpu.readByte(instruction.rightLocation());
        cpu.writeInt(instruction.destinationLocation(), number >> shift);
    }

    public void visit(SRA_I64 instruction) {
        final long number = cpu.readInt(instruction.leftLocation());
        final int shift = cpu.readByte(instruction.rightLocation());
        cpu.writeLong(instruction.destinationLocation(), number >> shift);
    }

    public void visit(SRL_I32 instruction) {
        final int number = cpu.readInt(instruction.leftLocation());
        final int shift = cpu.readByte(instruction.rightLocation());
        cpu.writeInt(instruction.destinationLocation(), number >>> shift);
    }

    public void visit(SRL_I64 instruction) {
        final long number = cpu.readInt(instruction.leftLocation());
        final int shift = cpu.readByte(instruction.rightLocation());
        cpu.writeLong(instruction.destinationLocation(), number >>> shift);
    }

    public void visit(SUB_I32 instruction) {
        final int a = cpu.readInt(instruction.leftLocation());
        final int b = cpu.readInt(instruction.rightLocation());
        cpu.writeInt(instruction.destinationLocation(), a - b);
    }

    public void visit(SUB_I64 instruction) {
        final long a = cpu.readLong(instruction.leftLocation());
        final long b = cpu.readLong(instruction.rightLocation());
        cpu.writeLong(instruction.destinationLocation(), a - b);
    }

    public void visit(SWITCH_I32 instruction) {
        final int a = cpu.readInt(instruction.tag().location());

        for (int i = 0; i < instruction.matches().length; i++) {
            if (a == instruction.matches() [i].value().asInt()) {
                cpu.gotoBlock(instruction.targets[i]);
                return;
            }
        }

        cpu.gotoBlock(instruction.defaultTarget());
    }

    public void visit(XOR_I32 instruction) {
        final int a = cpu.readInt(instruction.leftLocation());
        final int b = cpu.readInt(instruction.rightLocation());
        cpu.writeInt(instruction.destinationLocation(), a ^ b);

    }

    public void visit(XOR_I64 instruction) {
        final long a = cpu.readLong(instruction.leftLocation());
        final long b = cpu.readLong(instruction.rightLocation());
        cpu.writeLong(instruction.destinationLocation(), a ^ b);
    }

    public void visit(ZERO instruction) {
        switch (instruction.kind.asEnum) {
            case INT:
            case LONG:
            case WORD:
            case REFERENCE:
                cpu.writeLong(instruction.operand().location(), 0L);
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
}
