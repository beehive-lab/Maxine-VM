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
/*VCSID=49964826-747a-4ccd-a1e4-2e5bc7600e1a*/
package test.com.sun.max.vm.jit.amd64;

import junit.framework.*;
import test.com.sun.max.vm.jit.*;


/**
 * Runs unit test JITTest_resolvedGetField for AMD64.
 * @see JITTest_resolvedGetField
 * @author Laurent Daynes
 */
@org.junit.runner.RunWith(org.junit.runners.AllTests.class)
public class AMD64JITTest_resolvedGetField extends JITTest_resolvedGetField {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AMD64JITTest_resolvedGetField.suite());
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(AMD64JITTest_resolvedGetField.class.getSimpleName());
        // $JUnit-BEGIN$
        suite.addTestSuite(AMD64JITTest_resolvedGetField.class);
        // $JUnit-END$
        return new AMD64JITTestSetup(suite);
    }
}
