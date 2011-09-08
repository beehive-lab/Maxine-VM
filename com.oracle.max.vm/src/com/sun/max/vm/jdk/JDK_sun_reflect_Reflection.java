/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.jdk;

import static com.sun.cri.bytecode.Bytecodes.Infopoints.*;
import static com.sun.max.vm.runtime.VMRegister.*;

import java.security.*;

import sun.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.jni.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.type.*;

/**
 * Method substitutions for {@link sun.reflect.Reflection}.
 *
 */
@METHOD_SUBSTITUTIONS(Reflection.class)
public
final class JDK_sun_reflect_Reflection {

    private JDK_sun_reflect_Reflection() {
    }

    /**
     * This class implements a closure that records the method actor at a particular
     * position in the stack.
     */
    static class Context extends SourceFrameVisitor {
        MethodActor method;
        long frameId;
        int realFramesToSkip;

        Context(int realFramesToSkip) {
            this.realFramesToSkip = realFramesToSkip;
        }
        @Override
        public boolean visitSourceFrame(ClassMethodActor method, int bci, boolean trapped, long frameId) {
            ClassMethodActor original = method.original();

            // according to sun.reflect.Reflection.getCallerClass(), "Frames associated with
            // java.lang.reflect.Method.invoke() and its implementation are
            // completely ignored and do not count toward the number of "real"
            // frames skipped."
            if (original.holder().isReflectionStub()  // ignore invocation stubs
                || (original.holder().toJava() == MethodActor.class && original.name().startsWith("invoke")) // ignore invocation methods in method actor
                || (original.holder().toJava() == JniFunctions.class && original.name().startsWith("Call"))  // ignore invocation methods of JNI implementation
                || original.equals(ClassRegistry.Method_invoke)  // ignore java.lang.reflect.Method.invoke
                ) {
                return true;
            }
            if (realFramesToSkip == 0) {
                this.method = original;
                this.frameId = frameId;
                return false;
            }
            realFramesToSkip--;
            return true;
        }
    }

    /**
     * Get the caller Context at a specified place in the stack.
     * This is used by {@link JDK_java_security_AccessController} for {@link AccessController#doPrivileged}.
     *
     * @param realFramesToSkip the number of frames to skip
     * @return the Context object corresponding to the specified place in the stack
     */
    static Context getCallerContext(int realFramesToSkip) {
        final Context context = new Context(realFramesToSkip);
        context.walk(null, Pointer.fromLong(here()), getCpuStackPointer(), getCpuFramePointer());
        assert context.method != null : "realFramesToSkip is too high: " + realFramesToSkip;
        return context;
    }

    /**
     * Gets the class of the caller at a specified number of stack frames depth.
     *
     * @see sun.reflect.Reflection#getCallerClass(int)
     * @param realFramesToSkip the number of frames to skip
     * @return the class of the caller at the specified stack depth
     */
    @SUBSTITUTE
    public static Class getCallerClass(int realFramesToSkip) {
        if (realFramesToSkip < 0) {
            return null;
        }
        final Context context = new Context(realFramesToSkip);
        context.walk(null, Pointer.fromLong(here()), getCpuStackPointer(), getCpuFramePointer());
        if (context.method == null) {
            return null;
        }
        return context.method.holder().toJava();
    }

    /**
     * Get the access flags of the specified class.
     *
     * @see sun.reflect.Reflection#getClassAccessFlags(Class)
     * @param javaClass the java class for which to get the access flags
     * @return an integer which represents the encoding of the flags
     */
    @SUBSTITUTE
    private static int getClassAccessFlags(Class javaClass) {
        return ClassActor.fromJava(javaClass).flags();
    }

}
