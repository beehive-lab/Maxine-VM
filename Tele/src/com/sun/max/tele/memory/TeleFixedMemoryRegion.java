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
package com.sun.max.tele.memory;

import com.sun.max.tele.*;
import com.sun.max.unsafe.*;

/**
 * Representation of a span of memory in the VM where the description is immutable.
 *
 * @author Michael Van De Vanter
 */
public class TeleFixedMemoryRegion extends TeleMemoryRegion {

    private final String regionName;
    private final Address start;
    private final Size size;

    public TeleFixedMemoryRegion(TeleVM teleVM, String regionName, Address start, Size size) {
        super(teleVM);
        this.start = start;
        this.size = size;
        this.regionName = regionName;
    }

    public TeleFixedMemoryRegion(TeleVM teleVM, String regionName, TeleMemoryRegion memoryRegion) {
        this(teleVM, regionName, memoryRegion.start(), memoryRegion.size());
    }

    public final String regionName() {
        return regionName;
    }

    // TODO (mlvdv)  make final when all sorted out concerning code regions
    public Address start() {
        return start;
    }

    // TODO (mlvdv) make final when all sorted out
    public Size size() {
        return size;
    }

}
