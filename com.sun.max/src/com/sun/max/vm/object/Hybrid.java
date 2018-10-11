/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.object;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.layout.*;

/**
 * Classes that extend this class define objects that have a hybrid layout,
 * meaning that they may have both fields and a word array portion (thus they
 * are a <i>hybrid</i> between classes and arrays). This is used,
 * for example, to implement hubs, which contain metadata about an
 * object as well as the vtable and itable.
 *
 * We declare "Hybrid" as an abstract class that directly subclasses {@link Object}
 * so that all hybrid objects only contain fields that refer to hybrid objects exclusively.
 * This allows tuples and hybrids object to have different header sizes which,
 * depending on the configured {@link Layout}, may affect field offsets.
 * Otherwise field offsets in tuples would have to be aligned with field offsets in hybrids.
 */
public abstract class Hybrid {

    /**
     * Hybrid objects cannot be directly represented in the Java language,
     * so during bootstrapping that are represented as tuples plus an expansion.
     *
     */
    @HOSTED_ONLY
    public static class Expansion {
        public final Hybrid hybrid;
        public final Word[] words;
        public final int[] ints;

        Expansion(Hybrid hybrid, int length) {
            this.hybrid = hybrid;
            this.words = new Word[length];
            this.ints = new int[(length * Word.size()) / Ints.SIZE];
            Arrays.fill(this.words, Address.zero());
        }
    }

    protected Hybrid() {
    }

    @HOSTED_ONLY
    public Expansion expansion;

    /**
     * Expand the given initial hybrid object with fields to a fully fledged hybrid with array features and space.
     *
     * @param length the Word array length of the resulting hybrid
     * @return the expanded hybrid with array features
     */
    public final Hybrid expand(int length) {
        if (MaxineVM.isHosted()) {
            assert expansion == null;
            expansion = new Expansion(this, length);
            return this;
        }
        return Heap.expandHybrid(this, length);
    }

    public abstract int firstWordIndex();
    public abstract int lastWordIndex();

    public abstract int firstIntIndex();
    public abstract int lastIntIndex();

    /**
     * Retrieve the length of this hybrid object. Must not be called on non-expanded hybrids.
     * @return the length of the hybrid seen as a word array.
     */
    @INLINE
    public final int length() {
        if (MaxineVM.isHosted()) {
            return expansion.words.length;
        }
        return ArrayAccess.readArrayLength(this);
    }

    /**
     * Write a word into the word array portion of this hybrid at the specified offset.
     * @param wordIndex the index into the word array at the end of this hybrid
     * @param value the value to write into the word array
     */
    public final void setWord(int wordIndex, Word value) {
        assert wordIndex >= firstWordIndex();
        expansion.words[wordIndex] = value;
    }

    @LOCAL_SUBSTITUTION
    @INLINE
    public final void setWord_(int wordIndex, Word value) {
        ArrayAccess.setWord(this, wordIndex, value);
    }

    /**
     * Read a word from the word array portion of this hybrid at the specified offset.
     * @param wordIndex the index into the word array at the end of this hybrid
     * @return the value of the array at the specified index
     */
    public final Word getWord(int wordIndex) {
        assert wordIndex >= firstWordIndex();
        return expansion.words[wordIndex];
    }

    @LOCAL_SUBSTITUTION
    @INLINE
    public final Word getWord_(int wordIndex) {
        return ArrayAccess.getWord(this, wordIndex);
    }

    /**
     * Write an int into the array portion of this hybrid object at the specified offset.
     * @param intIndex the index into the array portion at the end of this hybrid
     * @param value the new value to write into the array portion
     */
    public final void setInt(int intIndex, int value) {
        assert intIndex >= firstIntIndex();
        expansion.ints[intIndex] = value;
    }

    @LOCAL_SUBSTITUTION
    @INLINE
    public final void setInt_(int intIndex, int value) {
        ArrayAccess.setInt(this, intIndex, value);
    }

    /**
     * Read an int from the array portion of this hybrid object at the specified offset.
     * @param intIndex the index into the array portion at the end of this hybrid
     * @return the value of the array at the specified index
     */
    public final int getInt(int intIndex) {
        assert intIndex >= firstIntIndex();
        return expansion.ints[intIndex];
    }

    @LOCAL_SUBSTITUTION
    @INLINE
    public final int getInt_(int intIndex) {
        return ArrayAccess.getInt(this, intIndex);
    }
}
