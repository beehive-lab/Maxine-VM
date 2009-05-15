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
import com.sun.max.program.*;
import com.sun.max.vm.compiler.ir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public abstract class EirValue implements IrValue {

    private LinkedIdentityHashSet<EirOperand> _operands = new LinkedIdentityHashSet<EirOperand>();

    /**
     * Gets the set of operands representing all the uses of this value by the instructions of an EIR method.
     */
    public final Sequence<EirOperand> operands() {
        return _operands;
    }



    public void substituteWith(EirValue other) {
        for (EirOperand operand : operands()) {
            other.addOperand(operand);
            operand.setEirValueWithoutUpdate(other);
        }

        _operands.clear();
    }

    public EirVariable asVariable() {
        return null;
    }

    public Preallocated asPreallocated() {
        return null;
    }

    private int _numberOfDefinitions;

    public int numberOfDefinitions() {
        return _numberOfDefinitions;
    }

    private int _numberOfUpdates;

    public int numberOfUpdates() {
        return _numberOfUpdates;
    }

    private int _numberOfUses;

    public int numberOfUses() {
        return _numberOfUses;
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
        return _numberOfDefinitions + _numberOfUpdates + _numberOfUses;
    }

    public boolean hasDefinitionsOnly() {
        return _numberOfUses == 0 && _numberOfUpdates == 0;
    }

    public void addOperand(EirOperand operand) {
        _locationCategories = null;
        _operands.add(operand);
        switch (operand.effect()) {
            case USE:
                _numberOfUses++;
                break;
            case UPDATE:
                _numberOfUpdates++;
                break;
            case DEFINITION:
                _numberOfDefinitions++;
                break;
        }
    }

    public final void removeOperand(EirOperand operand) {
        _locationCategories = null;
        _operands.remove(operand);
        switch (operand.effect()) {
            case USE:
                _numberOfUses--;
                break;
            case UPDATE:
                _numberOfUpdates--;
                break;
            case DEFINITION:
                _numberOfDefinitions--;
                break;
        }

        assert assertLocationCategory();
    }

    private PoolSet<EirLocationCategory> _locationCategories;

    public PoolSet<EirLocationCategory> locationCategories() {
        if (_locationCategories == null) {
            _locationCategories = EirLocationCategory.all();
            for (EirOperand operand : _operands) {
                _locationCategories.and(operand.locationCategories());
            }
        }
        return _locationCategories;
    }

    public void resetLocationCategories() {
        _locationCategories = null;
    }

    private EirLocation _location;

    public final EirLocation location() {
        assert !(_isLocationFixed && _location == null);
        return _location;
    }

    public final void setLocation(EirLocation location) {
        assert !_isLocationFixed;
        _location = location;

        assert assertLocationCategory();
    }

    private boolean _isLocationFixed;

    public boolean isLocationFixed() {
        assert !(_isLocationFixed && _location == null);
        return _isLocationFixed;
    }

    public void fixLocation(EirLocation location) {
        assert !(_isLocationFixed && location != _location);
        assert location != null : "must not fix location to null!";
        _location = location;
        _isLocationFixed = true;
    }

    protected EirValue() {
    }

    public static final class Preallocated extends EirValue {
        private final Kind _kind;

        @Override
        public Kind kind() {
            return _kind;
        }

        @Override
        public Preallocated asPreallocated() {
            return this;
        }

        public Preallocated(EirLocation location, Kind kind) {
            super();
            _kind = kind;
            fixLocation(location);
        }

        private final AppendableSequence<EirOperand> _definitions = new LinkSequence<EirOperand>();

        public Sequence<EirOperand> definitions() {
            return _definitions;
        }

        @Override
        public void recordDefinition(EirOperand operand) {
            _definitions.append(operand);
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
        _operands = null;
        _locationCategories = null;
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
