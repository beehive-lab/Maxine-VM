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
 * Represents a compiler spill slot or an outgoing stack-based argument in a method's frame
 * or an incoming stack-based argument in a method's {@linkplain #inCallerFrame() caller's frame}.
 *
 * @author Doug Simon
 */
public final class CiStackSlot extends CiValue {

    /**
     * @see CiStackSlot#index()
     */
    private final int index;

    /**
     * Gets a {@link CiStackSlot} instance representing a stack slot in the current frame
     * at a given index holding a value of a given kind.
     *  
     * @param kind the kind of the value stored in the stack slot
     * @param index the index of the stack slot
     */
    public static CiStackSlot get(CiKind kind, int index) {
        return get(kind, index, false);
    }
    
    /**
     * Gets a {@link CiStackSlot} instance representing a stack slot at a given index
     * holding a value of a given kind.
     *
     * @param kind the kind of the value stored in the stack slot
     * @param index the index of the stack slot
     * @param inCallerFrame specifies if the slot is in the current frame or in the caller's frame
     */
    public static CiStackSlot get(CiKind kind, int index, boolean inCallerFrame) {
        assert kind.stackKind() == kind;
        CiStackSlot[][] cache = inCallerFrame ? CALLER_FRAME_CACHE : CACHE;
        CiStackSlot[] slots = cache[kind.ordinal()];
        CiStackSlot slot;
        if (index < slots.length) {
            slot = slots[index];
        } else {
            slot = new CiStackSlot(kind, inCallerFrame ? -(index + 1) : index);
        }
        assert slot.inCallerFrame() == inCallerFrame;
        return slot; 
    }

    /**
     * Private constructor to enforce use of {@link #get(CiKind, int)} so that the
     * shared instance {@linkplain #CACHE cache} is used.
     */
    private CiStackSlot(CiKind kind, int index) {
        super(kind);
        this.index = index;
    }

    /**
     * Gets the index of this stack slot. If this is a spill slot or outgoing stack argument to a call,
     * then the index is relative to the current frame pointer. Otherwise this is an incoming stack
     * argument and the index is relative to the caller frame pointer.
     * 
     * @return the index of this slot
     * @see #inCallerFrame()
     */
    public int index() {
        return index < 0 ? -(index + 1) : index;
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
        return (inCallerFrame() ? "caller-stack" : "stack:") + index();
    }
    
    /**
     * Determines if this is a stack slot in the caller's frame.
     */
    public boolean inCallerFrame() {
        return index < 0;
    }

    /**
     * Default size of the cache to generate per kind.
     */
    private static final int CACHE_PER_KIND_SIZE = 100;
    
    private static final int CALLER_FRAME_CACHE_PER_KIND_SIZE = 10;
    
    /**
     * A cache of {@linkplain #inCallerFrame() non-caller-frame} stack slots.
     */
    private static final CiStackSlot[][] CACHE = makeCache(CACHE_PER_KIND_SIZE, false);
    
    /**
     * A cache of {@linkplain #inCallerFrame() caller-frame} stack slots.
     */
    private static final CiStackSlot[][] CALLER_FRAME_CACHE = makeCache(CALLER_FRAME_CACHE_PER_KIND_SIZE, true);

    private static CiStackSlot[][] makeCache(int cachePerKindSize, boolean inCallerFrame) {
        CiStackSlot[][] cache = new CiStackSlot[CiKind.VALUES.length][];
        cache[CiKind.Int.ordinal()]    = makeCacheForKind(CiKind.Int, cachePerKindSize, inCallerFrame);
        cache[CiKind.Long.ordinal()]   = makeCacheForKind(CiKind.Long, cachePerKindSize, inCallerFrame);
        cache[CiKind.Float.ordinal()]  = makeCacheForKind(CiKind.Float, cachePerKindSize, inCallerFrame);
        cache[CiKind.Double.ordinal()] = makeCacheForKind(CiKind.Double, cachePerKindSize, inCallerFrame);
        cache[CiKind.Word.ordinal()]   = makeCacheForKind(CiKind.Word, cachePerKindSize, inCallerFrame);
        cache[CiKind.Object.ordinal()] = makeCacheForKind(CiKind.Object, cachePerKindSize, inCallerFrame);
        cache[CiKind.Jsr.ordinal()]    = makeCacheForKind(CiKind.Jsr, cachePerKindSize, inCallerFrame);
        return cache;
    }
    
    /**
     * Creates an array of {@code CiStackSlot} objects for a given {@link CiKind}.
     * The {@link #index} values range from {@code 0} to {@code count - 1}.
     * 
     * @param kind the {@code CiKind} of the stack slot
     * @param count the size of the array to create
     * @return the generated {@code CiStackSlot} array
     */
    private static CiStackSlot[] makeCacheForKind(CiKind kind, int count, boolean inCallerFrame) {
        CiStackSlot[] slots = new CiStackSlot[count];
        for (int i = 0; i < count; ++i) {
            slots[i] = new CiStackSlot(kind, inCallerFrame ? -(i + 1) : i);
        }
        return slots;
    }
}
