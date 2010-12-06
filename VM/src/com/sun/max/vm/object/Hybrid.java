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
 *
 * @author Bernd Mathiske
 */
public abstract class Hybrid {

    /**
     * Hybrid objects cannot be directly represented in the Java language,
     * so during bootstrapping that are represented as tuples plus an expansion.
     *
     * @author Bernd Mathiske
     */
    @HOSTED_ONLY
    private static class Expansion {
        final Hybrid hybrid;
        final Word[] words;
        final int[] ints;

        Expansion(Hybrid hybrid, int length) {
            this.hybrid = hybrid;
            this.words = new Word[length];
            this.ints = new int[(length * Word.size()) / Ints.SIZE];
        }
    }

    /**
     * A map that stores the association between a hybrid and its expansion.
     */
    @HOSTED_ONLY
    private static final Map<Hybrid, Expansion> hybridToExpansion = Collections.synchronizedMap(new IdentityHashMap<Hybrid, Expansion>());

    protected Hybrid() {
    }

    /**
     * Expand the given initial hybrid object with fields to a fully fledged hybrid with array features and space.
     *
     * @param length the Word array length of the resulting hybrid
     * @return the expanded hybrid with array features
     */
    public final Hybrid expand(int length) {
        if (MaxineVM.isHosted()) {
            final Expansion oldValue = hybridToExpansion.put(this, new Expansion(this, length));
            assert oldValue == null;
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
            return hybridToExpansion.get(this).words.length;
        }
        return ArrayAccess.readArrayLength(this);
    }

    /**
     * Write a word into the word array portion of this hybrid at the specified offset.
     * @param wordIndex the index into the word array at the end of this hybrid
     * @param value the value to write into the word array
     */
    @INLINE
    public final void setWord(int wordIndex, Word value) {
        if (MaxineVM.isHosted()) {
            assert wordIndex >= firstWordIndex();
            final Expansion expansion = hybridToExpansion.get(this);
            WordArray.set(expansion.words, wordIndex, value);
        } else {
            ArrayAccess.setWord(this, wordIndex, value);
        }
    }

    /**
     * Read a word from the word array portion of this hybrid at the specified offset.
     * @param wordIndex the index into the word array at the end of this hybrid
     * @return the value of the array at the specified index
     */
    @INLINE
    public final Word getWord(int wordIndex) {
        if (MaxineVM.isHosted()) {
            assert wordIndex >= firstWordIndex();
            final Expansion expansion = hybridToExpansion.get(this);
            return WordArray.get(expansion.words, wordIndex);
        }
        return ArrayAccess.getWord(this, wordIndex);
    }

    /**
     * Write an int into the array portion of this hybrid object at the specified offset.
     * @param intIndex the index into the array portion at the end of this hybrid
     * @param value the new value to write into the array portion
     */
    @INLINE
    public final void setInt(int intIndex, int value) {
        if (MaxineVM.isHosted()) {
            assert intIndex >= firstIntIndex();
            final Expansion expansion = hybridToExpansion.get(this);
            expansion.ints[intIndex] = value;
        } else {
            ArrayAccess.setInt(this, intIndex, value);
        }
    }

    /**
     * Read an int from the array portion of this hybrid object at the specified offset.
     * @param intIndex the index into the array portion at the end of this hybrid
     * @return the value of the array at the specified index
     */
    @INLINE
    public final int getInt(int intIndex) {
        if (MaxineVM.isHosted()) {
            assert intIndex >= firstIntIndex();
            final Expansion expansion = hybridToExpansion.get(this);
            return expansion.ints[intIndex];
        }
        return ArrayAccess.getInt(this, intIndex);
    }
}
