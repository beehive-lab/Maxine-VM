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
package com.sun.max.jdwp.vm.proxy;

import com.sun.max.jdwp.vm.core.*;
import com.sun.max.jdwp.vm.data.*;

/**
 * Class representing a thread in the VM.

 * @author Thomas Wuerthinger
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
