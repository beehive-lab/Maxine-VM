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
package com.oracle.max.vm.ext.t1x.jvmti;

import static com.oracle.max.vm.ext.t1x.T1XTemplateTag.*;

import java.io.*;

import com.oracle.max.vm.ext.t1x.*;
import com.sun.max.annotate.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.vm.type.*;

/**
 * We generate an alternate subset of the standard templates that can generate JVMTI events, such as field access.
 * method entry, etc. These are used in place of the standard templates whenever a JVMTI agent requests any of those
 * capabilities.
 */
@HOSTED_ONLY
public class JVMTI_T1XTemplateGenerator extends T1XTemplateGenerator {

    class JVMTIAdviceHook implements AdviceHook {

        public void generate(T1XTemplateTag tag, AdviceType at, Object... args) {
            String k = null;
            if (args.length > 0) {
                if (args[0] instanceof Kind) {
                    k = j((Kind) args[0]);
                } else {
                    assert args[0] instanceof String;
                    k = (String) args[0];
                }
            }
            switch (tag) {
                case GETFIELD$byte:
                case GETFIELD$boolean:
                case GETFIELD$char:
                case GETFIELD$short:
                case GETFIELD$int:
                case GETFIELD$float:
                case GETFIELD$long:
                case GETFIELD$double:
                case GETFIELD$reference:
                case GETFIELD$word:
                    generateGetField(k, false);
                    break;

                case GETFIELD$byte$resolved:
                case GETFIELD$boolean$resolved:
                case GETFIELD$char$resolved:
                case GETFIELD$short$resolved:
                case GETFIELD$int$resolved:
                case GETFIELD$float$resolved:
                case GETFIELD$long$resolved:
                case GETFIELD$double$resolved:
                case GETFIELD$reference$resolved:
                case GETFIELD$word$resolved:
                    generateGetField(k, true);
                    break;

                case PUTFIELD$byte:
                case PUTFIELD$boolean:
                case PUTFIELD$char:
                case PUTFIELD$short:
                case PUTFIELD$int:
                case PUTFIELD$float:
                case PUTFIELD$long:
                case PUTFIELD$double:
                case PUTFIELD$reference:
                case PUTFIELD$word:
                    generatePutField(k, false);
                    break;

                case PUTFIELD$byte$resolved:
                case PUTFIELD$boolean$resolved:
                case PUTFIELD$char$resolved:
                case PUTFIELD$short$resolved:
                case PUTFIELD$int$resolved:
                case PUTFIELD$float$resolved:
                case PUTFIELD$long$resolved:
                case PUTFIELD$double$resolved:
                case PUTFIELD$reference$resolved:
                case PUTFIELD$word$resolved:
                    generatePutField(k, true);
                    break;

                case GETSTATIC$byte:
                case GETSTATIC$boolean:
                case GETSTATIC$char:
                case GETSTATIC$short:
                case GETSTATIC$int:
                case GETSTATIC$float:
                case GETSTATIC$long:
                case GETSTATIC$double:
                case GETSTATIC$reference:
                case GETSTATIC$word:
                    generateGetStatic(k, false);
                    break;

                case GETSTATIC$byte$init:
                case GETSTATIC$boolean$init:
                case GETSTATIC$char$init:
                case GETSTATIC$short$init:
                case GETSTATIC$int$init:
                case GETSTATIC$float$init:
                case GETSTATIC$long$init:
                case GETSTATIC$double$init:
                case GETSTATIC$reference$init:
                case GETSTATIC$word$init:
                    generateGetStatic(k, true);
                    break;

                case PUTSTATIC$byte:
                case PUTSTATIC$boolean:
                case PUTSTATIC$char:
                case PUTSTATIC$short:
                case PUTSTATIC$int:
                case PUTSTATIC$float:
                case PUTSTATIC$long:
                case PUTSTATIC$double:
                case PUTSTATIC$reference:
                case PUTSTATIC$word:
                    generatePutStatic(k, false);
                    break;

                case PUTSTATIC$byte$init:
                case PUTSTATIC$boolean$init:
                case PUTSTATIC$char$init:
                case PUTSTATIC$short$init:
                case PUTSTATIC$int$init:
                case PUTSTATIC$float$init:
                case PUTSTATIC$long$init:
                case PUTSTATIC$double$init:
                case PUTSTATIC$reference$init:
                case PUTSTATIC$word$init:
                    generatePutStatic(k, true);
                    break;

                case TRACE_METHOD_ENTRY:
                    generateTraceMethodEntry();
                    break;

                case BREAKPOINT:
                    generateBreakpoint();
                    break;
            }
        }

        public void startMethodGeneration() {
        }

    }

    private static final String INDENT8_PREFIX = "        ";
    private static final String ACCESS_PREFIX = "JVMTI.fieldAccessEvent";
    private static final String MODIFICATION_PREFIX = "JVMTI.fieldModificationEvent";
    private static final String INDENT8_ACCESS_PREFIX = INDENT8_PREFIX + ACCESS_PREFIX;
    private static final String INDENT8_MODIFICATION_PREFIX = INDENT8_PREFIX + MODIFICATION_PREFIX;

    private void generatePutField(String k, boolean resolved) {
        String offset = resolved ? "offset" : "f.offset()";
        out.printf(INDENT8_MODIFICATION_PREFIX + "(object, %s, false, %s);%n", offset, putValue(k));
    }

    private void generateGetField(String k, boolean resolved) {
        String offset = resolved ? "offset" : "f.offset()";
        out.printf(INDENT8_ACCESS_PREFIX + "(object, %s, false);%n", offset);
    }

    private void generatePutStatic(String k, boolean init) {
        String args = init ? "staticTuple, offset" : "f.holder().staticTuple(), f.offset()";
        out.printf(INDENT8_MODIFICATION_PREFIX + "(%s, true, %s);%n", args, putValue(k));
    }

    private void generateGetStatic(String k, boolean init) {
        String args = init ? "staticTuple, offset" : "f.holder().staticTuple(), f.offset()";
        out.printf(INDENT8_ACCESS_PREFIX + "(%s, true);%n", args);
    }

    private void generateTraceMethodEntry() {
        out.printf("        JVMTI.event(JVMTIEvent.METHOD_ENTRY, methodActor);\n");
    }

    private void generateBreakpoint() {
        out.printf("        JVMTIBreakpoints.event(id);\n");
    }

    private static String putValue(String k) {
        String value = "value";
        if (k.equals("Word")) {
            return value + ".asAddress().toLong()";
        } else {
            return value;
        }
    }

    @Override
    public void generateTraceMethodEntryTemplate() {
        startMethodGeneration();
        generateTemplateTag("%s", TRACE_METHOD_ENTRY);
        out.printf("    public static void traceMethodEntry(MethodActor methodActor) {%n");
        generateAfterAdvice();
        out.printf("    }%n");
        newLine();
    }

    private void generateBreakpointTemplate() {
        startMethodGeneration();
        generateTemplateTag("%s", BREAKPOINT);
        out.printf("    public static void breakpoint(long id) {%n");
        generateBeforeAdvice();
        out.printf("    }%n");
        newLine();
    }

    JVMTI_T1XTemplateGenerator(PrintStream ps) {
        super(ps);
    }

    @Override
    public void generateAll(AdviceHook adviceHook) {
        this.adviceHook = adviceHook;
        generateGetFieldTemplates();
        generatePutFieldTemplates();
        generateGetStaticTemplates();
        generatePutStaticTemplates();
        generateTraceMethodEntryTemplate();
        generateBreakpointTemplate();
    }

    static boolean generate(boolean checkOnly, Class target) throws Exception {
        File base = new File(JavaProject.findWorkspaceDirectory(), "com.oracle.max.vm.ext.t1x/src");
        File outputFile = new File(base, target.getName().replace('.', File.separatorChar) + ".java").getAbsoluteFile();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);
        JVMTI_T1XTemplateGenerator gen = new JVMTI_T1XTemplateGenerator(out);
        gen.generateAll(gen.new JVMTIAdviceHook());
        ReadableSource content = ReadableSource.Static.fromString(baos.toString());
        return Files.updateGeneratedContent(outputFile, content, "// START GENERATED CODE", "// END GENERATED CODE", checkOnly);
    }

    public static void main(String[] args) throws Exception {
        if (generate(false, JVMTI_T1XTemplateSource.class)) {
            System.out.println("Source for " + JVMTI_T1XTemplateSource.class + " was updated");
            System.exit(1);
        }

    }
}
