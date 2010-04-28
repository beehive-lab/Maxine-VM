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
package com.sun.cri.xir;

import static com.sun.cri.xir.CiXirAssembler.XirOp.*;

import java.util.*;

import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * Represents an assembler that allows a client such as the runtime system to
 * create {@link XirTemplate XIR templates}.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public abstract class CiXirAssembler {

    protected XirOperand resultOperand;
    protected boolean allocateResultOperand;

    protected final List<XirInstruction> instructions = new ArrayList<XirInstruction>();
    protected final List<XirLabel> labels = new ArrayList<XirLabel>();
    protected final List<XirParameter> parameters = new ArrayList<XirParameter>();
    protected final List<XirTemp> temps = new ArrayList<XirTemp>();
    protected final List<XirConstant> constants = new ArrayList<XirConstant>();

    /**
     * Increases by one for every {@link XirOperand operand} created.
     */
    protected int variableCount;
    /**
     * Marks the assembly complete.
     */
    protected boolean finished = true;

    /**
     * Represents additional address calculation information.
     */
    public static class AddressAccessInformation {

        public final XirConstantOperand scaling;
        public final XirConstantOperand offset;
        public final boolean canTrap;

        private AddressAccessInformation(boolean canTrap) {
            this.canTrap = canTrap;
            this.scaling = null;
            this.offset = null;
        }

        private AddressAccessInformation(boolean canTrap, XirConstantOperand offset) {
            this.canTrap = canTrap;
            this.scaling = null;
            this.offset = offset;
        }

        private AddressAccessInformation(boolean canTrap, XirConstantOperand offset, XirConstantOperand scaling) {
            this.canTrap = canTrap;
            this.scaling = scaling;
            this.offset = offset;
        }
    }

    /**
     * A label that is the target of a control flow instruction.
     */
    public static class XirLabel {
        public final String name;
        public final int index;
        /**
         * If {@code true} the label is to an instruction in the fast path sequence, otherwise to the slow path.
         */
        public final boolean inline;

        private XirLabel(String name, int index, boolean inline) {
            this.name = name;
            this.index = index;
            this.inline = inline;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Tagging interface that indicates that an {@link XirOperand} is a constant.
     */
    public interface XirConstantOperand {
        int getIndex();
    }

    /**
     * Operands for {@link XirInstruction instructions}.
     * There are three basic variants, {@link XirConstant constant}, {@link XirParameter parameter} and {@link XirTemp}.
     */
    public abstract class XirOperand {
        public final CiKind kind;
        /**
         * Unique id in range {@code 0} to {@link #variableCount variableCount - 1}.
         */
        public final int index;
        public final String name;

        public XirOperand(String name, CiKind kind) {
            this.kind = kind;
            this.name = name;
            this.index = variableCount++;
        }

        /**
         * Gets the index, necessary for objects only known to be of type {@link XirConstantOperand}.
         * @return the index
         */
        public int getIndex() {
            return index;
        }

        @Override
        public String toString() {
            return name;
        }

        public String detailedToString() {

            StringBuffer sb = new StringBuffer();

            sb.append(name);
            sb.append('$');
            sb.append(kind.typeChar);
            return sb.toString();
        }
    }

    /**
     * Parameters to {@link XirTemplate templates}.
     */
    public class XirParameter extends XirOperand {
        /**
         * Unique id in range {@code 0} to {@code parameters.Size()  - 1}.
         */
        public final int parameterIndex;

        XirParameter(String name, CiKind kind) {
            super(name, kind);
            this.parameterIndex = parameters.size();
            parameters.add(this);
        }

    }

    public class XirConstantParameter extends XirParameter implements XirConstantOperand {
        XirConstantParameter(String name, CiKind kind) {
            super(name, kind);
        }
    }

    public class XirVariableParameter extends XirParameter {
        XirVariableParameter(String name, CiKind kind) {
            super(name, kind);
        }
    }

    public class XirConstant extends XirOperand implements XirConstantOperand {
        public final CiConstant value;

        XirConstant(String name, CiConstant value) {
            super(name, value.kind);
            this.value = value;
        }
    }

    public class XirTemp extends XirOperand {
        XirTemp(String name, CiKind kind) {
            super(name, kind);
        }
    }

    public class XirFixed extends XirTemp {
        public final CiValue location;

        XirFixed(String name, CiValue location) {
            super(name, location.kind);
            this.location = location;
        }
    }

    /**
     * Start a new assembly with no initial {@link #resultOperand result operand}.
     */
    public void restart() {
        reset();
        resultOperand = null;
    }

    /**
     * Start a new assembly with a {@link #resultOperand result operand} of type {@code kind}.
     * @param kind the result kind
     * @return an {@code XirOperand} for the result operand
     */
    public XirOperand restart(CiKind kind) {
        reset();
        resultOperand = new XirTemp("result", kind);
        allocateResultOperand = true;
        return resultOperand;
    }

    /**
     * Reset the state of the class to the initial conditions to facilitate a new assembly.
     */
    private void reset() {
        assert finished : "must be finished before!";
        variableCount = 0;
        allocateResultOperand = false;
        finished = false;
        instructions.clear();
        labels.clear();
        parameters.clear();
        temps.clear();
        constants.clear();
    }

    /**
     * Represents an XIR instruction, characterized by an {@link XirOp operation}, a {@link CiKind kind}, an optional {@link XirOperand result}, a variable number of {@link XirOperand arguments},
     * and some optional instruction-specific state. The {@link #x}, {@link #y} and {@link #z} methods are convenient ways to access the first, second and third
     * arguments, respectively. Only the {@link XirOp#CallStub} and {@link XirOp#CallRuntime} instructions can have more than three arguments.
     *
     */
    public class XirInstruction {
        /**
         * The {@link CiKind kind} of values the instruction operates on.
         */
        public final CiKind kind;
        /**
         * The {@link XirOp operation}.
         */
        public final XirOp op;
        /**
         * The result, if any.
         */
        public final XirOperand result;
        /**
         * The arguments.
         */
        public final XirOperand[] arguments;
        /**
         * Arbitrary additional data associated with the instruction.
         */
        public final Object extra;

        public XirInstruction(CiKind kind, XirOp op, XirOperand result, XirOperand... arguments) {
            this(kind, null, op, result, arguments);
        }

        public XirInstruction(CiKind kind, Object extra, XirOp op, XirOperand result, XirOperand... arguments) {
            this.extra = extra;
            this.kind = kind;
            this.op = op;
            this.result = result;
            this.arguments = arguments;
        }

        public XirOperand x() {
            assert arguments.length > 0 : "no x operand for this instruction";
            return arguments[0];
        }

        public XirOperand y() {
            assert arguments.length > 1 : "no y operand for this instruction";
            return arguments[1];
        }

        public XirOperand z() {
            assert arguments.length > 2 : "no z operand for this instruction";
            return arguments[2];
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();

            if (result != null) {
                sb.append(result.toString());
                sb.append(" = ");
            }

            sb.append(op.name());

            if (kind != CiKind.Void) {
                sb.append('$');
                sb.append(kind.typeChar);
            }

            if (arguments != null && arguments.length > 0) {
                sb.append("(");

                for (int i = 0; i < arguments.length; i++) {
                    if (i != 0) {
                        sb.append(", ");
                    }
                    sb.append(arguments[i]);
                }

                sb.append(")");
            }

            if (extra != null) {
                sb.append(" ");
                sb.append(extra);
            }

            return sb.toString();
        }
    }

    /**
     * The set of opcodes for XIR instructions.
     * {@link XirInstruction} defines {@code x}, {@code y} and {@code z} as the first, second and third arguments, respectively.
     * We use these mnemonics, plus {@code args} for the complete set of arguments, {@code r} for the result, and {@code extra}
     * for the instruction-specific extra data, in the opcode specifications. Note that the opcodes that operate on values do not directly
     * specify the size (kind) of the data operated on;  this is is encoded in {@link XirInstruction#kind}.
     * Note: If the instruction kind differs from the argument/result kinds, the behavior is undefined.
     *
     */
    public enum XirOp {
        /**
         * Move {@code x} to {@code r}.
         */
        Mov,
        /**
         * Add {@code y} to {@code x} and put the result in {@code r}.
         */
        Add,
        /**
         * Subtract {@code y} from {@code x} and put the result in {@code r}.
         */
        Sub,
        /**
         * Divide {@code y} by {@code x} and put the result in {@code r}.
         */
        Div,
        /**
         * Multiply {@code y} by {@code x} and put the result in {@code r}.
         */
        Mul,
        /**
         * {@code y} modulus {@code x} and put the result in {@code r}.
         */
        Mod,
        /**
         * Shift  {@code y} left by {@code x} and put the result in {@code r}.
         */
        Shl,
        /**
         * Shift  {@code y} right by {@code x} and put the result in {@code r}.
         */
        Shr,
        /**
         * And {@code y} by {@code x} and put the result in {@code r}.
         */
        And,
        /**
         * Or {@code y} by {@code x} and put the result in {@code r}.
         */
        Or,
        /**
         * Exclusive Or {@code y} by {@code x} and put the result in {@code r}.
         */
        Xor,
        /**
         * Null check on {@code x}.
         */
        NullCheck,
        /**
         * Load value at address {@code x} and put the result in {@code r}.
         */
        PointerLoad,
        /**
         * Store {@code y} at address {@code x}.
         */
        PointerStore,
        /**
         * Load value at address defined by base {@code x} and index {@code y} and put the result in {@code r}.
         */
        PointerLoadDisp,
        /**
         * Store {@code z} at address defined by base {@code x} and index {@code y}.
         */
        PointerStoreDisp,
        /**
         * TBD.
         */
        PointerCAS,
        /**
         * Call the {@link XirTemplate.GlobalFlags#GLOBAL_STUB shared stub} defined by {@code extra} with {@code args} and put the result in {@code r}.
         */
        CallStub,
        /**
         * Call the {@link RiMethod} defined by {@code extra}  with {@code args} and put the result in {@code r}.
         */
        CallRuntime,
        /**
         * Transfer control to the instruction at the {@link XirLabel label} identified by {@code extra}.
         */
        Jmp,
        /**
         * If {@code x == y}, transfer control to the instruction at the {@link XirLabel label} identified by {@code extra}.
         */
        Jeq,
        /**
         * If {@code x != y}, transfer control to the instruction at the {@link XirLabel label} identified by {@code extra}.
         */
        Jneq,
        /**
         * If {@code x > y}, transfer control to the instruction at the {@link XirLabel label} identified by {@code extra}.
         */
        Jgt,
        /**
         * If {@code x >= y}, transfer control to the instruction at the {@link XirLabel label} identified by {@code extra}.
         */
        Jgteq,
        /**
         * If {@code x unsigned >= y}, transfer control to the instruction at the {@link XirLabel label} identified by {@code extra}.
         */
        Jugteq,
        /**
         * If {@code x < y}, transfer control to the instruction at the {@link XirLabel label} identified by {@code extra}.
         */
        Jlt,
        /**
         * If {@code x <= y}, transfer control to the instruction at the {@link XirLabel label} identified by {@code extra}.
         */
        Jlteq,
        /**
         * Bind the {@link XirLabel label} identified by {@code extra} to the current instruction and update any references to it.
         * A label may be bound more than once to the same location.
         */
        Bind
    }

    private void append(XirInstruction xirInstruction) {
        assert !finished : "no instructions can be added to finished template";
        instructions.add(xirInstruction);
    }

    public XirLabel createInlineLabel(String name) {
        final XirLabel result = new XirLabel(name, this.labels.size(), true);
        labels.add(result);
        return result;
    }

    public XirLabel createOutOfLineLabel(String name) {
        final XirLabel result = new XirLabel(name, this.labels.size(), false);
        labels.add(result);
        return result;
    }

    public void mov(XirOperand result, XirOperand a) {
        append(new XirInstruction(result.kind, Mov, result, a));
    }

    public void add(XirOperand result, XirOperand a, XirOperand b) {
        append(new XirInstruction(result.kind, Add, result, a, b));
    }

    public void sub(XirOperand result, XirOperand a, XirOperand b) {
        append(new XirInstruction(result.kind, Sub, result, a, b));
    }

    public void div(XirOperand result, XirOperand a, XirOperand b) {
        append(new XirInstruction(result.kind, Div, result, a, b));
    }

    public void mul(XirOperand result, XirOperand a, XirOperand b) {
        append(new XirInstruction(result.kind, Mul, result, a, b));
    }

    public void mod(XirOperand result, XirOperand a, XirOperand b) {
        append(new XirInstruction(result.kind, Mod, result, a, b));
    }

    public void shl(XirOperand result, XirOperand a, XirOperand b) {
        append(new XirInstruction(result.kind, Shl, result, a, b));
    }

    public void shr(XirOperand result, XirOperand a, XirOperand b) {
        append(new XirInstruction(result.kind, Shr, result, a, b));
    }

    public void and(XirOperand result, XirOperand a, XirOperand b) {
        append(new XirInstruction(result.kind, And, result, a, b));
    }

    public void or(XirOperand result, XirOperand a, XirOperand b) {
        append(new XirInstruction(result.kind, Or, result, a, b));
    }

    public void xor(XirOperand result, XirOperand a, XirOperand b) {
        append(new XirInstruction(result.kind, Xor, result, a, b));
    }

    public void nullCheck(XirOperand pointer) {
        append(new XirInstruction(CiKind.Object, NullCheck, (XirOperand) null, pointer));
    }

    public void pload(CiKind kind, XirOperand result, XirOperand pointer, boolean canTrap) {
        append(new XirInstruction(kind, canTrap, PointerLoad, result, pointer));
    }

    public void pstore(CiKind kind, XirOperand pointer, XirOperand value, boolean canTrap) {
        append(new XirInstruction(kind, canTrap, PointerStore, null, pointer, value));
    }

    public void pload(CiKind kind, XirOperand result, XirOperand pointer, XirOperand disp, boolean canTrap) {
        append(new XirInstruction(kind, new AddressAccessInformation(canTrap), PointerLoadDisp, result, pointer, disp));
    }

    public void pstore(CiKind kind, XirOperand pointer, XirOperand disp, XirOperand value, boolean canTrap) {
        append(new XirInstruction(kind, new AddressAccessInformation(canTrap), PointerStoreDisp, (XirOperand) null, pointer, disp, value));
    }

    public void pload(CiKind kind, XirOperand result, XirOperand pointer, XirOperand disp, XirConstantOperand offset, boolean canTrap) {
        append(new XirInstruction(kind, new AddressAccessInformation(canTrap, offset), PointerLoadDisp, result, pointer, disp));
    }

    public void pstore(CiKind kind, XirOperand pointer, XirOperand disp, XirOperand value, XirConstantOperand offset, boolean canTrap) {
        append(new XirInstruction(kind, new AddressAccessInformation(canTrap, offset), PointerStoreDisp, (XirOperand) null, pointer, disp, value));
    }

    public void pload(CiKind kind, XirOperand result, XirOperand pointer, XirOperand disp, XirConstantOperand offset, XirConstantOperand scaling,  boolean canTrap) {
        append(new XirInstruction(kind, new AddressAccessInformation(canTrap, offset, scaling), PointerLoadDisp, result, pointer, disp));
    }

    public void pstore(CiKind kind, XirOperand pointer, XirOperand disp, XirOperand value, XirConstantOperand offset, XirConstantOperand scaling, boolean canTrap) {
        append(new XirInstruction(kind, new AddressAccessInformation(canTrap, offset, scaling), PointerStoreDisp, (XirOperand) null, pointer, disp, value));
    }

    public void pcas(CiKind kind, XirOperand result, XirOperand pointer, XirOperand value, XirOperand expectedValue) {
        append(new XirInstruction(kind, PointerLoad, result, pointer, value, expectedValue));
    }

    public void jmp(XirLabel l) {
        append(new XirInstruction(CiKind.Void, l, Jmp, null));
    }

    public void jeq(XirLabel l, XirOperand a, XirOperand b) {
        jcc(Jeq, l, a, b);
    }

    private void jcc(XirOp op, XirLabel l, XirOperand a, XirOperand b) {
        append(new XirInstruction(CiKind.Void, l, op, null, a, b));
    }

    public void jneq(XirLabel l, XirOperand a, XirOperand b) {
        jcc(Jneq, l, a, b);
    }

    public void jgt(XirLabel l, XirOperand a, XirOperand b) {
        jcc(Jgt, l, a, b);
    }

    public void jgteq(XirLabel l, XirOperand a, XirOperand b) {
        jcc(Jgteq, l, a, b);
    }

    public void jugteq(XirLabel l, XirOperand a, XirOperand b) {
        jcc(Jugteq, l, a, b);
    }

    public void jlt(XirLabel l, XirOperand a, XirOperand b) {
        jcc(Jlt, l, a, b);
    }

    public void jlteq(XirLabel l, XirOperand a, XirOperand b) {
        jcc(Jlteq, l, a, b);
    }

    public void bindInline(XirLabel l) {
        assert l.inline;
        append(new XirInstruction(CiKind.Void, l, Bind, null));
    }

    public void bindOutOfLine(XirLabel l) {
        assert !l.inline;
        append(new XirInstruction(CiKind.Void, l, Bind, null));
    }

    public void callStub(XirTemplate stub, XirOperand result, XirOperand... args) {
        CiKind resultKind = result == null ? CiKind.Void : result.kind;
        append(new XirInstruction(resultKind, stub, CallStub, result, args));
    }

    public void callRuntime(Object rt, XirOperand result, XirOperand... args) {
        CiKind resultKind = result == null ? CiKind.Void : result.kind;
        append(new XirInstruction(resultKind, rt, CallRuntime, result, args));
    }

    /**
     * Terminates the assembly, checking invariants, in particular that {@link resultOperand} is set, and setting {@link #finished} to {@code true}.
     */
    private void end() {
        assert !finished : "template may only be finished once!";
        assert resultOperand != null : "result operand should be set";
        finished = true;
    }

    /**
     * Creates an {@link XirVariableParameter variable input parameter}  of given name and {@link CiKind kind}.
     * @param name a name for the parameter
     * @param kind the parameter kind
     * @return the  {@link XirVariableParameter}
     */
    public XirVariableParameter createInputParameter(String name, CiKind kind) {
        assert !finished;
        return new XirVariableParameter(name, kind);
    }

    /**
     * Creates an {@link XirConstantParameter constant input parameter}  of given name and {@link CiKind kind}.
     * @param name a name for the parameter
     * @param kind the parameter kind
     * @return the  {@link XirConstantParameter}
     */
    public XirConstantParameter createConstantInputParameter(String name, CiKind kind) {
        assert !finished;
        return new XirConstantParameter(name, kind);
    }

    public XirConstant createConstant(String name, CiConstant constant) {
        assert !finished;
        XirConstant temp = new XirConstant(name, constant);
        constants.add(temp);
        return temp;
    }

    public XirOperand createTemp(String name, CiKind kind) {
        assert !finished;
        XirTemp temp = new XirTemp(name, kind);
        temps.add(temp);
        return temp;
    }

    public XirOperand createRegister(String name, CiKind kind, CiRegister register) {
        assert !finished;
        XirFixed fixed = new XirFixed(name, register.asValue(kind));
        temps.add(fixed);
        return fixed;
    }

    public XirConstant i(int b) {
        return i(Integer.toString(b), b);
    }

    public XirConstant b(boolean t) {
        return b(Boolean.toString(t), t);
    }

    public XirConstant w(long b) {
        return w(Long.toString(b), b);
    }

    public XirConstant o(Object obj) {
        return o("" + obj, obj);
    }

    public XirConstant i(String name, int b) {
        return createConstant(name, CiConstant.forInt(b));
    }

    public XirConstant b(String name, boolean t) {
        return createConstant(name, CiConstant.forBoolean(t));
    }

    public XirConstant w(String name, long b) {
        return createConstant(name, CiConstant.forWord(b));
    }

    public XirConstant o(String name, Object obj) {
        return createConstant(name, CiConstant.forObject(obj));
    }

    /**
     * Finishes the assembly of a non-stub template, providing the {@link #resultOperand} and constructs the {@link XirTemplate}.
     * @param result the {@link XirOperand} to be set as the {@link #resultOperand}
     * @param name the name of the template
     * @return the generated template
     */
    public XirTemplate finishTemplate(XirOperand result, String name) {
        assert this.resultOperand == null;
        assert result != null;
        this.resultOperand = result;
        final XirTemplate template = buildTemplate(name, false);
        end();
        return template;
    }

    /**
     * Finishes the assembly of a non-stub template and constructs the {@link XirTemplate}.
     * @param name the name of the template
     * @return the generated template
     */
    public XirTemplate finishTemplate(String name) {
        final XirTemplate template = buildTemplate(name, false);
        end();
        return template;
    }

    /**
     * Finishes the assembly of a {@link XirTemplate.GlobalFlags#GLOBAL_STUB stub} and constructs the {@link XirTemplate}.
     * @param name the name of the template
     * @return the generated template
     */
    public XirTemplate finishStub(String name) {
        final XirTemplate template = buildTemplate(name, true);
        end();
        return template;
    }

    /**
     * Builds the {@link XirTemplate} from the assembly state in this object.
     * The actual assembly is dependent on the target architecture and implemented
     * in a concrete subclass.
     * @param name the name of the template
     * @param isStub {@code true} if the template represents a {@link XirTemplate.GlobalFlags#GLOBAL_STUB stub}
     * @return the generated template
     */
    protected abstract XirTemplate buildTemplate(String name, boolean isStub);

    public abstract CiXirAssembler copy();

}
