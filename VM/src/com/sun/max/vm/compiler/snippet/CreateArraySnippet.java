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

import java.lang.reflect.*;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.heap.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.type.*;

/**
 * Array creation snippets.
 *
 * @author Doug Simon
 */
public abstract class CreateArraySnippet extends Snippet {

    @INLINE
    static Object createArray(ClassActor arrayClassActor, int length) {
        if (length < 0) {
            Throw.negativeArraySizeException(length);
        }
        if (MaxineVM.isHosted()) {
            return Array.newInstance(arrayClassActor.componentClassActor().toJava(), length);
        }
        return Heap.createArray(arrayClassActor.dynamicHub(), length);
    }

    @INLINE
    static Object createNonNegativeSizeArray(ClassActor arrayClassActor, int length) {
        if (MaxineVM.isHosted()) {
            return Array.newInstance(arrayClassActor.componentClassActor().toJava(), length);
        }
        return Heap.createArray(arrayClassActor.dynamicHub(), length);
    }

    public static final class CreatePrimitiveArray extends Snippet {
        @NEVER_INLINE
        public static Object noninlineCreatePrimitiveArray(Kind kind, int length) {
            return createArray(kind.arrayClassActor(), length);
        }

        @SNIPPET
        public static Object createPrimitiveArray(Kind kind, int length) {
            return createArray(kind.arrayClassActor(), length);
        }
        public static final CreateArraySnippet.CreatePrimitiveArray SNIPPET = new CreatePrimitiveArray();
    }

    public static final class CreateReferenceArray extends Snippet {
        @NEVER_INLINE
        public static Object noninlineCreateReferenceArray(ArrayClassActor arrayClassActor, int length) {
            return createArray(arrayClassActor, length);
        }

        @SNIPPET
        public static Object createReferenceArray(ArrayClassActor arrayClassActor, int length) {
            return createArray(arrayClassActor, length);
        }
        public static final CreateArraySnippet.CreateReferenceArray SNIPPET = new CreateReferenceArray();
    }

    public static final class CreateMultiReferenceArray extends Snippet {
        private static Object createMultiReferenceArrayAtIndex(int index, ClassActor arrayClassActor, int[] lengths) {
            final int length = lengths[index];
            final Object result = createNonNegativeSizeArray(arrayClassActor, length);
            if (length > 0) {
                final int nextIndex = index + 1;
                if (nextIndex < lengths.length) {
                    final ClassActor subArrayClassActor = arrayClassActor.componentClassActor();
                    for (int i = 0; i < length; i++) {
                        final Object subArray = createMultiReferenceArrayAtIndex(nextIndex, subArrayClassActor, lengths);
                        if (MaxineVM.isHosted()) {
                            final Object[] array = (Object[]) result;
                            array[i] = subArray;
                        } else {
                            ArrayAccess.setObject(result, i, subArray);
                        }
                    }
                }
            }
            return result;
        }
        @SNIPPET
        public static Object createMultiReferenceArray(ClassActor classActor, int[] lengths) {
            if (!classActor.isArrayClass()) {
                throw new VerifyError("MULTIANEWARRAY cannot be applied to non-array type " + classActor);
            }
            return createMultiReferenceArrayAtIndex(0, classActor, lengths);
        }
        public static final CreateArraySnippet.CreateMultiReferenceArray SNIPPET = new CreateMultiReferenceArray();
    }
}
