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
/*VCSID=d795a44c-b30b-4f25-8cf6-5bc65d1ff0ca*/
package com.sun.max.vm.compiler.eir;

import com.sun.max.collect.*;
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
    Sequence<EirValue> calleeSavedRegisterVariables();

    EirABI abi();

    /**
     * Gets the actor for the method currently being analyzed.
     */
    MethodActor classMethodActor();

}
