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

import com.sun.c1x.debug.LogStream;


/**
 * The <code>ConstantIntValue</code> class definition.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 *
 */
public class ConstantIntValue extends ScopeValue {

    private int value;

    public ConstantIntValue(int value) {
        this.value = value;
    }

    /**
     * @param stream
     */
    public ConstantIntValue(DebugInfoReadStream stream) {
        value = stream.readInt();
    }

    public int value() {
        return value;
    }

    @Override
    public boolean isConstantInt() {
        return true;
    }

    @Override
    public boolean equals(ScopeValue other) {
        return false;
    }

    /**
     * Writes this value in a debug stream.
     *
     * @param stream the debug info stream used for writing
     */
    @Override
    public void writeOn(DebugInfoWriteStream stream) {
        stream.writeInt(ScopeValueCode.ConstantIntCode.ordinal());
        stream.writeInt(value);
    }

    /**
     * Prints this scope value into a logstream.
     *
     * @param out the output logstream
     *
     */
    @Override
    public void printOn(LogStream out) {
        out.printf("%d", value());
    }

}
