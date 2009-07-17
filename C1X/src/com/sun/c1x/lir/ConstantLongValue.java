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
package com.sun.c1x.lir;

import com.sun.c1x.util.*;


/**
 * The <code>ConstantLongValue</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class ConstantLongValue extends ScopeValue {

    private long value;

    public ConstantLongValue(long value) {
        this.value = value;
    }

    /**
     * @param stream
     */
    public ConstantLongValue(DebugInfoReadStream stream) {
        value = stream.readLong();
    }

    public long value() {
        return value;
    }

    @Override
    public boolean isConstantLong() {
        return true;
    }

    @Override
    public boolean equals(ScopeValue other) {
        return false;
    }

    @Override
    public void writeOn(DebugInfoWriteStream stream) {
        stream.writeInt(ScopeValueCode.ConstantLongCode.ordinal());
        stream.writeLong(value());
    }

    @Override
    public void printOn(LogStream out) {
        out.printf("%d", value());
    }
}
