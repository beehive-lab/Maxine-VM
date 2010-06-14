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
        return new NewSocket(new OpenCloseBench()).runBench(true);
    }

    public static boolean testall() {
        boolean result = new NewSocket(new OpenCloseBench()).runBench(true);
        result = result && new NewSocket(new OpenBench()).runBench(true);
        return result && new NewSocket(new CloseBench()).runBench(true);
    }

    public static boolean testOpen() {
        return new NewSocket(new OpenBench()).runBench(true);
    }

    public static boolean testClose() {
        return new NewSocket(new CloseBench()).runBench(true);
    }

    static class OpenCloseBench extends AbstractMicroBenchmark {

        public void run(boolean warmup) {
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
        }
    }

    static class OpenBench extends AbstractMicroBenchmark {

        private Socket socket;

        @Override
        public void run(boolean warmup) {
            try {
                socket = new Socket(host(), port());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
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

    static class CloseBench extends AbstractMicroBenchmark {

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
        public void run(boolean warmup) {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // for running stand-alone
    public static void main(String[] args) {
        test();
    }
}
