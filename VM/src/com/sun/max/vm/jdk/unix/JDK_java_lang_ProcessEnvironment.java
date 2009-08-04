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
package com.sun.max.vm.jdk.unix;

import java.util.*;

import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;

/**
 * This class implements substitutions for the process environment. On Unix systems,
 * it reads the environment block from memory and creates an array of byte arrays
 * that is handled internally by {@link java.lang.ProcessEnvironment the Solaris process environment}.
 *
 * @author Ben L. Titzer
 */
@METHOD_SUBSTITUTIONS(hiddenClass = "java.lang.ProcessEnvironment")
public class JDK_java_lang_ProcessEnvironment {

    /**
     * Maximum length of a name/value pair, in bytes.
     */
    private static final int MAX_VAR_LENGTH = Ints.K;

    /**
     * Maximum size of the entire process environment block.
     */
    private static final int MAX_ENVIRON_SIZE = 8 * Ints.K;

    /**
     * Parses the native process environment block, which is a list of C-style strings
     * that denote name=value pairs, and returns an array of byte arrays. The name and
     * value of each item are appended sequentially in pairs so that the first array
     * represents the name, and the second the value.
     * @return an array of byte arrays denoting the name and value pairs loaded from
     * the native process environment
     */
    @SUBSTITUTE
    public static byte[][] environ() {
        final Pointer environ = MaxineVM.native_environment();

        final List<byte[]> list = new ArrayList<byte[]>();

        for (int offset = 0; offset < MAX_ENVIRON_SIZE; offset += Word.size()) {
            final Pointer nameValuePair = environ.readWord(offset).asPointer();
            if (nameValuePair.isZero()) {
                break;
            }
            readNameValuePair(nameValuePair, list);
        }

        return list.toArray(new byte[list.size()][]);
    }

    /**
     * Parses a C-style string that should be of the form "name=value", null-terminated.
     * The name and the value strings are appended to the list independently, with the
     * name first and the value second. Nothing is added to the list if there is an error
     * parsing the string.
     * @param namePointer the pointer to the beginning of the name
     * @param list the list to append the name and value strings to; nothing should be
     * added if there is an error.
     */
    private static void readNameValuePair(Pointer namePointer, List<byte[]> list) {
        int length;
        for (length = 0; length < MAX_ENVIRON_SIZE; length++) {
            final byte value = namePointer.getByte(length);
            if (value == 0) {
                return;
            } else if (value == (byte) '=') {
                break;
            }
        }
        if (length == 0) {
            return;
        }
        list.add(CString.toByteArray(namePointer, length));
        final Pointer valuePointer = namePointer.plus(length + 1);
        for (length = 0; length < MAX_VAR_LENGTH; length++) {
            final byte value = valuePointer.getByte(length);
            if (value == 0) {
                break;
            }
        }
        list.add(CString.toByteArray(valuePointer, length));
    }

}
