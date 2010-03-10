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
package com.sun.max.asm.gen;

import java.lang.reflect.*;

import com.sun.max.collect.*;

/**
 * @author Bernd Mathiske
 */
public interface ImmediateParameter extends Parameter {

    public static final class Static {

        private Static() {
        }

        public static <Element_Type extends ImmediateArgument, Argument_Type> Sequence<Element_Type> createSequence(Class<Element_Type> elementType,
                        final Class<Argument_Type> argumentType, Argument_Type... values) throws NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
            final AppendableSequence<Element_Type> result = new ArrayListSequence<Element_Type>();
            final Constructor<Element_Type> elementConstructor = elementType.getConstructor(argumentType);
            for (Argument_Type value : values) {
                result.append(elementConstructor.newInstance(value));
            }
            return result;
        }
    }

}
