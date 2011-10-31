/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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

    protected NetSettings(MicroBenchmark bench) {
        super(bench, null);
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
