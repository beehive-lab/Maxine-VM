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

import com.sun.c1x.ci.CiField;
import com.sun.c1x.value.ValueStack;
import com.sun.c1x.value.ValueType;
import com.sun.c1x.C1XOptions;
import com.sun.c1x.util.InstructionClosure;

/**
 * The <code>AccessField</code> class is the base class of all instructions that access
 * (read or write) fields.
 *
 * @author Ben L. Titzer
 */
public abstract class AccessField extends Instruction {

    Instruction _object;
    final int _offset;
    final CiField _field;
    final ValueStack _stateBefore;
    ValueStack _lockStack;
    NullCheck _explicitNullCheck;

    /**
     * Constructs a new access field object.
     * @param object the instruction producing the receiver object
     * @param offset the offset of the field in bytes, if known
     * @param field the compiler interface representation of the field
     * @param isStatic indicates if the field is static
     * @param lockStack the lock stack
     * @param stateBefore the state before the field access
     * @param isLoaded indicates if the class is loaded
     * @param isInitialized indicates if the class is initialized
     */
    public AccessField(Instruction object, int offset, CiField field, boolean isStatic,
                       ValueStack lockStack, ValueStack stateBefore, boolean isLoaded, boolean isInitialized) {
        super(ValueType.fromBasicType(field.basicType()));
        _object = object;
        _offset = offset;
        _field = field;
        _lockStack = lockStack;
        _stateBefore = stateBefore;
        if (!isLoaded || (C1XOptions.TestPatching && !field.isVolatile())) {
            // require patching if the field is not loaded (i.e. resolved),
            // or if patch testing is turned on (but not if the field is volatile)
            setFlag(Flag.NeedsPatching);
        }
        setFlag(Flag.IsLoaded, isLoaded);
        setFlag(Flag.IsInitialized, isInitialized);
        setFlag(Flag.IsStatic, isStatic);
        pin(); // pin memory access instructions
    }

    /**
     * Gets the instruction that produces the receiver object of this field access
     * (for instance field accesses).
     * @return the instruction that produces the receiver object
     */
    public Instruction object() {
        return _object;
    }

    /**
     * Gets the offset of the field from the start of the object, in bytes.
     * @return the offset of the field within the object
     */
    public int offset() {
        return _offset;
    }

    /**
     * Gets the compiler interface field for this field access.
     * @return the compiler interface field for this field access
     */
    public CiField field() {
        return _field;
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

    /**
     * Checks whether the class of the field of this access is initialized.
     * @return <code>true</code> if the class is initialized
     */
    public boolean isInitialized() {
        return checkFlag(Flag.IsInitialized);
    }

    /**
     * Gets the value stack of the state before this field access.
     * @return the state before this field access
     */
    public ValueStack stateBefore() {
        return _stateBefore;
    }

    public ValueStack lockStack() {
        // XXX: what is a lock stack?
        return _lockStack;
    }

    public void setLockStack(ValueStack lockStack) {
        _lockStack = lockStack;
    }

    /**
     * Gets the instruction representing an explicit null check for this field access.
     * @return the object representing an explicit null check
     */
    public Object explicitNullCheck() {
        return _explicitNullCheck;
    }

    /**
     * Sets the instruction representing an explicit null check for this field access.
     * @param explicitNullCheck the instruction representing the explicit check
     */
    public void setExplicitNullCheck(NullCheck explicitNullCheck) {
        _explicitNullCheck = explicitNullCheck;
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
    public boolean canTrap() {
        return needsPatching() || (!checkFlag(Flag.IsStatic) && !_object.isNonNull());
    }

    /**
     * Iterates over the input values to this instruction. In this case,
     * it is only the receiver object of the field access.
     * @param closure the closure to apply to each value
     */
    public void inputValuesDo(InstructionClosure closure) {
        _object = closure.apply(_object);
    }

    /**
     * Iterates over the "other" values to this instruction. In this case,
     * it is any values in the state before the access or any values
     * in the lock stack.
     * @param closure the closure to apply to each value
     */
    public void otherValuesDo(InstructionClosure closure) {
        if (_stateBefore != null) {
            _stateBefore.valuesDo(closure);
        }
        if (_lockStack != null) {
            _lockStack.valuesDo(closure);
        }
    }
}