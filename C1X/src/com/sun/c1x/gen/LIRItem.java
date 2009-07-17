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
package com.sun.c1x.gen;

import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.util.*;
import com.sun.c1x.value.*;

/**
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class LIRItem {

    private Instruction value;
    private final LIRGenerator gen;
    private LIROperand result;
    private boolean destroysRegister;
    private LIROperand newResult;

    private LIRGenerator gen() {
        return gen;
    }

    public LIRItem(Instruction value, LIRGenerator gen) {
        this.gen = gen;
        setInstruction(value);
    }

    public void setInstruction(Instruction value) {
        this.value = value;
        this.result = LIROperandFactory.IllegalOperand;
        if (value != null) {
            gen.walk(value);
            result = value.operand();
        }
        newResult = LIROperandFactory.IllegalOperand;
    }

    public LIRItem(LIRGenerator gen) {
        this.gen = gen;
        result = LIROperandFactory.IllegalOperand;
        setInstruction(null);
    }

    Instruction value() {
        return value;
    }

    public ValueType type() {
        return value().type();
    }

    public void loadItemForce(LIROperand reg) {
        LIROperand r = result();
        if (r != reg) {
            if (r.type() != reg.type()) {
                // moves between different types need an intervening spill slot
                LIROperand tmp = gen.forceToSpill(r, reg.type());
                lir().move(tmp, reg);
            } else {
                lir().move(r, reg);
            }
            result = reg;
        }
    }

    public void loadForStore(BasicType type) {
        if (gen.canStoreAsConstant(value(), type)) {
            result = value().operand();
            if (!result.isConstant()) {
                result = LIROperandFactory.valueType(value().type());
            }
        } else if (type == BasicType.Byte || type == BasicType.Boolean) {
            loadByteItem();
        } else {
            loadItem();
        }
    }

    public LIROperand result() {
        assert !destroysRegister || (!result.isRegister() || result.isVirtual()) : "shouldn't use setDestroysRegister with physical regsiters";
        if (destroysRegister && result.isRegister()) {
            if (newResult.isIllegal()) {
                newResult = gen.newRegister(type().basicType);
                gen().lir().move(result, newResult);
            }
            return newResult;
        } else {
            return result;
        }
    }

    public void dontLoadItem() {
        // do nothing
    }

    public void setDestroysRegister() {
        destroysRegister = true;
    }

    public boolean isConstant() {
        return value instanceof Constant;
    }

    public boolean isStack() {
        return result.isStack();
    }

    public boolean isRegister() {
        return result.isRegister();
    }

    public LIRList lir() {
        return gen().lir();
    }

    public void loadByteItem() {
        if (gen.compilation.target.arch.isX86()) {
            loadItem();
            LIROperand res = result();

            if (!res.isVirtual() || !gen.isVregFlagSet(res, LIRGenerator.VregFlag.ByteReg)) {
                // make sure that it is a byte register
                assert !value().type().isFloat() && !value().type().isDouble() : "can't load floats in byte register";
                LIROperand reg = gen.rlockByte(BasicType.Byte);
                lir().move(res, reg);
                result = reg;
            }
        } else if (gen.compilation.target.arch.isSPARC()) {
            loadItem();
        } else {
            Util.shouldNotReachHere();
        }
    }

    public void loadNonconstant() {
        if (gen.compilation.target.arch.isX86()) {
            LIROperand r = value().operand();
            if (r.isConstant()) {
                result = r;
            } else {
                loadItem();
            }
        } else if (gen.compilation.target.arch.isSPARC()) {
            LIROperand r = value().operand();
            if (gen.canInlineAsConstant(value())) {
                if (!r.isConstant()) {
                    r = LIROperandFactory.valueType(value().type());
                }
                result = r;
            } else {
                loadItem();
            }
        } else {
            Util.shouldNotReachHere();
        }
    }

    void setResult(LIROperand opr) {
        assert value().operand().isIllegal() || value().operand().isConstant() : "operand should never change";
        value().setOperand(opr);

        if (opr.isVirtual()) {
            gen.instructionForOperand.put(opr.vregNumber(), value());
        }

        result = opr;
    }

    public void loadItem() {
        if (result().isIllegal()) {
            // update the items result
            result = value().operand();
        }
        if (!result().isRegister()) {
            LIROperand reg = gen.newRegister(value().type().basicType);
            lir().move(result(), reg);
            if (result().isConstant()) {
                result = reg;
            } else {
                setResult(reg);
            }
        }
    }

    public Object getJobjectConstant() {
        assert type().isConstant();
        if (type().isObject()) {
            return type().asConstant().asObject();
        }
        return null;
    }

    public int getJintConstant() {
        assert isConstant() && value() != null : "";
        assert type().isInt() : "type check";
        return type().asConstant().asInt();
    }

    public float getJfloatConstant() {
        assert isConstant() && value() != null : "";
        assert type().isFloat() : "type check";
        return type().asConstant().asFloat();
    }

    public double getJdoubleConstant() {
        assert isConstant() && value() != null : "";
        assert type().isDouble() : "type check";
        return type().asConstant().asDouble();
    }

    public long getJlongConstant() {
        assert isConstant() && value() != null : "";
        assert type().isLong() : "type check";
        return type().asConstant().asLong();
    }
}
