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
package com.sun.max.tele.debug.guestvm.xen.dbchannel.agent;

import com.sun.max.tele.debug.guestvm.xen.dbchannel.*;
import com.sun.max.tele.debug.guestvm.xen.dbchannel.db.*;

/**
 * A {@link SimpleProtocol} implementation that is called reflectively by {@link ProtocolAgent}
 * and delegates to the standard {@link DBProtocol}, save the {@link SimpleProtocol#gatherThreads(long, long)}
 * and {@link SimpleProtocol#readThreads} methods, which are implemented here.
 *
 * @author Mick Jordan
 *
 */

public class AgentDBProtocol extends AgentProtocolAdaptor {
    public AgentDBProtocol() {
        super(new DBProtocol());
    }

    @Override
    protected void implAttach(int domId, int threadLocalsAreaSize) {
        teleThreadLocalsInitialize(threadLocalsAreaSize);
    }

    private static native void teleThreadLocalsInitialize(int threadLocalsSize);

}
