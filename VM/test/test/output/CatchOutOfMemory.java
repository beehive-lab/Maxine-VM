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
package test.output;

import java.util.*;

/**
 * A simple class to allocate a lot of memory and then catch an OutOfMemory exception.
 *
 * @author Ben L. Titzer
 */
public class CatchOutOfMemory {
    public static void main(String[] args) {
        System.out.println("starting...");
        if (test(0) == 0) {
            System.out.println("ok.");
            System.out.flush();
/*            if (test(1) == 0) {
                System.out.println("ok.");
                System.out.flush();
                System.exit(30);
            }
*/
            System.exit(10);
        } else {
            System.out.println("failed.");
            System.out.flush();
            System.exit(20);
        }
    }
    public static int test(int a) {
        List<Object[]> leak = new ArrayList<Object[]>();
        try {
            while (true) {
                leak.add(new Object[200000]);
            }
        } catch (OutOfMemoryError ex) {
            return 0;
        } catch (Throwable ex) {
            return -1;
        } finally {
            leak = null;
        }
    }
}
