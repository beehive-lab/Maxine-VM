/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.value;

/**
 * The <code>ValueTag</code> class contains constants for denoting the value type of a variable.
 *
 * @author Ben L. Titzer
 */
public class ValueTag {

    public static final byte INT_TAG = 0;
    public static final byte LONG_TAG = 1;
    public static final byte FLOAT_TAG = 2;
    public static final byte DOUBLE_TAG = 3;
    public static final byte OBJECT_TAG = 4;
    public static final byte JSR_TAG = 5;
    public static final byte VOID_TAG = 6;
    public static final byte ILLEGAL_TAG = 7;

    /**
     * Gets the character for the specified tag.
     * @param tag the value tag
     * @return a single character suitable to print
     */
    public static char tagChar(byte tag) {
        switch (tag) {
            case INT_TAG: return 'i';
            case LONG_TAG: return 'l';
            case FLOAT_TAG: return 'f';
            case DOUBLE_TAG: return 'd';
            case OBJECT_TAG: return 'a';
            case JSR_TAG: return 'r';
            case VOID_TAG: return 'v';
            case ILLEGAL_TAG: return '!';
        }
        return '?';
    }

    /**
     * Gets the name of the tag as a string.
     * @param tag the value tag
     * @return a string suitable to print
     */
    public static String tagName(byte tag) {
        switch (tag) {
            case INT_TAG: return "int";
            case LONG_TAG: return "long";
            case FLOAT_TAG: return "float";
            case DOUBLE_TAG: return "double";
            case OBJECT_TAG: return "object";
            case JSR_TAG: return "address";
            case VOID_TAG: return "void";
            case ILLEGAL_TAG: return "illegal";
        }
        return "unknown";
    }
}
