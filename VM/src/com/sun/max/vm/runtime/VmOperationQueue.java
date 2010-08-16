/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.runtime;


public class VmOperationQueue {
    private int length;
    private VmOperation head;

    static class Head extends VmOperation {

        public Head() {
            super("VmOperationQueue.Head", null, null);
            this.next = this;
            this.previous = this;
        }
    }


    public VmOperationQueue() {
        head = new Head();
    }

    /**
     * Inserts {@code node} to the right of {@code queue}.
     *
     * @param queue
     * @param node
     */
    public static void insert(VmOperation queue, VmOperation node) {
        assert queue.next.previous == queue && queue.previous.next == queue;
        node.previous = queue;
        node.next = queue.next;
        queue.next.previous = node;
        queue.next = node;
    }

    public static void unlink(VmOperation node) {
        assert node.next.previous == node && node.previous.next == node;
        node.previous.next = node.next;
        node.next.previous = node.previous;
    }

    public void addFirst(VmOperation node) {
        length++;
        insert(head.next, node);
    }

    public void addLast(VmOperation node) {
        length++;
        insert(head.previous, node);
    }

    public boolean isEmpty() {
        return length == 0;
    }

    public void add(VmOperation node) {
        addLast(node);
    }

    /**
     * Retrieves and removes the head of this queue,
     * or returns {@code null} if it's empty.
     */
    public VmOperation poll() {
        if (isEmpty()) {
            return null;
        }
        VmOperation node = head.next;
        unlink(node);
        length--;
        return node;
    }
}
