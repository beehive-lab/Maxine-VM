/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.max.vm.compiler.snippet;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;

/**
 * Object instantiation snippet.
 *
 * @author Bernd Mathiske
 */
public final class CreateTupleOrHybrid extends Snippet {
    @SNIPPET
    public static Object createTupleOrHybrid(ClassActor classActor) {
        if (MaxineVM.isHosted()) {
            try {
                return Objects.allocateInstance(classActor.toJava());
            } catch (InstantiationException instantiationException) {
                throw ProgramError.unexpected(instantiationException);
            }
        }
        if (classActor.isHybridClass()) {
            return Heap.createHybrid(classActor.dynamicHub());
        }
        final Object object = Heap.createTuple(classActor.dynamicHub());
        if (classActor.hasFinalizer()) {
            SpecialReferenceManager.registerFinalizee(object);
        }
        return object;
    }
    public static final CreateTupleOrHybrid SNIPPET = new CreateTupleOrHybrid();
}
