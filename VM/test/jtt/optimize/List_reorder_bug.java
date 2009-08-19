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
package jtt.optimize;

/*
 * @Harness: java
 * @Runs: 0 = true;
 */
public class List_reorder_bug {

    static class List {
        List(int id) {
            this.id = id;
        }
        List next;
        int id;
        boolean bool = true;
    }

    private static List list;

    public static boolean test(int i) {
        list = new List(5);
        list.next = new List(6);
        new List_reorder_bug().match(new Object(), 27, 6, 0);
        return list.next == null;
    }

    private void match(Object a, int src, int id, int seq) {
        print("match: " + src + ", " + id);
        List item = list;
        List itemPrev = null;
        while (item != null) {
            if (item.id == id) {
                if (item.bool) {
                    outcall(item.id);
                }
                if (itemPrev != null) {
                    itemPrev.next = item.next;
                } else {
                    list = item.next;
                }

                item.next = null;
                return;
            }

            itemPrev = item;
            item = item.next;
        }
    }

    static int globalId;

    private static void outcall(int id) {
        globalId = id;
    }

    String s;
    private void print(String s) {
        this.s = s;
    }
}
