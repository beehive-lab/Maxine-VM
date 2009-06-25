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
class ReplyNode extends AbstractTypeListNode {

    String cmdName;

    @Override
    void set(String kind, List<Node> components, int lineno) {
        super.set(kind, components, lineno);
        components.add(0, new NameNode(kind));
    }

    @Override
    void constrain(Context ctx) {
        super.constrain(ctx.replyReadingSubcontext());
        final CommandNode cmd = (CommandNode) parent;
        cmdName = cmd.name();
    }

    @Override
    void genJava(PrintWriter writer, int depth) {
        genJavaPreDef(writer, depth);
        super.genJava(writer, depth);
        writer.println();

        indent(writer, depth);
        writer.println("public static class Reply implements OutgoingData {");

        indent(writer, depth + 1);
        writer.println("public byte getCommandId() { return COMMAND; }");
        indent(writer, depth + 1);
        writer.println("public byte getCommandSetId() { return COMMAND_SET; }");
        genJavaReadingClassBody(writer, depth + 1, "Reply");
        indent(writer, depth);
        writer.write("}");
        writer.println();
    }
}
