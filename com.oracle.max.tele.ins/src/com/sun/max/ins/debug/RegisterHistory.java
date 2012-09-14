/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.debug;

import com.sun.max.ins.*;
import com.sun.max.tele.*;
import com.sun.max.util.*;
import com.sun.max.vm.value.*;

/**
 * Wrapper for the description of a machine register in the VM that
 * adds a history of the values.
 */
public final class RegisterHistory extends AbstractInspectionHolder {

    private final MaxRegister register;
    private final ArrayValueHistory<Value> valueHistory;
    private MaxVMState lastRefreshedVMState = null;

    /**
     * Creates a history of values for a VM register.
     *
     * @param inspection
     * @param nGenerations the number of history generations to record
     * @param register the VM register whose values are to be recorded
     */
    public RegisterHistory(Inspection inspection, int nGenerations, MaxRegister register) {
        super(inspection);
        this.register = register;
        this.valueHistory = new ArrayValueHistory<Value>(nGenerations);
    }

    /**
     * @return the name of the register
     */
    public String name() {
        return register.name();
    }

    /**
     * @return the current value of the register, as cached by most recent {@link #refresh()}.
     */
    public Value value() {
        return valueHistory.value(0);
    }

    /**
     * @return the age, in generations, of the current value, since recording began.
     * 0 if different from immediate predecessor; -1 if no different value ever recorded
     */
    public int age() {
        return valueHistory.currentValueAge();
    }

    /**
     * Read and cache the current value of the register; increments generation count
     * if the VM state has advanced since the previous refresh.
     */
    public void refresh() {
        final MaxVMState newVMState = inspection().vm().state();
        final WordValue wordValue = new WordValue(register.value());
        if (newVMState.newerThan(lastRefreshedVMState)) {
            valueHistory.addNew(wordValue);
            this.lastRefreshedVMState = newVMState;
        } else {
            valueHistory.updateCurrent(wordValue);
        }
    }

}
