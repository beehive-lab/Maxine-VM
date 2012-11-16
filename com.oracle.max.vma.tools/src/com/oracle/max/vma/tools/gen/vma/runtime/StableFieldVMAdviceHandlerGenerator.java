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
import com.oracle.max.vm.ext.vma.handlers.sf.h.*;
import com.oracle.max.vma.tools.gen.vma.*;


public class StableFieldVMAdviceHandlerGenerator extends SuperAdviceHandlerGenerator {
    public static void main(String[] args) throws Exception {
        StableFieldVMAdviceHandlerGenerator self = (StableFieldVMAdviceHandlerGenerator) createGenerator(StableFieldVMAdviceHandlerGenerator.class);
        generateAutoComment();
        for (Method m : VMAdviceHandler.class.getMethods()) {
            String name = m.getName();
            if (name.startsWith("advise")) {
                if (name.contains("GC") ||
                    name.contains("ThreadStarting") || name.contains("ThreadTerminating")) {
                    continue;
                }
                self.generate(m, self.generateHeader(m));
                self.generateTrailer(m);
            }
        }
        AdviceGeneratorHelper.updateSource(StableFieldVMAdviceHandler.class, null, false);
    }

    @Override
    protected void generate(Method m, int argCount) {
        String name = m.getName();
        super.generate(m, argCount);
        out.println("        advanceTime();");
        if (name.equals("adviseAfterNew") || name.equals("adviseAfterNewArray")) {
            out.println("        recordNew(arg2, " + (name.equals("adviseAfterNew") ? "false" : "true") + ");");
        } else if (name.equals("adviseBeforePutField")) {
            out.println("        recordPutField(arg2, arg3);");
            if (lastParamIsObject(m)) {
                out.printf("        recordAccess(arg4);%n");
            }
        } else if (name.equals("adviseAfterMethodEntry")) {
            out.println("        recordMethodEntry(arg2, arg3);");
        } else if (name.contains("Return")) {
            out.printf("        recordReturn(%s);%n", name.contains("Throw") ? "arg3" : "1");
            if (lastParamIsObject(m)) {
                out.printf("        recordAccess(arg2);%n");
            }
        } else if (name.equals("adviseBeforeInvokeSpecial")) {
            out.println("        recordInvokeSpecial(arg2, arg3);");
        } else if (name.contains("adviseBeforeArrayStore")) {
            out.println("        recordArrayStore(arg2);");
            if (lastParamIsObject(m)) {
                out.printf("        recordAccess(arg4);%n");
            }
        } else {
            Class< ? >[] params = m.getParameterTypes();
            for (int i = 1; i < params.length; i++) {
                Class< ? > param = params[i];
                int argId = i + 1;
                if (param == Object.class && isApplicationObject(name, i)) {
                    out.printf("        recordAccess(arg%d);%n", argId);
                }
            }
        }
    }

    private static boolean isApplicationObject(String name, int arg) {
        if ((name.contains("GetStatic") || name.contains("PutStatic") || name.contains("InvokeStatic")) && arg == 1) {
            return false;
        } else if ((name.contains("CheckCast") || name.contains("InstanceOf")) && arg == 2) {
            return false;
        } else {
            return true;
        }
    }

    private static boolean lastParamIsObject(Method m) {
        String last = getLastParameterName(m);
        return last != null && last.equals("Object");
    }

}
