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

import java.lang.reflect.*;

import com.oracle.max.vm.ext.graal.vma.snippets.*;
import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vma.tools.gen.vma.*;


public class VMASnippetsGenerator {

    public static class SnippetsMethodNameOverride extends MethodNameOverride{
        public SnippetsMethodNameOverride(Method m) {
            super(m);
        }

        @Override
        public String overrideName() {
            String baseName = method.getName();
            if (baseName.contains("PutStatic") || baseName.contains("PutField") || baseName.contains("Return") ||
                            baseName.contains("ArrayStore") || baseName.equals("adviseBeforeIf")) {
                Class[] params = method.getParameterTypes();
                if (baseName.equals("adviseBeforeIf")) {
                    Class cmpClass = params[2];
                    baseName += AdviceGeneratorHelper.toFirstUpper(cmpClass.getSimpleName());
                } else {
                    if (params.length > 1) {
                        Class valueClass = params[params.length - 1];
                        baseName += AdviceGeneratorHelper.toFirstUpper(valueClass.getSimpleName());
                    }
                }
            }
            return baseName + "Snippet";
        }
    }

    public static void main(String[] args) throws Exception {
        createGenerator(VMASnippetsGenerator.class);
        generateAutoComment();

        for (Method m : BytecodeAdvice.class.getDeclaredMethods()) {
            if (m.getName().contains("MultiNewArray")) {
                // custom
                continue;
            }
            generateStatic(m);
        }
        AdviceGeneratorHelper.updateSource(AdviceSnippets.class, null, false);

    }

    private static void generateStatic(Method m) {
        out.printf("    @Snippet(inlining = MaxSnippetInliningPolicy.class)%n");
        int argCount = generateSignature(INDENT4, "private", new SnippetsMethodNameOverride(m), "static", null);
        out.printf(" {%n");
        out.printf("        VMAStaticBytecodeAdvice.%s(", m.getName());
        generateInvokeArgs(argCount);
        out.printf("    }%n%n");
    }

}
