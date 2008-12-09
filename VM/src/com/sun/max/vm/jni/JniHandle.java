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

package com.sun.max.vm.jni;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;

/**
 * A word type for representing a non-moving reference to a Java object. This
 * handle is used to pass objects to/from native code across the JNI interface.
 *
 * Note that this class cannot be generic as JNI handles are passed from native
 * code to Java code in most {@linkplain JniFunctions JNI functions} and we do not
 * have any guarantee that these values are actually JNI handles, let alone whether
 * they are handles to an expected type of object. The explicit casting that is
 * therefore required actually makes the MaxineVM VM fail in a more understandable
 * fashion (i.e. with a ClassCastException) in the presence of bad native code.
 *
 * @see JniHandles
 *
 * @author Doug Simon
 * @author Bernd Mathiske
 */
public abstract class JniHandle extends Word {

    @INLINE
    public static JniHandle zero() {
        final Class<JniHandle> type = null;
        return UnsafeLoophole.castWord(type, Word.zero());
    }

    /**
     * Dereferences this handle to "unwrap" the Java object. This method will return {@code null}
     * if this handle {@link #isZero() is zero}.
     */
    @INLINE
    public final Object get() {
        return JniHandles.get(this);
    }

    @INLINE
    public final Address getAddress() {
        return JniHandles.getAddress(this);
    }
}
