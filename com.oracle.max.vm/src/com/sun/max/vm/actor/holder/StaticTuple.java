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
package com.sun.max.vm.actor.holder;

import com.sun.max.annotate.*;
import com.sun.max.vm.actor.member.*;

/**
 * Instances of this class represent static tuples during bootstrapping.
 * The boot image generator substitutes the former by the latter.
 * Thus the boot image does not contain any of the former.
 */
@HOSTED_ONLY
public final class StaticTuple {

    private final ClassActor classActor;

    public ClassActor classActor() {
        return classActor;
    }

    public StaticTuple(ClassActor classActor) {
        this.classActor = classActor;
    }

    public static Object fromJava(Class javaClass) {
        return ClassActor.fromJava(javaClass).staticTuple();
    }

    public FieldActor findStaticFieldActor(int offset) {
        return classActor.findStaticFieldActor(offset);
    }

    @Override
    public String toString() {
        return "staticTuple-" + classActor.simpleName();
    }
}
