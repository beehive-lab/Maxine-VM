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
package com.sun.max.config;

import com.sun.max.annotate.*;
import com.sun.max.vm.*;

/**
 * Maxine packages that are subject to configuration with a {@link VMConfiguration} instance.
 *
 * @author Mick Jordan
 */

public abstract class BootImagePackage extends MaxPackage {

    /**
     * Determines if this package is part of the VM under the given configuration.
     * @param vmConfiguration
     * @return
     */
    public boolean isPartOfMaxineVM(VMConfiguration vmConfiguration) {
        return true;
    }

    /**
     * Determines if this package contains any classes annotated with {@link METHOD_SUBSTITUTIONS}.
     * @return
     */
    public boolean containsMethodSubstitutions() {
        return false;
    }

    protected BootImagePackage() {

    }

    /**
     * Use this when all sub-packages should considered included.
     * @param recursive
     */
    protected BootImagePackage(boolean recursive) {
        super(true);
    }

    /**
     * Constructor for redirected extension classes.
     * @param packageName
     */
    protected BootImagePackage(String... packageNames) {
        this(true, packageNames);
    }

    /**
     * Constructor for redirected extension classes with optional recursion nto sub-packages.
     * @param packageName
     */
    protected BootImagePackage(boolean recursive, String... packageNames) {
        super(packageNames, recursive);
    }

}
