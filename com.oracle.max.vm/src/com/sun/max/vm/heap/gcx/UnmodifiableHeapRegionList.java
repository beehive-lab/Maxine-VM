/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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

public final class UnmodifiableHeapRegionList extends HeapRegionList {

    @Override
    void linkRange(int rangeHead, int rangeTail) {
        throw new UnsupportedOperationException();
    }

    @Override
    void prependRange(int rangeHead, int rangeTail) {
        throw new UnsupportedOperationException();
    }

    @Override
    void appendRange(int rangeHead, int rangeTail) {
        throw new UnsupportedOperationException();
    }

    @Override
    void insertRangeAfter(int elem, int rangeHead, int rangeTail) {
        throw new UnsupportedOperationException();
    }

    @Override
    void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    void append(int elem) {
        throw new UnsupportedOperationException();
    }

    @Override
    void prepend(int elem) {
        throw new UnsupportedOperationException();
    }

    @Override
    void insertAfter(int elem, int newElem) {
        throw new UnsupportedOperationException();
    }

    @Override
    int removeHead() {
        throw new UnsupportedOperationException();
    }

    @Override
    int removeTail() {
        throw new UnsupportedOperationException();
    }

    @Override
    void remove(int elem) {
        throw new UnsupportedOperationException();
    }

    @Override
    void append(HeapRegionList list) {
        throw new UnsupportedOperationException();
    }

    @Override
    void prepend(HeapRegionList list) {
        throw new UnsupportedOperationException();
    }

    private HeapRegionList wrappedModifiableRegionList;

    UnmodifiableHeapRegionList(HeapRegionList modifiable) {
        super(modifiable);
        wrappedModifiableRegionList = modifiable;
    }

    @Override
    public int size() {
        return wrappedModifiableRegionList.size();
    }

    @Override
    public int head() {
        return wrappedModifiableRegionList.head();
    }

    @Override
    public int tail() {
        return wrappedModifiableRegionList.tail();
    }

    @Override
    public boolean isEmpty() {
        return wrappedModifiableRegionList.isEmpty();
    }

    @Override
    public boolean contains(int elem) {
        return wrappedModifiableRegionList.contains(elem);
    }

    @Override
    void checkIsAddressOrdered() {
        wrappedModifiableRegionList.checkIsAddressOrdered();
    }
}
