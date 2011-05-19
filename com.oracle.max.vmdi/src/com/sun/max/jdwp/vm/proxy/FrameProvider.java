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
package com.sun.max.jdwp.vm.proxy;

import com.sun.max.jdwp.vm.core.*;

/**
 * Class representing a JDWP stack frame.
 *
 * @author Thomas Wuerthinger
 *
 */
public interface FrameProvider extends Provider {

    /**
     * @return the current location of the instruction pointer in this frame
     */
    JdwpCodeLocation getLocation();

    /**
     * Accesses the values on the stack.
     *
     * @param i the index of the accessed value
     * @return the stack value at the specified index
     */
    VMValue getValue(int i);

    /**
     * Modifies a value on the stack.
     *
     * @param i the index of the value that should be set
     * @param value the new value of the stack slot
     */
    void setValue(int i, VMValue value);

    /**
     * @return the thread of this stack frame
     */
    ThreadProvider getThread();

    /**
     * @return the object representing the instance of the java "this" object in the stack frame
     */
    ObjectProvider thisObject();

    /**
     * @return the target method whose call produced the stack frame
     */
    @ConstantReturnValue
    @JDWPPlus
    TargetMethodAccess getTargetMethodProvider();

    /**
     * @return the address of the instruction pointer in this frame
     */
    @JDWPPlus
    long getInstructionPointer();

    /**
     * @return the address of the frame pointer
     */
    @JDWPPlus
    long getFramePointer();

    /**
     * @return the address of the stack pointer
     */
    @JDWPPlus
    long getStackPointer();

    /**
     * @return the raw values on the stack
     */
    @JDWPPlus
    long[] getRawValues();
}
