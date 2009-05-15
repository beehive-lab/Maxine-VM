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
package com.sun.max.vm.compiler.eir.allocate.linearscan.parts;

import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.allocate.linearscan.*;

/**
 * Assigns a unique even number to each instruction based on the linear scan order.
 *
 * @author Thomas Wuerthinger
 */
public class NumberInstructions extends AlgorithmPart {

    public NumberInstructions() {
        super(4);
    }

    @Override
    protected boolean assertPreconditions() {
        assert data().linearScanOrder() != null;
        return super.assertPreconditions();
    }

    @Override
    protected void doit() {
        int z = 0;
        for (EirBlock block : data().linearScanOrder()) {
            final int beginNumber = z;
            z += 2;

            for (EirInstruction instruction : block.instructions()) {

                instruction.setNumber(z);
                z += 2;
            }

            block.setNumbers(beginNumber, z);
        }
    }
}
