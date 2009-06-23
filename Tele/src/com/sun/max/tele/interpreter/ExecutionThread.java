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
package com.sun.max.tele.interpreter;

import java.io.*;

import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;

/**
 * Instances of this class contain the execution state of a single thread in the system.
 * NOTE: currently there _is_ only one thread. Please ignore the man behind the curtain.
 *
 * @author Athul Acharya
 */
class ExecutionThread {

    /**
     * The maximum frame depth for a thread.
     */
    public static final int STACK_SIZE = 1000;

    private ExecutionFrame frame;
    //private int _prio;
    //private ThreadType _threadType;

    public ExecutionThread(int prio, ThreadType threadType) {
        //_prio = prio;
        //_threadType = threadType;
        frame = null;
    }

    public ExecutionFrame pushFrame(ClassMethodActor method) {
        this.frame = new ExecutionFrame(frame, method);
        if (frame.depth() > STACK_SIZE) {
            throw new StackOverflowError();
        }
        return frame;
    }

    public ExecutionFrame popFrame() {
        frame = frame.callersFrame();
        return frame;
    }

    public ExecutionFrame frame() {
        return frame;
    }

    public static enum ThreadType {
        NORMAL_THREAD,
        VM_THREAD,
    }

    /**
     * Handles an exception at the current execution point in this thread by updating the call stack and instruction
     * pointer to a matching exception handler in this thread's current call stack. If no matching exception handler is
     * found for the current execution point and the given exception type, then the call stack and instruction pointer
     * in this thread are left unmodified.
     *
     * @param throwableClassActor
     * @return {@code true} if an exception handler was found, {@code false} otherwise
     */
    public boolean handleException(ClassActor throwableClassActor) {
        ExecutionFrame frame = this.frame;
        while (frame != null) {
            if (frame.handleException(throwableClassActor)) {
                this.frame = frame;
                return true;
            }

            frame = frame.callersFrame();
        }
        return false;
    }

    public void printStackTrace(PrintStream printStream, TeleInterpreterException executionException) {
        ExecutionFrame frame = this.frame;
        printStream.println(executionException.getMessage());
        while (frame != null) {
            printStream.println("\tat " + frame.method().toStackTraceElement(frame.currentOpcodePosition()));
            frame = frame.callersFrame();
        }
        if (executionException.getCause() != null) {
            printStream.print("Caused by: ");
            executionException.getCause().printStackTrace(printStream);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        ExecutionFrame frame = this.frame;
        while (frame != null) {
            sb.append(String.format("%n%s [bci:%d]", frame.method().toStackTraceElement(frame.currentOpcodePosition())));
            frame = frame.callersFrame();
        }
        return sb.toString();
    }
}
