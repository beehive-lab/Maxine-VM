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
package com.sun.max.io;

import java.io.*;

/**
 * A readable source is a character data source that provides a Reader to read the data.
 *
 * @author Doug Simon
 */
public interface ReadableSource {

    /**
     * @param buffered if true, the returned reader is guaranteed to be a BufferedReader
     * 
     * @return a reader to read the character data represented by this source
     */
    Reader reader(boolean buffered) throws IOException;

    public static final class Static {

        private Static() {

        }

        /**
         * Creates a ReadableSource to provides readers for the characters in a string.
         */
        public static ReadableSource fromString(final String s) {
            return new ReadableSource() {
                public Reader reader(boolean buffered) throws IOException {
                    return buffered ? new BufferedReader(new StringReader(s)) : new StringReader(s);
                }
            };
        }

        /**
         * Creates a ReadableSource to provides readers for the characters in a file.
         */
        public static ReadableSource fromFile(final File file) {
            return new ReadableSource() {
                public Reader reader(boolean buffered) throws IOException {
                    return buffered ? new BufferedReader(new FileReader(file)) : new FileReader(file);
                }
            };

        }
    }
}
