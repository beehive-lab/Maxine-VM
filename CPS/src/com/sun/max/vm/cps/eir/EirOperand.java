/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.max.vm.cps.eir;

import com.sun.max.collect.*;
import com.sun.max.vm.cps.ir.*;
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
                eirValue.recordDefinition(this);
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
                eirValue.recordUse(this);
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

    private EirValue eirValue;

    public EirValue eirValue() {
        return eirValue;
    }

    public void clearEirValue() {
        if (eirValue != null) {
            eirValue.removeOperand(this);
            eirValue = null;
        }
    }

    void setEirValueWithoutUpdate(EirValue eirValue) {
        assert !eirValue.isLocationFixed() || locationCategories.contains(eirValue.location().category());
        this.eirValue = eirValue;
    }

    public void setEirValue(EirValue eirValue) {
        clearEirValue();
        assert !eirValue.isLocationFixed() || locationCategories.contains(eirValue.location().category());
        this.eirValue = eirValue;
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

    private EirRegister requiredRegister;

    public EirRegister requiredRegister() {
        return requiredRegister;
    }

    public void setRequiredRegister(EirRegister register) {
        assert locationCategories.contains(register.category());
        requiredRegister = register;
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
        return eirValue.location();
    }

    public boolean isConstant() {
        return eirValue.isConstant();
    }

    public Value value() {
        return eirValue.value();
    }

    public Kind kind() {
        return eirValue.kind();
    }

    public void cleanup() {
        locationCategories = null;
        eirValue.cleanup();
    }

    @Override
    public String toString() {
        if (eirValue == null) {
            if (effect == null) {
                return null;
            }
            return effect.name();
        }
        return eirValue.toString();
    }
}
