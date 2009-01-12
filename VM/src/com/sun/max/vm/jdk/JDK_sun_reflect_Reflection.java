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

import java.lang.reflect.*;
import java.util.*;

import sun.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * Method substitutions for {@link sun.reflect.Reflection}.
 *
 */
@METHOD_SUBSTITUTIONS(Reflection.class)
final class JDK_sun_reflect_Reflection {

    private JDK_sun_reflect_Reflection() {
    }

    private static final CriticalMethod _javaLangReflectMethodInvoke = new CriticalMethod(Method.class, "invoke");

    /**
     * This class implements a closure that records the method actor at a particular
     * position in the stack.
     */
    private static class Context implements StackFrameVisitor {
        MethodActor _result;
        int _realFramesToSkip;

        Context(int realFramesToSkip) {
            _realFramesToSkip = realFramesToSkip;
        }

        public boolean visitFrame(StackFrame stackFrame) {
            if (stackFrame.isTopFrame()) {
                // skip 'getCallerMethod()'
                return true;
            }
            if (stackFrame.isAdapter()) {
                return true;
            }
            //final TargetMethod targetMethod = Code.codePointerToTargetMethod(stackFrame.instructionPointer());
            final TargetMethod targetMethod = stackFrame.targetMethod();
            if (targetMethod == null) {
                // native frame
                _realFramesToSkip--; // TODO: find out whether this is according to getCallerClass' intended "spec"
                return true;
            }
            // according to sun.reflect.Reflection, getCallerClass() should ignore java.lang.reflect.Method.invoke
            if (isReflectionMethod(targetMethod)) {
                return true;
            }

            final Iterator<BytecodeLocation> bytecodeLocations = targetMethod.getBytecodeLocationsFor(stackFrame.instructionPointer());
            if (bytecodeLocations == null) {
                if (_realFramesToSkip == 0) {
                    _result = targetMethod.classMethodActor();
                    return false;
                }
                _realFramesToSkip--;
            } else {
                while (bytecodeLocations.hasNext()) {
                    final BytecodeLocation bytecodeLocation = bytecodeLocations.next();
                    final MethodActor classMethodActor = bytecodeLocation.classMethodActor().original();
                    if (!classMethodActor.isWrapper() && !classMethodActor.holder().isGenerated()) {
                        if (_realFramesToSkip == 0) {
                            _result = classMethodActor;
                            return false;
                        }
                        _realFramesToSkip--;
                    }
                }
            }
            return true;
        }
    }

    private static boolean isReflectionMethod(TargetMethod targetMethod) {
        return targetMethod.classMethodActor() == _javaLangReflectMethodInvoke.classMethodActor();
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
        new VmStackFrameWalker(VmThread.current().vmThreadLocals()).inspect(VMRegister.getInstructionPointer(),
                                                       VMRegister.getCpuStackPointer(),
                                                       VMRegister.getCpuFramePointer(),
                                                       context);
        return context._result;
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
