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
/*VCSID=3e1ea90f-35c1-4da7-9768-cdc0ae4683f8*/
package com.sun.max.tele.debug;

import java.io.*;

import com.sun.max.unsafe.*;

public interface DataStreamFactory {

    /**
     * Creates an input stream for reading at most {@code size} bytes from memory starting at {@code address}.
     */
    InputStream createInputStream(Address address, int size);

    /**
     * Creates an output stream for writing at most {@code size} bytes to memory starting at {@code address}.
     */
    OutputStream createOutputStream(Address address, int size);

}
