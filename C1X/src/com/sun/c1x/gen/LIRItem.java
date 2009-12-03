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

import com.sun.c1x.ci.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.lir.*;
import com.sun.c1x.util.*;
import static com.sun.c1x.lir.LIROperand.*;

/**
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public class LIRItem {

    public Value value;
    private final LIRGenerator gen;
    private LIROperand result;
    private boolean destroysRegister;
    private LIROperand newResult;

    public LIRItem(Value value, LIRGenerator gen) {
        this.gen = gen;
        setInstruction(value);
    }

    public void setInstruction(Value value) {
        this.value = value;
        this.result = IllegalLocation;
        if (value != null) {
            gen.walk(value);
            result = value.operand();
        }
        newResult = IllegalLocation;
    }

    public LIRItem(LIRGenerator gen) {
        this.gen = gen;
        result = IllegalLocation;
        setInstruction(null);
    }

    public void loadItemForce(LIROperand reg) {
        LIROperand r = result();
        if (r != reg) {
            assert r.kind != CiKind.Illegal;
            if (r.kind != reg.kind) {
                // moves between different types need an intervening spill slot
                LIROperand tmp = gen.forceToSpill(r, reg.kind);
                gen.lir.move(tmp, reg);
            } else {
                gen.lir.move(r, reg);
            }
            result = reg;
        }
    }

    public void loadItem(CiKind type) {
        if (type == CiKind.Byte || type == CiKind.Boolean) {
            loadByteItem();
        } else {
            loadItem();
        }
    }

    public void loadForStore(CiKind type) {
        if (gen.canStoreAsConstant(value, type)) {
            result = value.operand();
            if (!isConstant(result)) {
                result = forConstant(value);
            }
        } else if (type == CiKind.Byte || type == CiKind.Boolean) {
            loadByteItem();
        } else {
            loadItem();
        }
    }

    public LIROperand result() {
        assert !destroysRegister || (!result.isRegister() || result.isVariable()) : "shouldn't use setDestroysRegister with physical regsiters";
        if (destroysRegister && result.isRegister()) {
            if (isIllegal(newResult)) {
                newResult = gen.newRegister(value.kind);
                gen.lir.move(result, newResult);
            }
            return newResult;
        } else {
            return result;
        }
    }

    public void setDestroysRegister() {
        destroysRegister = true;
    }

    public boolean isStack() {
        return result.isStack();
    }

    public boolean isRegister() {
        return result.isRegister();
    }

    public void loadByteItem() {
        if (gen.compilation.target.arch.isX86()) {
            loadItem();
            LIROperand res = result();

            if (!res.isVariable() || !gen.isVarFlagSet(res, LIRGenerator.VariableFlag.MustBeByteReg)) {
                // make sure that it is a byte register
                assert !value.kind.isFloat() && !value.kind.isDouble() : "can't load floats in byte register";
                LIROperand reg = gen.rlockByte(CiKind.Byte);
                gen.lir.move(res, reg);
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
            LIROperand r = value.operand();
            if (isConstant(r)) {
                result = r;
            } else {
                loadItem();
            }
        } else if (gen.compilation.target.arch.isSPARC()) {
            LIROperand r = value.operand();
            if (gen.canInlineAsConstant(value)) {
                if (!isConstant(r)) {
                    r = forConstant(value);
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
        assert isIllegal(value.operand()) || isConstant(value.operand()) : "operand should never change";
        value.setOperand(opr);

        if (opr.isVariable()) {
            gen.instructionForOperand.put(opr.variableNumber(), value);
        }

        result = opr;
    }

    public void loadItem() {
        if (isIllegal(result())) {
            // update the items result
            result = value.operand();
        }
        if (!result().isRegister()) {
            LIROperand reg = gen.newRegister(value.kind);
            gen.lir.move(result(), reg);
            if (isConstant(result())) {
                result = reg;
            } else {
                setResult(reg);
            }
        }
    }

    public int asInt() {
        assert value instanceof Constant : "must be a constant";
        return value.asConstant().asInt();
    }

    public long asLong() {
        assert value instanceof Constant : "must be a constant";
        return value.asConstant().asLong();
    }

    @Override
    public String toString() {
        return result() + "";
    }
}
