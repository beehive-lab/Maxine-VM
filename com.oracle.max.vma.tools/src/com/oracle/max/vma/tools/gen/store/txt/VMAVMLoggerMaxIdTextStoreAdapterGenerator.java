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
package com.oracle.max.vma.tools.gen.store.txt;

import static com.oracle.max.vma.tools.gen.vma.AdviceGeneratorHelper.*;

import java.lang.reflect.*;

import com.oracle.max.vm.ext.vma.*;
import com.oracle.max.vm.ext.vma.handlers.store.vmlog.h.*;
import com.oracle.max.vm.ext.vma.store.txt.*;
import com.oracle.max.vma.tools.gen.vma.*;
import com.sun.max.annotate.*;
import com.sun.max.vm.jni.*;


/**
 * Generates implementation of {@link VMAVMLoggerTextStoreAdaptor} from the
 * methods in {@link VMAdviceHandler}.
 *
 * This converts from the {@link ObjectID}, {@link ClassID}, etc. form to
 * the {@link String} and {@code long} valued API of {@link VMATextStore}.
 */
@HOSTED_ONLY
public class VMAVMLoggerMaxIdTextStoreAdapterGenerator {

    public static void main(String[] args) throws Exception {
        createGenerator(VMAVMLoggerMaxIdTextStoreAdapterGenerator.class);
        generateAutoComment();
        for (Method m : VMAVMLogger.VMAVMLoggerInterface.class.getMethods()) {
            if (m.getName().startsWith("advise")) {
                generate(m);
            }
        }
        AdviceGeneratorHelper.updateSource(VMAVMLoggerMaxIdTextStoreAdapter.class, null, false);
    }

    private static void generate(Method m) {
        String oname = getMethodNameRenamingObjectID(m);
        out.printf("%s@Override%n", INDENT4);
        generateSignature(INDENT4, "public", new MethodNameOverride(m), null, null);
        out.printf(" {%n");
        Class< ? >[] params = m.getParameterTypes();
        out.printf("%stxtStore.%s(arg1", INDENT8, oname);
        if (!noThread(m.getName())) {
            out.print(", null");
        }
        // skip arg1 (time)
        for (int i = 1; i < params.length; i++) {
            Class< ? > param = params[i];
            String arg = "arg" + (i + 1);
            out.printf(", %s", convertArg(param.getSimpleName(), arg));
        }
        out.printf(");%n");
        out.printf("    }%n%n");
    }

    private static String convertArg(String type, String arg) {
        if (type.equals("ObjectID")) {
            return arg + ".toLong()";
        } else if (type.equals("MethodID") || type.equals("FieldID")) {
            return "MemberID.getMemberIDAsInt(" + arg + ")";
        } else if (type.equals("ClassID")) {
            return "ClassID.asInt(" + arg + ")";
        } else {
            return arg;
        }
    }

    private static boolean noThread(String name) {
        return name.contains("Invoke") || name.contains("MethodEntry") || name.contains("unseen") ||
               name.contains("Static") || name.contains("Field") || name.contains("New") ||
               name.contains("CheckCast") || name.contains("InstanceOf");
    }

    private static String getMethodNameRenamingObjectID(Method m) {
        String result = m.getName();
        if (result.contains("PutStatic") || result.contains("PutField") ||
            result.contains("GetStatic") || result.contains("GetField") ||
            result.endsWith("Load") || result.endsWith("Store") || result.endsWith("Return") || result.endsWith("If")) {
            String lastParam = result.endsWith("If") ? getNextToLastParameterName(m) : getLastParameterName(m);
            if (lastParam != null && lastParam.equals("ObjectID")) {
                result += "Object";
            }
        }
        return result;
    }

}
