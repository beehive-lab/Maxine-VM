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

/**
 * @author JDK7: jdk/make/tools/src/build/tools/jdwpgen
 * @author Thomas Wuerthinger
 */
class Context {

    static final int OUTER = 0;
    static final int READING_REPLY = 1;
    static final int WRITING_COMMAND = 2;

    final String whereJava;
    final String whereC;

    int state = OUTER;
    private boolean inEvent = false;

    Context() {
        whereJava = "";
        whereC = "";
    }

    private Context(String whereJava, String whereC) {
        this.whereJava = whereJava;
        this.whereC = whereC;
    }

    Context subcontext(String level) {
        Context ctx;
        if (whereC.length() == 0) {
            ctx = new Context(level, level);
        } else {
            ctx = new Context(whereJava + "." + level, whereC + "_" + level);
        }
        ctx.state = state;
        ctx.inEvent = inEvent;
        return ctx;
    }

    private Context cloneContext() {
        final Context ctx = new Context(whereJava, whereC);
        ctx.state = state;
        ctx.inEvent = inEvent;
        return ctx;
    }

    Context replyReadingSubcontext() {
        final Context ctx = cloneContext();
        ctx.state = READING_REPLY;
        return ctx;
    }

    Context commandWritingSubcontext() {
        final Context ctx = cloneContext();
        ctx.state = WRITING_COMMAND;
        return ctx;
    }

    Context inEventSubcontext() {
        final Context ctx = cloneContext();
        ctx.inEvent = true;
        return ctx;
    }

    boolean inEvent() {
        return inEvent;
    }

    boolean isWritingCommand() {
        return state == WRITING_COMMAND;
    }

    boolean isReadingReply() {
        return state == READING_REPLY;
    }
}
