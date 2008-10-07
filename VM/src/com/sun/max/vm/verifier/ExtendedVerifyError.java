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
package com.sun.max.vm.verifier;

import java.io.*;

import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.classfile.*;

/**
 * A verification error that contains information the location in a {@linkplain ClassMethodActor method} that caused the error.
 *
 * @author Doug Simon
 */
public class ExtendedVerifyError extends VerifyError {

    private final ClassMethodActor _classMethodActor;
    private final CodeAttribute _codeAttribute;
    private final int _position;

    public ExtendedVerifyError(String detailMessage, ClassMethodActor classMethodActor, CodeAttribute codeAttribute, int position) {
        super(detailMessage);
        _classMethodActor = classMethodActor;
        _codeAttribute = codeAttribute;
        _position = position;
    }

    @Override
    public synchronized ExtendedVerifyError initCause(Throwable cause) {
        super.initCause(cause);
        return this;
    }

    public ClassMethodActor classMethodActor() {
        return _classMethodActor;
    }

    public CodeAttribute codeAttribute() {
        return _codeAttribute;
    }

    public int position() {
        return _position;
    }

    /**
     * Prefixes the detailed message describing this verification error with the name of the method and
     * the bytecode position at which the error occurred.
     */
    public String getContextualMessage() {
        final StringBuilder sb = new StringBuilder();
        if (_position != -1) {
            sb.append(" at offset ").append(_position);
        }
        if (_classMethodActor != null) {
            sb.append(" in method ").append(classMethodActor().format("%H.%n(%p)"));
        }
        if (sb.length() != 0) {
            sb.insert(0, "Verification error").append(": ");
        }
        sb.append(super.getMessage());
        return sb.toString();
    }

    public void printCode(PrintStream out) {
        CodeAttributePrinter.print(out, _classMethodActor.rawCodeAttribute());
    }

    @Override
    public String toString() {
        return "VerifyError: " + getMessage();
    }
}
