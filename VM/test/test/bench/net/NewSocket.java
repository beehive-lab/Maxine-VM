/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * Measures the opening of a client-side TCP socket.
 */
/*
 * @Harness: java
 *
 * @Runs: 0 = true
 */
package test.bench.net;

import java.io.*;
import java.net.*;

public class NewSocket extends NetSettings {

    protected NewSocket(MicroBenchmark bench) {
        super(bench);
    }

    public static boolean test() {
        return new NewSocket(new OpenCloseBench()).runBench();
    }

    public static boolean testall() {
        boolean result = new NewSocket(new OpenCloseBench()).runBench();
        result = result && new NewSocket(new OpenBench()).runBench();
        return result && new NewSocket(new CloseBench()).runBench();
    }

    public static boolean testOpen() {
        return new NewSocket(new OpenBench()).runBench();
    }

    public static boolean testClose() {
        return new NewSocket(new CloseBench()).runBench();
    }

    static class OpenCloseBench extends MicroBenchmark {

        @Override
        public long run() {
            Socket s = null;
            try {
                s = new Socket(host(), port());
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (s != null) {
                    try {
                        s.close();
                    } catch (Exception ex) {
                    }
                }
            }
            return defaultResult;
        }
    }

    static class OpenBench extends MicroBenchmark {

        private Socket socket;

        @Override
        public long run() {
            try {
                socket = new Socket(host(), port());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return defaultResult;
        }

        @Override
        public void postrun() {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class CloseBench extends MicroBenchmark {

        private Socket socket;

        @Override
        public void prerun() throws Exception {
            try {
                socket = new Socket(host(), port());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public long run() {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return defaultResult;
        }
    }

    // for running stand-alone
    public static void main(String[] args) {
        test();
    }
}
