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
import com.oracle.max.vm.ext.vma.handlers.store.vmlog.h.*;
import com.oracle.max.vma.tools.gen.vma.*;

/**
 * Handles the conversion of reference valued arguments to the {@link ObjectID}, {@link ClassID}, {@link MethodID},
 * {@link FieldID}. etc. Note that whereas the latter three support conversion back their {@link Actor} form,
 * an {@code ObjectID} does not. Therefore, the information on class and classloader has to be extracted
 * when the object is first observed, e.g., in a {@code NEW}.
 */
public class VMLogStoreVMAdviceHandlerGenerator {
    public static void main(String[] args) throws Exception {
        createGenerator(VMLogStoreVMAdviceHandlerGenerator.class);
        generateAutoComment();
        for (Method m : VMAdviceHandler.class.getMethods()) {
            String name = m.getName();
            if (name.startsWith("advise")) {
                if (name.contains("GC") ||
                    name.contains("ThreadStarting") || name.contains("ThreadTerminating")) {
                    continue;
                }
                generate(m);
            }
        }
        out.println("}");
        AdviceGeneratorHelper.updateSource(VMLogStoreVMAdviceHandler.class, null, false);
    }

    private static void generate(Method m) {
        String name = m.getName();
        out.printf("    @Override%n");
        int argCount = generateSignature(m, null);
        out.printf(" {%n");
        boolean isPutGet = false;
        boolean isPutGetStatic = false;
        if (name.contains("MultiNewArray")) {
            out.printf("        adviseAfterNewArray(arg1, arg2, arg3[0]);%n");
        } else {
            out.printf("        super.%s(", name);
            generateInvokeArgs(argCount);
            if (name.equals("adviseAfterNew") || name.equals("adviseAfterNewArray") ||
                name.contains("Field") || name.contains("GetStatic") || name.contains("PutStatic")) {
                out.printf("%sClassActor ca = ObjectAccess.readClassActor(arg2);%n", INDENT8);
            } else if (name.equals("adviseBeforeCheckCast") || name.equals("adviseBeforeInstanceOf")) {
                out.printf("%sClassActor ca = UnsafeCast.asClassActor(arg3);%n", INDENT8);
            }
            if (name.contains("Field") || name.contains("GetStatic") || name.contains("PutStatic")) {
                out.printf("%sFieldActor fa = ca.find%sFieldActor(arg3);%n", INDENT8, name.contains("Field") ? "Instance" : "Static");
                isPutGet = true;
                if (name.contains("Static")) {
                    isPutGetStatic = true;
                }
            }
            out.printf("        VMAVMLogger.logger.log%s(getTime()", toFirstUpper(m.getName()));
            // Have to convert Object to ObjectID etc.
            if (name.equals("adviseAfterNew") || name.equals("adviseAfterNewArray") ||
                            name.equals("adviseBeforeCheckCast") || name.equals("adviseBeforeInstanceOf")) {
                out.printf(", arg1, state.readId(arg2), ClassID.create(ca), state.readId(ca.classLoader)");
                if (name.endsWith("NewArray")) {
                    out.print(", arg3");
                }
            } else {
                Class<?>[] params = m.getParameterTypes();
                for (int i = 0; i < params.length; i++) {
                    if (isPutGetStatic && i == 1) {
                        // skip as encoded in FieldID
                        continue;
                    }
                    out.printf(", %s", convertArg(isPutGet, params[i].getSimpleName(), "arg" + (i + 1)));
                }
            }
            out.printf(");%n");
            if (m.getName().contains("NewArray")) {
                out.printf("        MultiNewArrayHelper.handleMultiArray(this, arg1, arg2);%n");
            }
        }
        out.printf("    }%n%n");
    }

    private static String convertArg(boolean isPutGet, String type, String arg) {
        if (type.equals("Object") || type.equals("Throwable")) {
            return "state.readId(" + arg + ")";
        } else if (type.equals("MethodActor")) {
            return "MethodID.fromMethodActor(" + arg + ")";
        } else if (isPutGet && arg.equals("arg3")) {
            return "FieldID.fromFieldActor(fa)";
        } else {
            return arg;
        }
    }
}
