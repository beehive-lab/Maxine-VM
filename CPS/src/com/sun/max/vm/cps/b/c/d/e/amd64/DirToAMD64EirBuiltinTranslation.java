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
package com.sun.max.vm.cps.b.c.d.e.amd64;

import com.sun.max.collect.*;
import com.sun.max.lang.*;
import com.sun.max.memory.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.builtin.*;
import com.sun.max.vm.compiler.builtin.AddressBuiltin.*;
import com.sun.max.vm.compiler.builtin.IEEE754Builtin.*;
import com.sun.max.vm.compiler.builtin.JavaBuiltin.*;
import com.sun.max.vm.compiler.builtin.PointerAtomicBuiltin.*;
import com.sun.max.vm.compiler.builtin.PointerLoadBuiltin.*;
import com.sun.max.vm.compiler.builtin.PointerStoreBuiltin.*;
import com.sun.max.vm.compiler.builtin.SpecialBuiltin.*;
import com.sun.max.vm.cps.dir.*;
import com.sun.max.vm.cps.dir.eir.*;
import com.sun.max.vm.cps.eir.*;
import com.sun.max.vm.cps.eir.amd64.*;
import com.sun.max.vm.cps.eir.amd64.AMD64EirInstruction.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
class DirToAMD64EirBuiltinTranslation extends DirToEirBuiltinTranslation {

    DirToAMD64EirBuiltinTranslation(DirToAMD64EirInstructionTranslation instructionTranslation, DirJavaFrameDescriptor javaFrameDescriptor) {
        super(instructionTranslation, javaFrameDescriptor);
    }

    private abstract class Unary {
        protected abstract EirInstruction createOperation(EirValue operand);

        protected Unary(Kind kind, DirValue dirResult, DirValue[] dirArguments) {
            final EirValue result = dirToEirValue(dirResult);
            final EirValue argument = dirToEirValue(dirArguments[0]);
            assign(kind, result, argument);
            addInstruction(createOperation(result));
        }
    }

    @Override
    public void visitIntNegated(IntNegated builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Unary(Kind.INT, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue operand) {
                return new NEG_I32(eirBlock(), operand);
            }
        };
    }

    private static final FloatValue XMM_FLOAT_SIGN_MASK = FloatValue.from(SpecialBuiltin.intToFloat(0x80000000));

    @Override
    public void visitFloatNegated(FloatNegated builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Unary(Kind.FLOAT, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue operand) {
                return new XORPS(eirBlock(), operand, createEirConstant(XMM_FLOAT_SIGN_MASK));
            }
        };
    }

    @Override
    public void visitLongNegated(LongNegated builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Unary(Kind.LONG, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue operand) {
                return new NEG_I64(eirBlock(), operand);
            }
        };
    }

    private static final DoubleValue XMM_DOUBLE_SIGN_MASK = DoubleValue.from(SpecialBuiltin.longToDouble(0x8000000000000000L));

    @Override
    public void visitDoubleNegated(DoubleNegated builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Unary(Kind.DOUBLE, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue operand) {
                return new XORPD(eirBlock(), operand, createEirConstant(XMM_DOUBLE_SIGN_MASK));
            }
        };
    }

    private abstract class Binary {
        protected abstract AMD64EirOperation createOperation(EirValue destination, EirValue source);

        private Binary(Kind destinationKind, Kind sourceKind, DirValue dirResult, DirValue[] dirArguments) {
            final EirValue result = dirToEirValue(dirResult);
            final EirValue destination = dirToEirValue(dirArguments[0]);
            assign(destinationKind, result, destination);
            final EirValue source = dirToEirValue(dirArguments[1]);
            final AMD64EirOperation operation = createOperation(result, source);
            addInstruction(operation);
        }

        private Binary(Kind kind, DirValue dirResult, DirValue[] dirArguments) {
            this(kind, kind, dirResult, dirArguments);
        }
    }

    @Override
    public void visitIntPlus(IntPlus builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.INT, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new AMD64EirInstruction.ADD_I32(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitFloatPlus(FloatPlus builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.FLOAT, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new ADDSS(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitLongPlus(LongPlus builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.LONG, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new ADD_I64(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitDoublePlus(DoublePlus builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.DOUBLE, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new ADDSD(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitIntMinus(IntMinus builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.INT, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new SUB_I32(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitFloatMinus(FloatMinus builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.FLOAT, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new SUBSS(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitLongMinus(LongMinus builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.LONG, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new SUB_I64(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitDoubleMinus(DoubleMinus builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.DOUBLE, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new SUBSD(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitIntTimes(IntTimes builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.INT, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new IMUL_I32(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitFloatTimes(FloatTimes builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.FLOAT, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new MULSS(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitLongTimes(LongTimes builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.LONG, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new IMUL_I64(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitDoubleTimes(DoubleTimes builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.DOUBLE, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new MULSD(eirBlock(), destination, source);
            }
        };
    }

    private abstract class Division {
        protected abstract void createOperation(EirValue rd, EirValue ra, EirValue divisor);

        protected Division(Kind dividendKind, Kind divisorKind, DirValue dirResult, DirValue[] dirArguments) {
            final EirValue result = dirToEirValue(dirResult);
            final EirValue dividend = dirToEirValue(dirArguments[0]);
            assign(dividendKind, result, dividend);
            final EirValue divisor = dirToEirValue(dirArguments[1]);
            final EirVariable rd = createEirVariable(dividendKind);
            createOperation(rd, result, divisor);
        }

        protected Division(Kind kind, DirValue dirResult, DirValue[] dirArguments) {
            this(kind, kind, dirResult, dirArguments);
        }
    }

    @Override
    public void visitIntDivided(IntDivided builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Division(Kind.INT, dirResult, dirArguments) {
            @Override
            protected void createOperation(EirValue rd, EirValue ra, EirValue divisor) {
                addInstruction(new CDQ(eirBlock(), rd, ra));
                addInstruction(new IDIV_I32(eirBlock(), rd, ra, divisor));
            }
        };
    }

    @Override
    public void visitLongDivided(LongDivided builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Division(Kind.LONG, dirResult, dirArguments) {
            @Override
            protected void createOperation(EirValue rd, EirValue ra, EirValue divisor) {
                addInstruction(new CQO(eirBlock(), rd, ra));
                addInstruction(new IDIV_I64(eirBlock(), rd, ra, divisor));
            }
        };
    }

    @Override
    public void visitFloatDivided(FloatDivided builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.FLOAT, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new DIVSS(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitDoubleDivided(DoubleDivided builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.DOUBLE, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new DIVSD(eirBlock(), destination, source);
            }
        };
    }

    private abstract class Remainder {
        protected abstract void createOperation(EirValue rd, EirValue ra, EirValue divisor);

        private void run(Kind dividendKind, Kind divisorKind, DirValue dirResult, DirValue[] dirArguments) {
            final EirValue result = dirToEirValue(dirResult);
            final EirValue dividend = dirToEirValue(dirArguments[0]);
            final EirVariable disposableDividend = createEirVariable(Kind.LONG);
            assign(Kind.LONG, disposableDividend, dividend);
            final EirValue divisor = dirToEirValue(dirArguments[1]);
            createOperation(result, disposableDividend, divisor);
        }

        protected Remainder(Kind dividendKind, Kind divisorKind, DirValue dirResult, DirValue[] dirArguments) {
            run(dividendKind, divisorKind, dirResult, dirArguments);
        }

        protected Remainder(Kind kind, DirValue dirResult, DirValue[] dirArguments) {
            run(kind, kind, dirResult, dirArguments);
        }
    }

    @Override
    public void visitIntRemainder(IntRemainder builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Remainder(Kind.INT, dirResult, dirArguments) {
            @Override
            protected void createOperation(EirValue rd, EirValue ra, EirValue divisor) {
                addInstruction(new CDQ(eirBlock(), rd, ra));
                addInstruction(new IDIV_I32(eirBlock(), rd, ra, divisor));
            }
        };
    }

    @Override
    public void visitLongRemainder(LongRemainder builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Remainder(Kind.LONG, dirResult, dirArguments) {
            @Override
            protected void createOperation(EirValue rd, EirValue ra, EirValue divisor) {
                addInstruction(new CQO(eirBlock(), rd, ra));
                addInstruction(new IDIV_I64(eirBlock(), rd, ra, divisor));
            }
        };
    }

    @Override
    public void visitIntShiftedLeft(IntShiftedLeft builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.INT, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new SAL_I32(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitLongShiftedLeft(LongShiftedLeft builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.LONG, Kind.INT, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new SAL_I64(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitIntSignedShiftedRight(IntSignedShiftedRight builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.INT, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new SAR_I32(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitLongSignedShiftedRight(LongSignedShiftedRight builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.LONG, Kind.INT, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new SAR_I64(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitIntUnsignedShiftedRight(IntUnsignedShiftedRight builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.INT, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new SHR_I32(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitLongUnsignedShiftedRight(LongUnsignedShiftedRight builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.LONG, Kind.INT, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new SHR_I64(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitIntNot(IntNot builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Unary(Kind.INT, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue operand) {
                return new NOT_I32(eirBlock(), operand);
            }
        };
    }

    @Override
    public void visitLongNot(LongNot builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Unary(Kind.LONG, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue operand) {
                return new NOT_I64(eirBlock(), operand);
            }
        };
    }

    @Override
    public void visitIntAnd(IntAnd builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.INT, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new AND_I32(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitLongAnd(LongAnd builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.LONG, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new AND_I64(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitIntOr(IntOr builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.INT, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new OR_I32(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitLongOr(LongOr builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.LONG, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new OR_I64(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitIntXor(IntXor builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.INT, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new XOR_I32(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitLongXor(LongXor builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Binary(Kind.LONG, dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation createOperation(EirValue destination, EirValue source) {
                return new XOR_I64(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitLongCompare(LongCompare builtin, DirValue dirResult, DirValue[] dirArguments) {
        final EirValue result = dirToEirValue(dirResult);
        final EirValue a = dirToEirValue(dirArguments[0]);
        final EirValue b = dirToEirValue(dirArguments[1]);
        final EirVariable flag = createEirVariable(Kind.INT);

        addInstruction(new XOR_I64(eirBlock(), result, result));
        addInstruction(new XOR_I64(eirBlock(), flag, flag));
        addInstruction(new CMP_I64(eirBlock(), a, b));
        addInstruction(new SETNLE(eirBlock(), result));
        addInstruction(new SETL(eirBlock(), flag));
        addInstruction(new SUB_I64(eirBlock(), result, flag));
        addInstruction(new MOVSX_I8(eirBlock(), result, result));
    }

    @Override
    public void visitFloatCompareL(FloatCompareL builtin, DirValue dirResult, DirValue[] dirArguments) {
        final EirValue result = dirToEirValue(dirResult);
        final EirValue a = dirToEirValue(dirArguments[0]);
        final EirValue b = dirToEirValue(dirArguments[1]);
        final EirVariable flag = createEirVariable(Kind.INT);
                                                                     // a,b:    0,1    0,0     1,0    0,nan
        addInstruction(new COMISS(eirBlock(), a, b));                //
        addInstruction(new SETB(eirBlock(), flag));                  // flag:    1      0       0      1
        addInstruction(new SETNBE(eirBlock(), result));              // result:  0      0       1      0
        addInstruction(new SUB_I64(eirBlock(), result, flag));       // result: -1      0       1     -1
        addInstruction(new MOVSX_I8(eirBlock(), result, result));
    }

    @Override
    public void visitFloatCompareG(FloatCompareG builtin, DirValue dirResult, DirValue[] dirArguments) {
        final EirValue result = dirToEirValue(dirResult);
        final EirValue a = dirToEirValue(dirArguments[0]);
        final EirValue b = dirToEirValue(dirArguments[1]);
        final EirVariable flag = createEirVariable(Kind.INT);
        // TODO: is there a way to compute this using only two temporaries?
        final EirVariable nan = createEirVariable(Kind.INT);
        // this code computes result = (isGreater + 2 * isNan - isLessThan)
        addInstruction(new COMISS(eirBlock(), a, b));                //
        addInstruction(new SETB(eirBlock(), flag));                  // flag:    1      0       0      1
        addInstruction(new SETNBE(eirBlock(), result));              // result:  0      0       1      0
        addInstruction(new SETP(eirBlock(), nan));                   // flag:    0      0       0      1
        addInstruction(new ADD_I32(eirBlock(), result, nan));        // flag:    0      0       0      1
        addInstruction(new ADD_I32(eirBlock(), result, nan));        // flag:    0      0       0      1
        addInstruction(new SUB_I32(eirBlock(), result, flag));       // result: -1      0       1     -1
        addInstruction(new MOVSX_I8(eirBlock(), result, result));
    }

    @Override
    public void visitDoubleCompareL(DoubleCompareL builtin, DirValue dirResult, DirValue[] dirArguments) {
        final EirValue result = dirToEirValue(dirResult);
        final EirValue a = dirToEirValue(dirArguments[0]);
        final EirValue b = dirToEirValue(dirArguments[1]);
        final EirVariable flag = createEirVariable(Kind.INT);
                                                                     // a,b:    0,1    0,0     1,0    0,nan
        addInstruction(new COMISD(eirBlock(), a, b));                //
        addInstruction(new SETB(eirBlock(), flag));                  // flag:    1      0       0      1
        addInstruction(new SETNBE(eirBlock(), result));              // result:  0      0       1      0
        addInstruction(new SUB_I64(eirBlock(), result, flag));       // result: -1      0       1     -1
        addInstruction(new MOVSX_I8(eirBlock(), result, result));
    }

    @Override
    public void visitDoubleCompareG(DoubleCompareG builtin, DirValue dirResult, DirValue[] dirArguments) {
        final EirValue result = dirToEirValue(dirResult);
        final EirValue a = dirToEirValue(dirArguments[0]);
        final EirValue b = dirToEirValue(dirArguments[1]);
        final EirVariable flag = createEirVariable(Kind.INT);
        final EirVariable nan = createEirVariable(Kind.INT);
        // this code computes result = (isGreater + 2 * isNan - isLessThan)
        addInstruction(new COMISD(eirBlock(), a, b));                //
        addInstruction(new SETB(eirBlock(), flag));                  // flag:    1      0       0      1
        addInstruction(new SETNBE(eirBlock(), result));              // result:  0      0       1      0
        addInstruction(new SETP(eirBlock(), nan));                   // flag:    0      0       0      1
        addInstruction(new ADD_I32(eirBlock(), result, nan));        // flag:    0      0       0      1
        addInstruction(new ADD_I32(eirBlock(), result, nan));        // flag:    0      0       0      1
        addInstruction(new SUB_I32(eirBlock(), result, flag));       // result: -1      0       1     -1
        addInstruction(new MOVSX_I8(eirBlock(), result, result));
    }

    private abstract class Conversion {
        protected abstract AMD64EirOperation operation(EirValue destination, EirValue source);

        protected Conversion(DirValue dirResult, DirValue[] dirArguments) {
            final EirValue result = dirToEirValue(dirResult);
            final EirValue argument = dirToEirValue(dirArguments[0]);
            addInstruction(operation(result, argument));
        }
    }

    @Override
    public void visitConvertByteToInt(ConvertByteToInt builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Conversion(dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation operation(EirValue destination, EirValue source) {
                return new MOVSX_I8(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitConvertCharToInt(ConvertCharToInt builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Conversion(dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation operation(EirValue destination, EirValue source) {
                return new MOVZX_I16(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitConvertShortToInt(ConvertShortToInt builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Conversion(dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation operation(EirValue destination, EirValue source) {
                return new MOVSX_I16(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitConvertIntToByte(ConvertIntToByte builtin, DirValue dirResult, DirValue[] dirArguments) {
        visitConvertByteToInt(ConvertByteToInt.BUILTIN, dirResult, dirArguments);
    }

    @Override
    public void visitConvertIntToChar(ConvertIntToChar builtin, DirValue dirResult, DirValue[] dirArguments) {
        visitConvertCharToInt(ConvertCharToInt.BUILTIN, dirResult, dirArguments);
    }

    @Override
    public void visitConvertIntToShort(ConvertIntToShort builtin, DirValue dirResult, DirValue[] dirArguments) {
        visitConvertShortToInt(ConvertShortToInt.BUILTIN, dirResult, dirArguments);
    }

    @Override
    public void visitConvertIntToFloat(ConvertIntToFloat builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Conversion(dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation operation(EirValue destination, EirValue source) {
                return new CVTSI2SS_I32(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitConvertIntToLong(ConvertIntToLong builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Conversion(dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation operation(EirValue destination, EirValue source) {
                return new MOVSXD(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitConvertIntToDouble(ConvertIntToDouble builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Conversion(dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation operation(EirValue destination, EirValue source) {
                return new CVTSI2SD_I32(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitConvertFloatToInt(ConvertFloatToInt builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Conversion(dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation operation(EirValue destination, EirValue source) {
                return new CVTTSS2SI_I32(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitConvertFloatToLong(ConvertFloatToLong builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Conversion(dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation operation(EirValue destination, EirValue source) {
                return new CVTTSS2SI_I64(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitConvertFloatToDouble(ConvertFloatToDouble builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Conversion(dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation operation(EirValue destination, EirValue source) {
                return new CVTSS2SD(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitConvertLongToInt(ConvertLongToInt builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Conversion(dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation operation(EirValue destination, EirValue source) {
                return new MOVZXD(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitConvertLongToFloat(ConvertLongToFloat builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Conversion(dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation operation(EirValue destination, EirValue source) {
                return new CVTSI2SS_I64(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitConvertLongToDouble(ConvertLongToDouble builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Conversion(dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation operation(EirValue destination, EirValue source) {
                return new CVTSI2SD_I64(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitConvertDoubleToInt(ConvertDoubleToInt builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Conversion(dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation operation(EirValue destination, EirValue source) {
                return new CVTTSD2SI_I32(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitConvertDoubleToFloat(ConvertDoubleToFloat builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Conversion(dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation operation(EirValue destination, EirValue source) {
                return new CVTSD2SS(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitConvertDoubleToLong(ConvertDoubleToLong builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Conversion(dirResult, dirArguments) {
            @Override
            protected AMD64EirOperation operation(EirValue destination, EirValue source) {
                return new CVTTSD2SI_I64(eirBlock(), destination, source);
            }
        };
    }

    @Override
    public void visitLessEqual(LessEqual builtin, DirValue dirResult, DirValue[] dirArguments) {
        final EirValue result = dirToEirValue(dirResult);
        final EirValue a = dirToEirValue(dirArguments[0]);
        final EirValue b = dirToEirValue(dirArguments[1]);
        final EirValue one = createEirVariable(Kind.INT);

        addInstruction(new ZERO(eirBlock(), Kind.WORD, result));
        assign(Kind.INT, one, createEirConstant(IntValue.ONE));
        addInstruction(new CMP_I64(eirBlock(), a, b));
        addInstruction(new CMOVBE_I32(eirBlock(), result, one));
    }

    @Override
    public void visitLessThan(LessThan builtin, DirValue dirResult, DirValue[] dirArguments) {
        final EirValue result = dirToEirValue(dirResult);
        final EirValue a = dirToEirValue(dirArguments[0]);
        final EirValue b = dirToEirValue(dirArguments[1]);
        final EirValue one = createEirVariable(Kind.INT);

        addInstruction(new ZERO(eirBlock(), Kind.WORD, result));
        assign(Kind.INT, one, createEirConstant(IntValue.ONE));
        addInstruction(new CMP_I64(eirBlock(), a, b));
        addInstruction(new CMOVB_I32(eirBlock(), result, one));
    }

    @Override
    public void visitGreaterEqual(GreaterEqual builtin, DirValue dirResult, DirValue[] dirArguments) {
        final EirValue result = dirToEirValue(dirResult);
        final EirValue a = dirToEirValue(dirArguments[0]);
        final EirValue b = dirToEirValue(dirArguments[1]);
        final EirValue one = createEirVariable(Kind.INT);

        addInstruction(new ZERO(eirBlock(), Kind.WORD, result));
        assign(Kind.INT, one, createEirConstant(IntValue.ONE));
        addInstruction(new CMP_I64(eirBlock(), a, b));
        addInstruction(new CMOVAE_I32(eirBlock(), result, one));
    }

    @Override
    public void visitGreaterThan(GreaterThan builtin, DirValue dirResult, DirValue[] dirArguments) {
        final EirValue result = dirToEirValue(dirResult);
        final EirValue a = dirToEirValue(dirArguments[0]);
        final EirValue b = dirToEirValue(dirArguments[1]);
        final EirValue one = createEirVariable(Kind.INT);

        addInstruction(new ZERO(eirBlock(), Kind.WORD, result));
        assign(Kind.INT, one, createEirConstant(IntValue.ONE));
        addInstruction(new CMP_I64(eirBlock(), a, b));
        addInstruction(new CMOVA_I32(eirBlock(), result, one));
    }

    @Override
    public void visitDividedByAddress(DividedByAddress builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Division(Kind.LONG, dirResult, dirArguments) {
            @Override
            protected void createOperation(EirValue rd, EirValue ra, EirValue divisor) {
                instructionTranslation().assignZero(Kind.LONG, rd);
                addInstruction(new DIV_I64(eirBlock(), rd, ra, divisor));
            }
        };
    }

    @Override
    public void visitDividedByInt(DividedByInt builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Division(Kind.LONG, Kind.INT, dirResult, dirArguments) {
            @Override
            protected void createOperation(EirValue rd, EirValue ra, EirValue divisor) {
                instructionTranslation().assignZero(Kind.LONG, rd);
                final EirVariable zeroExtendedDivisor = createEirVariable(Kind.LONG);
                addInstruction(new MOVZXD(eirBlock(), zeroExtendedDivisor, divisor));
                addInstruction(new DIV_I64(eirBlock(), rd, ra, zeroExtendedDivisor));
            }
        };
    }

    @Override
    public void visitRemainderByAddress(RemainderByAddress builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Remainder(Kind.LONG, dirResult, dirArguments) {
            @Override
            protected void createOperation(EirValue rd, EirValue ra, EirValue divisor) {
                instructionTranslation().assignZero(Kind.LONG, rd);
                addInstruction(new DIV_I64(eirBlock(), rd, ra, divisor));
            }
        };
    }

    @Override
    public void visitRemainderByInt(RemainderByInt builtin, DirValue dirResult, DirValue[] dirArguments) {
        new Remainder(Kind.LONG, Kind.INT, dirResult, dirArguments) {
            @Override
            protected void createOperation(EirValue rd, EirValue ra, EirValue divisor) {
                instructionTranslation().assignZero(Kind.LONG, rd);
                final EirVariable zeroExtendedDivisor = createEirVariable(Kind.LONG);
                addInstruction(new MOVZXD(eirBlock(), zeroExtendedDivisor, divisor));
                addInstruction(new DIV_I64(eirBlock(), rd, ra, zeroExtendedDivisor));
            }
        };
    }

    private void read(Kind kind, final Kind offsetKind, DirValue dirResult, DirValue[] dirArguments) {
        final EirValue pointer;
        DirValue dirPointer = dirArguments[0];
        if (dirPointer instanceof DirMethodValue) {
            final EirValue constant = methodTranslation().makeEirConstant(dirPointer.value());
            pointer = createEirVariable(constant.kind());
            assign(constant.kind(), pointer, constant);
        } else {
            pointer = dirToEirValue(dirPointer);
        }

        final EirValue result = dirToEirValue(dirResult);
        final DirValue dirOffset = dirArguments[1];
        final AMD64EirLoad loadInstruction = dirOffset.isZeroConstant() ? new AMD64EirLoad(eirBlock(), kind, result, pointer) :
                                                                          new AMD64EirLoad(eirBlock(), kind, result, pointer, offsetKind, dirToEirValue(dirOffset));
        addInstruction(loadInstruction);
    }

    private void get(Kind kind, DirValue dirResult, DirValue[] dirArguments) {
        final EirValue result = dirToEirValue(dirResult);
        final EirValue pointer = dirToEirValue(dirArguments[0]);

        final DirValue dirDisplacement = dirArguments[1];
        final DirValue dirIndex = dirArguments[2];

        if (dirIndex.isConstant() && dirDisplacement.isConstant()) {
            final DirConstant indexConstant = (DirConstant) dirIndex;
            final DirConstant displacementConstant = (DirConstant) dirDisplacement;

            final long offset = (indexConstant.value().toInt() * kind.width.numberOfBytes) + displacementConstant.value().toInt();

            if (offset > Integer.MIN_VALUE && offset < Integer.MAX_VALUE) {
                if (offset == 0) {
                    addInstruction(new AMD64EirLoad(eirBlock(), kind, result, pointer));
                } else {
                    final AMD64EirLoad loadInstruction = new AMD64EirLoad(eirBlock(), kind, result, pointer, Kind.INT, createEirConstant(IntValue.from((int) offset)));
                    addInstruction(loadInstruction);
                }
                return;
            }
        }
        if (dirDisplacement.isConstant()) {
            if (dirDisplacement.value().isZero()) {
                addInstruction(new AMD64EirLoad(eirBlock(), kind, result, pointer, dirToEirValue(dirIndex)));
            } else {
                addInstruction(new AMD64EirLoad(eirBlock(), kind, result, pointer, dirToEirValue(dirDisplacement), dirToEirValue(dirIndex)));
            }
        } else {
            final EirVariable p = createEirVariable(pointer.kind());
            assign(pointer.kind(), p, pointer);
            addInstruction(new ADD_I64(eirBlock(), p, dirToEirValue(dirDisplacement)));
            final AMD64EirLoad load = new AMD64EirLoad(eirBlock(), kind, result, p, dirToEirValue(dirIndex));
            addInstruction(load);
        }
    }

    @Override
    public void visitReadByte(ReadByte builtin, DirValue dirResult, DirValue[] dirArguments) {
        read(Kind.BYTE, Kind.LONG, dirResult, dirArguments);
    }

    @Override
    public void visitReadByteAtIntOffset(ReadByteAtIntOffset builtin, DirValue dirResult, DirValue[] dirArguments) {
        read(Kind.BYTE, Kind.INT, dirResult, dirArguments);
    }

    @Override
    public void visitGetByte(GetByte builtin, DirValue dirResult, DirValue[] dirArguments) {
        get(Kind.BYTE, dirResult, dirArguments);
    }

    @Override
    public void visitReadShort(ReadShort builtin, DirValue dirResult, DirValue[] dirArguments) {
        read(Kind.SHORT, Kind.LONG, dirResult, dirArguments);
    }

    @Override
    public void visitReadShortAtIntOffset(ReadShortAtIntOffset builtin, DirValue dirResult, DirValue[] dirArguments) {
        read(Kind.SHORT, Kind.INT, dirResult, dirArguments);
    }

    @Override
    public void visitGetShort(GetShort builtin, DirValue dirResult, DirValue[] dirArguments) {
        get(Kind.SHORT, dirResult, dirArguments);
    }

    @Override
    public void visitReadChar(ReadChar builtin, DirValue dirResult, DirValue[] dirArguments) {
        read(Kind.CHAR, Kind.LONG, dirResult, dirArguments);
    }

    @Override
    public void visitReadCharAtIntOffset(ReadCharAtIntOffset builtin, DirValue dirResult, DirValue[] dirArguments) {
        read(Kind.CHAR, Kind.INT, dirResult, dirArguments);
    }

    @Override
    public void visitGetChar(GetChar builtin, DirValue dirResult, DirValue[] dirArguments) {
        get(Kind.CHAR, dirResult, dirArguments);
    }

    @Override
    public void visitReadInt(ReadInt builtin, DirValue dirResult, DirValue[] dirArguments) {
        read(Kind.INT, Kind.LONG, dirResult, dirArguments);
    }

    @Override
    public void visitReadIntAtIntOffset(ReadIntAtIntOffset builtin, DirValue dirResult, DirValue[] dirArguments) {
        read(Kind.INT, Kind.INT, dirResult, dirArguments);
    }

    @Override
    public void visitGetInt(GetInt builtin, DirValue dirResult, DirValue[] dirArguments) {
        get(Kind.INT, dirResult, dirArguments);
    }

    @Override
    public void visitReadFloat(ReadFloat builtin, DirValue dirResult, DirValue[] dirArguments) {
        read(Kind.FLOAT, Kind.LONG, dirResult, dirArguments);
    }

    @Override
    public void visitReadFloatAtIntOffset(ReadFloatAtIntOffset builtin, DirValue dirResult, DirValue[] dirArguments) {
        read(Kind.FLOAT, Kind.INT, dirResult, dirArguments);
    }

    @Override
    public void visitGetFloat(GetFloat builtin, DirValue dirResult, DirValue[] dirArguments) {
        get(Kind.FLOAT, dirResult, dirArguments);
    }

    @Override
    public void visitReadLong(ReadLong builtin, DirValue dirResult, DirValue[] dirArguments) {
        read(Kind.LONG, Kind.LONG, dirResult, dirArguments);
    }

    @Override
    public void visitReadLongAtIntOffset(ReadLongAtIntOffset builtin, DirValue dirResult, DirValue[] dirArguments) {
        read(Kind.LONG, Kind.INT, dirResult, dirArguments);
    }

    @Override
    public void visitGetLong(GetLong builtin, DirValue dirResult, DirValue[] dirArguments) {
        get(Kind.LONG, dirResult, dirArguments);
    }

    @Override
    public void visitReadDouble(ReadDouble builtin, DirValue dirResult, DirValue[] dirArguments) {
        read(Kind.DOUBLE, Kind.LONG, dirResult, dirArguments);
    }

    @Override
    public void visitReadDoubleAtIntOffset(ReadDoubleAtIntOffset builtin, DirValue dirResult, DirValue[] dirArguments) {
        read(Kind.DOUBLE, Kind.INT, dirResult, dirArguments);
    }

    @Override
    public void visitGetDouble(GetDouble builtin, DirValue dirResult, DirValue[] dirArguments) {
        get(Kind.DOUBLE, dirResult, dirArguments);
    }

    @Override
    public void visitReadWord(ReadWord builtin, DirValue dirResult, DirValue[] dirArguments) {
        read(Kind.WORD, Kind.LONG, dirResult, dirArguments);
    }

    @Override
    public void visitReadWordAtIntOffset(ReadWordAtIntOffset builtin, DirValue dirResult, DirValue[] dirArguments) {
        read(Kind.WORD, Kind.INT, dirResult, dirArguments);
    }

    @Override
    public void visitGetWord(GetWord builtin, DirValue dirResult, DirValue[] dirArguments) {
        get(Kind.WORD, dirResult, dirArguments);
    }

    @Override
    public void visitReadReference(ReadReference builtin, DirValue dirResult, DirValue[] dirArguments) {
        read(Kind.REFERENCE, Kind.LONG, dirResult, dirArguments);
    }

    @Override
    public void visitReadReferenceAtIntOffset(ReadReferenceAtIntOffset builtin, DirValue dirResult, DirValue[] dirArguments) {
        read(Kind.REFERENCE, Kind.INT, dirResult, dirArguments);
    }

    @Override
    public void visitGetReference(GetReference builtin, DirValue dirResult, DirValue[] dirArguments) {
        get(Kind.REFERENCE, dirResult, dirArguments);
    }

    private void write(Kind kind, Kind offsetKind, DirValue[] dirArguments) {
        DirValue dirPointer = dirArguments[0];
        final EirValue pointer;
        if (dirPointer instanceof DirMethodValue) {
            final EirValue constant = methodTranslation().makeEirConstant(dirPointer.value());
            pointer = createEirVariable(constant.kind());
            assign(constant.kind(), pointer, constant);
        } else {
            pointer = dirToEirValue(dirPointer);
        }
        final DirValue dirOffset = dirArguments[1];
        final EirValue value = dirToEirValue(dirArguments[2]);

        final AMD64EirStore storeInstruction = dirOffset.isZeroConstant() ? new AMD64EirStore(eirBlock(), kind, value, pointer) :
                                                                            new AMD64EirStore(eirBlock(), kind, value, pointer, offsetKind, dirToEirValue(dirOffset));

        addInstruction(storeInstruction);
    }

    private void set(Kind kind, DirValue[] dirArguments) {
        final EirValue pointer = dirToEirValue(dirArguments[0]);
        final DirValue dirDisplacement = dirArguments[1];
        final DirValue dirIndex = dirArguments[2];
        final EirValue value = dirToEirValue(dirArguments[3]);

        if (dirIndex.isConstant() && dirDisplacement.isConstant()) {
            final DirConstant indexConstant = (DirConstant) dirIndex;
            final DirConstant displacementConstant = (DirConstant) dirDisplacement;

            final long offset = (indexConstant.value().toInt() * kind.width.numberOfBytes) + displacementConstant.value().toInt();

            if (offset > Integer.MIN_VALUE && offset < Integer.MAX_VALUE) {
                if (offset == 0) {
                    addInstruction(new AMD64EirStore(eirBlock(), kind, value, pointer));
                } else {
                    addInstruction(new AMD64EirStore(eirBlock(), kind, value, pointer, Kind.INT, createEirConstant(IntValue.from((int) offset))));
                }
                return;
            }
        }
        if (dirDisplacement.isConstant()) {
            if (dirDisplacement.value().isZero()) {
                addInstruction(new AMD64EirStore(eirBlock(), kind, value, pointer, dirToEirValue(dirIndex)));
            } else {
                addInstruction(new AMD64EirStore(eirBlock(), kind, value, pointer, dirToEirValue(dirDisplacement), dirToEirValue(dirIndex)));
            }
        } else {
            final EirVariable p = createEirVariable(pointer.kind());
            assign(pointer.kind(), p, pointer);
            addInstruction(new ADD_I64(eirBlock(), p, dirToEirValue(dirDisplacement)));
            addInstruction(new AMD64EirStore(eirBlock(), kind, value, p, dirToEirValue(dirIndex)));
        }
    }

    @Override
    public void visitWriteByte(WriteByte builtin, DirValue dirResult, DirValue[] dirArguments) {
        write(Kind.BYTE, Kind.LONG, dirArguments);
    }

    @Override
    public void visitWriteByteAtIntOffset(WriteByteAtIntOffset builtin, DirValue dirResult, DirValue[] dirArguments) {
        write(Kind.BYTE, Kind.INT, dirArguments);
    }

    @Override
    public void visitSetByte(SetByte builtin, DirValue dirResult, DirValue[] dirArguments) {
        set(Kind.BYTE, dirArguments);
    }

    @Override
    public void visitWriteShort(WriteShort builtin, DirValue dirResult, DirValue[] dirArguments) {
        write(Kind.SHORT, Kind.LONG, dirArguments);
    }

    @Override
    public void visitWriteShortAtIntOffset(WriteShortAtIntOffset builtin, DirValue dirResult, DirValue[] dirArguments) {
        write(Kind.SHORT, Kind.INT, dirArguments);
    }

    @Override
    public void visitSetShort(SetShort builtin, DirValue dirResult, DirValue[] dirArguments) {
        set(Kind.SHORT, dirArguments);
    }

    @Override
    public void visitWriteInt(WriteInt builtin, DirValue dirResult, DirValue[] dirArguments) {
        write(Kind.INT, Kind.LONG, dirArguments);
    }

    @Override
    public void visitWriteIntAtIntOffset(WriteIntAtIntOffset builtin, DirValue dirResult, DirValue[] dirArguments) {
        write(Kind.INT, Kind.INT, dirArguments);
    }

    @Override
    public void visitSetInt(SetInt builtin, DirValue dirResult, DirValue[] dirArguments) {
        set(Kind.INT, dirArguments);
    }

    @Override
    public void visitWriteFloat(WriteFloat builtin, DirValue dirResult, DirValue[] dirArguments) {
        write(Kind.FLOAT, Kind.LONG, dirArguments);
    }

    @Override
    public void visitWriteFloatAtIntOffset(WriteFloatAtIntOffset builtin, DirValue dirResult, DirValue[] dirArguments) {
        write(Kind.FLOAT, Kind.INT, dirArguments);
    }

    @Override
    public void visitSetFloat(SetFloat builtin, DirValue dirResult, DirValue[] dirArguments) {
        set(Kind.FLOAT, dirArguments);
    }

    @Override
    public void visitWriteLong(WriteLong builtin, DirValue dirResult, DirValue[] dirArguments) {
        write(Kind.LONG, Kind.LONG, dirArguments);
    }

    @Override
    public void visitWriteLongAtIntOffset(WriteLongAtIntOffset builtin, DirValue dirResult, DirValue[] dirArguments) {
        write(Kind.LONG, Kind.INT, dirArguments);
    }

    @Override
    public void visitSetLong(SetLong builtin, DirValue dirResult, DirValue[] dirArguments) {
        set(Kind.LONG, dirArguments);
    }

    @Override
    public void visitWriteDouble(WriteDouble builtin, DirValue dirResult, DirValue[] dirArguments) {
        write(Kind.DOUBLE, Kind.LONG, dirArguments);
    }

    @Override
    public void visitWriteDoubleAtIntOffset(WriteDoubleAtIntOffset builtin, DirValue dirResult, DirValue[] dirArguments) {
        write(Kind.DOUBLE, Kind.INT, dirArguments);
    }

    @Override
    public void visitSetDouble(SetDouble builtin, DirValue dirResult, DirValue[] dirArguments) {
        set(Kind.DOUBLE, dirArguments);
    }

    @Override
    public void visitWriteWord(WriteWord builtin, DirValue dirResult, DirValue[] dirArguments) {
        write(Kind.WORD, Kind.LONG, dirArguments);
    }

    @Override
    public void visitWriteWordAtIntOffset(WriteWordAtIntOffset builtin, DirValue dirResult, DirValue[] dirArguments) {
        write(Kind.WORD, Kind.INT, dirArguments);
    }

    @Override
    public void visitSetWord(SetWord builtin, DirValue dirResult, DirValue[] dirArguments) {
        set(Kind.WORD, dirArguments);
    }

    @Override
    public void visitWriteReference(WriteReference builtin, DirValue dirResult, DirValue[] dirArguments) {
        write(Kind.REFERENCE, Kind.LONG, dirArguments);
    }

    @Override
    public void visitWriteReferenceAtIntOffset(WriteReferenceAtIntOffset builtin, DirValue dirResult, DirValue[] dirArguments) {
        write(Kind.REFERENCE, Kind.INT, dirArguments);
    }

    @Override
    public void visitSetReference(SetReference builtin, DirValue dirResult, DirValue[] dirArguments) {
        set(Kind.REFERENCE, dirArguments);
    }

    private void compareAndSwapAtOffset(Kind kind, Kind offsetKind, DirValue dirResult, DirValue[] dirArguments) {
        final EirValue result = dirToEirValue(dirResult);
        final EirValue pointer = dirToEirValue(dirArguments[0]);
        final DirValue dirOffset = dirArguments[1];
        final EirValue expectedValue = dirToEirValue(dirArguments[2]);
        final EirValue newValue = dirToEirValue(dirArguments[3]);

        final EirVariable rax = createEirVariable(kind);
        assign(kind, rax, expectedValue);

        final AMD64EirCompareAndSwap instruction = dirOffset.isZeroConstant() ?
            new AMD64EirCompareAndSwap(eirBlock(), kind, newValue, pointer, rax) :
            new AMD64EirCompareAndSwap(eirBlock(), kind, newValue, pointer, offsetKind, dirToEirValue(dirOffset), rax);

        addInstruction(instruction);
        assign(kind, result, rax);
    }

    @Override
    public void visitCompareAndSwapInt(CompareAndSwapInt builtin, DirValue dirResult, DirValue[] dirArguments) {
        compareAndSwapAtOffset(Kind.INT, Kind.LONG, dirResult, dirArguments);
    }

    @Override
    public void visitCompareAndSwapIntAtIntOffset(CompareAndSwapIntAtIntOffset builtin, DirValue dirResult, DirValue[] dirArguments) {
        compareAndSwapAtOffset(Kind.INT, Kind.INT, dirResult, dirArguments);
    }

    @Override
    public void visitCompareAndSwapWord(CompareAndSwapWord builtin, DirValue dirResult, DirValue[] dirArguments) {
        compareAndSwapAtOffset(Kind.WORD, Kind.LONG, dirResult, dirArguments);
    }

    @Override
    public void visitCompareAndSwapWordAtIntOffset(CompareAndSwapWordAtIntOffset builtin, DirValue dirResult, DirValue[] dirArguments) {
        compareAndSwapAtOffset(Kind.WORD, Kind.INT, dirResult, dirArguments);
    }

    @Override
    public void visitCompareAndSwapReference(CompareAndSwapReference builtin, DirValue dirResult, DirValue[] dirArguments) {
        compareAndSwapAtOffset(Kind.REFERENCE, Kind.LONG, dirResult, dirArguments);
    }

    @Override
    public void visitCompareAndSwapReferenceAtIntOffset(CompareAndSwapReferenceAtIntOffset builtin, DirValue dirResult, DirValue[] dirArguments) {
        compareAndSwapAtOffset(Kind.REFERENCE, Kind.INT, dirResult, dirArguments);
    }

    @Override
    public void visitGetIntegerRegister(GetIntegerRegister builtin, DirValue dirResult, DirValue[] dirArguments) {
        assert dirArguments.length == 1 && dirArguments[0].isConstant() && dirArguments[0].value().asObject() instanceof VMRegister.Role;
        final EirValue result = dirToEirValue(dirResult);
        final VMRegister.Role registerRole = (VMRegister.Role) dirArguments[0].value().asObject();
        final EirValue roleValue = methodTranslation().integerRegisterRoleValue(registerRole);
        assert roleValue != null;
        assign(Kind.LONG, result, roleValue);
    }

    @Override
    public void visitSetIntegerRegister(SetIntegerRegister builtin, DirValue dirResult, DirValue[] dirArguments) {
        assert dirArguments.length == 2 && dirArguments[0].isConstant() && dirArguments[0].value().asObject() instanceof VMRegister.Role;
        final VMRegister.Role registerRole = (VMRegister.Role) dirArguments[0].value().asObject();
        final EirValue value = dirToEirValue(dirArguments[1]);
        assign(Kind.LONG, methodTranslation().integerRegisterRoleValue(registerRole), value);
    }

    @Override
    public void  visitLeastSignificantBit(LeastSignificantBit builtin, DirValue dirResult, DirValue[] dirArguments) {
        assert dirArguments.length == 1 && dirArguments[0].kind().equals(Kind.LONG);
        final EirValue result = dirToEirValue(dirResult);
        final EirValue value = dirToEirValue(dirArguments[0]);
        addInstruction(new  AMD64EirInstruction.BSF_I64(eirBlock(), result, value));
    }

    @Override
    public void visitMostSignificantBit(MostSignificantBit builtin,  DirValue dirResult, DirValue[] dirArguments) {
        assert dirArguments.length == 1  && dirArguments[0].kind().equals(Kind.LONG);
        final EirValue result = dirToEirValue(dirResult);
        final EirValue value = dirToEirValue(dirArguments[0]);
        addInstruction(new  AMD64EirInstruction.BSR_I64(eirBlock(), result, value));
    }

    @Override
    public void visitAdjustJitStack(AdjustJitStack builtin, DirValue dirResult, DirValue[] dirArguments) {
        assert dirArguments.length == 1;
        final EirValue registerPointerValue = methodTranslation().integerRegisterRoleValue(VMRegister.Role.ABI_STACK_POINTER);
        final EirValue delta = dirToEirValue(dirArguments[0]);
        if (delta.isConstant()) {
            final EirConstant constant = (EirConstant) delta;
            int addend = constant.value().asInt();
            if (addend == 0) {
                return;
            }
            addend <<= 3; // times word size (8)
            if (addend < 0) {
                addInstruction(new AMD64EirInstruction.SUB_I64(eirBlock(), registerPointerValue, methodTranslation().createEirConstant(IntValue.from(-addend))));
            } else {
                addInstruction(new AMD64EirInstruction.ADD_I64(eirBlock(), registerPointerValue, methodTranslation().createEirConstant(IntValue.from(addend))));
            }
            return;
        }
        addInstruction(new AMD64EirInstruction.SAL_I64(eirBlock(), delta, methodTranslation().createEirConstant(IntValue.from(3)))); // times word size (8)
        addInstruction(new AMD64EirInstruction.ADD_I64(eirBlock(), registerPointerValue, delta));
    }

    @Override
    public void visitGetInstructionPointer(GetInstructionPointer builtin, DirValue dirResult, DirValue[] dirArguments) {
        final EirValue result = dirToEirValue(dirResult);

        final EirVariable destination = createEirVariable(Kind.LONG);
        addInstruction(new LEA_PC(eirBlock(), destination));
        assign(Kind.LONG, result, destination);
    }

    @Override
    public void visitPause(Pause builtin, DirValue result, DirValue[] arguments) {
        addInstruction(new PAUSE(eirBlock()));
    }

    @Override
    public void visitCompareInts(CompareInts builtin, DirValue dirResult, DirValue[] dirArguments) {
        assert dirResult == null;
        final EirValue a = dirToEirValue(dirArguments[0]);
        final EirValue b = dirToEirValue(dirArguments[1]);
        addInstruction(new CMP_I32(eirBlock(), a, b));
    }
    @Override
    public void visitUnsignedIntGreaterEqual(UnsignedIntGreaterEqual builtin, DirValue dirResult, DirValue[] dirArguments) {
        final EirValue result = dirToEirValue(dirResult);
        final EirValue a = dirToEirValue(dirArguments[0]);
        final EirValue b = dirToEirValue(dirArguments[1]);
        final EirValue one = createEirVariable(Kind.INT);

        addInstruction(new ZERO(eirBlock(), Kind.WORD, result));
        assign(Kind.INT, one, createEirConstant(IntValue.ONE));
        addInstruction(new CMP_I32(eirBlock(), a, b));
        addInstruction(new CMOVAE_I32(eirBlock(), result, one));

    }

    @Override
    public void visitCompareWords(CompareWords builtin, DirValue dirResult, DirValue[] dirArguments) {
        assert dirResult == null;
        final EirValue a = dirToEirValue(dirArguments[0]);
        final EirValue b = dirToEirValue(dirArguments[1]);
        addInstruction(new CMP_I64(eirBlock(), a, b));
    }

    @Override
    public void visitBarMemory(BarMemory builtin, DirValue dirResult, DirValue[] dirArguments) {
        assert dirResult == null;
        if (!dirArguments[0].isConstant()) {
            ProgramWarning.message("optimizer failed to determine a memory barrier argument as a known constant => emitting a full barrier just in case");
            addInstruction(new MFENCE(eirBlock()));
            return;
        }
        final DirConstant dirConstant = (DirConstant) dirArguments[0];
        final Class<PoolSet<MemoryBarrier>> type = null;
        final PoolSet<MemoryBarrier> memoryBarriers = StaticLoophole.cast(type, dirConstant.value().asObject());
        if (methodTranslation().memoryModel.barriers.containsAll(memoryBarriers)) {
            return;
        }
        if (memoryBarriers.length() == 1) {
            if (memoryBarriers.contains(MemoryBarrier.LOAD_LOAD)) {
                addInstruction(new LFENCE(eirBlock()));
                return;
            }
            if (memoryBarriers.contains(MemoryBarrier.STORE_STORE)) {
                addInstruction(new SFENCE(eirBlock()));
                return;
            }
        }
        addInstruction(new MFENCE(eirBlock()));
    }

    @Override
    public void visitMakeStackVariable(MakeStackVariable builtin, DirValue dirResult, DirValue[] dirArguments) {
        assert dirArguments.length == 1;
        final EirVariable result = (EirVariable) dirToEirValue(dirResult);
        final EirValue value = dirToEirValue(dirArguments[0]);

        final EirVariable stackSlot;
        if (value instanceof EirVariable) {
            stackSlot = (EirVariable) value;
        } else {
            stackSlot = createEirVariable(value.kind());
            assign(value.kind(), stackSlot, value);
        }
        result.setAliasedVariable(stackSlot);
        methodTranslation().addEpilogueStackSlotUse(stackSlot);
        addInstruction(new LEA_STACK_ADDRESS(eirBlock(), result, stackSlot));
    }

    @Override
    public void visitStackAllocate(StackAllocate builtin, DirValue dirResult, DirValue[] dirArguments) {
        assert dirArguments.length == 1;
        assert dirArguments[0] instanceof DirConstant;
        final int size = ((DirConstant) dirArguments[0]).value().asInt();
        final int offset = methodTranslation().addStackAllocation(size);
        final EirVariable result = (EirVariable) dirToEirValue(dirResult);
        addInstruction(new STACK_ALLOCATE(eirBlock(), result, offset));
    }

    @Override
    public void visitIntToFloat(IntToFloat builtin, DirValue dirResult, DirValue[] dirArguments) {
        addInstruction(new MOVD_F32_I32(eirBlock(), dirToEirValue(dirResult), dirToEirValue(dirArguments[0])));
    }

    @Override
    public void visitFloatToInt(FloatToInt builtin, DirValue dirResult, DirValue[] dirArguments) {
        addInstruction(new MOVD_I32_F32(eirBlock(), dirToEirValue(dirResult), dirToEirValue(dirArguments[0])));
    }

    @Override
    public void visitLongToDouble(LongToDouble builtin, DirValue dirResult, DirValue[] dirArguments) {
        addInstruction(new MOVD_F64_I64(eirBlock(), dirToEirValue(dirResult), dirToEirValue(dirArguments[0])));
    }

    @Override
    public void visitDoubleToLong(DoubleToLong builtin, DirValue dirResult, DirValue[] dirArguments) {
        addInstruction(new MOVD_I64_F64(eirBlock(), dirToEirValue(dirResult), dirToEirValue(dirArguments[0])));
    }

}
