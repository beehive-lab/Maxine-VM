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
package com.sun.max.util;

import java.util.*;

/**
 * Deferred Runnables.
 *
 * Creating a Deferrable either causes immediate execution of its 'run()' method
 * or queues it for deferred execution later on when 'runAll()' is called.
 *
 * @author Bernd Mathiske
 */
public abstract class Deferrable implements Runnable {

    public Deferrable(Queue queue) {
        queue.handle(this);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public static final class Queue {

        private List<Deferrable> deferrables;

        private Queue() {
        }

        synchronized void handle(Deferrable deferrable) {
            if (deferrables != null) {
                deferrables.add(deferrable);
            } else {
                deferrable.run();
            }
        }

        public synchronized void deferAll() {
            deferrables = new LinkedList<Deferrable>();
        }

        public synchronized void runAll() {
            while (deferrables != null) {
                final List<Deferrable> oldDeferrables = this.deferrables;
                this.deferrables = new LinkedList<Deferrable>();
                for (Deferrable deferrable : oldDeferrables) {
                    deferrable.run();
                }
                if (oldDeferrables.isEmpty()) {
                    this.deferrables = null;
                }
            }
        }
    }

    public static Queue createRunning() {
        return new Queue();
    }

    public static Queue createDeferred() {
        final Queue queue = new Queue();
        queue.deferAll();
        return queue;
    }

    public abstract static class Block implements Runnable {
        public Block(Queue queue) {
            queue.deferAll();
            run();
            queue.runAll();
        }
    }
}
