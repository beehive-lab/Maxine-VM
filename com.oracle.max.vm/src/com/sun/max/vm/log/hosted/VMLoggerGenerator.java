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
package com.sun.max.vm.log.hosted;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.log.*;
import com.sun.max.vm.thread.*;

/**
 * Checks/Generates the boilerplate of a {@link VMLogger} implementation from
 * an interface specified by the developer.
 */
@HOSTED_ONLY
public class VMLoggerGenerator {

    private static final String INDENT4 = "    ";
    private static final String INDENT8 = INDENT4 + INDENT4;
    private static final String INDENT12 = INDENT8 + INDENT4;
    private static final String INDENT16 = INDENT12 + INDENT4;
    private static final String INDENT20 = INDENT16 + INDENT4;

    private static final String[] INDENTS = new String[] {"", INDENT4, INDENT8, INDENT12, INDENT16, INDENT20};

    private static boolean generate(boolean checkOnly, Class source, ArrayList<Class<?>> loggerInterfaces) throws Exception {
        File base = new File(JavaProject.findHgRoot(), "com.oracle.max.vm/src");
        File outputFile = new File(base, source.getName().replace('.', File.separatorChar) + ".java").getAbsoluteFile();

        Writer writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        for (Class loggerInterface : loggerInterfaces) {
            VMLoggerInterface vmLoggerInterface = getVMLoggerInterface(loggerInterface);
            String autoName = getRootName(loggerInterface) + "Auto";
            Method[] methods = loggerInterface.getDeclaredMethods();
            Map<String, Integer> refMap = computeRefMaps(methods);
            out.format("%sprivate static abstract class %s extends %s {%n", INDENT4, autoName, sanitizedName(vmLoggerInterface.parent().getName()));
            Map<String, Integer> enumMap = outOperationEnum(out, methods);
            Set<String> refTypes = new HashSet<String>();

            out.printf("%sprivate static final int[] REFMAPS = %s;%n%n", INDENT8, refMapArray(refMap));
            out.printf("%sprotected %s(String name, String optionDescription) {%n", INDENT8, autoName);
            out.printf("%ssuper(name, Operation.VALUES.length, optionDescription, REFMAPS);%n", INDENT12);
            out.printf("%s}%n%n", INDENT8);
            if (vmLoggerInterface.defaultConstructor()) {
                out.printf("%sprotected %s() {%n", INDENT8, autoName);
                out.printf("%s}%n%n", INDENT8);
            }
            out.printf("%s@Override%n", INDENT8);
            out.printf("%spublic String operationName(int opCode) {%n", INDENT8);
            out.printf("%sreturn Operation.VALUES[opCode].name();%n", INDENT12);
            out.printf("%s}%n%n", INDENT8);

            // where we store the string for the case body of the trace method
            ArrayList<String> traceCaseBodies = new ArrayList<String>(methods.length);

            for (Method method : methods) {
                String uName = operationName(method);
                if (isOverride(method, vmLoggerInterface)) {
                    out.printf("%s@Override%n", INDENT8);
                }
                out.printf("%s@INLINE%n", INDENT8);
                out.printf("%spublic final void log%s(", INDENT8, uName);
                Class<?>[] parameters = method.getParameterTypes();
                Annotation[][] parameterAnnotations = method.getParameterAnnotations();
                String[] formalNames = new String[parameters.length + 1];

                int argIndex = 1;
                for (Class< ? > parameter : parameters) {
                    Annotation[] paramAnnotations = parameterAnnotations[argIndex - 1];
                    if (argIndex > 1) {
                        out.print(",");
                        if (argIndex % 6 == 0) {
                            out.printf("%n%s", INDENT16);
                        } else {
                            out.print(' ');
                        }
                    }
                    formalNames[argIndex] = formalName(paramAnnotations, argIndex);
                    out.printf("%s %s", parameter.getSimpleName(), formalNames[argIndex]);
                    argIndex++;
                }
                out.print(") {\n");

                // generate call to VMLogger.log method with argument typing
                // generate case body for trace method at same time
                out.printf("%slog(Operation.%s.ordinal()", INDENT12, uName);

                argIndex = 1;
                for (Class< ? > parameter : parameters) {
                    String arg = formalNames[argIndex];
                    if (Word.class.isAssignableFrom(parameter)) {
                        // nothing needed
                    } else if (Hub.class.isAssignableFrom(parameter)) {
                        arg = "classActorArg(" + arg + ".classActor)";
                    } else {
                        arg = wrapLogArg(source, parameter, arg);
                    }
                    out.print(',');
                    if (argIndex % 6 == 0) {
                        out.printf("%n%s", INDENT16);
                    } else {
                        out.print(' ');
                    }
                    out.print(arg);
                    argIndex++;
                }
                out.print(");\n");
                out.printf("%s}%n", INDENT8);

                // trace call in case body
                int indx = 4;
                StringBuilder caseBody = new StringBuilder(INDENTS[indx]).append("case ");
                caseBody.append(enumMap.get(uName)).append(':').append(" { //").append(uName).append('\n');
                caseBody.append(INDENTS[indx + 1]).append("trace").append(uName).append('(');
                argIndex = 1;
                for (Class< ? > parameter : parameters) {
                    if (argIndex > 1) {
                        caseBody.append(", ");
                    }
                    caseBody.append(wrapTraceArg(refTypes, source, parameter, argIndex));
                    argIndex++;
                }
                caseBody.append(");\n");
                caseBody.append(INDENTS[indx + 1]).append("break;\n");
                caseBody.append(INDENTS[indx]).append("}\n");
                traceCaseBodies.add(caseBody.toString());

                // trace method
                out.printf("%sprotected abstract void trace%s(", INDENT8, uName);
                argIndex = 1;
                for (Class< ? > parameter : parameters) {
                    Annotation[] paramAnnotations = parameterAnnotations[argIndex - 1];
                    if (argIndex > 1) {
                        out.print(",");
                        if (argIndex % 6 == 0) {
                            out.printf("%n%s", INDENT16);
                        } else {
                            out.print(' ');
                        }
                    }
                    formalNames[argIndex] = formalName(paramAnnotations, argIndex);
                    out.printf("%s %s", parameter.getSimpleName(), formalNames[argIndex]);
                    argIndex++;
                }
                out.print(");\n\n");
            }
            outTraceMethod(out, traceCaseBodies);

            // casts
            for (String type : refTypes) {
                out.printf("%sstatic %s to%s(Record r, int argNum) {%n", INDENT8, type, type);
                out.printf("%sreturn as%s(toObject(r, argNum));%n", INDENT12, type);
                out.printf("%s}%n", INDENT8);
                out.printf("%s@INTRINSIC(UNSAFE_CAST)%n", INDENT8);
                out.printf("%sprivate static native %s as%s(Object arg);%n", INDENT8, type, type);
            }

            out.printf("%s}%n%n", INDENT4);


        }
        writer.close();
        boolean wouldUpdate = Files.updateGeneratedContent(outputFile, ReadableSource.Static.fromString(writer.toString()),
                        "// START GENERATED CODE", "// END GENERATED CODE", checkOnly);
        if (checkOnly && wouldUpdate) {
            System.out.println("NEW GENERATED CODE for " + source);
            System.out.println(writer.toString());
        }
        return wouldUpdate;
    }

    private static Map<String, Integer> computeRefMaps(Method[] methods) {
        Map<String, Integer> refMap = new HashMap<String, Integer>();
        for (Method method : methods) {
            int argIndex = 0;
            int refMapBits = 0;
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (Class<?> type : parameterTypes) {
                if (Word.class.isAssignableFrom(type)) {
                    // not a reference type
                } else if (Actor.class.isAssignableFrom(type) || VmThread.class.isAssignableFrom(type)) {
                    // scalar id alternative
                } else if (Object.class.isAssignableFrom(type)) {
                    refMapBits |= 1 << argIndex;
                }
                argIndex++;
            }
            refMap.put(operationName(method), refMapBits);
        }
        return refMap;
    }

    private static String sanitizedName(String name) {
        int ix = name.indexOf('$');
        if (ix < 0) {
            return name;
        }
        return name.replace("$", ".");
    }

    private static boolean isOverride(Method method, VMLoggerInterface vmLoggerInterface) {
        Class parent = vmLoggerInterface.parent();
        if (parent == VMLogger.class) {
            return false;
        }
        // look in parent for method that matches method name
        String logName = "log" + operationName(method);
        for (Method parentMethod : parent.getDeclaredMethods()) {
            if (parentMethod.getName().equals(logName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isStandardArgMethod(String name, Class sourceClass) {
        // look in VMLogger first
        if (isStandardArgMethodX(name, VMLogger.class)) {
            return true;
        }
        if (isStandardArgMethodX(name, sourceClass)) {
            return true;
        }
        return false;
    }

    private static boolean isStandardArgMethodX(String name, Class klass) {
        for (Method m : klass.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static String operationName(Method method) {
        return toFirstUpper(method.getName());
    }

    private static String refMapArray(Map<String, Integer> refMap) {
        boolean zero = true;
        for (int x : refMap.values()) {
            if (x != 0) {
                zero = false;
                break;
            }
        }
        if (zero) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("new int[] {");
        boolean first = true;
        for (int bits : refMap.values()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append("0x").append(Integer.toHexString(bits));
        }
        sb.append("}");
        return sb.toString();
    }

    private static Map<String, Integer> outOperationEnum(PrintWriter out, Method[] methods) {
        Map<String, Integer> enumMap = new HashMap<String, Integer>();
        out.format("%spublic enum Operation {%n", INDENT8);
        boolean first = true;
        out.print(INDENT12);
        int count = 0;
        for (Method method : methods) {
            if (!first) {
                out.print(",");
                if ((count + 1) % 4 == 0) {
                    out.printf("%n%s", INDENT12);
                } else {
                    out.print(' ');
                }
            } else {
                first = false;
            }
            String enumName = operationName(method);
            out.print(enumName);
            enumMap.put(enumName, count);
            count++;
        }
        out.println(";\n");
        out.printf("%spublic static final Operation[] VALUES = values();%n%s}%n%n", INDENT12, INDENT8);
        return enumMap;
    }

    private static void outTraceMethod(PrintWriter out, ArrayList<String> caseBodies) {
        out.printf("%s@Override%n", INDENT8);
        out.printf("%sprotected void trace(Record r) {%n", INDENT8);
//        out.printf("%sOperation op = Operation.VALUES[r.getOperation()];%n", INDENT12);
        out.printf("%sswitch (r.getOperation()) {%n", INDENT12);
        for (String caseBody : caseBodies) {
            out.print(caseBody);
        }
        out.printf("%s}%n", INDENT12);
        out.printf("%s}%n", INDENT8);
    }

    private static String formalName(Annotation[] paramAnnotations, int formalIndex) {
        VMLogParam paramAnnotation = null;
        for (Annotation annotation : paramAnnotations) {
            if (annotation instanceof VMLogParam) {
                paramAnnotation = (VMLogParam) annotation;
                break;
            }
        }
        if (paramAnnotation == null) {
            return "arg" + formalIndex;
        } else {
            return paramAnnotation.name();
        }
    }

    private static String wrapLogArg(Class sourceClass, Class klass, String argName) {
        String methodName = logArgMethodName(toFirstLower(klass.getSimpleName()));
        if (isStandardArgMethod(methodName, sourceClass)) {
            return wrapLogArg(methodName, argName);
        }
        // default to Object
        return wrapLogArg(logArgMethodName("object"), argName);
    }

    private static String logArgMethodName(String type) {
        return type + "Arg";
    }

    private static String traceArgMethodName(String type) {
        return "to" + type;
    }

    private static String wrapLogArg(String methodName, String name) {
        return methodName + "(" + name + ")";
    }

    private static String wrapTraceArg(Set<String> refTypes, Class sourceClass, Class klass, int argNum) {
        String type = toFirstUpper(klass.getSimpleName());
        String methodName = traceArgMethodName(type);
        if (isStandardArgMethod(methodName, sourceClass)) {
            return wrapTraceArg(methodName, argNum);
        } else {
            refTypes.add(type);
            return wrapTraceArg(methodName, argNum);
        }
    }

    private static String wrapTraceArg(String kind, int argNum) {
        return kind + "(r, " + argNum + ")";
    }

    public static String toFirstUpper(String s) {
        if (s.length() == 0) {
            return s;
        } else {
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        }
    }

    public static String toFirstLower(String s) {
        if (s.length() == 0) {
            return s;
        } else {
            return s.substring(0, 1).toLowerCase() + s.substring(1);
        }
    }

    private static String getRootName(Class loggerInterface) throws Exception {
        String rootName = loggerInterface.getSimpleName();
        int ix = rootName.indexOf("Interface");
        if (ix <= 0) {
            throw new Exception("Logger class " + rootName + " is not named XXXInterface");
        }
        return rootName.substring(0, ix);
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        boolean checkOnlyArg = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-check")) {
                checkOnlyArg = true;
            }
        }
        final boolean checkOnly = checkOnlyArg;
        new ClassSearch() {
            final HashSet<String> seenPackages = new HashSet<String>();
            @Override
            protected boolean visitClass(boolean isArchiveEntry, String className) {
                if (!className.endsWith("package-info")) {
                    String pkg = Classes.getPackageName(className);
                    if (seenPackages.add(pkg)) {
                        Trace.line(1, pkg);
                    }
                    Class c = null;
                    try {
                        c = Classes.forName(className, false, getClass().getClassLoader());
                    } catch (Throwable ex) {
                        // Ignore
                        System.out.println("WARNING: could not load class for " + className);
                        return true;
                    }
                    ArrayList<Class<?>> loggerInterfaces = findLoggerInterfaces(c);
                    if (loggerInterfaces.size() > 0) {
                        try {
                            boolean updated = generate(checkOnly, c, loggerInterfaces);
                            if (updated) {
                                System.out.println("Source for " + c + " was updated");
                            }
                        } catch (Exception ex) {
                            System.err.println(ex);
                            System.exit(1);
                        }
                    }
                }
                return true;
            }
        }.run(Classpath.fromSystem(), "com/sun/max");
    }

    private static ArrayList<Class< ? >> findLoggerInterfaces(Class< ? > klass) {
        ArrayList<Class< ? >> result = new ArrayList<Class< ? >>();
        Class< ? >[] declaredClasses = klass.getDeclaredClasses();
        for (Class< ? > innerClass : declaredClasses) {
            if (getVMLoggerInterface(innerClass) != null) {
                result.add(innerClass);
            }
        }
        return result;
    }

    private static VMLoggerInterface getVMLoggerInterface(Class klass) {
        Annotation[] annotations = klass.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof VMLoggerInterface) {
                return (VMLoggerInterface) annotation;
            }
        }
        return null;
    }

}
