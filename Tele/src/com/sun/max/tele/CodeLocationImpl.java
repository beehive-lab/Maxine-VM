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
package com.sun.max.tele;

import com.sun.max.jdwp.vm.data.*;
import com.sun.max.jdwp.vm.proxy.*;

/**
 * Class representing a location that can either point to a machine code instruction or a Java bytecode.
 *
 * @author Thomas Wuerthinger
 */
class CodeLocationImpl implements CodeLocation{

    private MethodProvider _method;
    private long _position;
    private boolean _isMachineCode;

    CodeLocationImpl(MethodProvider method) {
        this(method, 0);
    }

    CodeLocationImpl(MethodProvider method, int position) {
        this(method, position, false);
    }

    CodeLocationImpl(MethodProvider method, long position, boolean isMachineCode) {
        _method = method;
        _position = position;
        _isMachineCode = isMachineCode;

        long max = Integer.MIN_VALUE;
        long min = Integer.MAX_VALUE;
        for (LineTableEntry entry : method.getLineTable()) {
            max = Math.max(entry.getCodeIndex(), max);
            min = Math.min(entry.getCodeIndex(), min);
        }

        if (max == Integer.MIN_VALUE) {
            assert position == -1;
        } else {
            assert position >= min && position <= max;
        }
    }

    public MethodProvider method() {
        return _method;
    }

    public long position() {
        return _position;
    }

    public boolean isMachineCode() {
        return _isMachineCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CodeLocationImpl)) {
            return false;
        }
        final CodeLocationImpl cl = (CodeLocationImpl) obj;
        return cl.method().equals(method()) && cl._position == _position && cl._isMachineCode == _isMachineCode;
    }

    @Override
    public int hashCode() {
        return (int) (_position << 8) + _method.hashCode() + (_isMachineCode ? 1 : 0);
    }

    @Override
    public String toString() {
        return "Location[" + _method.toString() + ", " + _position + ", " + _isMachineCode + "]";
    }
}
