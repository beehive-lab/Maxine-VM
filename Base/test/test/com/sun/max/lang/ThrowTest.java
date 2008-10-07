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
/*VCSID=2e89b081-cbbb-40ef-9098-c960e1eca7fa*/
package test.com.sun.max.lang;

import java.io.*;

import com.sun.max.ide.*;

/**
 * Find out whether throwing an exception in a finally clause cancels out another exception in propagation flight.
 *
 * @author Bernd Mathiske
 */
public class ThrowTest extends MaxTestCase {

    public ThrowTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ThrowTest.class);
    }

    private void throwArithmeticException() throws ArithmeticException {
        throw new ArithmeticException();
    }

    private void throwIOExcepion() throws IOException {
        throw new IOException();
    }

    public void test() {
        try {
            try {
                throwArithmeticException();
            } finally {
                throwIOExcepion();
            }
        } catch (IOException ioException) {
            System.out.println(ioException);
        }
    }

}
