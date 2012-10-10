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
package com.oracle.max.vma.tools.gen.vma.runtime;

import static com.oracle.max.vma.tools.gen.vma.AdviceGeneratorHelper.*;

import java.lang.reflect.*;

import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.handlers.objstate.*;
import com.oracle.max.vm.ext.vma.handlers.store.vmlog.h.*;
import com.oracle.max.vma.tools.gen.vma.*;
import com.sun.max.vm.jni.*;

/**
 * Generate the {@code VMAVMLogger} class body which uses the tracing aspect of logging to handle log flushing.
 *
 * Provision is made to log "time" as the first argument of the log record. A handler may choose to ignore this field,
 * when generating log records.
 *
 */
public class VMAVMLoggerGenerator {
    private static final String TRACE_PREFIX = "trace";

    public static void main(String[] args) throws Exception {
        createGenerator(VMAVMLoggerGenerator.class);
        generateAutoComment();

        for (Method m : VMAdviceHandler.class.getMethods()) {
            String name = m.getName();
            if (name.startsWith("advise")) {
                generateImpl(m);
            }
        }

        Method unseenObject = ObjectStateHandlerAdaptor.class.getDeclaredMethod("unseenObject", Object.class);
        Method dead = ObjectStateHandler.DeadObjectHandler.class.getDeclaredMethod("dead", ObjectID.class);
        generateImpl(unseenObject);
        generateImpl(dead);
        out.printf("%s}%n%n", INDENT4);


        out.printf("%s@HOSTED_ONLY%n", INDENT4);
        out.printf("%s@VMLoggerInterface(hidden = true, traceThread = true)%n", INDENT4);
        out.printf("%sprivate interface VMAVMLoggerInterface {%n", INDENT4);
        for (Method m : VMAdviceHandler.class.getMethods()) {
            String name = m.getName();
            if (name.startsWith("advise")) {
                generate(m, null);
            }
        }
        generateIntf(unseenObject);
        generateIntf(dead);
        out.printf("%s}%n", INDENT4);
        AdviceGeneratorHelper.updateSource(VMAVMLogger.class, null, "// START GENERATED INTERFACE", "// END GENERATED INTERFACE", false);
    }

    private static class MyMethodNameOverride extends MethodNameOverride {

        public MyMethodNameOverride(Method m) {
            super(m);
        }
        @Override
        public String overrideName() {
            return "trace" + toFirstUpper(method.getName());
        }
    }

    /*
    private static void generateImpl(Method m) {
        out.print(INDENT8);
        out.println("@Override");
        int argCount = generateSignature(INDENT8, "protected", new MyMethodNameOverride(m), null, implArgumentsPrefix);
        out.println(" {");
        out.printf("%sstoreAdaptor(threadId).%s(", INDENT12, m.getName());
        generateInvokeArgs(argCount);
        out.printf("%s}%n", INDENT8);
    }
    */

    private static void generateIntf(Method m) {
        generate(m, null);
    }

    private static void generateImpl(Method m) {
        generate(m, TRACE_PREFIX);
    }

    private static void generate(Method m, String tracePrefix) {
        final String name = m.getName();
        if (name.contains("Multi")) {
            return;
        }
        if (tracePrefix != null) {
            out.printf("%s@Override%n", INDENT8);
        }

        final String methodName = tracePrefix == null ? name : tracePrefix + toFirstUpper(name);
        String threadId = tracePrefix == null ? "" : "int threadId, ";
        String protectedTag = tracePrefix == null ? "" : "protected ";
        out.printf("%s%svoid %s(%slong time%s", INDENT8, protectedTag, methodName, threadId,
                        AdviceGeneratorHelper.isBytecodeAdviceMethod(m) || m.getName().contains("ReturnByThrow") ? ", int bci" : "");
        if (name.contains("New") || name.contains("unseen")) {
            out.print(", ObjectID objId, ClassID classId");
            if (name.contains("NewArray")) {
                out.print(", int length");
            }
        } else if (name.contains("dead")) {
            out.print(", ObjectID objId");
        } else {
            Class< ? >[] params = m.getParameterTypes();
            // skip bci
            for (int i = 1; i < params.length; i++) {
                Class< ? > param = params[i];
                out.printf(", %s %s", convertParamType(param.getSimpleName()), "arg" + i);
            }
        }
        out.printf(")");

        if (tracePrefix == null) {
            out.println(";");
        } else {
            out.println(" {");
            out.printf("%s}%n%n", INDENT8);
        }
    }

    private static String convertParamType(String name) {
        if (name.equals("Object") || name.equals("Throwable")) {
            return "ObjectID";
        } else if (name.equals("MethodActor")) {
            return "MethodID";
        } else {
            return name;
        }
    }
}
