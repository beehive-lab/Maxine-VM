/*
 * Copyright (c) 2010, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.runtime;

/**
 * Queue used by the {@link VmOperationThread} to process {@linkplain VmOperationThread#submit(VmOperation) submitted} operations.
 */
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
        FatalError.check(node.next == null && node.previous == null, "Inserting VM operation into queue twice");
        FatalError.check(queue.next.previous == queue && queue.previous.next == queue, "Malformed queue");
        node.previous = queue;
        node.next = queue.next;
        queue.next.previous = node;
        queue.next = node;
    }

    public static void unlink(VmOperation node) {
        FatalError.check(node.next != null && node.previous != null, "Unlinking node not in queue");
        FatalError.check(node.next.previous == node && node.previous.next == node, "Malformed queue");
        node.previous.next = node.next;
        node.next.previous = node.previous;
        node.next = null;
        node.previous = null;
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
