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
import com.sun.max.jdwp.vm.data.*;

/**
 * Class representing a thread in the VM.

 *
 */
public interface ThreadProvider extends ObjectProvider {

    /**
     * @return the display name of the thread
     */
    @ConstantReturnValue
    String getName();

    /**
     * Increases the thread suspend count. If the count is positive, the thread is suspended.
     */
    void suspend();

    /**
     * Decreases the thread suspend count. If the count is zero or positive, the thread is resumed.
     */
    void resume();

    /**
     * This method may only be called when the thread is currently suspended.
     *
     * @return the current frames of the thread
     */
    FrameProvider[] getFrames();

    /**
     * Stops this thread with an asynchronous exception.
     *
     * @param exception the exception that stops the thread
     */
    void stop(ObjectProvider exception);

    /**
     * Interrupts the thread.
     */
    void interrupt();

    /**
     * @return the current suspend count of the thread
     */
    int suspendCount();

    /**
     * Schedules a single step when the thread is resumed the next time. After this method is called once, the thread must be resumed.
     */
    void doSingleStep();

    /**
     * Schedules a step out when the thread is resumed the next time. After this method is called once, the thread must be resumed.
     */
    void doStepOut();

    /**
     * Gets the thread group that this thread is a child of.
     *
     * @return the thread group of the thread
     */
    ThreadGroupProvider getThreadGroup();

    /**
     * Convenience method for accessing only a specific frame of the thread without retrieving the whole array as by the getFrames method.
     *
     * @param depth
     * @return
     */
    @JDWPPlus
    FrameProvider getFrame(int depth);

    /**
     * Calling this method is only valid when the thread is currently suspended. Retrieves the register groups of the thread and the current values of all registers.
     *
     * @return the current values of all registers
     */
    @JDWPPlus
    RegistersGroup getRegistersGroup();

    /**
     * @return the VM object of the VM in which this thread runs
     */
    @ConstantReturnValue
    @JDWPPlus
    VMAccess getVM();
}
