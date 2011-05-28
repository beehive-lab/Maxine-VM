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
import com.sun.max.vm.t1x.*;

/**
 * Generates the {@link VMAdviceHandlerLog} interface that operates in terms of {@link String strings}
 * and scalar types suitable for saving to a file.
 *
 * One consequence of the type mapping is that the method signatures related to {@link Object} and
 * {@code long} map to the same, as {@link Object} is converted to a unique id. Rather than
 * qualify every method with a type name, we just qualify the {@link Object} case.
 *
 * The generating thread (name) is added as the first argument to all the methods.
 */
@HOSTED_ONLY
public class VMAdviceHandlerLogGenerator {

    public static void main(String[] args) {
        T1XTemplateGenerator.setGeneratingClass(VMAdviceHandlerLogGenerator.class);
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
        if (name.endsWith("GetField")  && getFieldDone || name.endsWith("GetStatic") && getStaticDone ||
            name.endsWith("ArrayLoad") && arrayLoadDone) {
            return;
        }
        generateAutoComment();
        out.printf("    public abstract void %s(String threadName", getMethodNameRenamingObject(m));
        if (name.endsWith("GetField")) {
            out.print(", long objId, String fieldName");
            getFieldDone = true;
        } else if (name.endsWith("PutField")) {
            out.printf(", long objId, String fieldName, %s value", getLastParameterNameHandlingObject(m));
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
        } else if (name.contains("New")) {
            out.print(", long objId, String className, long clId");
            if (name.contains("NewArray")) {
                out.print(", int length");
            }
        } else if (name.endsWith("InvokeSpecial")) {
            out.print(", long objId");
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
            result.contains("ArrayStore")) {
            String lastParam = getLastParameterName(m);
            if (lastParam.equals("Object")) {
                result += "Object";
            }
        }
        return result;
    }

}
