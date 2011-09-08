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
public abstract class Node {

    String kind;
    List<Node> components;
    int lineno;
    List<String> commentList = new ArrayList<String>();
    Node parent = null;
    Context context;

    void set(String kind, List<Node> components, int lineno) {
        this.kind = kind;
        this.components = components;
        this.lineno = lineno;
    }

    void parentAndExtractComments() {
        for (final Iterator it = components.iterator(); it.hasNext();) {
            final Node node = (Node) it.next();
            if (node instanceof CommentNode) {
                it.remove();
                commentList.add(((CommentNode) node).text());
            } else {
                node.parent = this;
                node.parentAndExtractComments();
            }
        }
    }

    void prune() {
        for (Node node : components) {
            node.prune();
        }
    }

    void constrain(Context ctx) {
        context = ctx;
        for (Node node : components) {
            constrainComponent(ctx, node);
        }
    }

    void constrainComponent(Context ctx, Node node) {
        node.constrain(ctx);
    }

    void indent(PrintWriter writer, int depth) {
        for (int i = depth; i > 0; --i) {
            writer.print("    ");
        }
    }

    void documentIndex(PrintWriter writer) {
    }

    String comment() {
        final StringBuffer comment = new StringBuffer();
        for (String st : commentList) {
            comment.append(st);
        }
        return comment.toString();
    }

    public String javaType() {
        return "-- WRONG ---";
    }

    void genJava(PrintWriter writer, int depth) {
        for (Node node : components) {
            node.genJava(writer, depth);
        }
    }

    public void genJavaRead(PrintWriter writer, int depth, String readLabel) {
        error("Internal - Should not call Node.genJavaRead()");
    }

    public void genJavaPreDef(PrintWriter writer, int depth) {
        for (Node node : components) {
            node.genJavaPreDef(writer, depth);
        }
    }

    void error(String errmsg) {
        System.err.println();
        System.err.println("Error:" + lineno + ": " + kind + " - " + errmsg);
        System.err.println();
        System.exit(1);
    }
}
