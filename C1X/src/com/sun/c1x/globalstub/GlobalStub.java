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
package com.sun.c1x.globalstub;

import static com.sun.c1x.value.BasicType.*;

import com.sun.c1x.value.*;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public enum GlobalStub {


    SlowSubtypeCheck(Boolean, Object, Object),
    NewObjectArray(Object, Object, Int),
    NewInstance(Object, Object),
    NewTypeArray(Object, Object, Int),
    f2i(Int, Float),
    f2l(Long, Float),
    d2i(Int, Double),
    d2l(Long, Double);

    /**
     * Maximum number of arguments, determines how many stack words are reserved on the top of the stack.
     */
    public static final int MaxNumberOfArguments = 2;

    public final BasicType resultType;
    public final BasicType[] arguments;

    private GlobalStub() {
        resultType = Void;
        arguments = new BasicType[0];
    }

    private GlobalStub(BasicType resultType, BasicType... args) {
        this.resultType = resultType;
        this.arguments = args;
        assert args.length <= MaxNumberOfArguments;
    }
}
