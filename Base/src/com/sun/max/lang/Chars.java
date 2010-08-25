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
package com.sun.max.lang;

import com.sun.max.util.*;

/**
 * @author Bernd Mathiske
 */
public final class Chars {

    private Chars() {
    }

    public static final int SIZE = 2;

    public static final Range VALUE_RANGE = new Range(Character.MIN_VALUE, Character.MAX_VALUE);

    public static boolean isHexDigit(char c) {
        switch (c) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
                return true;
        }
        return false;
    }

    public static boolean isOctalDigit(char c) {
        if (c < '0') {
            return false;
        }
        return c <= '7';
    }

    public static String toJavaLiteral(char c) {
        if (c == '\n') {
            return "'\\n'";
        }
        if (c == '\t') {
            return "'\\t'";
        }
        if (c == '\r') {
            return "'\\r'";
        }
        if (c < ' ' || c > 127) {
            return "'\\" + Integer.toOctalString(c) + "'";
        }
        return "'" + c + "'";
    }
}
