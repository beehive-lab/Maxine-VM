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
