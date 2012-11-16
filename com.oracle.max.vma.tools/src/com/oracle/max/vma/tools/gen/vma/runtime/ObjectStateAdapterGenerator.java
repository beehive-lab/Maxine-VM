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
import com.oracle.max.vm.ext.vma.handlers.util.objstate.*;
import com.oracle.max.vma.tools.gen.vma.*;
import com.sun.max.annotate.*;

/**
 * Generates the rote implementations of ObjectStateHandlerAdaptor.
 * Object creation is special and requires that we assign a unique id.
 * All other methods that take objects as parameters have to be checked
 * in case their creation was not observed.
 *
 */

@HOSTED_ONLY
public class ObjectStateAdapterGenerator {

    public static void main(String[] args) throws Exception {
        createGenerator(ObjectStateAdapterGenerator.class);
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
        AdviceGeneratorHelper.updateSource(ObjectStateAdapter.class, null, false);
    }

    private static void generate(Method m) {
        String name = m.getName();
        out.printf("    @Override%n");
        generateSignature(m, null);
        out.printf(" {%n");
        if (name.equals("adviseAfterNew") || name.equals("adviseAfterNewArray")) {
            out.printf("        final Reference objRef = Reference.fromJava(arg2);%n");
            out.printf("        final Hub hub = UnsafeCast.asHub(Layout.readHubReference(objRef));%n");
            out.printf("        state.assignId(objRef);%n");
            out.printf("        checkClassLoaderId(hub.classActor);%n");
            out.printf("        visit(arg2);%n");
        } else {
            String checkMethod = null;
            if (name.contains("CheckCast") || name.contains("InstanceOf")) {
                checkMethod = "ClassLoaderIdOfClassActor";
            } else if (checkMemberActor(name)) {
                checkMethod = "ClassLoaderIdOfMemberActor";
            }
            Class<?>[] params = m.getParameterTypes();
            for (int i = 1; i < params.length; i++) {
                Class<?> param = params[i];
                int argId = i + 1;
                switch (i) {
                    case 2:
                        if (checkMethod != null) {
                            out.printf("        check%s(arg%d);%n", checkMethod, argId);
                        } else {
                            if (param == Object.class) {
                                out.printf("        visit(arg%d);%n", argId);
                            }
                        }
                        break;

                    default:
                        // Currently, the static tuple is passed to Get/Put/Invoke/Static but the
                        // classloader id of the class is is checked by ClassLoaderIdOfMemberActor
                        if (param == Object.class && !isGetPutStatic(name)) {
                            out.printf("        visit(arg%d);%n", argId);
                        }

                }
            }
        }
        out.printf("    }%n%n");
    }

    private static boolean checkMemberActor(String name) {
        return name.contains("Static") || name.contains("Field") || name.contains("Invoke") || name.contains("MethodEntry");
    }

    private static boolean isGetPutStatic(String name) {
        return name.contains("GetStatic") || name.contains("PutStatic") || name.contains("InvokeStatic");
    }

}
