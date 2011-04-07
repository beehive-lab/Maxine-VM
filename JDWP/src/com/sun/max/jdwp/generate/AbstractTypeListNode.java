/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
abstract class AbstractTypeListNode extends AbstractNamedNode {

    @Override
    void constrainComponent(Context ctx, Node node) {
        if (node instanceof TypeNode) {
            node.constrain(ctx);
        } else {
            error("Expected type descriptor item, got: " + node);
        }
    }

    void genJavaClassBodyComponents(PrintWriter writer, int depth) {
        for (final Iterator it = components.iterator(); it.hasNext();) {
            final TypeNode tn = (TypeNode) it.next();

            tn.genJavaDeclaration(writer, depth);
        }
    }

    void genJavaReads(PrintWriter writer, int depth) {
        for (final Iterator it = components.iterator(); it.hasNext();) {
            final TypeNode tn = (TypeNode) it.next();
            tn.genJavaRead(writer, depth, tn.fieldName());
        }
    }

    public String getAttributes() {
        return "";
    }

    void genJavaReadingClassBody(PrintWriter writer, int depth, String className) {
        genJavaClassBodyComponents(writer, depth);
        // if (!context.inEvent()) {
        // private when event?
        // }

        indent(writer, depth);
        writer.print("public ");
        writer.print(className + "(");
        printJavaParams(writer, depth + 1);
        writer.println(") {");
        int z = 0;
        for (Node node : components) {
            final TypeNode tn = (TypeNode) node;
            indent(writer, depth + 1);
            writer.println("this." + tn.fieldName() + " = " + tn.name() + ";");
            z++;
        }
        indent(writer, depth);
        writer.println("}");

        if (z > 0) {
            indent(writer, depth);
            writer.print("public ");
            writer.println(className + "() {");
            indent(writer, depth);
            writer.println("}");
        }

        if (getAttributes().length() > 0) {
            indent(writer, depth);
            writer.println(getAttributes());
        }
        indent(writer, depth);
        writer.println("public void read(JDWPInputStream ps) throws java.io.IOException, JDWPException {");
        genJavaReads(writer, depth + 1);
        indent(writer, depth);
        writer.println("}");

        if (getAttributes().length() > 0) {
            indent(writer, depth);
            writer.println(getAttributes());
        }
        indent(writer, depth);
        writer.println("public void write(JDWPOutputStream ps) throws java.io.IOException {");
        genJavaWrites(writer, depth + 1);
        indent(writer, depth);
        writer.println("}");

        indent(writer, depth);
        writer.println("@Override");
        indent(writer, depth);
        writer.println("public String toString() {");
        indent(writer, depth + 1);
        writer.println("final StringBuilder stringBuilder = new StringBuilder();");
        genJavaToString(writer, depth + 1);
        indent(writer, depth + 1);
        writer.println("return stringBuilder.toString();");
        indent(writer, depth);
        writer.println("}");
    }

    private void printJavaParams(PrintWriter writer, int depth) {
        for (final Iterator it = components.iterator(); it.hasNext();) {
            final TypeNode tn = (TypeNode) it.next();
            writer.print(tn.javaParam());
            if (it.hasNext()) {
                writer.println(",");
                indent(writer, depth);
            }
        }
    }

    String javaParams() {
        final StringBuffer sb = new StringBuffer();
        for (final Iterator it = components.iterator(); it.hasNext();) {
            final TypeNode tn = (TypeNode) it.next();
            sb.append(tn.javaParam());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    void genJavaWrites(PrintWriter writer, int depth) {
        for (Node node : components) {
            final TypeNode tn = (TypeNode) node;
            tn.genJavaWrite(writer, depth, tn.fieldName());
        }
    }

    void genJavaToString(PrintWriter writer, int depth) {
        int z = 0;
        for (Node node : components) {
            if (z != 0) {
                indent(writer, depth);
                writer.println("stringBuilder.append(\", \");");
            }
            final TypeNode tn = (TypeNode) node;
            tn.genJavaToString(writer, depth, tn.fieldName());
            z++;
        }
    }

    void genJavaWritingClassBody(PrintWriter writer, int depth, String className) {
        genJavaClassBodyComponents(writer, depth);
        writer.print(className + "(");
        printJavaParams(writer, depth + 1);
        writer.println(") {");
        for (Node node : components) {
            final TypeNode tn = (TypeNode) node;
            indent(writer, depth + 1);
            writer.println("this." + tn.fieldName() + " = " + tn.name() + ";");
        }
        indent(writer, depth);
        writer.println("}");
    }
}
