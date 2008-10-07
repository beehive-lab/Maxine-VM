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
/*VCSID=d36953fb-2ec1-4946-896f-c2061c9a5e60*/
package com.sun.max.memory;

import com.sun.max.unsafe.*;

/**
 * @author Bernd Mathiske
 */
public class FixedMemoryRegion implements MemoryRegion {

    private final Address _start;

    public Address start() {
        return _start;
    }

    private final Size _size;

    public Size size() {
        return _size;
    }

    private final Address _end;

    public Address end() {
        return _end;
    }

    public FixedMemoryRegion(Address start, Size size) {
        _start = start;
        _size = size;
        _end = start.plus(size);
    }

    public FixedMemoryRegion(MemoryRegion memoryRegion) {
        _start = memoryRegion.start();
        _size = memoryRegion.size();
        _end = memoryRegion.end();
    }

    public boolean contains(Address address) {
        return address.greaterEqual(start()) && address.lessThan(end());
    }

    public boolean overlaps(MemoryRegion memoryRegion) {
        return (_start.greaterEqual(memoryRegion.start()) && _start.lessThan(memoryRegion.end())) ||
               (_end.greaterEqual(memoryRegion.start()) && _end.lessThan(memoryRegion.end()));
    }
}
