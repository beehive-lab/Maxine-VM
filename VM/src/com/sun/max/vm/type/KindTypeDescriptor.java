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
/*VCSID=2360ef7f-5360-440d-b410-d011717ab658*/
package com.sun.max.vm.type;

import java.util.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.unsafe.*;


/**
 * The {@code KindTypeDescriptor} class encapsulates utilities and operations related to
 * the type descriptors and {@linkplain Kind kinds}.
 *
 * @author Ben L. Titzer
 */
public final class KindTypeDescriptor {

    static final Set<TypeDescriptor> _wordTypeDescriptors = Sets.from(com.sun.max.lang.Arrays.map(Word.getSubclasses(), TypeDescriptor.class,
        new MapFunction<Class, TypeDescriptor>() {
            public TypeDescriptor map(Class javaClass) {
                return JavaTypeDescriptor.forJavaClass(javaClass);
            }
        }
    ));

    private KindTypeDescriptor() {
        // do nothing.
    }

    public static Kind toKind(String string) {
        // Table #4.2 + Word:
        switch (string.charAt(0)) {
            case 'B':
                return Kind.BYTE;
            case 'C':
                return Kind.CHAR;
            case 'D':
                return Kind.DOUBLE;
            case 'F':
                return Kind.FLOAT;
            case 'I':
                return Kind.INT;
            case 'J':
                return Kind.LONG;
            case 'L':
                return refersToWordType(string) ? Kind.WORD : Kind.REFERENCE;
            case 'S':
                return Kind.SHORT;
            case 'V':
                return Kind.VOID;
            case 'Z':
                return Kind.BOOLEAN;
            case '[':
                return Kind.REFERENCE;
            default:
                throw ProgramError.unexpected("unknown kind for type descriptor: " + "\"" + string + "\"");
        }
    }

    private static boolean refersToWordType(String string) {
        return isWord(TypeDescriptor.makeTypeDescriptor(string));
    }

    public static boolean isWord(final TypeDescriptor descriptor) {
        return _wordTypeDescriptors.contains(descriptor);
    }

}
