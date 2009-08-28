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

import com.sun.c1x.ri.*;
import com.sun.c1x.value.*;

/**
 * The <code>LoadField</code> instruction represents a read of a static or instance field.
 *
 * @author Ben L. Titzer
 */
public class LoadField extends AccessField {

    /**
     * Creates a new LoadField instance.
     * @param object the receiver object
     * @param field the compiler interface field
     * @param isStatic indicates if the field is static
     * @param stateBefore the state before the field access
     * @param isLoaded indicates if the class is loaded
     */
    public LoadField(Value object, RiField field, boolean isStatic, ValueStack stateBefore, boolean isLoaded, char cpi, RiConstantPool constantPool) {
        super(object, field, isStatic, stateBefore, isLoaded, cpi, constantPool);
    }

    /**
     * Gets the declared type of the field being accessed.
     * @return the declared type of the field being accessed.
     */
    @Override
    public RiType declaredType() {
        return field().type();
    }

    /**
     * Gets the exact type of the field being accessed. If the field type is
     * a primitive array or an instance class and the class is loaded and final,
     * then the exact type is the same as the declared type. Otherwise it is <code>null</code>
     * @return the exact type of the field if known; <code>null</code> otherwise
     */
    @Override
    public RiType exactType() {
        return declaredType().exactType();
    }

    /**
     * Implements this instruction's half of the visitor pattern.
     * @param v the visitor to accept
     */
    @Override
    public void accept(ValueVisitor v) {
        v.visitLoadField(this);
    }
}
