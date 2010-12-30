/*
 * Copyright (c) 2009, 2009, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
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
