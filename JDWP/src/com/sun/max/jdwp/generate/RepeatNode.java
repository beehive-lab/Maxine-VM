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

/**
 * @author JDK7: jdk/make/tools/src/build/tools/jdwpgen
 * @author Thomas Wuerthinger
 */
class RepeatNode extends AbstractTypeNode {

    Node member;

    @Override
    void constrain(Context ctx) {
        super.constrain(ctx);
        if (components.size() != 1) {
            error("Repeat must have exactly one member, use Group for more");
        }
        member = components.get(0);
        if (!(member instanceof TypeNode)) {
            error("Repeat member must be type specifier");
        }
    }

    String docType() {
        return "-BOGUS-"; // should never call this
    }

    @Override
    public String javaType() {
        return member.javaType() + "[]";
    }

    public void genJavaWrite(PrintWriter writer, int depth, String writeLabel) {
        indent(writer, depth);
        writer.println("ps.write(" + writeLabel + ".length);");
        indent(writer, depth);
        writer.println("for (int i = 0; i < " + writeLabel + ".length; i++) {");
        ((TypeNode) member).genJavaWrite(writer, depth + 1, writeLabel + "[i]");
        indent(writer, depth);
        writer.println("}");
    }

    public void genJavaToString(PrintWriter writer, int depth, String writeLabel) {
        indent(writer, depth);
        writer.println("stringBuilder.append(\"" + writeLabel + "=[\" + " + writeLabel + ".length + \"]{\");");
        indent(writer, depth);
        writer.println("for (int i = 0; i < " + writeLabel + ".length; i++) {");
        indent(writer, depth + 1);
        writer.println("if (i != 0) { stringBuilder.append(\", \"); }");
        ((TypeNode) member).genJavaToString(writer, depth + 1, writeLabel + "[i]");

        indent(writer, depth);
        writer.println("}");
        indent(writer, depth);
        writer.println("stringBuilder.append(\"}\");");
    }

    String javaRead() {
        error("Internal - Should not call RepeatNode.javaRead()");
        return "";
    }

    @Override
    public void genJavaRead(PrintWriter writer, int depth, String readLabel) {
        String cntLbl = readLabel + "Count";
        if (cntLbl.startsWith("_")) {
            cntLbl = cntLbl.substring(1);
        }
        indent(writer, depth);
        writer.println("final int " + cntLbl + " = ps.readInt();");
        indent(writer, depth);
        writer.println(readLabel + " = new " + member.javaType() + "[" + cntLbl + "];");
        indent(writer, depth);
        writer.println("for (int i = 0; i < " + cntLbl + "; i++) {");
        member.genJavaRead(writer, depth + 1, readLabel + "[i]");
        indent(writer, depth);
        writer.println("}");
    }
}
