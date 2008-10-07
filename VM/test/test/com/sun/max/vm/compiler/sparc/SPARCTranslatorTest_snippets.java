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
/*VCSID=a1c2dbfa-ed4c-4086-9b3a-e2023207d920*/
package test.com.sun.max.vm.compiler.sparc;

import junit.framework.*;
import test.com.sun.max.vm.compiler.*;

import com.sun.max.io.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.compiler.target.*;

@org.junit.runner.RunWith(org.junit.runners.AllTests.class)
public class SPARCTranslatorTest_snippets extends CompilerTestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(SPARCTranslatorTest_snippets.suite());
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(SPARCTranslatorTest_snippets.class.getSimpleName());
        //$JUnit-BEGIN$
        suite.addTestSuite(SPARCTranslatorTest_snippets.class);
        //$JUnit-END$
        return new SPARCTranslatorTestSetup(suite); // This performs the test
    }

    public SPARCTranslatorTest_snippets(String name) {
        super(name);
    }

    public void test() {
        for (Snippet snippet : Snippet.snippets()) {
            Trace.line(1, "snippet " + snippet.name() + ":");
            final TargetMethod targetMethod = (TargetMethod) compilerTestSetup().translate(snippet.classMethodActor());
            targetMethod.traceBundle(IndentWriter.traceStreamWriter());
        }
    }
}
