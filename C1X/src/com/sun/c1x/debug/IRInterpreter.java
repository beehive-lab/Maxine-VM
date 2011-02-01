/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.c1x.debug;

import static java.lang.reflect.Modifier.*;

import java.lang.reflect.*;
import java.util.*;

import sun.misc.*;

import com.sun.c1x.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.ir.Value.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;
import com.sun.c1x.value.FrameState.*;
import com.sun.cri.bytecode.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * This class implements an interpreter for HIR.
 *
 * @author Marcelo Cintra
 */
public class IRInterpreter {
    /**
     * This class is thrown when the IRInterpreter detects a problem with the IR.
     */
    public static class IRInterpreterException extends RuntimeException {

        public static final long serialVersionUID = 8974598793158773L;

        public IRInterpreterException(String msg) {
            super(msg);
        }
    }

    private static final Map<String, IR> compiledMethods = new HashMap<String, IR>();
    public static final Unsafe unsafe = (Unsafe) getStaticField(Unsafe.class, "theUnsafe");

    public RiRuntime runtime;
    public C1XCompiler compiler;

    public IRInterpreter(RiRuntime runtime, C1XCompiler compiler) {
        this.runtime = runtime;
        this.compiler = compiler;
    }

    private static class Val {
        int counter;
        CiConstant value;

        public Val(int counter, CiConstant value) {
            this.setCounter(counter);
            this.value = value;
        }

        /**
         * Sets the counter instance variable.
         *
         * @param counter the counter to set
         */
        public void setCounter(int counter) {
            this.counter = counter;
        }
    }

    private class Environment {

        private class PhiMove {
            Phi phi;
            Value value;

            public PhiMove(Phi phi, Value value) {
                super();
                this.phi = phi;
                this.value = value;
            }
        }

        private Map<Value, Val> instructionValueMap = new HashMap<Value, Val>();
        private Map<Value, ArrayList<PhiMove>> phiMoves = new HashMap<Value, ArrayList<PhiMove>>();

        private class ValueMapInitializer implements BlockClosure {

            public void apply(final BlockBegin block) {
                FrameState state = block.stateBefore();
                state.forEachPhi(block, new PhiProcedure() {
                    public boolean doPhi(Phi phi) {
                        for (int j = 0; j < phi.inputCount(); j++) {
                            Value phiOperand = block.isExceptionEntry() ? phi.inputAt(j) : phi.block().predAt(j).end();
                            assert phiOperand != null : "Illegal phi operand";

                            if (phiOperand instanceof Phi) {
                                if (phiOperand != phi) {
                                    phi.setFlag(Flag.PhiVisited);
                                    addPhiToInstructionList((Phi) phiOperand, phi);
                                    phi.clearFlag(Flag.PhiVisited);
                                }
                            } else {
                                ArrayList<PhiMove> blockPhiMoves = phiMoves.get(phiOperand);
                                if (blockPhiMoves == null) {
                                    blockPhiMoves = new ArrayList<PhiMove>();
                                    phiMoves.put(phiOperand, blockPhiMoves);
                                }
                                blockPhiMoves.add(new PhiMove(phi, phi.inputAt(j)));
                            }
                        }
                        return true;
                    }
                });

                for (Instruction instr = block; instr != null; instr = instr.next()) {
                    instructionValueMap.put(instr, new Val(-1, null));
                }
            }

            private void addPhiToInstructionList(Phi phiSrc, Phi phi) {
                phiSrc.setFlag(Flag.PhiVisited);
                for (int j = 0; j < phiSrc.inputCount(); j++) {
                    Value phiOperand = phiSrc.inputAt(j);
                    assert phiOperand != null : "Illegal phi operand";

                    if (phiOperand instanceof Phi) {
                        if (phiOperand != phi && !phiOperand.checkFlag(Flag.PhiVisited)) {
                            addPhiToInstructionList((Phi) phiOperand, phi);
                        }
                    } else {
                        ArrayList<PhiMove> operandPhiMoves = phiMoves.get(phiOperand);
                        if (operandPhiMoves == null) {
                            operandPhiMoves = new ArrayList<PhiMove>();
                            phiMoves.put(phiOperand, operandPhiMoves);
                        }
                        operandPhiMoves.add(new PhiMove(phi, phiOperand));
                    }
                }
                phiSrc.clearFlag(Flag.PhiVisited);
            }
        }

        public void performPhiMove(Value i) {
            ArrayList<PhiMove> blockPhiMoves = phiMoves.get(i);
            if (blockPhiMoves != null) {
                ArrayList <CiConstant> currentPhiValues = new ArrayList <CiConstant>();

                // first save the current values of phi instructions
                // phi instructions must be executed atomically
                for (PhiMove phiMove : blockPhiMoves) {
                    currentPhiValues.add(lookup(phiMove.value));
                }

                // perform the phi move using the current value
                // each phi holds
                for (PhiMove phiMove : blockPhiMoves) {
                    bind(phiMove.phi, currentPhiValues.remove(0), 0);
                }
            }
        }

        public void bind(Value i, CiConstant value, Integer iCounter) {
            Val v = new Val(iCounter, value);
            assert v.counter >= 0;
            instructionValueMap.put(i, new Val(iCounter, value));
        }

        public Environment(FrameState newFrameState, CiConstant[] values, IR ir) {
            assert values.length <= newFrameState.localsSize() : "Incorrect number of initialization arguments";
            ir.startBlock.iteratePreOrder(new ValueMapInitializer());
            int index = 0;

            for (CiConstant value : values) {
                CiConstant obj;
                // These conversions are necessary since the input values are
                // parsed as integers
                Value local = newFrameState.localAt(index);
                if (local.kind == CiKind.Float && value.kind == CiKind.Int) {
                    obj = CiConstant.forFloat(value.asInt());
                } else if ((local.kind == CiKind.Double && value.kind == CiKind.Int)) {
                    obj = CiConstant.forDouble(value.asInt());
                } else {
                    obj = value;
                }
                bind(local, obj, 0);
                performPhiMove(local);
                index += local.kind.sizeInSlots();
            }
        }

        CiConstant lookup(Value instruction) {
            if (!(instruction instanceof Constant)) {
                final Val result = instructionValueMap.get(instruction);
                assert result != null : "Value not defined for instruction: " + instruction;
                return result.value;
            } else {
                return instruction.asConstant();
            }
        }
    }

    private class Evaluator extends DefaultValueVisitor {

        private final RiMethod method;
        private BlockBegin block;
        private Instruction currentInstruction;
        private CiConstant result;
        private Environment environment;
        private int instructionCounter;
        private Throwable throwable;
        private IRInterpreter interpreter = new IRInterpreter(runtime, compiler);

        public Evaluator(IR hir, CiConstant[] arguments) {
            this.method = hir.compilation.method;
            block = hir.startBlock;
            currentInstruction = hir.startBlock;
            result = null;
            environment = new Environment(hir.startBlock.stateBefore(), arguments, hir);
            instructionCounter = 1;
            throwable = null;
        }

        @Override
        public void visitPhi(Phi i) {
            jumpNextInstruction();
        }

        @Override
        public void visitLocal(Local i) {
            jumpNextInstruction();
        }

        @Override
        public void visitConstant(Constant i) {
            environment.bind(i, i.asConstant(), instructionCounter);
            jumpNextInstruction();
        }

        private void jumpNextInstruction() {
            if (C1XOptions.PrintStateInInterpreter && currentInstruction.stateAfter() != null) {
                printInstructionState(currentInstruction, true);
            }
            environment.performPhiMove(currentInstruction);
            currentInstruction = currentInstruction.next();
        }

        @Override
        public void visitResolveClass(ResolveClass i) {
            Class <?> javaClass = resolveClass(i.type);
            environment.bind(i, CiConstant.forObject(javaClass), instructionCounter);
            jumpNextInstruction();
        }

        private void raiseException(Instruction i, Throwable e) {
            List<ExceptionHandler> exceptionHandlerList = i.exceptionHandlers();
            for (ExceptionHandler eh : exceptionHandlerList) {
                if (eh.handler.isCatchAll() || resolveClass(eh.handler.catchKlass()).isAssignableFrom(e.getClass())) {
                    jump(eh.entryBlock());
                    // bind the exception object to e
                    environment.bind(eh.entryBlock().next(), CiConstant.forObject(e), instructionCounter);
                    return;
                }
            }
            environment.bind(i, CiConstant.forObject(e), instructionCounter);
            throwable = e;
        }

        @Override
        public void visitLoadField(LoadField i) {
            try {
                Class<?> klass = resolveClass(i.field().holder());
                String name = i.field().name();
                Field field;
                try {
                    field = klass.getDeclaredField(name);
                } catch (NoSuchFieldException e) {
                    field = klass.getField(name);
                }
                field.setAccessible(true);
                Object boxedJavaValue;
                Value object = i.object();
                if (object != null) {
                    boxedJavaValue = field.get(environment.lookup(object).boxedValue());
                } else {
                    assert i.isStatic() : "Field must be static in LoadField";
                    boxedJavaValue = field.get(null);
                }
                environment.bind(i, fromBoxedJavaValue(boxedJavaValue), instructionCounter);
            } catch (Throwable e) {
                raiseException(i, e);
            }
            jumpNextInstruction();
        }

        @Override
        public void visitStoreField(StoreField i) {
            try {
                Class<?> klass = resolveClass(i.field().holder());
                Field field = klass.getDeclaredField(i.field().name());
                field.setAccessible(true);
                field.set(environment.lookup(i.object()).asObject(), getCompatibleBoxedValue(resolveClass(i.field().type()), i.value()));
            } catch (IllegalAccessException e) {
                raiseException(i, e);
            } catch (SecurityException e) {
                raiseException(i, e);
            } catch (NoSuchFieldException e) {
                raiseException(i, e);
            } catch (Throwable e) {
                raiseException(i, e);
            }
            jumpNextInstruction();
        }

        @Override
        public void visitArrayLength(ArrayLength i) {
            assertKind(i.array().kind, CiKind.Object);
            assertArrayType(i.array().exactType());
            assertArrayType(i.array().declaredType());
            assertKind(i.kind, CiKind.Int);

            try {
                CiConstant array = environment.lookup(i.array());
                environment.bind(i, CiConstant.forInt(Array.getLength(array.asObject())), instructionCounter);
                jumpNextInstruction();
            } catch (NullPointerException ne) {
                raiseException(i, ne);
            }
        }

        @Override
        public void visitLoadIndexed(LoadIndexed i) {
            Object array = environment.lookup(i.array()).asObject();
            try {
                int arrayIndex = environment.lookup(i.index()).asInt();
                if (arrayIndex >= Array.getLength(array)) {
                    raiseException(i, new ArrayIndexOutOfBoundsException());
                    return;
                }
                Object result = Array.get(array, arrayIndex);
                environment.bind(i, fromBoxedJavaValue(result), instructionCounter);
            } catch (NullPointerException e) {
                raiseException(i, e);
            } catch (ArrayIndexOutOfBoundsException e) {
                raiseException(i, e);
            } catch (IllegalArgumentException e) {
                raiseException(i, e);
            } catch (ArrayStoreException e) {
                raiseException(i, e);
            }
            jumpNextInstruction();
        }

        private Object getCompatibleBoxedValue(Class<?> type, Value value) {
            CiConstant lookupValue = environment.lookup(value);
            if (type == byte.class) {
                assert value.kind == CiKind.Int : "Types are not compatible";
                return (byte) lookupValue.asInt();
            } else if (type == short.class) {
                assert value.kind == CiKind.Int : "Types are not compatible";
                return (short) lookupValue.asInt();
            } else if (type == char.class) {
                assert value.kind == CiKind.Int : "Types are not compatible";
                return (char) lookupValue.asInt();
            } else if (type == boolean.class) {
                assert value.kind == CiKind.Int : "Types are not compatible";
                return lookupValue.asInt() == 1;
            } else if (type == double.class) {
                if (lookupValue.kind == CiKind.Int) {
                    return (double) lookupValue.asInt();
                } else {
                    return lookupValue.boxedValue();
                }
            } else if (type == float.class) {
                if (lookupValue.kind == CiKind.Int) {
                    return (float) lookupValue.asInt();
                } else {
                    return lookupValue.boxedValue();
                }
            } else {
                return lookupValue.boxedValue();
            }
        }

        @Override
        public void visitStoreIndexed(StoreIndexed i) {
            Object array = environment.lookup(i.array()).asObject();

            try {
                Class<?> componentType = getElementType(array);
                Array.set(array, environment.lookup(i.index()).asInt(), getCompatibleBoxedValue(componentType, i.value()));
            } catch (NullPointerException ne) {
                raiseException(i, ne);
            } catch (IllegalArgumentException ie) {
                raiseException(i, new ArrayStoreException());
            } catch (ArrayIndexOutOfBoundsException ae) {
                raiseException(i, ae);
            }
            jumpNextInstruction();
        }

        private Class<?> getElementType(Object array) {
            Class <?> elementType = array.getClass().getComponentType();
            while (elementType.isArray()) {
                elementType = elementType.getComponentType();
            }
            return elementType;
        }

        @Override
        public void visitNegateOp(NegateOp i) {
            CiConstant xval = environment.lookup(i.x());
            assertKind(i.kind, xval.kind);

            switch (i.kind) {
                case Int:
                    environment.bind(i, CiConstant.forInt(-xval.asInt()), instructionCounter);
                    break;
                case Long:
                    environment.bind(i, CiConstant.forLong(-xval.asLong()), instructionCounter);
                    break;
                case Float:
                    environment.bind(i, CiConstant.forFloat(-xval.asFloat()), instructionCounter);
                    break;
                case Double:
                    environment.bind(i, CiConstant.forDouble(-xval.asDouble()), instructionCounter);
                    break;
                default:
                    Util.shouldNotReachHere();
                    break;
            }
            jumpNextInstruction();
        }

        @Override
        public void visitArithmeticOp(ArithmeticOp i) {
            CiConstant xval = environment.lookup(i.x());
            CiConstant yval = environment.lookup(i.y());

            assertKind(xval.kind.stackKind(), yval.kind.stackKind(), i.kind.stackKind());

            switch (i.opcode) {
                case Bytecodes.IADD:
                    environment.bind(i, CiConstant.forInt((xval.asInt() + yval.asInt())), instructionCounter);
                    break;
                case Bytecodes.ISUB:
                    environment.bind(i, CiConstant.forInt((xval.asInt() - yval.asInt())), instructionCounter);
                    break;
                case Bytecodes.IMUL:
                    environment.bind(i, CiConstant.forInt((xval.asInt() * yval.asInt())), instructionCounter);
                    break;
                case Bytecodes.IDIV:
                    try {
                        environment.bind(i, CiConstant.forInt((xval.asInt() / yval.asInt())), instructionCounter);
                    } catch (ArithmeticException ae) {
                        raiseException(i, ae);
                    }
                    break;
                case Bytecodes.IREM:
                    try {
                        environment.bind(i, CiConstant.forInt((xval.asInt() % yval.asInt())), instructionCounter);
                    } catch (ArithmeticException ae) {
                        raiseException(i, ae);
                    }
                    break;

                case Bytecodes.LADD:
                    environment.bind(i, CiConstant.forLong((xval.asLong() + yval.asLong())), instructionCounter);
                    break;
                case Bytecodes.LSUB:
                    environment.bind(i, CiConstant.forLong((xval.asLong() - yval.asLong())), instructionCounter);
                    break;
                case Bytecodes.LMUL:
                    environment.bind(i, CiConstant.forLong((xval.asLong() * yval.asLong())), instructionCounter);
                    break;
                case Bytecodes.LDIV:
                    try {
                        environment.bind(i, CiConstant.forLong((xval.asLong() / yval.asLong())), instructionCounter);
                    } catch (ArithmeticException ae) {
                        raiseException(i, ae);
                    }
                    break;
                case Bytecodes.LREM:
                    try {
                        environment.bind(i, CiConstant.forLong((xval.asLong() % yval.asLong())), instructionCounter);
                    } catch (ArithmeticException ae) {
                        raiseException(i, ae);
                    }
                    break;

                case Bytecodes.FADD:
                    environment.bind(i, CiConstant.forFloat((xval.asFloat() + yval.asFloat())), instructionCounter);
                    break;
                case Bytecodes.FSUB:
                    environment.bind(i, CiConstant.forFloat((xval.asFloat() - yval.asFloat())), instructionCounter);
                    break;
                case Bytecodes.FMUL:
                    environment.bind(i, CiConstant.forFloat((xval.asFloat() * yval.asFloat())), instructionCounter);
                    break;
                case Bytecodes.FDIV:
                    environment.bind(i, CiConstant.forFloat((xval.asFloat() / yval.asFloat())), instructionCounter);
                    break;
                case Bytecodes.FREM:
                    environment.bind(i, CiConstant.forFloat((xval.asFloat() % yval.asFloat())), instructionCounter);
                    break;

                case Bytecodes.DADD:
                    environment.bind(i, CiConstant.forDouble((xval.asDouble() + yval.asDouble())), instructionCounter);
                    break;
                case Bytecodes.DSUB:
                    environment.bind(i, CiConstant.forDouble((xval.asDouble() - yval.asDouble())), instructionCounter);
                    break;
                case Bytecodes.DMUL:
                    environment.bind(i, CiConstant.forDouble((xval.asDouble() * yval.asDouble())), instructionCounter);
                    break;
                case Bytecodes.DDIV:
                    environment.bind(i, CiConstant.forDouble((xval.asDouble() / yval.asDouble())), instructionCounter);
                    break;
                case Bytecodes.DREM:
                    environment.bind(i, CiConstant.forDouble((xval.asDouble() % yval.asDouble())), instructionCounter);
                    break;

                default:
                    Util.shouldNotReachHere();
            }
            jumpNextInstruction();
        }

        @Override
        public void visitShiftOp(ShiftOp i) {
            CiConstant xval = environment.lookup(i.x());
            CiConstant yval = environment.lookup(i.y());
            int s;

            switch (i.opcode) {
                case Bytecodes.ISHL:
                    assertKind(xval.kind.stackKind(), yval.kind.stackKind(), CiKind.Int);
                    assert (yval.asInt() < 32) : "Illegal shift constant in a ISH instruction";
                    environment.bind(i, CiConstant.forInt((xval.asInt() << (yval.asInt() & 0x1F))), instructionCounter);
                    break;

                case Bytecodes.ISHR:
                    assertKind(xval.kind.stackKind(), yval.kind.stackKind(), CiKind.Int);
                    assert (yval.asInt() < 32) : "Illegal shift constant in a ISH instruction";
                    environment.bind(i, CiConstant.forInt((xval.asInt() >> (yval.asInt() & 0x1F))), instructionCounter);
                    break;

                case Bytecodes.IUSHR:
                    assertKind(xval.kind.stackKind(), yval.kind.stackKind(), CiKind.Int);
                    assert (yval.asInt() < 32) : "Illegal shift constant in a ISH instruction";
                    s = yval.asInt() & 0x1f;
                    int iresult = xval.asInt() >> s;
                    if (xval.asInt() < 0) {
                        iresult = iresult + (2 << ~s);
                    }
                    environment.bind(i, CiConstant.forInt(iresult), instructionCounter);
                    break;

                case Bytecodes.LSHL:
                    assertKind(xval.kind.stackKind(), CiKind.Long);
                    assertKind(yval.kind.stackKind(), CiKind.Int);
                    assert (yval.asInt() < 64) : "Illegal shift constant in a ISH instruction";
                    environment.bind(i, CiConstant.forLong((xval.asLong() << (yval.asInt() & 0x3F))), instructionCounter);
                    break;

                case Bytecodes.LSHR:
                    assertKind(xval.kind.stackKind(), CiKind.Long);
                    assertKind(yval.kind.stackKind(), CiKind.Int);
                    assert (yval.asInt() < 64) : "Illegal shift constant in a ISH instruction";
                    environment.bind(i, CiConstant.forLong((xval.asLong() >> (yval.asInt() & 0x3F))), instructionCounter);
                    break;

                case Bytecodes.LUSHR:
                    assertKind(xval.kind.stackKind(), CiKind.Long);
                    assertKind(yval.kind.stackKind(), CiKind.Int);
                    assert (yval.asInt() < 64) : "Illegal shift constant in a ISH instruction";
                    s = yval.asInt() & 0x3f;
                    long lresult = xval.asLong() >> s;
                    if (xval.asLong() < 0) {
                        lresult = lresult + (2L << ~s);
                    }
                    environment.bind(i, CiConstant.forLong(lresult), instructionCounter);
                    break;
                default:
                    fail("Illegal ShiftOp opcode");
            }
            jumpNextInstruction();
        }

        @Override
        public void visitLogicOp(LogicOp i) {
            final CiConstant xval = environment.lookup(i.x());
            final CiConstant yval = environment.lookup(i.y());

            switch (i.opcode) {
                case Bytecodes.IAND:
                    assertKind(xval.kind.stackKind(), yval.kind.stackKind(), CiKind.Int);
                    environment.bind(i, CiConstant.forInt((xval.asInt() & yval.asInt())), instructionCounter);
                    break;
                case Bytecodes.IOR:
                    assertKind(xval.kind.stackKind(), yval.kind.stackKind(), CiKind.Int);
                    environment.bind(i, CiConstant.forInt((xval.asInt() | yval.asInt())), instructionCounter);
                    break;
                case Bytecodes.IXOR:
                    assertKind(xval.kind.stackKind(), yval.kind.stackKind(), CiKind.Int);
                    environment.bind(i, CiConstant.forInt((xval.asInt() ^ yval.asInt())), instructionCounter);
                    break;
                case Bytecodes.LAND:
                    assertKind(xval.kind.stackKind(), yval.kind.stackKind(), CiKind.Long);
                    environment.bind(i, CiConstant.forLong((xval.asLong() & yval.asLong())), instructionCounter);
                    break;
                case Bytecodes.LOR:
                    assertKind(xval.kind.stackKind(), yval.kind.stackKind(), CiKind.Long);
                    environment.bind(i, CiConstant.forLong((xval.asLong() | yval.asLong())), instructionCounter);
                    break;
                case Bytecodes.LXOR:
                    assertKind(xval.kind.stackKind(), yval.kind.stackKind(), CiKind.Long);
                    environment.bind(i, CiConstant.forLong((xval.asLong() ^ yval.asLong())), instructionCounter);
                    break;
                default:
                    fail("Logic operation instruction has an illegal opcode");
            }
            jumpNextInstruction();
        }

        @Override
        public void visitCompareOp(CompareOp i) {
            final CiConstant xval = environment.lookup(i.x());
            final CiConstant yval = environment.lookup(i.y());

            switch (i.opcode) {
                case Bytecodes.LCMP:
                    environment.bind(i, CiConstant.forInt(compareLongs(xval.asLong(), yval.asLong())), instructionCounter);
                    break;

                case Bytecodes.FCMPG:
                case Bytecodes.FCMPL:
                    environment.bind(i, CiConstant.forInt(compareFloats(i.opcode, xval.asFloat(), yval.asFloat())), instructionCounter);
                    break;

                case Bytecodes.DCMPG:
                case Bytecodes.DCMPL:
                    environment.bind(i, CiConstant.forInt(compareDoubles(i.opcode, xval.asDouble(), yval.asDouble())), instructionCounter);
                    break;

                default:
                    fail("Illegal CompareOp opcode");
            }
            jumpNextInstruction();
        }

        @Override
        public void visitIfOp(IfOp i) {
            final CiConstant tval = environment.lookup(i.trueValue());
            final CiConstant fval = environment.lookup(i.falseValue());
            final CiConstant x = environment.lookup(i.x());
            final CiConstant y = environment.lookup(i.y());

            switch (i.condition()) {
                case EQ:
                    bindIfOp(i, x.equals(y), tval, fval);
                    break;

                case NE:
                    bindIfOp(i, !x.equals(y), tval, fval);
                    break;

                case GT:
                    bindIfOp(i, x.asInt() > y.asInt(), tval, fval);
                    break;

                case GE:
                    bindIfOp(i, x.asInt() >= y.asInt(), tval, fval);
                    break;

                case LT:
                    bindIfOp(i, x.asInt() < y.asInt(), tval, fval);
                    break;

                case LE:
                    bindIfOp(i, x.asInt() <= y.asInt(), tval, fval);
                    break;
            }
            jumpNextInstruction();
        }

        private void bindIfOp(IfOp i, boolean condition, final CiConstant tval, final CiConstant fval) {
            if (condition) {
                environment.bind(i, tval, instructionCounter);
            } else {
                environment.bind(i, fval, instructionCounter);
            }
        }

        @Override
        public void visitConvert(Convert i) {
            final CiConstant value = environment.lookup(i.value());
            switch (i.opcode) {
                case Bytecodes.I2L:
                    assertKind(value.kind, CiKind.Int);
                    environment.bind(i, CiConstant.forLong(value.asInt()), instructionCounter);
                    break;
                case Bytecodes.I2F:
                    assertKind(value.kind, CiKind.Int);
                    environment.bind(i, CiConstant.forFloat(value.asInt()), instructionCounter);
                    break;
                case Bytecodes.I2D:
                    assertKind(value.kind, CiKind.Int);
                    environment.bind(i, CiConstant.forDouble(value.asInt()), instructionCounter);
                    break;

                case Bytecodes.I2B:
                    assertKind(value.kind, CiKind.Int);
                    environment.bind(i, CiConstant.forByte((byte) value.asInt()), instructionCounter);
                    break;
                case Bytecodes.I2C:
                    assertKind(value.kind, CiKind.Int);
                    environment.bind(i, CiConstant.forChar((char) value.asInt()), instructionCounter);
                    break;
                case Bytecodes.I2S:
                    assertKind(value.kind, CiKind.Int);
                    environment.bind(i, CiConstant.forShort((short) value.asInt()), instructionCounter);
                    break;

                case Bytecodes.L2I:
                    assertKind(value.kind, CiKind.Long);
                    environment.bind(i, CiConstant.forInt((int) value.asLong()), instructionCounter);
                    break;
                case Bytecodes.L2F:
                    assertKind(value.kind, CiKind.Long);
                    environment.bind(i, CiConstant.forFloat(value.asLong()), instructionCounter);
                    break;
                case Bytecodes.L2D:
                    assertKind(value.kind, CiKind.Long);
                    environment.bind(i, CiConstant.forDouble(value.asLong()), instructionCounter);
                    break;

                case Bytecodes.F2I:
                    assertKind(value.kind, CiKind.Float);
                    environment.bind(i, CiConstant.forInt((int) value.asFloat()), instructionCounter);
                    break;
                case Bytecodes.F2L:
                    assertKind(value.kind, CiKind.Float);
                    environment.bind(i, CiConstant.forLong((long) value.asFloat()), instructionCounter);
                    break;
                case Bytecodes.F2D:
                    assertKind(value.kind, CiKind.Float);
                    environment.bind(i, CiConstant.forDouble(value.asFloat()), instructionCounter);
                    break;

                case Bytecodes.D2I:
                    assertKind(value.kind, CiKind.Double);
                    environment.bind(i, CiConstant.forInt((int) value.asDouble()), instructionCounter);
                    break;
                case Bytecodes.D2L:
                    assertKind(value.kind, CiKind.Double);
                    environment.bind(i, CiConstant.forLong((long) value.asDouble()), instructionCounter);
                    break;
                case Bytecodes.D2F:
                    assertKind(value.kind, CiKind.Double);
                    environment.bind(i, CiConstant.forFloat((float) value.asDouble()), instructionCounter);
                    break;

                default:
                    fail("invalid opcode in Convert");
            }
            jumpNextInstruction();
        }

        @Override
        public void visitNullCheck(NullCheck i) {
            final CiConstant object = environment.lookup(i.object());
            assertKind(object.kind, CiKind.Object);
            if (object.isNonNull()) {
                environment.bind(i, CiConstant.forObject(object.boxedValue()), instructionCounter);
            } else {
                raiseException(i, new NullPointerException());
            }
            jumpNextInstruction();
        }

        @Override
        public void visitInvoke(Invoke i) {
            if (!C1XOptions.InterpretInvokedMethods) {
                invokeUsingReflection(i);
                return;
            }

            RiMethod targetMethod = i.target();
            String methodName = targetMethod.name();
            if ("<init>".equals(methodName) || "<clinit>".equals(methodName)) {
                Object res = callInitMethod(i);
                environment.bind(i.arguments()[0], CiConstant.forObject(res), instructionCounter);
                jumpNextInstruction();
                return;
            }
            if (!targetMethod.isResolved()) {
                switch (i.opcode()) {
                    case Bytecodes.INVOKEINTERFACE: {
                        targetMethod = runtime.getRiMethod(resolveMethod(targetMethod));
                        break;
                    }
                    case Bytecodes.INVOKESPECIAL: {
                        if (targetMethod.isConstructor()) {
                            Constructor<?> constructor = resolveConstructor(targetMethod);
                            targetMethod = runtime.getRiMethod(constructor);
                        } else {
                            Method method = resolveMethod(targetMethod);
                            if (!Modifier.isStatic(targetMethod.accessFlags())) {
                                throw new IncompatibleClassChangeError(method + " is a static method");
                            }
                            targetMethod = runtime.getRiMethod(method);
                        }
                        break;
                    }
                    case Bytecodes.INVOKESTATIC: {
                        Method method = resolveMethod(targetMethod);
                        if (!Modifier.isStatic(targetMethod.accessFlags())) {
                            throw new IncompatibleClassChangeError(method + " is not a static method");
                        }
                        targetMethod = runtime.getRiMethod(method);
                        break;
                    }
                    case Bytecodes.INVOKEVIRTUAL: {
                        Method method = resolveMethod(targetMethod);
                        if (!Modifier.isStatic(targetMethod.accessFlags())) {
                            throw new IncompatibleClassChangeError(method + " is a static method");
                        }
                        targetMethod = runtime.getRiMethod(method);
                        break;
                    }
                }
            }
            // native methods are invoked using reflection.
            // some special methods/classes are also always called using reflection
            if (isNative(targetMethod.accessFlags()) || "newInstance".equals(methodName) || "newInstance0".equals(methodName) ||
                targetMethod.holder().name().startsWith("sun/reflect/Unsafe")                     ||
                targetMethod.holder().name().startsWith("sun/reflect/Reflection")                 ||
                targetMethod.holder().name().startsWith("sun/reflect/FieldAccessor")) {
                invokeUsingReflection(i);
                return;
            }

            switch (i.opcode()) {
                case Bytecodes.INVOKEINTERFACE: {
                    RiType type;
                    try {
                        Class<?> objClass = environment.lookup(i.arguments()[0]).asObject().getClass();
                        type = runtime.getRiType(objClass);
                    } catch (NullPointerException ne) {
                        raiseException(i, ne);
                        return;
                    }
                    targetMethod = type.resolveMethodImpl(targetMethod);
                    break;
                }

                case Bytecodes.INVOKEVIRTUAL: {
                    RiType type;
                    try {
                        Class<?> objClass = environment.lookup(i.arguments()[0]).asObject().getClass();
                        type = runtime.getRiType(objClass);
                    } catch (NullPointerException ne) {
                        raiseException(i, ne);
                        return;
                    }
                    targetMethod = type.resolveMethodImpl(targetMethod);
                    break;
                }
            }
            if (!i.isStatic()) {
                if (environment.lookup(i.arguments()[0]).boxedValue() == null) {
                    raiseException(i, new NullPointerException());
                    return;
                }
            }

            CiConstant result;
            try {
                IR methodHir = compiledMethods.get(targetMethod.holder().name() + methodName + targetMethod.signature().toString());
                if (methodHir == null) {
                    C1XCompilation compilation = new C1XCompilation(compiler, targetMethod, -1);
                    methodHir = compilation.emitHIR();
                    compiledMethods.put(targetMethod.holder().name() + methodName + targetMethod.signature().toString(), methodHir);
                }
                result = interpreter.execute(methodHir, arguments(i));
                environment.bind(i, fromBoxedJavaValue(result.boxedValue()), instructionCounter);
            } catch (InvocationTargetException e) {
                raiseException(i, e.getTargetException());
            } catch (Throwable e) {
                raiseException(i, e);
            }
            jumpNextInstruction();
        }

        private CiConstant [] arguments(Invoke i) {
            RiSignature signature = i.target().signature();

            int nargs = signature.argumentCount(!i.isStatic());
            CiConstant[] arglist = new CiConstant[nargs];
            int index = 0;
            if (i.isStatic()) {
                index = 0;
                for (int j = 0; j < nargs; j++) {
                    CiKind argumentType = signature.argumentKindAt(j);
                    arglist[j] = getCompatibleCiConstant(resolveClass(signature.argumentTypeAt(j, null)), (i.arguments()[index])); //environment.lookup(i.arguments()[index]);
                    index += argumentType.sizeInSlots();
                }
            } else {
                arglist[0] = environment.lookup(i.receiver());
                index = 1;
                for (int j = 1; j < nargs; j++) {
                    CiKind argumentType = signature.argumentKindAt(j - 1);
                    arglist[j] = getCompatibleCiConstant(resolveClass(signature.argumentTypeAt(j - 1, null)), (i.arguments()[index])); //environment.lookup(i.arguments()[index]);
                    index += argumentType.sizeInSlots();
                }
            }
            return arglist;
        }
        private CiConstant getCompatibleCiConstant(Class<?> arrayType, Value value) {
            if (arrayType == byte.class) {
                assert value.kind == CiKind.Int : "Types are not compatible";
                return CiConstant.forByte((byte) environment.lookup(value).asInt());
            } else if (arrayType == short.class) {
                assert value.kind == CiKind.Int : "Types are not compatible";
                return CiConstant.forShort((short) environment.lookup(value).asInt());
            } else if (arrayType == char.class) {
                assert value.kind == CiKind.Int : "Types are not compatible";
                return CiConstant.forChar((char) environment.lookup(value).asInt());
            } else if (arrayType == boolean.class) {
                assert value.kind == CiKind.Int : "Types are not compatible";
                return CiConstant.forBoolean(environment.lookup(value).asInt() != 0);
            } else if (arrayType == double.class) {
                CiConstant rvalue = environment.lookup(value);
                if (rvalue.kind == CiKind.Int) {
                    return CiConstant.forDouble(rvalue.asInt());
                } else {
                    return rvalue;
                }
            } else if (arrayType == float.class) {
                CiConstant rvalue = environment.lookup(value);
                if (rvalue.kind == CiKind.Int) {
                    return CiConstant.forFloat(rvalue.asInt());
                } else {
                    return rvalue;
                }
            } else {
                return environment.lookup(value);
            }
        }

        public void invokeUsingReflection(Invoke i) {
            RiMethod targetMethod = i.target();
            RiSignature signature = targetMethod.signature();
            String methodName = targetMethod.name();

            if (i.isStatic()) {
                Class<?> methodClass = resolveClass(targetMethod.holder());
                Method m = null;
                try {
                    m = methodClass.getDeclaredMethod(methodName, resolveSignature(signature));
                } catch (SecurityException e1) {
                    raiseException(i, e1.getCause());
                } catch (NoSuchMethodException e1) {
                    raiseException(i, e1);
                }

                Object res = null;
                try {
                    if (m != null) {
                        m.setAccessible(true);
                        res = m.invoke(null, argumentList(i, signature));
                    } else {
                        throw new Error();
                    }
                } catch (IllegalArgumentException e) {
                    raiseException(i, e.getCause());
                } catch (IllegalAccessException e) {
                    raiseException(i, e.getCause());
                } catch (InvocationTargetException e) {
                    raiseException(i, e.getTargetException());
                }

                environment.bind(i, CiConstant.forBoxed(signature.returnKind(), res), instructionCounter);

            } else {
                // Call init methods
                if ("<init>".equals(methodName) || "<clinit>".equals(methodName)) {
                    Object res = callInitMethod(i);
                    environment.bind(i.arguments()[0], CiConstant.forObject(res), instructionCounter);
                    jumpNextInstruction();
                    return;
                }
                Object objref = environment.lookup((i.arguments()[0])).boxedValue();
                Class<?> methodClass;
                try {
                    methodClass = objref.getClass();
                } catch (NullPointerException ne) {
                    raiseException(i, ne);
                    return;
                }

                Method m = null;
                while (m == null && methodClass != null) {
                    try {
                        m = methodClass.getDeclaredMethod(methodName, resolveSignature(signature));
                    } catch (SecurityException e) {
                        raiseException(i, e);
                        return;
                    } catch (NoSuchMethodException e) {
                        methodClass = methodClass.getSuperclass();
                    }
                }
                if (methodClass == null) {
                    raiseException(i, new NoSuchMethodException());
                    return;
                }

                Object res = null;
                try {
                    if (objref instanceof Class<?> && ("newInstance".equals(methodName) || "newInstance0".equals(methodName))) {
                        res = callInitMethod(i);
                    } else if (m != null) {
                        m.setAccessible(true);
                        res = m.invoke(objref, argumentList(i, signature));
                    }
                } catch (IllegalArgumentException e) {
                    raiseException(i, e.getCause());
                } catch (IllegalAccessException e) {
                    raiseException(i, e.getCause());
                } catch (InvocationTargetException e) {
                    raiseException(i, e.getTargetException());
                }
                environment.bind(i, CiConstant.forBoxed(signature.returnKind(), res), instructionCounter);
            }
            jumpNextInstruction();
        }

        private Object[] argumentList(Invoke i, RiSignature signature) {
            int nargs = signature.argumentCount(false);
            Object[] arglist = new Object[nargs];
            int index = i.isStatic() ? 0 : 1;
            for (int j = 0; j < nargs; j++) {
                CiKind argumentType = signature.argumentKindAt(j);
                arglist[j] = getCompatibleBoxedValue(resolveClass(signature.argumentTypeAt(j, null)), (i.arguments()[index]));
                index += argumentType.sizeInSlots();
            }
            return arglist;
        }

        private Object callInitMethod(Invoke i) {
            Object receiver = environment.lookup(i.arguments()[0]).asObject();
            // at this point, objectRef can represent a class, or a new uninitialized object produced by a NewInstance instruction
            // in both cases, a new object will be allocated and initialized.
            Class<?> javaClass = (receiver instanceof Class<?>) ? (Class <?>) receiver : receiver.getClass();
            Object newReference = null;
            int nargs = i.target().signature().argumentCount(false);
            Object[] arglist = new Object[nargs];
            for (int j = 0; j < nargs; j++) {
                arglist[j] = getCompatibleBoxedValue(resolveClass(i.target().signature().argumentTypeAt(j, null)), i.arguments()[j + 1]); //environment.lookup((i.arguments()[j + 1])).boxedValue();
            }

            Class<?>[] partypes = new Class<?>[i.target().signature().argumentCount(false)];
            for (int j = 0; j < nargs; j++) {
                partypes[j] = resolveClass(i.target().signature().argumentTypeAt(j, null));
            }

            try {
                final Constructor<?> constructor = javaClass.getDeclaredConstructor(partypes);
                constructor.setAccessible(true);
                newReference = constructor.newInstance(arglist);
            } catch (InstantiationException e) {
                raiseException(i, e);
            } catch (InvocationTargetException e) {
                raiseException(i, e.getTargetException());
            } catch (NoSuchMethodException e) {
                raiseException(i, new InstantiationException());
            } catch (Exception e) {
                raiseException(i, new InstantiationException());
            }
            return newReference;
        }

        private Class<?>[] resolveSignature(RiSignature signature) {
            int nargs = signature.argumentCount(false);
            Class<?>[] partypes = new Class<?>[signature.argumentCount(false)];
            for (int j = 0; j < nargs; j++) {
                partypes[j] = resolveClass(signature.argumentTypeAt(j, null));
            }
            return partypes;
        }

        private Class<?> resolveClass(String internalName) {
            Class<?> resolved = null;
            try {
                if (internalName.startsWith("[")) {
                    int arrayDimensions = 0;
                    do {
                        internalName = internalName.substring(1);
                        arrayDimensions++;
                    } while (internalName.startsWith("["));

                    if (internalName.length() == 1) {
                        resolved = CiKind.fromPrimitiveOrVoidTypeChar(internalName.charAt(0)).primitiveArrayClass();
                        arrayDimensions--;
                    } else {
                        String name = internalName.substring(1, internalName.length() - 1).replace('/', '.');
                        resolved = Class.forName(name);
                    }
                    while (arrayDimensions > 0) {
                        resolved = Array.newInstance(resolved, 0).getClass();
                        arrayDimensions--;
                    }
                } else {
                    String name = CiUtil.internalNameToJava(internalName, true);
                    resolved = Class.forName(name);
                }
            } catch (ClassNotFoundException e) {
                throwable = new NoClassDefFoundError(internalName);
            }
            return resolved;
        }

        private Class<?> resolveClass(RiType type) {
            if (type.isResolved()) {
                return type.javaClass();
            } else {
                return resolveClass(type.name());
            }
        }

        Field resolveField(RiField field) {
            Class<?> klass = resolveClass(field.holder());
            try {
                Field f = klass.getDeclaredField(field.name());
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                throw new NoSuchFieldError(klass.getName() + "." + field.name());
            }
        }

        Method resolveMethod(RiMethod method) {
            Class<?> klass = resolveClass(method.holder());
            try {
                Method m = klass.getDeclaredMethod(method.name(), resolveSignature(method.signature()));
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) {
                throw new NoSuchMethodError(klass.getName() + "." + method.name() + method.signature());
            }
        }

        Constructor<?> resolveConstructor(RiMethod constructor) {
            assert constructor.isConstructor();
            Class<?> klass = resolveClass(method.holder());
            try {
                Constructor<?> c = klass.getDeclaredConstructor(resolveSignature(constructor.signature()));
                c.setAccessible(true);
                return c;
            } catch (NoSuchMethodException e) {
                throw new NoSuchMethodError(klass.getName() + ".<init>" + method.signature());
            }
        }

        /**
         * @param i
         * @throws IllegalAccessException
         * @throws InstantiationException
         */
        @Override
        public void visitNewInstance(NewInstance i) {
            RiType type = i.instanceClass();
            Class<?> javaClass = resolveClass(type);
            try {
                // bind the NewInstance instruction to the new allocated object
                environment.bind(i, CiConstant.forObject(unsafe.allocateInstance(javaClass)), instructionCounter);
            } catch (InstantiationException e) {
                raiseException(i, e.getCause());
            }
            jumpNextInstruction();
        }

        @Override
        public void visitNewTypeArray(NewTypeArray i) {
            assertPrimitive(i.elementKind());
            assertKind(i.length().kind, CiKind.Int);
            int length = environment.lookup(i.length()).asInt();
            if (length < 0) {
                raiseException(i, new NegativeArraySizeException());
                return;
            }
            Object newObjectArray = null;
            switch (i.elementKind()) {
                case Boolean:
                    newObjectArray = new boolean[length];
                    break;
                case Byte:
                    newObjectArray = new byte[length];
                    break;
                case Short:
                    newObjectArray = new short[length];
                    break;
                case Int:
                    newObjectArray = new int[length];
                    break;
                case Long:
                    newObjectArray = new long[length];
                    break;
                case Char:
                    newObjectArray = new char[length];
                    break;
                case Float:
                    newObjectArray = new float[length];
                    break;
                case Double:
                    newObjectArray = new double[length];
                    break;
                default:
                    Util.shouldNotReachHere();
            }
            environment.bind(i, CiConstant.forObject(newObjectArray), instructionCounter);
            jumpNextInstruction();
        }

        @Override
        public void visitNewObjectArrayClone(NewObjectArrayClone newObjectArrayClone) {
            throw Util.unimplemented();
        }

        @Override
        public void visitNewObjectArray(NewObjectArray i) {
            int length = environment.lookup(i.length()).asInt();
            if (length < 0) {
                raiseException(i, new NegativeArraySizeException());
                return;
            }
            Object newObjectArray = Array.newInstance(resolveClass(i.elementClass()), length);
            environment.bind(i, CiConstant.forObject(newObjectArray), instructionCounter);
            jumpNextInstruction();
        }

        @Override
        public void visitNewMultiArray(NewMultiArray i) {
            int nDimensions = i.rank();
            assert nDimensions >= 1 : "Number of dimensions in a NewMultiArray must be greater than 1";
            int[] dimensions = new int[nDimensions];
            for (int j = 0; j < nDimensions; j++) {
                dimensions[j] = environment.lookup(i.dimensions()[j]).asInt();
            }

            Object newObjectArray = null;
            try {
                newObjectArray = Array.newInstance(elementTypeNewArray(i), dimensions);
            } catch (NegativeArraySizeException e) {
                raiseException(i, e);
            } catch (Throwable e) {
                raiseException(i, e);
            }
            environment.bind(i, CiConstant.forObject(newObjectArray), instructionCounter);
            jumpNextInstruction();
        }

        private Class<?> elementTypeNewArray(NewMultiArray i) {
            Class<?> elementType = resolveClass(i.elementType());
            while (elementType.isArray()) {
                elementType = elementType.getComponentType();
            }
            return elementType;
        }

        @Override
        public void visitCheckCast(CheckCast i) {
            CiConstant objectRef = environment.lookup(i.object());

            if (objectRef.boxedValue() != null && !resolveClass(i.declaredType()).isInstance(objectRef.boxedValue())) {
                throwable = new ClassCastException("Object reference cannot be cast to the resolved type.");
            }
            environment.bind(i, objectRef, instructionCounter);
            jumpNextInstruction();
        }

        @Override
        public void visitInstanceOf(InstanceOf i) {
            Value object = i.object();
            Object objectRef = environment.lookup(object).asObject();

            if (objectRef == null || !(resolveClass(i.targetClass()).isInstance(objectRef))) {
                environment.bind(i, CiConstant.INT_0, instructionCounter);
            } else {
                environment.bind(i, CiConstant.INT_1, instructionCounter);
            }
            jumpNextInstruction();
        }

        @Override
        public void visitMonitorEnter(MonitorEnter i) {
            try {
                Object obj = environment.lookup(i.object()).asObject();
                unsafe.monitorEnter(obj);
            } catch (NullPointerException e) {
                raiseException(i, e);
            }
            jumpNextInstruction();
        }

        @Override
        public void visitMonitorExit(MonitorExit i) {
            try {
                unsafe.monitorExit(environment.lookup(i.object()).asObject());
            } catch (NullPointerException e) {
                raiseException(i, e);
            } catch (IllegalMonitorStateException e) {
                raiseException(i, e);
            }
            jumpNextInstruction();
        }

        @Override
        public void visitIntrinsic(Intrinsic i) {
            // TODO: execute the intrinsic via reflection
            jumpNextInstruction();
        }

        @Override
        public void visitBlockBegin(BlockBegin i) {
            if (C1XOptions.PrintStateInInterpreter) {
              printInstructionState(currentInstruction, false);
            }
            jumpNextInstruction();
        }

        @Override
        public void visitGoto(Goto i) {
            environment.performPhiMove(i);
            jump(i.suxAt(0));
        }

        @Override
        public void visitIf(If i) {
            final CiConstant x = environment.lookup(i.x());
            final CiConstant y = environment.lookup(i.y());
            int cmp = compareValues(x, y);

            switch (i.condition()) {
                case EQ:
                    if (cmp == 0) {
                        jump(i.successor(true));
                    } else {
                        jump(i.successor(false));
                    }
                    break;

                case NE:
                    if (x.kind.isDouble() && (Double.isNaN(x.asDouble()) || Double.isNaN(y.asDouble()))) {
                        jump(i.unorderedSuccessor());
                    } else if (x.kind.isFloat() && (Float.isNaN(x.asFloat()) || Float.isNaN(y.asFloat()))) {
                        jump(i.unorderedSuccessor());
                    } else if (!(cmp == 0)) {
                        jump(i.successor(true));
                    } else {
                        jump(i.successor(false));
                    }
                    break;

                case GT:
                    if (cmp == 1) {
                        jump(i.successor(true));
                    } else {
                        jump(i.successor(false));
                    }
                    break;

                case GE:
                    if (x.kind.isDouble() && (Double.isNaN(x.asDouble()) || Double.isNaN(y.asDouble()))) {
                        jump(i.unorderedSuccessor());
                    } else if (x.kind.isFloat() && (Float.isNaN(x.asFloat()) || Float.isNaN(y.asFloat()))) {
                        jump(i.unorderedSuccessor());
                    } else if (cmp >= 0) {
                        jump(i.successor(true));
                    } else {
                        jump(i.successor(false));
                    }

                    break;

                case LT:
                    if (cmp == -1) {
                        jump(i.successor(true));
                    } else {
                        jump(i.successor(false));
                    }
                    break;

                case LE:
                    if (x.kind.isDouble() && (Double.isNaN(x.asDouble()) || Double.isNaN(y.asDouble()))) {
                        jump(i.unorderedSuccessor());
                    } else if (x.kind.isFloat() && (Float.isNaN(x.asFloat()) || Float.isNaN(y.asFloat()))) {
                        jump(i.unorderedSuccessor());
                    } else if (cmp <= 0) {
                        jump(i.successor(true));
                    } else {
                        jump(i.successor(false));
                    }
                    break;
            }
            environment.performPhiMove(i);
        }

        private int compareValues(CiConstant x, CiConstant y) {
            if (x.kind.isFloat() && y.kind.isFloat()) {
                return Float.compare(x.asFloat(), y.asFloat());
            } else if (x.kind.isDouble() || y.kind.isDouble()) {
                return Double.compare(x.asDouble(), y.asDouble());
            } else if (x.kind.isLong() || y.kind.isLong()) {
                final long xLong = x.asLong();
                final long yLong = y.asLong();
                if (xLong == yLong) {
                    return 0;
                } else if (xLong > yLong) {
                    return 1;
                } else {
                    return -1;
                }
            } else if (x.kind.isObject()) {
                return x.asObject() == y.asObject() ? 0 : 1;
            } else {
                final int xInt = x.asInt();
                final int yInt = y.kind.isObject() ? (Integer) y.boxedValue() : y.asInt();
                if (xInt == yInt) {
                    return 0;
                } else if (xInt > yInt) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }

        private void jump(BlockBegin successor) {
            if (C1XOptions.PrintStateInInterpreter && currentInstruction.stateAfter() != null) {
                printInstructionState(currentInstruction, true);
            }
            block = successor;
            currentInstruction = block;
        }

        @Override
        public void visitIfInstanceOf(IfInstanceOf i) {
            environment.performPhiMove(i);
            currentInstruction = currentInstruction.next();
        }

        @Override
        public void visitTableSwitch(TableSwitch i) {
            assert i.value().kind == CiKind.Int : "TableSwitch key must be of type int";
            int index = environment.lookup(i.value()).asInt();

            if (index >= i.lowKey() && index < i.highKey()) {
                jump(i.suxAt(index - i.lowKey()));
            } else {
                jump(i.defaultSuccessor());
            }
            environment.performPhiMove(i);
        }

        @Override
        public void visitLookupSwitch(LookupSwitch i) {
            assert i.value().kind == CiKind.Int : "LookupSwitch key must be of type int";
            int key = environment.lookup(i.value()).asInt();
            int succIndex = -1;

            for (int j = 0; j < i.keysLength(); j++) {
                if (i.keyAt(j) == key) {
                    succIndex = j;
                    break;
                }
            }
            if (succIndex != -1) {
                jump(i.suxAt(succIndex));
            } else {
                jump(i.defaultSuccessor());
            }
            environment.performPhiMove(i);
        }

        @Override
        public void visitReturn(Return i) {
            if (i.result() == null) {
                result = null;
                jumpNextInstruction();
                return;
            }
            result = environment.lookup(i.result());
            CiKind returnType = method.signature().returnKind();
            if (returnType == CiKind.Boolean) {
                result = CiConstant.forBoolean(result.asInt() != 0);
            } else if (returnType == CiKind.Int) {
                result = CiConstant.forInt(result.asInt());
            } else if (returnType == CiKind.Char) {
                result = CiConstant.forChar((char) result.asInt());
            }
            jumpNextInstruction();
        }

        @Override
        public void visitThrow(Throw i) {
            try {
                CiConstant exception = environment.lookup(i.exception());
                environment.performPhiMove(i);
                throw (Throwable) exception.asObject();
            } catch (Throwable e) {
                raiseException(i, e);
            }
        }

        @Override
        public void visitBase(Base i) {
            assert block.end().successors().size() == 1 : "Base instruction must have one successor node";
            jump(block.end().successors().get(0));
            environment.performPhiMove(i);
        }

        @Override
        public void visitOsrEntry(OsrEntry i) {
            jumpNextInstruction();
        }

        @Override
        public void visitExceptionObject(ExceptionObject i) {
            jumpNextInstruction();
        }

        @Override
        public void visitUnsafeGetRaw(UnsafeGetRaw i) {
            Object address = environment.lookup(i.base()).asObject();
            long index = i.index() == null ? 0 : environment.lookup(i.index()).asLong();
            Object result = null;
            switch (i.unsafeOpKind) {
                case Boolean:
                    result = unsafe.getBoolean(address, index);
                    break;
                case Byte:
                    result = unsafe.getByte(address, index);
                    break;
                case Short:
                    result = unsafe.getShort(address, index);
                    break;
                case Int:
                    result = unsafe.getInt(address, index);
                    break;
                case Long:
                    result = unsafe.getLong(address, index);
                    break;
                case Float:
                    result = unsafe.getFloat(address, index);
                    break;
                case Double:
                    result = unsafe.getDouble(address, index);
                    break;
                default:
                    fail("Should not reach here");

            }
            environment.bind(i, CiConstant.forBoxed(i.unsafeOpKind, result), instructionCounter);
            jumpNextInstruction();
        }

        @Override
        public void visitUnsafePutRaw(UnsafePutRaw i) {
            Object address = environment.lookup(i.base()).asObject();
            long index = i.index() == null ? 0 : environment.lookup(i.index()).asLong();
            CiConstant value = environment.lookup(i.value());
            switch (i.unsafeOpKind) {
                case Boolean:
                    unsafe.putBoolean(address, index, value.asInt() != 0);
                    break;
                case Byte:
                    unsafe.putByte(address, index, (byte) value.asInt());
                    break;
                case Short:
                    unsafe.putShort(address, index, (short) value.asInt());
                    break;
                case Int:
                    unsafe.putInt(address, index, value.asInt());
                    break;
                case Long:
                    unsafe.putLong(address, index, value.asLong());
                    break;
                case Float:
                    unsafe.putFloat(address, index, value.asFloat());
                    break;
                case Double:
                    unsafe.putDouble(address, index, value.asDouble());
                    break;
                case Word:
                case Object:
                    unsafe.putObject(address, index, value.asObject());
                    break;

                default:
                    fail("Should not reach here");

            }
            jumpNextInstruction();
        }

        @Override
        public void visitUnsafeGetObject(UnsafeGetObject i) {
            Object object = environment.lookup(i.object()).asObject();
            long offset = environment.lookup(i.offset()).asLong();

            Object result = null;
            switch (i.unsafeOpKind) {
                case Boolean:
                    result = unsafe.getBoolean(object, offset);
                    break;
                case Byte:
                    result = unsafe.getByte(object, offset);
                    break;
                case Short:
                    result = unsafe.getShort(object, offset);
                    break;
                case Int:
                    result = unsafe.getInt(object, offset);
                    break;
                case Long:
                    result = unsafe.getLong(object, offset);
                    break;
                case Float:
                    result = unsafe.getFloat(object, offset);
                    break;
                case Double:
                    result = unsafe.getDouble(object, offset);
                    break;
                case Word:
                case Object:
                    result = unsafe.getObject(object, offset);
                    break;

                default:
                    fail("Should not reach here");

            }
            environment.bind(i, CiConstant.forBoxed(i.unsafeOpKind, result), instructionCounter);
            jumpNextInstruction();
        }

        @Override
        public void visitUnsafePutObject(UnsafePutObject i) {
            Object object = environment.lookup(i.object());
            long offset = environment.lookup(i.offset()).asLong();
            CiConstant value = environment.lookup(i.value());

            switch (i.unsafeOpKind) {
                case Boolean:
                    unsafe.putBoolean(object, offset, value.asInt() != 0);
                    break;
                case Byte:
                    unsafe.putByte(object, offset, (byte) value.asInt());
                    break;
                case Short:
                    unsafe.putShort(object, offset, (short) value.asInt());
                    break;
                case Int:
                    unsafe.putInt(object, offset, value.asInt());
                    break;
                case Long:
                    unsafe.putLong(object, offset, value.asLong());
                    break;
                case Float:
                    unsafe.putFloat(object, offset, value.asFloat());
                    break;
                case Double:
                    unsafe.putDouble(object, offset, value.asDouble());
                    break;
                case Word:
                case Object:
                    unsafe.putObject(object, offset, value.asObject());
                    break;

                default:
                    fail("Should not reach here");

            }
            jumpNextInstruction();
        }

        @Override
        public void visitUnsafePrefetchRead(UnsafePrefetchRead i) {
            jumpNextInstruction();
        }

        @Override
        public void visitUnsafePrefetchWrite(UnsafePrefetchWrite i) {
            jumpNextInstruction();
        }

        @Override
        protected void visit(Value value) {
            fail("unimplemented: visiting value of type " + value.getClass().getSimpleName());
        }

        public CiConstant run() throws InvocationTargetException {
            if (C1XOptions.PrintStateInInterpreter) {
                System.out.println("\n********** Running " + CiUtil.toJavaName(method.holder()) + ":" + method.name() + method.signature().toString() + " **********\n");
                System.out.println("Initial state");
                printState(currentInstruction.stateBefore());
                System.out.println("");
            }
            while (currentInstruction != null) {
                currentInstruction.accept(this);
                instructionCounter++;
                if (throwable != null) {
                    throw new InvocationTargetException(throwable);
                }
            }
            if (C1XOptions.PrintStateInInterpreter) {
                System.out.println("********** " + CiUtil.toJavaName(method.holder()) + ":" + method.name() + method.signature().toString() + " ended  **********");
            }
            if (result != null) {
                assert method.signature().returnKind() != CiKind.Void;
                return result;
            } else {
                return CiConstant.NULL_OBJECT;
            }
        }

        public void valuesDo(FrameState state, ValueClosure closure) {
            final int maxLocals = state.localsSize();
            if (maxLocals > 0) {
                System.out.println("** Locals **");
                for (int i = 0; i < maxLocals; i++) {
                    System.out.print("[" + i + "]: ");
                    closure.apply(state.loadLocal(i));
                }
            }
            final int maxStack = state.stackSize();
            if (maxStack > 0) {
                System.out.println("\n** Stack **");
                for (int i = 0; i < maxStack; i++) {
                    System.out.print("[" + i + "]: ");
                    closure.apply(state.stackAt(i));
                }
            }

            final int maxLocks = state.locksSize();
            if (maxLocks > 0) {
                System.out.println("\n** Locks **");
                for (int i = 0; i < maxLocks; i++) {
                    closure.apply(state.lockAt(i));
                }
            }
            FrameState callerState = state.callerState();
            if (callerState != null) {
                System.out.println("\n** Caller state **");
                valuesDo(callerState, closure);
            }
        }

        private void printInstructionState(Instruction instruction, boolean stateAfter) {
            if (stateAfter) {
                System.out.println("instruction: " + instruction.name() + "   ID: " + instruction.id() + "   BCI: " + instruction.bci());
            } else {
                System.out.println("State at " + instruction.name() + "   ID: " + instruction.id());
            }
            if (stateAfter) {
                printState(instruction.stateAfter());
            } else {
                printState(instruction.stateBefore());
            }
            System.out.println();
        }

        private void printState(FrameState newFrameState) {
            valuesDo(newFrameState, new ValueClosure() {

                    public Value apply(Value i) {
                        CiConstant lookupValue;
                        if (i == null) {
                            lookupValue = null;
                        } else {
                            lookupValue = environment.lookup(i);
                        }
                        String output = null;
                        if (lookupValue != null && lookupValue.boxedValue() != null) {
                            Object object = lookupValue.boxedValue();
                            if (lookupValue.kind == CiKind.Object) {
                                output = object.getClass().toString();
                            } else {
                                output = object.toString();
                            }
                        }
                        System.out.println(output);
                        return i;
                    }
            });
        }

        private int compareLongs(long x, long y) {
            if (x == y) {
                return 0;
            } else if (x > y) {
                return 1;
            } else {
                return -1;
            }
        }

        private int compareFloats(int opcode, float x, float y) {
            if (Float.isNaN(x) || Float.isNaN(y)) {
                if (opcode == Bytecodes.FCMPG) {
                    return 1;
                } else if (opcode == Bytecodes.FCMPL) {
                    return -1;
                } else {
                    fail("Illegal opcode for fcmp<op> instruction");
                }
            }
            if (x == y) {
                return 0;
            } else if (x > y) {
                return 1;
            } else {
                return -1;
            }
        }

        private int compareDoubles(int opcode, double x, double y) {
            if (Double.isNaN(x) || Double.isNaN(y)) {
                if (opcode == Bytecodes.DCMPG) {
                    return 1;
                } else if (opcode == Bytecodes.DCMPL) {
                    return -1;
                } else {
                    fail("Illegal opcode for dcmp<op> instruction");
                }
            }
            if (x == y) {
                return 0;
            } else if (x > y) {
                return 1;
            } else {
                return -1;
            }
        }

        private void assertKind(CiKind xval, CiKind yval, CiKind kind) {
            assertKind(xval, kind);
            assertKind(yval, kind);
        }

        private void assertKind(CiKind x, CiKind kind) {
            if (x != kind) {
                if (!(x.isInt() && (kind.isLong() || kind.isInt()))) {
                    throw new CiBailout("Type mismatch");
                }
            }
        }

        private void assertPrimitive(CiKind kind) {
            if (!kind.isPrimitive()) {
                fail("RiType " + kind + " must be a primitive");
            }
        }

        private void assertArrayType(RiType riType) {
            if (riType != null && riType.isResolved()) {
                if (!riType.isArrayClass()) {
                    fail("RiType " + riType + " must be an array class");
                }
            }
        }

        private void fail(String msg) {
            throw new IRInterpreterException(msg);
        }
    }

    public CiConstant execute(IR hir, CiConstant... arguments) throws InvocationTargetException {
        if (isNative(hir.compilation.method.accessFlags())) {
            // TODO: invoke the native method via reflection?
            return null;
        }
        final Evaluator evaluator = new Evaluator(hir, arguments);
        return evaluator.run();
    }

    private static Field findField(Class<?> javaClass, String fieldName) {
        Class<?> c = javaClass;
        while (c != null) {
            try {
                final Field field = c.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException noSuchFieldException) {
                // do nothing
            }
            c = c.getSuperclass();
        }
        accessError(fieldName);
        return null;
    }

    public static Object getStaticField(Class<?> javaClass, String fieldName) {
        final Field field = findField(javaClass, fieldName);
        try {
            return field.get(javaClass);
        } catch (IllegalAccessException illegalAccessException) {
            accessError(field.toString());
            return null;
        }
    }

    private static void accessError(String field) {
        throw new Error("could not access field " + field);
    }

    public static CiConstant fromBoxedJavaValue(Object boxedJavaValue) {
        if (boxedJavaValue == null) {
            return CiConstant.NULL_OBJECT;
        }
        if (boxedJavaValue instanceof Byte) {
            final Byte box = (Byte) boxedJavaValue;
            return CiConstant.forByte(box);
        }
        if (boxedJavaValue instanceof Boolean) {
            final Boolean box = (Boolean) boxedJavaValue;
            return CiConstant.forBoolean(box);
        }
        if (boxedJavaValue instanceof Short) {
            final Short box = (Short) boxedJavaValue;
            return CiConstant.forShort(box);
        }
        if (boxedJavaValue instanceof Character) {
            final Character box = (Character) boxedJavaValue;
            return CiConstant.forChar(box);
        }
        if (boxedJavaValue instanceof Integer) {
            final Integer box = (Integer) boxedJavaValue;
            return CiConstant.forInt(box);
        }
        if (boxedJavaValue instanceof Float) {
            final Float box = (Float) boxedJavaValue;
            return CiConstant.forFloat(box);
        }
        if (boxedJavaValue instanceof Long) {
            final Long box = (Long) boxedJavaValue;
            return CiConstant.forLong(box);
        }
        if (boxedJavaValue instanceof Double) {
            final Double box = (Double) boxedJavaValue;
            return CiConstant.forDouble(box);
        }
        return CiConstant.forObject(boxedJavaValue);
    }
}
