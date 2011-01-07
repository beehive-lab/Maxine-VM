/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.runtime;

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
    protected void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
        if (internalFormat) {
            if (ip.isZero()) {
                Log.println(vmThread.toString());
            } else {
                Throw.stackDump(vmThread.toString(), ip, sp, fp);
            }
        } else {
            Thread thread = vmThread.javaThread();
            Log.println(thread);
            if (!ip.isZero()) {
                VmStackFrameWalker sfw = new VmStackFrameWalker(vmThread.tla());
                StackTraceElement[] trace = JDK_java_lang_Throwable.getStackTrace(sfw, ip, sp, fp, null, Integer.MAX_VALUE);
                for (StackTraceElement e : trace) {
                    Log.println("\tat " + e);
                }
            }
        }
    }

    public void handle(Signal sig) {
        submit();
    }
}
