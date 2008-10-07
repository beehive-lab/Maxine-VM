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

import com.sun.max.ide.*;
import com.sun.max.util.*;


/**
 * Tests for {@link JavaIdentifier}.
 *
 * @author Hiroshi Yamauchi
 */
public class JavaIdentifierTest extends MaxTestCase {

    public JavaIdentifierTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(JavaIdentifierTest.class);
    }

    public void test_isValid() {
        assertTrue(JavaIdentifier.isValid("a000"));
        assertTrue(!JavaIdentifier.isValid("000x"));
        assertTrue(JavaIdentifier.isValid("$a000"));
        assertTrue(JavaIdentifier.isValid("_000x"));
        assertTrue(JavaIdentifier.isValid("abc"));
        assertTrue(!JavaIdentifier.isValid("<init>"));
        assertTrue(JavaIdentifier.isValid("abc$bcc"));
        assertTrue(!JavaIdentifier.isValid("-000x"));
        assertTrue(JavaIdentifier.isValid("for"));
    }

    public void test_isValidQualified() {
        assertTrue(JavaIdentifier.isValidQualified("java.a000"));
        assertTrue(JavaIdentifier.isValidQualified("java.a000$bbb"));
        assertTrue(JavaIdentifier.isValidQualified("_.aa.__"));
        assertTrue(!JavaIdentifier.isValidQualified("java.1"));
        assertTrue(JavaIdentifier.isValidQualified("java.C$222"));
        assertTrue(!JavaIdentifier.isValidQualified("13"));
    }
}
