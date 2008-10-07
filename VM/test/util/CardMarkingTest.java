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
/*VCSID=d86149a9-28c0-4221-a874-5c4d2aecc8bc*/
package util;


/**
 *
 * @author Karthik M
 */
public class CardMarkingTest {

    char[] _charArray = new char[500];
    int _nodeNum;
    public CardMarkingTest _next;
    public static CardMarkingTest _end;
    public static CardMarkingTest _start;

    public static int _currentNodeNum = 0;

    public static void addNode(CardMarkingTest node) {
        _end._next = node;
        _end = node;
        node._nodeNum = _currentNodeNum++;
        node._next = null;
    }

    public static void printList() {
        CardMarkingTest node = _start;
        while (node != null) {
            System.out.println("node number " + node._nodeNum);
            node = node._next;
        }
    }

    public static void main(String[] args) {
        _start = new CardMarkingTest();
        _start._nodeNum = _currentNodeNum++;
        _end = _start;
        addNode(new CardMarkingTest());
        addNode(new CardMarkingTest());
        addNode(new CardMarkingTest());
        addNode(new CardMarkingTest());
        addNode(new CardMarkingTest());
        printList();
        System.gc();
    }

}
