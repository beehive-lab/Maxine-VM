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

import static com.oracle.max.vma.tools.gen.vma.AdviceGeneratorHelper.*;

import java.lang.reflect.*;
import java.util.regex.*;

import com.oracle.max.vm.ext.vma.*;
import com.sun.cri.bytecode.*;

/**
 * Generates the {@link VMABytecodes} enum that is a clone of {@link Bytecodes} but
 * with the code provided as a field and a methodname that is used in {@link BytecodeAdvice}.
 *
 * One pseudo bytecode, MENTRY, is added for method entry; it corresponds to the
 * {@link T1XTemplateTag#TRACE_METHOD_ENTRY} template.
 *
 */
public class VMABytecodesGenerator {

    public static void main(String[] args) throws Exception {
        boolean first = true;
        createGenerator(VMABytecodesGenerator.class);
        generateAutoComment();
        for (Field f : Bytecodes.class.getDeclaredFields()) {
            int modifiers = f.getModifiers();
            if (Modifier.isPublic(modifiers))  {
                generate(f, first);
            }
            first = false;
        }
        // pseudo bytecode for method entry
        out.printf(",%n    %s(-1, \"%s\")", "MENTRY", "MethodEntry");
        out.printf(";%n");
    }

    private static void generate(Field f, boolean first) {
        final String name = f.getName();
        if (!first) {
            out.printf(",%n");
        }
        out.printf("    %s(Bytecodes.%s, \"%s\")", name, name, getMethodStyleName(name));
    }

    private static String getMethodStyleName(String name) {
        // no automatic way to do this
        String result;
        if ((result = checkChangeCase(name, "FIELD")) != null) {
            return result;
        } else if ((result = checkChangeCase(name, "STATIC")) != null) {
            return result;
        } else if ((result = checkChangeCase(name, "NEW")) != null) {
            return result;
        } else if ((result = checkChangeCase(name, "SPECIAL")) != null) {
            return result;
        } else if ((result = checkChangeCase(name, "VIRTUAL")) != null) {
            return result;
        } else if ((result = checkChangeCase(name, "INTERFACE")) != null) {
            return result;
        } else if (name.equals("NEWARRAY") || name.equals("ANEWARRAY")) {
            return "NewArray";
        } else if (name.equals("CHECKCAST")) {
            return "CheckCast";
        } else if (name.equals("INSTANCEOF")) {
            return "InstanceOf";
        } else if (name.equals("ARRAYLENGTH")) {
            return "ArrayLength";
        } else if (name.equals("ATHROW")) {
            return "Throw";
        } else if (name.equals("MONITORENTER")) {
            return "MonitorEnter";
        } else if (name.equals("MONITOREXIT")) {
            return "MonitorExit";
        } else if (name.equals("MULTIANEWARRAY")) {
            return "MultiNewArray";
        } else if (Pattern.matches(".{1}RETURN|RETURN", name)) {
            return "Return";
        } else if (Pattern.matches(".{1}ALOAD", name)) {
            return "ArrayLoad";
        } else if (Pattern.matches(".{1}ASTORE", name)) {
            return "ArrayStore";
        } else if (Pattern.matches(".{1}CONST.*|LDC.*", name)) {
            return "ConstLoad";
        } else if (Pattern.matches(".{1}LOAD.*", name)) {
            return "Load";
        } else if (Pattern.matches(".{1}STORE.*", name)) {
            return "Store";
        } else if (Pattern.matches(".{1}IPUSH", name)) {
            return "ConstLoad";
        } else if (Pattern.matches(".{1}(ADD|SUB|MUL|DIV|REM|AND|OR|XOR|SHL|SHR|USHR|NEG)", name)) {
            return "Operation";
        } else if (Pattern.matches(".{1}2.{1}|MOV_.*", name)) {
            return "Conversion";
        } else if (Pattern.matches("IF.*", name)) {
            return "If";
        } else if (Pattern.matches("POP|POP2|DUP.*|SWAP", name)) {
            return "StackAdjust";
        } else if (Pattern.matches("[FLD]CMP.*", name)) {
            return "Operation";
        } else if (name.equals("IINC")) {
            return "Operation";
        } else {
            return "Bytecode";
        }
    }

    private static String checkChangeCase(String name, String suffix) {
        if (name.endsWith(suffix)) {
            int index = name.indexOf(suffix);
            assert index >= 0;
            return AdviceGeneratorHelper.toFirstUpper(name.substring(0, index).toLowerCase()) + AdviceGeneratorHelper.toFirstUpper(suffix.toLowerCase());
        } else {
            return null;
        }
    }

}
