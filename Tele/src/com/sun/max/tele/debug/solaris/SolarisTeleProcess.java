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
package com.sun.max.tele.debug.solaris;

import java.io.*;

import com.sun.max.platform.*;
import com.sun.max.tele.*;
import com.sun.max.tele.debug.*;
import com.sun.max.tele.debug.TeleNativeThread.Params;
import com.sun.max.tele.debug.unix.*;
import com.sun.max.vm.prototype.*;

/**
 * @author Bernd Mathiske
 * @author Aritra Bandyopadhyay
 * @author Doug Simon
 * @author Hannes Payer
 * @author Mick Jordan
 */
public final class SolarisTeleProcess extends UnixTeleProcessAdaptor {

    /**
     * Creates a handle to a native Solaris Maxine VM process by launching a new process with a given set of command line arguments.
     *
     * @param teleVM
     * @param platform
     * @param programFile
     * @param commandLineArguments
     * @throws BootImageException
     */
    SolarisTeleProcess(TeleVM teleVM, Platform platform, File programFile, String[] commandLineArguments) throws BootImageException {
        super(teleVM, platform, programFile, commandLineArguments);
    }

    /**
     * Attach to an existing native Solaris Maxine VM process, or a core dump.
     * @param teleVM
     * @param platform
     * @param programFile
     * @param id
     * @throws BootImageException
     */
    SolarisTeleProcess(TeleVM teleVM, Platform platform, File programFile, int id) throws BootImageException {
        super(teleVM, platform, programFile, id);
    }

    @Override
    protected TeleNativeThread createTeleNativeThread(Params params) {
        return new SolarisTeleNativeThread(this, params);
    }

    @Override
    public int platformWatchpointCount() {
        // not sure, try max
        return Integer.MAX_VALUE;
    }


}
