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
package com.sun.max.tele.method;

import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.unsafe.*;

/**
 * Represents a region of VM memory that holds compiled code.
 *
 * @author Michael Van De Vanter
  */
public abstract class TargetCodeRegion extends TeleFixedMemoryRegion {

    private final TeleTargetRoutine teleTargetRoutine;

    public TargetCodeRegion(TeleVM teleVM, TeleTargetRoutine teleTargetRoutine, Address start, Size size, String description) {
        super(teleVM, description, start, size);
        this.teleTargetRoutine = teleTargetRoutine;
    }

    public TeleTargetRoutine teleTargetRoutine() {
        return teleTargetRoutine;
    }

    /**
     * Does this region of compiled code contain a particular location.
     * Always false if the location is not a compiled location.
     *
     * @param codeLocation location of a code instruction in the VM
     * @return whether the code instruction is a target instruction in this region
     */
    public boolean contains(MaxCodeLocation codeLocation) {
        if (codeLocation.hasAddress()) {
            return contains(codeLocation.address());
        }
        return false;
    }

}
