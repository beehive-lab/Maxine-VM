/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.jdwp.generate;

import java.io.*;
import java.util.*;

/**
 * @author JDK7: jdk/make/tools/src/build/tools/jdwpgen
 * @author Thomas Wuerthinger
 */
class AltNode extends AbstractGroupNode implements TypeNode {

    SelectNode select;

    @Override
    void constrain(Context ctx) {
        super.constrain(ctx);

        if (!(nameNode() instanceof NameValueNode)) {
            error("Alt name must have value: " + nameNode());
        }
        if (parent instanceof SelectNode) {
            select = (SelectNode) parent;
        } else {
            error("Alt must be in Select");
        }
    }

    @Override
    public String getAttributes() {
        return "@Override";
    }

    @Override
    String javaClassImplements() {
        return " extends " + select.commonBaseClass();
    }

    @Override
    void genJavaClassSpecifics(PrintWriter writer, int depth) {
        indent(writer, depth);
        writer.print("public static final " + select.typeNode().javaType());
        writer.println(" ALT_ID = " + nameNode().value() + ";");
        if (context.isWritingCommand()) {
            genJavaCreateMethod(writer, depth);
        } else {
            indent(writer, depth);
            writer.println("@Override");
            indent(writer, depth);
            writer.println("public " + select.typeNode().javaParam() + "() {");
            indent(writer, depth + 1);
            writer.println("return ALT_ID;");
            indent(writer, depth);
            writer.println("}");
        }
        super.genJavaClassSpecifics(writer, depth);
    }

    @Override
    void genJavaWriteMethod(PrintWriter writer, int depth) {
        genJavaWriteMethod(writer, depth, "");
    }

    void genJavaReadsSelectCase(PrintWriter writer, int depth, String common) {
        indent(writer, depth);
        writer.println("case " + nameNode().value() + ":");
        indent(writer, depth + 1);
        writer.println(common + " = new " + name() + "();");
        indent(writer, depth + 1);
        writer.println("break;");
    }

    void genJavaCreateMethod(PrintWriter writer, int depth) {

        indent(writer, depth);
        writer.print("public static " + select.name() + " create(");
        writer.print(javaParams());
        writer.println(") {");
        indent(writer, depth + 1);
        writer.print("return new " + select.name() + "(");
        writer.print("ALT_ID, new " + javaClassName() + "(");
        for (final Iterator it = components.iterator(); it.hasNext();) {
            final TypeNode tn = (TypeNode) it.next();
            writer.print(tn.name());
            if (it.hasNext()) {
                writer.print(", ");
            }
        }
        writer.println("));");
        indent(writer, depth);
        writer.println("}");
    }

}
