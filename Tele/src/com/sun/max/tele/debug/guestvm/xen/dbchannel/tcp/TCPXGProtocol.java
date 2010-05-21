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
package com.sun.max.tele.debug.guestvm.xen.dbchannel.tcp;

import com.sun.max.elf.*;
import com.sun.max.program.*;
import com.sun.max.tele.debug.guestvm.xen.dbchannel.*;
import com.sun.max.tele.debug.guestvm.xen.dbchannel.xg.*;

/**
 * A variant of {@link TCPProtocol} that is communicating to an {@link AgentXGProtocol}.
 *
 * @author Mick Jordan
 *
 */

public class TCPXGProtocol extends TCPProtocol {
    private ImageFileHandler imageFileHandler;
    private XGProtocol xgProtocol;
    private boolean started;

    public TCPXGProtocol(ImageFileHandler imageFileHandler, String hostAndPort) {
        super(hostAndPort);
        this.imageFileHandler = imageFileHandler;
        xgProtocol = new XGProtocol(imageFileHandler);
    }

    @Override
    public long getBootHeapStart() {
        final long addr = imageFileHandler.getBootHeapStartSymbolAddress();
        // delegate
        final long result = xgProtocol.getBootHeapStart(this, addr);
        Trace.line(1, "getBootHeapStart returned " + Long.toHexString(result));
        return result;
    }

    @Override
    public int resume() {
        if (!started) {
            // release domain
            final long addr = imageFileHandler.getSymbolAddress("xg_resume_flag");
            assert addr > 0;
            byte[] one = new byte[] {1};
            writeBytes(addr, one, 0, 1);
        }
        final int result = super.resume();
        Trace.line(1, "resume returned " + result);
        return result;
    }
}
