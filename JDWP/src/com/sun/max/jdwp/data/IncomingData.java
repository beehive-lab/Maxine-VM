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
/*VCSID=f6fb472d-8ec6-4fb5-a4de-17196c9b8e43*/

package com.sun.max.jdwp.data;

import java.io.*;

/**
 * This class represents data coming from a JDWP stream.
 *
 * @author Thomas Wuerthinger
 */
public interface IncomingData {

    /**
     * Uses the given JDWPInputStream object to read in the values of this object.
     *
     * @param inputStream the stream used to read the values
     * @throws IOException this exception is thrown when an error occurred while reading the bytes
     * @throws JDWPException this exception is thrown when an error occurred while translating the bytes according to
     *             JDWP semantics
     */
    void read(JDWPInputStream inputStream) throws IOException, JDWPException;
}
