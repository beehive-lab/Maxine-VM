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
/*VCSID=d0ce656f-84f1-4055-9400-68597dace0b6*/
package test.com.sun.max.util;

import com.sun.max.ide.*;
import com.sun.max.util.*;

/**
 * Tests for {@link Deferrable}.
 *
 * @author Hiroshi Yamauchi
 */
public class DeferrableTest extends MaxTestCase {

    public DeferrableTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(DeferrableTest.class);
    }

    private int _counter;

    public void test_immediate_run() {
        _counter = 0;
        final Deferrable.Queue queue = Deferrable.createRunning();
        new Deferrable(queue) {
            public void run() {
                _counter = 19;
            }
        };
        assertTrue(_counter == 19);
    }

    public void test_deferred_run() {
        _counter = 0;
        final Deferrable.Queue queue = Deferrable.createRunning();
        new Deferrable.Block(queue) {
            public void run() {
                assertTrue(_counter == 0);
                for (int i = 0; i < 100; i++) {
                    new Deferrable(queue) {
                        public void run() {
                            _counter++;
                        }
                    };
                }
            }
        };
        assertTrue(_counter == 100);
    }
}
