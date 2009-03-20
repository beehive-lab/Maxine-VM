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
import com.sun.max.program.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.heap.*;

/**
 * Classes that extend this class define objects that have a hybrid layout,
 * meaning that they may have both fields and a word array portion (thus they
 * are a <i>hybrid</i> between classes and arrays). This is used,
 * for example, to implement hubs, which contain metadata about an
 * object as well as the vtable and itable.
 *
 * We declare "Hybrid" as an abstract class as close as possible
 * to the top of the hierarchy rather than as an interface
 * to prevent inheriting any fields before inheriting from subclasses of "Hybrid".
 * Thus we can rely on all fields existing in any hybrid class
 * referring to hybrid objects exclusively.
 * This provides the freedom to configure the object headers
 * and origins of tuples and hybrids independently,
 * i.e. otherwise field offsets in tuples would have to be aligned with field offsets in hybrids.
 *
 * @author Bernd Mathiske
 */
public abstract class Hybrid<Hybrid_Type extends Hybrid> {

    /**
     * Hybrid objects cannot be directly represented in the Java language,
     * so during prototyping that are represented as as tuples an expansion.
     *
     * @author Bernd Mathiske
     */
    @PROTOTYPE_ONLY
    private static class Expansion {
        final Hybrid _hybrid;
        final Word[] _words;
        final int[] _ints;

        Expansion(Hybrid hybrid, int length) {
            _hybrid = hybrid;
            _words = new Word[length];
            _ints = new int[(length * Word.size()) / Ints.SIZE];
        }
    }

    /**
     * A map that stores the association between a hybrid and its expansion, used only at prototyping time.
     */
    @PROTOTYPE_ONLY
    private static final Map<Hybrid, Expansion> _hybridToExpansion = Collections.synchronizedMap(new IdentityHashMap<Hybrid, Expansion>());

    protected Hybrid() {
    }

    /**
     * Expand the given initial hybrid object with fields to a fully fledged hybrid with array features and space.
     *
     * @param length the Word array length of the resulting hybrid
     * @return the expanded hybrid with array features
     */
    public final Hybrid expand(int length) {
        if (MaxineVM.isPrototyping()) {
            final Expansion oldValue = _hybridToExpansion.put(this, new Expansion(this, length));
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
        if (MaxineVM.isPrototyping()) {
            return _hybridToExpansion.get(this)._words.length;
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
        if (MaxineVM.isPrototyping()) {
            assert wordIndex >= firstWordIndex();
            final Expansion expansion = _hybridToExpansion.get(this);
            WordArray.set(expansion._words, wordIndex, value);
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
        if (MaxineVM.isPrototyping()) {
            assert wordIndex >= firstWordIndex();
            final Expansion expansion = _hybridToExpansion.get(this);
            return WordArray.get(expansion._words, wordIndex);
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
        if (MaxineVM.isPrototyping()) {
            assert intIndex >= firstIntIndex();
            final Expansion expansion = _hybridToExpansion.get(this);
            expansion._ints[intIndex] = value;
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
        if (MaxineVM.isPrototyping()) {
            assert intIndex >= firstIntIndex();
            final Expansion expansion = _hybridToExpansion.get(this);
            try {
                return expansion._ints[intIndex];
            } catch (RuntimeException e) {
                for (Map.Entry<Hybrid, Expansion> entry : _hybridToExpansion.entrySet()) {
                    Trace.line(1, entry.getKey() + " -> Word[" + entry.getValue()._words.length + "], int[" + entry.getValue()._ints.length + "]");
                }
                throw ProgramError.unexpected("Error while accessing expansion for " + this, e);
            }
        }
        return ArrayAccess.getInt(this, intIndex);
    }
}
