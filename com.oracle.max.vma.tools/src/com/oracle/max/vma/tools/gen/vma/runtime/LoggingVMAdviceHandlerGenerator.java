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
package com.oracle.max.vma.tools.gen.vma.runtime;

import static com.sun.max.vm.t1x.T1XTemplateGenerator.*;
import static com.oracle.max.vma.tools.gen.vma.AdviceGeneratorHelper.*;

import java.lang.reflect.*;

import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.runtime.*;
import com.oracle.max.vma.tools.gen.vma.log.*;
import com.sun.max.annotate.*;

/**
 * Generates the rote implementations of {@link LoggingVMAdviceHandler} from the
 * methods in {@link VMAdviceHandler}.
 */
@HOSTED_ONLY
public class LoggingVMAdviceHandlerGenerator {

    public static void main(String[] args) {
        createGenerator(LoggingVMAdviceHandlerGenerator.class);
        for (Method m : VMAdviceHandler.class.getMethods()) {
            if (m.getName().startsWith("advise")) {
                generate(m);
            }
        }
    }

    private static void generate(Method m) {
        String name = m.getName();
        String oname = VMAdviceHandlerLogGenerator.getMethodNameRenamingObject(m);
        generateAutoComment();
        out.printf("    @Override%n");
        generateSignature(m, null);
        out.printf(" {%n");
        if (name.endsWith("ConstLoad")) {
            generateLogCallPrefix(oname);
            generateValueArg(m, 1);
            out.printf(");%n");
        } else if (name.endsWith("GetField")  || name.endsWith("PutField")) {
            out.printf("        ClassActor ca = ObjectAccess.readClassActor(arg1);%n");
            out.printf("        FieldActor fa = ca.findInstanceFieldActor(arg2);%n");
            generateLogCallPrefix(oname);
            out.printf(", state.readId(arg1), fa.holder().name(), state.readId(ca.classLoader), fa.name()");
            if (name.endsWith("PutField")) {
                generateValueArg(m, 3);
            }
            out.printf(");%n");
        } else if (name.endsWith("GetStatic")  || name.endsWith("PutStatic")) {
            out.printf("        ClassActor ca = ObjectAccess.readClassActor(arg1);%n");
            generateLogCallPrefix(oname);
            out.printf(", ca.name(), state.readId(ca.classLoader), ca.findStaticFieldActor(arg2).name()");
            if (name.endsWith("PutStatic")) {
                if (name.endsWith("PutStatic")) {
                    generateValueArg(m, 3);
                }
            }
            out.printf(");%n");
        } else if (name.endsWith("ArrayLoad") || name.endsWith("ArrayStore")) {
            generateLogCallPrefix(oname);
            out.printf(", state.readId(arg1), arg2");
            if (name.endsWith("ArrayStore")) {
                generateValueArg(m, 3);
            }
            out.printf(");%n");
        } else if (name.endsWith("Store")) {
            generateLogCallPrefix(oname);
            generateValueArg(m, 1);
            generateValueArg(m, 2);
            out.printf(");%n");
        } else if (name.equals("adviseAfterNew") || name.equals("adviseAfterNewArray")) {
            out.printf("        final Reference objRef = Reference.fromJava(arg1);%n");
            out.printf("        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));%n");
            generateLogCallPrefix(oname);
            out.printf(", state.readId(arg1), hub.classActor.name(), state.readId(hub.classActor.classLoader)");
            if (name.endsWith("NewArray")) {
                out.print(", arg2");
            }
            out.printf(");%n");
        } else if (name.equals("adviseAfterMultiNewArray")) {
            out.printf("        ProgramError.unexpected(\"adviseAfterMultiNewArray\");%n");
        } else if (name.endsWith("If")) {
            generateLogCallPrefix(oname);
            generateValueArg(m, 1);
            generateValueArg(m, 2);
            generateValueArg(m, 3);
            out.printf(");%n");
        } else if (name.contains("Return")) {
            generateLogCallPrefix(oname);
            if (m.getParameterTypes().length > 0) {
                generateValueArg(m, 1);
            }
            out.printf(");%n");
        } else if (name.contains("Invoke") || name.contains("MethodEntry")) {
            generateLogCallPrefix(oname);
            String arg1 = name.contains("Static") ? "0" : "state.readId(arg1)";
            out.printf(", %s, arg2.holder().name(), state.readId(arg2.holder().classLoader), arg2.name()", arg1);
            out.printf(");%n");
        } else if (name.endsWith("ArrayLength")) {
            generateLogCallPrefix(oname);
            out.print(", state.readId(arg1), arg2");
            out.printf(");%n");
        } else if (name.contains("Monitor") || name.contains("Throw")) {
            generateLogCallPrefix(oname);
            out.print(", state.readId(arg1)");
            out.printf(");%n");
        } else if (name.contains("CheckCast") || name.contains("InstanceOf")) {
            out.printf("        ClassActor ca = (ClassActor) arg2;%n");
            generateLogCallPrefix(oname);
            out.print(", state.readId(arg1), ca.name(), state.readId(ca.classLoader)");
            out.printf(");%n");
        } else if (name.contains("Thread")) {
            // drop VmThread arg
            generateLogCallPrefix(oname);
            out.printf(");%n");
        } else {
            generateLogCallPrefix(oname);
            Class<?>[] params = m.getParameterTypes();
            for (int argc = 1; argc <= params.length; argc++) {
                out.printf(", %s", "arg" + argc);
            }
            out.printf(");%n");
        }
        out.printf("    }%n%n");

    }

    private static void generateLogCallPrefix(String name) {
        out.printf("        log.%s(tng.getThreadName()", name);
    }

    private static void generateValueArg(Method m, int argc) {
        String arg = "arg" + argc;
        String valueType = getNthParameterName(m, argc);
        if (valueType.equals("Object")) {
            out.printf(", state.readId(%s)", arg);
        } else {
            out.printf(", %s", arg);
        }
    }

}
