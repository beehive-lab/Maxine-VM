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
 * An agent that opens a socket for the VM to communicate the address of the boot image once it has
 * been loaded and relocated. The agent spawns a separate thread to listen for a connection from the VM.
 *
 * Note that this is not a general purpose agent for process control and inspection in the VM. It exists
 * solely to simplify the task of getting the address of the boot image in the VM.
 *
 * @author Doug Simon
 */
public class TeleVMAgent implements Runnable {

    /**
     * Number of seconds to wait for the VM being debugged to connect to the agent socket.
     */
    public static final int VM_CONNECT_TIMEOUT = 5;

    private ServerSocket _serverSocket;
    private Socket _socket;
    private IOException _acceptException;

    public TeleVMAgent() throws BootImageException {
        try {
            _serverSocket = new ServerSocket(0);
            _serverSocket.setSoTimeout(5000);
        } catch (IOException ioException) {
            throw new BootImageException("Error opening agent socket", ioException);
        }
        new Thread(this, "ConnectorAgent").start();
    }

    public void run() {
        try {
            Trace.line(1, "Opening agent socket on port " + _serverSocket.getLocalPort());
            _socket = _serverSocket.accept();
            Trace.line(1, "Agent received connection from VM on " + _socket);
        } catch (IOException ioException) {
            _acceptException = ioException;
        }
    }

    /**
     * Gets the port on which the agent is listening for a connection from the VM.
     */
    public int port() {
        return _serverSocket.getLocalPort();
    }

    /**
     * Waits for the VM to connect to the socket opened by this agent.
     *
     * @return the socket representing a connection from the VM
     * @throws BootImageException if the VM did not connect within {@link #VM_CONNECT_TIMEOUT} seconds or there was some
     *             other IO error while waiting for the connection
     */
    public Socket waitForVM() throws BootImageException {
        int remainingSeconds = VM_CONNECT_TIMEOUT;
        while (_socket == null && remainingSeconds > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            --remainingSeconds;
        }
        if (_acceptException != null) {
            throw new BootImageException("Error while waiting for connection from VM", _acceptException);
        }
        if (_socket == null) {
            throw new BootImageException("Timed out while waiting for connection from VM", _acceptException);
        }
        Trace.line(1, "Received connection on agent socket: " + _socket);
        return _socket;
    }
}
