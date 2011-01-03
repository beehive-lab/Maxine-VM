/*
 * Copyright (c) 2010, 2010, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.jit;

import test.com.sun.max.vm.cps.*;

import com.sun.max.vm.cps.jit.*;

/**
 * Placeholder for testing compilation of arbitrary app methods that do not pertain to Maxine, for testing purposes.
 * @author Laurent Daynes
 */
public class JitTest_compileAppClasses extends JitCompilerTestCase {


    public void test_lusearch() {
        try {
            Class testedClass = Class.forName("org.apache.lucene.queryParser.QueryParserTokenManager");
            final JitTargetMethod method = compileMethod(testedClass, "getNextToken");
            traceBundleAndDisassemble(method);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
