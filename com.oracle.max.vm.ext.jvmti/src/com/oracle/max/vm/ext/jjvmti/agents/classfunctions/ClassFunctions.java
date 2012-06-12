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
package com.oracle.max.vm.ext.jjvmti.agents.classfunctions;

import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;

import com.sun.max.vm.ext.jvmti.*;

/**
 * A {@link JJVMTI Java JVMTI agent} that tests the class functions part of the interface.
 */
public class ClassFunctions extends JJVMTIStdAgentAdapter {

    private ClassFunctions() {
        JJVMTIStdAgentAdapter.register(this);
    }

    public static void onLoad(String agentArgs) {
        boolean classLoad = false;
        String[] args = agentArgs.split(",");
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("classLoad")) {
                classLoad = true;
            }
        }

        ClassFunctions classFunctions = new ClassFunctions();

        if (classLoad) {
            classFunctions.setEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_LOAD, null);
        }
    }

    @Override
    public void classLoad(Thread thread, Class<?> klass) {
        System.out.printf("JJVMTI.classLoad: %s in %s%n", klass.getName(), thread.getName());
    }
}
