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
/*VCSID=69cd50b3-5011-41cc-813b-e086a8afed91*/
package com.sun.max.tele.debug;

import java.io.*;

import com.sun.max.unsafe.*;

public class ProcMemory {

    private final int _processID;
    private final int _pageSize;

    public int pageSize() {
        return _pageSize;
    }

    private final RandomAccessFile _file;

    public ProcMemory(int processID, int pageSize) throws FileNotFoundException {
        _processID = processID;
        _pageSize = pageSize;
        _file = new RandomAccessFile("/proc/" + _processID + "/mem", "rw");
    }

    private native int nativeRead(int fd, byte[] pageData);

    public synchronized void readPage(Address address, byte[] pageData) throws IOException {
        if (pageData.length != _pageSize) {
            throw new IOException("wrong page size");
        }
        _file.seek(address.toLong());
        final Integer fd = (Integer) WithoutAccessCheck.getInstanceField(_file.getFD(), "fd");
        final int n = nativeRead(fd, pageData);
        if (n != _pageSize) {
            throw new IOException("could not read page");
        }
    }

    public synchronized void writePage(Address address, byte[] pageData) throws IOException {
        if (pageData.length != _pageSize) {
            throw new IOException("wrong page size");
        }
        _file.seek(address.toLong());
        _file.write(pageData);
    }

    public synchronized void close() throws IOException {
        _file.close();
    }

}
