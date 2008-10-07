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
/*VCSID=cf4abbbd-c83c-462b-9c9b-ff8a9a176194*/
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

    private final EirInstruction _instruction;

    public EirInstruction instruction() {
        return _instruction;
    }

    public enum Effect {
        DEFINITION, UPDATE, USE;
    }

    private final Effect _effect;

    public Effect effect() {
        return _effect;
    }

    public void recordDefinition() {
        switch (_effect) {
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
        switch (_effect) {
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

    private PoolSet<EirLocationCategory> _locationCategories;

    public PoolSet<EirLocationCategory> locationCategories() {
        return _locationCategories;
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

    public void setEirValue(EirValue eirValue) {
        clearEirValue();
        _eirValue = eirValue;
        eirValue.addOperand(this);
    }

    private int _weight;

    public int weight() {
        return _weight;
    }

    public void setWeight(int weight) {
        _weight = weight;
    }

    public EirOperand(EirInstruction instruction, Effect effect, PoolSet<EirLocationCategory> locationCategories) {
        _instruction = instruction;
        _effect = effect;
        _locationCategories = locationCategories;
    }

    private EirRegister _preferredRegister;

    public EirRegister preferredRegister() {
        return _preferredRegister;
    }

    public void setPreferredRegister(EirRegister register) {
        assert _locationCategories.contains(register.category());
        _preferredRegister = register;
    }

    private EirRegister _requiredRegister;

    public EirRegister requiredRegister() {
        return _requiredRegister;
    }

    public void setRequiredRegister(EirRegister register) {
        assert _locationCategories.contains(register.category());
        _requiredRegister = register;
        setPreferredRegister(register);
    }

    private EirLocation _requiredLocation;

    public EirLocation requiredLocation() {
        return _requiredLocation;
    }

    public void setRequiredLocation(EirLocation location) {
        assert _locationCategories.contains(location.category());
        _requiredLocation = location;
        if (location instanceof EirRegister) {
            setRequiredRegister((EirRegister) location);
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
        _locationCategories = null;
        _eirValue.cleanup();
    }

    @Override
    public String toString() {
        if (_eirValue == null) {
            if (_effect == null) {
                return null;
            }
            return _effect.name();
        }
        return _eirValue.toString();
    }
}
