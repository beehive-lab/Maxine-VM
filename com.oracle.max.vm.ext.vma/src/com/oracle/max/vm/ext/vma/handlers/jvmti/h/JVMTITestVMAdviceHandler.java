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

import com.oracle.max.vm.ext.jjvmti.agents.util.*;
import com.oracle.max.vm.ext.vma.handlers.util.*;
import com.oracle.max.vm.ext.vma.run.java.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.ext.jvmti.*;

/**
 * A simple test of the VMA variant of method entry/exit events.
 * Can be built into the boot image or dynamically loaded.
 */
public class JVMTITestVMAdviceHandler extends NullVMAdviceHandler {

    /**
     * Used to get access to local variables.
     */
    private static final JJVMTIAgentAdapter jjvmti;

    static {
        jjvmti = JJVMTIAgentAdapter.register(new NullJJVMTICallbacks());
    }

    private static Stack<MethodActor> callStack = new Stack<MethodActor>();

    public static void onLoad(String args) {
        VMAJavaRunScheme.registerAdviceHandler(new JVMTITestVMAdviceHandler());
    }

    @Override
    public void adviseAfterMethodEntry(Object arg1, MethodActor methodActor) {
        callStack.push(methodActor);
        // Don't report synthetic methods (not clear we should even get the event)
        if (jjvmti.isMethodSynthetic(methodActor)) {
            return;
        }
        System.out.printf("Method entry: %s%n", methodActor.format("%H.%n"));
        JJVMTI.LocalVariableEntry[] lve = jjvmti.getLocalVariableTable((ClassMethodActor) methodActor);
        if (lve.length == 0) {
            return;
        }
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
        MethodActor methodActor = callStack.pop();
        // Don't report synthetic methods (not clear we should even get the event)
        if (jjvmti.isMethodSynthetic(methodActor)) {
            return;
        }
        System.out.printf("Method exit: %s, exception %b%n", methodActor.format("%H.%n"), exeception);
    }

}
