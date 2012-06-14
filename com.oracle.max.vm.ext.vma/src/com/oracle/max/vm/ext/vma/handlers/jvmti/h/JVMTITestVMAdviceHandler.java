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
import java.util.*;

import com.oracle.max.vm.ext.jvmti.*;
import com.oracle.max.vm.ext.vma.handlers.nul.h.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.ext.jvmti.*;
import com.sun.max.vm.log.VMLog.Record;
import com.sun.max.vm.log.hosted.*;


public class JVMTITestVMAdviceHandler extends NullVMAdviceHandler {

    @Override
    public void initialise(MaxineVM.Phase phase) {
        if (phase == MaxineVM.Phase.BOOTSTRAPPING) {
            JJVMTIMaxAgentAdapter.register(jjvmti);
        }
    }

    private static final JJVMTIMaxAgentAdapter jjvmti = new JJVMTIMaxAgentAdapter();

    private static Stack<MethodActor> callStack = new Stack<MethodActor>();

    @Override
    public void adviseAfterMethodEntry(Object arg1, MethodActor arg2) {
        int fc = jjvmti.getFrameCount(null);
        if (logger.enabled()) {
            logger.logMethodEntry(arg1, arg2, fc);
            if (logger.traceEnabled()) {
                logger.traceMethodEntry(arg1, arg2, fc);
            }
        }
        callStack.push(arg2);
    }

    @Override
    public void adviseBeforeReturn(long value) {
        methodExit(false, value);
    }

    @Override
    public void adviseBeforeReturn(float value) {
        methodExit(false, value);
    }

    @Override
    public void adviseBeforeReturn(double value) {
        methodExit(false, value);
    }

    @Override
    public void adviseBeforeReturn(Object value) {
        methodExit(false, value);
    }

    @Override
    public void adviseBeforeReturn() {
        methodExit(false, null);
    }

    @Override
    public void adviseBeforeReturnByThrow(Throwable throwable, int poppedFrames) {
        methodExit(true, throwable);
        // May need to pop more frames
        while (poppedFrames > 1) {
            callStack.pop();
            poppedFrames--;
        }
    }

    private void methodExit(boolean exeception, Object returnValue) {
        if (logger.enabled()) {
            logger.logMethodExit(exeception, returnValue);
            MethodActor methodActor = callStack.pop();
            if (logger.traceEnabled()) {
                // don't print return value as it invokes toString
                System.out.printf("Method exit: %s, exception %b%n", methodActor.format("%H.%n"), exeception);
            }
        }
    }

    private static class JVMTITestLogger extends JVMTITestLoggerAuto {

        protected JVMTITestLogger() {
            super("JVMTITest", "test JVMTI in Java implementation");
        }

        @Override
        protected void trace(Record r) { }

        protected void traceMethodEntry(Object arg1, MethodActor methodActor, int arg3) {
            System.out.printf("Method entry: %s fc %d%n", methodActor.format("%H.%n"), arg3);
            JJVMTICommon.LocalVariableEntry[] lve = jjvmti.getLocalVariableTable(methodActor);
            Type[] params = methodActor.getGenericParameterTypes();
            for (int i = 0; i < params.length; i++) {
                Class<?> param = (Class) params[i];
                int index = methodActor.isStatic() ? i : i + 1;
                System.out.printf("param %d, name %s, type %s, value ", index, lve[index].name, lve[index].signature);
                int slot = lve[index].slot;
                if (Object.class.isAssignableFrom(param)) {
                    Object paramValue = jjvmti.getLocalObject(null, 0, slot);
                    System.out.println(paramValue);
                } else {
                    if (param == int.class) {
                        System.out.println(jjvmti.getLocalInt(null, 0, slot));
                    } else if (param == long.class) {
                        System.out.println(jjvmti.getLocalLong(null, 0, slot));
                    } else if (param == float.class) {
                        System.out.println(jjvmti.getLocalFloat(null, 0, slot));
                    } else if (param == double.class) {
                        System.out.println(jjvmti.getLocalDouble(null, 0, slot));
                    } else {
                        assert false;
                    }
                }
            }
        }

    }

    private static final JVMTITestLogger logger = new JVMTITestLogger();

    @VMLoggerInterface(noTrace = true)
    private interface JVMTITestLoggerInterface {
        void methodEntry(Object arg1, MethodActor arg2, int frameCount);
        void methodExit(boolean exeception, Object returnValue);
    }

// START GENERATED CODE
    private static abstract class JVMTITestLoggerAuto extends com.sun.max.vm.log.VMLogger {
        public enum Operation {
            MethodEntry, MethodExit;

            @SuppressWarnings("hiding")
            public static final Operation[] VALUES = values();
        }

        private static final int[] REFMAPS = new int[] {0x1, 0x2};

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
        @INLINE
        public final void logMethodExit(boolean arg1, Object arg2) {
            log(Operation.MethodExit.ordinal(), booleanArg(arg1), objectArg(arg2));
        }
    }

// END GENERATED CODE
}
