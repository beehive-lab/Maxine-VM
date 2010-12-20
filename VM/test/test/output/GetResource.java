/*
 * Copyright (c) 2008 Sun Microsystems, Inc.  All rights reserved.
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
package test.output;

import java.io.*;

/**
 * A simple program to test the Class.getResource().
 *
 * @author Ben L. Titzer
 */
public class GetResource {
    private static final String FILE_NAME = "GetResource.input";

    public static void main(String[] args) throws IOException {
        final InputStream input = GetResource.class.getResourceAsStream(FILE_NAME);
        if (input != null) {
            final byte[] buffer = new byte[128];
            System.out.println(FILE_NAME + ": ");
            while (input.available() > 0) {
                final int length = input.read(buffer);
                System.out.write(buffer, 0, length);
            }
            System.out.println("done.");
        } else {
            System.out.println("Could not get resource: " + FILE_NAME);
        }
    }
}
