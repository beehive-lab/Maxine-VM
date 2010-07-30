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

package com.sun.max.vm.heap.gcx;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Region list management.
 * Heaps organize their memory into non-overlapping spaces made of (possibly) discontinuous regions.
 * Spaces keep track of their regions in list of regions.
 * Thus a region always belong at any one time to a region list.
 *
 * @author Laurent Daynes
 */
public class ThreadedRegionList {
    /**
     * Pointer to raw storage of the list.
     */
    final int nullElement;
    final Pointer listStorage;

    static final int fieldNumberOfBytes = KindEnum.INT.asKind().width.numberOfBytes;
    static final int log2FieldNumberOfBytes = KindEnum.INT.asKind().width.log2numberOfBytes;
    static final int log2Elem = 1 + log2FieldNumberOfBytes;
    static final int NEXT_INDEX = 0;
    static final int PREV_INDEX = 1;

    private int head;
    private int tail;

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

    private int next(int elem) {
        return listStorage.getInt(nextFieldIndex(elem));
    }

    private int prev(int elem) {
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
        tail = nullElement;
        head = nullElement;
    }

    ThreadedRegionList(Pointer backingStorage, Size length) {
        listStorage = backingStorage;
        nullElement = length.toInt();
        clear();
    }

    public boolean isEmpty() {
        return head == nullElement;
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
        } else {
            // Add to tail
            setNext(tail, elem);
            init(elem, nullElement, tail);
            tail = elem;
        }
    }

    void prepend(int elem) {
        if (isEmpty()) {
            head = elem;
            tail = elem;
            clear(elem);
        } else {
            // Insert at head
            setPrev(head, elem);
            init(elem, head, nullElement);
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
        }
    }

    /**
     * Remove element from the list.
     * @param elem
     */
    void remove(int elem) {
        FatalError.check(elem != nullElement, "Must be a valid list element");
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

    void append(ThreadedRegionList list) {
        if (isEmpty()) {
            head = list.head;
        } else {
            setNext(tail, list.head);
            setPrev(list.head, tail);
        }
        tail = list.tail;
    }

    void prepend(ThreadedRegionList list) {
        if (isEmpty()) {
            tail = list.tail;
        } else {
            setPrev(head, list.tail);
            setNext(list.tail, head);
        }
        head = list.head;
    }

}

