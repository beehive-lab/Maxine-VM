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
package test.com.sun.max.sync;

import java.util.*;

import junit.framework.*;

import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.sync.*;

/**
 * Tests for {@link BlockingServerDaemon}.
 *
 * @author Bernd Mathiske
 */
public class BlockingServerDaemonTest extends TestCase {

    public BlockingServerDaemonTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(BlockingServerDaemonTest.class);
    }

    public void test_blockingServerDaemon() {
        Trace.begin(2, "test_blockingServerDaemon");
        final MutableInnerClassGlobal<Integer> counter = new MutableInnerClassGlobal<Integer>(0);
        final BlockingServerDaemon server = new BlockingServerDaemon("test_blockingServerDaemon");
        server.start();
        final int numberOfThreads = 100;
        final int numberOfRequests = 10;
        final Thread[] threads = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            final Runnable procedure = new Runnable() {
                public void run() {
                    counter.setValue(counter.value() + 1);
                    Trace.line(3, "SERVER counter: " + counter.value() + " thread: " + threadIndex);
                }
            };
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    final Random random = new Random();
                    for (int j = 0; j < numberOfRequests; j++) {
                        try {
                            Thread.sleep(random.nextLong() & 0xff);
                        } catch (InterruptedException interruptedException) {
                            fail();
                        }
                        Trace.line(3, "BEGIN counter: " + counter.value() + " thread: " + threadIndex);
                        final int value = counter.value();
                        server.service(procedure);
                        if (counter.value() == value) {
                            fail();
                        }
                        Trace.line(3, "END counter: " + counter.value() + " thread: " + threadIndex);
                    }
                }
            });
            threads[i].start();
        }
        for (int i = 0; i < numberOfThreads; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException interruptedException) {
                fail();
            }
        }
        assertTrue(counter.value() == numberOfThreads * numberOfRequests);
        Trace.end(2, "test_blockingServerDaemon: " + counter.value());
    }
}
