/*
 * Copyright (c) 2007, 2009, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.max.vm.cps.amd64;

import java.io.*;

import junit.framework.*;
import test.com.sun.max.vm.cps.*;

import com.sun.max.asm.*;
import com.sun.max.asm.dis.amd64.*;
import com.sun.max.io.*;
import com.sun.max.program.*;
import com.sun.max.vm.compiler.snippet.*;
import com.sun.max.vm.cps.target.*;

/**
 * Test whether the internal Snippets get translated at all.
 * This test is subsumed by each of the other translator tests.
 *
 * @author Bernd Mathiske
 */
@org.junit.runner.RunWith(org.junit.runners.AllTests.class)
public class AMD64TranslatorTest_snippets extends CompilerTestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AMD64TranslatorTest_snippets.suite());
    }

    public static Test suite() {
        return new AMD64TranslatorTestSetup(new TestSuite(AMD64TranslatorTest_snippets.class)); // This performs the test
    }

    public AMD64TranslatorTest_snippets(String name) {
        super(name);
    }

    public void test() throws IOException, AssemblyException {
        AMD64Disassembler disassembler;
        for (Snippet snippet : Snippet.snippets()) {
            Trace.line(1, "snippet " + snippet.name + ":");
            final CPSTargetMethod targetMethod = (CPSTargetMethod) compilerTestSetup().translate(snippet.executable);
            targetMethod.traceBundle(IndentWriter.traceStreamWriter());
            disassembler = new AMD64Disassembler(targetMethod.codeStart().toLong(), InlineDataDecoder.createFrom(targetMethod.encodedInlineDataDescriptors()));
            final BufferedInputStream stream = new BufferedInputStream(new ByteArrayInputStream(targetMethod.code()));
            disassembler.scanAndPrint(stream, Trace.stream());
        }
    }
}
