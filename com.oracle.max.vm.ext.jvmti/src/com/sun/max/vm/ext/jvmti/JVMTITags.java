/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.ext.jvmti;

import java.lang.ref.*;
import java.util.*;

import static com.sun.max.vm.ext.jvmti.JVMTIConstants.*;

import com.sun.max.memory.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.jni.*;

/**
 * JVMTI object tagging support. The tag map is allocated lazily. We cannot use a standard {@link WeakHashMap} because
 * that invokes the class-specific {@link Object#hashCode()} method, which can have all kinds of inappropriate side
 * effects. Plus we only need a handful of the standard {@link Map} methods.
 *
 * We support {@link Object} (for JJVMTI) and {@code long} values, not via {@link Long} to avoid unnecessary allocation.
 *
 */
class JVMTITags {

    /**
     * A cut down and modified version of {@link WeakHashMap}.
     * The value 0 is not allowed, and used to mean lookup failure.
     */
    static class Map {
        static abstract class Entry extends WeakReference<Object> {
            final int hash;
            Entry next;

            Entry(Object object, int hash, Entry next) {
                super(object);
                this.hash = hash;
                this.next = next;
            }

        }

        static class LongEntry extends Entry {
            long value;
            LongEntry(Object object, long value, int hash, Entry next) {
                super(object, hash, next);
                this.value = value;
            }

        }

        static class ObjectEntry extends Entry {
            Object value;
            ObjectEntry(Object object, Object value, int hash, Entry next) {
                super(object, hash, next);
                this.value = value;
            }
        }

        private static final int DEFAULT_INITIAL_CAPACITY = 16;
        private static final int MAXIMUM_CAPACITY = 1 << 30;
        private static final float DEFAULT_LOAD_FACTOR = 0.75f;
        private int size;
        private int threshold;
        private final float loadFactor;
        private Entry[] table;

        private Entry[] getTable() {
            // expungeStaleEntries(); TODO
            return table;
        }

        Map(boolean isNative) {
            this.loadFactor = DEFAULT_LOAD_FACTOR;
            threshold = DEFAULT_INITIAL_CAPACITY;
            if (isNative) {
                table = new LongEntry[DEFAULT_INITIAL_CAPACITY];
            } else {
                table = new ObjectEntry[DEFAULT_INITIAL_CAPACITY];
            }
        }

        static int indexFor(int h, int length) {
            return h & (length - 1);
        }

        void put(Object key, long value) {
            LongEntry e = (LongEntry) putCommon(key, true);
            e.value = value;
        }

        void put(Object key, Object value) {
            ObjectEntry e = (ObjectEntry) putCommon(key, false);
            e.value = value;
        }

        private Entry putCommon(Object key, boolean isNative) {
            int h = System.identityHashCode(key);
            Entry[] tab = getTable();
            int i = indexFor(h, tab.length);

            for (Entry e = tab[i]; e != null; e = e.next) {
                if (h == e.hash && key == e.get()) {
                    return e;
                }
            }

            Entry e = tab[i];
            Entry ne = isNative ? new LongEntry(key, 0, h, e) : new ObjectEntry(key, null, h, e);
            tab[i] = ne;
            if (++size >= threshold) {
                resize(tab.length * 2);
            }
            return ne;
        }

        long getLong(Object key) {
            LongEntry e = (LongEntry) getCommon(key);
            return e == null ? 0 : e.value;
        }

        Object getObject(Object key) {
            ObjectEntry e = (ObjectEntry) getCommon(key);
            return e == null ? null : e.value;
        }

        private Entry getCommon(Object key) {
            int h = System.identityHashCode(key);
            Entry[] tab = getTable();
            int index = indexFor(h, tab.length);
            Entry e = tab[index];
            while (e != null) {
                if (e.hash == h && e.get() == key) {
                    return e;
                }
                e = e.next;
            }
            return null;
        }

        public void remove(Object key) {
            int h = System.identityHashCode(key);
            Entry[] tab = getTable();
            int i = indexFor(h, tab.length);
            Entry prev = tab[i];
            Entry e = prev;

            while (e != null) {
                Entry next = e.next;
                if (h == e.hash && key == e.get()) {
                    size--;
                    if (prev == e) {
                        tab[i] = next;
                    } else {
                        prev.next = next;
                    }
                    return;
                }
                prev = e;
                e = next;
            }
        }

        void resize(int newCapacity) {
            Entry[] oldTable = getTable();
            int oldCapacity = oldTable.length;
            if (oldCapacity == MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return;
            }

            Entry[] newTable = new Entry[newCapacity];
            transfer(oldTable, newTable);
            table = newTable;

            /*
             * If ignoring null elements and processing ref queue caused massive
             * shrinkage, then restore old table.  This should be rare, but avoids
             * unbounded expansion of garbage-filled tables.
             */
            if (size >= threshold / 2) {
                threshold = (int) (newCapacity * loadFactor);
            } else {
                // expungeStaleEntries();
                transfer(newTable, oldTable);
                table = oldTable;
            }
        }

        private void transfer(Entry[] src, Entry[] dest) {
            for (int j = 0; j < src.length; ++j) {
                Entry e = src[j];
                src[j] = null;
                while (e != null) {
                    Entry next = e.next;
                    Object key = e.get();
                    if (key == null) {
                        e.next = null;  // Help GC
                        size--;
                    } else {
                        int i = indexFor(e.hash, dest.length);
                        e.next = dest[i];
                        dest[i] = e;
                    }
                    e = next;
                }
            }
        }

        static abstract class Visitor {
            abstract void visit(Entry e);
            Object getResult() {
                return null;
            }
        }

        Visitor apply(Visitor visitor) {
            for (int t = 0; t < table.length; t++) {
                Map.Entry e = table[t];
                while (e != null) {
                    if (e.get() != null) {
                        visitor.visit(e);
                    }
                    e = e.next;
                }
            }
            return visitor;
        }

    }

    private Map tagMap;

    /*
     * Next two functions are for use by JVMTIHeapFunctions, where no synchronization is necessary.
     */

    boolean isTagged(Object object) {
        if (tagMap == null) {
            return false;
        } else {
            return tagMap.getCommon(object) != null;
        }
    }

    long getLongTag(Object object) {
        return checkMap(true).getLong(object);
    }

    Object getObjectTag(Object object) {
        return checkMap(false).getObject(object);
    }

    /*
     * Implementation of API methods where we need to protect against concurrent access.
     */

    synchronized int getTag(Object object, Pointer tagPtr) {
        long tag = checkMap(true).getLong(object);
        tagPtr.writeLong(0, tag);
        return JVMTI_ERROR_NONE;
    }

    synchronized int setTag(Object object, long tag) {
        if (tag == 0) {
            checkMap(true).remove(object);
        } else {
            checkMap(true).put(object, tag);
        }
        return JVMTI_ERROR_NONE;
    }

    public synchronized void setTag(Object object, Object tag) {
        checkMap(false).put(object, tag);
    }

    public synchronized Object getTag(Object object) {
        return checkMap(false).getObject(object);
    }

    private Map checkMap(boolean isNative) {
        if (tagMap == null) {
            tagMap  = new Map(isNative);
        }
        return tagMap;
    }

    synchronized int getObjectsWithTags(final int tagCount, final Pointer tags, Pointer countPtr, Pointer objectResultPtrPtr, Pointer tagResultPtrPtr) {
        if (tagCount < 0) {
            return JVMTI_ERROR_ILLEGAL_ARGUMENT;
        }
        for (int i = 0; i < tagCount; i++) {
            long givenTag = tags.getInt(i);
            if (givenTag == 0) {
                return JVMTI_ERROR_ILLEGAL_ARGUMENT;
            }
        }

        Map.Visitor counter = new Map.Visitor() {
            private int count;

            @Override
            void visit(Map.Entry e) {
                for (int i = 0; i < tagCount; i++) {
                    long givenTag = tags.getInt(i);
                    if (givenTag == ((Map.LongEntry) e).value) {
                        count++;
                    }
                }
            }

            @Override
            Object getResult() {
                return new Integer(count);
            }
        };

        final int count = (Integer) tagMap.apply(counter).getResult();
        // count is an upper bound, entries may disappear through GC actions
        final Pointer objectResultPtr = objectResultPtrPtr.isZero() ? Pointer.zero() : Memory.allocate(Size.fromInt(count * Word.size()));
        if (objectResultPtr.isZero()) {
            return JVMTI_ERROR_OUT_OF_MEMORY;
        }
        final Pointer tagResultPtr = tagResultPtrPtr.isZero() ? Pointer.zero() : Memory.allocate(Size.fromInt(count * Word.size()));
        if (tagResultPtr.isZero()) {
            Memory.deallocate(objectResultPtr);
            return JVMTI_ERROR_OUT_OF_MEMORY;
        }

        Map.Visitor copier = new Map.Visitor() {
            @Override
            void visit(Map.Entry e) {
                int index = 0;
                Object key = e.get();
                if (key != null) {
                    for (int i = 0; i < tagCount; i++) {
                        long givenTag = tags.getInt(i);
                        if (givenTag == ((Map.LongEntry) e).value) {
                            assert index < count;
                            if (!objectResultPtr.isZero()) {
                                objectResultPtr.setWord(index, JniHandles.createLocalHandle(key));
                            }
                            if (!tagResultPtr.isZero()) {
                                tagResultPtr.setLong(index, givenTag);
                            }
                            index++;
                            break;
                        }
                    }
                }
            }
        };
        tagMap.apply(copier);
        countPtr.setInt(count);
        return JVMTI_ERROR_NONE;
    }
}
