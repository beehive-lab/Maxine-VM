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
package com.oracle.max.vma.tools.gen.vma;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.oracle.max.vm.ext.t1x.*;
import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vma.tools.gen.t1x.*;
import com.sun.max.annotate.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;

/**
 * Helper methods for auto-generation of code related to the advice interface.
 * We use {@link T1XTemplateGenerator} for basic support methods.
 */

@HOSTED_ONLY
public class AdviceGeneratorHelper {

    public static final VMABytecodes[] VMABytecodeValues = VMABytecodes.values();
    /**
     * Map from actual bytecode encoding value to {@link VMABytecode}.
     */
    public static Map<Integer, VMABytecodes> codeMap = new HashMap<Integer, VMABytecodes>();

    static {
        for (VMABytecodes bc : VMABytecodeValues) {
            codeMap.put(bc.code, bc);
        }
    }

    public static VMAdviceTemplateGenerator t1xTemplateGen;
    private static String generatingClassName;

    public static ByteArrayOutputStream bsOut;
    public static PrintStream out = System.out;

    public static void generateAutoComment() {
        out.printf("// EDIT AND RUN %s.main() TO MODIFY%n%n", generatingClassName);
    }

    public static void createGenerator(Class<?> klass) {
        generatingClassName = klass.getSimpleName();
        bsOut = new ByteArrayOutputStream();
        out = new PrintStream(bsOut);
        t1xTemplateGen = new VMAdviceTemplateGenerator(out);

    }

    public static int updateSource(Class target, String updatedContent, boolean checkOnly) throws IOException {
        File base = new File(JavaProject.findHgRoot(), "com.oracle.max.vm.ext.vma/src");
        File outputFile = new File(base, target.getName().replace('.', File.separatorChar) + ".java").getAbsoluteFile();
        if (updatedContent == null) {
            updatedContent = bsOut.toString();
        }
        ReadableSource content = ReadableSource.Static.fromString(updatedContent);
        if (Files.updateGeneratedContent(outputFile, content,
                        "// START GENERATED CODE", "// END GENERATED CODE", checkOnly)) {
            System.out.println("Source for " + target + " was updated");
            return 1;
        }
        return 0;

    }
    /**
     * Generates the name/signature definition for given method, returning a count of the
     * number of arguments.
     * @param m
     * @param modifiers TODO
     * @return
     */
    public static int generateSignature(Method m, String modifiers) {
        out.print("    public ");
        if (modifiers != null) {
            out.printf("%s ", modifiers);
        }
        out.printf("void %s(", m.getName());
        int count = 1;
        for (Class<?> klass : m.getParameterTypes()) {
            if (count != 1) {
                out.print(", ");
            }
            out.printf("%s %s", klass.getSimpleName(), "arg" + count);
            count++;
        }
        out.print(")");
        return count;
    }

    /**
     * Generate the args for a method invocation and closing bracket and newline.
     * @param argCount
     */
    public static void generateInvokeArgs(int argCount) {
        for (int count = 1; count < argCount; count++) {
            if (count != 1) {
                out.print(", ");
            }
            out.printf("%s", "arg" + count);
        }
        out.printf(");%n");
    }

    public static String getLastParameterName(Method m) {
        Class<?>[] types = m.getParameterTypes();
        if (types.length == 0) {
            return null;
        }
        return types[types.length - 1].getSimpleName();
    }

    public static String getNthParameterName(Method m, int argc) {
        Class<?>[] types = m.getParameterTypes();
        if (types.length == 0 || argc > types.length) {
            return null;
        }
        return types[argc - 1].getSimpleName();

    }


}
