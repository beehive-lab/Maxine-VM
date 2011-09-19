/*
 * Copyright (c) 2004, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vma.tools.qa;

import java.util.*;

import com.oracle.max.vm.ext.vma.runtime.TransientVMAdviceHandlerTypes.AdviceRecord;

/**
 * Manages the list of {@link } instances for an {@link ObjectRecord}.
 * This is where most of the space goes, so it pays to have a custom implementation
 * that tries to use the minimum space. There are the following implementations:
 * <ul>
 * <li>{@link Empty the empty list} which is how we start
 * <li>{@link Singleton one element}
 * <li>{@link Doubleton two elements}
 * <li>{@link SimpleArray array from 3 to 7 elements}
 * <li>{@link ChunkedArray lists 8 elements and larger} stored as a list of chunks of size 8
 * </ul>
 *
 * @author Mick Jordan
 *
 */
public class GrowableArrayImpl {

    private GrowableArrayImpl() {
    }

    public static GrowableArray create() {
        return new Empty();
    }

    static abstract class GAIteratorAdaptor implements Iterator<AdviceRecord> {
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    static class Empty extends GrowableArray {

        @Override
        public GrowableArray add(AdviceRecord te) {
            return new Singleton(te);
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public AdviceRecord get(int index) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public Iterator<AdviceRecord> iterator() {
            return new GAIterator();
        }

        private static class GAIterator extends GAIteratorAdaptor {
            public boolean hasNext() {
                return false;
            }

            public AdviceRecord next() {
                throw new NoSuchElementException();
            }
        }
    }

    /**
     * Single element.
     */
    static class Singleton extends GrowableArray {
        private final AdviceRecord te;

        Singleton(AdviceRecord te) {
            this.te = te;
        }

        @Override
        public GrowableArray add(AdviceRecord te) {
            return new Doubleton(this.te, te);
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public AdviceRecord get(int index) {
            if (index > 0) {
                throw new IndexOutOfBoundsException();
            }
            return te;
        }

        @Override
        public Iterator<AdviceRecord> iterator() {
            return new GAIterator();
        }

        private class GAIterator extends GAIteratorAdaptor {
            private boolean processed;
            public boolean hasNext() {
                return !processed;
            }

            public AdviceRecord next() {
                processed = true;
                return te;
            }
        }
    }

    /**
     * Two elements.
     *
     */
    static class Doubleton extends GrowableArray {
        private final AdviceRecord first;
        private final AdviceRecord second;

        Doubleton(AdviceRecord first, AdviceRecord second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public AdviceRecord get(int index) {
            if (index > 1) {
                throw new IndexOutOfBoundsException();
            }
            return index  == 0 ? first : second;

        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public GrowableArray add(AdviceRecord te) {
            GrowableArray result = new SimpleArray(first, second, te);
            return result;
        }

        @Override
        public Iterator<AdviceRecord> iterator() {
            return new GAIterator();
        }

        private class GAIterator extends GAIteratorAdaptor {
            private int iterIndex;
            public boolean hasNext() {
                return iterIndex < 2;
            }

            public AdviceRecord next() {
                return iterIndex++ == 0 ? first : second;
            }
        }
    }

    static class SimpleArray extends GrowableArray {
        AdviceRecord[] array;
        int nextIndex;

        SimpleArray(AdviceRecord first, AdviceRecord second, AdviceRecord third) {
            array = new AdviceRecord[3];
            array[0] = first;
            array[1] = second;
            array[2] = third;
            nextIndex = 3;
        }

        @Override
        public GrowableArray add(AdviceRecord element) {
            if (nextIndex < array.length) {
                array[nextIndex++] = element;
                return this;
            } else if (array.length < ChunkedArray.CHUNK_SIZE - 1) {
                final AdviceRecord[] newArray = new AdviceRecord[array.length + 1];
                System.arraycopy(array, 0, newArray, 0, array.length);
                array = newArray;
                array[nextIndex++] = element;
                return this;
            } else {
                final GrowableArray result = new ChunkedArray(array);
                result.add(element);
                return result;
            }
        }

        @Override
        public AdviceRecord get(int index) {
            return array[index];
        }

        @Override
        public int size() {
            return array.length;
        }

        @Override
        public Iterator<AdviceRecord> iterator() {
            return new GAIterator();
        }

        private class GAIterator extends GAIteratorAdaptor {
            private int iterIndex;
            public boolean hasNext() {
                return iterIndex < nextIndex;
            }

            public AdviceRecord next() {
                return array[iterIndex++];
            }
        }

    }

    /**
     * An extensible array based on a list of chunks, each of which is an array.
     * For large lists.
     */
    static class ChunkedArray extends GrowableArray {
        private ArrayList<AdviceRecord[]> chunks = new ArrayList<AdviceRecord[]>();
        private AdviceRecord[] currentChunk;
        private int indexInChunk;
        private static final int CHUNK_SIZE = 8;

        ChunkedArray(AdviceRecord[] starter) {
            for (AdviceRecord te : starter) {
                add(te);
            }
        }

        @Override
        public GrowableArray add(AdviceRecord element) {
            if (currentChunk == null || indexInChunk >= CHUNK_SIZE) {
                currentChunk = new AdviceRecord[CHUNK_SIZE];
                chunks.add(currentChunk);
                indexInChunk = 0;
            }
            currentChunk[indexInChunk++] = element;
            return this;
        }

        @Override
        public AdviceRecord get(int index) {
            final int chunkIndex = index / CHUNK_SIZE;
            final int thisIndexInChunk = index % CHUNK_SIZE;
            return chunks.get(chunkIndex)[thisIndexInChunk];
        }

        @Override
        public int size() {
            if (currentChunk == null) {
                return 0;
            } else {
                return ((chunks.size() - 1) * CHUNK_SIZE) + indexInChunk;
            }
        }

        @Override
        public Iterator<AdviceRecord> iterator() {
            return new ThisIterator();
        }

        class ThisIterator implements Iterator<AdviceRecord> {
            private AdviceRecord[] iterCurrentChunk;
            private int iterChunkIndex;
            private int iterIndexInChunk;
            private int iterChunkLimit;

            ThisIterator() {
                iterCurrentChunk = chunks.size() > 0 ? chunks.get(0) : null;
                iterChunkLimit = chunks.size() == 1 ? indexInChunk : CHUNK_SIZE;
            }

            public boolean hasNext() {
                return iterCurrentChunk != null
                        && iterIndexInChunk < iterChunkLimit;
            }

            public AdviceRecord next() {
                final AdviceRecord result = iterCurrentChunk[iterIndexInChunk++];
                if (iterIndexInChunk >= iterChunkLimit) {
                    iterIndexInChunk = 0;
                    iterChunkIndex++;
                    iterCurrentChunk = iterChunkIndex < chunks.size() ? chunks.get(iterChunkIndex) : null;
                    iterChunkLimit = iterChunkIndex == chunks.size() - 1 ? indexInChunk
                            : CHUNK_SIZE;
                }
                return result;
            }

            public void remove() {

            }
        }
    }

}
