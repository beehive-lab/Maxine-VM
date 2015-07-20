/*
 * Copyright (c) 2009, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.jni;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.sun.max.*;
import com.sun.max.annotate.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.program.*;

/**
 * This class implements the {@linkplain #generate process} by which the source in {@link JniFunctionsSource JniFunctionsSource.java}
 * and {@link JmmFunctionsSource JmmFunctionsSource.java} is pre-processed to produce source in
 * {@link JniFunctions JniFunctions.java} and {@link JmmFunctions JmmFunctions.java}.
 * The generated source is delineated by the following lines:
 * <pre>
 * // START GENERATED CODE
 *
 * ...
 *
 * // END GENERATED CODE
 * </pre>
 */
@HOSTED_ONLY
public class JniFunctionsGenerator {
    /**
     * Flag that controls if timing / counting code is inserted into the generated JNI functions.
     */
    private static final boolean TIME_JNI_FUNCTIONS = false;

    private static final String VM_ENTRY_POINT_ANNOTATION = "@" + VM_ENTRY_POINT.class.getSimpleName();
    static final int BEFORE_FIRST_VM_ENTRY_POINT_FUNCTION = -1;
    static final int BEFORE_VM_ENTRY_POINT_FUNCTION = 0;
    static final int BEFORE_PROLOGUE = 1;
    static ArrayList<String> logOperations;
    public static VmEntryFunctionDeclaration currentMethod;

    /**
     * An extension of {@link BufferedReader} that tracks the line number.
     */
    static class LineReader extends BufferedReader {
        int lineNo;
        String line;
        final File file;
        public LineReader(File file) throws FileNotFoundException {
            super(new FileReader(file));
            this.file = file;
        }

        @Override
        public String readLine() throws IOException {
            lineNo++;
            line = super.readLine();
            return line;
        }

        public String where() {
            return file.getAbsolutePath() + ":" + lineNo;
        }

        public void check(boolean condition, String errorMessage) {
            if (!condition) {
                throw new InternalError(String.format("%n" + where() + ": " + errorMessage + "%n" + line));
            }
        }
    }

    public static class VmEntryFunctionDeclaration {
        static Pattern PATTERN = Pattern.compile("    private static (native )?(\\w+(?:\\[\\])*) (\\w+)\\(([^)]*)\\).*");

        String line;
        String returnType;
        String jniReturnType;
        boolean isNative;
        public String name;
        public String parameters;
        public String jniParameters;
        public String arguments;
        String sourcePos;

        static VmEntryFunctionDeclaration parse(String line, String sourcePos) {
            Matcher m = PATTERN.matcher(line);
            if (!m.matches()) {
                return null;
            }

            VmEntryFunctionDeclaration decl = new VmEntryFunctionDeclaration();
            decl.line = line;
            decl.isNative = m.group(1) != null;
            decl.returnType = m.group(2);
            decl.name = m.group(3);
            decl.parameters = m.group(4);

            String[] parameters = decl.parameters.split(",\\s*");
            StringBuilder jniParameters = new StringBuilder();
            StringBuilder arguments = new StringBuilder();
            for (int i = 0; i < parameters.length; ++i) {
                String parameter = parameters[i];
                if (parameter.length() != 0) {
                    if (arguments.length() != 0) {
                        arguments.append(", ");
                        jniParameters.append(", ");
                    }
                    String[] typeAndName = parameter.split("\\s+");
                    assert typeAndName.length == 2 : line;
                    jniParameters.append(toJniType(typeAndName[0])).append(' ').append(typeAndName[1]);
                    arguments.append(parameter.substring(parameter.lastIndexOf(' ') + 1));
                }
            }
            decl.arguments = arguments.toString();
            decl.sourcePos = sourcePos;
            decl.jniParameters = jniParameters.toString(); decl.jniReturnType = toJniType(decl.returnType);
            return decl;
        }

        static Map<String, String> jniTypes = Utils.addEntries(new HashMap<String, String>(),
                        "boolean", "jboolean",
                        "byte", "jbyte",
                        "char", "jchar",
                        "short", "jshort",
                        "int", "jint",
                        "long", "jlong",
                        "float", "jfloat",
                        "double", "jdouble",
                        "void", "void",
                        "MethodID", "jmethodID",
                        "FieldID", "jfieldID",
                        "JniHandle", "jobject",
                        "Pointer", "void*",
                        "Word", "void*",
                        "Address", "void*",
                        "Size", "size_t",
                        "Offset", "off_t");

        static String toJniType(String type) {
            String jniType = jniTypes.get(type);
            assert jniType != null : "Type cannot present in a native interface signature: " + type;
            return jniType;
        }

        public String declareHelper() {
            int index = line.indexOf('(');
            return line.substring(0, index) + '_' + line.substring(index);
        }

        public String callHelper() {
            return name + "_(" + arguments + ")";
        }
    }

    public abstract static class Customizer {
        public String customizeBody(String line) {
            return line;
        }

        public void startFunction(VmEntryFunctionDeclaration decl) {
            currentMethod = decl;
            logOperations.add(decl.name);
        }

        public void close(PrintWriter writer) throws Exception {
            writer.println("    public static enum LogOperations {");
            for (int i = 0; i < logOperations.size(); i++) {
                String methodName = logOperations.get(i);
                if (i > 0) {
                    writer.println(",");
                }
                writer.printf("        /* %d */ %s", i, methodName);
            }
            customizeOperations(writer);
            writer.println(";\n");
            writer.println("    }");
        }

        public String customizeHandler(String returnStatement) {
            String result = "            VmThread.fromJniEnv(env).setJniException(t);";
            if (returnStatement != null) {
                result += "\n            " + returnStatement;
            }
            return result;
        }

        public void customizeOperations(PrintWriter writer) {
            // op for user downcalls/invoke
            writer.printf(",\n        // operation for logging native method down call\n");
            writer.printf("        /* %d */ %s", logOperations.size(), "NativeMethodCall");
            writer.printf(",\n        // operation for logging reflective invocation\n");
            writer.printf("        /* %d */ %s", logOperations.size() + 1, "ReflectiveInvocation");
            writer.printf(",\n        // operation for logging dynamic linking\n");
            writer.printf("        /* %d */ %s", logOperations.size() + 2, "DynamicLink");
            writer.printf(",\n        // operation for logging native method registration\n");
            writer.printf("        /* %d */ %s", logOperations.size() + 3, "RegisterNativeMethod");
        }

        public abstract String customizeTracePrologue(VmEntryFunctionDeclaration decl);

        public abstract String customizeTraceEpilogue(VmEntryFunctionDeclaration decl);

    }

    public static class JniCustomizer extends Customizer {

        @Override
        public String customizeTracePrologue(VmEntryFunctionDeclaration decl) {
            return entryLogging();
        }

        @Override
        public String customizeTraceEpilogue(VmEntryFunctionDeclaration decl) {
            return exitLogging();
        }

        private static String entryLogging() {
            StringBuilder sb = new StringBuilder();
            String[] args = getDefaultArgs();
            sb.append("        if (logger.enabled()) {\n");
            sb.append("            logger.log(LogOperations.");
            sb.append(currentMethod.name);
            sb.append('.');
            sb.append("ordinal(), UPCALL_ENTRY, anchor");
            for (int i = 0; i < args.length; i++) {
                String tag = args[i];
                sb.append(", ");
                sb.append(tag);
            }
            sb.append(");\n");
            sb.append("        }\n");
            return sb.toString();
        }

        private static String exitLogging() {
            StringBuilder sb = new StringBuilder();
            sb.append("            if (logger.enabled()) {\n");
            sb.append("                logger.log(LogOperations.");
            sb.append(currentMethod.name);
            sb.append('.');
            sb.append("ordinal(), UPCALL_EXIT);\n");
            sb.append("            }\n");
            return sb.toString();
        }
    }

    public static class VMCustomizer extends Customizer {

        private final boolean checkOnly;

        public VMCustomizer(boolean checkOnly) {
            this.checkOnly = checkOnly;
        }

        private ArrayList<VmEntryFunctionDeclaration> decls = new ArrayList<JniFunctionsGenerator.VmEntryFunctionDeclaration>();

        @Override
        public void startFunction(VmEntryFunctionDeclaration decl) {
            super.startFunction(decl);
            decls.add(decl);
        }

        @Override
        public String customizeTracePrologue(VmEntryFunctionDeclaration decl) {
            return  JniCustomizer.entryLogging();
        }

        @Override
        public String customizeTraceEpilogue(VmEntryFunctionDeclaration decl) {
            return JniCustomizer.exitLogging();
        }

        @Override
        public void customizeOperations(PrintWriter writer) {
            // no special operations
        }

        @Override
        public void close(PrintWriter writer) throws Exception {
            super.close(writer);
            final File vmHeaderFile = new File(new File(JavaProject.findWorkspace(), "com.oracle.max.vm.native/substrate/vm.h").getAbsolutePath());
            ProgramError.check(vmHeaderFile.exists(), "JMM header file " + vmHeaderFile + " does not exist");

            Writer vmDecls = new StringWriter();
            PrintWriter out = new PrintWriter(vmDecls);
            for (VmEntryFunctionDeclaration decl : decls) {
                assert decl.jniParameters.startsWith("void* env") : "First parameter of a VM function must be 'void* env': " + decl.jniParameters;
                String parameters = decl.jniParameters.replace("void* env", "JNIEnv *env");
                out.println("        " + decl.jniReturnType + " (JNICALL *" + decl.name + ") (" + parameters + ");");

            }
            vmDecls.close();

            if (Files.updateGeneratedContent(vmHeaderFile, ReadableSource.Static.fromString(vmDecls.toString()), "// START GENERATED CODE", "// END GENERATED CODE", checkOnly)) {
                System.out.println("Source for " + vmHeaderFile.getPath() + " was updated");
                System.exit(1);
            }
        }
    }

    public static String[] getDefaultArgs() {
        String[] paramNames = currentMethod.arguments.split(",\\s");
        String[] params = currentMethod.parameters.split(",\\s*");
        for (int i = 0; i < paramNames.length; i++) {
            String paramType = params[i].split(" ")[0];
            if (paramType.equals("int") || paramType.equals("short") || paramType.equals("char") ||
                            paramType.equals("byte")) {
                paramNames[i] = "Address.fromInt(" + paramNames[i] + ")";
            } else if (paramType.equals("long")) {
                paramNames[i] = "Address.fromLong(" + paramNames[i] + ")";
            } else if (paramType.equals("boolean")) {
                paramNames[i] = "Address.fromInt(" + paramNames[i] + " ? 1 : 0)";
            } else if (paramType.equals("float")) {
                paramNames[i] = "Address.fromInt(Float.floatToRawIntBits(" + paramNames[i] + "))";
            } else if (paramType.equals("double")) {
                paramNames[i] = "Address.fromLong(Double.doubleToRawLongBits(" + paramNames[i] + "))";
            }
        }
        return paramNames;
    }

    public static boolean generate(boolean checkOnly, Class source, Class target) throws Exception {
        return generate(checkOnly, "com.oracle.max.vm", source, target, new JniCustomizer());
    }

    /**
     * Inserts or updates generated source into {@code target}. The generated source is derived from
     * {@code source} and is delineated in {@code target} by the following lines:
     * <pre>
     * // START GENERATED CODE
     *
     * ...
     *
     * // END GENERATED CODE
     * </pre>

     *
     * @param checkOnly if {@code true}, then {@code target} is not updated; the value returned by this method indicates
     *            whether it would have been updated were this argument {@code true}
     * @param project TODO
     * @return {@code true} if {@code target} was modified (or would have been if {@code checkOnly} was {@code false}); {@code false} otherwise
     */
    public static boolean generate(boolean checkOnly, String project, Class source, Class target, Customizer customizer) throws Exception {
        File base = new File(JavaProject.findWorkspace(), project + File.separator + "src");
        File inputFile = new File(base, source.getName().replace('.', File.separatorChar) + ".java").getAbsoluteFile();
        File outputFile = new File(base, target.getName().replace('.', File.separatorChar) + ".java").getAbsoluteFile();

        LineReader lr = new LineReader(inputFile);
        String line = null;

        int state = BEFORE_FIRST_VM_ENTRY_POINT_FUNCTION;
        Writer writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        logOperations = new ArrayList<String>();

        while ((line = lr.readLine()) != null) {

            // Stop once the closing brace for the source class is found
            if (line.equals("}")) {
                break;
            }

            if (line.trim().equals(VM_ENTRY_POINT_ANNOTATION)) {
                lr.check(state == BEFORE_VM_ENTRY_POINT_FUNCTION || state == BEFORE_FIRST_VM_ENTRY_POINT_FUNCTION, "Illegal state (" + state + ") when parsing @JNI_FUNCTION");
                if (state == BEFORE_FIRST_VM_ENTRY_POINT_FUNCTION) {
                    out.println();
                    out.println("    private static final boolean INSTRUMENTED = " + TIME_JNI_FUNCTIONS + ";");
                    out.println();
                }
                state = BEFORE_PROLOGUE;
                out.println(line);
                continue;
            }

            if (state == BEFORE_PROLOGUE) {

                VmEntryFunctionDeclaration decl = VmEntryFunctionDeclaration.parse(line, inputFile.getName() + ":" + lr.lineNo);
                lr.check(decl != null, "JNI function declaration does not match pattern \"" + VmEntryFunctionDeclaration.PATTERN + "\"");

                out.println(line);
                out.println("        // Source: " + decl.sourcePos);

                if (!decl.isNative) {
                    customizer.startFunction(decl);
                    StringBuilder bodyBuffer = new StringBuilder();
                    String body = null;
                    while ((line = lr.readLine()) != null) {
                        if (line.equals("    }")) {
                            body = bodyBuffer.toString();
                            break;
                        }
                        if (line.length() > 0) {
                            bodyBuffer.append("    ");
                        }
                        bodyBuffer.append(customizer.customizeBody(line)).append("\n");
                    }

                    if (body == null) {
                        assert false;
                    }

                    if (decl.returnType.equals("void")) {
                        generateVoidFunction(out, decl, body, customizer);
                    } else {
                        generateNonVoidFunction(out, decl, body, customizer);
                    }
                }
                state = BEFORE_VM_ENTRY_POINT_FUNCTION;
                continue;
            }

            if (state == BEFORE_FIRST_VM_ENTRY_POINT_FUNCTION) {
                continue;
            }
            out.println(line);
        }

        customizer.close(out);
        writer.close();
        return Files.updateGeneratedContent(outputFile, ReadableSource.Static.fromString(writer.toString()), "// START GENERATED CODE", "// END GENERATED CODE", checkOnly);
    }

    private static void generateNonVoidFunction(PrintWriter out, VmEntryFunctionDeclaration decl, String body, Customizer customizer) {
        final String errReturnValue;
        if (decl.returnType.equals("boolean")) {
            errReturnValue = "false";
        } else if (decl.returnType.equals("char")) {
            errReturnValue = " (char) JNI_ERR";
        } else if (decl.returnType.equals("int") ||
                   decl.returnType.equals("byte") ||
                   decl.returnType.equals("char") ||
                   decl.returnType.equals("short") ||
                   decl.returnType.equals("float") ||
                   decl.returnType.equals("long") ||
                   decl.returnType.equals("double")) {
            errReturnValue = "JNI_ERR";
        } else {
            errReturnValue = "as" + decl.returnType + "(0L)";
        }

        generateFunction(out, decl, body, "return " + errReturnValue + ";", customizer);
    }

    private static void generateVoidFunction(PrintWriter out, VmEntryFunctionDeclaration decl, String body, Customizer customizer) {
        generateFunction(out, decl, body, null, customizer);
    }

    private static void generateFunction(PrintWriter out, VmEntryFunctionDeclaration decl, String body, String returnStatement, Customizer customizer) {
        boolean insertTimers = TIME_JNI_FUNCTIONS && decl.name != null;

        out.println("        Pointer anchor = prologue(env);");
        out.println(customizer.customizeTracePrologue(decl));
        if (insertTimers) {
            out.println("        long startTime = System.nanoTime();");
        }
        out.println("        try {");
        out.print(body);
        out.println("        } catch (Throwable t) {");
        out.println(customizer.customizeHandler(returnStatement));
        out.println("        } finally {");
        if (insertTimers) {
            out.println("            TIMER_" + decl.name + " += System.nanoTime() - startTime;");
            out.println("            COUNTER_" + decl.name + "++;");
        }
        out.println("            epilogue(anchor);");
        out.println(customizer.customizeTraceEpilogue(decl));
        out.println("        }");
        out.println("    }");
        if (insertTimers) {
            out.println("    public static long COUNTER_" + decl.name + ";");
            out.println("    public static long TIMER_" + decl.name + ";");
        }
    }

    /**
     * Command line interface for running the source code generator.
     * If the generation process modifies the existing source, then the exit
     * code of the JVM process will be non-zero.
     */
    public static void main(String[] args) throws Exception {
        boolean updated = false;
        if (generate(false, JniFunctionsSource.class, JniFunctions.class)) {
            System.out.println("Source for " + JniFunctions.class + " was updated");
            updated = true;
        }
        if (generate(false, JmmFunctionsSource.class, JmmFunctions.class)) {
            System.out.println("Source for " + JmmFunctions.class + " was updated");
            updated = true;
        }
        if (generate(false, "com.oracle.max.vm", VMFunctionsSource.class, VMFunctions.class, new VMCustomizer(false))) {
            System.out.println("Source for " + VMFunctions.class + " was updated");
            updated = true;
        }

        if (updated) {
            System.exit(1);
        }
    }
}
