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
 * then executes a given procedure when requested,
 * then waits again.
 *
 * The client thread is blocking while the server thread
 * is executing a request and
 * the server thread is blocking between serving requests.
 *
 * Only one thread at a time can interact with the server thread.
 *
 * @author Bernd Mathiske
 */
public class BlockingServerDaemon extends Thread {

    private final Object _token = new Object();

    public BlockingServerDaemon(String name) {
        super(name);

        synchronized (this) {
            setDaemon(true);

            synchronized (_token) {
                start();
                try {
                    // Block clients until the server is waiting for requests:
                    _token.wait();
                } catch (InterruptedException interruptedException) {
                    ProgramError.unexpected();
                }
            }
        }
    }

    private Runnable _procedure;

    @Override
    public final void run() {
        while (true) {
            // Let the client thread continue as soon as we are waiting for requests again:
            synchronized (_token) {
                _token.notify();
                try {
                    _token.wait();
                } catch (InterruptedException interruptedException) {
                    ProgramError.unexpected();
                }
                _procedure.run();
            }
        }
    }

    public synchronized void execute(Runnable procedure) {
        _procedure = procedure;
        synchronized (_token) {

            // Make the server run one loop round:
            _token.notify();

            try {
                // Block the client until the procedure has been executed
                // and the server is waiting for requests again:
                _token.wait();
            } catch (InterruptedException interruptedException) {
                ProgramError.unexpected();
            }
        }
    }

}
