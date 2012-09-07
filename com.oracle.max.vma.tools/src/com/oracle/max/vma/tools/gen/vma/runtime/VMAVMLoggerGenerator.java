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

/**
 * Generate the {@code VMAVMLogger} class body which uses the tracing aspect of logging to handle log flushing.
 *
 * Provision is made to log "time" as the first argument of the log record. A handler may choose to ignore this field,
 * when generating log records.
 */
public class VMAVMLoggerGenerator {
    public static void main(String[] args) throws Exception {
        createGenerator(VMAVMLoggerGenerator.class);
        generateAutoComment();

        for (Method m : VMAdviceHandler.class.getMethods()) {
            String name = m.getName();
            if (name.startsWith("advise")) {
                generateImpl(m);
            }
        }
        generateImpl(ObjectStateHandlerAdaptor.class.getDeclaredMethod("unseenObject", Object.class));
        out.printf("%s}%n%n", INDENT4);


        out.printf("%s@HOSTED_ONLY%n", INDENT4);
        out.printf("%s@VMLoggerInterface(hidden = true, traceThread = true)%n", INDENT4);
        out.printf("%sprivate interface VMAVMLoggerInterface {%n", INDENT4);
        for (Method m : VMAdviceHandler.class.getMethods()) {
            String name = m.getName();
            if (name.startsWith("advise")) {
                generateInterface(m);
            }
        }
        generateInterface(ObjectStateHandlerAdaptor.class.getDeclaredMethod("unseenObject", Object.class));
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

    private static class IntfArgumentsPrefix extends ArgumentsPrefix {
        @Override
        public int prefixArguments() {
            out.print("long arg1");
            return 1;
        }
    }

    private static class ImplArgumentsPrefix extends ArgumentsPrefix {
        @Override
        public int prefixArguments() {
            out.print("int threadId, long arg1");
            return 1;
        }
    }

    private static final IntfArgumentsPrefix intfArgumentsPrefix = new IntfArgumentsPrefix();
    private static final ImplArgumentsPrefix implArgumentsPrefix = new ImplArgumentsPrefix();

    private static void generateImpl(Method m) {
        out.print(INDENT8);
        out.println("@Override");
        int argCount = generateSignature(INDENT8, "protected", new MyMethodNameOverride(m), null, implArgumentsPrefix);
        out.println(" {");
        out.printf("%sstoreAdaptor(threadId).%s(", INDENT12, m.getName());
        generateInvokeArgs(argCount);
        out.printf("%s}%n", INDENT8);
    }

    private static void generateInterface(Method m) {
        generateSignature(INDENT8, null, new MethodNameOverride(m), null, intfArgumentsPrefix);
        out.printf(";%n%n");
    }
}
