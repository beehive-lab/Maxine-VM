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

import java.util.*;

import test.com.sun.max.vm.compiler.*;

import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;

/**
 * Compiles all methods that makes the alternate jit-ed VM start.
 *
 * @author Laurent Daynes
 */
public class JITTest_compileBootImage extends JitCompilerTestCase {

    public void test_compileAll() {
        final LinkedList<TargetMethod> toDo = new LinkedList<TargetMethod>();
        MaxineVM.usingTarget(new Runnable() {
            public void run() {
                final RuntimeCompilerScheme jit = MaxineVM.target().configuration.jitScheme();

                final ClassActor classActor = ClassActor.fromJava(com.sun.max.vm.run.jitTest.JitTest.class);

                for (VirtualMethodActor virtualMethodActor : classActor.localVirtualMethodActors()) {
                    toDo.add(jit.compile(virtualMethodActor));
                }
                for (StaticMethodActor staticMethodActor : classActor.localStaticMethodActors()) {
                    toDo.add(jit.compile(staticMethodActor));
                }
            }
        });
        for (TargetMethod targetMethod : toDo) {
            Trace.line(1,  targetMethod.description());
            traceBundleAndDisassemble((CPSTargetMethod) targetMethod);
        }
    }

}
