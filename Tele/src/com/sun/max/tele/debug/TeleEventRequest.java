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
package com.sun.max.tele.debug;


/**
 * A tele event request encapsulates some action that {@linkplain #execute() modifies} the execution
 * {@linkplain TeleProcess#state() state} of the tele process as well as some
 * {@linkplain TeleEventRequest#afterExecution(WaitResult) action} to take when the tele process next stops after the
 * request has been issued.
 *
 * @author Aritra Bandyopadhyay
 * @author Doug Simon
 */
public abstract class TeleEventRequest {

    /**
     * The thread to which this execution request is specific. This value will be null for requests that don't pertain
     * to a single thread.
     */
    public final TeleNativeThread _thread;

    /**
     * A descriptive name for this execution request (e.g. "single-step").
     */
    public final String _name;

    public TeleEventRequest(String name, TeleNativeThread thread) {
        _name = name;
        _thread = thread;
    }

    /**
     * Modifies the execution {@linkplain TeleProcess#state() state} of the tele process.
     *
     * @throws OSExecutionRequestException if an error occurred while trying to modify the execution state
     */
    public abstract void execute() throws OSExecutionRequestException;

    /**
     * Performs some action once the tele process next stops after {@link #execute()} has been called.
     */
    public void notifyProcessStopped() {
    }

    @Override
    public String toString() {
        return _name;
    }
}
