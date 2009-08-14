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
