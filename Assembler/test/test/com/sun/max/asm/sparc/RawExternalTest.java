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
package test.com.sun.max.asm.sparc;

import java.io.*;
import java.util.*;

import junit.framework.*;
import test.com.sun.max.asm.*;

import com.sun.max.asm.gen.*;
import com.sun.max.asm.gen.risc.sparc.*;

/**
 * JUnit harness for testing the generated PowerPC assembler against an external assembler.
 * 
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class RawExternalTest extends ExternalAssemblerTestCase {

    public RawExternalTest() {
        super();
    }

    public RawExternalTest(String name) {
        super(name);
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(RawExternalTest.class.getName());
        suite.addTestSuite(RawExternalTest.class);
        return suite;
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(RawExternalTest.class);
    }

    public void test_assemble() throws FileNotFoundException, IOException {
        run(new SPARC32AssemblyTester(EnumSet.of(AssemblyTestComponent.EXTERNAL_ASSEMBLER)));
    }
}
