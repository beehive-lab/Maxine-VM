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
/*VCSID=02c85768-ef37-404c-aecc-77c35d4ee35d*/

package com.sun.max.jdwp.generate;

import java.io.*;
import java.util.*;

/**
 * @author JDK7: jdk/make/tools/src/build/tools/jdwpgen
 * @author Thomas Wuerthinger
 */
class AltNode extends AbstractGroupNode implements TypeNode {

    SelectNode _select;

    @Override
    void constrain(Context ctx) {
        super.constrain(ctx);

        if (!(nameNode() instanceof NameValueNode)) {
            error("Alt name must have value: " + nameNode());
        }
        if (_parent instanceof SelectNode) {
            _select = (SelectNode) _parent;
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
        return " extends " + _select.commonBaseClass();
    }

    @Override
    void genJavaClassSpecifics(PrintWriter writer, int depth) {
        indent(writer, depth);
        writer.print("public static final " + _select.typeNode().javaType());
        writer.println(" ALT_ID = " + nameNode().value() + ";");
        if (_context.isWritingCommand()) {
            genJavaCreateMethod(writer, depth);
        } else {
            indent(writer, depth);
            writer.println("@Override");
            indent(writer, depth);
            writer.println("public " + _select.typeNode().javaParam() + "() {");
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
        writer.print("public static " + _select.name() + " create(");
        writer.print(javaParams());
        writer.println(") {");
        indent(writer, depth + 1);
        writer.print("return new " + _select.name() + "(");
        writer.print("ALT_ID, new " + javaClassName() + "(");
        for (final Iterator it = _components.iterator(); it.hasNext();) {
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
