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
package test.com.sun.max.vm.jit;

import test.com.sun.max.vm.cps.*;
import test.com.sun.max.vm.cps.bytecode.*;

import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.type.*;

/**
 * Testing the JIT-compiler with methods performing table switch.
 *
 * @author Laurent Daynes
 */
public class JITTest_compileSwitches extends JitCompilerTestCase {
    int i0;
    int i1;
    int i2;
    int i3;
    int iDefault;

    public static int perform_lookupswitch(int a) {
        int b = a;
        switch (b) {
            case 'X':
                b = 10;
                break;
            case -1:
                b = 20;
                break;
            default:
                b = 30;
        }
        return b;
    }

    public void test_lookupswitch() {
        final TargetMethod method = compileMethod("perform_lookupswitch", SignatureDescriptor.create(int.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            protected void lookupswitch(int defaultOffset, int numberOfCases) {
                bytecodeScanner().skipBytes(numberOfCases * 8);
                confirmPresence();
            }
        };
    }

    public void perform_tableswitch(int i) {
        switch(i) {
            case 0:
                i0 += i;
                break;
            case 1:
                i1 += i;
                break;
            case 2:
                i2 += i;
                break;
            case 3:
                i3 += i;
                break;
            default:
                iDefault += i;
                break;
        }
    }

    public void test_tableswitch() {
        final TargetMethod method = compileMethod("perform_tableswitch", SignatureDescriptor.create(void.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases)  {
                bytecodeScanner().skipBytes(numberOfCases * 4);
                confirmPresence();
            }
        };
    }
}
