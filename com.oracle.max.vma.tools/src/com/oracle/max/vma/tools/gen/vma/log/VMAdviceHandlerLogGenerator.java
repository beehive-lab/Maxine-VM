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
package com.oracle.max.vma.tools.gen.vma.log;

import static com.sun.max.vm.t1x.T1XTemplateGenerator.*;
import static com.oracle.max.vma.tools.gen.vma.AdviceGeneratorHelper.*;

import java.lang.reflect.*;

import com.oracle.max.vm.ext.vma.*;
import com.sun.max.annotate.*;

/**
 * Generates the {@link VMAdviceHandlerLog} interface that operates in terms of {@link String strings}
 * and scalar types suitable for saving to a file. The mapping is generated from the
 * {@link VMAdviceHandler} interface. This mapping isn't very amenable to automation
 * simply based on the type signatures, as the interpretation of {@link Object} arguments is
 * method specific (e.g., whether it denotes an instance or a static tuple).
 *
 * One consequence of the type mapping is that the method signatures with values of type {@link Object} and
 * {@code long} map to the same, as {@link Object} is converted to a unique id. Rather than
 * qualify every method with a type name, we just qualify the {@link Object} case.
 *
 * The generating thread (name) is added as the first argument to all the methods.
 *
 * The API is designed to avoid object allocation as far as possible. For example it would be convenient
 * to encapsulate a class name and the class loader id as a (value) object and, similarly, for (qualified) field
 * and method names. However, measurements showed that that would place a significant load on the heap just
 * to pass those values.
 *
 *
 */
@HOSTED_ONLY
public class VMAdviceHandlerLogGenerator {

    public static void main(String[] args) {
        createGenerator(VMAdviceHandlerLogGenerator.class);
        for (Method m : VMAdviceHandler.class.getMethods()) {
            if (m.getName().startsWith("advise")) {
                generate(m);
            }
        }
    }

    private static boolean getFieldDone;
    private static boolean getStaticDone;
    private static boolean arrayLoadDone;

    private static void generate(Method m) {
        final String name = m.getName();
        generateAutoComment();
        out.printf("    public abstract void %s(String threadName", getMethodNameRenamingObject(m));
        if (name.endsWith("ConstLoad")) {
            out.printf(", %s value", getLastParameterNameHandlingObject(m));
        } else if (name.endsWith("GetField")) {
            out.print(", long objId, String className, long clId, String fieldName");
            getFieldDone = true;
        } else if (name.endsWith("PutField")) {
            out.printf(", long objId, String className, long clId, String fieldName, %s value", getLastParameterNameHandlingObject(m));
        } else if (name.endsWith("GetStatic")) {
            out.print(", String className, long clId, String fieldName");
            getStaticDone = true;
        } else if (name.endsWith("PutStatic")) {
            out.printf(", String className, long clId, String fieldName, %s value", getLastParameterNameHandlingObject(m));
        } else if (name.endsWith("ArrayLoad")) {
            out.print(", long objId, int index");
            arrayLoadDone = true;
        } else if (name.endsWith("ArrayStore")) {
            out.printf(", long objId, int index, %s value", getLastParameterNameHandlingObject(m));
        } else if (name.endsWith("Store")) {
            out.printf(", int dispToLocalSlot, %s value", getLastParameterNameHandlingObject(m));
        } else if (name.contains("New")) {
            out.print(", long objId, String className, long clId");
            if (name.contains("NewArray")) {
                out.print(", int length");
            }
        } else if (name.endsWith("If")) {
            out.print(", int opcode, ");
            String lastParam = getLastParameterName(m);
            if (lastParam.equals("Object")) {
                out.print("long objId1, long objId2");
            } else {
                out.print("int op1, int op2");
            }
        } else if (name.endsWith("Return")) {
            if (m.getParameterTypes().length > 0) {
                out.printf(", %s value", getLastParameterNameHandlingObject(m));
            }
        } else if (name.contains("Invoke")) {
            out.print(", long objId, String className, long clId, String methodName");
        } else if (name.endsWith("ArrayLength")) {
            out.print(", long objId, int length");
        } else if (name.contains("Monitor") || name.contains("Throw")) {
            out.print(", long objId");
        } else if (name.contains("CheckCast") || name.contains("InstanceOf")) {
            out.print(", long objId, String className, long clId");
        } else if (name.contains("Thread")) {
            // drop VmThread arg
        } else {
            Class<?>[] params = m.getParameterTypes();
            int argc = 1;
            for (Class<?> param : params) {
                out.printf(", %s %s", param.getSimpleName(), "arg" + argc);
                argc++;
            }
        }
        out.printf(");%n%n");
    }

    private static String getLastParameterNameHandlingObject(Method m) {
        String result = getLastParameterName(m);
        if (result.equals("Object")) {
            result = "long";
        }
        return result;
    }

    public static String getMethodNameRenamingObject(Method m) {
        String result = m.getName();
        if (result.contains("PutStatic") || result.contains("PutField") ||
            result.contains("GetStatic") || result.contains("GetField") ||
            result.endsWith("Load") || result.endsWith("Store") || result.endsWith("Return") || result.endsWith("If")) {
            String lastParam = getLastParameterName(m);
            if (lastParam != null && lastParam.equals("Object")) {
                result += "Object";
            }
        }
        return result;
    }

}
