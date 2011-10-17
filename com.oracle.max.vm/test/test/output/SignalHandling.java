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
package test.output;
import java.util.concurrent.atomic.*;

import sun.misc.*;

/**
 * Tests signal handling by raising a specified signal a specified number of times.
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
