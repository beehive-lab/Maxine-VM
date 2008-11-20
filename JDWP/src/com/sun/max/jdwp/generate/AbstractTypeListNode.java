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
        for (final Iterator it = _components.iterator(); it.hasNext();) {
            final TypeNode tn = (TypeNode) it.next();

            tn.genJavaDeclaration(writer, depth);
        }
    }

    void genJavaReads(PrintWriter writer, int depth) {
        for (final Iterator it = _components.iterator(); it.hasNext();) {
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
        for (Node node : _components) {
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
        for (final Iterator it = _components.iterator(); it.hasNext();) {
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
        for (final Iterator it = _components.iterator(); it.hasNext();) {
            final TypeNode tn = (TypeNode) it.next();
            sb.append(tn.javaParam());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    void genJavaWrites(PrintWriter writer, int depth) {
        for (Node node : _components) {
            final TypeNode tn = (TypeNode) node;
            tn.genJavaWrite(writer, depth, tn.fieldName());
        }
    }

    void genJavaToString(PrintWriter writer, int depth) {
        int z = 0;
        for (Node node : _components) {
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
        for (Node node : _components) {
            final TypeNode tn = (TypeNode) node;
            indent(writer, depth + 1);
            writer.println("this." + tn.fieldName() + " = " + tn.name() + ";");
        }
        indent(writer, depth);
        writer.println("}");
    }
}
