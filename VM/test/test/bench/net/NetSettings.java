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

/**
 * Support for tests that require a server program listening on the port given by the property {@code test.bench.net.port}.
 * on the host given by the property {@code test.bench.net.host}.

 */
package test.bench.net;

import test.bench.util.*;

public class NetSettings extends RunBench {
    private static final String HOST_NAME_PROPERTY = "test.bench.net.host";
    private static final String HOST_PORT_PROPERTY = "test.bench.net.port";

    private static String host;
    private static int port;

    protected NetSettings(LoopRunnable bench) {
        super(bench);
        host = getRequiredProperty(HOST_NAME_PROPERTY);
        port = Integer.parseInt(getRequiredProperty(HOST_PORT_PROPERTY));
    }

    protected static String host() {
        return host;
    }

    protected static int port() {
        return port;
    }
}
