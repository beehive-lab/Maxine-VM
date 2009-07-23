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
package com.sun.max.vm.compiler.eir.allocate.linearscan;

import com.sun.max.collect.*;
import com.sun.max.vm.compiler.eir.*;

/**
 * Result of a random execution path. Saves a map from each operand to a sorted list
 * of operands that were the value of the variable used for this operand.
 *
 * @author Thomas Wuerthinger
 */
public class VerificationRunResult {

    private VariableMapping<EirOperand, VariableSequence<EirOperand>> map;
    private VariableSequence<String> blockSequence;

    public VerificationRunResult() {
        map = new ChainedHashMapping<EirOperand, VariableSequence<EirOperand>>();
        blockSequence = new ArrayListSequence<String>();
    }

    public VariableMapping<EirOperand, VariableSequence<EirOperand>> map() {
        return map;
    }

    public VariableSequence<String> blockSequence() {
        return blockSequence;
    }
}
