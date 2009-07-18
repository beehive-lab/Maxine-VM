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

import com.sun.c1x.asm.RelocInfo.*;

/**
 *
 * @author Thomas Wuerthinger
 *
 */
public class Relocation {

    public static RelocationHolder specSimple(Type rtype) {
        // TODO Auto-generated method stub
        return null;
    }

    public Type type() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isCall() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isData() {
        // TODO Auto-generated method stub
        return false;
    }

    public static RelocationHolder specExternalWord(Pointer loc) {
        // TODO Auto-generated method stub
        return null;
    }

    public static RelocationHolder specInternalWord(Pointer loc) {
        // TODO Auto-generated method stub
        return null;
    }

    public static RelocationHolder specRuntimeCall() {
        // TODO Auto-generated method stub
        return null;
    }

    public static RelocationHolder specSimple(Pointer loc) {
        // TODO Auto-generated method stub
        return null;
    }

    public static RelocationHolder specOptVirtualCallRelocation(long address) {
        // TODO Auto-generated method stub
        return null;
    }

    public static RelocationHolder specStaticCallRelocation(long address) {
        // TODO Auto-generated method stub
        return null;
    }

}
