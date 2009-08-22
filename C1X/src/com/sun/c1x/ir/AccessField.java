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
package com.sun.c1x.ir;

import com.sun.c1x.*;
import com.sun.c1x.ci.*;
import com.sun.c1x.value.*;

/**
 * The <code>AccessField</code> class is the base class of all instructions that access
 * (read or write) fields.
 *
 * @author Ben L. Titzer
 */
public abstract class AccessField extends StateSplit {

    Instruction object;
    final int offset;
    final RiField field;

    /**
     * Constructs a new access field object.
     * @param object the instruction producing the receiver object
     * @param field the compiler interface representation of the field
     * @param isStatic indicates if the field is static
     * @param stateBefore the state before the field access
     * @param isLoaded indicates if the class is loaded
     */
    public AccessField(Instruction object, RiField field, boolean isStatic, ValueStack stateBefore, boolean isLoaded) {
        super(field.basicType().stackType(), stateBefore);
        this.object = object;
        this.offset = isLoaded ? field.offset() : -1;
        this.field = field;
        if (!isLoaded || C1XOptions.TestPatching && !field.isVolatile()) {
            // require patching if the field is not loaded (i.e. resolved),
            // or if patch testing is turned on (but not if the field is volatile)
            setFlag(Flag.NeedsPatching);
        }
        initFlag(Flag.IsLoaded, isLoaded);
        initFlag(Flag.IsStatic, isStatic);
        if (isLoaded && object != null && object.isNonNull()) {
            clearNullCheck();
            C1XMetrics.NullChecksRedundant++;
        }
    }

    /**
     * Gets the instruction that produces the receiver object of this field access
     * (for instance field accesses).
     * @return the instruction that produces the receiver object
     */
    public Instruction object() {
        return object;
    }

    /**
     * Gets the offset of the field from the start of the object, in bytes.
     * @return the offset of the field within the object
     */
    public int offset() {
        return offset;
    }

    /**
     * Gets the compiler interface field for this field access.
     * @return the compiler interface field for this field access
     */
    public RiField field() {
        return field;
    }

    /**
     * Checks whether this field access is an access to a static field.
     * @return <code>true</code> if this field access is to a static field
     */
    public boolean isStatic() {
        return checkFlag(Flag.IsStatic);
    }

    /**
     * Checks whether the class of the field of this access is loaded.
     * @return <code>true</code> if the class is loaded
     */
    public boolean isLoaded() {
        return checkFlag(Flag.IsLoaded);
    }

    @Override
    public void clearNullCheck() {
        if (isLoaded()) {
            stateBefore = null;
        }
        setFlag(Flag.NoNullCheck);
    }

    /**
     * Gets the instruction representing an explicit null check for this field access.
     * @return the object representing an explicit null check
     */
    public NullCheck explicitNullCheck() {
        return null;
    }

    /**
     * Checks whether this field access will require patching.
     * @return <code>true</code> if this field access will require patching
     */
    public boolean needsPatching() {
        return checkFlag(Flag.NeedsPatching);
    }

    /**
     * Checks whether this field access may cause a trap or an exception, which
     * is if it either requires a null check or needs patching.
     * @return <code>true</code> if this field access can cause a trap
     */
    @Override
    public boolean canTrap() {
        return needsPatching() || needsNullCheck();
    }

    /**
     * Iterates over the input values to this instruction. In this case,
     * it is only the receiver object of the field access.
     * @param closure the closure to apply to each value
     */
    @Override
    public void inputValuesDo(InstructionClosure closure) {
        if (object != null) {
            object = closure.apply(object);
        }
    }
}
