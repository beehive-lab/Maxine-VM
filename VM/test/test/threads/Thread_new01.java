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
/*VCSID=5b09fc39-a854-4951-ba4c-875ff5b6ed4d*/
/*
 * @Harness: java
 * @Runs: 0 = true; 1 = true; 2 = true; 3 = true; 4 = false
 */
package test.threads;


public final class Thread_new01 {
    private Thread_new01() {
    }

    public static boolean test(int i) {
        if (i == 0) {
            return new Thread() != null;
        }
        if (i == 1) {
            return new Thread("Thread_new01") != null;
        }
        if (i == 2) {
            return new Thread(new Thread()) != null;
        }
        if (i == 3) {
            return new Thread(new Thread(), "Thread_new01") != null;
        }
        return false;
    }
}
