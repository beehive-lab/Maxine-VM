/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.vm.intrinsics.Infopoints.*;
import static com.sun.max.vm.runtime.VMRegister.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.stack.*;

/**
 * Custom stack walking to find the appropriate class loader for a {@link JniFunctions#FindClass} upcall
 * from a JVMTI agent.
 */
public class JVMTIClassLoader {
    /**
     * Find the first non-VM frame on the stack.
     * There can be arbitrary platform frames interleaved so we have to keep going until
     * we reach the base and then back up from there.
     */
    static class NonVMContext extends SourceFrameVisitor {
        private static final String COM_SUN_MAX = "com.sun.max";
        private static final String COM_ORACLE_MAX = "com.oracle.max";

        MethodActor nonVMMethod;
        boolean lastFrameInVM;

        @Override
        public boolean visitSourceFrame(ClassMethodActor method, int bci, boolean trapped, long frameId) {
            ClassMethodActor original = method.original();

            // a VM frame is one whose package prefix is either "com.sun.max" or "com.oracle.max"
            final String name = original.holder().name();
            if (original.holder().isReflectionStub() ||
                (name.startsWith(COM_SUN_MAX) || name.startsWith(COM_ORACLE_MAX))) {
                lastFrameInVM = true;
                nonVMMethod = null;
            } else {
                // Not a VM frame
                if (lastFrameInVM) {
                    nonVMMethod = original;
                }
            }
            return true;
        }
    }

    public static Class getCallerClass() {
        Class result = null;
        final NonVMContext context = new NonVMContext();
        context.walk(null, Pointer.fromLong(here()), getCpuStackPointer(), getCpuFramePointer());
        if (context.nonVMMethod != null) {
            result = context.nonVMMethod.holder().toJava();
        }
        return result;
    }
}
