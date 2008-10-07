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
/*VCSID=a3c6a619-d2bc-4ce0-ad88-571ef9130e68*/
package test.com.sun.max.lang;

import com.sun.max.ide.*;
import com.sun.max.lang.*;

public class StringsTest extends MaxTestCase {

    public StringsTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(StringsTest.class);
    }

    public void test_firstCharToLowerCase()  {
        assertEquals(Strings.firstCharToLowerCase(""), "");
        assertEquals(Strings.firstCharToLowerCase("a"), "a");
        assertEquals(Strings.firstCharToLowerCase("A"), "a");
        assertEquals(Strings.firstCharToLowerCase("aa"), "aa");
        assertEquals(Strings.firstCharToLowerCase("Aa"), "aa");
        assertEquals(Strings.firstCharToLowerCase("1"), "1");
        assertEquals(Strings.firstCharToLowerCase("#"), "#");
        assertEquals(Strings.firstCharToLowerCase(" A"), " A");
    }

    public void test_firstCharToUpperCase() {
        assertEquals(Strings.firstCharToUpperCase(""), "");
        assertEquals(Strings.firstCharToUpperCase("A"), "A");
        assertEquals(Strings.firstCharToUpperCase("a"), "A");
        assertEquals(Strings.firstCharToUpperCase("AA"), "AA");
        assertEquals(Strings.firstCharToUpperCase("aa"), "Aa");
        assertEquals(Strings.firstCharToUpperCase("1"), "1");
        assertEquals(Strings.firstCharToUpperCase("#"), "#");
        assertEquals(Strings.firstCharToUpperCase(" a"), " a");
    }

    public void test_times() {
        assertEquals(Strings.times('a', -1), "");
        assertEquals(Strings.times('a', 0), "");
        assertEquals(Strings.times('a', 1), "a");
        assertEquals(Strings.times('a', 2), "aa");
    }

    public void test_spaces() {
        assertEquals(Strings.spaces(-1), "");
        assertEquals(Strings.spaces(0), "");
        assertEquals(Strings.spaces(1), " ");
        assertEquals(Strings.spaces(2), "  ");
    }

    public void test_padLengthWithSpaces() {
        assertEquals(Strings.padLengthWithSpaces("", -1), "");
        assertEquals(Strings.padLengthWithSpaces("", 0), "");
        assertEquals(Strings.padLengthWithSpaces("", 1), " ");
        assertEquals(Strings.padLengthWithSpaces("a", -1), "a");
        assertEquals(Strings.padLengthWithSpaces("a", 0), "a");
        assertEquals(Strings.padLengthWithSpaces("a", 1), "a");
        assertEquals(Strings.padLengthWithSpaces("a", 2), "a ");
        assertEquals(Strings.padLengthWithSpaces(" ", -1), " ");
        assertEquals(Strings.padLengthWithSpaces(" ", 0), " ");
        assertEquals(Strings.padLengthWithSpaces(" ", 1), " ");
        assertEquals(Strings.padLengthWithSpaces(" ", 2), "  ");

        assertEquals(Strings.padLengthWithSpaces(-1, ""), "");
        assertEquals(Strings.padLengthWithSpaces(0, ""), "");
        assertEquals(Strings.padLengthWithSpaces(1, ""), " ");
        assertEquals(Strings.padLengthWithSpaces(-1, "a"), "a");
        assertEquals(Strings.padLengthWithSpaces(0, "a"), "a");
        assertEquals(Strings.padLengthWithSpaces(1, "a"), "a");
        assertEquals(Strings.padLengthWithSpaces(2, "a"), " a");
        assertEquals(Strings.padLengthWithSpaces(-1, " "), " ");
        assertEquals(Strings.padLengthWithSpaces(0, " "), " ");
        assertEquals(Strings.padLengthWithSpaces(1, " "), " ");
        assertEquals(Strings.padLengthWithSpaces(2, " "), "  ");

    }

    public void test_indexOfNonEscapedChar() {
        assertTrue(Strings.indexOfNonEscapedChar('"', " \" \" ", 0) == 1);
        assertTrue(Strings.indexOfNonEscapedChar('"', " \" \" ", 1) == 1);
        assertTrue(Strings.indexOfNonEscapedChar('"', " \" \" ", 2) == 3);
        assertTrue(Strings.indexOfNonEscapedChar('"', " \" \" ", 3) == 3);
        assertTrue(Strings.indexOfNonEscapedChar('"', " \" \" ", 4) == -1);
        assertTrue(Strings.indexOfNonEscapedChar('"', " \" \" ", -1) == 1);
        assertTrue(Strings.indexOfNonEscapedChar('"', " \" \" ", 10) == -1);
    }

    private void assertSplitCommandEquals(String command, String... parts) {
        final String[] parsedParts = Strings.splitCommand(command);
        final boolean result = Arrays.equals(parsedParts, parts);
        assertTrue(Arrays.toString(parsedParts, " "), result);
    }

    private void assertSplitCommandThrowsException(String command) {
        try {
            Strings.splitCommand(command);
            fail("command parsing should have thrown IllegalArgumentException: " + command);
        } catch (IllegalArgumentException illegalArgumentException) {
        }
    }

    public void test_splitCommand() {
        assertSplitCommandEquals("");
        assertSplitCommandEquals("cmd", "cmd");
        assertSplitCommandEquals("\"cmd\"", "cmd");
        assertSplitCommandEquals("cmd \"arg1_with_quote\\\"\" arg2 arg3", "cmd", "arg1_with_quote\\\"", "arg2", "arg3");
        assertSplitCommandEquals("cmd arg1 arg2 arg3", "cmd", "arg1", "arg2", "arg3");
        assertSplitCommandEquals(" cmd arg1 arg2  arg3", "cmd", "arg1", "arg2", "arg3");
        assertSplitCommandEquals("cmd arg1 arg2  arg3 ", "cmd", "arg1", "arg2", "arg3");
        assertSplitCommandEquals("cmd \"arg1 with space\" arg2 arg3", "cmd", "arg1 with space", "arg2", "arg3");
        assertSplitCommandEquals("cmd \"arg1 with space\"  arg2 arg3", "cmd", "arg1 with space", "arg2", "arg3");
        assertSplitCommandEquals("cmd  \"arg1 with space\" arg2 arg3", "cmd", "arg1 with space", "arg2", "arg3");
        assertSplitCommandEquals("cmd \"arg1 with space\"suffix arg3", "cmd", "arg1 with spacesuffix", "arg3");
        assertSplitCommandEquals("cmd prefix\"arg1 with space\" arg3", "cmd", "prefixarg1 with space", "arg3");

        assertSplitCommandThrowsException("cmd arg1 \\");
        assertSplitCommandThrowsException("cmd \"arg1 ");
    }

    public void test_truncate() {
        assertEquals(Strings.truncate("", 0), "");
        assertEquals(Strings.truncate("", 1), "");
        assertEquals(Strings.truncate("a", 0), "...");
        assertEquals(Strings.truncate("a", 1), "a");
        assertEquals(Strings.truncate("a", 2), "a");
        assertEquals(Strings.truncate("abc", 0), "...");
        assertEquals(Strings.truncate("abc", 1), "a...");
        assertEquals(Strings.truncate("abc", 2), "ab...");
        assertEquals(Strings.truncate("abc", 3), "abc");
        assertEquals(Strings.truncate("abc", 4), "abc");
        try {
            assertEquals(Strings.truncate("", -1), "");
            fail();
        } catch (IllegalArgumentException illegalArgumentException) {
        }
    }

    public void test_chopSuffix() {
        try {
            assertEquals(Strings.chopSuffix("", -1), "");
            fail();
        } catch  (IndexOutOfBoundsException indexOutOfBoundsException) {
        }
        assertEquals(Strings.chopSuffix("", 0), "");
        try {
            assertEquals(Strings.chopSuffix("", 1), "");
            fail();
        } catch  (IndexOutOfBoundsException indexOutOfBoundsException) {
        }

        try {
            assertEquals(Strings.chopSuffix("abc", -1), "abc");
            fail();
        } catch  (IndexOutOfBoundsException indexOutOfBoundsException) {
        }
        assertEquals(Strings.chopSuffix("abc", 0), "abc");
        assertEquals(Strings.chopSuffix("abc", 1), "ab");
        assertEquals(Strings.chopSuffix("abc", 2), "a");
        assertEquals(Strings.chopSuffix("abc", 3), "");
        try {
            assertEquals(Strings.chopSuffix("abc", 4), "");
            fail();
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
        }
    }

    public void test_indent() {
        assertEquals(Strings.indent("", "foo"), "");
        assertEquals(Strings.indent("bar", "foo"), "foobar");
        assertEquals(Strings.indent("\n", "foo"), "foo\n");
        assertEquals(Strings.indent("bar\n", "foo"), "foobar\n");
        assertEquals(Strings.indent("bar\nbar", "foo"), "foobar\nfoobar");

        assertEquals(Strings.indent("", -1), "");
        assertEquals(Strings.indent("bar", -1), "bar");
        assertEquals(Strings.indent("\n", -1), "\n");
        assertEquals(Strings.indent("bar\n", -1), "bar\n");

        assertEquals(Strings.indent("", 0), "");
        assertEquals(Strings.indent("bar", 0), "bar");
        assertEquals(Strings.indent("\n", 0), "\n");
        assertEquals(Strings.indent("bar\n", 0), "bar\n");

        assertEquals(Strings.indent("", 1), "");
        assertEquals(Strings.indent("bar", 1), " bar");
        assertEquals(Strings.indent("\n", 1), " \n");
        assertEquals(Strings.indent("bar\n", 1), " bar\n");
    }
}
