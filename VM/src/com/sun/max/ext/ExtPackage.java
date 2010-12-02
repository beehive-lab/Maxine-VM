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
package com.sun.max.ext;

import com.sun.max.vm.*;

/**
 * The superclass of a package that defines an <i>extension</i> to the Maxine VM.
 * It may reside anywhere in the universal package name space, but it must be
 * referenced initially from a sub-package of {@code com.sun.max.ext}.
 *
 * For extensions in which the author has complete control over package naming
 * and a compilation dependence on Maxine is acceptable, the recommended
 * approach is to place extensions directly in a sub-package of {@code com.sun.max.ext},
 * and use the default constructor. Then, provided that the package is on the boot image
 * build classpath, all classes in the package will be included in the image.
 *
 * For situations where the extension cannot depend on Maxine and/or uses package names
 * in the universal package space, e.g. {@code com.acme.*}, use the <i>redirect</i>
 * {@link #ExtPackage(String) constructor}.
 *
 * Extension packages are configurable with a {@link VMConfiguration} instance
 * but are not subclasses of {@link VMPackage} to keep the search namespace
 * separate.
 *
 * @author Mick Jordan
 */
public class ExtPackage extends VMConfigPackage {

    /**
     * Use this constructor when the extension classes are located in the same
     * package as this class.
     */
    protected ExtPackage() {
    }

    /**
     * Use this constructor when the extension classes are located in the same
     * package as this class and you want all classes in sub-packages to be included
     * regardless of whether they have a {code Package} class.
     */
    protected ExtPackage(boolean recursive) {
        super(true);
    }

    /**
     * Use this constructor to redirect the search for classes to the given package name
     * and its sub-packages. I.e. recursion is assumed.
     *
     * @param packageName package name containing classes to include with this extension
     */
    protected ExtPackage(String... packageNames) {
        this(true, packageNames);
    }

    /**
     * Use this constructor to redirect the search for classes to the given package name
     * with optional recursion into sub-packages.
     *
     * @param packageName package name containing classes to include with this extension
     */

    protected ExtPackage(boolean recursive, String... packageNames) {
        super(recursive, packageNames);
    }
}
