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
package test.com.sun.max.vm.bytecode;

import java.util.Arrays;

import junit.framework.*;
import test.com.sun.max.vm.*;

import com.sun.max.lang.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.classfile.*;

@org.junit.runner.RunWith(org.junit.runners.AllTests.class)
public class BytecodePreprocessorTest extends VmTestCase {

    public BytecodePreprocessorTest(String name) {
        super(name);
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(BytecodePreprocessorTest.class.getName());
        suite.addTestSuite(BytecodePreprocessorTest.class);
        return new VmTestSetup(suite);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(BytecodePreprocessorTest.suite());
    }

    private synchronized float perform_virtualSync(int v) {
        if (v < 0) {
            return -1.0f;
        } else if (v > 0) {
            return 1.0f;
        } else {
            return 0.0f;
        }
    }

    private static synchronized String perform_staticSync(int v) {
        if (v < 0) {
            return "-1";
        } else if (v > 0) {
            return "1";
        } else {
            return "0";
        }
    }

    private void assertProcessedCodeIsDifferent(String methodName) {
        final ClassMethodActor classMethodActor = ClassMethodActor.fromJava(Classes.getDeclaredMethod(getClass(), methodName, int.class));
        final CodeAttribute originalCodeAttribute = classMethodActor.originalCodeAttribute();
        assertFalse(Arrays.equals(originalCodeAttribute.code(), classMethodActor.compilee().codeAttribute().code()));
    }

    public void test_virtualSync() {
        assertProcessedCodeIsDifferent("perform_virtualSync");
    }

    public void test_staticSync() {
        assertProcessedCodeIsDifferent("perform_staticSync");
    }
}
