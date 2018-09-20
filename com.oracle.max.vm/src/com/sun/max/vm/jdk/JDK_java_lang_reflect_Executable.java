/*
 * Copyright (c) 2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
 */
package com.sun.max.vm.jdk;

import com.sun.max.annotate.INLINE;
import com.sun.max.annotate.INTRINSIC;
import com.sun.max.annotate.METHOD_SUBSTITUTIONS;
import com.sun.max.annotate.SUBSTITUTE;
import com.sun.max.vm.actor.member.MethodActor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.UNSAFE_CAST;

/**
 * Method substitutions for {@link Executable}.
 */
@METHOD_SUBSTITUTIONS(Executable.class)
final class JDK_java_lang_reflect_Executable {

    private JDK_java_lang_reflect_Executable() {
    }

    /**
     * Casts this to {@link Executable}.
     */
    @INTRINSIC(UNSAFE_CAST)
    private native Executable thisExecutable();

    /**
     * Gets the method actor associated with this method.
     * @return the method actor for this method
     */
    @INLINE
    private MethodActor thisMethodActor() {
        if (thisExecutable() instanceof Method) {
            return MethodActor.fromJava((Method) thisExecutable());
        } else {
            assert thisExecutable() instanceof Constructor;
            return MethodActor.fromJavaConstructor((Constructor) thisExecutable());
        }
    }

    @SUBSTITUTE
    private Parameter[] getParameters0() {
        return null;
    }
}
