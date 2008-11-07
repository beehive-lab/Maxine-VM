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
package test.com.sun.max.util;

import junit.framework.*;

import com.sun.max.util.*;

/**
 * @author Bernd Mathiske
 */
public class Utf8Test extends TestCase {

    public Utf8Test(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Utf8Test.class);
    }

    private void convertStringToUtf8AndBack(String string) throws Utf8Exception {
        final byte[] utf8 = Utf8.stringToUtf8(string);
        final String result = Utf8.utf8ToString(false, utf8);
        assertEquals(result, string);
    }

    public void test_utf8() throws Utf8Exception {
        convertStringToUtf8AndBack("");
        convertStringToUtf8AndBack(" ");
        convertStringToUtf8AndBack("\n");
        convertStringToUtf8AndBack("abcABC!@#$%^&*()_=/.,;:?><|`~' xyzZXY");
        convertStringToUtf8AndBack("???????????????????????????????");
        convertStringToUtf8AndBack("????p??90=?a");
        for (char ch = Character.MIN_VALUE; ch < Character.MAX_VALUE; ch++) {
            convertStringToUtf8AndBack("abc" + ch + "mno" + ch + ch + "xyz");
        }
    }

}
