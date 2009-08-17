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
package com.sun.max.ins.gui;

import com.sun.max.collect.*;


/**
 * Constants denoting the kinds of code that can be inspected for a method.
 *
 * @author Michael Van De Vanter
 */
public enum MethodCodeKind {
    TARGET_CODE("Target Code", true),
    BYTECODES("Bytecodes", false),
    JAVA_SOURCE("Java Source", false);

    private final String label;
    private final boolean defaultVisibility;

    private MethodCodeKind(String label, boolean defaultVisibility) {
        this.label = label;
        this.defaultVisibility = defaultVisibility;
    }

    /**
     * Determines if it the display of this source kind is implemented.
     *
     * TODO (mlvdv) This is a hack until source code viewing is implemented
     */
    public boolean isImplemented() {
        return this != JAVA_SOURCE;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    /**
     * Determines if this kind should be visible by default in new inspectors.
     */
    public boolean defaultVisibility() {
        return defaultVisibility;
    }

    public static final IndexedSequence<MethodCodeKind> VALUES = new ArraySequence<MethodCodeKind>(values());
}
