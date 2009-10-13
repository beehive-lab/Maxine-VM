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
package test.com.sun.max.io;

import java.io.*;

import junit.framework.*;

import com.sun.max.io.*;


public class StreamsTest extends TestCase {

    public StreamsTest(String name) {
        super(name);
    }

    private static BufferedInputStream asStream(String s) {
        return new BufferedInputStream(new ByteArrayInputStream(s.getBytes()));
    }

    private static boolean streamSearch(String content, String... keys) {
        try {
            final BufferedInputStream stream = asStream(content);
            for (String key : keys) {
                if (!Streams.search(stream, key.getBytes())) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void test_search() {
        assertTrue(streamSearch("search", "search"));
        assertTrue(streamSearch("search", ""));
        assertTrue(streamSearch("", ""));
        assertTrue(streamSearch("search", "sea"));
        assertTrue(streamSearch("seasearch", "search"));
        assertTrue(streamSearch("one two three", "one", "two", "three"));

        assertFalse(streamSearch("se arch", "sea"));
        assertFalse(streamSearch("", "key"));
    }
}
