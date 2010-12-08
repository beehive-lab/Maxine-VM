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
package com.sun.max.program.option;

import static com.sun.max.lang.Classes.*;


/**
 * The {@code PackageOptionType} class.
 * Created Nov 20, 2007
 *
 * @author Ben L. Titzer
 */
public class PackageOptionType extends Option.Type<String> {
    public final String superPackage;

    public PackageOptionType(String superPackage) {
        super(String.class, "vm-package");
        this.superPackage = superPackage;
    }

    private Class packageClass(String pkgName) {
        try {
            return Class.forName(pkgName + ".Package");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Override
    public String parseValue(String string) {
        final String fullName = superPackage + "." + string;
        if (string != null && string.length() > 0) {
            Class pkgClass = packageClass(fullName);
            if (pkgClass == null) {
                pkgClass = packageClass(string);
            }
            if (pkgClass == null) {
                throw new Option.Error("Package not found: " + string + " (or " + fullName + ")");
            }
            return getPackageName(pkgClass);
        }
        return null;
    }

    @Override
    public String getValueFormat() {
        return "<package-name>";
    }
}
