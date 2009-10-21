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
package com.sun.c1x.xir;

import java.util.ArrayList;
import java.util.List;

import com.sun.c1x.ci.CiConstant;
import com.sun.c1x.ci.CiKind;
import com.sun.c1x.ci.CiLocation;
import com.sun.c1x.ci.CiRegister;

/**
 * This class represents an assembler which allows a client such as the runtime system to
 * create {@link XirTemplate XIR templates}.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public abstract class CiXirAssembler {

	protected XirVariable resultOperand;

    protected final List<XirInstruction> instructions = new ArrayList<XirInstruction>();
    protected final List<XirLabel> labels = new ArrayList<XirLabel>();
    protected final List<XirParameter> parameters = new ArrayList<XirParameter>();
    protected final List<XirTemp> temps = new ArrayList<XirTemp>();
    protected final List<XirConstant> constants = new ArrayList<XirConstant>();

    protected int variableCount;
    protected boolean finished;

    public class XirLabel {
    	public final String name;
        public final int index;
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

    public abstract class XirVariable {
        public boolean written;
        public final CiKind kind;
        public final int index;
        public final boolean constant;

        public XirVariable(CiKind kind, boolean constant) {
            this.kind = kind;
            this.index = variableCount++;
            this.constant = constant;
        }

		public boolean isConstant() {
			return constant;
		}
    }

    public final class XirResult extends XirVariable {

    	public XirResult(CiKind kind, int index) {
    		super(kind, false);
    	}

    	public String toString() {
    		return "result";
    	}
    }

    public class XirParameter extends XirVariable {
        public final CiConstant value;
        public final String name;
        public final int parameterIndex;

        XirParameter(String name, CiKind kind, boolean unknownConstant, CiConstant value, int parameterIndex) {
            super(kind, unknownConstant);
            this.parameterIndex = parameterIndex;
            this.value = value;
            this.name = name;
        }

        @Override
        public String toString() {
        	return name;
        }

        public String detailedToString() {

        	StringBuffer sb = new StringBuffer();

        	sb.append(name);
        	sb.append('$');
        	sb.append(super.kind.typeChar);
        	if (isConstant()) {
        		sb.append("$const");
        	}
        	return sb.toString();
        }
    }

    public class XirConstant extends XirVariable {
        public final CiConstant value;

        XirConstant(CiConstant value) {
        	super(value.basicType, true);
        	this.value = value;
        }

        @Override
        public String toString() {
        	return value.valueString();
        }

        public String detailedToString() {
        	StringBuffer sb = new StringBuffer();
        	sb.append(value.valueString());
        	sb.append('$');
        	sb.append(super.kind.typeChar);
        	return sb.toString();
        }
    }

    public class XirTemp extends XirVariable {
        public final String name;

        XirTemp(String name, CiKind kind) {
            super(kind, false);
            this.name = name;
        }

        @Override
        public String toString() {
        	return name;
        }

        public String detailedToString() {
        	StringBuffer sb = new StringBuffer();
        	sb.append(name);
        	sb.append('$');
        	sb.append(super.kind.typeChar);
        	return sb.toString();
        }
    }

    public class XirFixed extends XirTemp {
        public final CiLocation location;

        XirFixed(String name, CiLocation location) {
            super(name, location.kind);
            this.location = location;
        }
    }

    public void restart() {
    	reset();
    	resultOperand = null;
    }

    public void restart(CiKind kind) {
        reset();
        resultOperand = new XirResult(kind, 0);
    }

    private void reset() {
        variableCount = 0;
        finished = false;
        instructions.clear();
        labels.clear();
        parameters.clear();
        temps.clear();
        constants.clear();
    }
    
    protected final XirInstruction NOP = new XirInstruction(CiKind.Illegal, XirOp.Nop, null);

    public class XirInstruction {
        public final CiKind kind;
        public final XirOp op;
        public final XirVariable result;
        public final XirVariable[] arguments;
        public final Object extra;

        public XirInstruction(CiKind kind, XirOp op, XirVariable result, XirVariable... arguments) {
            this(kind, null, op, result, arguments);
        }

        public XirInstruction(CiKind kind, Object extra, XirOp op, XirVariable result, XirVariable... arguments) {
            this.extra = extra;
            this.kind = kind;
            this.op = op;
            this.result = result;
            this.arguments = arguments;
        }

        public XirVariable x() {
            assert arguments.length > 0 : "no x operand for this instruction";
            return arguments[0];
        }

        public XirVariable y() {
            assert arguments.length > 1 : "no y operand for this instruction";
            return arguments[1];
        }

        public XirVariable z() {
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

    public enum XirOp {
    	Nop,
        Mov,
        Add,
        Sub,
        Div,
        Mul,
        Mod,
        Shl,
        Shr,
        And,
        Or,
        Xor,
        PointerLoad,
        PointerStore,
        PointerLoadDisp,
        PointerStoreDisp,
        PointerCAS,
        CallStub,
        CallRuntime,
        CallJava,
        Jmp,
        Jeq,
        Jneq,
        Jgt,
        Jgteq,
        Jugteq,
        Jlt,
        Jlteq,
        Bind,
        Ret
    }

    private void append(XirInstruction xirInstruction) {
    	// (tw) TODO: Check that return or java call is always the last instruction
    	//assert !finished;
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

    public void mov(XirVariable result, XirVariable a) {
        append(new XirInstruction(result.kind, XirOp.Mov, result, a));
    }

    public void add(XirVariable result, XirVariable a, XirVariable b) {
        append(new XirInstruction(result.kind, XirOp.Add, result, a, b));
    }

    public void sub(XirVariable result, XirVariable a, XirVariable b) {
        append(new XirInstruction(result.kind, XirOp.Sub, result, a, b));
    }

    public void div(XirVariable result, XirVariable a, XirVariable b) {
        append(new XirInstruction(result.kind, XirOp.Div, result, a, b));
    }

    public void mul(XirVariable result, XirVariable a, XirVariable b) {
        append(new XirInstruction(result.kind, XirOp.Mul, result, a, b));
    }

    public void mod(XirVariable result, XirVariable a, XirVariable b) {
        append(new XirInstruction(result.kind, XirOp.Mod, result, a, b));
    }

    public void shl(XirVariable result, XirVariable a, XirVariable b) {
        append(new XirInstruction(result.kind, XirOp.Shl, result, a, b));
    }

    public void shr(XirVariable result, XirVariable a, XirVariable b) {
        append(new XirInstruction(result.kind, XirOp.Shr, result, a, b));
    }

    public void and(XirVariable result, XirVariable a, XirVariable b) {
        append(new XirInstruction(result.kind, XirOp.And, result, a, b));
    }

    public void or(XirVariable result, XirVariable a, XirVariable b) {
        append(new XirInstruction(result.kind, XirOp.Or, result, a, b));
    }

    public void xor(XirVariable result, XirVariable a, XirVariable b) {
        append(new XirInstruction(result.kind, XirOp.Xor, result, a, b));
    }

    public void pload(CiKind kind, XirVariable result, XirVariable pointer, boolean canTrap) {
        append(new XirInstruction(kind, canTrap, XirOp.PointerLoad, result, pointer));
    }

    public void pstore(CiKind kind, XirVariable pointer, XirVariable value, boolean canTrap) {
        append(new XirInstruction(kind, canTrap, XirOp.PointerStore, (XirVariable) null, pointer, value));
    }

    public void pload(CiKind kind, XirVariable result, XirVariable pointer, XirVariable disp, boolean canTrap) {
        append(new XirInstruction(kind, canTrap, XirOp.PointerLoadDisp, result, pointer, disp));
    }

    public void pstore(CiKind kind, XirVariable pointer, XirVariable disp, XirVariable value, boolean canTrap) {
        append(new XirInstruction(kind, canTrap, XirOp.PointerStoreDisp, (XirVariable) null, pointer, disp, value));
    }

    public void pcas(CiKind kind, XirVariable result, XirVariable pointer, XirVariable value, XirVariable expectedValue) {
        append(new XirInstruction(kind, XirOp.PointerLoad, result, pointer, value, expectedValue));
    }

    public void jmp(XirLabel l) {
        append(new XirInstruction(CiKind.Void, l, XirOp.Jmp, null));
    }

    public void jeq(XirLabel l, XirVariable a, XirVariable b) {
        jcc(XirOp.Jeq, l, a, b);
    }

    private void jcc(XirOp op, XirLabel l, XirVariable a, XirVariable b) {
        append(new XirInstruction(CiKind.Void, l, op, null, a, b));
    }

    public void jneq(XirLabel l, XirVariable a, XirVariable b) {
        jcc(XirOp.Jneq, l, a, b);
    }

    public void jgt(XirLabel l, XirVariable a, XirVariable b) {
        jcc(XirOp.Jgt, l, a, b);
    }

    public void jgteq(XirLabel l, XirVariable a, XirVariable b) {
        jcc(XirOp.Jgteq, l, a, b);
    }

    public void jugteq(XirLabel l, XirVariable a, XirVariable b) {
        jcc(XirOp.Jugteq, l, a, b);
    }

    public void jlt(XirLabel l, XirVariable a, XirVariable b) {
        jcc(XirOp.Jlt, l, a, b);
    }

    public void jlteq(XirLabel l, XirVariable a, XirVariable b) {
        jcc(XirOp.Jlteq, l, a, b);
    }

    public void bindInline(XirLabel l) {
        assert l.inline;
        append(new XirInstruction(CiKind.Void, l, XirOp.Bind, null));
    }

    public void bindOutOfLine(XirLabel l) {
        assert !l.inline;
        append(new XirInstruction(CiKind.Void, l, XirOp.Bind, null));
    }

    public void callJava(XirVariable destination) {
    	this.resultOperand = destination;
    	end();
    }

    public void callStub(XirTemplate stub, XirVariable result, XirVariable... args) {
        CiKind resultKind = result == null ? CiKind.Void : result.kind;
        append(new XirInstruction(resultKind, stub, XirOp.CallStub, result, args));
    }

    public void callRuntime(Object rt, XirVariable result, XirVariable... args) {
        CiKind resultKind = result == null ? CiKind.Void : result.kind;
        append(new XirInstruction(resultKind, rt, XirOp.CallRuntime, result, args));
    }

    public void end() {
        append(new XirInstruction(CiKind.Void, XirOp.Ret, null));
        finished = true;
    }

    public void ret(CiKind kind, XirVariable result) {
        append(new XirInstruction(kind, XirOp.Ret, result));
    }

    public XirParameter createInputParameter(String name, CiKind kind) {
        XirParameter param = new XirParameter(name, kind, false, null, parameters.size());
        parameters.add(param);
        return param;
    }

    public XirParameter createConstantInputParameter(String name, CiKind kind) {
        XirParameter param = new XirParameter(name, kind, true, null, parameters.size());
        parameters.add(param);
        return param;
    }

    public XirConstant createConstant(CiConstant constant) {
    	XirConstant temp = new XirConstant(constant);
        constants.add(temp);
        return temp;
    }

    public XirVariable createTemp(String name, CiKind kind) {
        XirTemp temp = new XirTemp(name, kind);
        temps.add(temp);
        return temp;
    }

    public XirVariable createRegister(String name, CiKind kind, CiRegister register) {
        XirFixed fixed = new XirFixed(name, new CiLocation(kind, register));
        temps.add(fixed);
        return fixed;
    }

    public XirVariable i(int b) {
        return createConstant(CiConstant.forInt(b));
    }

    public XirVariable b(boolean t) {
        return createConstant(CiConstant.forBoolean(t));
    }

    public XirVariable w(long b) {
        return createConstant(CiConstant.forWord(b));
    }

    public XirVariable o(Object obj) {
        return createConstant(CiConstant.forObject(obj));
    }

    public XirVariable getResultOperand() {
        return resultOperand;
    }

    public XirTemplate finishTemplate(String name) {
        return buildTemplate(name, false);
    }

    public XirTemplate finishStub(String name) {
        return buildTemplate(name, true);
    }


    protected abstract XirTemplate buildTemplate(String name, boolean isStub);
    public abstract CiXirAssembler copy();
}
