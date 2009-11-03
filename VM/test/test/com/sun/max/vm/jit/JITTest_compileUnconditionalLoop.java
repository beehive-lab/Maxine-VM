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

import test.com.sun.max.vm.compiler.*;
import test.com.sun.max.vm.compiler.bytecode.*;

import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.template.source.*;
import com.sun.max.vm.type.*;


/**
 * Testing the JIT-compiler with methods performing loop. This tests the JIT support for the goto bytecode.
 *
 * @author Laurent Daynes
 */
public class JITTest_compileUnconditionalLoop extends JitCompilerTestCase {

    void perform_loop() {
        int a = 0;
        while (true) {
            a = a << 2;
        }
    }

    public void test_perform_loop() {
        final TargetMethod targetMethod = compileMethod("perform_loop", SignatureDescriptor.VOID);
        new BytecodeConfirmation(targetMethod.classMethodActor()) {
            @Override
            public void goto_(int offset) {
                confirmPresence();
                // We're testing with backward branch only
                assert offset <= 0;
            }
        };
    }

    @Override
    protected Class[] templateSources() {
        return new Class[]{InstrumentedBytecodeSource.class, UnoptimizedBytecodeTemplateSource.class};
    }
}
