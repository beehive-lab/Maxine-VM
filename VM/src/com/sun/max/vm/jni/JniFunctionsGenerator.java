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

    static final int BEFORE_FIRST_JNI_FUNCTION = -1;
    static final int BEFORE_JNI_FUNCTION = 0;
    static final int BEFORE_PROLOGUE = 1;
    static final int BEFORE_EPILOGUE = 2;

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

        Pattern decl = Pattern.compile("    private static (\\w+) (\\w+).*");

        String errReturnValue = null;
        String functionName = null;

        int state = BEFORE_FIRST_JNI_FUNCTION;

        Writer writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);

        while ((line = lr.readLine()) != null) {

            // Stop once the closing brace for the source class is found
            if (line.equals("}")) {
                break;
            }

            if (line.trim().equals("@JNI_FUNCTION")) {
                lr.check(state == BEFORE_JNI_FUNCTION || state == BEFORE_FIRST_JNI_FUNCTION, "Illegal state (" + state + ") when parsing @JNI_FUNCTION");
                if (state == BEFORE_FIRST_JNI_FUNCTION) {
                    out.println();
                }
                state = BEFORE_PROLOGUE;
                out.println("    // Source: " + inputFile.getName() + ":" + lr.lineNo);
                out.println(line);
                continue;
            }

            if (state == BEFORE_PROLOGUE) {
                out.println(line);
                Matcher matcher = decl.matcher(line);
                lr.check(matcher.matches(), "JNI function declaration does not match pattern \"" + decl + "\"");
                if (!matcher.group(1).equals("native")) {

                    String returnType = matcher.group(1);
                    functionName = matcher.group(2);

                    out.println("        Pointer anchor = prologue(env, \"" + functionName + "\");");
                    out.println("        try {");

                    if (returnType.equals("void")) {
                        errReturnValue = null;
                    } else if (returnType.equals("boolean")) {
                        errReturnValue = " false";
                    } else if (returnType.equals("char")) {
                        errReturnValue = " (char) JNI_ERR";
                    } else if (returnType.equals("int") ||
                               returnType.equals("byte") ||
                               returnType.equals("char") ||
                               returnType.equals("short") ||
                               returnType.equals("float") ||
                               returnType.equals("long") ||
                               returnType.equals("double")) {
                        errReturnValue = " JNI_ERR";
                    } else {
                        errReturnValue = " as" + returnType + "(0)";
                    }

                    state = BEFORE_EPILOGUE;
                } else {
                    state = BEFORE_JNI_FUNCTION;
                }
                continue;
            }

            if (line.equals("    }") && state == BEFORE_EPILOGUE) {
                state = BEFORE_JNI_FUNCTION;
                out.println("        } catch (Throwable t) {");
                out.println("            VmThread.fromJniEnv(env).setPendingException(t);");
                if (errReturnValue != null) {
                    out.println("            return" + errReturnValue + ";");
                }
                out.println("        } finally {");
                out.println("            epilogue(anchor, \"" + functionName + "\");");
                out.println("        }");
                out.println(line);
                continue;
            }

            if (state == BEFORE_FIRST_JNI_FUNCTION) {
                continue;
            }

            if (state == BEFORE_EPILOGUE) {
                out.print("    ");
            }
            out.println(line);
        }

        lr.check(line != null, "Did not find last line consisting of '}'");

        writer.close();
        return Files.updateGeneratedContent(outputFile, ReadableSource.Static.fromString(writer.toString()), "// START GENERATED CODE", "// END GENERATED CODE", checkOnly);
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
