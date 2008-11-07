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

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.collect.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * Substitutions for {@link java.lang.Throwable java.lang.Throwable} that collect the stack trace.
 *
 * @author Ben L. Titzer
 * @author Laurent Daynes
 */
@METHOD_SUBSTITUTIONS(Throwable.class)
final class JDK_java_lang_Throwable {

    /**
     * This field provides access to the private field "stackTrace" in the {@link java.lang.Throwable java.lang.Throwable}
     * class.
     */
    private static final ReferenceFieldActor stackTrace = (ReferenceFieldActor) ClassActor.fromJava(Throwable.class).findFieldActor(SymbolTable.makeSymbol("stackTrace"));

    private JDK_java_lang_Throwable() {
    }

    /**
     * Casts this object to its corresponding {@code java.lang.Throwable} instance.
     * @return this object cast to the {@code java.lang.Throwable} type
     */
    @INLINE
    private Throwable thisThrowable() {
        return UnsafeLoophole.cast(this);
    }

    /**
     * Fills in the stack trace for this exception. This implementation eagerly creates a
     * stack trace and fills in all the {@link java.lang.StackTraceElement stack trace elements}.
     * @see java.lang.Throwable#fillInStackTrace()
     * @return the throwable with a filled-in stack trace (typically this object)
     */
    @SUBSTITUTE
    public synchronized Throwable fillInStackTrace() {
        // TODO: one possible optimization is to only record the sequence of frames for an exception
        // and build the exception stack trace elements later. There is a field in the Throwable object
        // called "backtrace" to allow for some natively cached stuff for this purpose.
        final Throwable thisThrowable = thisThrowable();
        if (thisThrowable instanceof OutOfMemoryError) {
            // Don't record stack traces in situations where memory may be exhausted
            return thisThrowable;
        }
        final ClassActor throwableActor = ClassActor.fromJava(thisThrowable.getClass());
        // use the stack walker to collect the frames
        final StackFrameWalker stackFrameWalker = new VmStackFrameWalker(VmThread.current().vmThreadLocals());
        final Pointer instructionPointer = VMRegister.getInstructionPointer();
        final Pointer cpuStackPointer = VMRegister.getCpuStackPointer();
        final Pointer  cpuFramePointer = VMRegister.getCpuFramePointer();

        boolean atImplicitExceptionThrow = false;
        boolean inFiller = true;

        final Sequence<StackFrame> stackFrames = stackFrameWalker.frames(null, instructionPointer, cpuStackPointer, cpuFramePointer);
        final List<StackTraceElement> result = new ArrayList<StackTraceElement>();
        for (StackFrame stackFrame : stackFrames) {
            if (stackFrame.isAdapter()) {
                continue;
            }
            final TargetMethod targetMethod = stackFrame.targetMethod();
            if (targetMethod == null) {
                // native frame
                continue;
            } else if (targetMethod.classMethodActor().isTrapStub()) {
                // Reset the stack trace. We want the trace to start from the actual exception throw.
                result.clear();
                atImplicitExceptionThrow = true;
                continue;
            } else if (atImplicitExceptionThrow) {
                // if it is an implicit exception, do not look for a java frame descriptor
                addDefaultStackTraceElement(result, targetMethod.classMethodActor(), -1);
                atImplicitExceptionThrow = false;
                inFiller = false;
                continue;
            } else if (inFiller) {
                final ClassMethodActor methodActor = targetMethod.classMethodActor();
                if (methodActor.holder() == throwableActor && methodActor.isInstanceInitializer()) {
                    // This will initiate filling the stack trace.
                    inFiller = false;
                }
                continue;
            }
            addStackTraceElements(result, targetMethod, stackFrame);
        }
        stackTrace.writeObject(thisThrowable, result.toArray(new StackTraceElement[result.size()]));
        return thisThrowable;
    }

    private static void addStackTraceElements(List<StackTraceElement> result, TargetMethod targetMethod, StackFrame stackFrame) {
        TargetJavaFrameDescriptor javaFrameDescriptor = targetMethod.getPrecedingJavaFrameDescriptor(stackFrame.instructionPointer());
        if (javaFrameDescriptor == null) {
            addDefaultStackTraceElement(result, targetMethod.classMethodActor(), -1);
        } else {
            do {
                final BytecodeLocation bytecodeLocation = javaFrameDescriptor.bytecodeLocation();
                final ClassMethodActor classMethodActor = bytecodeLocation.classMethodActor();
                if (classMethodActor.isApplicationVisible()) {
                    addDefaultStackTraceElement(result, classMethodActor, bytecodeLocation.sourceLineNumber());
                }
                javaFrameDescriptor = javaFrameDescriptor.parent();
            } while (javaFrameDescriptor != null);
        }
    }

    /**
     * Adds a default stack trace element to the specified result if there is no source information
     * for the specified class method actor.
     *
     * @param result a list of stack trace elements that will represent the final result
     * @param classMethodActor the class method actor on the stack
     * @param sourceLineNumber the source line number
     */
    private static void addDefaultStackTraceElement(final List<StackTraceElement> result, final ClassMethodActor classMethodActor, int sourceLineNumber) {
        final ClassActor holder = classMethodActor.holder();
        result.add(new StackTraceElement(holder.name().toString(), classMethodActor.name().toString(), holder.sourceFileName(), sourceLineNumber));
    }

    /**
     * Gets the depth of the stack trace, in the number of stack trace elements.
     *
     * @see java.lang.Throwable#getStackTraceDepth()
     * @return the number of stack trace elements
     */
    @SUBSTITUTE
    private int getStackTraceDepth() {
        final StackTraceElement[] elements = getStackTraceElements();
        if (elements != null) {
            return elements.length;
        }
        return 0;
    }

    /**
     * Gets a single stack trace element at the specified index.
     * @see java.lang.Throwable#getStackTraceElement(int)
     * @param index the index into the stack trace at which to get the element
     * @return the element at the specified index
     */
    @SUBSTITUTE
    private StackTraceElement getStackTraceElement(int index) {
        final StackTraceElement[] elements = getStackTraceElements();
        if (elements != null) {
            return elements[index];
        }
        return null;
    }

    /**
     * Gets the complete stack trace for this throwable.
     * @see java.lang.Throwable#getStackTraceElements()
     * @return an array of stack trace elements representing the complete stack trace
     */
    @INLINE
    private StackTraceElement[] getStackTraceElements() {
        return (StackTraceElement[]) stackTrace.readObject(thisThrowable());
    }

}
