/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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
package test.output;

import java.io.*;

public class ManyOpenedFilesTest {
    public static void main(String[] args) {
        System.out.println("Creating 10000 Testing File");
        File tempDir = new File("/tmp/testdir");
        tempDir.mkdir();
        try {
            for (int i = 0; i < 10000; i++) {
                new PrintStream(new FileOutputStream("/tmp/testdir/test." + i)).println("tempt test file #" + i);
            }
        } catch (IOException eio) {
            eio.printStackTrace(System.err);
        } finally {
            System.out.println("Deleting created test files");
            for (String filename : tempDir.list()) {
                new File(tempDir, filename).delete();
            }
            tempDir.delete();
            System.out.println("Done");
        }
    }
}
