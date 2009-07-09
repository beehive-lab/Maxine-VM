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
package com.sun.max.sync;

import com.sun.max.program.*;

/**
 * A daemon thread that hangs around, waiting,
 * then {@linkplain #execute(Runnable) executes) a given procedure when requested,
 * then waits again.
 *
 * The client thread is blocked while the server thread is executing a request and
 * the server thread is blocked between serving requests.
 *
 * Only one thread at a time can interact with the server thread.
 *
 * @author Bernd Mathiske
 */
public class BlockingServerDaemon extends Thread {

    /**
     * The lock object used to synchronize client/server interaction.
     */
    private final Object token = new Object();

    public BlockingServerDaemon(String name) {
        super(name);

        synchronized (this) {
            setDaemon(true);

            synchronized (token) {
                start();
                try {
                    // Block clients until the server is waiting for requests:
                    token.wait();
                } catch (InterruptedException interruptedException) {
                    ProgramError.unexpected();
                }
            }
        }
    }

    private Runnable procedure;

    /**
     * The server loop that will run any {@linkplain #execute(Runnable) scheduled} request.
     */
    @Override
    public void run() {
        while (true) {
            // Let the client thread continue as soon as we are waiting for requests again:
            synchronized (token) {
                token.notify();
                try {
                    token.wait();
                } catch (InterruptedException interruptedException) {
                    ProgramError.unexpected();
                }
                procedure.run();
            }
        }
    }

    /**
     * Schedules a given procedure to be run on the server thread. The client calling this method is
     * blocked until the given procedure has been executed.
     *
     * @param proc a procedure to be executed on the server thread
     */
    public synchronized void execute(Runnable proc) {
        this.procedure = proc;
        synchronized (token) {

            // Make the server run one loop round:
            token.notify();

            try {
                // Block the client until the procedure has been executed
                // and the server is waiting for requests again:
                token.wait();
            } catch (InterruptedException interruptedException) {
                ProgramError.unexpected();
            }
        }
    }
}
