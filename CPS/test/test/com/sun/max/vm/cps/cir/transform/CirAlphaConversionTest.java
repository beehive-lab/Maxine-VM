/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package test.com.sun.max.vm.cps.cir.transform;

import junit.framework.*;

import com.sun.max.ide.*;
import com.sun.max.program.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.cps.cir.variable.*;
import com.sun.max.vm.type.*;

@org.junit.runner.RunWith(org.junit.runners.AllTests.class)
public class CirAlphaConversionTest extends MaxTestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CirAlphaConversionTest.suite());
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(CirAlphaConversionTest.class.getName());
        suite.addTestSuite(CirAlphaConversionTest.class);
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
