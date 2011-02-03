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

import com.sun.max.vm.actor.member.*;

/**
 * An interface by which a client can analyze the variables and values used and defined by an {@link EirInstruction}.
 * This is done by passing an instance of this interface to {@link AEirInstruction#collectLiveRangeEvents(EirDefUseAnalyzer)}.
 *
 * @author Doug Simon
 */
public interface EirLiveRangeAnalyzer {

    /**
     * Gets the variables assigned for the incoming variables of the {@linkplain #classMethodActor() method} being
     * analyzed.
     */
    EirValue[] parameters();

    /**
     * Notifies this analyzer of a variable defined by the current instruction being analyzed.
     */
    void define(EirValue variable);

    /**
     * Notifies this analyzer of a value used by the current instruction being analyzed.
     */
    void use(EirValue value);

    /**
     * Gets a variable used to represent a given location.
     */
    EirValue variableForLocation(EirLocation location);

    /**
     * Notifies this analyzer that the variables corresponding to the registers used for parameter passing (as specified
     * by the {@linkplain #abi() ABI}) are defined by the current instruction being analyzed.
     */
    List<EirValue> calleeSavedRegisterVariables();

    EirABI abi();

    /**
     * Gets the actor for the method currently being analyzed.
     */
    MethodActor classMethodActor();

}
