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
package com.sun.max.vm.compiler.deps;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.deps.DependencyProcessor.*;


public class ToStringDependencyVisitorGenerator {
    private static final String INDENT4 = "    ";
    private static final String INDENT8 = INDENT4 + "    ";
    private static final String INDENT12 = INDENT8 + "    ";
    private static final String INDENT16 = INDENT12 + "    ";
    private static final String GENERATED_CLASS_NAME = "AllToStringDependencyVisitor";

    public static void main(String[] args) throws Exception {
        boolean checkOnly = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-check")) {
                checkOnly = true;
            }
        }
        boolean updatedSource = new ToStringDependencyVisitorGenerator().generate(checkOnly);
        if (updatedSource) {
            System.out.println("Source for " + GENERATED_CLASS_NAME + (checkOnly ? "would be" : "was") + " updated");
        }
        if (updatedSource) {
            System.exit(1);
        }
    }

    boolean generate(boolean checkOnly) throws IOException {
        ToStringVisitorClassSearch search = new ToStringVisitorClassSearch();
        search.run(Classpath.fromSystem(), "com/sun/max");
        ToStringVisitorClassInfo[] infoArray = new ToStringVisitorClassInfo[search.toStringVisitorClasses.size()];
        search.toStringVisitorClasses.toArray(infoArray);
        Arrays.sort(infoArray);

        File base = new File(JavaProject.findWorkspace(), "com.oracle.max.vm/src");
        String sourceClass = this.getClass().getPackage().getName() + "." + GENERATED_CLASS_NAME;
        File outputFile = new File(base, sourceClass.replace('.', File.separatorChar) + ".java").getAbsoluteFile();
        Writer writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        for (ToStringVisitorClassInfo info : infoArray) {
            if (info.processorClass.getPackage() != this.getClass().getPackage()) {
                out.printf("import %s;%n", info.processorClass.getName());
            }
            out.printf("import %s.*;%n", info.processorClass.getName());
        }
        out.printf("\nclass %s extends DependencyVisitor implements\n", GENERATED_CLASS_NAME);
        boolean first = true;
        for (ToStringVisitorClassInfo info : search.toStringVisitorClasses) {
            if (first) {
                first = false;
            } else {
                out.printf(",%n");
            }
            out.printf("%s%s", INDENT8, info.visitorClassInterface.getSimpleName());
        }
        out.println(" {\n");
        out.printf("%s%s(StringBuilder sb) {%n", INDENT4, GENERATED_CLASS_NAME);
        for (ToStringVisitorClassInfo info : infoArray) {
            out.printf("%s%s.toString%s.setStringBuilder(sb);%n", INDENT8, info.processorClass.getSimpleName(), info.visitorClassInterface.getSimpleName());
        }
        out.println(INDENT4 + "}\n");

        for (ToStringVisitorClassInfo info : infoArray) {
            Method method = info.visitorClassInterface.getDeclaredMethods()[0];
            Class<?>[] params = method.getParameterTypes();
            out.printf("%spublic boolean %s(", INDENT4, method.getName());
            first = true;
            for (int i = 0; i < params.length; i++) {
                Class<?> param = params[i];
                if (first) {
                    first = false;
                } else {
                    out.print(", ");
                }
                out.printf("%s %s", param.getSimpleName(), "arg" + i);
            }
            out.printf(") {%n");
            out.printf("%sreturn %s.toString%s.%s(", INDENT8, info.processorClass.getSimpleName(), info.visitorClassInterface.getSimpleName(), method.getName());
            first = true;
            for (int i = 0; i < params.length; i++) {
                if (first) {
                    first = false;
                } else {
                    out.print(", ");
                }
                out.print("arg" + i);
            }
            out.printf(");%n%s}%n", INDENT4);
        }
        out.println("}");
        writer.close();
        boolean wouldUpdate = Files.updateGeneratedContent(outputFile, ReadableSource.Static.fromString(writer.toString()),
                        "// START GENERATED CODE", "// END GENERATED CODE", checkOnly);
        return wouldUpdate;
    }

    private static Class< ? > findToStringVisitorClass(Class< ? > klass) {
        Class< ? >[] declaredClasses = klass.getDeclaredClasses();
        for (Class< ? > innerClass : declaredClasses) {
            if (ToStringDependencyProcessorVisitor.class.isAssignableFrom(innerClass)) {
                return innerClass;
            }
        }
        return null;
    }

    private static class ToStringVisitorClassInfo implements Comparable {
        Class<?> processorClass;
        Class<?> toStringisitorClass;
        Class<?> visitorClassInterface;

        @Override
        public int compareTo(Object arg0) {
            ToStringVisitorClassInfo otherInfo = (ToStringVisitorClassInfo) arg0;
            return processorClass.getName().compareTo(otherInfo.processorClass.getName());
        }
    }

    private static class ToStringVisitorClassSearch extends ClassSearch {
        final ArrayList<ToStringVisitorClassInfo> toStringVisitorClasses = new ArrayList<ToStringVisitorClassInfo>();
        final HashSet<Class<?>> pClasses = new HashSet<Class<?>>();

        @Override
        protected boolean visitClass(boolean isArchiveEntry, String className) {
            if (!className.endsWith("package-info")) {
                Class<?> source = null;
                try {
                    source = Classes.forName(className, false, getClass().getClassLoader());
                } catch (Throwable ex) {
                    // Ignore
                    System.out.println("WARNING: could not load class for " + className);
                    return true;
                }
                if (source != DependencyProcessor.class && DependencyProcessor.class.isAssignableFrom(source)) {
                    // TODO this is a workaround for multiple calls when hosted in the Inspector; should figure out why.
                    if (pClasses.contains(source)) {
                        return true;
                    }
                    pClasses.add(source);
                    Class< ? > toStringVisitorClass = findToStringVisitorClass(source);
                    if (toStringVisitorClass != null) {
                        ToStringVisitorClassInfo info = new ToStringVisitorClassInfo();
                        info.processorClass = source;
                        info.toStringisitorClass = toStringVisitorClass;
                        info.visitorClassInterface = toStringVisitorClass.getInterfaces()[0];
                        toStringVisitorClasses.add(info);
                    } else {
                        ProgramError.unexpected(className + " does not define a ToStringDependencyProcessorVisitor");
                    }
                }
            }
            return true;
        }

    }

}
