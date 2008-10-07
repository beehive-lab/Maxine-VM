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
/*VCSID=01a34d41-9f65-4b08-83f9-6486d8bbdc37*/
package com.sun.max.vm.compiler.eir.sparc;

import com.sun.max.vm.*;
import com.sun.max.vm.compiler.eir.*;
import com.sun.max.vm.compiler.eir.EirStackSlot.*;
import com.sun.max.vm.type.*;

/**
 * @author Michael Bebenita
 */
public class SPARCEirTreeABI extends SPARCEirJavaABI {
    public SPARCEirTreeABI(VMConfiguration vmConfiguration) {
        super(vmConfiguration);
    }

    /**
     * Trees expect all their values on the stack.
     */
    @Override
    public EirLocation[] getParameterLocations(Purpose stackSlotPurpose, Kind... kinds) {
        return createStackParameterLocations(stackSlotPurpose, kinds);
    }
}
