/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.max.vm.actor.member.ClassMethodActor;
import com.sun.max.vm.value.Value;

/**
 * This interface represents a visitor of Java frames on a stack.
 */
public interface JavaFrameVisitor {

    DebugValueVisitor getDebugValueVisitor();

    /**
     * This method is called by each frame as it is visited to enumerate the Java stack frames corresponding
     * to the VM-level call stack. This method is called for each frame, from the topmost frame down.
     * The format of the debug values, if they are generated is as follows:
     *
     * @param classMethodActor the method actor corresponding to the Java method
     * @param bci the bytecode index in the method actor
     * @return {@code true} if the stack walk should continue
     */
    boolean visit(ClassMethodActor classMethodActor, int bci);

    public interface DebugValueVisitor {
        void visitLocal(int index, Value value);
        void visitStack(int index, Value value);
        void visitMonitor(int number, Value value);
    }
}
