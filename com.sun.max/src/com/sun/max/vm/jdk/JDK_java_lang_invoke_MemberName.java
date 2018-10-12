/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
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

import static com.sun.max.vm.intrinsics.MaxineIntrinsicIDs.*;

import java.lang.invoke.*;

import com.sun.max.annotate.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;

@METHOD_SUBSTITUTIONS(className = "java.lang.invoke.MemberName")
public final class JDK_java_lang_invoke_MemberName {

    private JDK_java_lang_invoke_MemberName() {
    }

    /**
     * Aliased members of MemberName.
     *
     */
    public static final class MemberNameAlias {

        @ALIAS(declaringClassName = "java.lang.invoke.MemberName", name = "<init>")
        native void init();

        @ALIAS(declaringClassName = "java.lang.invoke.MemberName", name = "checkForTypeAlias")
        native void checkForTypeAlias(Class< ? > refc);

        @ALIAS(declaringClassName = "java.lang.invoke.MemberName", name = "initResolved")
        native void initResolved(boolean b);

        @ALIAS(declaringClassName = "java.lang.invoke.MemberName", name = "referenceKindIsConsistent")
        native boolean referenceKindIsConsistent();

        @ALIAS(declaringClassName = "java.lang.invoke.MemberName")
        public Class< ? > clazz;

        @ALIAS(declaringClassName = "java.lang.invoke.MemberName")
        String name;

        @ALIAS(declaringClassName = "java.lang.invoke.MemberName")
        public int flags;

        @ALIAS(declaringClassName = "java.lang.invoke.MemberName")
        native MethodType getMethodType();

        @ALIAS(declaringClassName = "java.lang.invoke.MemberName")
        Object type;

        @ALIAS(declaringClassName = "java.lang.invoke.MemberName")
        Object resolution;
    }

    static final Class< ? > MemberName_Class;

    static {
        try {
            MemberName_Class = Class.forName("java.lang.invoke.MemberName");
        } catch (ClassNotFoundException x) {
            throw ProgramError.unexpected("Failed to find MemberName.class", x);
        }
    }

    @SUBSTITUTE()
    private boolean vminfoIsConsistent() {
        // TODO implement the check.
        Trace.line(1, "*** MemberName.vmInfoIsConsistent called ***");
        Trace.line(1, "memberName id=" + System.identityHashCode(this));
        return true;
    }

    @INTRINSIC(UNSAFE_CAST)
    public static native MemberNameAlias asMemberName(Object o);

    /**
     * Allocate a new MemberName object.
     *
     * @return
     */
    static Object newMemberName() {
        final Object o = Heap.createTuple(ClassActor.fromJava(MemberName_Class).dynamicHub());
        asMemberName(o).init();
        return o;
    }

}
