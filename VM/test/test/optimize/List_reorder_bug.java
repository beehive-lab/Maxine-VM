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
package test.optimize;

/*
 * @Harness: java
 * @Runs: 0 = true;
 */
public class List_reorder_bug {

    static class List {
        List(int id) {
            _id = id;
        }
        List _next;
        int _id;
        boolean _bool = true;
    }

    private static List _list;

    public static boolean test(int i) {
        _list = new List(5);
        _list._next = new List(6);
        new List_reorder_bug().match(new Object(), 27, 6, 0);
        return _list._next == null;
    }

    private void match(Object a, int src, int id, int seq) {
        print("match: " + src + ", " + id);
        List item = _list;
        List itemPrev = null;
        while (item != null) {
            if (item._id == id) {
                if (item._bool) {
                    outcall(item._id);
                }
                if (itemPrev != null) {
                    itemPrev._next = item._next;
                } else {
                    _list = item._next;
                }

                item._next = null;
                return;
            }

            itemPrev = item;
            item = item._next;
        }
    }

    static int _globalId;

    private static void outcall(int id) {
        _globalId = id;
    }

    String _s;
    private void print(String s) {
        _s = s;
    }
}
