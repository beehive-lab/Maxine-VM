/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
