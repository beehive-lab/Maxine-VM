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
package test.com.sun.max.vm;

import java.io.*;

import junit.framework.*;

import com.sun.max.ide.*;
import com.sun.max.io.*;
import com.sun.max.program.*;

/**
 *
 * @author Doug Simon
 */
public abstract class VmTestCase extends MaxTestCase {

    /**
     * An indent writer that sends its output to the standard {@linkplain Trace#stream() trace stream}.
     */
    public static final IndentWriter INDENT_WRITER = new IndentWriter(new PrintWriter(Trace.stream(), true));
    /**
     * A handle to the result of a running test is maintained so that individual method compilation failures can be
     * reported without short-circuiting compilation of other methods in the enclosing class/package being tested.
     * Of course this will not apply to test cases that only compile one method and execute it.
     */
    public TestResult testResult;

    public VmTestCase() {
    }

    public VmTestCase(String name) {
        super(name);
    }

    protected void addTestError(Throwable error) {
        testResult.addError(this, error);
    }

    @Override
    public void run(TestResult result) {
        testResult = result;
        try {
            super.run(result);
        } finally {
            testResult = null;
        }
    }
}
