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

    public TCPXGProtocol(ImageFileHandler imageFileHandler, String hostAndPort) {
        super(hostAndPort);
        this.imageFileHandler = imageFileHandler;
        xgProtocol = new XGProtocol(imageFileHandler);
    }

    @Override
    public long getBootHeapStart() {
//        final long addr = imageFileHandler.getBootHeapStartSymbolAddress();
        final long addr = 0x6add410;
        // delegate
        final long result = xgProtocol.getBootHeapStart(this, addr);
        Trace.line(1, "getBootHeapStart returned " + Long.toHexString(result));
        return result;
    }
}
