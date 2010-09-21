/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
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
package test.output;
import java.util.concurrent.atomic.*;

import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Tests signal handling by raising a specified signal a specified number of times.
 *
 * @author Doug Simon
 */
public class SignalHandling {

    public static final int NUMBER_OF_SIGNALS = 1000;

    public static final String SIGNAL_NAME = System.getProperty("signalName", "HUP");
    public static final int PER_SIGNAL_PAUSE = Integer.getInteger("perSignalPause", 0);
    public static final int POST_SIGNAL_PAUSE = Integer.getInteger("postSignalPause", 2000);

    public static void main(String[] args) throws InterruptedException {
        int signals = NUMBER_OF_SIGNALS;
        if (args.length > 0) {
            signals = Integer.parseInt(args[0]);
        }
        final AtomicInteger signalsHandled = new AtomicInteger();

        SignalHandler handler = new SignalHandler() {
            public void handle(Signal sig) {
                signalsHandled.incrementAndGet();
            }
        };
        Signal signal = new Signal(SIGNAL_NAME);
        Signal.handle(signal, handler);

        System.out.println("Installed signal handler: " + signal);

        if (PER_SIGNAL_PAUSE != 0) {
            System.out.println("Raising " + signals + " signals...");
            for (int i = 0; i < signals; i++) {
                Signal.raise(signal);
                if (i % 1000 == 0 && i != 0) {
                    System.out.println("raised signals: " + i);
                }
                Thread.sleep(PER_SIGNAL_PAUSE);
            }
        } else {
            System.out.print("Raising " + signals + " signals...");
            for (int i = 0; i < signals; i++) {
                Signal.raise(signal);
                Thread.sleep(PER_SIGNAL_PAUSE);
            }
            System.out.println(" done");
        }

        System.out.print("Sleeping for " + POST_SIGNAL_PAUSE + " milliseconds...");
        Thread.sleep(POST_SIGNAL_PAUSE);
        System.out.println(" done");

        System.out.println("Handled " + signalsHandled.get() + " signals [*runtime-variable*]");
    }
}
