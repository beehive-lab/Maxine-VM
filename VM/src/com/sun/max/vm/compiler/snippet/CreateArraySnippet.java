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
            if (!classActor.isArrayClassActor()) {
                throw new VerifyError("MULTIANEWARRAY cannot be applied to non-array type " + classActor);
            }
            return createMultiReferenceArrayAtIndex(0, classActor, lengths);
        }
        public static final CreateArraySnippet.CreateMultiReferenceArray SNIPPET = new CreateMultiReferenceArray();
    }
}
