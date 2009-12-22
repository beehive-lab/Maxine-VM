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
package com.sun.max.vm.compiler.cps.eir;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.cps.ir.*;
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
    public final Sequence<EirOperand> operands() {
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

        private final AppendableSequence<EirOperand> definitions = new LinkSequence<EirOperand>();

        public Sequence<EirOperand> definitions() {
            return definitions;
        }

        @Override
        public void recordDefinition(EirOperand operand) {
            definitions.append(operand);
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
