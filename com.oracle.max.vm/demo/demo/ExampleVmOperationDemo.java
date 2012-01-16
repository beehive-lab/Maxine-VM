/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package demo;

import com.sun.max.unsafe.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;


/**
 * The example in the Wiki.
 */
public class ExampleVmOperationDemo extends VmOperation {

    private static boolean systemThreads;

    public ExampleVmOperationDemo() {
        super("ExampleGCDemo", null, Mode.Safepoint);
    }

    @Override
    protected boolean operateOnThread(VmThread vmThread) {
        if (!systemThreads) {
            return vmThread.javaThread().getThreadGroup() != VmThread.systemThreadGroup;
        } else {
            return true;
        }
    }

    @Override
    public void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
        System.out.println("Thread " + vmThread.getName() + " is stopped at 0x" + Long.toHexString(ip.toLong()));
    }

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-sys")) {
                systemThreads = true;
            }
        }
        new ExampleVmOperationDemo().submit();
    }

}
