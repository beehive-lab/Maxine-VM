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
package com.sun.max.tele.debug;


import java.io.*;
import java.util.*;

import com.sun.max.tele.*;
import com.sun.max.unsafe.*;

/**
 * Singleton access for information about threads in the VM.
 */
public class VmThreadAccess extends AbstractVmHolder implements MaxThreadManager {

    private static final int TRACE_VALUE = 1;

    private static VmThreadAccess vmThreadAccess;

    public static VmThreadAccess make(TeleVM vm) {
        if (vmThreadAccess == null) {
            vmThreadAccess = new VmThreadAccess(vm);
        }
        return vmThreadAccess;
    }

    private VmThreadAccess(TeleVM vm) {
        super(vm);
    }

    public List<MaxThread> threads() {
        return vm().state().threads();
    }

    public MaxThread findThread(Address address) {
        for (MaxThread maxThread : threads()) {
            final MaxStack stack = maxThread.stack();
            final MaxThreadLocalsBlock threadLocalsBlock = maxThread.localsBlock();
            if (stack.memoryRegion().contains(address) ||
                            (threadLocalsBlock.memoryRegion() != null && threadLocalsBlock.memoryRegion().contains(address))) {
                return maxThread;
            }
        }
        return null;
    }

    public MaxStack findStack(Address address) {
        for (MaxThread maxThread : threads()) {
            final MaxStack stack = maxThread.stack();
            if (stack.memoryRegion().contains(address)) {
                return stack;
            }
        }
        return null;
    }

    public MaxThreadLocalsBlock findThreadLocalsBlock(Address address) {
        for (MaxThread maxThread : threads()) {
            final MaxThreadLocalsBlock threadLocalsBlock = maxThread.localsBlock();
            if (threadLocalsBlock.memoryRegion() != null && threadLocalsBlock.memoryRegion().contains(address)) {
                return threadLocalsBlock;
            }
        }
        return null;
    }

    public MaxThread getThread(long threadID) {
        for (MaxThread maxThread : threads()) {
            if (maxThread.id() == threadID) {
                return maxThread;
            }
        }
        return null;
    }


    /**
     * Writes a description of every thread to the stream,
     * with more detail than typically displayed.
     * <br>
     * Thread-safe
     *
     * @param printStream
     */
    public void writeSummary(PrintStream printStream) {
        printStream.println("Threads :" + vm().state());
        for (MaxThread maxThread : threads()) {
            printStream.println("  " + maxThread);
        }
    }

}
