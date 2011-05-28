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
package com.oracle.max.vma.tools.gen.vma;

import static com.sun.max.vm.t1x.T1XTemplateGenerator.*;
import static com.oracle.max.vm.ext.vma.VMABytecodes.*;

import java.util.*;
import java.util.regex.*;

import com.oracle.max.vm.ext.vma.*;
import com.sun.cri.bytecode.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.t1x.*;

/**
 * Generates the bytecode advising interface by processing {@link VMABytecodes}. This interface supports advising by having
 * the VM invoke the advice method when a bytecode is (logically) executed (logically because the bytecodes may have
 * been compiled into native code). The pertinent state associated with the bytecode is typically provided as arguments
 * to the method, to avoid having a separate API to the execution state.
 *
 * The generated API is not simply 1-1 with the list of bytecodes because several bytecodes, e.g. {@code ICONST_*} form
 * a group for which there is no value in providing independent advice and some, e.g. {@code GETFIELD} for which it is
 * necessary to create type-specific variants (in order to pass arguments unboxed). To reduce the fan out of methods
 * boolean, byte, char, short, int and Word values are all passed as long. To reduce fan out further a value argument is
 * always specified even if it might not be meaningful, e.g. {@link AdviceMode#BEFORE before advice} on a
 * {@link Bytecodes#GETFIELD}.
 *
 * N.B. Currently the default generated API is limited to advising a subset of the bytecodes, although
 * it is possible to generate a complete API if required with the "-complete" option.
 *
 * We use some of the auto-generation facilities from {@link T1XTemplateGenerator}.
 */

@HOSTED_ONLY
public class BytecodeAdviceGenerator {

    private static final String METHOD_PREFIX = "    public abstract void advise%s%s(";

    private static final Set<String> getPutTypes = new HashSet<String>();

    private static String adviceMode;

    public static void main(String[] args) {
        boolean complete = false;
        for (String arg : args) {
            if (arg.equals("-complete")) {
                complete = true;
            }
        }
        setGeneratingClass(BytecodeAdviceGenerator.class);
        for (String k : types) {
            if (!(k.equals("boolean") || k.equals("byte") || k.equals("char") || k.equals("short") || k.equals("int") || k.equals("Word"))) {
                getPutTypes.add(oType(k));
            }
        }
        for (String a : new String[] {"Before", "After"}) {
            adviceMode = a;
            for (VMABytecodes f : VMABytecodes.values()) {
                if (complete) {
                    generateComplete(f);
                } else {
                    generateSpecific(f);
                }
            }
        }
    }

    private static void generateComplete(VMABytecodes bytecode) {
        generateAutoComment();
        out.printf(METHOD_PREFIX + ");%n%n", adviceMode, bytecode.methodName);
    }

    private static void generateSpecific(VMABytecodes bytecode) {
        if (bytecode == GETSTATIC) {
            generateGetPutStatic(bytecode);
        } else if (bytecode == GETFIELD) {
            generateGetPutField(bytecode);
        } else if (bytecode == PUTFIELD) {
            generateGetPutField(bytecode);
        } else if (bytecode == PUTSTATIC) {
            generateGetPutStatic(bytecode);
        } else if (bytecode == NEW) {
            generateNew(bytecode);
        } else if (bytecode == NEWARRAY) {
            // NEWARRAY and ANEWARRAY
            generateNewArray(bytecode);
        } else if (bytecode == MULTIANEWARRAY) {
            generateMultiNewArray(bytecode);
        } else if (bytecode == INVOKESPECIAL) {
            generateInvoke(bytecode);
        } else if (Pattern.matches(".{1}ALOAD", bytecode.name())) {
            generateArrayLoadStore(bytecode);
        } else if (Pattern.matches(".{1}ASTORE", bytecode.name())) {
            generateArrayLoadStore(bytecode);
        }
    }

    private static void generateGetPutField(VMABytecodes bytecode) {
        if (adviceMode.equals("Before")) {
            for (String k : getPutTypes) {
                if (hasGetPutTemplates(k)) {
                    generateAutoComment();
                    out.printf(METHOD_PREFIX + "Object object, int offset, %s value);%n%n", adviceMode, bytecode.methodName, k);
                }
            }
        }
    }

    private static void generateGetPutStatic(VMABytecodes bytecode) {
        if (adviceMode.equals("Before")) {
            for (String k : getPutTypes) {
                if (hasGetPutTemplates(k)) {
                    generateAutoComment();
                    // StaticTuple but ClassActor.staticTuple returns Object
                    out.printf(METHOD_PREFIX + "Object staticTuple, int offset, %s value);%n%n", adviceMode, bytecode.methodName, k);
                }
            }
        }
    }

    private static void generateNew(VMABytecodes bytecode) {
        if (adviceMode.equals("After")) {
            generateAutoComment();
            out.printf(METHOD_PREFIX + "Object object);%n%n", adviceMode, bytecode.methodName);
        }
    }

    private static void generateNewArray(VMABytecodes bytecode) {
        if (adviceMode.equals("After")) {
            generateAutoComment();
            out.printf(METHOD_PREFIX + "Object object, int length);%n%n", adviceMode, bytecode.methodName);
        }
    }

    private static void generateMultiNewArray(VMABytecodes bytecode) {
        if (adviceMode.equals("After")) {
            generateAutoComment();
            out.printf(METHOD_PREFIX + "Object object, int[] lengths);%n%n", adviceMode, bytecode.methodName);
        }
    }

    private static void generateInvoke(VMABytecodes bytecode) {
        if (adviceMode.equals("After")) {
            generateAutoComment();
            out.printf(METHOD_PREFIX + "Object object);%n%n", adviceMode, bytecode.methodName);
        }
    }

    private static Set<String> scalarArraySet = new HashSet<String>();

    private static void generateArrayLoadStore(VMABytecodes bytecode) {
        if (adviceMode.equals("Before")) {
            String type = typeFor(bytecode.name().charAt(0));
            if (!scalarArraySet.contains(bytecode.methodName + type)) {
                generateAutoComment();
                out.printf(METHOD_PREFIX + "Object array, int index, %s value);%n%n", adviceMode, bytecode.methodName, type);
                scalarArraySet.add(bytecode.methodName + type);
            }
        }
    }

    private static String typeFor(char c) {
        switch (c) {
            case 'A':
                return "Object";
            case 'I':
            case 'L':
            case 'B':
            case 'C':
            case 'S':
                return "long";
            case 'F':
                return "float";
            case 'D':
                return "double";
        }
        return "???";
    }

}
