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
package com.sun.max.ins.debug;

import com.sun.max.tele.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.value.*;

/**
 * Wrapper for the description of a machine register in the VM that
 * adds a history of the values.
 *
 * @author Michael Van De Vanter
 */
public final class RegisterHistory {

    private final MaxRegister register;
    private final ValueHistory<Value> valueHistory;

    public RegisterHistory(MaxRegister register) {
        this.register = register;
        this.valueHistory = new ArrayValueHistory<Value>(6);
    }

    public String name() {
        return register.name();
    }

    /**
     * @return the current value of the register, as cached by most recent {@link #refresh()}.
     */
    public Value value() {
        return valueHistory.get();
    }

    /**
     * @return the age, in generations, of the current value, since recording began.
     * 0 if different from immediate predecessor; -1 if no different value ever recorded
     */
    public int age() {
        return valueHistory.getAge();
    }

    /**
     * Read and cache the current value of the register; increment generation count.
     */
    public void refresh() {
        final Address address = register.value();
        valueHistory.add(new WordValue(address));
    }

}
