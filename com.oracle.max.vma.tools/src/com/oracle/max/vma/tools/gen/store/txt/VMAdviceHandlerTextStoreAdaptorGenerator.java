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
package com.oracle.max.vma.tools.gen.store.txt;

import static com.oracle.max.vma.tools.gen.vma.AdviceGeneratorHelper.*;

import java.lang.reflect.*;

import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.store.txt.*;
import com.oracle.max.vma.tools.gen.vma.*;
import com.sun.max.annotate.*;

/**
 * Generates implementation of {@link VMAdviceHandlerTextStoreAdaptor} from the
 * methods in {@link VMAdviceHandler}.
 */
@HOSTED_ONLY
public class VMAdviceHandlerTextStoreAdaptorGenerator {

    private static class MyArgumentsPrefix extends ArgumentsPrefix {
        int argCount;
        @Override
        public int prefixArguments() {
            out.print("long time");
            if (argCount > 0) {
                out.print(", ");
            }
            return 0;
        }
    }

    private static MyArgumentsPrefix prefixArgs = new MyArgumentsPrefix();

    public static void main(String[] args) throws Exception {
        createGenerator(VMAdviceHandlerTextStoreAdaptorGenerator.class);
        generateAutoComment();
        for (Method m : VMAdviceHandler.class.getMethods()) {
            if (m.getName().startsWith("advise")) {
                generate(m);
            }
        }
        AdviceGeneratorHelper.updateSource(VMAdviceHandlerTextStoreAdapter.class, null, false);
    }

    private static void generate(Method m) {
        String name = m.getName();
        String oname = VMATextStoreGenerator.getMethodNameRenamingObject(m);
        boolean isBytecodeAdviceMethod = AdviceGeneratorHelper.isBytecodeAdviceMethod(m);
        prefixArgs.argCount = m.getParameterTypes().length;
        generateSignature(INDENT4, "public", new MethodNameOverride(m), null, prefixArgs);
        out.printf(" {%n");
        if (name.endsWith("ConstLoad")) {
            generateStoreCallPrefix(oname, isBytecodeAdviceMethod);
            generateValueArg(m, 2);
            out.printf(");%n");
        } else if (name.endsWith("GetField")  || name.endsWith("PutField")) {
            out.printf("        ClassActor ca = ObjectAccess.readClassActor(arg2);%n");
            generateStoreCallPrefix(oname, isBytecodeAdviceMethod);
            out.printf(", state.readId(arg2).toLong(), arg3.holder().name(), state.readId(ca.classLoader).toLong(), arg3.name()");
            if (name.endsWith("PutField")) {
                generateValueArg(m, 4);
            }
            out.printf(");%n");
        } else if (name.endsWith("GetStatic")  || name.endsWith("PutStatic")) {
            out.printf("        ClassActor ca = ObjectAccess.readClassActor(arg2);%n");
            generateStoreCallPrefix(oname, isBytecodeAdviceMethod);
            out.printf(", ca.name(), state.readId(ca.classLoader).toLong(), arg3.name()");
            if (name.endsWith("PutStatic")) {
                generateValueArg(m, 4);
            }
            out.printf(");%n");
        } else if (name.endsWith("ArrayLoad") || name.endsWith("ArrayStore")) {
            generateStoreCallPrefix(oname, isBytecodeAdviceMethod);
            out.printf(", state.readId(arg2).toLong(), arg3");
            if (name.endsWith("ArrayStore") || name.endsWith("AfterArrayLoad")) {
                generateValueArg(m, 4);
            }
            out.printf(");%n");
        } else if (name.endsWith("Store") || name.endsWith("AfterLoad")) {
            generateStoreCallPrefix(oname, isBytecodeAdviceMethod);
            generateValueArg(m, 2);
            generateValueArg(m, 3);
            out.printf(");%n");
        } else if (name.equals("adviseAfterNew") || name.equals("adviseAfterNewArray")) {
            out.printf("        final Reference objRef = Reference.fromJava(arg2);%n");
            out.printf("        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));%n");
            generateStoreCallPrefix(oname, isBytecodeAdviceMethod);
            out.printf(", state.readId(arg2).toLong(), hub.classActor.name(), state.readId(hub.classActor.classLoader).toLong()");
            if (name.endsWith("NewArray")) {
                out.print(", arg3");
            }
            out.printf(");%n");
        } else if (name.equals("adviseAfterMultiNewArray")) {
            out.printf("        ProgramError.unexpected(\"adviseAfterMultiNewArray\");%n");
        } else if (name.endsWith("If")) {
            generateStoreCallPrefix(oname, isBytecodeAdviceMethod);
            generateValueArg(m, 2);
            generateValueArg(m, 3);
            generateValueArg(m, 4);
            generateValueArg(m, 5);
            out.printf(");%n");
        } else if (name.contains("ReturnByThrow")) {
            generateStoreCallPrefix(oname, true);
            out.print(", state.readId(arg2).toLong(), arg3);\n");
        } else if (name.contains("Return")) {
            generateStoreCallPrefix(oname, isBytecodeAdviceMethod);
            if (m.getParameterTypes().length > 1) {
                generateValueArg(m, 2);
            }
            out.printf(");%n");
        } else if (name.contains("Invoke") || name.contains("MethodEntry")) {
            generateStoreCallPrefix(oname, isBytecodeAdviceMethod);
            String arg1 = name.contains("Static") ? "0" : "state.readId(arg2).toLong()";
            out.printf(", %s, arg3.holder().name(), state.readId(arg3.holder().classLoader).toLong(), arg3.name()", arg1);
            out.printf(");%n");
        } else if (name.endsWith("ArrayLength")) {
            generateStoreCallPrefix(oname, isBytecodeAdviceMethod);
            out.print(", state.readId(arg2).toLong(), arg3");
            out.printf(");%n");
        } else if (name.contains("Monitor") || name.contains("Throw")) {
            generateStoreCallPrefix(oname, isBytecodeAdviceMethod);
            out.print(", state.readId(arg2).toLong()");
            out.printf(");%n");
        } else if (name.contains("CheckCast") || name.contains("InstanceOf")) {
            out.printf("        ClassActor ca = (ClassActor) arg3;%n");
            generateStoreCallPrefix(oname, isBytecodeAdviceMethod);
            out.print(", state.readId(arg2).toLong(), ca.name(), state.readId(ca.classLoader).toLong()");
            out.printf(");%n");
        } else if (name.contains("Thread")) {
            // drop VmThread arg
            generateStoreCallPrefix(oname, isBytecodeAdviceMethod);
            out.printf(");%n");
        } else {
            generateStoreCallPrefix(oname, isBytecodeAdviceMethod);
            Class<?>[] params = m.getParameterTypes();
            for (int argc = isBytecodeAdviceMethod ? 2 : 1; argc <= params.length; argc++) {
                out.printf(", %s", "arg" + argc);
            }
            out.printf(");%n");
        }
        out.printf("    }%n%n");

    }

    private static void generateStoreCallPrefix(String name, boolean isBytecodeAdviceMethod) {
        // Every bytecode advice method has arg1, as it's the bci value
        out.printf("        txtStore.%s(time, perThread ? null : tng.getThreadName()", name);
        if (isBytecodeAdviceMethod) {
            out.print(", arg1");
        }
    }

    private static void generateValueArg(Method m, int argc) {
        String arg = "arg" + argc;
        String valueType = getNthParameterName(m, argc);
        if (valueType.equals("Object")) {
            out.printf(", state.readId(%s).toLong()", arg);
        } else {
            out.printf(", %s", arg);
        }
    }

}
