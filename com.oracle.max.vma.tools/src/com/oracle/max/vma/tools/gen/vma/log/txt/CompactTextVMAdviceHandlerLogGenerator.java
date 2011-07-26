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
package com.oracle.max.vma.tools.gen.vma.log.txt;

import static com.sun.max.vm.t1x.T1XTemplateGenerator.*;
import static com.oracle.max.vma.tools.gen.vma.AdviceGeneratorHelper.*;

import java.lang.reflect.*;

import com.oracle.max.vm.ext.vma.log.*;

public class CompactTextVMAdviceHandlerLogGenerator {

    public static void main(String[] args) {
        createGenerator(CompactTextVMAdviceHandlerLogGenerator.class);
        for (Method m : VMAdviceHandlerLog.class.getMethods()) {
            if (m.getName().startsWith("advise")) {
                generate(m);
            }
        }
    }

    private static void generate(Method m) {
        final String name = m.getName();
        generateAutoComment();
        out.printf("    @Override%n");
        generateSignature(m, null);
        out.printf(" {%n");
        if (name.equals("adviseAfterMultiNewArray")) {
            out.printf("        ProgramError.unexpected(\"adviseAfterMultiNewArray\");%n");
            out.printf("    }%n%n");
            return;
        }
        out.printf("        del.%s(", m.getName());
        out.print("getThreadShortForm(arg1)");
        if (name.contains("GetField") || name.contains("PutField")) {
            out.print(", checkRepeatId(arg2, arg1), getClassShortForm(arg3, arg4), arg4, getFieldShortForm(arg3, arg4, arg5)");
            if (name.contains("PutField")) {
                out.print(", arg6");
            }
            out.printf(");%n");
        } else if (name.contains("GetStatic") || name.contains("PutStatic")) {
            out.print(", getClassShortForm(arg2, arg3), arg3, getFieldShortForm(arg2, arg3, arg4)");
            if (name.contains("PutStatic")) {
                out.print(", arg5");
            }
            out.printf(");%n");
        } else if (name.contains("ArrayLoad") || name.contains("ArrayStore")) {
            out.print(",  checkRepeatId(arg2, arg1), arg3");
            if (name.contains("ArrayStore")) {
                out.print(", arg4");
            }
            out.printf(");%n");
        } else if (name.contains("New")) {
            out.print(", checkRepeatId(arg2, arg1), getClassShortForm(arg3, arg4), arg4");
            if (name.contains("NewArray")) {
                out.print(", arg5");
            }
            out.printf(");%n");
        } else if (name.contains("Invoke") || name.contains("MethodEntry")) {
            out.print(", checkRepeatId(arg2, arg1), getClassShortForm(arg3, arg4), arg4, getMethodShortForm(arg3, arg4, arg5)");
            out.printf(");%n");
        } else if (name.contains("Monitor") || name.contains("Throw")) {
            out.print(", checkRepeatId(arg2, arg1)");
            out.printf(");%n");
        } else if (name.contains("CheckCast") || name.contains("InstanceOf")) {
            out.print(", checkRepeatId(arg2, arg1), getClassShortForm(arg3, arg4), arg4");
            out.printf(");%n");
        } else {
            Class<?>[] params = m.getParameterTypes();
            for (int argc = 2; argc <= params.length; argc++) {
                out.printf(", %s", "arg" + argc);
            }
            out.printf(");%n");
        }
        out.printf("    }%n%n");
    }

}
