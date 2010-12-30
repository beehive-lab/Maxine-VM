/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.cps.cir.optimize;

import junit.framework.*;
import test.com.sun.max.vm.cps.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.cps.cir.*;
import com.sun.max.vm.cps.cir.transform.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public class CirOptimizerTest_word extends CompilerTestCase<CirMethod> {

    public static Test suite() {
        final TestSuite suite = new TestSuite(CirOptimizerTest_word.class.getSimpleName());
        suite.addTestSuite(CirOptimizerTest_word.class);
        return new CirOptimizerTestSetup(suite);
    }

    public CirOptimizerTest_word(String name) {
        super(name);
    }

    public static void main(String[] programArguments) {
        junit.textui.TestRunner.run(CirOptimizerTest_word.suite());
    }

    private Pointer explicitCheckCast(Address address) {
        return (Pointer) address;
    }

    /**
     * Checks that the optimizer removes explicit type casts to a subtype of Word.
     */
    public void test_explicitCheckCast() {
        final CirMethod cirMethod = compileMethod("explicitCheckCast");
        assertNull(CirSearch.byPredicate(cirMethod.closure(), new CirPredicate() {
            @Override
            public boolean evaluateValue(CirValue cirValue) {
                if (cirValue instanceof CirConstant && cirValue.kind().isReference) {
                    final ReferenceValue referenceValue = (ReferenceValue) cirValue.value();
                    return referenceValue.asObject() == ClassActor.fromJava(ClassCastException.class);
                }
                return false;
            }
        }));
    }

    private long gratuitousCheckCast(Pointer[] pointers) {
        return WordArray.get(pointers, 0).asAddress().toLong();
    }

    /**
     * Checks that the optimizer removes the checkcast inserted by javac before "toLong()".
     */
    public void test_gratuitousCheckCast() {
        final CirMethod cirMethod = compileMethod("gratuitousCheckCast");
        assertNull(CirSearch.byPredicate(cirMethod.closure(), new CirPredicate() {
            @Override
            public boolean evaluateValue(CirValue cirValue) {
                if (cirValue instanceof CirConstant && cirValue.kind().isReference) {
                    final ReferenceValue referenceValue = (ReferenceValue) cirValue.value();
                    return referenceValue.asObject() == ClassActor.fromJava(ClassCastException.class);
                }
                return false;
            }
        }));
    }

}
