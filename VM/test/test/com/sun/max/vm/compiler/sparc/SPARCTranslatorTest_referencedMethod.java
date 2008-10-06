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
/*VCSID=31e56a6b-6a12-45f2-be27-b18deb51067c*/
package test.com.sun.max.vm.compiler.sparc;

import junit.framework.*;
import test.com.sun.max.vm.compiler.*;

import com.sun.max.vm.compiler.target.*;


public class SPARCTranslatorTest_referencedMethod extends CompilerTestCase<TargetMethod> {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SPARCTranslatorTest_referencedMethod.suite());
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(SPARCTranslatorTest_referencedMethod.class.getSimpleName());
        suite.addTestSuite(SPARCTranslatorTest_referencedMethod.class);
        return new SPARCTranslatorTestSetup(suite);
    }

    public SPARCTranslatorTest_referencedMethod(String name) {
        super(name);
    }
}
