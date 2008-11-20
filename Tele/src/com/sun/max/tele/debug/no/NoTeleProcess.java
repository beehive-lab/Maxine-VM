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
package com.sun.max.tele.debug.no;

import java.io.*;

import com.sun.max.collect.*;
import com.sun.max.platform.*;
import com.sun.max.program.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.unsafe.*;

/**
 * @author Bernd Mathiske
 */
public final class NoTeleProcess extends TeleProcess {

    private final DataAccess _dataAccess;

    @Override
    public DataAccess dataAccess() {
        return _dataAccess;
    }

    public NoTeleProcess(TeleVM teleVM, Platform platform, File programFile) {
        super(teleVM, platform, programFile, NO_COMMAND_LINE_ARGUMENTS);
        _dataAccess = new StreamDataAccess(new MemoryDataStreamFactory(), platform.processorKind().dataModel());
    }

    @Override
    protected boolean gatherThreads(AppendableSequence<TeleNativeThread> threads) {
        return true;
    }

    @Override
    protected int read0(Address address, byte[] buffer, int offset, int length) {
        return _dataAccess.read(address, buffer, offset, length);
    }

    @Override
    protected int write0(byte[] buffer, int offset, int length, Address address) {
        return _dataAccess.write(buffer, offset, length, address);
    }

    @Override
    protected void kill() throws OSExecutionRequestException {
        throw Problem.unimplemented();
    }

    @Override
    protected void resume() throws OSExecutionRequestException {
        throw Problem.unimplemented();
    }

    @Override
    protected void suspend() throws OSExecutionRequestException {
        Problem.unimplemented();
    }

    @Override
    protected boolean waitUntilStopped() {
        throw Problem.unimplemented();
    }
}
