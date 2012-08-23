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
 */
class CommandNode extends AbstractNamedNode {

    @Override
    void constrain(Context ctx) {
        if (components.size() == 3) {
            final Node out = components.get(0);
            final Node reply = components.get(1);
            final Node error = components.get(2);
            if (!(out instanceof OutNode)) {
                error("Expected 'Out' item, got: " + out);
            }
            if (!(reply instanceof ReplyNode)) {
                error("Expected 'Reply' item, got: " + reply);
            }
            if (!(error instanceof ErrorSetNode)) {
                error("Expected 'ErrorSet' item, got: " + error);
            }
        } else if (components.size() == 1) {
            final Node evt = components.get(0);
            if (!(evt instanceof EventNode)) {
                error("Expected 'Event' item, got: " + evt);
            }
        } else {
            error("Command must have Out and Reply items or ErrorSet item");
        }
        super.constrain(ctx);
    }

    @Override
    void genJavaClassSpecifics(PrintWriter writer, int depth) {
        indent(writer, depth);

        writer.println("public static final byte COMMAND = " + nameNode().value() + ";");

        indent(writer, depth);
        writer.println("public abstract static class Handler implements CommandHandler<IncomingRequest, Reply> {");
        indent(writer, depth + 1);
        writer.println("public IncomingRequest createIncomingDataObject() { return new IncomingRequest(); }");
        indent(writer, depth + 1);
        writer.print("public int helpAtDecodingUntaggedValue(IncomingRequest incomingRequest) throws JDWPException {");
        writer.println("assert false : \"If this method can be called, it must be overwritten in subclasses!\"; return 0; }");
        indent(writer, depth + 1);
        writer.println("public Reply handle(IncomingRequest incomingRequest, JDWPSender replyChannel) throws JDWPException { return handle(incomingRequest); }");
        indent(writer, depth + 1);
        writer.println("public Reply handle(IncomingRequest incomingRequest) throws JDWPException { throw new JDWPNotImplementedException(); }");
        indent(writer, depth + 1);
        writer.println("public byte getCommandId() { return COMMAND; }");
        indent(writer, depth + 1);
        writer.println("public byte getCommandSetId() { return COMMAND_SET; }");
        indent(writer, depth);
        writer.println("}");

        if (components.size() == 1) {
            indent(writer, depth);
            writer.println("public static class IncomingRequest implements IncomingData {");
            indent(writer, depth + 1);
            writer.println("public void read(JDWPInputStream ps) throws JDWPException { }");
            indent(writer, depth);
            writer.println("}");
        }
    }

    @Override
    void genJava(PrintWriter writer, int depth) {
        genJavaClass(writer, depth);
    }
}
