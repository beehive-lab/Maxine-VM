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

import static com.oracle.max.vma.tools.gen.vma.AdviceGeneratorHelper.*;
import static com.sun.max.vm.t1x.T1XTemplateGenerator.*;

import java.lang.reflect.*;

import com.oracle.max.vm.ext.vma.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.t1x.*;

@HOSTED_ONLY
public class SyncLogVMAdviceHandlerGenerator {
    public static void main(String[] args) {
        T1XTemplateGenerator.setGeneratingClass(SyncLogVMAdviceHandlerGenerator.class);
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
    }

    private static void generate(Method m) {
        generateAutoComment();
        out.printf("    @Override%n");
        int argCount = generateSignature(m, null);
        out.printf(" {%n");
        if (m.getName().contains("MultiNewArray")) {
            out.printf("        adviseAfterNewArray(arg1, arg2[0]);%n");
        } else {
            out.printf("        super.%s(", m.getName());
            generateInvokeArgs(argCount);
            out.printf("        logHandler.%s(", m.getName());
            generateInvokeArgs(argCount);
            if (m.getName().contains("NewArray")) {
                out.printf("        MultiNewArrayHelper.handleMultiArray(this, arg1);%n");
            }
        }
        out.printf("    }%n%n");
    }


}
