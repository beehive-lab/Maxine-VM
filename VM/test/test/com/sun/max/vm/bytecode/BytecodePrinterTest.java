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

import junit.framework.*;
import test.com.sun.max.vm.compiler.cir.generate.*;

import com.sun.max.ide.*;
import com.sun.max.lang.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.prototype.*;
import com.sun.max.vm.type.*;

@org.junit.runner.RunWith(org.junit.runners.AllTests.class)
public class BytecodePrinterTest extends MaxTestCase {

    public BytecodePrinterTest(String name) {
        super(name);
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(BytecodePrinterTest.class.getName());
        suite.addTestSuite(BytecodePrinterTest.class);
        return new CirTranslatorTestSetup(suite);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(BytecodePrinterTest.suite());
    }

    private static class A {
        public void f() {
            System.out.println("A");
        }
    }

    private static final class B extends A {
        @Override
        public void f() {
            System.out.println("B");
        }
    }

    private A _a = new A();
    private B _b = new B();

    private void perform() {
        _a.f();
        _b.f();
    }

    public void test() {
        final Class testClass = Classes.load(PrototypeClassLoader.PROTOTYPE_CLASS_LOADER, BytecodePrinterTest.class.getName());
        final ClassMethodActor methodActor = ClassActor.fromJava(testClass).findClassMethodActor(SymbolTable.makeSymbol("perform"), SignatureDescriptor.create(void.class));
        CodeAttributePrinter.print(System.out, methodActor.codeAttribute());
    }

}
