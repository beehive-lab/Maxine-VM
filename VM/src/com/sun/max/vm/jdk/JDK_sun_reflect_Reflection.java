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
package com.sun.max.vm.jdk;

import static com.sun.max.vm.runtime.VMRegister.*;

import java.security.*;

import sun.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
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
            // according to sun.reflect.Reflection, getCallerClass() should ignore java.lang.reflect.Method.invoke
            if (method.equals(ClassRegistry.Method_invoke)) {
                return true;
            }
            if (realFramesToSkip == 0) {
                this.method = method.original();
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
