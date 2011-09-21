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
package com.oracle.max.vm.ext.t1x;

import java.io.*;
import java.util.*;

import com.sun.cri.ci.*;
import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.actor.member.*;

/**
 * Generates Java code that wraps an intrinsic method. It emits the correct {@link Slot} annotations and makes sure
 * the parameter and return types are the ones supported by T1X.
 */
public class T1XIntrinsicTemplateGenerator {
    /**
     * The stream to use for the generated output.
     */
    public final PrintStream out;

    private T1XIntrinsicTemplateGenerator(PrintStream out) {
        this.out = out;
    }


    public static String templateInvokerName(ClassMethodActor method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.holder().javaSignature(true)).append("$").append(method.name()).append('$');
        for (int i = 0; i < method.signature().argumentCount(false); i++) {
            sb.append(method.signature().argumentKindAt(i).typeChar);
        }
        return sb.toString().replace('.', '_');
    }

    protected static class Param {
        protected String name;
        protected ClassActor type;
        protected int slot;
    }

    private String tn(ClassActor type) {
        if (type.kind().stackKind() != type.kind()) {
            return type.kind().stackKind().javaName;
        } else {
            return type.javaSignature(true);
        }
    }

    private void generate() {
        List<ClassMethodActor> methods = T1X.intrinsicTemplateMethods();
        Collections.sort(methods, new Comparator<ClassMethodActor>() {
            @Override
            public int compare(ClassMethodActor o1, ClassMethodActor o2) {
                return templateInvokerName(o1).compareTo(templateInvokerName(o2));
            }
        });

        for (ClassMethodActor method : methods) {
            generate(method);
        }
    }

    private void generate(ClassMethodActor method) {
        String methodName = templateInvokerName(method);
        ClassActor returnType = (ClassActor) method.signature().returnType(method.holder());
        boolean isStatic = method.isStatic();
        int firstRealParam = isStatic ? 0 : 1;
        Param[] params = new Param[method.signature().argumentCount(!isStatic)];

        // reverse loop order is necessary to compute the correct slot indices
        int slot = 0;
        for (int i = params.length - 1; i >= firstRealParam; i--) {
            Param param = new Param();
            param.type = (ClassActor) method.signature().argumentTypeAt(i - firstRealParam, method.holder());
            param.name = "param" + i;
            param.slot = slot;
            slot += param.type.kind().sizeInSlots();
            params[i] = param;
        }
        if (!isStatic) {
            Param param = new Param();
            param.name = "param" + 0;
            param.type = method.holder();
            param.slot = slot;
            params[0] = param;
        }

        // Line: Annotation before the method declaration
        out.printf("    @T1X_INTRINSIC_TEMPLATE%n");

        // Line: Method declaration
        out.printf("    public static %s %s(", tn(returnType), methodName);
        for (int i = 0; i < params.length; i++) {
            Param param = params[i];
            if (i != 0) {
                out.print(", ");
            }
            out.printf("@Slot(%d) %s %s", param.slot, tn(param.type), param.name);
        }
        out.printf(") {%n");

        // Line: call of the intrinsic method
        out.printf("        ");
        if (returnType.kind() != CiKind.Void) {
            out.print("return ");
        }
        if (returnType.kind() == CiKind.Boolean) {
            out.print("UnsafeCast.asInt(");
        }
        if (isStatic) {
            out.print(tn(method.holder()));
        } else {
            out.print(params[0].name);
        }
        out.printf(".%s(", method.name());
        for (int i = firstRealParam; i < params.length; i++) {
            Param param = params[i];
            if (i != firstRealParam) {
                out.print(", ");
            }
            if (param.type.kind().stackKind() != param.type.kind()) {
                out.printf("(%s) ", param.type.kind().javaName);
            }
            out.print(param.name);
        }
        if (returnType.kind() == CiKind.Boolean) {
            out.print(")");
        }
        out.printf(");%n");

        // Line: closing of method
        out.printf("    }%n%n");
    }

    /**
     * Inserts or updates generated source into {@code target}.
     *
     * @return {@code true} if {@code target} was modified; {@code false} otherwise
     */
    public static boolean generate(Class target) throws Exception {
        File base = new File(JavaProject.findWorkspaceDirectory(), "com.oracle.max.vm.ext.t1x/src");
        File outputFile = new File(base, target.getName().replace('.', File.separatorChar) + ".java").getAbsoluteFile();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);

        new T1XIntrinsicTemplateGenerator(out).generate();
        ReadableSource content = ReadableSource.Static.fromString(baos.toString());
        return Files.updateGeneratedContent(outputFile, content, "// START GENERATED CODE", "// END GENERATED CODE", false);
    }
}
