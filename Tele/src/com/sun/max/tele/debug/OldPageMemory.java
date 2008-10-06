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
/*VCSID=929496d4-89ba-44fa-bb0b-aa60137a77b4*/
package com.sun.max.tele.debug;

import java.io.*;
import java.util.*;

import com.sun.max.unsafe.*;

public class OldPageMemory {

    private ProcMemory _procMemory;

    public OldPageMemory(ProcMemory procMemory) {
        _procMemory = procMemory;
    }

    public void close() throws IOException {
        _procMemory.close();
    }

    private final Map<Long, byte[]> _pages = new HashMap<Long, byte[]>();

    private byte[] getPageBytes(Address pageBase) throws IOException {
        byte[] pageBytes = _pages.get(pageBase.toLong());
        if (pageBytes == null) {
            pageBytes = new byte[_procMemory.pageSize()];
            _procMemory.readPage(pageBase, pageBytes);
            _pages.put(pageBase.toLong(), pageBytes);
        }
        return pageBytes;
    }

    public synchronized byte readByte(Address address) throws IOException {
        final Address pageBase = address.roundedDownBy(_procMemory.pageSize());
        final int index = address.remainder(_procMemory.pageSize());
        return getPageBytes(pageBase)[index];
    }

    public synchronized void writeByte(Address address, byte value) throws IOException {
        final Address pageBase = address.roundedDownBy(_procMemory.pageSize());
        final int index = address.remainder(_procMemory.pageSize());
        final byte[] pageBytes = getPageBytes(address);
        pageBytes[index] = value;
        _procMemory.writePage(pageBase, pageBytes);
    }

    public void clear() {
        _pages.clear();
    }

}
