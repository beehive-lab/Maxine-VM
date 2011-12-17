/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.jvmti;

import static com.sun.max.vm.MaxineVM.*;
import static com.sun.max.vm.jvmti.JVMTIConstants.*;

import java.util.*;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.deopt.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.jvmti.JVMTIThreadFunctions.*;
import com.sun.max.vm.thread.*;

/**
 * Everything concerned with modifications to compiled code.
 */
public class JVMTICode {

    /**
     * Handle any change in compiled code for the methods on the (logical) call stack
     * necessary for the pervasive events such as SINGLE_STEP, FRAME_POP, for a single thread.
     * N.B. This method may be called multiple times for different events on the same (suspended) thread.
     */
    static void deOptForEvent(int eventType, int mode, Thread thread) {
        SingleThreadStackTraceVmOperation op = new FindAppFramesStackTraceOperation(VmThread.fromJava(thread)).submitOp();
        if (mode == JVMTI_ENABLE) {
            // its not clear whether we should deopt every frame at this point.
            // currently there is a bug walking stacks containing a deopt stub so
            // we only deopt the top frame
            ArrayList<TargetMethod> targetMethods = new ArrayList<TargetMethod>();
            for (int i = 0; i < op.stackTraceVisitor.stackElements.size(); i++) {
                ClassMethodActor methodActor = op.stackTraceVisitor.getStackElement(i).classMethodActor;
                TargetMethod targetMethod = methodActor.currentTargetMethod();
                targetMethod.finalizeReferenceMaps();
                targetMethods.add(targetMethod);
                // just top frame for now
                break;
            }
            // Calling this multiple times before the thread resumes is harmless as it takes care to
            // filter out already invalidated methods. However, it would be better to be able to detect
            // the multiple calls and just do all this once.
            new Deoptimization(targetMethods).go();
        } else {
            // is it worth re-opting.
        }
    }

    /**
     * A new breakpoint is being set in code that is already compiled.
     * @param classMethodActor
     */
    static void deOptForBreakpoint(ClassMethodActor classMethodActor) {
        // Potentially need to deopt the code in all threads, but note that it
        // may not be active on any thread stack, in which case we just need to recompile
        // and patch call sites. Deopt takes care of it either way.
        ArrayList<TargetMethod> targetMethods = new ArrayList<TargetMethod>();
        targetMethods.add(classMethodActor.currentTargetMethod());
        new Deoptimization(targetMethods).go();
    }
}
