/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
 * therefore required makes the VM fail in a more understandable
 * fashion (i.e. with a ClassCastException) in the presence of bad native code.
 *
 * @see JniHandles
 */
public abstract class JniHandle extends Word {

    @INLINE
    public static JniHandle zero() {
        return Word.zero().asJniHandle();
    }

    @HOSTED_ONLY
    protected JniHandle() {
    }

    /**
     * Dereferences this handle to "unhand" the Java object. This method will return {@code null}
     * if this handle {@link #isZero() is zero}.
     */
    @INLINE
    public final Object unhand() {
        return JniHandles.get(this);
    }

    @INLINE
    public final Address getAddress() {
        return JniHandles.getAddress(this);
    }
}
