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

import com.sun.c1x.ci.CiKind;
import com.sun.c1x.ci.CiConstant;
import com.sun.c1x.ci.CiRegister;
import com.sun.c1x.ri.RiSignature;

/**
 * This class represents an assembler which allows a client such as the runtime system to
 * create {@link XirTemplate XIR templates}.
 *
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public class XirAssembler {

    private final XirParameter resultOperand;
    private final XirVariable nullOperand;

    private final List<XirInstruction> instructions = new ArrayList<XirInstruction>();
    private final List<XirLabel> labels = new ArrayList<XirLabel>();
    private final List<XirParameter> parameters = new ArrayList<XirParameter>();
    private final List<XirTemp> temps = new ArrayList<XirTemp>();
    private final List<XirCache> caches = new ArrayList<XirCache>();

    public class XirLabel {
        public final int index;
        public final boolean inline;

        private XirLabel(int index, boolean inline) {
            this.index = index;
            this.inline = inline;
        }
    }

    public abstract class XirVariable {
        public final CiKind kind;
        public final int index;

        public XirVariable(CiKind kind, int index) {
            this.kind = kind;
            this.index = index;
        }
    }

    public class XirParameter extends XirVariable {
        public final boolean unknownConstant;
        public final CiConstant value;

        XirParameter(CiKind kind, boolean unknownConstant, CiConstant value, int index) {
            super(kind, index);
            this.unknownConstant = unknownConstant;
            this.value = value;
        }
    }

    public class XirTemp extends XirVariable {
        public final CiConstant value;

        XirTemp(CiKind kind, CiConstant value, int index) {
            super(kind, index);
            this.value = value;
        }
    }

    public class XirCache extends XirVariable {
        public final boolean writeMany;

        XirCache(CiKind kind, int index, boolean writeMany) {
            super(kind, index);
            this.writeMany = writeMany;
        }
    }

    public XirAssembler(CiKind kind) {
        nullOperand = createParameter(CiKind.Illegal, true);
        resultOperand = createParameter(kind, false);
    }

    public XirAssembler(RiSignature signature) {
        nullOperand = createParameter(CiKind.Illegal, true);
        resultOperand = createParameter(signature.returnBasicType(), false);
        for (int i = 0; i < signature.argumentCount(false); i++) {
            createInputParameter(signature.argumentBasicTypeAt(i));
        }
    }

    private XirParameter createParameter(CiKind kind, boolean isConstant) {
        final XirParameter result = new XirParameter(kind, isConstant, null, parameters.size());
        parameters.add(result);
        return result;
    }

    public class XirInstruction {
        public final CiKind kind;
        public final OperatorKind operator;
        public final XirVariable result;
        public final XirVariable a;
        public final XirVariable b;
        public final XirVariable c;
        public final XirLabel destination;

        public XirInstruction(CiKind kind, OperatorKind operator, XirVariable result, XirVariable... arguments) {
            this(kind, operator, null, result, arguments);
        }

        public XirInstruction(CiKind kind, OperatorKind operator, XirLabel destination, XirVariable result, XirVariable... arguments) {
            this.destination = destination;
            this.kind = kind;
            this.operator = operator;
            this.result = result;

            if (arguments.length > 0) {
                a = arguments[0];
            } else {
                a = null;
            }

            if (arguments.length > 1) {
                b = arguments[1];
            } else {
                b = null;
            }

            if (arguments.length > 2) {
                c = arguments[2];
            } else {
                c = null;
            }
        }
    }

    public enum OperatorKind {
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
        instructions.add(xirInstruction);
    }

    public XirLabel createInlineLabel() {
        final XirLabel result = new XirLabel(this.labels.size(), true);
        labels.add(result);
        return result;
    }

    public XirLabel createOutOfLineLabel() {
        final XirLabel result = new XirLabel(this.labels.size(), false);
        labels.add(result);
        return result;
    }

    public void mov(XirVariable result, XirVariable a) {
        append(new XirInstruction(result.kind, OperatorKind.Mov, result, a));
    }

    public void add(XirVariable result, XirVariable a, XirVariable b) {
        append(new XirInstruction(result.kind, OperatorKind.Add, result, a, b));
    }

    public void sub(XirVariable result, XirVariable a, XirVariable b) {
        append(new XirInstruction(result.kind, OperatorKind.Sub, result, a, b));
    }

    public void div(XirVariable result, XirVariable a, XirVariable b) {
        append(new XirInstruction(result.kind, OperatorKind.Div, result, a, b));
    }

    public void mul(XirVariable result, XirVariable a, XirVariable b) {
        append(new XirInstruction(result.kind, OperatorKind.Mul, result, a, b));
    }

    public void mod(XirVariable result, XirVariable a, XirVariable b) {
        append(new XirInstruction(result.kind, OperatorKind.Mod, result, a, b));
    }

    public void shl(XirVariable result, XirVariable a, XirVariable b) {
        append(new XirInstruction(result.kind, OperatorKind.Shl, result, a, b));
    }

    public void shr(XirVariable result, XirVariable a, XirVariable b) {
        append(new XirInstruction(result.kind, OperatorKind.Shr, result, a, b));
    }

    public void and(XirVariable result, XirVariable a, XirVariable b) {
        append(new XirInstruction(result.kind, OperatorKind.And, result, a, b));
    }

    public void or(XirVariable result, XirVariable a, XirVariable b) {
        append(new XirInstruction(result.kind, OperatorKind.Or, result, a, b));
    }

    public void xor(XirVariable result, XirVariable a, XirVariable b) {
        append(new XirInstruction(result.kind, OperatorKind.Xor, result, a, b));
    }

    public void pload(CiKind kind, XirVariable result, XirVariable pointer) {
        append(new XirInstruction(kind, OperatorKind.PointerLoad, result, pointer));
    }

    public void pstore(CiKind kind, XirVariable pointer, XirVariable value) {
        append(new XirInstruction(kind, OperatorKind.PointerStore, nullOperand, pointer, value));
    }

    public void pload(CiKind kind, XirVariable result, XirVariable pointer, XirVariable disp) {
        append(new XirInstruction(kind, OperatorKind.PointerLoadDisp, result, pointer, disp));
    }

    public void pstore(CiKind kind, XirVariable pointer, XirVariable disp, XirVariable value) {
        append(new XirInstruction(kind, OperatorKind.PointerStoreDisp, nullOperand, pointer, disp, value));
    }

    public void pcas(CiKind kind, XirVariable result, XirVariable pointer, XirVariable value, XirVariable expectedValue) {
        append(new XirInstruction(kind, OperatorKind.PointerLoad, result, pointer, value, expectedValue));
    }

    public void jmp(XirLabel l) {
        append(new XirInstruction(CiKind.Void, OperatorKind.Jmp, l, nullOperand));
    }

    public void jeq(XirLabel l, XirVariable a, XirVariable b) {
        jcc(OperatorKind.Jeq, l, a, b);
    }

    private void jcc(OperatorKind op, XirLabel l, XirVariable a, XirVariable b) {
        append(new XirInstruction(CiKind.Void, op, l, nullOperand, a, b));
    }

    public void jneq(XirLabel l, XirVariable a, XirVariable b) {
        jcc(OperatorKind.Jneq, l, a, b);
    }

    public void jgt(XirLabel l, XirVariable a, XirVariable b) {
        jcc(OperatorKind.Jgt, l, a, b);
    }

    public void jgteq(XirLabel l, XirVariable a, XirVariable b) {
        jcc(OperatorKind.Jgteq, l, a, b);
    }

    public void jugteq(XirLabel l, XirVariable a, XirVariable b) {
        jcc(OperatorKind.Jgteq, l, a, b);
    }

    public void jlt(XirLabel l, XirVariable a, XirVariable b) {
        jcc(OperatorKind.Jlt, l, a, b);
    }

    public void jlteq(XirLabel l, XirVariable a, XirVariable b) {
        jcc(OperatorKind.Jlteq, l, a, b);
    }

    public void bindInline(XirLabel l) {
        assert l.inline;
        append(new XirInstruction(CiKind.Void, OperatorKind.Bind, l, nullOperand));
    }

    public void bindOutOfLine(XirLabel l) {
        assert !l.inline;
        append(new XirInstruction(CiKind.Void, OperatorKind.Bind, l, nullOperand));
    }

    public void callJava(XirVariable result, XirVariable destination) {
        append(new XirInstruction(result.kind, OperatorKind.CallJava, nullOperand, destination));
    }

    public void callStub(XirTemplate stub, XirVariable result, XirVariable... args) {
        if (args.length < 2) {
            // TODO: create instructions to store parameters
        } else {
            // TODO: create instructions to store parameters
        }
    }

    public void callRuntime(Object rt, XirVariable result, XirVariable... args) {
        // TODO: create instructions to store parameters for runtime
    }

    public void end() {
        append(new XirInstruction(CiKind.Void, OperatorKind.Ret, nullOperand));
    }

    public void ret(CiKind kind, XirVariable result) {
        append(new XirInstruction(kind, OperatorKind.Ret, result));
    }

    public XirParameter createInputParameter(CiKind kind) {
        XirParameter param = new XirParameter(kind, false, null, parameters.size());
        parameters.add(param);
        return param;
    }

    public XirParameter createConstantInputParameter(CiKind kind) {
        XirParameter param = new XirParameter(kind, true, null, parameters.size());
        parameters.add(param);
        return param;
    }

    public XirTemp createConstantTemp(CiConstant constant) {
        XirTemp temp = new XirTemp(constant.basicType, constant, parameters.size());
        temps.add(temp);
        return temp;
    }

    public XirTemp createTemp(CiKind kind) {
        XirTemp temp = new XirTemp(kind, null, temps.size());
        temps.add(temp);
        return temp;
    }

    public XirVariable createRegister(CiKind kind, CiRegister register) {
        return null;
    }

    public XirCache createCache(CiKind kind, boolean writeMany) {
        XirCache cache = new XirCache(kind, caches.size(), writeMany);
        caches.add(cache);
        return cache;
    }

    public XirTemp i(int b) {
        return createConstantTemp(CiConstant.forInt(b));
    }

    public XirTemp b(boolean t) {
        return createConstantTemp(CiConstant.forBoolean(t));
    }

    public XirTemp w(long b) {
        return createConstantTemp(CiConstant.forWord(b));
    }

    public XirTemp o(Object obj) {
        return createConstantTemp(CiConstant.forObject(obj));
    }

    public XirParameter getResultOperand() {
        return resultOperand;
    }

    public XirTemplate finishTemplate() {
        return new XirTemplate(instructions.toArray(new XirInstruction[instructions.size()]), labels.toArray(new XirAssembler.XirLabel[labels.size()]), parameters.toArray(new XirParameter[parameters
                .size()]));
    }

    public XirTemplate finishStub() {
        return new XirTemplate(instructions.toArray(new XirInstruction[instructions.size()]), labels.toArray(new XirAssembler.XirLabel[labels.size()]), parameters.toArray(new XirParameter[parameters
                .size()]));
    }
}
