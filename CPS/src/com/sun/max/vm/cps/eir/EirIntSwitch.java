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
package com.sun.max.vm.cps.eir;

public abstract class EirIntSwitch<EirInstructionVisitor_Type extends EirInstructionVisitor,
    EirTargetEmitter_Type extends EirTargetEmitter>  extends EirSwitch<EirInstructionVisitor_Type, EirTargetEmitter_Type> {

    protected static enum INT_SWITCH_SELECTED_IMPLEMENTATION {
            COMPARE_AND_BRANCH,
            TABLE_SWITCH,
            LOOKUP_SWITCH
    }

    public static final int COMPARE_AND_BRANCH_MAX_SIZE = 3;
    public static final double TABLE_SWITCH_MIN_DENSITY_PERCENT = 25.0;

    private INT_SWITCH_SELECTED_IMPLEMENTATION selectImplementation() {
        if (numberOfSwitchKeys() <= COMPARE_AND_BRANCH_MAX_SIZE) {
            return INT_SWITCH_SELECTED_IMPLEMENTATION.COMPARE_AND_BRANCH;
        }
        final double keyDensityPercentage = (100.0 * numberOfSwitchKeys()) / numberOfTableElements();
        if (keyDensityPercentage >= TABLE_SWITCH_MIN_DENSITY_PERCENT) {
            return INT_SWITCH_SELECTED_IMPLEMENTATION.TABLE_SWITCH;
        } else if (numberOfSwitchKeys() <= COMPARE_AND_BRANCH_MAX_SIZE + 1) {
            return INT_SWITCH_SELECTED_IMPLEMENTATION.COMPARE_AND_BRANCH;
        } else {
            return INT_SWITCH_SELECTED_IMPLEMENTATION.LOOKUP_SWITCH;
        }
    }

    INT_SWITCH_SELECTED_IMPLEMENTATION selectedImplementation;

    protected INT_SWITCH_SELECTED_IMPLEMENTATION selectedImplementation() {
        return selectedImplementation;
    }

    public EirIntSwitch(EirBlock block, EirValue tag, EirValue[] matches, EirBlock[] targets, EirBlock defaultTarget) {
        super(block, tag, matches, targets, defaultTarget);
        selectedImplementation = selectImplementation();
    }

    protected int minMatchValue() {
        return matches()[0].value().asInt();
    }

    protected int maxMatchValue() {
        return matches()[matches().length - 1].value().asInt();
    }

    protected int numberOfTableElements() {
        assert minMatchValue() <= maxMatchValue();
        return maxMatchValue() - minMatchValue() + 1;
    }

    protected int numberOfSwitchKeys() {
        return matches().length;
    }

    protected abstract void assembleCompareAndBranch(EirTargetEmitter_Type emitter);
    protected abstract void assembleTableSwitch(EirTargetEmitter_Type emitter);
    protected abstract void assembleLookupSwitch(EirTargetEmitter_Type emitter);

    @Override
    public void emit(EirTargetEmitter_Type emitter) {
        switch(selectedImplementation) {
            case COMPARE_AND_BRANCH:
                assembleCompareAndBranch(emitter);
                break;
            case TABLE_SWITCH:
                assembleTableSwitch(emitter);
                break;
            case LOOKUP_SWITCH:
                assembleLookupSwitch(emitter);
                break;
        }
    }
}
