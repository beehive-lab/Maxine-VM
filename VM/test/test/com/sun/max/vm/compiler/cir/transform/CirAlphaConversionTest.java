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
/*VCSID=58860d93-b5d4-4876-874c-2ae5dc29cb40*/
package test.com.sun.max.vm.compiler.cir.transform;

import junit.framework.*;

import com.sun.max.ide.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.cir.variable.*;
import com.sun.max.vm.type.*;

@org.junit.runner.RunWith(org.junit.runners.AllTests.class)
public class CirAlphaConversionTest extends MaxTestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CirAlphaConversionTest.suite());
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(CirAlphaConversionTest.class.getName());
        //$JUnit-BEGIN$
        suite.addTestSuite(CirAlphaConversionTest.class);
        //$JUnit-END$
        Trace.on(1);
        return suite;
    }

    public CirAlphaConversionTest(String name) {
        super(name);
        Trace.on(1);
    }

    private CirClosure createExample1() {
        final CirVariableFactory variableFactory = new CirVariableFactory();
        final CirVariable a = variableFactory.createTemporary(Kind.INT);
        final CirVariable b = variableFactory.createTemporary(Kind.BYTE);
        final CirVariable c = variableFactory.createTemporary(Kind.CHAR);
        final CirVariable d = variableFactory.createTemporary(Kind.DOUBLE);
        final CirVariable e = variableFactory.createTemporary(Kind.FLOAT);
        final CirVariable f = variableFactory.createTemporary(Kind.REFERENCE);

        return new CirClosure(
            new CirCall(
                new CirClosure(
                    new CirCall(
                        f,
                        d,
                        CirConstant.fromInt(3),
                        b,
                        e
                    ),
                    a,
                    b,
                    c
                ),
                e,
                CirConstant.fromInt(2),
                d
            ),
            d,
            e,
            f
        );
    }

    public void test_apply1() {
        final CirClosure closure = createExample1();
        Trace.line(1, "BEFORE CirAlphaConversion: test_apply1");
        closure.trace(1);
        CirAlphaConversion.apply(closure);
        Trace.line(1, "AFTER CirAlphaConversion: test_apply1");
        closure.trace(1);
    }

    private CirCall createExample2() {
        final CirVariableFactory variableFactory = new CirVariableFactory();
        final CirVariable a = variableFactory.createTemporary(Kind.INT);
        final CirVariable b = variableFactory.createTemporary(Kind.BYTE);
        final CirVariable c = variableFactory.createTemporary(Kind.CHAR);
        final CirVariable d = variableFactory.createTemporary(Kind.DOUBLE);
        final CirVariable e = variableFactory.createTemporary(Kind.FLOAT);
        final CirVariable f = variableFactory.createTemporary(Kind.REFERENCE);

        return new CirCall(
            new CirClosure(
                new CirCall(
                    f,
                    a,
                    b,
                    CirConstant.fromInt(3),
                    d
                ),
                a,
                b,
                c,
                d,
                e,
                f
            ),
            CirConstant.fromInt(4),
            CirConstant.fromInt(5),
            CirConstant.fromInt(6),
            CirConstant.fromDouble(7.0),
            CirConstant.fromFloat(8),
            CirConstant.fromObject(null)
        );
    }

    public void test2_apply2() {
        final CirCall call = createExample2();
        Trace.line(1, "BEFORE CirAlphaConversion: test_apply2");
        call.trace(1);
        CirAlphaConversion.apply(call);
        Trace.line(1, "AFTER CirAlphaConversion: test_apply2");
        call.trace(1);
    }
}
