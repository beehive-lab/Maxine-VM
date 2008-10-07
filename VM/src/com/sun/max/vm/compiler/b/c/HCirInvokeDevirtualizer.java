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
/*VCSID=e7af3829-7d1e-429b-8254-117e61a4ad36*/
package com.sun.max.vm.compiler.b.c;

import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.cir.*;
import com.sun.max.vm.compiler.cir.operator.*;
import com.sun.max.vm.compiler.cir.transform.*;
import com.sun.max.vm.compiler.instrument.*;


public class HCirInvokeDevirtualizer extends CirVisitor {
    @Override
    public void visitCall(CirCall call) {
        final CirValue proc = call.procedure();
        if (proc.getClass() == InvokeVirtual.class) {
            final MethodInstrumentation instrumentation = VMConfiguration.target().compilationScheme().getMethodInstrumentation(call.bytecodeLocation().classMethodActor());
            final Hub mostFrequentHub = (instrumentation == null)
                                    ? null
                                    : instrumentation.getMostFrequentlyUsedHub(call.bytecodeLocation().position());

            if (mostFrequentHub != null) {
                /*
                 * invokevirtual index      objref, args...
                 *
                 * =>
                 *
                 * class = objref.getClass();
                 * if (class == mirror index)
                 *     invokespecial index    objref, args...
                 * else
                 *     invokevirtual index     objref, args...
                 */
            }
        }
    }
}
