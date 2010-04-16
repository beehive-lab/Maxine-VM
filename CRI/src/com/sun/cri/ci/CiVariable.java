/*
 * Copyright (c) 2010 Sun Microsystems, Inc.  All rights reserved.
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
 * Represents a value that is yet to be bound to a machine location (such as
 * a {@linkplain CiRegister register} or stack {@linkplain CiAddress address})
 * by a register allocator.
 * 
 * @author Doug Simon
 */
public class CiVariable extends CiValue {
    
    /**
     * The identifier of the variable. This is a non-zero index in a contiguous 0-based name space. 
     */
    public final int index;

    /**
     * Creates a new variable.
     * @param kind
     * @param index
     */
    private CiVariable(CiKind kind, int index) {
        super(kind);
        this.index = index;
    }
    
    private static CiVariable[] generate(CiKind kind, int count) {
        CiVariable[] variables = new CiVariable[count];
        for (int i = 0; i < count; i++) {
            variables[i] = new CiVariable(kind, i);
        }
        return variables;
    }
    
    private static final int CACHE_PER_KIND_SIZE = 100;
    
    /**
     * Cache of common variables.
     */
    private static final CiVariable[][] cache = new CiVariable[CiKind.values().length][];
    static {
        for (CiKind kind : CiKind.values()) {
            cache[kind.ordinal()] = generate(kind, CACHE_PER_KIND_SIZE);
        }
    }
    
    /**
     * Gets a variable for a given kind and index.
     * 
     * @param kind
     * @param index
     * @return
     */
    public static CiVariable get(CiKind kind, int index) {
        assert index >= 0;
        CiVariable[] cachedVars = cache[kind.ordinal()];
        if (index < cachedVars.length) {
            return cachedVars[index];
        }
        return new CiVariable(kind, index);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof CiVariable) {
            CiVariable var = (CiVariable) obj;
            return kind == var.kind && index == var.index;
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return (index << 4) | kind.ordinal();
    }
    
    @Override
    public String name() {
        return "v" + index;
    }
}
