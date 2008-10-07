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
package util;

public class GCLinkedListTest {

    public static void main(String[] args) {
        final Node1 start = new Node1("0");
        Node1 previous = start;
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            final Node1 temp = new Node1(String.valueOf(i));
            previous.setNext(temp);
            previous = temp;
            System.out.println("Iteration: " + i);

           //GCLinkedListTest.printList(start);
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

    String _id;
    Node1 _next = null;
    long[] _array;

    Node1(String id) {
        _id = id;
        _array = new long[500];
    }

    public String getId() {
        return _id;
    }

    public void setNext(Node1 next) {
        _next = next;
    }

    public Node1 getNext() {
        return _next;
    }

}
