/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import com.sun.max.annotate.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.actor.holder.*;


/**
 * A placeholder class for representing a non-moving reference to an object.
 * Similar to a {@link JniHandle} but with no prescribed implementation.
 *
 * In Maxine, a class id is actually represented as an {@code int} and
 * managed by {@link ClassIDManager}. However, for use with {@link VMLogger}
 * and for consistency with the other {@code xxxID} classes, it is defined
 * here, even though it is not used in the JNI implementation.
 */
public class ClassID extends Word {
    @HOSTED_ONLY
    protected ClassID(long value) {
        super(value);
    }

    public static ClassID create(ClassActor classActor) {
        return fromWord(Address.fromInt(classActor.id));
    }

    @INTRINSIC(UNSAFE_CAST)
    public static ClassID fromWord(Word word) {
        return new ClassID(word.value);
    }

}
