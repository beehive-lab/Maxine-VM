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
package com.sun.max.unsafe.box;

import java.util.*;

import com.sun.max.lang.*;
import com.sun.max.unsafe.*;

/**
 * Common part of any boxed value.
 *
 * @author Bernd Mathiske
 */
public interface UnsafeBox {

    long nativeWord();

    public static final class Static {
        private Static() {
        }

        /**
         * Lazily initialized mapping from non-boxed word types to the corresponding boxed word types.
         * The initialization of this map need not be synchronized as the initialized map will be exactly
         * the same no matter how many times it is initialized.
         */
        private static Map<Class<? extends Word>, Class<? extends Word>> unboxedToBoxedTypes;

        public static <Word_Type extends Word> Class<? extends Word_Type> getBoxedType(Class<Word_Type> wordType) {
            if (UnsafeBox.class.isAssignableFrom(wordType)) {
                return wordType;
            }
            if (unboxedToBoxedTypes == null) {
                final Map<Class<? extends Word>, Class<? extends Word>> map = new HashMap<Class<? extends Word>, Class<? extends Word>>();
                for (Class wordClass : Word.getSubclasses()) {
                    if (!UnsafeBox.class.isAssignableFrom(wordClass)) {
                        final Class result = Classes.forName(new com.sun.max.unsafe.box.Package().name() + ".Boxed" + wordClass.getSimpleName());
                        final Class<Class<? extends Word>> type = null;
                        map.put(StaticLoophole.cast(type, wordClass), StaticLoophole.cast(type, result));
                        unboxedToBoxedTypes = map;
                    }
                }
            }
            final Class<Class<? extends Word_Type>> type = null;
            return StaticLoophole.cast(type, unboxedToBoxedTypes.get(wordType));
        }
    }
}
