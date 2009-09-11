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
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * Substitutions for {@link java.lang.Throwable} that collect the stack trace.
 *
 * @author Ben L. Titzer
 * @author Laurent Daynes
 */
@METHOD_SUBSTITUTIONS(Throwable.class)
public final class JDK_java_lang_Throwable {

    /**
     * This field provides access to the private field "stackTrace" in the {@link java.lang.Throwable java.lang.Throwable}
     * class.
     */
    private static final FieldActor stackTrace = ClassActor.fromJava(Throwable.class).findFieldActor(SymbolTable.makeSymbol("stackTrace"));

    private JDK_java_lang_Throwable() {
    }

    /**
     * Casts this object to its corresponding {@code java.lang.Throwable} instance.
     * @return this object cast to the {@code java.lang.Throwable} type
     */
    @UNSAFE_CAST
    private native Throwable thisThrowable();

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
        final Pointer cpuFramePointer = VMRegister.getCpuFramePointer();

        boolean atImplicitExceptionThrow = false;
        boolean inFiller = true;

        final Sequence<StackFrame> stackFrames = stackFrameWalker.frames(null, instructionPointer, cpuStackPointer, cpuFramePointer);
        final List<StackTraceElement> result = new ArrayList<StackTraceElement>();
        for (StackFrame stackFrame : stackFrames) {
            if (stackFrame instanceof AdapterStackFrame) {
                continue;
            }
            final TargetMethod targetMethod = stackFrame.targetMethod();
            if (targetMethod == null || targetMethod.classMethodActor() == null) {
                // native frame or stub frame without a class method actor
                continue;
            } else if (targetMethod.classMethodActor().isTrapStub()) {
                // Reset the stack trace. We want the trace to start from the actual exception throw.
                result.clear();
                atImplicitExceptionThrow = true;
                continue;
            } else if (atImplicitExceptionThrow) {
                // if it is an implicit exception, do not look for a java frame descriptor
                addStackTraceElement(result, targetMethod.classMethodActor(), -1, stackFrame.instructionPointer.minus(targetMethod.codeStart()).toInt());
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
        final Object value = result.toArray(new StackTraceElement[result.size()]);
        TupleAccess.writeObject(thisThrowable, stackTrace.offset(), value);
        return thisThrowable;
    }

    private static void addStackTraceElements(List<StackTraceElement> result, TargetMethod targetMethod, StackFrame stackFrame) {
        final Iterator<? extends BytecodeLocation> bytecodeLocations = targetMethod.getBytecodeLocationsFor(stackFrame.instructionPointer);
        if (bytecodeLocations == null) {
            addStackTraceElement(result, targetMethod.classMethodActor(), -1, stackFrame.instructionPointer.minus(targetMethod.codeStart()).toInt());
        } else {
            while (bytecodeLocations.hasNext()) {
                final BytecodeLocation bytecodeLocation = bytecodeLocations.next();
                final ClassMethodActor classMethodActor = bytecodeLocation.classMethodActor();
                if (classMethodActor.isApplicationVisible()) {
                    addStackTraceElement(result, classMethodActor, bytecodeLocation.sourceLineNumber(), -1);
                }
            }
        }
    }

    /**
     * Adds a stack trace element to the specified result.
     *
     * @param result a list of stack trace elements that will represent the final result
     * @param classMethodActor the class method actor on the stack
     * @param sourceLineNumber the source line number
     * @param targetMethodOffset the instruction pointer offset within the target method. This value is ignored if {@code sourceLineNumber >= 0}
     */
    private static void addStackTraceElement(final List<StackTraceElement> result, final ClassMethodActor classMethodActor, int sourceLineNumber, int targetMethodOffset) {
        final ClassActor holder = classMethodActor.holder();
        final String sourceFileName = sourceLineNumber < 0 && targetMethodOffset >= 0 ? holder.sourceFileName + "@" + targetMethodOffset : holder.sourceFileName;
        result.add(new StackTraceElement(holder.name.toString(), classMethodActor.name.toString(), sourceFileName, sourceLineNumber));
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
        return (StackTraceElement[]) TupleAccess.readObject(thisThrowable(), stackTrace.offset());
    }

}
