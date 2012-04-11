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
package com.sun.max.vm.stack;

import com.sun.max.unsafe.*;

/**
 * Carries context that may be necessary while unwinding a stack for exception handling.
 * The information that needs to be carried, beside the exception object, is platform-dependent.
 * Platforms that need additional information must extend this class.
 */
public class StackUnwindingContext {
    /**
     * The cause of the stack unwinding.
     */
    public Throwable throwable;
    public Word stackPointer;
    /**
     * When {@code true} means walk is only checking for a handler.
     */
    public boolean checking;
    /**
     * Only set when {@code checking == true}, indicates the cursor of the handler frame, if found.
     */
    public StackFrameCursor handlerCursor;

    public StackUnwindingContext() {
    }

    public StackUnwindingContext(Word stackPointer, Throwable throwable) {
        this.throwable = throwable;
        this.stackPointer = stackPointer;
    }
}
