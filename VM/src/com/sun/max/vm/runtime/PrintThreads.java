/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.runtime;

import java.util.*;

import sun.misc.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.jdk.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * A VM operation that dumps a stack trace to the {@linkplain Log log stream}
 * for each Java thread in the system.
 *
 * @author Doug Simon
 */
public class PrintThreads extends VmOperation implements SignalHandler {

    protected boolean internalFormat;

    public PrintThreads(boolean internalFormat) {
        super("PrintThreads", null, Mode.Safepoint);
        this.internalFormat = internalFormat;
    }

    @Override
    protected void doIt() {
        Log.println("Full thread dump:");
        super.doIt();
    }

    @Override
    protected void doThread(Pointer threadLocals, Pointer ip, Pointer sp, Pointer fp) {
        VmThread vmThread = VmThread.fromVmThreadLocals(threadLocals);
        if (internalFormat) {
            Throw.stackDump(vmThread.toString(), ip, sp, fp);
        } else {
            final List<StackFrame> frameList = new ArrayList<StackFrame>();
            new VmStackFrameWalker(threadLocals).frames(frameList, ip, sp, fp);
            Thread thread = vmThread.javaThread();
            StackTraceElement[] trace = JDK_java_lang_Throwable.asStackTrace(frameList, null, Integer.MAX_VALUE);
            Log.println(thread);
            for (StackTraceElement e : trace) {
                Log.println("\tat " + e);
            }
        }
    }

    public void handle(Signal sig) {
        submit();
    }
}
