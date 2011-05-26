/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.t1x.vma.gen;

import static com.sun.max.vm.t1x.T1XTemplateGenerator.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.t1x.T1XTemplateGenerator;
import com.sun.max.vm.t1x.T1XTemplateGenerator.AdviceType;
import com.sun.max.vm.t1x.T1XTemplateTag;
import com.sun.max.vm.t1x.T1XTemplateGenerator.AdviceHook;

/**
 * Template generation that supports the {@link VMAAdvice} interface.
 */

@HOSTED_ONLY
public class VMAAdviceTemplateGenerator {
    private static class VMAT1XAdvice implements AdviceHook {

        public void generate(T1XTemplateTag tag, AdviceType adviceType, String... args) {
            final String k = args.length > 0 ? args[0] : null;
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
                    if (adviceType == AdviceType.BEFORE) {
                        generateGetField(adviceType, k, false);
                    }
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
                    if (adviceType == AdviceType.BEFORE) {
                        generateGetField(adviceType, k, true);
                    }
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
                    if (adviceType == AdviceType.BEFORE) {
                        generatePutField(adviceType, k, false);
                    }
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
                    if (adviceType == AdviceType.BEFORE) {
                        generatePutField(adviceType, k, true);
                    }
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
                    if (adviceType == AdviceType.BEFORE) {
                        generateGetStatic(adviceType, k, false);
                    }
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
                    if (adviceType == AdviceType.BEFORE) {
                        generateGetStatic(adviceType, k, true);
                    }
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
                    if (adviceType == AdviceType.BEFORE) {
                        generatePutStatic(adviceType, k, false);
                    }
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
                    if (adviceType == AdviceType.BEFORE) {
                        generatePutStatic(adviceType, k, true);
                    }
                    break;

                case IALOAD:
                case LALOAD:
                case FALOAD:
                case DALOAD:
                case AALOAD:
                case BALOAD:
                case CALOAD:
                case SALOAD:
                    if (adviceType == AdviceType.BEFORE) {
                        generateArrayLoad(adviceType, k);
                    }
                    break;
                case IASTORE:
                case LASTORE:
                case FASTORE:
                case DASTORE:
                case AASTORE:
                case BASTORE:
                case CASTORE:
                case SASTORE:
                    if (adviceType == AdviceType.BEFORE) {
                        generateArrayStore(adviceType, k);
                    }
                    break;


                case NEW:
                case NEW$init:
                    if (adviceType == AdviceType.AFTER) {
                        generateNew(adviceType);
                    }
                    break;

                case NEWARRAY:
                case ANEWARRAY:
                case ANEWARRAY$resolved:
                    if (adviceType == AdviceType.AFTER) {
                        generateNewUniArray(adviceType);
                    }
                    break;

                case MULTIANEWARRAY:
                case MULTIANEWARRAY$resolved:
                    if (adviceType == AdviceType.AFTER) {
                        generateNewMultiArray(adviceType);
                    }
                    break;

                case INVOKESPECIAL$void:
                case INVOKESPECIAL$void$resolved:
                    if (adviceType == AdviceType.AFTER) {
                        generateInvokeSpecialVoid(adviceType);
                    }
                    break;

                default:
                    // no advice
            }
        }
    }
    private static void generatePutField(AdviceType adviceType, String k, boolean resolved) {
        String offset = resolved ? "offset" : "f.offset()";
        out.printf("        if (isAdvising()) {%n");
        out.printf("            VMAStaticBytecodeAdvice.advise%sPutField(object, %s, %s);%n", adviceType.methodNameComponent, offset, putValue(k));
        out.printf("        }%n");
    }

    private static void generateGetField(AdviceType adviceType, String k, boolean resolved) {
        String offset = resolved ? "offset" : "f.offset()";
        out.printf("        if (isAdvising()) {%n");
        out.printf("            VMAStaticBytecodeAdvice.advise%sGetField(object, %s, %s);%n", adviceType.methodNameComponent, offset, getDefault(oType(k)));
        out.printf("        }%n");
    }

    private static void generatePutStatic(AdviceType adviceType, String k, boolean init) {
        String args = init ? "staticTuple, offset" : "f.holder().staticTuple(), f.offset()";
        out.printf("        if (isAdvising()) {%n");
        out.printf("            VMAStaticBytecodeAdvice.advise%sPutStatic(%s, %s);%n", adviceType.methodNameComponent, args, putValue(k));
        out.printf("        }%n");
    }

    private static void generateGetStatic(AdviceType adviceType, String k, boolean init) {
        String args = init ? "staticTuple, offset" : "f.holder().staticTuple(), f.offset()";
        out.printf("        if (isAdvising()) {%n");
        out.printf("            VMAStaticBytecodeAdvice.advise%sGetStatic(%s, %s);%n", adviceType.methodNameComponent, args, getDefault(oType(k)));
        out.printf("        }%n");
    }

    private static void generateArrayLoad(AdviceType adviceType, String k) {
        out.printf("        if (isAdvising()) {%n");
        out.printf("            VMAStaticBytecodeAdvice.advise%sArrayLoad(array, index, %s);%n", adviceType.methodNameComponent, getDefault(oType(k)));
        out.printf("        }%n");
    }

    private static void generateArrayStore(AdviceType adviceType, String k) {
        out.printf("        if (isAdvising()) {%n");
        out.printf("            VMAStaticBytecodeAdvice.advise%sArrayStore(array, index, %s);%n", adviceType.methodNameComponent, putValue(k));
        out.printf("        }%n");
    }

    private static String putValue(String k) {
        if (k.equals("Word")) {
            return "value.asAddress().toLong()";
        } else if (k.equals("boolean")) {
            return "value ? 1 : 0";
        } else {
            return "value";
        }
    }

    private static String getDefault(String k) {
        if (k.equals("float")) {
            return "0.0f";
        } else if (k.equals("double")) {
            return "0.0";
        } else if (k.equals("Object")) {
            return "null";
        } else {
            return "0";
        }
    }

    private static void generateNew(AdviceType adviceType) {
        out.printf("        if (isAdvising()) {%n");
        out.printf("            VMAStaticBytecodeAdvice.advise%sNew(object);%n", adviceType.methodNameComponent);
        out.printf("        }%n");
    }

    private static void generateNewUniArray(AdviceType adviceType) {
        out.printf("        if (isAdvising()) {%n");
        out.printf("            VMAStaticBytecodeAdvice.advise%sNewArray(array, length);%n", adviceType.methodNameComponent);
        out.printf("        }%n");
    }

    private static void generateNewMultiArray(AdviceType adviceType) {
        out.printf("        if (isAdvising()) {%n");
        out.printf("            VMAStaticBytecodeAdvice.advise%sMultiNewArray(array, lengths);%n", adviceType.methodNameComponent);
        out.printf("        }%n");
    }

    private static void generateInvokeSpecialVoid(AdviceType adviceType) {
        out.printf("        if (isAdvising()) {%n");
        out.printf("            VMAStaticBytecodeAdvice.advise%sInvokeSpecial(Reference.fromOrigin(receiver).toJava());%n", adviceType.methodNameComponent);
        out.printf("        }%n");
    }

    public static void main(String[] args) {
        final AdviceHook hook = new VMAT1XAdvice();
        setAdviceHook(hook);
        setGeneratingClass(VMAAdviceTemplateGenerator.class);
        generateNewTemplates();
        generatePutTemplates();
        generateGetTemplates();
        T1XTemplateGenerator.generateArrayLoadTemplates();
        T1XTemplateGenerator.generateArrayStoreTemplates();
    }

    private static void generateNewTemplates() {
        T1XTemplateGenerator.generateNewTemplates();
        T1XTemplateGenerator.generateNewArrayTemplate();
        T1XTemplateGenerator.generateANewArrayTemplates();
        T1XTemplateGenerator.generateMultiANewArrayTemplates();
        T1XTemplateGenerator.generateInvokeSSTemplate("void", "special", "");
        T1XTemplateGenerator.generateInvokeSSTemplate("void", "special", "resolved");
    }

    private static void generatePutTemplates() {
        T1XTemplateGenerator.generatePutFieldTemplates();
        T1XTemplateGenerator.generatePutStaticTemplates();
    }

    private static void generateGetTemplates() {
        T1XTemplateGenerator.generateGetFieldTemplates();
        T1XTemplateGenerator.generateGetStaticTemplates();
    }

}
