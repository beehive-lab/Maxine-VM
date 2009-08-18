/*
 * Copyright (c) 2008 Sun Microsystems, Inc.  All rights reserved.
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

import java.util.concurrent.*;

/**
 * A simple test that uses {@link java.util.concurrent.ArrayBlockingQueue}.
 *
 * @author Ben L. Titzer
 */
public class BlockingQueue {
    public static final int MESSAGES = 5;

    public static void main(String[] args) throws InterruptedException {
        int messages = MESSAGES;
        if (args.length > 0) {
            messages = Integer.parseInt(args[0]);
        }
        final ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(messages);
        final Consumer consumer = new Consumer(queue, messages);
        final Producer producer = new Producer(queue, messages);
        consumer.start();
        producer.start();
        consumer.join();
        producer.join();
        System.out.println("Done.");
    }

    static class Producer extends Thread {
        final ArrayBlockingQueue<Integer> queue;
        final int messages;
        Producer(ArrayBlockingQueue<Integer> queue, int messages) {
            super("Producer");
            this.queue = queue;
            this.messages = messages;
        }
        @Override
        public void run() {
            for (int i = 0; i < messages; i++) {
                queue.add(i);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    // do nothing.
                }
            }
        }
    }

    static class Consumer extends Thread {
        final ArrayBlockingQueue<Integer> queue;
        final int messages;
        Consumer(ArrayBlockingQueue<Integer> queue, int messages) {
            super("Consumer");
            this.queue = queue;
            this.messages = messages;
        }
        @Override
        public void run() {
            for (int i = 0; i < messages; i++) {
                try {
                    System.out.println(queue.take());
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    // do nothing.
                }
            }
        }
    }
}
