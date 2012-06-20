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
package com.oracle.max.vm.ext.jjvmti.agents.methodargs;

import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;

import java.lang.reflect.*;
import java.util.regex.*;

import com.oracle.max.vm.ext.jjvmti.agents.util.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.ext.jvmti.*;


public class MethodArgs extends NullJJVMTICallbacks implements JJVMTI.EventCallbacks {
    private static MethodArgs methodArgs;
    private static String MethodArgsArgs;
    private static boolean inEvent;

    private static Pattern methodPattern;

    static {
        methodArgs = (MethodArgs) JJVMTIAgentAdapter.register(new MethodArgs());
        if (MaxineVM.isHosted()) {
            VMOptions.addFieldOption("-XX:", "MethodArgsArgs", "arguments for methodargs JJVMTI agent");
        }
    }

    /***
     * VM extension entry point.
     *
     * @param args
     */
    public static void onLoad(String agentArgs) {
        MethodArgsArgs = agentArgs;
        methodArgs.onBoot();
    }

    /**
     * Boot image entry point.
     */
    @Override
    public void onBoot() {
        methodArgs.setEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, null);
    }

    @Override
    public void vmInit() {
        String pattern = ".*";
        if (MethodArgsArgs != null) {
            String[] args = MethodArgsArgs.split(",");
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("methods")) {
                    int ix = arg.indexOf('=');
                    if (ix < 0) {
                        usage();
                    }
                    pattern = arg.substring(ix + 1);
                } else {
                    usage();
                }
            }
        }

        methodPattern = Pattern.compile(pattern);

        methodArgs.setEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_ENTRY, null);
        methodArgs.setEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_METHOD_EXIT, null);
    }

    private static void usage() {
        fail("usage: methods=pattern");
    }

    private static void fail(String message) {
        Log.println(message);
        MaxineVM.exit(-1);
    }

    @Override
    public void methodEntry(Thread thread, MethodActor methodActor) {
        if (!inEvent) {
            try {
                inEvent = true;
                // Don't report synthetic methods (not clear we should even get the event)
                if (isMethodSynthetic(methodActor)) {
                    return;
                }
                String string = methodActor.format("%H.%n");
                if (methodPattern.matcher(string).matches()) {
                    System.out.printf("Method entry: %s%n", string);
                    Type[] params = methodActor.getGenericParameterTypes();
                    JJVMTI.LocalVariableEntry[] lve = getLocalVariableTable(methodActor);
                    if (lve.length == 0) {
                        return;
                    }
                    for (int i = 0; i < params.length; i++) {
                        Class< ? > param = (Class) params[i];
                        int index = methodActor.isStatic() ? i : i + 1;
                        System.out.printf("param %d, name %s, type %s, value ", index, lve[index].name, lve[index].signature);
                        int slot = lve[index].slot;
                        if (Object.class.isAssignableFrom(param)) {
                            Object paramValue = getLocalObject(null, 0, slot);
                            System.out.println(paramValue);
                        } else {
                            if (param == int.class) {
                                System.out.println(getLocalInt(null, 0, slot));
                            } else if (param == long.class) {
                                System.out.println(getLocalLong(null, 0, slot));
                            } else if (param == float.class) {
                                System.out.println(getLocalFloat(null, 0, slot));
                            } else if (param == double.class) {
                                System.out.println(getLocalDouble(null, 0, slot));
                            } else {
                                assert false;
                            }
                        }
                    }
                }
            } finally {
                inEvent = false;
            }
        }
    }

    @Override
    public void methodExit(Thread thread, MethodActor methodActor, boolean exception, Object returnValue) {
        if (!inEvent) {
            try {
                inEvent = true;
                // Don't report synthetic methods (not clear we should even get the event)
                if (isMethodSynthetic(methodActor)) {
                    return;
                }
                String string = methodActor.format("%H.%n");
                if (methodPattern.matcher(string).matches()) {
                    // don't print return value as it invokes toString
                    System.out.printf("Method exit: %s, exception %b%n", string, exception);
                }
            } finally {
                inEvent = false;
            }
        }
    }


}
