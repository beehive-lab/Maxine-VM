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
/*VCSID=5c082655-a919-41d4-80d4-126f7e942409*/
package test.com.sun.max.vm.jit;

import test.com.sun.max.vm.compiler.*;
import test.com.sun.max.vm.compiler.bytecode.*;

import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.template.source.*;
import com.sun.max.vm.type.*;


/**
 * Testing the JIT-compiler with methods performing table switch.
 *
 * @author Laurent Daynes
 */
public class JITTest_compileTableSwitch extends JitCompilerTestCase {
    int _i0;
    int _i1;
    int _i2;
    int _i3;
    int _i;

    public void perform_tableswitch(int i) {
        switch(i) {
            case 0:
                _i0 += i;
                break;
            case 1:
                _i1 += i;
                break;
            case 2:
                _i2 += i;
                break;
            case 3:
                _i3 += i;
                break;
            default:
                _i += i;
                break;
        }
    }

    public void test_tableswitch() {
        final TargetMethod method = compileMethod("perform_tableswitch", SignatureDescriptor.create(void.class, int.class));
        new BytecodeConfirmation(method.classMethodActor()) {
            @Override
            public void tableswitch(int defaultOffset, int lowMatch, int highMatch, int numberOfCases)  {
                getBytecodeScanner().skipBytes(numberOfCases * 4);
                confirmPresence();
            }
        };
    }

    @Override
    protected Class[] templateSources() {
        return new Class[]{InstrumentedBytecodeSource.class, UnoptimizedBytecodeTemplateSource.class, ResolvedFieldAccessTemplateSource.class};
    }


}
