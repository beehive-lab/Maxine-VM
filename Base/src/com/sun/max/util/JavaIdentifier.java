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
/*VCSID=6c26c74e-f44a-4a10-b201-ad3a6cd6f3b2*/
package com.sun.max.util;



/**
 * @author Bernd Mathiske
 */
public final class JavaIdentifier {

    // Utility classes should not be instantiated.
    private JavaIdentifier() {
    }

    public static boolean isValid(String string) {
        if (!Character.isJavaIdentifierStart(string.charAt(0))) {
            return false;
        }
        for (int i = 1; i < string.length(); i++) {
            if (!Character.isJavaIdentifierPart(string.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidQualified(String string) {
        for (String identifier : string.split("\\.")) {
            if (!isValid(identifier)) {
                return false;
            }
        }
        return true;
    }

    public static String unqualifiedSuffix(String qualifiedIdentifier) {
        final int dotIndex = qualifiedIdentifier.lastIndexOf('.');
        if (dotIndex > 0) {
            return qualifiedIdentifier.substring(dotIndex + 1);
        }
        return qualifiedIdentifier;
    }

    public static String linkQualifiedIdentifier(char[]... identifiers) {
        String qualifiedIdentifier = "";
        if (identifiers != null) {
            String separator = "";
            for (int i = 0; i < identifiers.length; i++) {
                qualifiedIdentifier += separator;
                qualifiedIdentifier += new String(identifiers[i]);
                separator = ".";
            }
        }
        return qualifiedIdentifier;
    }

}
