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

	protected XirOperand resultOperand;
	protected boolean allocateResultOperand;

    protected final List<XirInstruction> instructions = new ArrayList<XirInstruction>();
    protected final List<XirLabel> labels = new ArrayList<XirLabel>();
    protected final List<XirParameter> parameters = new ArrayList<XirParameter>();
    protected final List<XirTemp> temps = new ArrayList<XirTemp>();
    protected final List<XirConstant> constants = new ArrayList<XirConstant>();

    protected int variableCount;
    protected boolean finished = true;

    /**
     * Class that represents additional address calculation information.
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

    public interface XirConstantOperand {
    	int getIndex();
    }

    public abstract class XirOperand {
        public final CiKind kind;
        public final int index;
        public final String name;

        public XirOperand(String name, CiKind kind) {
            this.kind = kind;
            this.name = name;
            this.index = variableCount++;
        }

        public int getIndex() {
        	return index;
        }

        @Override
        public String toString() {
        	return name;
        }
    }

    public class XirParameter extends XirOperand {
        public final int parameterIndex;

        XirParameter(String name, CiKind kind, int parameterIndex) {
            super(name, kind);
            this.parameterIndex = parameterIndex;
        }

        public String detailedToString() {

        	StringBuffer sb = new StringBuffer();

        	sb.append(name);
        	sb.append('$');
        	sb.append(super.kind.typeChar);
        	return sb.toString();
        }
    }

    public class XirConstantParameter extends XirParameter implements XirConstantOperand {
    	XirConstantParameter(String name, CiKind kind, int parameterIndex) {
    		super(name, kind, parameterIndex);
        }
    }

    public class XirVariableParameter extends XirParameter {
    	XirVariableParameter(String name, CiKind kind, int parameterIndex) {
    		super(name, kind, parameterIndex);
        }
    }

    public class XirConstant extends XirOperand implements XirConstantOperand {
        public final CiConstant value;

        XirConstant(String name, CiConstant value) {
        	super(name, value.kind);
        	this.value = value;
        }

        public String detailedToString() {
        	StringBuffer sb = new StringBuffer();
        	sb.append(value.valueString());
        	sb.append('$');
        	sb.append(super.kind.typeChar);
        	return sb.toString();
        }
    }

    public class XirTemp extends XirOperand {
        XirTemp(String name, CiKind kind) {
            super(name, kind);
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

    public XirOperand restart(CiKind kind) {
        reset();
        resultOperand = new XirTemp("result", kind);
        allocateResultOperand = true;
        return resultOperand;
    }

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

    public class XirInstruction {
        public final CiKind kind;
        public final XirOp op;
        public final XirOperand result;
        public final XirOperand[] arguments;
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

    public enum XirOp {
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
        Jmp,
        Jeq,
        Jneq,
        Jgt,
        Jgteq,
        Jugteq,
        Jlt,
        Jlteq,
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
        append(new XirInstruction(result.kind, XirOp.Mov, result, a));
    }

    public void add(XirOperand result, XirOperand a, XirOperand b) {
        append(new XirInstruction(result.kind, XirOp.Add, result, a, b));
    }

    public void sub(XirOperand result, XirOperand a, XirOperand b) {
        append(new XirInstruction(result.kind, XirOp.Sub, result, a, b));
    }

    public void div(XirOperand result, XirOperand a, XirOperand b) {
        append(new XirInstruction(result.kind, XirOp.Div, result, a, b));
    }

    public void mul(XirOperand result, XirOperand a, XirOperand b) {
        append(new XirInstruction(result.kind, XirOp.Mul, result, a, b));
    }

    public void mod(XirOperand result, XirOperand a, XirOperand b) {
        append(new XirInstruction(result.kind, XirOp.Mod, result, a, b));
    }

    public void shl(XirOperand result, XirOperand a, XirOperand b) {
        append(new XirInstruction(result.kind, XirOp.Shl, result, a, b));
    }

    public void shr(XirOperand result, XirOperand a, XirOperand b) {
        append(new XirInstruction(result.kind, XirOp.Shr, result, a, b));
    }

    public void and(XirOperand result, XirOperand a, XirOperand b) {
        append(new XirInstruction(result.kind, XirOp.And, result, a, b));
    }

    public void or(XirOperand result, XirOperand a, XirOperand b) {
        append(new XirInstruction(result.kind, XirOp.Or, result, a, b));
    }

    public void xor(XirOperand result, XirOperand a, XirOperand b) {
        append(new XirInstruction(result.kind, XirOp.Xor, result, a, b));
    }

    public void pload(CiKind kind, XirOperand result, XirOperand pointer, boolean canTrap) {
        append(new XirInstruction(kind, canTrap, XirOp.PointerLoad, result, pointer));
    }

    public void pstore(CiKind kind, XirOperand pointer, XirOperand value, boolean canTrap) {
        append(new XirInstruction(kind, canTrap, XirOp.PointerStore, null, pointer, value));
    }

    public void pload(CiKind kind, XirOperand result, XirOperand pointer, XirOperand disp, boolean canTrap) {
        append(new XirInstruction(kind, new AddressAccessInformation(canTrap), XirOp.PointerLoadDisp, result, pointer, disp));
    }

    public void pstore(CiKind kind, XirOperand pointer, XirOperand disp, XirOperand value, boolean canTrap) {
        append(new XirInstruction(kind, new AddressAccessInformation(canTrap), XirOp.PointerStoreDisp, (XirOperand) null, pointer, disp, value));
    }

    public void pload(CiKind kind, XirOperand result, XirOperand pointer, XirOperand disp, XirConstantOperand offset, boolean canTrap) {
        append(new XirInstruction(kind, new AddressAccessInformation(canTrap, offset), XirOp.PointerLoadDisp, result, pointer, disp));
    }

    public void pstore(CiKind kind, XirOperand pointer, XirOperand disp, XirOperand value, XirConstantOperand offset, boolean canTrap) {
        append(new XirInstruction(kind, new AddressAccessInformation(canTrap, offset), XirOp.PointerStoreDisp, (XirOperand) null, pointer, disp, value));
    }

    public void pload(CiKind kind, XirOperand result, XirOperand pointer, XirOperand disp, XirConstantOperand offset, XirConstantOperand scaling,  boolean canTrap) {
        append(new XirInstruction(kind, new AddressAccessInformation(canTrap, offset, scaling), XirOp.PointerLoadDisp, result, pointer, disp));
    }

    public void pstore(CiKind kind, XirOperand pointer, XirOperand disp, XirOperand value, XirConstantOperand offset, XirConstantOperand scaling, boolean canTrap) {
        append(new XirInstruction(kind, new AddressAccessInformation(canTrap, offset, scaling), XirOp.PointerStoreDisp, (XirOperand) null, pointer, disp, value));
    }

    public void pcas(CiKind kind, XirOperand result, XirOperand pointer, XirOperand value, XirOperand expectedValue) {
        append(new XirInstruction(kind, XirOp.PointerLoad, result, pointer, value, expectedValue));
    }

    public void jmp(XirLabel l) {
        append(new XirInstruction(CiKind.Void, l, XirOp.Jmp, null));
    }

    public void jeq(XirLabel l, XirOperand a, XirOperand b) {
        jcc(XirOp.Jeq, l, a, b);
    }

    private void jcc(XirOp op, XirLabel l, XirOperand a, XirOperand b) {
        append(new XirInstruction(CiKind.Void, l, op, null, a, b));
    }

    public void jneq(XirLabel l, XirOperand a, XirOperand b) {
        jcc(XirOp.Jneq, l, a, b);
    }

    public void jgt(XirLabel l, XirOperand a, XirOperand b) {
        jcc(XirOp.Jgt, l, a, b);
    }

    public void jgteq(XirLabel l, XirOperand a, XirOperand b) {
        jcc(XirOp.Jgteq, l, a, b);
    }

    public void jugteq(XirLabel l, XirOperand a, XirOperand b) {
        jcc(XirOp.Jugteq, l, a, b);
    }

    public void jlt(XirLabel l, XirOperand a, XirOperand b) {
        jcc(XirOp.Jlt, l, a, b);
    }

    public void jlteq(XirLabel l, XirOperand a, XirOperand b) {
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

    public void callStub(XirTemplate stub, XirOperand result, XirOperand... args) {
        CiKind resultKind = result == null ? CiKind.Void : result.kind;
        append(new XirInstruction(resultKind, stub, XirOp.CallStub, result, args));
    }

    public void callRuntime(Object rt, XirOperand result, XirOperand... args) {
        CiKind resultKind = result == null ? CiKind.Void : result.kind;
        append(new XirInstruction(resultKind, rt, XirOp.CallRuntime, result, args));
    }

    private void end() {
    	assert !finished : "template may only be finished once!";
        assert resultOperand != null : "result operand should be set";
        finished = true;
    }

    public XirVariableParameter createInputParameter(String name, CiKind kind) {
    	assert !finished;
    	XirVariableParameter param = new XirVariableParameter(name, kind, parameters.size());
        parameters.add(param);
        return param;
    }

    public XirConstantParameter createConstantInputParameter(String name, CiKind kind) {
    	assert !finished;
    	XirConstantParameter param = new XirConstantParameter(name, kind, parameters.size());
        parameters.add(param);
        return param;
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
        XirFixed fixed = new XirFixed(name, new CiLocation(kind, register));
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

    public XirTemplate finishTemplate(XirOperand result, String name) {
		assert this.resultOperand == null;
		assert result != null;
		this.resultOperand = result;
        final XirTemplate template = buildTemplate(name, false);
        end();
        return template;
    }
    public XirTemplate finishTemplate(String name) {
        final XirTemplate template = buildTemplate(name, false);
        end();
        return template;
    }

    public XirTemplate finishStub(String name) {
        final XirTemplate template = buildTemplate(name, true);
    	end();
    	return template;
    }

    protected abstract XirTemplate buildTemplate(String name, boolean isStub);
    public abstract CiXirAssembler copy();

}
