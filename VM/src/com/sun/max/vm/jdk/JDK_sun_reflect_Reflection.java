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

import java.security.*;

import sun.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.*;
import com.sun.max.vm.thread.*;
import com.sun.max.vm.type.*;

/**
 * Method substitutions for {@link sun.reflect.Reflection}.
 *
 */
@METHOD_SUBSTITUTIONS(Reflection.class)
final class JDK_sun_reflect_Reflection {

    private JDK_sun_reflect_Reflection() {
    }

    /**
     * This class implements a closure that records the method actor at a particular
     * position in the stack.
     */
    static class Context extends RawStackFrameVisitor {
        MethodActor methodActorResult;
        Pointer framePointerResult;
        int realFramesToSkip;

        Context(int realFramesToSkip) {
            this.realFramesToSkip = realFramesToSkip;
        }

        @Override
        public boolean visitFrame(Cursor current, Cursor callee) {
            if (current.isTopFrame()) {
                // skip 'getCallerMethod()'
                return true;
            }
            TargetMethod targetMethod = current.targetMethod();
            if (targetMethod instanceof Adapter) {
                // adapter frame
                return true;
            }
            if (targetMethod == null) {
                // native frame
                realFramesToSkip--; // TODO: find out whether this is according to getCallerClass' intended "spec"
                return true;
            }
            // according to sun.reflect.Reflection, getCallerClass() should ignore java.lang.reflect.Method.invoke
            if (targetMethod.classMethodActor().equals(ClassRegistry.Method_invoke)) {
                return true;
            }

            BytecodeLocation bytecodeLocation = targetMethod.getBytecodeLocationFor(current.ip(), false);
            if (bytecodeLocation == null) {
                if (realFramesToSkip == 0) {
                    methodActorResult = targetMethod.classMethodActor();
                    framePointerResult = current.fp();
                    return false;
                }
                realFramesToSkip--;
            } else {
                while (bytecodeLocation != null) {
                    final MethodActor classMethodActor = bytecodeLocation.classMethodActor.original();
                    if (!classMethodActor.holder().isGenerated()) {
                        if (realFramesToSkip == 0) {
                            methodActorResult = classMethodActor;
                            framePointerResult = current.fp();
                            return false;
                        }
                        realFramesToSkip--;
                    }
                    bytecodeLocation = bytecodeLocation.parent();
                }
            }
            return true;
        }
    }

    /**
     * Get the method actor at a specified place in the stack.
     *
     * @param realFramesToSkip the number of frames to skip
     * @return a reference to the method actor corresponding to the method on the stack at the specified position
     */
    @NEVER_INLINE
    private static MethodActor getCallerMethod(int realFramesToSkip) {
        final Context context = new Context(realFramesToSkip);
        new VmStackFrameWalker(VmThread.current().tla()).inspect(VMRegister.getInstructionPointer(),
                                                       VMRegister.getCpuStackPointer(),
                                                       VMRegister.getCpuFramePointer(),
                                                       context);
        return context.methodActorResult;
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
        new VmStackFrameWalker(VmThread.current().tla()).inspect(VMRegister.getInstructionPointer(),
                                                       VMRegister.getCpuStackPointer(),
                                                       VMRegister.getCpuFramePointer(),
                                                       context);
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
    private static Class getCallerClass(int realFramesToSkip) {
        if (realFramesToSkip < 0) {
            return null;
        }
        return getCallerMethod(realFramesToSkip).holder().toJava();
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
