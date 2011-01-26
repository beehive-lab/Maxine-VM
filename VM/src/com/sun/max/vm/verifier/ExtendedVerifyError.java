/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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

    public final ClassMethodActor classMethodActor;
    public final CodeAttribute codeAttribute;
    public final int position;

    public ExtendedVerifyError(String detailMessage, ClassMethodActor classMethodActor, CodeAttribute codeAttribute, int position) {
        super(detailMessage);
        this.classMethodActor = classMethodActor;
        this.codeAttribute = codeAttribute;
        this.position = position;
    }

    @Override
    public synchronized ExtendedVerifyError initCause(Throwable cause) {
        super.initCause(cause);
        return this;
    }

    /**
     * Prefixes the detailed message describing this verification error with the name of the method and
     * the bytecode position at which the error occurred.
     */
    public String getContextualMessage() {
        final StringBuilder sb = new StringBuilder();
        if (position != -1) {
            sb.append(" at offset ").append(position);
        }
        if (classMethodActor != null) {
            sb.append(" in method ").append(classMethodActor.format("%H.%n(%p)"));
        }
        if (sb.length() != 0) {
            sb.insert(0, "Verification error").append(": ");
        }
        sb.append(super.getMessage());
        return sb.toString();
    }

    public void printCode(PrintStream out) {
        CodeAttributePrinter.print(out, codeAttribute);
    }

    @Override
    public String toString() {
        return "VerifyError: " + getMessage();
    }
}
