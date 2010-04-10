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

import static com.sun.cri.ci.CiValue.*;

import com.sun.c1x.alloc.OperandPool.*;
import com.sun.c1x.ir.*;
import com.sun.c1x.util.*;
import com.sun.cri.ci.*;

/**
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 */
public class LIRItem {

    public Value value;
    private final LIRGenerator gen;
    private CiValue result;
    private boolean destroysRegister;
    private CiValue newResult;

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

    private CiKind nonWordKind(CiKind kind) {
        if (kind.isWord()) {
            return gen.is64 ? CiKind.Long : CiKind.Int;
        }
        return kind;
    }

    public LIRItem loadItemForce(CiValue reg) {
        CiValue r = result();
        if (r != reg) {
            assert r.kind != CiKind.Illegal;
            if (nonWordKind(r.kind) != nonWordKind(reg.kind)) {
                // moves between different types need an intervening spill slot
                CiValue tmp = gen.forceToSpill(r, reg.kind);
                gen.lir.move(tmp, reg);
            } else {
                gen.lir.move(r, reg);
            }
            result = reg;
        }
        return this;
    }

    public LIRItem loadItem(CiKind kind) {
        if (kind == CiKind.Byte || kind == CiKind.Boolean) {
            loadByteItem();
        } else {
            loadItem();
        }
        return this;
    }

    public void loadForStore(CiKind kind) {
        if (gen.canStoreAsConstant(value, kind)) {
            result = value.operand();
            if (!result.isConstant()) {
                result = value.asConstant();
            }
        } else if (kind == CiKind.Byte || kind == CiKind.Boolean) {
            loadByteItem();
        } else {
            loadItem();
        }
    }

    public CiValue result() {
        assert !destroysRegister || (!result.isVariableOrRegister() || result.isVariable()) : "shouldn't use setDestroysRegister with physical registers";
        if (destroysRegister && result.isVariableOrRegister()) {
            if (newResult.isIllegal()) {
                newResult = gen.newVariable(value.kind);
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
        return result.isStackSlot();
    }

    public boolean isRegister() {
        return result.isVariableOrRegister();
    }

    public void loadByteItem() {
        if (gen.compilation.target.arch.isX86()) {
            loadItem();
            CiValue res = result();

            if (!res.isVariable() || !gen.operands().mustBeByteRegister(res.asLocation())) {
                // make sure that it is a byte register
                assert !value.kind.isFloat() && !value.kind.isDouble() : "can't load floats in byte register";
                CiValue reg = gen.operands().newVariable(CiKind.Byte, VariableFlag.MustBeByteRegister);
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
            CiValue r = value.operand();
            if (r.isConstant()) {
                result = r;
            } else {
                loadItem();
            }
        } else if (gen.compilation.target.arch.isSPARC()) {
            CiValue r = value.operand();
            if (gen.canInlineAsConstant(value)) {
                if (!r.isConstant()) {
                    r = value.asConstant();
                }
                result = r;
            } else {
                loadItem();
            }
        } else {
            Util.shouldNotReachHere();
        }
    }

    void setResult(CiVariable operand) {
        assert value.operand().isIllegal() || value.operand().isConstant() : "operand should never change";
        value.setOperand(operand);
        gen.operands().recordResult(operand, value);
        result = operand;
    }

    public LIRItem loadItem() {
        if (result().isIllegal()) {
            // update the items result
            result = value.operand();
        }
        if (!result().isVariableOrRegister()) {
            CiVariable operand = gen.newVariable(value.kind);
            gen.lir.move(result(), operand);
            if (result().isConstant()) {
                result = operand;
            } else {
                setResult(operand);
            }
        }
        return this;
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
