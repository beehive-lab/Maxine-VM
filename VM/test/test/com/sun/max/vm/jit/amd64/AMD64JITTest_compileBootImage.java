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
/*VCSID=57ba68b0-1ab1-4377-9a63-1ce77b58186d*/
package test.com.sun.max.vm.jit.amd64;

import junit.framework.*;
import test.com.sun.max.vm.jit.*;


/**
 * Runs unit test JITTest_compileBootImage for AMD64.
 * @see JITTest_compileBootImage
 * @author Laurent Daynes
 */
@org.junit.runner.RunWith(org.junit.runners.AllTests.class)
public class AMD64JITTest_compileBootImage extends JITTest_compileBootImage {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AMD64JITTest_compileBootImage.suite());
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(AMD64JITTest_compileBootImage.class.getSimpleName());
        // $JUnit-BEGIN$
        suite.addTestSuite(AMD64JITTest_compileBootImage.class);
        // $JUnit-END$
        return new AMD64JITTestSetup(suite);
    }
}
