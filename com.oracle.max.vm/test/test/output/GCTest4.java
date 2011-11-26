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
package test.output;

public class GCTest4 {

    static int MAX_ITERATIONS;
    static {
        MAX_ITERATIONS = Integer.parseInt(System.getProperty("gctest.iterations", "50"));
    }

    public static void main(String[] args) {
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            System.out.println("Iteration " + i + "...");
            createList();
        }
        System.out.println(GCTest4.class.getSimpleName() + " done.");
    }

    private static void createList() {
        // build a list of length 1000
        final Node1 start = new Node1("0");
        Node1 previous = start;
        final int length = 10000;
        for (int i = 1; i < length; i++) {
            final Node1 temp = new Node1(String.valueOf(i));
            previous.setNext(temp);
            previous = temp;
        }

        // verify the contents of the list
        int len = 0;
        Node1 node = start;
        while (node != null) {
            if (!node.id.equals(String.valueOf(len))) {
                throw new Error("assert fail");
            }
            node = node.next;
            len++;
        }
        if (len != length) {
            throw new Error("assert fail");
        }
    }

    public static void printList(Node1 start) {
        Node1 temp = start;
        while (temp.getNext() != null) {
            System.out.print(temp.getId() + ", ");
            temp = temp.getNext();
        }
    }

}

class Node1 {

    String id;
    Node1 next = null;
    long[] array;

    Node1(String id) {
        this.id = id;
        this.array = new long[500];
    }

    public String getId() {
        return id;
    }

    public void setNext(Node1 next) {
        this.next = next;
    }

    public Node1 getNext() {
        return next;
    }

}
