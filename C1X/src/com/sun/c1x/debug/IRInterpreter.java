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
package com.sun.c1x.debug;

import java.lang.reflect.*;
import java.util.*;

import sun.misc.*;

import com.sun.c1x.*;
import com.sun.c1x.bytecode.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.ir.Value.*;
import com.sun.c1x.ri.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 * The <code>IRInterpreter</code> class definition.
 *
 * @author Marcelo Cintra
 *
 */
public class IRInterpreter {

    /**
     * The <code>IRCheckException</code> class is thrown when the IRChecker detects a problem with the IR.
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

            public void apply(BlockBegin block) {
                ValueStack valueStack = block.stateBefore();
                ArrayList<Phi> phis = (ArrayList<Phi>) valueStack.allPhis(block);

                for (Phi phi : phis) {
                    for (int j = 0; j < phi.operandCount(); j++) {
                        Value phiOperand = block.isExceptionEntry() ? phi.operandAt(j) : phi.block().predAt(j).end();
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
                            blockPhiMoves.add(new PhiMove(phi, phi.operandAt(j)));
                        }
                    }
                }
                for (Instruction instr = block; instr != null; instr = instr.next()) {
                    instructionValueMap.put(instr, new Val(-1, null));
                }
            }

            private void addPhiToInstructionList(Phi phiSrc, Phi phi) {
                phiSrc.setFlag(Flag.PhiVisited);
                for (int j = 0; j < phiSrc.operandCount(); j++) {
                    Value phiOperand = phiSrc.operandAt(j);
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

        public Environment(ValueStack valueStack, CiConstant[] values, IR ir) {
            assert values.length <= valueStack.localsSize() : "Incorrect number of initialization arguments";
            ir.startBlock.iteratePreOrder(new ValueMapInitializer());
            int index = 0;

            for (CiConstant value : values) {
                Object obj;
                // These conversions are necessary since the input values are
                // parsed as integers
                Value local = valueStack.localAt(index);
                if (local.type() == CiKind.Float && value.basicType == CiKind.Int) {
                    obj = new Float(value.asInt());
                } else if ((local.type() == CiKind.Double && value.basicType == CiKind.Int)) {
                    obj = new Double(value.asInt());
                } else {
                    obj = value.boxedValue();
                }
                bind(local, new CiConstant(local.type(), obj), 0);
                performPhiMove(local);
                index += local.type().sizeInSlots();
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

    private class Evaluator extends ValueVisitor {

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
            Class <?> javaClass = toJavaClass(i.type);
            environment.bind(i, CiConstant.forObject(javaClass), instructionCounter);
            jumpNextInstruction();
        }

        private void unexpected(Instruction i, Throwable e) {
            List<ExceptionHandler> exceptionHandlerList = i.exceptionHandlers();
            for (ExceptionHandler eh : exceptionHandlerList) {
                if (eh.handler.isCatchAll() || toJavaClass(eh.handler.catchKlass()).isAssignableFrom(e.getClass())) {
                    jump(eh.entryBlock());
                    // bind the exception object to e
                    environment.bind(eh.entryBlock().next(), new CiConstant(CiKind.Object, e), instructionCounter);
                    return;
                }
            }
            environment.bind(i, new CiConstant(CiKind.Object, e), instructionCounter);
            throwable = e;
        }

        @Override
        public void visitLoadField(LoadField i) {
            try {
                Class< ? > klass = toJavaClass(i.field().holder());
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
                environment.bind(i, CiConstant.fromBoxedJavaValue(boxedJavaValue), instructionCounter);
            } catch (Throwable e) {
                unexpected(i, e);
            }
            jumpNextInstruction();
        }

        @Override
        public void visitStoreField(StoreField i) {
            try {
                Class< ? > klass = toJavaClass(i.field().holder());
                Field field = klass.getDeclaredField(i.field().name());
                field.setAccessible(true);
                field.set(environment.lookup(i.object()).asObject(), getCompatibleBoxedValue(toJavaClass(i.field().type()), i.value()));
            } catch (IllegalAccessException e) {
                unexpected(i, e);
            } catch (SecurityException e) {
                unexpected(i, e);
            } catch (NoSuchFieldException e) {
                unexpected(i, e);
            } catch (Throwable e) {
                unexpected(i, e);
            }
            jumpNextInstruction();
        }

        @Override
        public void visitArrayLength(ArrayLength i) {
            assertBasicType(i.array().type(), CiKind.Object);
            assertArrayType(i.array().exactType());
            assertArrayType(i.array().declaredType());
            assertBasicType(i.type(), CiKind.Int);

            try {
                CiConstant array = environment.lookup(i.array());
                environment.bind(i, CiConstant.forInt(Array.getLength(array.asObject())), instructionCounter);
                jumpNextInstruction();
            } catch (NullPointerException ne) {
                unexpected(i, ne);
            }
        }

        @Override
        public void visitLoadIndexed(LoadIndexed i) {
            Object array = environment.lookup(i.array()).asObject();
            try {
                int arrayIndex = environment.lookup(i.index()).asInt();
                if (arrayIndex >= Array.getLength(array)) {
                    unexpected(i, new ArrayIndexOutOfBoundsException());
                    return;
                }
                Object result = Array.get(array, arrayIndex);
                environment.bind(i, CiConstant.fromBoxedJavaValue(result), instructionCounter);
            } catch (NullPointerException e) {
                unexpected(i, e);
            } catch (ArrayIndexOutOfBoundsException e) {
                unexpected(i, e);
            } catch (IllegalArgumentException e) {
                unexpected(i, e);
            } catch (ArrayStoreException e) {
                unexpected(i, e);
            }
            jumpNextInstruction();
        }

        private Object getCompatibleBoxedValue(Class< ? > type, Value value) {
            CiConstant lookupValue = environment.lookup(value);
            if (type == byte.class) {
                assert value.type().basicType == CiKind.Int : "Types are not compatible";
                return new Byte((byte) lookupValue.asInt());
            } else if (type == short.class) {
                assert value.type().basicType == CiKind.Int : "Types are not compatible";
                return new Short((short) lookupValue.asInt());
            } else if (type == char.class) {
                assert value.type().basicType == CiKind.Int : "Types are not compatible";
                return new Character((char) lookupValue.asInt());
            } else if (type == boolean.class) {
                assert value.type().basicType == CiKind.Int : "Types are not compatible";
                return new Boolean(lookupValue.asInt() == 1 ? true : false);
            } else if (type == double.class) {
                if (lookupValue.basicType == CiKind.Int) {
                    return new Double(lookupValue.asInt());
                } else {
                    return lookupValue.boxedValue();
                }
            } else if (type == float.class) {
                CiConstant rvalue = lookupValue;
                if (rvalue.basicType == CiKind.Int) {
                    return new Float(rvalue.asInt());
                } else {
                    return rvalue.boxedValue();
                }
            } else {
                return lookupValue.boxedValue();
            }
        }



        /**
         * @param i
         */
        @Override
        public void visitStoreIndexed(StoreIndexed i) {
            Object array = environment.lookup(i.array()).asObject();

            try {
                Class< ? > componentType = getElementType(array);
                Array.set(array, environment.lookup(i.index()).asInt(), getCompatibleBoxedValue(componentType, i.value()));
            } catch (NullPointerException ne) {
                unexpected(i, ne);
            } catch (IllegalArgumentException ie) {
                unexpected(i, new ArrayStoreException());
            } catch (ArrayIndexOutOfBoundsException ae) {
                unexpected(i, ae);
            }
            jumpNextInstruction();
        }

        private Class< ? > getElementType(Object array) {
            Class <?> elementType = array.getClass().getComponentType();
            while (elementType.isArray()) {
                elementType = elementType.getComponentType();
            }
            return elementType;
        }

        @Override
        public void visitNegateOp(NegateOp i) {
            CiConstant xval = environment.lookup(i.x());
            assertBasicType(i.type(), xval.basicType);

            switch (i.type().basicType) {
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

            assertBasicType(xval.basicType.stackType(), yval.basicType.stackType(), i.type().basicType.stackType());

            switch (i.opcode()) {
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
                        unexpected(i, ae);
                    }
                    break;
                case Bytecodes.IREM:
                    try {
                        environment.bind(i, CiConstant.forInt((xval.asInt() % yval.asInt())), instructionCounter);
                    } catch (ArithmeticException ae) {
                        unexpected(i, ae);
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
                        unexpected(i, ae);
                    }
                    break;
                case Bytecodes.LREM:
                    try {
                        environment.bind(i, CiConstant.forLong((xval.asLong() % yval.asLong())), instructionCounter);
                    } catch (ArithmeticException ae) {
                        unexpected(i, ae);
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

            switch (i.opcode()) {
                case Bytecodes.ISHL:
                    assertBasicType(xval.basicType.stackType(), yval.basicType.stackType(), CiKind.Int);
                    assert (yval.asInt() < 32) : "Illegal shift constant in a ISH instruction";
                    environment.bind(i, CiConstant.forInt((xval.asInt() << (yval.asInt() & 0x1F))), instructionCounter);
                    break;

                case Bytecodes.ISHR:
                    assertBasicType(xval.basicType.stackType(), yval.basicType.stackType(), CiKind.Int);
                    assert (yval.asInt() < 32) : "Illegal shift constant in a ISH instruction";
                    environment.bind(i, CiConstant.forInt((xval.asInt() >> (yval.asInt() & 0x1F))), instructionCounter);
                    break;

                case Bytecodes.IUSHR:
                    assertBasicType(xval.basicType.stackType(), yval.basicType.stackType(), CiKind.Int);
                    assert (yval.asInt() < 32) : "Illegal shift constant in a ISH instruction";
                    s = yval.asInt() & 0x1f;
                    int iresult = xval.asInt() >> s;
                    if (xval.asInt() < 0) {
                        iresult = iresult + (2 << ~s);
                    }
                    environment.bind(i, CiConstant.forInt(iresult), instructionCounter);
                    break;

                case Bytecodes.LSHL:
                    assertBasicType(xval.basicType.stackType(), CiKind.Long);
                    assertBasicType(yval.basicType.stackType(), CiKind.Int);
                    assert (yval.asInt() < 64) : "Illegal shift constant in a ISH instruction";
                    environment.bind(i, CiConstant.forLong((xval.asLong() << (yval.asInt() & 0x3F))), instructionCounter);
                    break;

                case Bytecodes.LSHR:
                    assertBasicType(xval.basicType.stackType(), CiKind.Long);
                    assertBasicType(yval.basicType.stackType(), CiKind.Int);
                    assert (yval.asInt() < 64) : "Illegal shift constant in a ISH instruction";
                    environment.bind(i, CiConstant.forLong((xval.asLong() >> (yval.asInt() & 0x3F))), instructionCounter);
                    break;

                case Bytecodes.LUSHR:
                    assertBasicType(xval.basicType.stackType(), CiKind.Long);
                    assertBasicType(yval.basicType.stackType(), CiKind.Int);
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

            switch (i.opcode()) {
                case Bytecodes.IAND:
                    assertBasicType(xval.basicType.stackType(), yval.basicType.stackType(), CiKind.Int);
                    environment.bind(i, CiConstant.forInt((xval.asInt() & yval.asInt())), instructionCounter);
                    break;
                case Bytecodes.IOR:
                    assertBasicType(xval.basicType.stackType(), yval.basicType.stackType(), CiKind.Int);
                    environment.bind(i, CiConstant.forInt((xval.asInt() | yval.asInt())), instructionCounter);
                    break;
                case Bytecodes.IXOR:
                    assertBasicType(xval.basicType.stackType(), yval.basicType.stackType(), CiKind.Int);
                    environment.bind(i, CiConstant.forInt((xval.asInt() ^ yval.asInt())), instructionCounter);
                    break;
                case Bytecodes.LAND:
                    assertBasicType(xval.basicType.stackType(), yval.basicType.stackType(), CiKind.Long);
                    environment.bind(i, CiConstant.forLong((xval.asLong() & yval.asLong())), instructionCounter);
                    break;
                case Bytecodes.LOR:
                    assertBasicType(xval.basicType.stackType(), yval.basicType.stackType(), CiKind.Long);
                    environment.bind(i, CiConstant.forLong((xval.asLong() | yval.asLong())), instructionCounter);
                    break;
                case Bytecodes.LXOR:
                    assertBasicType(xval.basicType.stackType(), yval.basicType.stackType(), CiKind.Long);
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

            switch (i.opcode()) {
                case Bytecodes.LCMP:
                    environment.bind(i, CiConstant.forInt(compareLongs(xval.asLong(), yval.asLong())), instructionCounter);
                    break;

                case Bytecodes.FCMPG:
                case Bytecodes.FCMPL:
                    environment.bind(i, CiConstant.forInt(compareFloats(i.opcode(), xval.asFloat(), yval.asFloat())), instructionCounter);
                    break;

                case Bytecodes.DCMPG:
                case Bytecodes.DCMPL:
                    environment.bind(i, CiConstant.forInt(compareDoubles(i.opcode(), xval.asDouble(), yval.asDouble())), instructionCounter);
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
                case eql:
                    bindIfOp(i, x.equals(y), tval, fval);
                    break;

                case neq:
                    bindIfOp(i, !x.equals(y), tval, fval);
                    break;

                case gtr:
                    bindIfOp(i, x.asInt() > y.asInt(), tval, fval);
                    break;

                case geq:
                    bindIfOp(i, x.asInt() >= y.asInt(), tval, fval);
                    break;

                case lss:
                    bindIfOp(i, x.asInt() < y.asInt(), tval, fval);
                    break;

                case leq:
                    bindIfOp(i, x.asInt() <= y.asInt(), tval, fval);
                    break;
            }
            jumpNextInstruction();
        }

        /**
         * @param i
         * @param tval
         * @param fval
         * @param x
         * @param y
         */
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
            switch (i.opcode()) {
                case Bytecodes.I2L:
                    assertBasicType(value.basicType, CiKind.Int);
                    environment.bind(i, CiConstant.forLong(value.asInt()), instructionCounter);
                    break;
                case Bytecodes.I2F:
                    assertBasicType(value.basicType, CiKind.Int);
                    environment.bind(i, CiConstant.forFloat(value.asInt()), instructionCounter);
                    break;
                case Bytecodes.I2D:
                    assertBasicType(value.basicType, CiKind.Int);
                    environment.bind(i, CiConstant.forDouble(value.asInt()), instructionCounter);
                    break;

                case Bytecodes.I2B:
                    assertBasicType(value.basicType, CiKind.Int);
                    environment.bind(i, CiConstant.forByte((byte) value.asInt()), instructionCounter);
                    break;
                case Bytecodes.I2C:
                    assertBasicType(value.basicType, CiKind.Int);
                    environment.bind(i, CiConstant.forChar((char) value.asInt()), instructionCounter);
                    break;
                case Bytecodes.I2S:
                    assertBasicType(value.basicType, CiKind.Int);
                    environment.bind(i, CiConstant.forShort((short) value.asInt()), instructionCounter);
                    break;

                case Bytecodes.L2I:
                    assertBasicType(value.basicType, CiKind.Long);
                    environment.bind(i, CiConstant.forInt((int) value.asLong()), instructionCounter);
                    break;
                case Bytecodes.L2F:
                    assertBasicType(value.basicType, CiKind.Long);
                    environment.bind(i, CiConstant.forFloat(value.asLong()), instructionCounter);
                    break;
                case Bytecodes.L2D:
                    assertBasicType(value.basicType, CiKind.Long);
                    environment.bind(i, CiConstant.forDouble(value.asLong()), instructionCounter);
                    break;

                case Bytecodes.F2I:
                    assertBasicType(value.basicType, CiKind.Float);
                    environment.bind(i, CiConstant.forInt((int) value.asFloat()), instructionCounter);
                    break;
                case Bytecodes.F2L:
                    assertBasicType(value.basicType, CiKind.Float);
                    environment.bind(i, CiConstant.forLong((long) value.asFloat()), instructionCounter);
                    break;
                case Bytecodes.F2D:
                    assertBasicType(value.basicType, CiKind.Float);
                    environment.bind(i, CiConstant.forDouble(value.asFloat()), instructionCounter);
                    break;

                case Bytecodes.D2I:
                    assertBasicType(value.basicType, CiKind.Double);
                    environment.bind(i, CiConstant.forInt((int) value.asDouble()), instructionCounter);
                    break;
                case Bytecodes.D2L:
                    assertBasicType(value.basicType, CiKind.Double);
                    environment.bind(i, CiConstant.forLong((long) value.asDouble()), instructionCounter);
                    break;
                case Bytecodes.D2F:
                    assertBasicType(value.basicType, CiKind.Double);
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
            assertBasicType(object.basicType, CiKind.Object);
            if (object.isNonNull()) {
                environment.bind(i, new CiConstant(CiKind.Object, object.boxedValue()), instructionCounter);
            } else {
                unexpected(i, new NullPointerException());
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
            if (methodName.equals("<init>") || methodName.equals("<clinit>")) {
                Object res = callInitMethod(i);
                environment.bind(i.arguments()[0], new CiConstant(CiKind.Object, res), instructionCounter);
                jumpNextInstruction();
                return;
            }
            if (!targetMethod.isLoaded()) {
                switch (i.opcode()) {
                    case Bytecodes.INVOKEINTERFACE:
                        targetMethod = i.constantPool.resolveInvokeInterface(i.cpi);
                        break;
                    case Bytecodes.INVOKESPECIAL:
                        targetMethod = i.constantPool.resolveInvokeSpecial(i.cpi);
                        break;
                    case Bytecodes.INVOKESTATIC:
                        targetMethod = i.constantPool.resolveInvokeStatic(i.cpi);
                        break;
                    case Bytecodes.INVOKEVIRTUAL:
                        targetMethod = i.constantPool.resolveInvokeVirtual(i.cpi);
                        break;
                }
            }
            // native methods are invoked using reflection.
            // some special methods/classes are also always called using reflection
            if (targetMethod.isNative() || methodName.equals("newInstance") || methodName.equals("newInstance0") ||
                targetMethod.holder().javaClass().getName().startsWith("sun.reflect.Unsafe")                     ||
                targetMethod.holder().javaClass().getName().startsWith("sun.reflect.Reflection")                 ||
                targetMethod.holder().javaClass().getName().startsWith("sun.reflect.FieldAccessor")) {
                invokeUsingReflection(i);
                return;
            }

            switch (i.opcode()) {
                case Bytecodes.INVOKEINTERFACE: {
                    RiType type = null;
                    try {
                        Class< ? > objClass = environment.lookup(i.arguments()[0]).boxedValue().getClass();
                        type = runtime.getRiType(objClass);
                    } catch (NullPointerException ne) {
                        unexpected(i, ne);
                        return;
                    }
                    targetMethod = type.resolveMethodImpl(targetMethod);
                    break;
                }

                case Bytecodes.INVOKEVIRTUAL: {
                    RiType type = null;
                    try {
                        Class< ? > objClass = environment.lookup(i.arguments()[0]).boxedValue().getClass();
                        type = runtime.getRiType(objClass);
                    } catch (NullPointerException ne) {
                        unexpected(i, ne);
                        return;
                    }
                    targetMethod = type.resolveMethodImpl(targetMethod);
                    break;
                }
            }
            if (!i.isStatic()) {
                if (environment.lookup(i.arguments()[0]).boxedValue() == null) {
                    unexpected(i, new NullPointerException());
                    return;
                }
            }

            CiConstant result;
            try {
                IR methodHir = compiledMethods.get(targetMethod.holder().javaClass().getName() + methodName + targetMethod.signatureType().toString());
                if (methodHir == null) {
                    C1XCompilation compilation = new C1XCompilation(compiler, compiler.target, runtime, null, targetMethod);
                    methodHir = compilation.emitHIR();
                    compiledMethods.put(targetMethod.holder().javaClass().getName() + methodName + targetMethod.signatureType().toString(), methodHir);
                }
                result = interpreter.execute(methodHir, arguments(i));
                environment.bind(i, CiConstant.fromBoxedJavaValue(result.boxedValue()), instructionCounter);
            } catch (InvocationTargetException e) {
                unexpected(i, e.getTargetException());
            } catch (CiBailout e) {
                unexpected(i, e.getCause());
            } catch (StackOverflowError e) {
                unexpected(i, e);
            }
            jumpNextInstruction();
        }

        private CiConstant [] arguments(Invoke i) {
            RiSignature signature = i.target().signatureType();

            int nargs = signature.argumentCount(!i.isStatic());
            CiConstant[] arglist = new CiConstant[nargs];
            int index = 0;
            if (i.isStatic()) {
                index = 0;
                for (int j = 0; j < nargs; j++) {
                    CiKind argumentType = signature.argumentTypeAt(j).basicType();
                    arglist[j] = getCompatibleCiConstant(toJavaClass(signature.argumentTypeAt(j)), (i.arguments()[index])); //environment.lookup(i.arguments()[index]);
                    index += argumentType.sizeInSlots();
                }
            } else {
                arglist[0] = environment.lookup(i.receiver());
                index = 1;
                for (int j = 1; j < nargs; j++) {
                    CiKind argumentType = signature.argumentTypeAt(j - 1).basicType();
                    arglist[j] = getCompatibleCiConstant(toJavaClass(signature.argumentTypeAt(j - 1)), (i.arguments()[index])); //environment.lookup(i.arguments()[index]);
                    index += argumentType.sizeInSlots();
                }
            }
            return arglist;
        }
        private CiConstant getCompatibleCiConstant(Class< ? > arrayType, Value value) {
            if (arrayType == byte.class) {
                assert value.type().basicType == CiKind.Int : "Types are not compatible";
                return CiConstant.forByte((byte) environment.lookup(value).asInt());
            } else if (arrayType == short.class) {
                assert value.type().basicType == CiKind.Int : "Types are not compatible";
                return CiConstant.forShort((short) environment.lookup(value).asInt());
            } else if (arrayType == char.class) {
                assert value.type().basicType == CiKind.Int : "Types are not compatible";
                return CiConstant.forChar((char) environment.lookup(value).asInt());
            } else if (arrayType == boolean.class) {
                assert value.type().basicType == CiKind.Int : "Types are not compatible";
                return CiConstant.forBoolean(environment.lookup(value).asInt() == 0 ? false : true);
            } else if (arrayType == double.class) {
                CiConstant rvalue = environment.lookup(value);
                if (rvalue.basicType == CiKind.Int) {
                    return CiConstant.forDouble(rvalue.asInt());
                } else {
                    return rvalue;
                }
            } else if (arrayType == float.class) {
                CiConstant rvalue = environment.lookup(value);
                if (rvalue.basicType == CiKind.Int) {
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
            RiSignature signature = targetMethod.signatureType();
            String methodName = targetMethod.name();

            if (i.isStatic()) {
                Class< ? > methodClass = toJavaClass(targetMethod.holder());
                Method m = null;
                try {
                    m = methodClass.getDeclaredMethod(methodName, toJavaSignature(signature));
                } catch (SecurityException e1) {
                    unexpected(i, e1.getCause());
                } catch (NoSuchMethodException e1) {
                    unexpected(i, e1);
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
                    unexpected(i, e.getCause());
                } catch (IllegalAccessException e) {
                    unexpected(i, e.getCause());
                } catch (InvocationTargetException e) {
                    unexpected(i, e.getTargetException());
                }

                environment.bind(i, new CiConstant(signature.returnBasicType(), res), instructionCounter);

            } else {
                // Call init methods
                if (methodName.equals("<init>") || methodName.equals("<clinit>")) {
                    Object res = callInitMethod(i);
                    environment.bind(i.arguments()[0], new CiConstant(CiKind.Object, res), instructionCounter);
                    jumpNextInstruction();
                    return;
                }
                Object objref = environment.lookup((i.arguments()[0])).boxedValue();
                Class< ? > methodClass = null;
                try {
                    methodClass = objref.getClass();
                } catch (NullPointerException ne) {
                    unexpected(i, ne);
                    return;
                }

                Method m = null;
                while (m == null && methodClass != null) {
                    try {
                        m = methodClass.getDeclaredMethod(methodName, toJavaSignature(signature));
                    } catch (SecurityException e) {
                        unexpected(i, e);
                        return;
                    } catch (NoSuchMethodException e) {
                        methodClass = methodClass.getSuperclass();
                    }
                }
                if (methodClass == null) {
                    unexpected(i, new NoSuchMethodException());
                    return;
                }

                Object res = null;
                try {
                    if (objref instanceof Class< ? > && (methodName.equals("newInstance") || methodName.equals("newInstance0"))) {
                        res = callInitMethod(i);
                    } else if (m != null) {
                        m.setAccessible(true);
                        res = m.invoke(objref, argumentList(i, signature));
                    }
                } catch (IllegalArgumentException e) {
                    unexpected(i, e.getCause());
                } catch (IllegalAccessException e) {
                    unexpected(i, e.getCause());
                } catch (InvocationTargetException e) {
                    unexpected(i, e.getTargetException());
                }
                environment.bind(i, new CiConstant(signature.returnBasicType(), res), instructionCounter);
            }
            jumpNextInstruction();
        }

        private Object[] argumentList(Invoke i, RiSignature signature) {
            int nargs = signature.argumentCount(false);
            Object[] arglist = new Object[nargs];
            int index = 0;
            if (i.isStatic()) {
                index = 0;
            } else {
                index = 1;
            }
            for (int j = 0; j < nargs; j++) {
                CiKind argumentType = signature.argumentTypeAt(j).basicType();
                arglist[j] = getCompatibleBoxedValue(toJavaClass(signature.argumentTypeAt(j)), (i.arguments()[index]));
                index += argumentType.sizeInSlots();
            }
            return arglist;
        }

        private Object callInitMethod(Invoke i) {
            Object objectRef = environment.lookup(i.arguments()[0]).boxedValue();
            // at this point, objectRef can represent a class, or a new uninitialized object produced by a NewInstance instruction
            // in both cases, a new object will be allocated and initialized.
            Class< ? > javaClass = (objectRef instanceof Class<?>) ? (Class <?>) objectRef : objectRef.getClass();
            Object newReference = null;
            int nargs = i.target().signatureType().argumentCount(false);
            Object[] arglist = new Object[nargs];
            for (int j = 0; j < nargs; j++) {
                arglist[j] = getCompatibleBoxedValue(toJavaClass(i.target().signatureType().argumentTypeAt(j)), i.arguments()[j + 1]); //environment.lookup((i.arguments()[j + 1])).boxedValue();
            }

            Class< ? >[] partypes = new Class< ? >[i.target().signatureType().argumentCount(false)];
            for (int j = 0; j < nargs; j++) {
                partypes[j] = toJavaClass(i.target().signatureType().argumentTypeAt(j));
            }

            try {
                final Constructor< ? > constructor = javaClass.getDeclaredConstructor(partypes);
                constructor.setAccessible(true);
                newReference = constructor.newInstance(arglist);
            } catch (InstantiationException e) {
                unexpected(i, e);
            } catch (InvocationTargetException e) {
                unexpected(i, e.getTargetException());
            } catch (NoSuchMethodException e) {
                unexpected(i, new InstantiationException());
            } catch (Exception e) {
                unexpected(i, new InstantiationException());
            }
            return newReference;
        }

        private Class< ? >[] toJavaSignature(RiSignature signature) {
            int nargs = signature.argumentCount(false);
            Class< ? >[] partypes = new Class< ? >[signature.argumentCount(false)];
            for (int j = 0; j < nargs; j++) {
                partypes[j] = toJavaClass(signature.argumentTypeAt(j));
            }
            return partypes;
        }

        private Class< ? > toJavaClass(RiType type) {
            Class< ? > resolved = null;
            if (type.isLoaded()) {
                resolved = type.javaClass();
            } else {
                try {
                    String internalName = type.name();
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
                        String name = Util.toJavaName(type);
                        resolved = Class.forName(name);
                    }
                } catch (ClassNotFoundException e) {
                    throwable = e;
                }
            }
            return resolved;
        }

        /**
         * @param i
         * @throws IllegalAccessException
         * @throws InstantiationException
         */
        @Override
        public void visitNewInstance(NewInstance i) {
            RiType type = i.instanceClass();
            Class< ? > javaClass = toJavaClass(type);
            try {
                // bind the NewInstance instruction to the new allocated object
                environment.bind(i, new CiConstant(CiKind.Object, unsafe.allocateInstance(javaClass)), instructionCounter);
            } catch (InstantiationException e) {
                unexpected(i, e.getCause());
            }
            jumpNextInstruction();
        }

        @Override
        public void visitNewTypeArray(NewTypeArray i) {
            assertPrimitive(i.elementType());
            assertBasicType(i.length().type(), CiKind.Int);
            int length = environment.lookup(i.length()).asInt();
            if (length < 0) {
                unexpected(i, new NegativeArraySizeException());
                return;
            }
            Object newObjectArray = null;
            switch (i.elementType()) {
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
            environment.bind(i, new CiConstant(CiKind.Object, newObjectArray), instructionCounter);
            jumpNextInstruction();
        }

        @Override
        public void visitNewObjectArray(NewObjectArray i) {
            int length = environment.lookup(i.length()).asInt();
            if (length < 0) {
                unexpected(i, new NegativeArraySizeException());
                return;
            }
            Object newObjectArray = Array.newInstance(toJavaClass(i.elementClass()), length);
            environment.bind(i, new CiConstant(CiKind.Object, newObjectArray), instructionCounter);
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
                unexpected(i, e);
            } catch (Throwable e) {
                unexpected(i, e);
            }
            environment.bind(i, new CiConstant(CiKind.Object, newObjectArray), instructionCounter);
            jumpNextInstruction();
        }

        /**
         * @param i
         * @return
         */
        private Class<?> elementTypeNewArray(NewMultiArray i) {
            Class<?> elementType = toJavaClass(i.elementType());
            while (elementType.isArray()) {
                elementType = elementType.getComponentType();
            }
            return elementType;
        }

        @Override
        public void visitCheckCast(CheckCast i) {
            CiConstant objectRef = environment.lookup(i.object());

            if (objectRef.boxedValue() != null && !toJavaClass(i.declaredType()).isInstance(objectRef.boxedValue())) {
                throwable = new ClassCastException("Object reference cannot be cast to the resolved type.");
            }
            environment.bind(i, objectRef, instructionCounter);
            jumpNextInstruction();
        }

        @Override
        public void visitInstanceOf(InstanceOf i) {
            Value object = i.object();
            Object objectRef = environment.lookup(object).asObject();

            if (objectRef == null || !(toJavaClass(i.targetClass()).isInstance(objectRef))) {
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
                unexpected(i, e);
            }
            jumpNextInstruction();
        }

        @Override
        public void visitMonitorExit(MonitorExit i) {
            try {
                unsafe.monitorExit(environment.lookup(i.object()).asObject());
            } catch (NullPointerException e) {
                unexpected(i, e);
            } catch (IllegalMonitorStateException e) {
                unexpected(i, e);
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
                case eql:
                    if (cmp == 0) {
                        jump(i.successor(true));
                    } else {
                        jump(i.successor(false));
                    }
                    break;

                case neq:
                    if (x.basicType.isDouble() && (Double.isNaN(x.asDouble()) || Double.isNaN(y.asDouble()))) {
                        jump(i.unorderedSuccessor());
                    } else if (x.basicType.isFloat() && (Float.isNaN(x.asFloat()) || Float.isNaN(y.asFloat()))) {
                        jump(i.unorderedSuccessor());
                    } else if (!(cmp == 0)) {
                        jump(i.successor(true));
                    } else {
                        jump(i.successor(false));
                    }
                    break;

                case gtr:
                    if (cmp == 1) {
                        jump(i.successor(true));
                    } else {
                        jump(i.successor(false));
                    }
                    break;

                case geq:
                    if (x.basicType.isDouble() && (Double.isNaN(x.asDouble()) || Double.isNaN(y.asDouble()))) {
                        jump(i.unorderedSuccessor());
                    } else if (x.basicType.isFloat() && (Float.isNaN(x.asFloat()) || Float.isNaN(y.asFloat()))) {
                        jump(i.unorderedSuccessor());
                    } else if (cmp >= 0) {
                        jump(i.successor(true));
                    } else {
                        jump(i.successor(false));
                    }

                    break;

                case lss:
                    if (cmp == -1) {
                        jump(i.successor(true));
                    } else {
                        jump(i.successor(false));
                    }
                    break;

                case leq:
                    if (x.basicType.isDouble() && (Double.isNaN(x.asDouble()) || Double.isNaN(y.asDouble()))) {
                        jump(i.unorderedSuccessor());
                    } else if (x.basicType.isFloat() && (Float.isNaN(x.asFloat()) || Float.isNaN(y.asFloat()))) {
                        jump(i.unorderedSuccessor());
                    } else if (cmp <= 0) {
                        jump(i.successor(true));
                    } else {
                        jump(i.successor(false));
                    }
                    break;
                default:
                    Util.shouldNotReachHere();
            }
            environment.performPhiMove(i);
        }

        private int compareValues(CiConstant x, CiConstant y) {
            if (x.basicType.isFloat() && y.basicType.isFloat()) {
                return Float.compare(x.asFloat(), y.asFloat());
            } else if (x.basicType.isDouble() || y.basicType.isDouble()) {
                return Double.compare(x.asDouble(), y.asDouble());
            } else if (x.basicType.isLong() || y.basicType.isLong()) {
                final long xLong = x.asLong();
                final long yLong = y.asLong();
                if (xLong == yLong) {
                    return 0;
                } else if (xLong > yLong) {
                    return 1;
                } else {
                    return -1;
                }
            } else if (x.basicType.isObject()) {
                return x.asObject() == y.asObject() ? 0 : 1;
            } else {
                final int xInt = x.asInt();
                final int yInt = y.basicType.isObject() ? (Integer) y.boxedValue() : y.asInt();
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
            assert i.value().type().basicType == CiKind.Int : "TableSwitch key must be of type int";
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
            assert i.value().type().basicType == CiKind.Int : "LookupSwitch key must be of type int";
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
            CiKind returnType = method.signatureType().returnBasicType();
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
                unexpected(i, e);
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
        public void visitRoundFP(RoundFP i) {
            jumpNextInstruction();
        }

        @Override
        public void visitUnsafeGetRaw(UnsafeGetRaw i) {
            Object address = environment.lookup(i.base()).asObject();
            long index = i.index() == null ? 0 : environment.lookup(i.index()).asLong();
            Object result = null;
            switch (i.basicType()) {
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
            environment.bind(i, new CiConstant(i.basicType(), result), instructionCounter);
            jumpNextInstruction();
        }

        @Override
        public void visitUnsafePutRaw(UnsafePutRaw i) {
            Object address = environment.lookup(i.base()).asObject();
            long index = i.index() == null ? 0 : environment.lookup(i.index()).asLong();
            CiConstant value = environment.lookup(i.value());
            switch (i.basicType()) {
                case Boolean:
                    unsafe.putBoolean(address, index, value.asInt() == 0 ? false : true);
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
            switch (i.basicType()) {
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
            environment.bind(i, new CiConstant(i.basicType(), result), instructionCounter);
            jumpNextInstruction();
        }

        @Override
        public void visitUnsafePutObject(UnsafePutObject i) {
            Object object = environment.lookup(i.object());
            long offset = environment.lookup(i.offset()).asLong();
            CiConstant value = environment.lookup(i.value());

            switch (i.basicType()) {
                case Boolean:
                    unsafe.putBoolean(object, offset, value.asInt() == 0 ? false : true);
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
        public void visitProfileCall(ProfileCall i) {
            jumpNextInstruction();
        }

        @Override
        public void visitProfileCounter(ProfileCounter i) {
            jumpNextInstruction();
        }

        /**
         * @return
         */
        public CiConstant run() throws InvocationTargetException {
            if (C1XOptions.PrintStateInInterpreter) {
                System.out.println("\n********** Running " + Util.toJavaName(method.holder()) + ":" + method.name() + method.signatureType().toString() + " **********\n");
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
                System.out.println("********** " + Util.toJavaName(method.holder()) + ":" + method.name() + method.signatureType().toString() + " ended  **********");
            }
            if (result != null) {
                assert method.signatureType().returnBasicType() != CiKind.Void;
                return result;
            } else {
                return CiConstant.NULL_OBJECT;
            }
        }

        public void valuesDo(ValueStack valueStack, ValueClosure closure) {
            final int maxLocals = valueStack.localsSize();
            if (maxLocals > 0) {
                System.out.println("** Locals **");
                for (int i = 0; i < maxLocals; i++) {
                    System.out.print("[" + i + "]: ");
                    closure.apply(valueStack.loadLocal(i));
                }
            }
            final int maxStack = valueStack.stackSize();
            if (maxStack > 0) {
                System.out.println("\n** Stack **");
                for (int i = 0; i < maxStack; i++) {
                    System.out.print("[" + i + "]: ");
                    closure.apply(valueStack.stackAt(i));
                }
            }

            final int maxLocks = valueStack.locksSize();
            if (maxLocks > 0) {
                System.out.println("\n** Locks **");
                for (int i = 0; i < maxLocks; i++) {
                    closure.apply(valueStack.lockAt(i));
                }
            }
            ValueStack state = valueStack.scope().callerState();
            if (state != null) {
                System.out.println("\n** Caller state **");
                valuesDo(state, closure);
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

        private void printState(ValueStack state) {
            valuesDo(state, new ValueClosure() {

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
                            if (lookupValue.basicType == CiKind.Object) {
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

        private void assertBasicType(CiKind xval, CiKind yval, CiKind type) {
            assertBasicType(xval, type);
            assertBasicType(yval, type);
        }

        private void assertBasicType(CiKind x, CiKind type) {
            if (x != type) {
                if (!(x.isInt() && (type.isLong() || type.isInt()))) {
                    throw new CiBailout("Type mismatch");
                }
            }
        }

        private void assertPrimitive(CiKind basicType) {
            if (!basicType.isPrimitive()) {
                fail("RiType " + basicType + " must be a primitive");
            }
        }

        private void assertArrayType(RiType riType) {
            if (riType != null && riType.isLoaded()) {
                if (!riType.isArrayKlass()) {
                    fail("RiType " + riType + " must be an array class");
                }
            }
        }

        private void fail(String msg) {
            throw new IRInterpreterException(msg);
        }
    }

    public CiConstant execute(IR hir, CiConstant... arguments) throws InvocationTargetException {
        if (hir.compilation.method.isNative()) {
            // TODO: invoke the native method via reflection?
            return null;
        }
        final Evaluator evaluator = new Evaluator(hir, arguments);
        return evaluator.run();
    }

    private static Field findField(Class< ? > javaClass, String fieldName) {
        Class< ? > c = javaClass;
        while (c != null) {
            try {
                final Field field = c.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException noSuchFieldException) {
            }
            c = c.getSuperclass();
        }
        accessError(fieldName);
        return null;
    }

    public static Object getStaticField(Class< ? > javaClass, String fieldName) {
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
}
