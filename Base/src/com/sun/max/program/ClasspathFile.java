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
package com.sun.max.program;

import com.sun.max.program.Classpath.*;

/**
 * Encapulates the contents of a file loaded from an {@linkplain Entry entry} on a {@linkplain Classpath classpath}.
 *
 * @author Doug Simon
 */
public final class ClasspathFile {

    /**
     * The bytes of the file represented by this object.
     */
    public final byte[] contents;

    /**
     * The classpath entry from which the file represented by this object was read.
     */
    public final Entry classpathEntry;

    /**
     * Creates an object encapsulating the bytes of a file read via a classpath entry.
     *
     * @param contents the bytes of the file that was read
     * @param classpathEntry the entry from which the file was read
     */
    public ClasspathFile(byte[] contents, Entry classpathEntry) {
        this.classpathEntry = classpathEntry;
        this.contents = contents;
    }
}
