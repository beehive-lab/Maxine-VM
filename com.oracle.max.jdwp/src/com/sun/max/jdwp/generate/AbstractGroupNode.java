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

/**
 * @author JDK7: jdk/make/tools/src/build/tools/jdwpgen
 * @author Thomas Wuerthinger
 */
abstract class AbstractGroupNode extends AbstractTypeListNode {

    @Override
    public String javaType() {
        return name();
    }

    @Override
    void genJava(PrintWriter writer, int depth) {
        genJavaClass(writer, depth);
    }

    void genJavaWriteMethod(PrintWriter writer, int depth) {
        genJavaWriteMethod(writer, depth, "private ");
    }

    void genJavaWriteMethod(PrintWriter writer, int depth, String modifier) {
        writer.println();
        if (this.getAttributes().length() > 0) {
            indent(writer, depth);
            writer.println(this.getAttributes());
        }
        indent(writer, depth);
        writer.print(modifier);
        writer.println("void write(JDWPOutputStream ps) {");
        genJavaWrites(writer, depth + 1);
        indent(writer, depth);
        writer.println("}");
    }

    @Override
    void genJavaClassSpecifics(PrintWriter writer, int depth) {
        switch (context.state) {
            case Context.READING_REPLY:
                genJavaReadingClassBody(writer, depth, name());
                break;

            case Context.WRITING_COMMAND:
                genJavaReadingClassBody(writer, depth, name());
                break;

            default:
                error("Group in outer");
                break;
        }
    }

    public void genJavaDeclaration(PrintWriter writer, int depth) {
        writer.println();
        indent(writer, depth);
        writer.print("final ");
        writer.print(name());
        writer.print(" a" + name());
        writer.println(";");
    }

    public String javaParam() {
        return name() + " a" + name();
    }

    public void genJavaWrite(PrintWriter writer, int depth, String writeLabel) {
        indent(writer, depth);
        writer.println(writeLabel + ".write(ps);");
    }

    public void genJavaToString(PrintWriter writer, int depth, String writeLabel) {
        indent(writer, depth);
        writer.println("stringBuilder.append(\"" + writeLabel + "=\" + " + writeLabel + ");");
    }

    String javaRead() {
        error("Internal - Should not call AbstractGroupNode.javaRead()");
        return "";
    }

    @Override
    public void genJavaRead(PrintWriter writer, int depth, String readLabel) {
        indent(writer, depth);
        writer.print(readLabel);
        writer.print(" = new ");
        writer.print(name());
        writer.println("();");
        indent(writer, depth);
        writer.print(readLabel);
        writer.print(".read(ps);");
        writer.println();
    }
}
