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
/*VCSID=855b58d6-e2cc-4f2c-845d-76a29067e270*/
package com.sun.max.jdwp.generate;

import java.io.*;

/**
 * @author JDK7: jdk/make/tools/src/build/tools/jdwpgen
 * @author Thomas Wuerthinger
 */
class RepeatNode extends AbstractTypeNode {

    Node _member;

    @Override
    void constrain(Context ctx) {
        super.constrain(ctx);
        if (_components.size() != 1) {
            error("Repeat must have exactly one member, use Group for more");
        }
        _member = _components.get(0);
        if (!(_member instanceof TypeNode)) {
            error("Repeat member must be type specifier");
        }
    }

    @Override
    String docType() {
        return "-BOGUS-"; // should never call this
    }

    @Override
    public String javaType() {
        return _member.javaType() + "[]";
    }

    @Override
    public void genJavaWrite(PrintWriter writer, int depth, String writeLabel) {
        indent(writer, depth);
        writer.println("ps.write(" + writeLabel + ".length);");
        indent(writer, depth);
        writer.println("for (int i = 0; i < " + writeLabel + ".length; i++) {");
        ((TypeNode) _member).genJavaWrite(writer, depth + 1, writeLabel + "[i]");
        indent(writer, depth);
        writer.println("}");
    }

    @Override
    public void genJavaToString(PrintWriter writer, int depth, String writeLabel) {
        indent(writer, depth);
        writer.println("stringBuilder.append(\"" + writeLabel + "=[\" + " + writeLabel + ".length + \"]{\");");
        indent(writer, depth);
        writer.println("for (int i = 0; i < " + writeLabel + ".length; i++) {");
        indent(writer, depth + 1);
        writer.println("if (i != 0) { stringBuilder.append(\", \"); }");
        ((TypeNode) _member).genJavaToString(writer, depth + 1, writeLabel + "[i]");

        indent(writer, depth);
        writer.println("}");
        indent(writer, depth);
        writer.println("stringBuilder.append(\"}\");");
    }

    @Override
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
        writer.println(readLabel + " = new " + _member.javaType() + "[" + cntLbl + "];");
        indent(writer, depth);
        writer.println("for (int i = 0; i < " + cntLbl + "; i++) {");
        _member.genJavaRead(writer, depth + 1, readLabel + "[i]");
        indent(writer, depth);
        writer.println("}");
    }
}
