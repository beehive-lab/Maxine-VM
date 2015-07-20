/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.graal.hosted;

import java.io.*;

import com.sun.max.ide.*;
import com.sun.max.io.*;


public abstract class SourceGenerator {

    public interface PackageChecker {
        boolean include(String name);
    }

    protected PrintStream out;
    private ByteArrayOutputStream baos;
    protected PackageChecker packageChecker;

    protected SourceGenerator() {
        this.baos = new ByteArrayOutputStream();
        this.out = new PrintStream(baos);
    }

    protected boolean generate(boolean checkOnly, Class<?> target) throws IOException {
        File base = new File(JavaProject.findWorkspace(), "com.oracle.max.vm.ext.graal/src");
        File outputFile = new File(base, target.getName().replace('.', File.separatorChar) + ".java").getAbsoluteFile();
        doGenerate();
        ReadableSource content = ReadableSource.Static.fromString(baos.toString());
        boolean result = Files.updateGeneratedContent(outputFile, content, "// START GENERATED CODE", "// END GENERATED CODE", checkOnly);
        if (result) {
            System.out.println("Source for " + target + (checkOnly ? " would be" : " was") + " updated");
            System.out.print(baos.toString());
        }
        return result;
    }

    protected boolean includePackage(String name) {
        if (packageChecker == null) {
            return true;
        } else {
            return packageChecker.include(name);
        }
    }

    public SourceGenerator setPackageChecker(PackageChecker packageChecker) {
        this.packageChecker = packageChecker;
        return this;
    }

    /**
     * Returns the argument with first character upper-cased.
     * @param s
     */
    protected String toFirstUpper(String s) {
        if (s.length() == 0) {
            return s;
        } else {
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        }
    }

    protected abstract void doGenerate() throws IOException;


}
