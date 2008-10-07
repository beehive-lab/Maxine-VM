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
/*VCSID=8f84cfc1-72bd-4823-b240-aac92f422845*/
package test.com.sun.max.lang;

import com.sun.max.ide.*;

/**
 * @author Bernd Mathiske
 */
public class ClassesTest extends MaxTestCase {
    public ClassesTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ClassesTest.class);
    }

    /**
     * Wanted to know whether forName() can get hold of a package private class.
     * Seems that it can.
     */
    public void test_forName() {
        try {
            Class.forName("java.lang.ref.Finalizer");
        } catch (ClassNotFoundException classNotFoundException) {
        }
    }

}
