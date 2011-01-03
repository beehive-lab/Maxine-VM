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
package com.sun.max.tele.debug;

/**
 * A tele event request encapsulates some action that {@linkplain #execute() modifies} the execution
 * {@linkplain TeleProcess#processState() state} of the tele process as well as some
 * {@linkplain TeleEventRequest#afterExecution(WaitResult) action} to take when the tele process next stops after the
 * request has been issued.
 *
 * @author Aritra Bandyopadhyay
 * @author Doug Simon
 * @author Michael Van De Vanter
 */
public abstract class TeleEventRequest {

    /**
     * The thread to which this execution request is specific. This value will be null for requests that don't pertain
     * to a single thread.
     */
    public final TeleNativeThread thread;

    public final boolean withClientBreakpoints;

    /**
     * A descriptive name for this execution request (e.g. "single-step").
     */
    public final String name;

    private boolean complete = false;

    public TeleEventRequest(String name, TeleNativeThread thread, boolean withClientBreakpoints) {
        this.name = name;
        this.thread = thread;
        this.withClientBreakpoints = withClientBreakpoints;
    }

    /**
     * Modifies the execution {@linkplain TeleProcess#processState() state} of the tele process.
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
        return name;
    }

    public synchronized void notifyOfCompletion() {
        complete = true;
        notify();
    }

    public synchronized void waitUntilComplete() {
        while (!complete) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
