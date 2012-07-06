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

    public static final String INDENT4 = "    ";
    public static final String INDENT8 = INDENT4 + "    ";
    public static final String INDENT12 = INDENT8 + "    ";

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

    /**
     * Default generator that assumes {@code com.oracle.max.vm.ext.vma} project.
     */
    public static int updateSource(Class target, String updatedContent, boolean checkOnly) throws IOException {
        return updateSource("com.oracle.max.vm.ext.vma", target, updatedContent, "// START GENERATED CODE", "// END GENERATED CODE", checkOnly);
    }

    /**
     * Generator with variable project.
     */
    public static int updateSource(String project, Class target, String updatedContent, boolean checkOnly) throws IOException {
        return updateSource(project, target, updatedContent, "// START GENERATED CODE", "// END GENERATED CODE", checkOnly);
    }

    public static int updateSource(Class target, String updatedContent, String startString, String endString, boolean checkOnly) throws IOException {
        return updateSource("com.oracle.max.vm.ext.vma", target, updatedContent, startString, endString, checkOnly);
    }

    private static int updateSource(String project, Class target, String updatedContent, String startString, String endString, boolean checkOnly) throws IOException {
        File base = new File(JavaProject.findWorkspace(), project + File.separator + "src");
        File outputFile = new File(base, target.getName().replace('.', File.separatorChar) + ".java").getAbsoluteFile();
        if (updatedContent == null) {
            updatedContent = bsOut.toString();
        }
        ReadableSource content = ReadableSource.Static.fromString(updatedContent);
        if (Files.updateGeneratedContent(outputFile, content,
                        startString, endString, checkOnly)) {
            System.out.println("Source for " + target + " was updated");
            return 1;
        }
        return 0;

    }

    public static class MethodNameOverride {
        public final Method method;
        public MethodNameOverride(Method m) {
            this.method = m;
        }
        public String overrideName() {
            return method.getName();
        }
    }

    public static class ArgumentsPrefix {
        /**
         * Handle for adding prefix argument to the signature.
         * Return the number added.
         */
        public int prefixArguments() {
            return  0;
        }
    }

    /**
     * Generates the name/signature definition for given method, returning a count of the
     * number of arguments.
     * @param protection public/private etc or null for nothing
     * @param m
     * @param modifiers extra modifiers
     * @param prefix TODO
     */
    public static int generateSignature(String indent, String protection, MethodNameOverride m, String modifiers, ArgumentsPrefix prefixArgs) {
        String methodName = m.overrideName();
        out.print(indent);
        if (protection != null) {
            out.printf("%s ", protection);
        }
        if (modifiers != null) {
            out.printf("%s ", modifiers);
        }
        out.printf("void %s(", methodName);
        int prefixArgCount = prefixArgs == null ? 0 : prefixArgs.prefixArguments();
        int count = 1 + prefixArgCount;
        for (Class<?> klass : m.method.getParameterTypes()) {
            if (count != 1) {
                out.print(", ");
            }
            out.printf("%s %s", klass.getSimpleName(), "arg" + count);
            count++;
        }
        out.print(")");
        return count - 1;
    }

    public static int generateSignature(Method m, String modifiers) {
        return generateSignature(INDENT4, "public", new MethodNameOverride(m), modifiers, null);
    }

    public static void generateInvokeArgs(int argCount) {
        generateInvokeArgs(argCount, 1);
    }
    /**
     * Generate the args for a method invocation and closing bracket and newline.
     * @param argCount
     */
    public static void generateInvokeArgs(int argCount, int start) {
        for (int count = start; count <= argCount; count++) {
            if (count != start) {
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

    public static String toFirstUpper(String s) {
        if (s.length() == 0) {
            return s;
        } else {
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        }
    }


}
