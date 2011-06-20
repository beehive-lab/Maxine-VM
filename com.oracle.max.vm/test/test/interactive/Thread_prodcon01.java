/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
/*
 * @Harness: java
 * @Runs: 0 = true;
 */
package test.interactive;

import com.sun.max.program.*;

public class Thread_prodcon01 {

    public static final boolean debug = true;
    private static boolean ok = true;

    public static boolean test(int i) throws InterruptedException {
        final Drop drop = new Drop();
        final Thread producer = new Thread(new Producer(drop));
        final Thread consumer = new Thread(new Consumer(drop));
        producer.start();
        consumer.start();
        consumer.join();
        return ok;
    }

    static class Drop {
        //Message sent from producer to consumer.
        private String message;
        //True if consumer should wait for producer to send message, false
        //if producer should wait for consumer to retrieve message.
        private boolean empty = true;

        public synchronized String take() {
            //Wait until message is available.
            while (empty) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    //
                }
            }
            //Toggle status.
            empty = true;
            //Notify producer that status has changed.
            notifyAll();
            return message;
        }

        public synchronized void put(String message) {
            // Wait until message has been retrieved.
            while (!empty) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
            //Toggle status.
            empty = false;
            //Store message.
            this.message = message;
            //Notify consumer that status has changed.
            notifyAll();
        }
    }

    static class Producer implements Runnable {
        private Drop drop;

        public Producer(Drop drop) {
            this.drop = drop;
        }

        static String[] importantInfo = {
            "Mares eat oats",
            "Does eat oats",
            "Little lambs eat ivy",
            "A kid will eat ivy too"
        };

        public void run() {

            //Random random = new Random();

            for (int i = 0; i < importantInfo.length; i++) {
                drop.put(importantInfo[i]);
                /*
                try {
                    Thread.sleep(random.nextInt(5000));
                } catch (InterruptedException e) {}
                */
            }
            drop.put("DONE");
        }
    }

    static class Consumer implements Runnable {
        private Drop drop;

        public Consumer(Drop drop) {
            this.drop = drop;
        }

        public void run() {
            // Random random = new Random();
            int i = 0;
            for (String message = drop.take(); !message.equals("DONE"); message = drop.take()) {
                debug("MESSAGE RECEIVED: " + message);
                if (!message.equals(Producer.importantInfo[i])) {
                    ok = false;
                }
                i++;
                /**
                 * try { Thread.sleep(random.nextInt(5000)); } catch (InterruptedException e) {}
                 */
            }
        }
    }

    private static void debug(String s) {
        if (debug) {
            Trace.stream().println(s);
        }
    }
}
