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

    final String _whereJava;
    final String _whereC;

    int _state = OUTER;
    private boolean _inEvent = false;

    Context() {
        _whereJava = "";
        _whereC = "";
    }

    private Context(String whereJava, String whereC) {
        this._whereJava = whereJava;
        this._whereC = whereC;
    }

    Context subcontext(String level) {
        Context ctx;
        if (_whereC.length() == 0) {
            ctx = new Context(level, level);
        } else {
            ctx = new Context(_whereJava + "." + level, _whereC + "_" + level);
        }
        ctx._state = _state;
        ctx._inEvent = _inEvent;
        return ctx;
    }

    private Context cloneContext() {
        final Context ctx = new Context(_whereJava, _whereC);
        ctx._state = _state;
        ctx._inEvent = _inEvent;
        return ctx;
    }

    Context replyReadingSubcontext() {
        final Context ctx = cloneContext();
        ctx._state = READING_REPLY;
        return ctx;
    }

    Context commandWritingSubcontext() {
        final Context ctx = cloneContext();
        ctx._state = WRITING_COMMAND;
        return ctx;
    }

    Context inEventSubcontext() {
        final Context ctx = cloneContext();
        ctx._inEvent = true;
        return ctx;
    }

    boolean inEvent() {
        return _inEvent;
    }

    boolean isWritingCommand() {
        return _state == WRITING_COMMAND;
    }

    boolean isReadingReply() {
        return _state == READING_REPLY;
    }
}
