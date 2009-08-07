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
import com.sun.c1x.*;
import com.sun.c1x.bytecode.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.graph.*;
import com.sun.c1x.ir.*;
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
     *
     * @author Marcelo Cintra
     */
    public static class IRInterpreterException extends RuntimeException {

        public static final long serialVersionUID = 8974598793158773L;

        public IRInterpreterException(String msg) {
            super(msg);
        }
    }

    public CiRuntime runtime;
    public InterpreterInterface ii; // TODO: not used yet

    /**
     * Creates a new IRInterpreter for the specified IR.
     *
     * @param ir
     */
    public IRInterpreter(CiRuntime runtime, InterpreterInterface ii) {
        this.runtime = runtime;
        this.ii = ii;
    }

    private class Environment {

        private Map<Instruction, Pair<Integer, ConstType>> instructionTrace = new HashMap<Instruction, Pair<Integer, ConstType>>();

        private class Pair<T, V> {

            T value1;
            V value2;

            public Pair(T value1, V value2) {
                this.value1 = value1;
                this.value2 = value2;
            }
        }

        private class InstructionMapInitializer implements BlockClosure {

            public void apply(BlockBegin block) {
                for (Instruction instr = block; instr != null; instr = instr.next()) {
                    instructionTrace.put(instr, new Pair<Integer, ConstType>(new Integer(-1), null));
                }
            }
        }

        public void bind(Instruction i, ConstType value, Integer counter) {
            instructionTrace.put(i, new Pair<Integer, ConstType>(counter, value));
        }

        public Environment(ValueStack valueStack, ConstType[] values, IR ir) {
            assert values.length <= valueStack.localsSize() : "Incorrect number of initialization arguments";
            ir.startBlock.iteratePreOrder(new InstructionMapInitializer());
            int index = 0;
            for (int i = 0; i < values.length; i++) {
                bind(valueStack.localAt(index), values[i], 0);
                index += values[i].size();
            }
        }

        ConstType lookup(Instruction instruction) {
            if (instruction instanceof Phi) {
                Pair<Integer, ConstType> resultOperand = resolvePhi((Phi) instruction);
                return resultOperand.value2;
            } else if (!(instruction instanceof Constant)) {
                final Pair<Integer, ConstType> result = instructionTrace.get(instruction);
                assert result != null : "Value not defined for instruction: " + instruction;
                return result.value2;
            } else {
                return instruction.type().asConstant();
            }
        }

        /**
         * @param operand
         * @return
         */
        private Pair<Integer, ConstType> resolvePhi(Phi phi) {
            Instruction operand = phi.operandAt(0);
            Pair<Integer, ConstType> resultOperand = resolvePair(operand);

            for (int j = 1; j < phi.operandCount(); j++) {
                Pair<Integer, ConstType> currOperand = resolvePair(phi.operandAt(j));
                if (currOperand.value1.intValue() > resultOperand.value1.intValue()) {
                    resultOperand = currOperand;
                }
            }
            return resultOperand;
        }

        /**
         * @param operand
         */
        private Pair<Integer, ConstType> resolvePair(Instruction operand) {
            if (operand instanceof Phi) {
                return resolvePhi((Phi) operand);
            } else {
                return instructionTrace.get(operand);
            }
        }
    }

    private class Evaluator extends InstructionVisitor {

        private final CiMethod method;
        private BlockBegin block;
        private Instruction currentInstruction;
        private ConstType result;
        private Environment environment;
        private int instructionCounter;


        /**
         * @param method
         * @param executeArguments
         */
        public Evaluator(IR hir, ConstType[] arguments) {
            this.method = hir.compilation.method;
            block = hir.startBlock;
            currentInstruction = hir.startBlock;
            result = null;
            environment = new Environment(hir.startBlock.state(), arguments, hir);
            instructionCounter = 1;

        }

        /**
         * @param i
         */
        @Override
        public void visitPhi(Phi i) {
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitLocal(Local i) {
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitConstant(Constant i) {
            environment.bind(i, i.type().asConstant(), instructionCounter);
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitResolveClass(ResolveClass i) {
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitLoadField(LoadField i) {
            if (i.field().isConstant()) {
                environment.bind(i, ConstType.fromBoxedJavaValue(i.field().constantValue().asObject()), instructionCounter);
            } else {
                try {
                    Class< ? > klass = i.field().holder().javaClass();
                    String name = i.field().name();
                    Field field = klass.getDeclaredField(name);
                    field.setAccessible(true);
                    Object value = field.get(environment.lookup(i.object()).asObject());
                    environment.bind(i, ConstType.fromBoxedJavaValue(value), instructionCounter);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitStoreField(StoreField i) {
            try {
                Class< ? > klass = i.field().holder().javaClass(); // Class.forName(i.getClass().getName());
                Field field = klass.getDeclaredField(i.field().name());
                field.setAccessible(true);
                field.set(environment.lookup(i.object()).asObject(), environment.lookup(i.value()).boxedValue());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitArrayLength(ArrayLength i) {
            assertBasicType(i.array().type(), BasicType.Object);
            assertArrayType(i.array().exactType());
            assertArrayType(i.array().declaredType());
            assertBasicType(i.type(), BasicType.Int);

            ConstType array = environment.lookup(i.array());
            environment.bind(i, new ConstType(BasicType.Int, Array.getLength(array.asObject())), instructionCounter);
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitLoadIndexed(LoadIndexed i) {
            Object array = environment.lookup(i.array()).asObject();
            Object result = Array.get(array, environment.lookup(i.index()).asInt());
            environment.bind(i, ConstType.fromBoxedJavaValue(result), instructionCounter);
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitStoreIndexed(StoreIndexed i) {
            Object array = environment.lookup(i.array()).asObject();
            Array.set(array, environment.lookup(i.index()).asInt(), environment.lookup(i.value()).boxedValue());
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitNegateOp(NegateOp i) {
            ConstType xval = environment.lookup(i.x());
            assertBasicType(i.type(), xval.basicType);

            switch (i.type().basicType) {
                case Int:
                    environment.bind(i, new ConstType(BasicType.Int, -xval.asInt()), instructionCounter);
                    break;
                case Long:
                    environment.bind(i, new ConstType(BasicType.Int, -xval.asLong()), instructionCounter);
                    break;
                case Float:
                    environment.bind(i, new ConstType(BasicType.Int, -xval.asFloat()), instructionCounter);
                    break;
                case Double:
                    environment.bind(i, new ConstType(BasicType.Int, -xval.asDouble()), instructionCounter);
                    break;
                default:
                    Util.shouldNotReachHere();
                    break;
            }
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitArithmeticOp(ArithmeticOp i) {
            ConstType xval = environment.lookup(i.x());
            ConstType yval = environment.lookup(i.y());

            assertBasicType(xval, yval, i.type().basicType);

            switch (i.opcode()) {
                case Bytecodes.IADD:
                    environment.bind(i, new ConstType(BasicType.Int, (xval.asInt() + yval.asInt())), instructionCounter);
                    break;
                case Bytecodes.ISUB:
                    environment.bind(i, new ConstType(BasicType.Int, (xval.asInt() - yval.asInt())), instructionCounter);
                    break;
                case Bytecodes.IMUL:
                    environment.bind(i, new ConstType(BasicType.Int, (xval.asInt() * yval.asInt())), instructionCounter);
                    break;
                case Bytecodes.IDIV:
                    environment.bind(i, new ConstType(BasicType.Int, (xval.asInt() / yval.asInt())), instructionCounter);
                    break;
                case Bytecodes.IREM:
                    environment.bind(i, new ConstType(BasicType.Int, (xval.asInt() % yval.asInt())), instructionCounter);
                    break;

                case Bytecodes.LADD:
                    environment.bind(i, new ConstType(BasicType.Long, (xval.asLong() + yval.asLong())), instructionCounter);
                    break;
                case Bytecodes.LSUB:
                    environment.bind(i, new ConstType(BasicType.Long, (xval.asLong() - yval.asLong())), instructionCounter);
                    break;
                case Bytecodes.LMUL:
                    environment.bind(i, new ConstType(BasicType.Long, (xval.asLong() * yval.asLong())), instructionCounter);
                    break;
                case Bytecodes.LDIV:
                    environment.bind(i, new ConstType(BasicType.Long, (xval.asLong() / yval.asLong())), instructionCounter);
                    break;
                case Bytecodes.LREM:
                    environment.bind(i, new ConstType(BasicType.Long, (xval.asLong() % yval.asLong())), instructionCounter);
                    break;

                case Bytecodes.FADD:
                    environment.bind(i, new ConstType(BasicType.Float, (xval.asFloat() + yval.asFloat())), instructionCounter);
                    break;
                case Bytecodes.FSUB:
                    environment.bind(i, new ConstType(BasicType.Float, (xval.asFloat() - yval.asFloat())), instructionCounter);
                    break;
                case Bytecodes.FMUL:
                    environment.bind(i, new ConstType(BasicType.Float, (xval.asFloat() * yval.asFloat())), instructionCounter);
                    break;
                case Bytecodes.FDIV:
                    environment.bind(i, new ConstType(BasicType.Float, (xval.asFloat() / yval.asFloat())), instructionCounter);
                    break;
                case Bytecodes.FREM:
                    environment.bind(i, new ConstType(BasicType.Float, (xval.asFloat() % yval.asFloat())), instructionCounter);
                    break;

                case Bytecodes.DADD:
                    environment.bind(i, new ConstType(BasicType.Double, (xval.asDouble() + yval.asDouble())), instructionCounter);
                    break;
                case Bytecodes.DSUB:
                    environment.bind(i, new ConstType(BasicType.Double, (xval.asDouble() - yval.asDouble())), instructionCounter);
                    break;
                case Bytecodes.DMUL:
                    environment.bind(i, new ConstType(BasicType.Double, (xval.asDouble() * yval.asDouble())), instructionCounter);
                    break;
                case Bytecodes.DDIV:
                    environment.bind(i, new ConstType(BasicType.Double, (xval.asDouble() / yval.asDouble())), instructionCounter);
                    break;
                case Bytecodes.DREM:
                    environment.bind(i, new ConstType(BasicType.Double, (xval.asDouble() % yval.asDouble())), instructionCounter);
                    break;

                default:
                    Util.shouldNotReachHere();
            }
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitShiftOp(ShiftOp i) {
            ConstType xval = environment.lookup(i.x());
            ConstType yval = environment.lookup(i.y());
            int s;

            switch (i.opcode()) {
                case Bytecodes.ISHL:
                    assertBasicType(xval, yval, BasicType.Int);
                    assert (yval.asInt() < 32) : "Illegal shift constant in a ISH instruction";
                    environment.bind(i, new ConstType(BasicType.Int, (xval.asInt() << (yval.asInt() & 0x1F))), instructionCounter);
                    break;

                case Bytecodes.ISHR:
                    assertBasicType(xval, yval, BasicType.Int);
                    assert (yval.asInt() < 32) : "Illegal shift constant in a ISH instruction";
                    environment.bind(i, new ConstType(BasicType.Int, (xval.asInt() >> (yval.asInt() & 0x1F))), instructionCounter);
                    break;

                case Bytecodes.IUSHR:
                    assertBasicType(xval, yval, BasicType.Int);
                    assert (yval.asInt() < 32) : "Illegal shift constant in a ISH instruction";
                    s = yval.asInt() & 0x1f;
                    int iresult = xval.asInt() >> s;
                    if (xval.asInt() < 0) {
                        iresult = iresult + (2 << ~s);
                    }
                    environment.bind(i, new ConstType(BasicType.Int, iresult), instructionCounter);
                    break;

                case Bytecodes.LSHL:
                    assertBasicType(xval, BasicType.Long);
                    assertBasicType(yval, BasicType.Int);
                    assert (yval.asInt() < 64) : "Illegal shift constant in a ISH instruction";
                    environment.bind(i, new ConstType(BasicType.Long, (xval.asLong() << (yval.asInt() & 0x3F))), instructionCounter);
                    break;

                case Bytecodes.LSHR:
                    assertBasicType(xval, BasicType.Long);
                    assertBasicType(yval, BasicType.Int);
                    assert (yval.asInt() < 64) : "Illegal shift constant in a ISH instruction";
                    environment.bind(i, new ConstType(BasicType.Long, (xval.asLong() >> (yval.asInt() & 0x3F))), instructionCounter);
                    break;

                case Bytecodes.LUSHR:
                    assertBasicType(xval, BasicType.Long);
                    assertBasicType(yval, BasicType.Int);
                    assert (yval.asInt() < 64) : "Illegal shift constant in a ISH instruction";
                    s = yval.asInt() & 0x3f;
                    long lresult = xval.asLong() >> s;
                    if (xval.asLong() < 0) {
                        lresult = lresult + (2L << ~s);
                    }
                    environment.bind(i, new ConstType(BasicType.Long, lresult), instructionCounter);
                    break;
                default:
                    fail("Illegal ShiftOp opcode");
            }
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitLogicOp(LogicOp i) {
            final ConstType xval = environment.lookup(i.x());
            final ConstType yval = environment.lookup(i.y());

            switch (i.opcode()) {
                case Bytecodes.IAND:
                    assertBasicType(xval, yval, BasicType.Int);
                    environment.bind(i, new ConstType(BasicType.Int, (xval.asInt() & yval.asInt())), instructionCounter);
                    break;
                case Bytecodes.IOR:
                    assertBasicType(xval, yval, BasicType.Int);
                    environment.bind(i, new ConstType(BasicType.Int, (xval.asInt() | yval.asInt())), instructionCounter);
                    break;
                case Bytecodes.IXOR:
                    assertBasicType(xval, yval, BasicType.Int);
                    environment.bind(i, new ConstType(BasicType.Int, (xval.asInt() ^ yval.asInt())), instructionCounter);
                    break;
                case Bytecodes.LAND:
                    assertBasicType(xval, yval, BasicType.Long);
                    environment.bind(i, new ConstType(BasicType.Long, (xval.asLong() & yval.asLong())), instructionCounter);
                    break;
                case Bytecodes.LOR:
                    assertBasicType(xval, yval, BasicType.Long);
                    environment.bind(i, new ConstType(BasicType.Long, (xval.asLong() | yval.asLong())), instructionCounter);
                    break;
                case Bytecodes.LXOR:
                    assertBasicType(xval, yval, BasicType.Long);
                    environment.bind(i, new ConstType(BasicType.Long, (xval.asLong() ^ yval.asLong())), instructionCounter);
                    break;
                default:
                    fail("Logic operation instruction has an illegal opcode");
            }
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitCompareOp(CompareOp i) {
            final ConstType xval = environment.lookup(i.x());
            final ConstType yval = environment.lookup(i.y());

            switch (i.opcode()) {
                case Bytecodes.LCMP:
                    environment.bind(i, new ConstType(BasicType.Int, compareLongs(xval.asLong(), yval.asLong())), instructionCounter);
                    break;

                case Bytecodes.FCMPG:
                case Bytecodes.FCMPL:
                    environment.bind(i, new ConstType(BasicType.Int, compareFloats(i.opcode(), xval.asFloat(), yval.asFloat())), instructionCounter);
                    break;

                case Bytecodes.DCMPG:
                case Bytecodes.DCMPL:
                    environment.bind(i, new ConstType(BasicType.Int, compareDoubles(i.opcode(), xval.asDouble(), yval.asDouble())), instructionCounter);
                    break;

                default:
                    fail("Illegal CompareOp opcode");
            }
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitIfOp(IfOp i) {
            final ConstType tval = environment.lookup(i.trueValue());
            final ConstType fval = environment.lookup(i.falseValue());
            final ConstType x = environment.lookup(i.x());
            final ConstType y = environment.lookup(i.y());

            assertBasicType(x, y, i.type().basicType);

            switch (i.condition()) {
                case eql:
                    if (x.equals(y)) {
                        environment.bind(i, tval, instructionCounter);
                    } else {
                        environment.bind(i, fval, instructionCounter);
                    }
                    break;

                case neq:
                    if (!x.equals(y)) {
                        environment.bind(i, tval, instructionCounter);
                    } else {
                        environment.bind(i, fval, instructionCounter);
                    }
                    break;

                case gtr:
                    if (x.asInt() > y.asInt()) {
                        environment.bind(i, tval, instructionCounter);
                    } else {
                        environment.bind(i, fval, instructionCounter);
                    }
                    break;

                case geq:
                    if (x.asInt() >= y.asInt()) {
                        environment.bind(i, tval, instructionCounter);
                    } else {
                        environment.bind(i, fval, instructionCounter);
                    }
                    break;

                case lss:
                    if (x.asInt() < y.asInt()) {
                        environment.bind(i, tval, instructionCounter);
                    } else {
                        environment.bind(i, fval, instructionCounter);
                    }
                    break;

                case leq:
                    if (x.asInt() >= y.asInt()) {
                        environment.bind(i, tval, instructionCounter);
                    } else {
                        environment.bind(i, fval, instructionCounter);
                    }
                    break;
            }
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitConvert(Convert i) {
            final ConstType value = environment.lookup(i.value());
            switch (i.opcode()) {
                case Bytecodes.I2L:
                    assertBasicType(value, BasicType.Int);
                    environment.bind(i, new ConstType(BasicType.Long, (long) value.asInt()), instructionCounter);
                    break;
                case Bytecodes.I2F:
                    assertBasicType(value, BasicType.Int);
                    environment.bind(i, new ConstType(BasicType.Float, (float) value.asInt()), instructionCounter);
                    break;
                case Bytecodes.I2D:
                    assertBasicType(value, BasicType.Int);
                    environment.bind(i, new ConstType(BasicType.Double, (double) value.asInt()), instructionCounter);
                    break;

                case Bytecodes.I2B:
                    assertBasicType(value, BasicType.Int);
                    environment.bind(i, new ConstType(BasicType.Byte, (byte) value.asInt()), instructionCounter);
                    break;
                case Bytecodes.I2C:
                    assertBasicType(value, BasicType.Int);
                    environment.bind(i, new ConstType(BasicType.Char, (char) value.asInt()), instructionCounter);
                    break;
                case Bytecodes.I2S:
                    assertBasicType(value, BasicType.Int);
                    environment.bind(i, new ConstType(BasicType.Short, (short) value.asInt()), instructionCounter);
                    break;

                case Bytecodes.L2I:
                    assertBasicType(value, BasicType.Long);
                    environment.bind(i, new ConstType(BasicType.Int, (int) value.asLong()), instructionCounter);
                    break;
                case Bytecodes.L2F:
                    assertBasicType(value, BasicType.Long);
                    environment.bind(i, new ConstType(BasicType.Float, (float) value.asLong()), instructionCounter);
                    break;
                case Bytecodes.L2D:
                    assertBasicType(value, BasicType.Long);
                    environment.bind(i, new ConstType(BasicType.Double, (double) value.asLong()), instructionCounter);
                    break;

                case Bytecodes.F2I:
                    assertBasicType(value, BasicType.Float);
                    environment.bind(i, new ConstType(BasicType.Int, (int) value.asFloat()), instructionCounter);
                    break;
                case Bytecodes.F2L:
                    assertBasicType(value, BasicType.Float);
                    environment.bind(i, new ConstType(BasicType.Long, (long) value.asFloat()), instructionCounter);
                    break;
                case Bytecodes.F2D:
                    assertBasicType(value, BasicType.Float);
                    environment.bind(i, new ConstType(BasicType.Double, (double) value.asFloat()), instructionCounter);
                    break;

                case Bytecodes.D2I:
                    assertBasicType(value, BasicType.Double);
                    environment.bind(i, new ConstType(BasicType.Int, (int) value.asDouble()), instructionCounter);
                    break;
                case Bytecodes.D2L:
                    assertBasicType(value, BasicType.Double);
                    environment.bind(i, new ConstType(BasicType.Long, (long) value.asDouble()), instructionCounter);
                    break;
                case Bytecodes.D2F:
                    assertBasicType(value, BasicType.Double);
                    environment.bind(i, new ConstType(BasicType.Float, (float) value.asDouble()), instructionCounter);
                    break;

                default:
                    fail("invalid opcode in Convert");
            }
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitNullCheck(NullCheck i) {
            final ConstType object = environment.lookup(i.object());
            assertBasicType(object, BasicType.Object);
            // TODO: Not sure how to store the result
            if (object.isNonNull()) {
                environment.bind(i, new ConstType(BasicType.Object, true), instructionCounter);
            } else {
                environment.bind(i, new ConstType(BasicType.Object, false), instructionCounter);
            }
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitInvoke(Invoke i) {

            if (!i.isStatic()) {
                CiMethod method = i.target();
                Object objref = environment.lookup((i.arguments()[0])).boxedValue();

                int nargs = method.signatureType().argumentCount(false);
                Object[] arglist = new Object[nargs];
                for (int j = 0; j < nargs; j++) {
                    arglist[j] = environment.lookup((i.arguments()[j + 1])).boxedValue();
                }

                Class< ? > methodClass = objref.getClass();
                Class< ? >[] partypes = new Class< ? >[method.signatureType().argumentCount(false)];
                for (int j = 0; j < nargs; j++) {
                    partypes[j] = method.signatureType().argumentTypeAt(j).javaClass();
                }

                Method m = null;
                String methodName = method.name();
                // no need to invoke init methods
                if (methodName.equals("<init>") || methodName.equals("<clinit>")) {
                    currentInstruction = currentInstruction.next();
                    return;
                }

                try {
                    m = methodClass.getDeclaredMethod(method.name(), partypes);
                } catch (SecurityException e1) {
                    e1.printStackTrace();
                } catch (NoSuchMethodException e1) {
                    e1.printStackTrace();
                }
                Object res = null;
                try {
                    if (m != null) {
                        m.setAccessible(true);
                        res = m.invoke(objref, arglist);
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                environment.bind(i, new ConstType(method.signatureType().returnBasicType(), res), instructionCounter);

            } else {
                CiMethod method = i.target();

                int nargs = method.signatureType().argumentCount(false);
                Object[] arglist = new Object[nargs];
                for (int j = 0; j < nargs; j++) {
                    arglist[j] = environment.lookup((i.arguments()[j])).boxedValue();
                }

                Class< ? > methodClass = method.holder().javaClass();
                Class< ? >[] partypes = new Class< ? >[method.signatureType().argumentCount(false)];
                for (int j = 0; j < nargs; j++) {
                    CiType argumentType = method.signatureType().argumentTypeAt(j);
                    if (!argumentType.isLoaded()) {
                        try {
                            String name = argumentType.name();
                            name = name.replace('/', '.');
                            name = name.substring(1, name.length() - 1);
                            partypes[j] = Class.forName(name);
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else {
                        partypes[j] = argumentType.javaClass();
                    }
                }

                Method m = null;
                try {
                    m = methodClass.getDeclaredMethod(method.name(), partypes);
                } catch (SecurityException e1) {
                    e1.printStackTrace();
                } catch (NoSuchMethodException e1) {
                    e1.printStackTrace();
                }

                Object res = null;
                try {
                    if (m != null) {
                        m.setAccessible(true);
                        res = m.invoke(null, arglist);
                    } else {
                        throw new Error();
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

                environment.bind(i, new ConstType(method.signatureType().returnBasicType(), res), instructionCounter);
            }
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         * @throws IllegalAccessException
         * @throws InstantiationException
         */
        @Override
        public void visitNewInstance(NewInstance i) {
            CiType type = i.instanceClass();
            Class <?> javaClass = null;
            Object obj = null;
                if (!type.isLoaded()) {
                    try {
                        String name = type.name();
                        name = name.replace('/', '.');
                        name = name.substring(1, name.length() - 1);
                        javaClass = Class.forName(name);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    javaClass = type.javaClass();
                }

              try {
                  if (javaClass != null) {
                      obj = javaClass.newInstance();
                  } else {
                      throw new Error("Class" + type.name() + " could not be loaded");
                  }
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
             environment.bind(i, new ConstType(BasicType.Object, obj), instructionCounter);
             currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitNewTypeArray(NewTypeArray i) {
            assertPrimitive(i.elementType());
            assertBasicType(i.length().type(), BasicType.Int);
            int length = environment.lookup(i.length()).asInt();
            Object newObjectArray = Array.newInstance(i.elementType().primitiveArrayClass(), length);
            environment.bind(i, new ConstType(BasicType.Object, newObjectArray), instructionCounter);
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitNewObjectArray(NewObjectArray i) {
            int length = environment.lookup(i.length()).asInt();
            Object newObjectArray = Array.newInstance(i.elementClass().javaClass(), length);
            environment.bind(i, new ConstType(BasicType.Object, newObjectArray), instructionCounter);
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitNewMultiArray(NewMultiArray i) {
            int nDimensions = i.rank();
            assert nDimensions >= 1 : "Number of dimensions in a NewMultiArray must be greater than 1";
            int[] dimensions = new int[nDimensions];
            for (int j = 0; j < nDimensions; j++) {
                dimensions[j] = environment.lookup(i.dimensions()[j]).asInt();
            }
            Object newObjectArray = Array.newInstance(i.elementType().javaClass(), dimensions);
            environment.bind(i, new ConstType(BasicType.Object, newObjectArray), instructionCounter);
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitCheckCast(CheckCast i) {
            ConstType objectRef = environment.lookup(i.object());

            if (objectRef != null && i.declaredType().getClass().isInstance(objectRef.asObject())) {
                throw new ClassCastException("Object reference cannot be cast to the resolved type.");
            }
            environment.bind(i, objectRef, instructionCounter);
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitInstanceOf(InstanceOf i) {
            Instruction object = i.object();
            Object objectRef = environment.lookup(object).asObject();

            if (objectRef == null || !(i.targetClass().javaClass().isInstance(objectRef))) {
                environment.bind(i, ConstType.INT_0, instructionCounter);
            } else {
                environment.bind(i, ConstType.INT_1, instructionCounter);
            }
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitMonitorEnter(MonitorEnter i) {
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitMonitorExit(MonitorExit i) {
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitIntrinsic(Intrinsic i) {
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitBlockBegin(BlockBegin i) {
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitGoto(Goto i) {
            jump(i.suxAt(0));
        }

        /**
         * @param i
         */
        @Override
        public void visitIf(If i) {
            final ConstType x = environment.lookup(i.x());
            final ConstType y = environment.lookup(i.y());
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
                    if (x.isDouble() && (Double.isNaN(x.asDouble()) || Double.isNaN(y.asDouble()))) {
                        jump(i.unorderedSuccessor());
                    } else if (x.isFloat() && (Float.isNaN(x.asFloat()) || Float.isNaN(y.asFloat()))) {
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
                    if (x.isDouble() && (Double.isNaN(x.asDouble()) || Double.isNaN(y.asDouble()))) {
                        jump(i.unorderedSuccessor());
                    } else if (x.isFloat() && (Float.isNaN(x.asFloat()) || Float.isNaN(y.asFloat()))) {
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
                    if (x.isDouble() && (Double.isNaN(x.asDouble()) || Double.isNaN(y.asDouble()))) {
                        jump(i.unorderedSuccessor());
                    } else if (x.isFloat() && (Float.isNaN(x.asFloat()) || Float.isNaN(y.asFloat()))) {
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
        }

        private int compareValues(ConstType x, ConstType y) {
            if (x.isFloat() && y.isFloat()) {
                return Float.compare(x.asFloat(), y.asFloat());
            } else if (x.isDouble() || y.isDouble()) {
                return Double.compare(x.asDouble(), y.asDouble());
            } else if (x.isLong() || y.isLong()) {
                final long xLong = x.asLong();
                final long yLong = y.asLong();
                if (xLong == yLong) {
                    return 0;
                } else if (xLong > yLong) {
                    return 1;
                } else {
                    return -1;
                }
            } else if (x.isObject()) {
                return x.asObject() == y.asObject() ? 0 : 1;
            } else {
                final int xInt = x.asInt();
                final int yInt = y.asInt();
                if (xInt == yInt) {
                    return 0;
                } else if (xInt > yInt) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }

        /**
         * @param successor
         */
        private void jump(BlockBegin successor) {
            block = successor;
            currentInstruction = block;
        }

        /**
         * @param i
         */
        @Override
        public void visitIfInstanceOf(IfInstanceOf i) {
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitTableSwitch(TableSwitch i) {
            assert i.value().type().basicType == BasicType.Int : "TableSwitch key must be of type int";
            int index = environment.lookup(i.value()).asInt();

            if (index >= i.lowKey() && index < i.highKey()) {
                jump(i.suxAt(index - i.lowKey()));
            } else {
                jump(i.defaultSuccessor());
            }
        }

        /**
         * @param i
         */
        @Override
        public void visitLookupSwitch(LookupSwitch i) {
            assert i.value().type().basicType == BasicType.Int : "LookupSwitch key must be of type int";
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
        }

        /**
         * @param i
         */
        @Override
        public void visitReturn(Return i) {
            result = environment.lookup(i.result());
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitThrow(Throw i) {
            ConstType exception = environment.lookup(i.exception());

            if (!(exception.asObject() instanceof Throwable)) {
                throw new Error("Exception object must be an instance of class Throwable or of a subclass of Throwable");
            }

            List<ExceptionHandler> exceptionHandlerList = i.exceptionHandlers();

            for (ExceptionHandler e : exceptionHandlerList) {
                if (e.covers(i.bci())) {
                    // TODO: to be completed
                }
            }
            result = exception;
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitBase(Base i) {
            assert block.end().successors().size() == 1 : "Base instruction must have one successor node";
            jump(block.end().successors().get(0));
        }

        /**
         * @param i
         */
        @Override
        public void visitOsrEntry(OsrEntry i) {
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitExceptionObject(ExceptionObject i) {
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitRoundFP(RoundFP i) {
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitUnsafeGetRaw(UnsafeGetRaw i) {
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitUnsafePutRaw(UnsafePutRaw i) {
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitUnsafeGetObject(UnsafeGetObject i) {
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitUnsafePutObject(UnsafePutObject i) {
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitUnsafePrefetchRead(UnsafePrefetchRead i) {
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitUnsafePrefetchWrite(UnsafePrefetchWrite i) {
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitProfileCall(ProfileCall i) {
            currentInstruction = currentInstruction.next();
        }

        /**
         * @param i
         */
        @Override
        public void visitProfileCounter(ProfileCounter i) {
            currentInstruction = currentInstruction.next();
        }

        /**
         * @return
         */
        public ConstType run() {
            while (hasNextInstruction()) {
                currentInstruction.accept(this);
                instructionCounter++;
            }

            if (result != null) {
                assert method.signatureType().returnBasicType() != BasicType.Void;
                // TODO: Need to improve this!
                if (method.signatureType().returnBasicType() == BasicType.Boolean) {
                    result = new ConstType(BasicType.Boolean, new Boolean(result.asInt() == 0 ? false : true));
                }
                return result;
            } else {
                return new ConstType(BasicType.Object, null);
            }
        }

        /**
         * @return
         */
        private boolean hasNextInstruction() {
            return currentInstruction != null;
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

        private void assertBasicType(ValueType xval, ValueType yval, BasicType type) {
            if (!(xval.basicType == type && yval.basicType == type)) {
                throw new Bailout("Type mismatch");
            }
        }

        private void assertBasicType(ValueType x, BasicType type) {
            if (!(x.basicType == type)) {
                throw new Bailout("Type mismatch");
            }
        }

        private void assertPrimitive(BasicType basicType) {
            if (!basicType.isPrimitiveType()) {
                fail("CiType " + basicType + " must be a primitive");
            }
        }

        private void assertArrayType(CiType ciType) {
            if (ciType != null && ciType.isLoaded()) {
                if (!ciType.isArrayKlass()) {
                    fail("CiType " + ciType + " must be an array class");
                }
            }
        }

        private void fail(String msg) {
            throw new IRInterpreterException(msg);
        }
    }

    public ConstType execute(IR hir, ConstType ... arguments) {
        if (hir.compilation.method.isNative()) {
            // TODO: invokes the native method via reflection?
            return null;
        }
        final Evaluator evaluator = new Evaluator(hir, arguments);
        return evaluator.run();
    }
}
