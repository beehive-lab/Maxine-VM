/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

import sun.misc.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.thread.*;

/**
 * A VM operation that dumps a stack trace to the {@linkplain Log log stream}
 * for each Java thread in the system.
 */
public class PrintThreads extends VmOperation implements SignalHandler {

    protected boolean internalFormat;

    public PrintThreads(boolean internalFormat) {
        super("PrintThreads", null, Mode.Safepoint);
        this.internalFormat = internalFormat;
    }

    @Override
    protected void doIt() {
        Date now = new Date();
        Log.println(now);
        Log.println(String.format("Full thread dump %s (%s %s):",
            System.getProperty("java.vm.name"),
            System.getProperty("java.vm.version"),
            System.getProperty("java.vm.info")));

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
            Log.println();
            Log.println('"' + thread.getName() + "\" prio=" + thread.getPriority() + " tid=" + thread.getId() +
                        " nid=" + vmThread.nativeThread().to0xHexString());
            Log.println("   java.lang.Thread.State: " + thread.getState());

            if (!ip.isZero()) {
                VmStackFrameWalker sfw = new VmStackFrameWalker(vmThread.tla());
                StackTraceVisitor stv = new StackTraceVisitor(null, Integer.MAX_VALUE) {
                    @Override
                    public boolean add(ClassMethodActor method, int sourceLineNumber) {
                        ClassActor holder = method.holder();
                        Log.println("\tat " + new StackTraceElement(holder.name.toString(), method.name(), holder.sourceFileName, sourceLineNumber));
                        return true;
                    }
                };
                stv.walk(sfw, ip, sp, fp);
            }
        }
    }

    public void handle(Signal sig) {
        submit();
    }
}
