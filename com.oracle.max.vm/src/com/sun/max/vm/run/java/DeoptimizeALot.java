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
package com.sun.max.vm.run.java;

import static com.sun.max.vm.compiler.deopt.Deoptimization.*;

import java.util.*;

import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.code.*;
import com.sun.max.vm.compiler.deopt.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.StackFrameWalker.Cursor;
import com.sun.max.vm.thread.*;

/**
 * A daemon thread that triggers deoptimization periodically.
 */
class DeoptimizeALot extends Thread {

    private final int frequency;

    static class MethodSelector extends VmOperation {
        ArrayList<TargetMethod> methods = new ArrayList<TargetMethod>();
        MethodSelector() {
            super("DeoptimizeALotMethodSelector", null, Mode.Safepoint);
        }

        class Visitor extends RawStackFrameVisitor {
            @Override
            public boolean visitFrame(Cursor current, Cursor callee) {
                if (!current.isTopFrame()) {
                    TargetMethod tm = current.targetMethod();
                    if (assessMethod(tm)) {
                        methods.add(tm);
                    }
                }
                return true;
            }

            /**
             * Determines if a given method should be selected for deopt.
             */
            boolean assessMethod(TargetMethod tm) {
                return tm != null &&
                    tm.classMethodActor != null &&
                    !Code.bootCodeRegion().contains(tm.codeStart()) &&
                    !tm.isInterpreterCompatible() &&
                    !tm.classMethodActor.isUnsafe() &&
                    tm.invalidated() == null &&
                    !methods.contains(tm);
            }
        }

        @Override
        public void doThread(VmThread vmThread, Pointer ip, Pointer sp, Pointer fp) {
            new VmStackFrameWalker(vmThread.tla()).inspect(ip, sp, fp, new Visitor());
        }
    }

    /**
     * Creates a daemon thread that triggers deoptimization every {@code frequency} milliseconds.
     */
    public DeoptimizeALot(int frequency) {
        super("DeoptimizeALot");
        setDaemon(true);
        this.frequency = frequency;
    }

    @Override
    public void run() {
        MethodSelector selector = new MethodSelector();
        while (true) {
            try {
                Thread.sleep(frequency);
                selector.submit();
                ArrayList<TargetMethod> methods = selector.methods;
                if (!methods.isEmpty()) {
                    if (TraceDeopt) {
                        Log.println("DEOPT: DeoptimizeALot selected methods:");
                        for (TargetMethod tm : methods) {
                            Log.println("DEOPT:   " + tm + " [" + tm.codeStart().to0xHexString() + "]");
                        }
                    }
                    new Deoptimization(methods).go();
                }
                methods.clear();
            } catch (InterruptedException e) {
            }
        }
    }
}
