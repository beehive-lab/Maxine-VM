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
package test.com.sun.max.tele.interpreter;

import junit.framework.*;
import test.com.sun.max.vm.compiler.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 */
public class TeleInterpreterTest_word extends CompilerTestCase<CirMethod> {

    public static Test suite() {
        final TestSuite suite = new TestSuite(TeleInterpreterTest_word.class.getSimpleName());
        suite.addTestSuite(TeleInterpreterTest_word.class);
        return new TeleInterpreterTestSetup(suite);
    }

    public TeleInterpreterTest_word(String name) {
        super(name);
    }

    public static void main(String[] programArguments) {
        junit.textui.TestRunner.run(TeleInterpreterTest_word.suite());
    }

    Pointer explicitCheckCast(Address address) {
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
                if (cirValue instanceof CirConstant && cirValue.kind() == Kind.REFERENCE) {
                    final ReferenceValue referenceValue = (ReferenceValue) cirValue.value();
                    return referenceValue.asObject() == ClassActor.fromJava(ClassCastException.class);
                }
                return false;
            }
        }));
    }


    long gratuitousCheckCast(Pointer[] pointers) {
        return WordArray.get(pointers, 0).toLong();
    }

    /**
     * Checks that the optimizer removes the checkcast inserted by javac before "toLong()".
     */
    public void test_gratuitousCheckCast() {
        final CirMethod cirMethod = compileMethod("gratuitousCheckCast");
        assertNull(CirSearch.byPredicate(cirMethod.closure(), new CirPredicate() {
            @Override
            public boolean evaluateValue(CirValue cirValue) {
                if (cirValue instanceof CirConstant && cirValue.kind() == Kind.REFERENCE) {
                    final ReferenceValue referenceValue = (ReferenceValue) cirValue.value();
                    return referenceValue.asObject() == ClassActor.fromJava(ClassCastException.class);
                }
                return false;
            }
        }));
    }

}
