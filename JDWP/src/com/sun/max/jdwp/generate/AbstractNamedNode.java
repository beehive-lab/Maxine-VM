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
/*VCSID=1454492c-eba8-48e4-90d2-00c57f5f2e51*/

package com.sun.max.jdwp.generate;

import java.io.*;
import java.util.*;

/**
 * @author JDK7: jdk/make/tools/src/build/tools/jdwpgen
 * @author Thomas Wuerthinger
 */
public abstract class AbstractNamedNode extends Node {

    private NameNode _nameNode;
    private String _name;

    public String name() {
        return _name;
    }

    public NameNode nameNode() {
        return _nameNode;
    }

    public String fieldName() {
        return "_" + name();
    }

    @Override
    void prune() {
        final Iterator it = _components.iterator();

        if (it.hasNext()) {
            final Node curNameNode = (Node) it.next();

            if (curNameNode instanceof NameNode) {
                this._nameNode = (NameNode) curNameNode;
                this._name = this._nameNode.text();
                it.remove();
            } else {
                error("Bad name: " + _name);
            }
        } else {
            error("empty");
        }
        super.prune();
    }

    @Override
    void constrain(Context ctx) {
        _nameNode.constrain(ctx);
        super.constrain(ctx.subcontext(name()));
    }

    String javaClassName() {
        return name();
    }

    void genJavaClassSpecifics(PrintWriter writer, int depth) {
    }

    String javaClassImplements() {
        return ""; // does not implement anything, by default
    }

    void genJavaClass(PrintWriter writer, int depth) {
        writer.println();
        indent(writer, depth);
        writer.print("public ");
        if (depth != 0) {
            writer.print("static ");
        } else {
            writer.print("final ");
        }
        writer.print("class " + javaClassName());
        writer.println(javaClassImplements() + " {");
        genJavaClassSpecifics(writer, depth + 1);
        for (final Iterator it = _components.iterator(); it.hasNext();) {
            ((Node) it.next()).genJava(writer, depth + 1);
        }
        indent(writer, depth);
        writer.println("}");
    }
}
