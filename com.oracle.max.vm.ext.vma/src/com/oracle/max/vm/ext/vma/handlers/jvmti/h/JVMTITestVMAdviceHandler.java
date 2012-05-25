/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.vma.handlers.jvmti.h;

import java.lang.reflect.*;

import com.oracle.max.vm.ext.vma.handlers.nul.h.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.ext.jvmti.*;
import com.sun.max.vm.ext.jvmti.JJVMTI.*;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.hosted.*;


public class JVMTITestVMAdviceHandler extends NullVMAdviceHandler {

    @Override
    public void initialise(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.BOOTSTRAPPING) {
            JJVMTIAgentAdapter.register(jjvmti);
        }
    }

    private static final JJVMTIAgentAdapter jjvmti = new JJVMTIAgentAdapter();

    @Override
    public void adviseAfterMethodEntry(Object arg1, MethodActor arg2) {
        logger.logMethodEntry(arg1, arg2, jjvmti.getFrameCount(null));
    }

    private static class JVMTITestLogger extends JVMTITestLoggerAuto {

        protected JVMTITestLogger() {
            super("JVMTITest", "test JVMTI in Java implementation");
        }

        @Override
        protected void traceMethodEntry(Object arg1, MethodActor arg2, int arg3) {
            System.out.printf("Method entry: %s fc %d%n", arg2.format("%H.%n"), arg3);
            Method method = arg2.toJava();
            Class<?>[] params = method.getParameterTypes();
            LocalVariableEntry[] lve = jjvmti.getLocalVariableTable(method);
            int slot = 0;
            for (Class<?> param : params) {
                System.out.printf("  slot %d, name %s, type %s, value ", slot, lve[slot].name, lve[slot].signature);
                if (Object.class.isAssignableFrom(param)) {
                    Object paramValue = jjvmti.getLocalObject(null, 0, slot);
                    System.out.println(paramValue);
                }
            }
        }

    }

    private static final JVMTITestLogger logger = new JVMTITestLogger();

    @VMLoggerInterface
    private static interface JVMTITestLoggerInterface {
        void methodEntry(Object arg1, MethodActor arg2, int frameCount);
    }

// START GENERATED CODE
    private static abstract class JVMTITestLoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            MethodEntry;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = new int[] {0x1};

        protected JVMTITestLoggerAuto(String name, String optionDescription) {
            super(name, Operation.VALUES.length, optionDescription, REFMAPS);
        }

        @Override
        public String operationName(int opCode) {
            return Operation.VALUES[opCode].name();
        }

        @INLINE
        public final void logMethodEntry(Object arg1, MethodActor arg2, int arg3) {
            log(Operation.MethodEntry.ordinal(), objectArg(arg1), methodActorArg(arg2), intArg(arg3));
        }
        protected abstract void traceMethodEntry(Object arg1, MethodActor arg2, int arg3);

        @Override
        protected void trace(Record r) {
            switch (r.getOperation()) {
                case 0: { //MethodEntry
                    traceMethodEntry(toObject(r, 1), toMethodActor(r, 2), toInt(r, 3));
                    break;
                }
            }
        }
    }

// END GENERATED CODE
}
