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
/*VCSID=a2bd8a35-5441-41cd-97b4-46c4d2511281*/
package com.sun.max.vm.compiler.dir;

import com.sun.max.vm.type.*;

/**
 * @author Bernd Mathiske
 */
public class DirVariable extends DirValue {

    private final Kind _kind;
    private final int _serial;

    public DirVariable(Kind kind, int serial) {
        super();
        _kind = kind;
        _serial = serial;
    }

    public Kind kind() {
        return _kind;
    }

    public boolean isConstant() {
        return false;
    }

    @Override
    public int hashCodeForBlock() {
        final int a = super.hashCodeForBlock();
        final int b = _kind.hashCode();
        return a ^ b;
    }

    @Override
    public String toString() {
        return Character.toString(_kind.character()) + _serial;
    }

}
