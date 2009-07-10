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
package com.sun.max.vm.heap;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

/**
 * Class to capture common methods for heap scheme implementations.
 *
 * @author Mick Jordan
 */

public abstract class HeapSchemeAdaptor extends AbstractVMScheme implements HeapScheme {

    /**
     * Switch to turn off allocation globally.
     */
    protected boolean allocationSwitch = true;

    public HeapSchemeAdaptor(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    public boolean decreaseMemory(Size amount) {
        return false;
    }

    public boolean increaseMemory(Size amount) {
        return false;
    }

    public void disableAllocationsGlobally() {
        if (!allocationSwitch) {
            FatalError.unexpected("Global allocation Switch already turned off.");
        }
        allocationSwitch = false;
    }

    public void enableAllocationsGlobally() {
        allocationSwitch = true;
    }

    public void disableAllocationsLocally() {
        if (VmThreadLocal.ALLOCATION_SWITCH.getVariableWord().equals(Word.zero())) {
            FatalError.unexpected("Local allocation Switch already turned off.");
        }
        VmThreadLocal.ALLOCATION_SWITCH.setVariableWord(Word.zero());
    }

    public void enableAllocationsLocally() {
        VmThreadLocal.ALLOCATION_SWITCH.setVariableWord(Word.allOnes());
    }

}
