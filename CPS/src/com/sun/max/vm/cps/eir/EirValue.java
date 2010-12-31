/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.cps.collect.*;
import com.sun.max.vm.cps.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public abstract class EirValue implements IrValue {

    private LinkedIdentityHashSet<EirOperand> operands = new LinkedIdentityHashSet<EirOperand>();

    /**
     * Gets the set of operands representing all the uses of this value by the instructions of an EIR method.
     */
    public final IterableWithLength<EirOperand> operands() {
        return operands;
    }

    public boolean isMutable() {
        return numberOfUpdates() + numberOfDefinitions() > 1;
    }

    public void substituteWith(EirValue other) {
        for (EirOperand operand : operands()) {
            other.addOperand(operand);
            operand.setEirValueWithoutUpdate(other);
        }

        operands.clear();
    }

    public EirVariable asVariable() {
        return null;
    }

    public Preallocated asPreallocated() {
        return null;
    }

    private int numberOfDefinitions;

    public int numberOfDefinitions() {
        return numberOfDefinitions;
    }

    private int numberOfUpdates;

    public int numberOfUpdates() {
        return numberOfUpdates;
    }

    private int numberOfUses;

    public int numberOfUses() {
        return numberOfUses;
    }

    public boolean assertLocationCategory() {

        // TODO (tw): Try to get valid location categories for constants!
        if (this.location() == null || this instanceof EirConstant) {
            return true;
        }

        assert this.locationCategories().contains(location().category()) : "location invalid";
        for (EirOperand operand : operands()) {
            assert operand.locationCategories().contains(location().category()) : "invalid location at instruction " + operand.instruction().toString();
        }

        return true;
    }

    public int numberOfEffects() {
        return numberOfDefinitions + numberOfUpdates + numberOfUses;
    }

    public boolean hasDefinitionsOnly() {
        return numberOfUses == 0 && numberOfUpdates == 0;
    }

    public void addOperand(EirOperand operand) {
        locationCategories = null;
        operands.add(operand);
        switch (operand.effect()) {
            case USE:
                numberOfUses++;
                break;
            case UPDATE:
                numberOfUpdates++;
                break;
            case DEFINITION:
                numberOfDefinitions++;
                break;
        }
    }

    public final void removeOperand(EirOperand operand) {
        locationCategories = null;
        operands.remove(operand);
        switch (operand.effect()) {
            case USE:
                numberOfUses--;
                break;
            case UPDATE:
                numberOfUpdates--;
                break;
            case DEFINITION:
                numberOfDefinitions--;
                break;
        }

        assert assertLocationCategory();
    }

    private PoolSet<EirLocationCategory> locationCategories;

    public PoolSet<EirLocationCategory> locationCategories() {
        if (locationCategories == null) {
            locationCategories = EirLocationCategory.all();
            for (EirOperand operand : operands) {
                locationCategories.and(operand.locationCategories());
            }
        }
        return locationCategories;
    }

    public void resetLocationCategories() {
        locationCategories = null;
    }

    private EirLocation location;

    public final EirLocation location() {
        assert !(isLocationFixed && location == null);
        return location;
    }

    public final void setLocation(EirLocation location) {
        assert !isLocationFixed;
        this.location = location;
        assert assertLocationCategory();
    }

    private boolean isLocationFixed;

    public boolean isLocationFixed() {
        assert !(isLocationFixed && location == null);
        return isLocationFixed;
    }

    public void fixLocation(EirLocation loc) {
        assert !(isLocationFixed && this.location != loc);
        assert loc != null : "must not fix location to null!";
        this.location = loc;
        isLocationFixed = true;
    }

    protected EirValue() {
    }

    public static final class Preallocated extends EirValue {
        private final Kind kind;

        @Override
        public Kind kind() {
            return kind;
        }

        @Override
        public Preallocated asPreallocated() {
            return this;
        }

        public Preallocated(EirLocation location, Kind kind) {
            this.kind = kind;
            fixLocation(location);
        }

        private final List<EirOperand> definitions = new LinkedList<EirOperand>();

        public List<EirOperand> definitions() {
            return definitions;
        }

        @Override
        public void recordDefinition(EirOperand operand) {
            definitions.add(operand);
        }

        @Override
        public String toString() {
            return location().toString();
        }
    }

    public void recordDefinition(EirOperand operand) {
        ProgramError.unexpected("trying to update a constant");
    }

    public void recordUse(EirOperand operand) {
    }

    public void cleanup() {
        operands = null;
        locationCategories = null;
    }

    public boolean isConstant() {
        return false;
    }

    public Value value() {
        throw new IllegalArgumentException();
    }

    public Kind kind() {
        throw new IllegalArgumentException();
    }

    public static final class Undefined extends EirValue {
        private Undefined() {
            fixLocation(EirLocation.UNDEFINED);
        }

        @Override
        public void addOperand(EirOperand operand) {
        }
    }

    public static final Undefined UNDEFINED = new Undefined();

}
