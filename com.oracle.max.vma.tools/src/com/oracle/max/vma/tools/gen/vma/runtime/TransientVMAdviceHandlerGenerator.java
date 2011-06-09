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
package com.oracle.max.vma.tools.gen.vma.runtime;

import static com.oracle.max.vma.tools.gen.vma.AdviceGeneratorHelper.*;
import static com.sun.max.vm.t1x.T1XTemplateGenerator.*;
import static com.oracle.max.vma.tools.gen.vma.runtime.TransientVMAdviceHandlerTypesGenerator.*;

import java.lang.reflect.*;

import com.oracle.max.vm.ext.vma.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.t1x.*;

@HOSTED_ONLY
public class TransientVMAdviceHandlerGenerator {

    public static void main(String[] args) {
        T1XTemplateGenerator.setGeneratingClass(TransientVMAdviceHandlerGenerator.class);
        TransientVMAdviceHandlerTypesGenerator.createEnumMaps();
        TransientVMAdviceHandlerTypesGenerator.generateRecordToEnumList();
        for (Method m : VMAdviceHandler.class.getMethods()) {
            if (m.getName().startsWith("advise")) {
                generate(m);
            }
        }
    }

    private static void generate(Method m) {
        final String name = m.getName();
        if (name.contains("GC") || name.contains("ThreadStarting") || name.contains("ThreadTerminating")) {
            return;
        }
        String recordType = methodToEnum.get(m);
        String adviceRecordName = enumToRecordName.get(recordType);
        String adviceMode = name.contains("Before") ? "0" : "1";

        generateAutoComment();
        out.printf("    @Override%n");
        int argCount = generateSignature(m, null);
        out.printf(" {%n");
        if (name.equals("adviseAfterMultiNewArray")) {
            out.printf("        adviseAfterNewArray(arg1, arg2[0]);%n");
        } else {
            out.printf("        super.%s(", name);
            generateInvokeArgs(argCount);
            if (name.contains("GetField") || name.contains("GetStatic") ||
                            name.endsWith("ArrayLength") || name.contains("NewArray") || name.contains("ArrayLoad")) {
                outStoreRecord(recordType, adviceMode, true);
                out.printf(", arg1, arg2);%n");
            } else if (name.contains("PutField") || name.contains("PutStatic") || name.contains("ArrayStore")) {
                outRecordDeclAndStore(adviceRecordName, recordType, adviceMode);
                out.printf(", arg1, arg2);%n");
                out.printf("        if (r != null) {%n");
                out.printf("            r.value2 = arg3;%n");
                out.printf("        }%n%n");
            } else if (name.contains("Operation") || name.endsWith("If") || name.contains("IInc")) {
                outRecordDeclAndStore(adviceRecordName, recordType, adviceMode);
                out.printf(", arg1);%n");
                out.printf("        if (r != null) {%n");
                out.printf("            r.value = arg2;%n");
                out.printf("            r.value2 = arg3;%n");
                out.printf("        }%n");
            } else if (name.contains("CheckCast") || name.contains("InstanceOf") || name.contains("Invoke")) {
                outRecordDeclAndStore(adviceRecordName, recordType, adviceMode);
                out.printf(", arg1);%n");
                out.printf("        if (r != null) {%n");
                out.printf("            r.value2 = arg2;%n");
                out.printf("        }%n");
            } else if (name.contains("Store")) {
                outRecordDeclAndStore(adviceRecordName, recordType, adviceMode);
                out.printf(", arg1);%n");
                out.printf("        if (r != null) {%n");
                out.printf("            r.value = arg2;%n");
                out.printf("        }%n");
            } else if (name.contains("Throw") || name.contains("Monitor") || name.contains("New") ||
                            name.contains("Conversion")  ||
                            name.contains("StackAdjust") || name.contains("Bytecode") || name.contains("IPush") ||
                            name.contains("Load") || (name.contains("Return") && argCount > 1)) {
                outStoreRecord(recordType, adviceMode, true);
                out.printf(", arg1);%n");
            } else {
                outStoreRecord(recordType, adviceMode, true);
                out.printf(");%n");
            }
        }
        out.printf("    }%n%n");
    }

    private static void outRecordDecl(String recordName) {
        out.printf("        %s r = (%s)", recordName, recordName);
    }

    private static void outStoreRecord(String recordType, String adviceMode, boolean indent) {
        if (indent) {
            out.printf("        ");
        }
        out.printf("storeRecord(%s, %s", recordType, adviceMode);
    }

    private static void outRecordDeclAndStore(String recordName, String recordType, String adviceMode) {
        outRecordDecl(recordName);
        out.print(' ');
        outStoreRecord(recordType, adviceMode, false);
    }

}
