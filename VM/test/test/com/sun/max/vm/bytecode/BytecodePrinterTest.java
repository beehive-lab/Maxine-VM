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
package test.com.sun.max.vm.bytecode;

import junit.framework.*;
import test.com.sun.max.vm.*;

import com.sun.max.lang.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

@org.junit.runner.RunWith(org.junit.runners.AllTests.class)
public class BytecodePrinterTest extends VmTestCase {

    public BytecodePrinterTest(String name) {
        super(name);
    }

    public static Test suite() {
        final TestSuite suite = new TestSuite(BytecodePrinterTest.class.getName());
        suite.addTestSuite(BytecodePrinterTest.class);
        return new VmTestSetup(suite);
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

    private A a = new A();
    private B b = new B();

    private void perform() {
        a.f();
        b.f();
    }

    public void test() {
        final Class testClass = Classes.load(HostedBootClassLoader.HOSTED_BOOT_CLASS_LOADER, BytecodePrinterTest.class.getName());
        final ClassMethodActor methodActor = ClassActor.fromJava(testClass).findClassMethodActor(SymbolTable.makeSymbol("perform"), SignatureDescriptor.create(void.class));
        CodeAttributePrinter.print(System.out, methodActor.codeAttribute());
    }

}
