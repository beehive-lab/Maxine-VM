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
/*VCSID=d0cc276b-199c-4283-b307-837c13f75a44*/
package com.sun.max.asm.gen;

import com.sun.max.asm.*;

/**
 * @author Bernd Mathiske
 */
public class ArgumentRange {

    private final WrappableSpecification _specification;
    private final long _minValue;
    private final long _maxValue;

    public ArgumentRange(WrappableSpecification specification, long minValue, long maxValue) {
        _specification = specification;
        _minValue = minValue;
        _maxValue = maxValue;
    }

    public WrappableSpecification wrappedSpecification() {
        return _specification;
    }

    public long minValue() {
        return _minValue;
    }

    public long maxValue() {
        return _maxValue;
    }

    public boolean includes(Argument argument) {
        return _minValue <= argument.asLong() && argument.asLong() <= _maxValue;
    }

    private boolean _appliesInternally = true;

    public boolean appliesInternally() {
        return _appliesInternally;
    }

    public void doNotApplyInternally() {
        _appliesInternally = false;
    }

    public static final ArgumentRange UNSPECIFIED = null;

}
