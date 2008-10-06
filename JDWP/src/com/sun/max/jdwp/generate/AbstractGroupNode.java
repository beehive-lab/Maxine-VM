/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
/*VCSID=5eb3b108-d765-4eef-a5e6-5c5d1a41e9b7*/

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
        switch (_context._state) {
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
