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

/**
 * @author JDK7: jdk/make/tools/src/build/tools/jdwpgen
 * @author Thomas Wuerthinger
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
