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
package test.com.sun.max.vm.jit;

import test.com.sun.max.vm.cps.*;
import test.com.sun.max.vm.cps.bytecode.*;

import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.template.source.*;
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

    @Override
    protected Class[] templateSources() {
        return new Class[]{InstrumentedBytecodeSource.class, UnoptimizedBytecodeTemplateSource.class, ResolvedFieldAccessTemplateSource.class};
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
