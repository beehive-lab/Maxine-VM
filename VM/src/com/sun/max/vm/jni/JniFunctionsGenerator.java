/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.jni;

import java.io.*;
import java.util.regex.*;

import com.sun.max.annotate.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;

/**
 * This class implements the {@linkplain #generate() process} by which the source in {@link JniFunctionsSource JniFunctionsSource.java}.
 * is pre-processed to produce source in {@link JniFunctions JniFunctions.java}. The generated source is delineated by the following lines:
 * <pre>
 * // START GENERATED CODE
 *
 * ...
 *
 * // END GENERATED CODE
 * </pre>
 *
 * @author Doug Simon
 */
@HOSTED_ONLY
public class JniFunctionsGenerator {

    private static final String JNI_FUNCTION_ANNOTATION = "@VM_ENTRY_POINT";
    static final int BEFORE_FIRST_JNI_FUNCTION = -1;
    static final int BEFORE_JNI_FUNCTION = 0;
    static final int BEFORE_PROLOGUE = 1;

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

    static class JniFunctionDeclaration {
        static Pattern PATTERN = Pattern.compile("    private static (native )?(\\w+) (\\w+)\\(([^)]*)\\).*");

        String line;
        String returnType;
        boolean isNative;
        String name;
        String parameters;
        String arguments;
        String sourcePos;

        static JniFunctionDeclaration parse(String line, String sourcePos) {
            Matcher m = PATTERN.matcher(line);
            if (!m.matches()) {
                return null;
            }

            JniFunctionDeclaration decl = new JniFunctionDeclaration();
            decl.line = line;
            decl.isNative = m.group(1) != null;
            decl.returnType = m.group(2);
            decl.name = m.group(3);
            decl.parameters = m.group(4);

            String[] parameters = decl.parameters.split(",\\s*");
            StringBuilder arguments = new StringBuilder();
            for (int i = 0; i < parameters.length; ++i) {
                if (arguments.length() != 0) {
                    arguments.append(", ");
                }
                arguments.append(parameters[i].substring(parameters[i].lastIndexOf(' ') + 1));
            }
            decl.arguments = arguments.toString();
            decl.sourcePos = sourcePos;
            return decl;
        }

        public String declareHelper() {
            int index = line.indexOf('(');
            return line.substring(0, index) + '_' + line.substring(index);
        }

        public String callHelper() {
            return name + "_(" + arguments + ")";
        }
    }

    /**
     * Inserts or updates generated source into {@link JniFunctions JniFunctions.java}. The generated source is derived from
     * {@link JniFunctionsSource JniFunctionsSource.java} and is delineated in {@code JniFunctions.java} by the following lines:
     * <pre>
     * // START GENERATED CODE
     *
     * ...
     *
     * // END GENERATED CODE
     * </pre>

     *
     * @param checkOnly if {@code true}, then {@code JniFunctions.java} is not updated; the value returned by this method indicates
     *            whether it would have been updated were this argument {@code true}
     * @return {@code true} if {@link JniFunctions JniFunctions.java}. was modified (or would have been if {@code checkOnly} was {@code false}); {@code false} otherwise
     */
    static boolean generate(boolean checkOnly) throws Exception {
        File base = new File(JavaProject.findVcsProjectDirectory().getParentFile().getAbsoluteFile(), "VM/src");
        File inputFile = new File(base, JniFunctionsSource.class.getName().replace('.', File.separatorChar) + ".java").getAbsoluteFile();
        File outputFile = new File(base, JniFunctions.class.getName().replace('.', File.separatorChar) + ".java").getAbsoluteFile();

        LineReader lr = new LineReader(inputFile);
        String line = null;

        int state = BEFORE_FIRST_JNI_FUNCTION;
        Writer writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);

        while ((line = lr.readLine()) != null) {

            // Stop once the closing brace for the source class is found
            if (line.equals("}")) {
                break;
            }

            if (line.trim().equals(JNI_FUNCTION_ANNOTATION)) {
                lr.check(state == BEFORE_JNI_FUNCTION || state == BEFORE_FIRST_JNI_FUNCTION, "Illegal state (" + state + ") when parsing @JNI_FUNCTION");
                if (state == BEFORE_FIRST_JNI_FUNCTION) {
                    out.println();
                }
                state = BEFORE_PROLOGUE;
                out.println(line);
                continue;
            }

            if (state == BEFORE_PROLOGUE) {

                JniFunctionDeclaration decl = JniFunctionDeclaration.parse(line, inputFile.getName() + ":" + lr.lineNo);
                lr.check(decl != null, "JNI function declaration does not match pattern \"" + JniFunctionDeclaration.PATTERN + "\"");

                out.println(line);
                out.println("        // Source: " + decl.sourcePos);

                if (!decl.isNative) {
                    StringBuilder bodyBuffer = new StringBuilder();
                    String body = null;
                    while ((line = lr.readLine()) != null) {
                        if (line.equals("    }")) {
                            body = bodyBuffer.toString();
                            break;
                        }
                        bodyBuffer.append("    ").append(line).append("\n");
                    }

                    if (body == null) {
                        assert false;
                    }

                    if (decl.returnType.equals("void")) {
                        generateVoidFunction(out, decl, body);
                    } else {
                        generateNonVoidFunction(out, decl, body);
                    }
                }
                state = BEFORE_JNI_FUNCTION;
                continue;
            }

            if (state == BEFORE_FIRST_JNI_FUNCTION) {
                continue;
            }
            out.println(line);
        }

        writer.close();
        return Files.updateGeneratedContent(outputFile, ReadableSource.Static.fromString(writer.toString()), "// START GENERATED CODE", "// END GENERATED CODE", checkOnly);
    }

    private static void generateNonVoidFunction(PrintWriter out, JniFunctionDeclaration decl, String body) {
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
            errReturnValue = "as" + decl.returnType + "(0)";
        }

        out.println("        Pointer anchor = prologue(env, \"" + decl.name + "\");");
        out.println("        try {");
        out.print(body);
        out.println("        } catch (Throwable t) {");
        out.println("            VmThread.fromJniEnv(env).setPendingException(t);");
        out.println("            return " + errReturnValue + ";");
        out.println("        } finally {");
        out.println("            epilogue(anchor, \"" + decl.name + "\");");
        out.println("        }");
        out.println("    }");
    }

    private static void generateVoidFunction(PrintWriter out, JniFunctionDeclaration decl, String body) {
        out.println("        Pointer anchor = prologue(env, \"" + decl.name + "\");");
        out.println("        try {");
        out.print(body);
        out.println("        } catch (Throwable t) {");
        out.println("            VmThread.fromJniEnv(env).setPendingException(t);");
        out.println("        } finally {");
        out.println("            epilogue(anchor, \"" + decl.name + "\");");
        out.println("        }");
        out.println("    }");
    }

    /**
     * Command line interface for running the source code generator.
     * If the generation process modifies {@link JniFunctions JniFunctions.java}, then the exit
     * code of the JVM process will be non-zero.
     */
    public static void main(String[] args) throws Exception {
        if (generate(false)) {
            System.out.println("Source for " + JniFunctions.class + " was updated");
            System.exit(1);
        }
    }
}
