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
package com.sun.c1x.asm;

import com.sun.c1x.asm.RelocInfo.Type;
import com.sun.c1x.util.Util;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public class Relocation {

    public static final Relocation none = new Relocation(Type.none);
    private final Type type;

    public Relocation(Type type) {
        this.type = type;
    }

    public Relocation(int offset, Object obj) {
        this.type = Type.objectType;
    }

    public Type type() {
        return type;
    }

    public boolean isCall() {
        return Util.nonFatalUnimplemented(false);
    }

    public boolean isData() {
        return Util.nonFatalUnimplemented(false);
    }

    public static Relocation specExternalWord(long address) {
        return Util.nonFatalUnimplemented(null);
    }

    public static Relocation specInternalWord(long address) {
        return new Relocation(Type.internalWordType);
    }

    public static Relocation specRuntimeCall() {
        return specSimple(RelocInfo.Type.runtimeCallType);
    }

    public static Relocation specOptVirtualCallRelocation(long address) {
        return Util.nonFatalUnimplemented(null);
    }

    public static Relocation specStaticCallRelocation(long address) {
        return Util.nonFatalUnimplemented(null);
    }

    // make a generic relocation for a given type (if possible)
    public static Relocation specSimple(Type rtype) {
        if (rtype == RelocInfo.Type.none) {
            return Relocation.none;
        }
        return new Relocation(rtype);
    }

    public static Relocation specForImmediate() {
        return Util.nonFatalUnimplemented(null);
    }

    public int format() {
        return Util.nonFatalUnimplemented(0);
    }



}
