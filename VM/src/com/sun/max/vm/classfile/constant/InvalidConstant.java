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
/*VCSID=78ec995a-3b3c-4a1b-92cd-2929ec4a0a37*/
package com.sun.max.vm.classfile.constant;

import static com.sun.max.vm.classfile.ErrorContext.*;
import com.sun.max.vm.classfile.constant.ConstantPool.*;

/**
 * Place holder for invalid constant pool indexes such as 0 and the indexes immediately after a {@link Tag#LONG} or
 * {@link Tag#DOUBLE} entry.
 *
 * @author Doug Simon
 */
public final class InvalidConstant extends AbstractPoolConstant<InvalidConstant> {

    private InvalidConstant() {

    }

    @Override
    public PoolConstantKey<InvalidConstant> key(ConstantPool pool) {
        throw classFormatError("Invalid constant pool entry");
    }

    @Override
    public Tag tag() {
        return Tag.INVALID;
    }

    public String valueString(ConstantPool pool) {
        return "--INVALID--";
    }

    public static final InvalidConstant VALUE = new InvalidConstant();
}
