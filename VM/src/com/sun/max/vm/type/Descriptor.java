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
/*VCSID=e6782187-bf3c-4247-a6cb-5141e1cae130*/
package com.sun.max.vm.type;

import com.sun.max.annotate.*;

/**
 * String descriptions of Java types and signatures, see #4.3.
 * 
 * @author Bernd Mathiske
 */
public abstract class Descriptor implements Comparable<Descriptor> {

    @INSPECTED
    private final String _string;

    public final String string() {
        return _string;
    }

    public static String dottified(String className) {
        return className.replace('/', '.');
    }

    public static String slashified(String className) {
        return className.replace('.', '/');
    }

    protected Descriptor(String value) {
        _string = slashified(value);
    }

    @Override
    public final String toString() {
        return _string;
    }

    @Override
    public final boolean equals(Object other) {
        return other == this;
    }

    @Override
    public final int hashCode() {
        return _string.hashCode();
    }

    public final int compareTo(Descriptor other) {
        return _string.compareTo(other._string);
    }

}
