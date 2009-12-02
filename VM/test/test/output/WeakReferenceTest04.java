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

/**
 * This class implements a simple test for weak references and reference queues.
 *
 * @author Ben L. Titzer
 */
public class WeakReferenceTest04 {
    public static void main(String[] args) {
        // now test with a reference queue
        final ReferenceQueue<String> queue = new ReferenceQueue<String>();
        final WeakReference<String> w3 = new WeakReference<String>(new String("alive"), queue);
        test(w3);
        while (true) {
            try {
                final Object obj = queue.remove();
                System.out.println(obj == w3);
                break;
            } catch (InterruptedException e) {
            }
        }
    }

    private static void test(final WeakReference<? extends Object> w1) {
        System.out.println("" + w1.get());
        System.gc();
        System.out.println("" + w1.get());
    }
}
