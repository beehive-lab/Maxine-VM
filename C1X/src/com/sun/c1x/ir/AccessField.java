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

import com.sun.c1x.C1XOptions;
import com.sun.c1x.ci.CiField;
import com.sun.c1x.value.ValueStack;
import com.sun.c1x.value.ValueType;

/**
 * The <code>AccessField</code> class is the base class of all instructions that access
 * (read or write) fields.
 *
 * @author Ben L. Titzer
 */
public abstract class AccessField extends Instruction {

    Instruction object;
    final int offset;
    final CiField field;
    ValueStack stateBefore;
    ValueStack lockStack;
    NullCheck explicitNullCheck;
    boolean isStatic;

    /**
     * Constructs a new access field object.
     * @param object the instruction producing the receiver object
     * @param field the compiler interface representation of the field
     * @param isStatic indicates if the field is static
     * @param lockStack the lock stack
     * @param stateBefore the state before the field access
     * @param isLoaded indicates if the class is loaded
     */
    public AccessField(Instruction object, CiField field, boolean isStatic,
                       ValueStack lockStack, ValueStack stateBefore, boolean isLoaded) {
        super(ValueType.fromBasicType(field.basicType()));
        this.object = object;
        this.offset = isLoaded ? field.offset() : -1;
        this.field = field;
        this.lockStack = lockStack;
        this.stateBefore = stateBefore;
        this.isStatic = isStatic;
        if (!isLoaded || (C1XOptions.TestPatching && !field.isVolatile())) {
            // require patching if the field is not loaded (i.e. resolved),
            // or if patch testing is turned on (but not if the field is volatile)
            setFlag(Flag.NeedsPatching);
        }
        initFlag(Flag.IsLoaded, isLoaded);
        initFlag(Flag.NeedsNullCheck, !object.isNonNull());
        pin(); // pin memory access instructions
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
    public CiField field() {
        return field;
    }

    /**
     * Checks whether this field access is an access to a static field.
     * @return <code>true</code> if this field access is to a static field
     */
    public boolean isStatic() {
        return isStatic;
    }

    /**
     * Checks whether the class of the field of this access is loaded.
     * @return <code>true</code> if the class is loaded
     */
    public boolean isLoaded() {
        return checkFlag(Flag.IsLoaded);
    }

    /**
     * Checks whether the class of the field of this access is initialized.
     * @return <code>true</code> if the class is initialized
     */
    public boolean isInitialized() {
        return !isStatic || isLoaded() && field.holder().isInitialized();
    }

    /**
     * Gets the value stack of the state before this field access.
     * @return the state before this field access
     */
    public ValueStack stateBefore() {
        return stateBefore;
    }

    @Override
    public ValueStack lockStack() {
        // XXX: what is a lock stack?
        return lockStack;
    }

    public void setLockStack(ValueStack lockStack) {
        this.lockStack = lockStack;
    }

    /**
     * Gets the instruction representing an explicit null check for this field access.
     * @return the object representing an explicit null check
     */
    public NullCheck explicitNullCheck() {
        return explicitNullCheck;
    }

    /**
     * Sets the instruction representing an explicit null check for this field access.
     * @param explicitNullCheck the instruction representing the explicit check
     */
    public void setExplicitNullCheck(NullCheck explicitNullCheck) {
        this.explicitNullCheck = explicitNullCheck;
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
        object = closure.apply(object);
    }

    /**
     * Iterates over the "other" values to this instruction. In this case,
     * it is any values in the state before the access or any values
     * in the lock stack.
     * @param closure the closure to apply to each value
     */
    @Override
    public void otherValuesDo(InstructionClosure closure) {
        if (stateBefore != null) {
            stateBefore.valuesDo(closure);
        }
        if (lockStack != null) {
            lockStack.valuesDo(closure);
        }
    }
}
