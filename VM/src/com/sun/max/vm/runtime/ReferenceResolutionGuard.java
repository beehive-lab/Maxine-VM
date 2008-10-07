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
/*VCSID=6e146bbc-2c0c-48df-ba11-5311e2aad91e*/
package com.sun.max.vm.runtime;

import com.sun.max.annotate.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public final class ReferenceResolutionGuard extends ResolutionGuard {

    public ReferenceResolutionGuard(ConstantPool constantPool, int constantPoolIndex) {
        super(constantPool, constantPoolIndex);
    }

    @CONSTANT_WHEN_NOT_ZERO
    private Object _value = null;

    @INLINE
    public void set(Object value) {
        _value = value;
    }

    /**
     * @return whether this guard has a definitive value or whether the code it guards shall be executed
     */
    @INLINE
    public boolean inlinedIsClear() {
        return _value == null;
    }

    @Override
    public boolean isClear() {
        return inlinedIsClear();
    }

    @Override
    public Kind kind() {
        return Kind.REFERENCE;
    }

    @INLINE
    public Object value() {
        return _value;
    }
}
