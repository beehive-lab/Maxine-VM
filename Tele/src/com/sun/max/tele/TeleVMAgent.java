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
package com.sun.max.tele;

import java.io.*;
import java.net.*;

import com.sun.max.program.*;
import com.sun.max.vm.prototype.*;

/**
 * An agent that opens a socket for receiving the boot image address from the VM.
 *
 * Note that this is not a general purpose agent for process control and inspection in the VM. It exists
 * solely to simplify the task of getting the address of the boot image in the VM. It originated
 * as a work around for the lack of support on Darwin for intercepting system calls. On systems
 * that support such functionality, the boot image address can be discovered by intercepting the
 * call to 'mmap' that loads the boot image.
 *
 * @author Doug Simon
 */
public class TeleVMAgent {

    /**
     * Number of seconds to wait for the VM being debugged to connect to the agent socket.
     */
    public static final int VM_CONNECT_TIMEOUT = 5;

    private ServerSocket serverSocket;
    private Socket socket;
    private IOException acceptException;

    public void start() throws BootImageException {
        try {
            serverSocket = new ServerSocket(0);
        } catch (IOException ioException) {
            throw new BootImageException("Error opening agent socket", ioException);
        }
        new Thread("ConnectorAgent") {
            @Override
            public void run() {
                try {
                    Trace.line(1, "Opening agent socket on port " + serverSocket.getLocalPort());
                    socket = serverSocket.accept();
                } catch (IOException ioException) {
                    acceptException = ioException;
                }
            }
        }.start();
    }

    public void close() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                Trace.line(1, "Closing agent socket on port " + serverSocket.getLocalPort());
                serverSocket.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    /**
     * Gets the port on which the agent is listening for a connection from the VM.
     */
    public int port() {
        return serverSocket.getLocalPort();
    }

    /**
     * Waits for the VM to connect to the socket opened by this agent.
     *
     * @return the socket representing a connection from the VM
     * @throws BootImageException if the VM did not connect within {@link #VM_CONNECT_TIMEOUT} seconds or there was some
     *             other IO error while waiting for the connection
     */
    public Socket waitForVM() throws BootImageException {
        // Give the VM a few seconds to load and relocate the boot image
        final int millisecondsPerAttempt = 200;
        int attempts = (VM_CONNECT_TIMEOUT * 1000) / millisecondsPerAttempt;
        while (socket == null && attempts > 0) {
            try {
                Thread.sleep(millisecondsPerAttempt);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            --attempts;
        }
        if (acceptException != null) {
            throw new BootImageException("Error while waiting for connection from VM", acceptException);
        }
        if (socket == null) {
            throw new BootImageException("Timed out while waiting for connection from VM", acceptException);
        }
        Trace.line(1, "Received connection on agent socket: " + socket);
        return socket;
    }
}
