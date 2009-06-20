/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.eir;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * An EIR operand object is created for each use (i.e. definition or read) of an {@linkplain EirValue EIR value}.
 * This layer of indirection between EIR instructions and operands simplifies the task of writing a
 * register allocator.
 *
 * @author Bernd Mathiske
 */
public final class EirOperand implements IrValue {

    public interface Procedure {
        void run(EirOperand operand);
    }

    private final EirInstruction instruction;

    public EirInstruction instruction() {
        return instruction;
    }

    public enum Effect {
        DEFINITION, UPDATE, USE;
    }

    private final Effect effect;

    public Effect effect() {
        return effect;
    }

    public void recordDefinition() {
        switch (effect) {
            case DEFINITION:
            case UPDATE: {
                _eirValue.recordDefinition(this);
                break;
            }
            default: {
                break;
            }
        }
    }

    public void recordUse() {
        switch (effect) {
            case USE:
            case UPDATE: {
                _eirValue.recordUse(this);
                break;
            }
            default: {
                break;
            }
        }
    }

    private PoolSet<EirLocationCategory> locationCategories;

    public PoolSet<EirLocationCategory> locationCategories() {
        return locationCategories;
    }

    private EirValue _eirValue;

    public EirValue eirValue() {
        return _eirValue;
    }

    public void clearEirValue() {
        if (_eirValue != null) {
            _eirValue.removeOperand(this);
            _eirValue = null;
        }
    }

    void setEirValueWithoutUpdate(EirValue eirValue) {
        assert !eirValue.isLocationFixed() || locationCategories.contains(eirValue.location().category());
        _eirValue = eirValue;
    }

    public void setEirValue(EirValue eirValue) {
        clearEirValue();
        assert !eirValue.isLocationFixed() || locationCategories.contains(eirValue.location().category());
        _eirValue = eirValue;
        eirValue.addOperand(this);
    }

    private int weight;

    public int weight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public EirOperand(EirInstruction instruction, Effect effect, PoolSet<EirLocationCategory> locationCategories) {
        this.instruction = instruction;
        this.effect = effect;
        this.locationCategories = locationCategories;
    }

    private EirRegister preferredRegister;

    public EirRegister preferredRegister() {
        return preferredRegister;
    }

    public void setPreferredRegister(EirRegister register) {
        assert locationCategories.contains(register.category());
        preferredRegister = register;
    }

    private EirRegister _requiredRegister;

    public EirRegister requiredRegister() {
        return _requiredRegister;
    }

    public void setRequiredRegister(EirRegister register) {
        assert locationCategories.contains(register.category());
        _requiredRegister = register;
        setPreferredRegister(register);
    }

    private EirLocation requiredLocation;

    public EirLocation requiredLocation() {
        return requiredLocation;
    }

    public void setRequiredLocation(EirLocation location) {
        assert locationCategories.contains(location.category());
        requiredLocation = location;
        if (location.asRegister() != null) {
            setRequiredRegister(location.asRegister());
        }
    }

    public EirLocation location() {
        return _eirValue.location();
    }

    public boolean isConstant() {
        return _eirValue.isConstant();
    }

    public Value value() {
        return _eirValue.value();
    }

    public Kind kind() {
        return _eirValue.kind();
    }

    public void cleanup() {
        locationCategories = null;
        _eirValue.cleanup();
    }

    @Override
    public String toString() {
        if (_eirValue == null) {
            if (effect == null) {
                return null;
            }
            return effect.name();
        }
        return _eirValue.toString();
    }
}
