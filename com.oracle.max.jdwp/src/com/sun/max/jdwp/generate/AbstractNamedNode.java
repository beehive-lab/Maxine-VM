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
public abstract class AbstractNamedNode extends Node {

    private NameNode nameNode;
    private String name;

    public String name() {
        return name;
    }

    public NameNode nameNode() {
        return nameNode;
    }

    public String fieldName() {
        return name();
    }

    @Override
    void prune() {
        final Iterator it = components.iterator();

        if (it.hasNext()) {
            final Node curNameNode = (Node) it.next();

            if (curNameNode instanceof NameNode) {
                this.nameNode = (NameNode) curNameNode;
                this.name = this.nameNode.text();
                it.remove();
            } else {
                error("Bad name: " + name);
            }
        } else {
            error("empty");
        }
        super.prune();
    }

    @Override
    void constrain(Context ctx) {
        nameNode.constrain(ctx);
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
        for (final Iterator it = components.iterator(); it.hasNext();) {
            ((Node) it.next()).genJava(writer, depth + 1);
        }
        indent(writer, depth);
        writer.println("}");
    }
}
