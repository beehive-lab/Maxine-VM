/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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

import java.lang.ref.*;
import java.util.*;

/**
 * This class implements a simple test for weak references and reference queues.
 *
 * @author Ben L. Titzer
 */
public class WeakReferenceTest03 {

    private static ReferenceQueue<T> refQueue = new ReferenceQueue<T>();
    private static int count;

    public static void main(String[] args) {
        for (int i = 0; i < 5; i++) {
            T t = new T();
            System.out.println("t = " + t);
            t = null;
            System.gc();
            NT q;
            while ((q = (NT) refQueue.poll()) == null) {
                // do nothing.
            }
            System.out.println("q = " + q);
        }
    }

    static class T {

        private NT nt;
        private int id = count++;

        T() {
            nt = new NT(this);
        }

        @Override
        public String toString() {
            return "a T #" + id;
        }
    }

    static class NT extends WeakReference<T> {

        private static List<NT> refList = new ArrayList<NT>();
        private int id = count++;

        NT(T t) {
            super(t, refQueue);
            refList.add(this);
        }

        @Override
        public String toString() {
            return "a T #" + id;
        }
    }
}
