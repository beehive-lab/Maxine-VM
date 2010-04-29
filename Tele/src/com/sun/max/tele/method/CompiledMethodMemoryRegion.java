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

import com.sun.max.collect.*;
import com.sun.max.tele.*;
import com.sun.max.tele.memory.*;
import com.sun.max.unsafe.*;

/**
 * Represents a region of VM memory that holds compiled code.
 * <br>
 * The parent of this region is the {@link TeleCompiledCodeRegion} in which it is allocated.
 * <br>
 * This region has no children (although it could if we decided to subdivide it further);
 *
 * @author Michael Van De Vanter
  */
public abstract class CompiledMethodMemoryRegion extends TeleFixedMemoryRegion implements MaxEntityMemoryRegion<MaxCompiledCode>{

    private static final IndexedSequence<MaxEntityMemoryRegion< ? extends MaxEntity>> EMPTY =
        new ArrayListSequence<MaxEntityMemoryRegion< ? extends MaxEntity>>(0);
    private final MaxCompiledCode maxCompiledCode;

    public CompiledMethodMemoryRegion(TeleVM teleVM, MaxCompiledCode maxCompiledCode, Address start, Size size, String regionName) {
        super(teleVM, regionName, start, size);
        this.maxCompiledCode = maxCompiledCode;
    }

    public MaxEntityMemoryRegion< ? extends MaxEntity> parent() {
        // TODO (mlvdv) fix
        return null;
    }

    public IndexedSequence<MaxEntityMemoryRegion< ? extends MaxEntity>> children() {
        return EMPTY;
    }

    public MaxCompiledCode owner() {
        return maxCompiledCode;
    }

    public boolean isBootRegion() {
        return false;
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
