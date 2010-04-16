/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.cri.ci;

/**
 * Represents a spill slot or an outgoing stack-based argument in a method's frame.
 *
 * @author Doug Simon
 */
public final class CiStackSlot extends CiValue {

    /**
     * The index of this stack slot within the spill area of a method's frame.
     * This index will be converted to an offset relative to {@link CiRegister#Frame}.
     */
    public final int index;

    /**
     * Gets a {@link CiStackSlot} instance representing a stack slot at a given index
     * holding a value of a given kind.
     *
     * @param kind the kind of the value stored in the stack slot
     * @param index the index of the stack slot
     */
    public static CiStackSlot get(CiKind kind, int index) {
        assert kind.stackKind() == kind;
        CiStackSlot[] slots = cache[kind.ordinal()];
        if (index < slots.length) {
            return slots[index];
        }
        return new CiStackSlot(kind, index);
    }

    /**
     * Private constructor to enforce use of {@link #get(CiKind, int)} so that the
     * shared instance {@linkplain #cache cache} is used.
     */
    private CiStackSlot(CiKind kind, int index) {
        super(kind);
        assert index >= 0;
        this.index = index;
    }

    @Override
    public int hashCode() {
        return kind.ordinal() + index;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof CiStackSlot) {
            CiStackSlot l = (CiStackSlot) o;
            return l.kind == kind && l.index == index;
        }
        return false;
    }

    @Override
    public String name() {
        return "stack:" + index;
    }

    /**
     * Creates an array of {@core CiStackSlot} objects for a given {@link CiKind}.
     * The {@link #index} values range from {@code 0} to {@code count - 1}.
     * @param kind the {@code CiKind} of the stack slot
     * @param count the size of the array to create
     * @return the generated {@code CiStackSlot} array
     */
    private static CiStackSlot[] generate(CiKind kind, int count) {
        CiStackSlot[] slots = new CiStackSlot[count];
        for (int i = 0; i < count; ++i) {
            slots[i] = new CiStackSlot(kind, i);
        }
        return slots;
    }

    /**
     * Default size of the cache to generate per kind.
     */
    private static final int CACHE_PER_KIND_SIZE = 100;

    /**
     * The cache of {@link CiStackSlot} objects, all created at startup.
     */
    private static final CiStackSlot[][] cache = new CiStackSlot[CiKind.values().length][];
    static {
        cache[CiKind.Int.ordinal()]    = generate(CiKind.Int, CACHE_PER_KIND_SIZE);
        cache[CiKind.Long.ordinal()]   = generate(CiKind.Long, CACHE_PER_KIND_SIZE);
        cache[CiKind.Float.ordinal()]  = generate(CiKind.Float, CACHE_PER_KIND_SIZE);
        cache[CiKind.Double.ordinal()] = generate(CiKind.Double, CACHE_PER_KIND_SIZE);
        cache[CiKind.Word.ordinal()]   = generate(CiKind.Word, CACHE_PER_KIND_SIZE);
        cache[CiKind.Object.ordinal()] = generate(CiKind.Object, CACHE_PER_KIND_SIZE);
        cache[CiKind.Jsr.ordinal()]    = generate(CiKind.Jsr, CACHE_PER_KIND_SIZE);
    }
}
