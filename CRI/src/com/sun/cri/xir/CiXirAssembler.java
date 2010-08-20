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
import com.sun.cri.ci.CiAddress.*;
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
    protected final List<XirLabel> labels = new ArrayList<XirLabel>(5);
    protected final List<XirParameter> parameters = new ArrayList<XirParameter>(5);
    protected final List<XirTemp> temps = new ArrayList<XirTemp>(5);
    protected final List<XirConstant> constants = new ArrayList<XirConstant>(5);
    protected final List<XirMark> marks = new ArrayList<XirMark>(5);

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

        /**
         * The scaling factor for the scaled-index part of an address computation.
         */
        public final Scale scale;

        /**
         * The constant byte-sized displacement part of an address computation.
         */
        public final int disp;
        
        /**
         * Determines if the memory access through the address can trap.
         */
        public final boolean canTrap;

        private AddressAccessInformation(boolean canTrap) {
            this.canTrap = canTrap;
            this.scale = Scale.Times1;
            this.disp = 0;
        }

        private AddressAccessInformation(boolean canTrap, int disp) {
            this.canTrap = canTrap;
            this.scale = Scale.Times1;
            this.disp = disp;
        }

        private AddressAccessInformation(boolean canTrap, int disp, Scale scale) {
            this.canTrap = canTrap;
            this.scale = scale;
            this.disp = disp;
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

    public static final XirOperand VOID = null;
    
    /**
     * Operands for {@link XirInstruction instructions}.
     * There are three basic variants, {@link XirConstant constant}, {@link XirParameter parameter} and {@link XirTemp}.
     */
    public static abstract class XirOperand {
        
        public final CiKind kind;
        
        /**
         * Unique id in range {@code 0} to {@link #variableCount variableCount - 1}.
         */
        public final int index;
        
        /**
         * Value whose {@link #toString()} method provides a name for this operand.
         */
        public final Object name;

        public XirOperand(CiXirAssembler asm, Object name, CiKind kind) {
            this.kind = kind;
            this.name = name;
            this.index = asm.variableCount++;
        }

        @Override
        public String toString() {
            return String.valueOf(name);
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
    public static class XirParameter extends XirOperand {
        /**
         * Unique id in range {@code 0} to {@code parameters.Size()  - 1}.
         */
        public final int parameterIndex;

        XirParameter(CiXirAssembler asm, String name, CiKind kind) {
            super(asm, name, kind);
            this.parameterIndex = asm.parameters.size();
            asm.parameters.add(this);
        }

    }

    public static class XirConstantParameter extends XirParameter implements XirConstantOperand {
        XirConstantParameter(CiXirAssembler asm, String name, CiKind kind) {
            super(asm, name, kind);
        }

        public int getIndex() {
            return index;
        }
    }

    public static class XirVariableParameter extends XirParameter {
        XirVariableParameter(CiXirAssembler asm, String name, CiKind kind) {
            super(asm, name, kind);
        }
    }

    public static class XirConstant extends XirOperand implements XirConstantOperand {
        public final CiConstant value;

        XirConstant(CiXirAssembler asm, CiConstant value) {
            super(asm, value, value.kind);
            this.value = value;
        }
        
        public int getIndex() {
            return index;
        }
    }

    public static class XirTemp extends XirOperand {
        XirTemp(CiXirAssembler asm, String name, CiKind kind) {
            super(asm, name, kind);
        }
    }

    public static class XirRegister extends XirTemp {
        public final CiValue register;

        XirRegister(CiXirAssembler asm, String name, CiRegisterValue register) {
            super(asm, name, register.kind);
            this.register = register;
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
        resultOperand = new XirTemp(this, "result", kind);
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
        marks.clear();
    }

    /**
     * Represents an XIR instruction, characterized by an {@link XirOp operation}, a {@link CiKind kind}, an optional {@link XirOperand result}, a variable number of {@link XirOperand arguments},
     * and some optional instruction-specific state. The {@link #x}, {@link #y} and {@link #z} methods are convenient ways to access the first, second and third
     * arguments, respectively. Only the {@link XirOp#CallStub} and {@link XirOp#CallRuntime} instructions can have more than three arguments.
     *
     */
    public static class XirInstruction {
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
     * These marks let the RiXirGenerator mark positions in the generated native code and bring them in relationship with on another.
     * This is necessary for code patching, etc.
     */
    public static class XirMark {
        public final XirMark[] references;
        public final Object id;
        
        // special mark used to refer to the actual call site of an invoke
        public static final XirMark CALLSITE = new XirMark(null);

        public XirMark(Object id, XirMark... references) {
            this.id = id;
            this.references = references;
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
         * Load value at an effective address defined by base {@code x} and either a scaled index {@code y} plus displacement
         * or an offset {@code y} and put the result in {@code r}.
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
        Bind,
        /**
         * Record a safepoint.
         */
        Safepoint,
        /**
         * Align the code following this instruction to a multiple of (int)extra.
         */
        Align,
        /**
         * Creates the stack frame for the method. This is also where stack banging is implemented.
         */
        PushFrame,
        /**
         * Removes the stack frame of the method.
         */
        PopFrame,
        /**
         * Inserts an array of bytes directly into the code output.
         */
        RawBytes,
        /**
         * Pushes a value onto the stack.
         */
        Push,
        /**
         * Pops a value from the stack.
         */
        Pop,
        /**
         * Marks a position in the generated native code.
         */
        Mark,
        /**
         * This instruction should never be reached, this is useful for debugging purposes.
         */
         ShouldNotReachHere
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
        append(new XirInstruction(CiKind.Object, NullCheck, VOID, pointer));
    }

    public void pload(CiKind kind, XirOperand result, XirOperand pointer, boolean canTrap) {
        append(new XirInstruction(kind, canTrap, PointerLoad, result, pointer));
    }

    public void pstore(CiKind kind, XirOperand pointer, XirOperand value, boolean canTrap) {
        append(new XirInstruction(kind, canTrap, PointerStore, null, pointer, value));
    }

    public void pload(CiKind kind, XirOperand result, XirOperand pointer, XirOperand offset, boolean canTrap) {
        append(new XirInstruction(kind, new AddressAccessInformation(canTrap), PointerLoadDisp, result, pointer, offset));
    }

    public void pstore(CiKind kind, XirOperand pointer, XirOperand offset, XirOperand value, boolean canTrap) {
        append(new XirInstruction(kind, new AddressAccessInformation(canTrap), PointerStoreDisp, VOID, pointer, offset, value));
    }

    public void pload(CiKind kind, XirOperand result, XirOperand pointer, XirOperand index, int disp, Scale scale,  boolean canTrap) {
        append(new XirInstruction(kind, new AddressAccessInformation(canTrap, disp, scale), PointerLoadDisp, result, pointer, index));
    }

    public void pstore(CiKind kind, XirOperand pointer, XirOperand index, XirOperand value, int disp, Scale scale, boolean canTrap) {
        append(new XirInstruction(kind, new AddressAccessInformation(canTrap, disp, scale), PointerStoreDisp, VOID, pointer, index, value));
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
    
    public void safepoint() {
        append(new XirInstruction(CiKind.Void, null, Safepoint, null));
    }

    public void align(int multiple) {
        assert multiple > 0;
        append(new XirInstruction(CiKind.Void, multiple, Align, null));
    }
    
    public void pushFrame() {
        append(new XirInstruction(CiKind.Void, null, PushFrame, null));
    }

    public void popFrame() {
        append(new XirInstruction(CiKind.Void, null, PopFrame, null));
    }

    public void rawBytes(byte[] bytes) {
        append(new XirInstruction(CiKind.Void, bytes, RawBytes, null));
    }
    
    public void push(XirOperand value) {
        append(new XirInstruction(CiKind.Void, Push, VOID, value));
    }
    
    public void pop(XirOperand result) {
        append(new XirInstruction(result.kind, Pop, result));
    }
    
    public XirMark mark(Object id, XirMark... references) {
        XirMark mark = new XirMark(id, references);
        marks.add(mark);
        append(new XirInstruction(CiKind.Void, mark, Mark, null));
        return mark;
    }
    
    public void shouldNotReachHere() {
        append(new XirInstruction(CiKind.Void, null, ShouldNotReachHere, null));
    }

    public void shouldNotReachHere(String message) {
        append(new XirInstruction(CiKind.Void, message, ShouldNotReachHere, null));
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
        return new XirVariableParameter(this, name, kind);
    }

    /**
     * Creates an {@link XirConstantParameter constant input parameter}  of given name and {@link CiKind kind}.
     * @param name a name for the parameter
     * @param kind the parameter kind
     * @return the  {@link XirConstantParameter}
     */
    public XirConstantParameter createConstantInputParameter(String name, CiKind kind) {
        assert !finished;
        return new XirConstantParameter(this, name, kind);
    }

    public XirConstant createConstant(CiConstant constant) {
        assert !finished;
        XirConstant temp = new XirConstant(this, constant);
        constants.add(temp);
        return temp;
    }

    public XirOperand createTemp(String name, CiKind kind) {
        assert !finished;
        XirTemp temp = new XirTemp(this, name, kind);
        temps.add(temp);
        return temp;
    }

    public XirOperand createRegister(String name, CiKind kind, CiRegister register) {
        assert !finished;
        XirRegister fixed = new XirRegister(this, name, register.asValue(kind));
        temps.add(fixed);
        return fixed;
    }

    public XirConstant i(int b) {
        return createConstant(CiConstant.forInt(b));
    }

    public XirConstant b(boolean t) {
        return createConstant(CiConstant.forBoolean(t));
    }

    public XirConstant w(long b) {
        return createConstant(CiConstant.forWord(b));
    }

    public XirConstant o(Object obj) {
        return createConstant(CiConstant.forObject(obj));
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
