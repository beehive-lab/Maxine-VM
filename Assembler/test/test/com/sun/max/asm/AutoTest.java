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
/*VCSID=38ec8c1d-a7f3-4cf6-a066-04d7c1de5bdc*/
/*
 * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved. Use is subject to license terms.
 */
package test.com.sun.max.asm;

import junit.framework.*;

@org.junit.runner.RunWith(org.junit.runners.AllTests.class)
public final class AutoTest {

    private AutoTest() {
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AutoTest.suite());
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(AllTests.class.getPackage().getName());
        //$JUnit-BEGIN$
        suite.addTestSuite(MethodAnnotationsTest.class);
        suite.addTestSuite(test.com.sun.max.asm.sparc.InternalTest.class);
        suite.addTestSuite(test.com.sun.max.asm.ia32.InternalTest.class);
        suite.addTestSuite(test.com.sun.max.asm.amd64.InternalTest.class);
        suite.addTestSuite(test.com.sun.max.asm.ppc.InternalTest.class);
        //$JUnit-END$
        return suite;
    }
}
