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
