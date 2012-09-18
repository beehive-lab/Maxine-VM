/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * A doubly linked list of regions implemented on top of a shared array of region identifiers.
 * Used for region ownership lists, wherein a region can only belong to one list at a time.
 * This allows to share the backing storage for all the list, i.e., all lists of regions for a particular use are threaded over the same array.
 */
public class HeapRegionList {

    /**
     * Region List Uses. There's as many list backing storage as there are uses.
     * A region can only belong to one region list at a time for a given use.
     */
    public enum RegionListUse {
        /***
         * List used by HeapAccount users.
         */
        OWNERSHIP,
        /**
         * List used by HeapAccount to keep track of region allocated to the heap account.
         */
        ACCOUNTING;

        HeapRegionList createList() {
            Pointer base = Reference.fromJava(listsStorage[ordinal()]).toOrigin().plus(Layout.intArrayLayout().getElementOffsetInCell(0));
            return new HeapRegionList(base);
        }
    }

    private static final int [][] listsStorage = new int[RegionListUse.values().length][];
    private static int [] sortingArea;

    static void initializeListStorage(int numRegions) {
        final int regionListSize = numRegions << 1; // 2 entries per regions, one for each link (prev and next).
        for (RegionListUse use : RegionListUse.values()) {
            int [] storage = new int[regionListSize];
            Arrays.fill(storage, INVALID_REGION_ID);
            listsStorage[use.ordinal()] = storage;
        }
        sortingArea = new int[numRegions];
    }

    // Sort using the shared, pre-allocated, sorting area.
    private static synchronized void sort(HeapRegionList list) {
        int index = 0;
        int current = list.head;
        if (current != INVALID_REGION_ID) {
            do {
                sortingArea[index++] = current;
                current = list.next(current);
            } while(current != INVALID_REGION_ID);
            Arrays.sort(sortingArea, 0, index);
            // Rebuild link list from sorted array.
            list.clear();
            list.append(sortingArea[0]);
            int i = 1;
            while (i < index) {
                list.appendNonEmpty(sortingArea[i++]);
            }
        }
    }

    public  void sort() {
        if (size > 1) {
            sort(this);
        }
    }

    /**
     * The value denoting the null element. Used as a list terminator.
     */
    private static final int nullElement = INVALID_REGION_ID;

     /**
      * Pointer to shared raw storage of the list.
      */
    private final Pointer listStorage;

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

    final int next(int elem) {
        return listStorage.getInt(nextFieldIndex(elem));
    }

    final int prev(int elem) {
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

    private HeapRegionList(Pointer backingStorage) {
        listStorage = backingStorage;
        clear();
    }

    protected HeapRegionList(HeapRegionList list) {
        listStorage = list.listStorage;
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

    void clear() {
        size = 0;
        tail = nullElement;
        head = nullElement;
    }

    private void appendNonEmpty(int elem) {
        setNext(tail, elem);
        init(elem, nullElement, tail);
        tail = elem;
        size++;
    }

    /**
     * Append element at the end of the list.
     * @param elem element to append
     */
    void append(int elem) {
        FatalError.check(elem != nullElement, "must not append null element");
        if (isEmpty()) {
            head = elem;
            tail = elem;
            clear(elem);
            size = 1;
        } else {
            // Add to tail
            appendNonEmpty(elem);
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
            head = elem;
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

    int removeHead() {
        if (isEmpty()) {
            return nullElement;
        }
        int elem = head;
        head = next(elem);
        setNext(elem, nullElement);
        if (head != nullElement) {
            setPrev(head, nullElement);
        } else {
            tail = nullElement;
        }
        size--;
        return elem;
    }

    int removeTail() {
        if (isEmpty()) {
            return nullElement;
        }
        int elem = tail;
        tail = prev(elem);
        setPrev(elem, nullElement);
        if (tail == nullElement) {
            head = nullElement;
        }
        size--;
        return elem;
    }

    /**
     * Remove element from the list.
     * @param elem
     */
    void remove(int elem) {
        FatalError.check(elem != nullElement, "Must be a valid list element");
        if (MaxineVM.isDebug() && !contains(elem)) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Element ");
            Log.print(elem);
            Log.println(" must be in list");
            Log.unlock(lockDisabledSafepoints);
            FatalError.unexpected("Invalid argument to list removal");
        }
        int nextElem = next(elem);
        int prevElem = prev(elem);

        // favor head removal (most frequent occurrence)
        if (prevElem == nullElement) {
            FatalError.check(elem == head, "Only the head can have null prev element");
            if (nextElem == nullElement) {
                FatalError.check(head == tail, "Only singleton list can have both prev and next null element");
                head = nullElement;
                tail = nullElement;
            } else {
                head = nextElem;
                setPrev(head, nullElement);
            }
        } else if (nextElem == nullElement) {
            tail = prevElem;
            setNext(tail, nullElement);
        } else {
            setNext(prevElem, nextElem);
            setPrev(nextElem, prevElem);
        }
        clear(elem);
        size--;
    }

    void appendAndClear(HeapRegionList list) {
        if (list.isEmpty()) {
            return;
        }
        append(list);
        list.clear();
    }

    private void append(int head, int tail) {
        if (isEmpty()) {
            this.head = head;
        } else {
            setNext(this.tail, head);
            setPrev(head, this.tail);
        }
        this.tail = tail;
    }

    private void prepend(int head, int tail) {
        if (isEmpty()) {
            this.tail = tail;
        } else {
            setPrev(this.head, tail);
            setNext(tail, this.head);
        }
        this.head = head;
    }


    void append(HeapRegionList list) {
        FatalError.check(list.listStorage == listStorage, "can only merge list with same listStorage");
        append(list.head, list.tail);
        size += list.size;
    }

    void prepend(HeapRegionList list) {
        FatalError.check(list.listStorage == listStorage, "can only merge list with same listStorage");
        prepend(list.head, list.tail);
        size += list.size;
    }

    void prependRange(int rangeHead, int rangeTail) {
        prepend(rangeHead, rangeTail);
        size += rangeTail - rangeHead + 1;
    }

    void appendRange(int rangeHead, int rangeTail) {
        append(rangeHead, rangeTail);
        size += rangeTail - rangeHead + 1;
    }

    void linkRange(int rangeHead, int rangeTail) {
        int r = rangeHead;
        setPrev(r, nullElement);
        while (r < rangeTail) {
            int n = r + 1;
            setNext(r, n);
            setPrev(n, r);
            r = n;
        }
        setNext(rangeTail, nullElement);
    }

    void insertRangeAfter(int elem, int rangeHead, int rangeTail) {
        if (elem == tail) {
            appendRange(rangeHead, rangeTail);
        } else {
            int nextElem = next(elem);
            setNext(elem, rangeHead);
            setPrev(nextElem, rangeTail);
            size += rangeTail - rangeHead + 1;
        }
    }

    /**
     * Remove a contiguous range of regions from the list.
     */
    void removeRange(int rangeHead, int rangeTail) {
        FatalError.check(rangeHead != nullElement, "Must be a valid list element");
        if (MaxineVM.isDebug() && !containsRange(rangeHead, rangeTail)) {
            final boolean lockDisabledSafepoints = Log.lock();
            Log.print("Range [");
            Log.print(rangeHead);
            Log.print(", ");
            Log.print(rangeTail);
            Log.println("] must be a valid regions range in the list");
            Log.unlock(lockDisabledSafepoints);
            FatalError.unexpected("Invalid argument to list removal");
        }
        int nextElem = next(rangeTail);
        int prevElem = prev(rangeHead);
        if (nextElem == nullElement) {
            FatalError.check(rangeTail == tail, "Only the tail can have null next element");
            if (prevElem == nullElement) {
                FatalError.check(size ==  rangeTail - rangeHead + 1, "the list must be equal to the range");
                head = nullElement;
                tail = nullElement;
            } else {
                tail = prevElem;
                setNext(tail, nullElement);
            }
        } else if (prevElem == nullElement) {
            FatalError.check(rangeHead == head, "Only the head can have null prev element");
            head = nextElem;
            setPrev(head, nullElement);
        } else {
            setNext(prevElem, nextElem);
            setPrev(nextElem, prevElem);
        }
        size -= rangeTail - rangeHead + 1;
    }

    /**
     * Returns a boolean indicating whether the specified range is a valid range within the shared linked list storage.
     * A valid range is one in which all the elements are contiguous and linked in increasing region order in the list storage.
     * @param rangeHead first region in the range
     * @param rangeTail last region in the range
     * @return true if the range is valid
     */
    private boolean isValidRange(int rangeHead, int rangeTail) {
        int cursor = rangeHead;
        while (cursor != nullElement) {
            if (cursor == rangeTail) {
                return true;
            }
            int n = next(cursor);
            if (n != ++cursor) {
                break;
            }
        }
        return false;
    }

    boolean containsRange(int rangeHead, int rangeTail) {
        int cursor = head;
        while (cursor != nullElement) {
            if (cursor == rangeHead) {
                return isValidRange(rangeHead, rangeTail);
            }
            cursor = next(cursor);
        }
        return false;
    }

    void checkIsAddressOrdered() {
        if (size < 2) {
            return;
        }
        int last = head;
        do {
            int next = next(last);
            FatalError.check(next > last, "region list isn't address-ordered");
            last = next;
        } while (last != tail);
    }
}

