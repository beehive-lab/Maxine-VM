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
/*VCSID=141d4f68-0a7b-4bc6-9ea2-a679a4da54f3*/
package com.sun.max.unsafe.box;

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

        public static <Word_Type extends Word> Class<? extends Word_Type> getBoxedType(Class<Word_Type> wordType) {
            if (UnsafeBox.class.isAssignableFrom(wordType)) {
                return wordType;
            }
            final Class result = Classes.forName(new com.sun.max.unsafe.box.Package().name() + ".Boxed" + wordType.getSimpleName());
            final Class<Class<? extends Word_Type>> type = null;
            return StaticLoophole.cast(type, result);
        }
    }
}
