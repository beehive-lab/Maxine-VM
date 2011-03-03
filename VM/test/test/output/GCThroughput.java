/*
 * Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

public class GCThroughput implements Runnable {

    private static int runTime = 30;
    private static int reportFrequency = 5;
    private static int maxSize = 1024;
    private static volatile boolean done = false;
    private static volatile long ta;
    private static volatile long ts;
   /**
     * @param args
     */
    public static void main(String[] args) {
        final Random rand = new Random(467673);
        // Checkstyle: stop modified control variable check
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equals("t")) {
                runTime = Integer.parseInt(args[++i]);
            } else if (arg.equals("s")) {
                maxSize = Integer.parseInt(args[++i]);
            }
        }
        // Checkstyle: resume modified control variable check
        new Thread(new GCThroughput()).start();
        while (!done) {
            final int size = rand.nextInt(maxSize);
            @SuppressWarnings("unused")
            final Object[] o = new Object[size];
            ta++;
            ts += size;
        }
        System.out.println("Average APS: " + (ta / runTime) + ", Average BAPS: " + (ts / runTime));
    }

    public void run() {
        long time = 0;
        long lta = 0;
        long lts = 0;
        while (time < runTime) {
            try {
                Thread.sleep(reportFrequency * 1000);
                time += reportFrequency;
                System.out.println("Interval APS: " + ((ta - lta) / reportFrequency) + ", BAPS: " + ((ts - lts) / reportFrequency));
                lta = ta;
                lts = ts;
            } catch (InterruptedException ex) {
                time = runTime;
            }
        }
        done = true;
    }

}

