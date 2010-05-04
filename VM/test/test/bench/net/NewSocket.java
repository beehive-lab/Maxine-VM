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
 * @Runs: 0 = true
 */
package test.bench.net;

import java.net.*;

public class NewSocket extends NetSettings {

    protected NewSocket(LoopRunnable bench) {
        super(bench);
    }
    public static boolean test(int i) throws InterruptedException {
        return new NewSocket(new Bench()).runBench(true);
    }

    static class Bench extends SimpleLoopRunnable {
        public void run(long loopCount) {
            Socket s = null;
            final String host = host();
            final int port = port();
            for (long i = 0; i < loopCount; i++) {
                try {
                    s = new Socket(host, port);
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

    }

}
