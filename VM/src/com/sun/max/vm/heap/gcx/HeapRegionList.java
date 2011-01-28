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

package com.sun.max.vm.heap.gcx;
import static com.sun.max.vm.heap.gcx.HeapRegionConstants.*;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.layout.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * A doubly linked list of regions implemented as an array of int region identifiers.
 * Used for region ownership list, wherein a list can only belong to one owner at a time.
 * This allows to share the backing storage for all the list.
 *
 * @author Laurent Daynes
 */
public final class HeapRegionList {

    public enum RegionListUse {
        /***
         * List used by HeapAccount users.
         */
        OWNERSHIP,
        /**
         * List used by HeapAccount.
         */
        ACCOUNTING;

        HeapRegionList createList() {
            Pointer base = Reference.fromJava(listsStorage[ordinal()]).toOrigin().plus(Layout.intArrayLayout().getElementOffsetInCell(0));
            return new HeapRegionList(base);
        }
    }

    private static final int [][] listsStorage = new int[RegionListUse.values().length][];

    static void initializeListStorage(RegionListUse use, int [] storage) {
        Arrays.fill(storage, INVALID_REGION_ID);
        listsStorage[use.ordinal()] = storage;
    }


    /**
     * The value denoting the null element. Used as a list terminator.
     */
    private static final int nullElement = INVALID_REGION_ID;

     /**
      * Pointer to raw storage of the list.
      */
    final Pointer listStorage;

    static final int fieldNumberOfBytes = KindEnum.INT.asKind().width.numberOfBytes;
    static final int log2FieldNumberOfBytes = KindEnum.INT.asKind().width.log2numberOfBytes;
    static final int log2Elem = 1 + log2FieldNumberOfBytes;
    static final int NEXT_INDEX = 0;
    static final int PREV_INDEX = 1;

    private int head;
    private int tail;
    private int size;

    @INLINE
    private void checkNonNullElem(int elem) {
        if (MaxineVM.isDebug()) {
            FatalError.check(elem != nullElement, "Invalid region identifier");
        }
    }

    private Pointer toNonNullPointer(int elem) {
        checkNonNullElem(elem);
        return listStorage.plus(elem << log2Elem);
    }

    private Pointer toPointer(int elem) {
        return (elem == nullElement) ? Pointer.zero() : toNonNullPointer(elem);
    }

    private int nextFieldIndex(int elem) {
        return elem << 1 + NEXT_INDEX;
    }

    private int prevFieldIndex(int elem) {
        return nextFieldIndex(elem) + PREV_INDEX;
    }

    private Pointer nextFieldPointer(int elem) {
        return toNonNullPointer(elem);
    }

    private Pointer prevFieldPointer(int elem) {
        return nextFieldPointer(elem).plus(fieldNumberOfBytes);
    }

    int next(int elem) {
        return listStorage.getInt(nextFieldIndex(elem));
    }

    int prev(int elem) {
        return listStorage.getInt(prevFieldIndex(elem));
    }


    private void setNext(int elem, int nextElem) {
        listStorage.setInt(nextFieldIndex(elem), nextElem);
    }

    private void setPrev(int elem, int prevElem) {
        listStorage.setInt(prevFieldIndex(elem), prevElem);
    }

    private void init(int elem, int nextElem, int prevElem) {
        Pointer elemPtr = toNonNullPointer(elem);
        elemPtr.setInt(NEXT_INDEX, nextElem);
        elemPtr.setInt(PREV_INDEX, prevElem);
    }

    private void clear(int elem) {
        init(elem, nullElement, nullElement);
    }

    void clear() {
        size = 0;
        tail = nullElement;
        head = nullElement;
    }

    private HeapRegionList(Pointer backingStorage) {
        listStorage = backingStorage;
        clear();
    }

    public int size() {
        return size;
    }

    public int head() {
        return head;
    }

    public int tail() {
        return tail;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    boolean contains(int elem) {
        int cursor = head;
        while (cursor != nullElement) {
            if (cursor == elem) {
                return true;
            }
            cursor = next(cursor);
        }
        return false;
    }

    /**
     * Append element at the end of the list.
     * @param elem element to append
     */
    void append(int elem) {
        if (isEmpty()) {
            head = elem;
            tail = elem;
            clear(elem);
            size = 1;
        } else {
            // Add to tail
            setNext(tail, elem);
            init(elem, nullElement, tail);
            tail = elem;
            size++;
        }
    }

    void prepend(int elem) {
        if (isEmpty()) {
            head = elem;
            tail = elem;
            clear(elem);
            size = 1;
        } else {
            // Insert at head
            setPrev(head, elem);
            init(elem, head, nullElement);
            size++;
        }
    }

    void insertAfter(int elem, int newElem) {
        if (elem == tail) {
            append(newElem);
        } else {
            int nextElem = next(elem);
            init(newElem, elem, nextElem);
            setNext(elem, newElem);
            setPrev(nextElem, newElem);
            size++;
        }
    }

    /**
     * Remove element from the list.
     * @param elem
     */
    void remove(int elem) {
        FatalError.check(elem != nullElement, "Must be a valid list element");
        if (MaxineVM.isDebug()) {
            FatalError.check(!contains(elem), "element must be in list");
        }
        int nextElem = next(elem);
        int prevElem = prev(elem);
        if (nextElem == nullElement) {
            FatalError.check(elem == tail, "Only the tail can have null next element");
            if (prevElem == nullElement) {
                FatalError.check(head == tail, "Only the tail can have null next element");
                head = nullElement;
                tail = nullElement;
            } else {
                tail = prevElem;
                setNext(tail, nullElement);
            }

        } else if (prevElem == nullElement) {
            head = nextElem;
            setPrev(head, nullElement);
        } else {
            setNext(prevElem, nextElem);
            setPrev(nextElem, prevElem);
        }
        clear(elem);
    }

    void append(HeapRegionList list) {
        FatalError.check(list.listStorage == listStorage, "can only merge list with same listStorage");
        if (isEmpty()) {
            head = list.head;
        } else {
            setNext(tail, list.head);
            setPrev(list.head, tail);
        }
        tail = list.tail;
    }

    void prepend(HeapRegionList list) {
        FatalError.check(list.listStorage == listStorage, "can only merge list with same listStorage");
        if (isEmpty()) {
            tail = list.tail;
        } else {
            setPrev(head, list.tail);
            setNext(list.tail, head);
        }
        head = list.head;
    }
}

