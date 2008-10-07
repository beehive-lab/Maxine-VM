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
package com.sun.max.collect;

import java.util.*;

/**
 * @author Bernd Mathiske
 */
public final class EnumSets {

    private EnumSets() {
    }

    public static <Element_Type extends Enum<Element_Type>> EnumSet<Element_Type> of(Class<Element_Type> elementType, Element_Type[] elements) {
        if (elements.length == 0) {
            return EnumSet.noneOf(elementType);
        }
        return EnumSet.of(elements[0], elements);
    }

    public static <Element_Type extends Enum<Element_Type>> EnumSet<Element_Type> union(EnumSet<Element_Type> set1, EnumSet<Element_Type> set2) {
        final EnumSet<Element_Type> result = EnumSet.copyOf(set1);
        result.addAll(set2);
        return result;
    }

    public static <Element_Type extends Enum<Element_Type>> EnumSet<Element_Type> add(EnumSet<Element_Type> set, Element_Type element) {
        final EnumSet<Element_Type> result = EnumSet.copyOf(set);
        result.add(element);
        return result;
    }

}
