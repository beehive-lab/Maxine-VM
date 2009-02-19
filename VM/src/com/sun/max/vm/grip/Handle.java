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
package com.sun.max.vm.grip;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;

/**
 * A pointer to a pointer to an object.
 *
 * @author Bernd Mathiske
 */
public abstract class Handle extends Word {

    @UNSAFE_CAST
    public static native Handle from(Word word);

    @Override
    public String toString() {
        return "^H" + toHexString();
    }

    @INLINE
    public final Pointer getPointer() {
        return asPointer().readWord(0).asPointer();
    }

    @INLINE
    public final void setPointer(Pointer origin) {
        asPointer().writeWord(0, origin);
    }

    @UNSAFE_CAST
    public static native Handle fromGrip(Grip grip);

    @UNSAFE_CAST
    public final Grip toGrip() {
        return UnsafeLoophole.wordToGrip(this);
    }

}
